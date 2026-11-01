/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ChestSearchHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.tileentity.TileEntityShulkerBox;
import net.minecraft.util.math.AxisAlignedBB;
import net.wurst0.Category;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

/**
 * Highlights containers that hold an item matching the search text.
 * Works because the server syncs container contents for tiles that
 * are in render distance.
 */
public final class ChestSearchHack extends Hack
        implements UpdateListener, RenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting query =
                new TextFieldSetting("Search for", "", 32);
        private final SliderSetting range =
                new SliderSetting("Range", 16, 8, 32, 4);

        private final List<AxisAlignedBB> found =
                new ArrayList<AxisAlignedBB>();
        private int scanTimer;

        public ChestSearchHack()
        {
                super("ChestSearch",
                        "Highlights chests and shulkers that contain an item"
                                + " matching the search text.");
                setCategory(Category.RENDER);
                addSettings(query, range);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(--scanTimer > 0)
                        return;
                scanTimer = 20;

                found.clear();
                String q = query.getValue().toLowerCase();
                if(q.isEmpty())
                        return;

                double rangeSq = range.getValue() * range.getValue();
                for(TileEntity te : mc.world.loadedTileEntityList)
                {
                        if(!(te instanceof TileEntityLockableLoot))
                                continue;
                        if(!(te instanceof TileEntityChest)
                                && !(te instanceof TileEntityShulkerBox))
                                continue;

                        double dist = te.getDistanceSq(
                                p.posX, p.posY, p.posZ);
                        if(dist > rangeSq)
                                continue;

                        TileEntityLockableLoot loot =
                                (TileEntityLockableLoot)te;
                        int size = loot.getSizeInventory();
                        for(int i = 0; i < size; i++)
                        {
                                ItemStack stack = loot.getStackInSlot(i);
                                if(stack.isEmpty())
                                        continue;
                                if(stack.getDisplayName().toLowerCase()
                                        .contains(q))
                                {
                                        found.add(new AxisAlignedBB(te.getPos()));
                                        break;
                                }
                        }
                }
        }

        @Override
        public void onRender(float partialTicks)
        {
                if(found.isEmpty() || mc.player == null)
                        return;

                RenderUtils.enable3D(1.5F, false);
                RenderUtils.applyCameraTransform(partialTicks);
                for(AxisAlignedBB box : found)
                        RenderUtils.drawBox(box, 0F, 1F, 1F, 0.6F);
                RenderUtils.disable3D(false);
        }

        @Override
        protected void onEnable()
        {
                scanTimer = 0;
                found.clear();
        }

        @Override
        protected void onDisable()
        {
                found.clear();
        }
}
