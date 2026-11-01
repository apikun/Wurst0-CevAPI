/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI LootSearchHack (GPL-3.0-or-later)
 * Lite adaptation: the 26.x loot-export JSON workflow is replaced by
 * live scanning of every loaded container tile entity, with ESP boxes
 * for containers holding a matching item.
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

/**
 * Scans all loaded containers (chests, shulkers, furnaces, hoppers,
 * dispensers, brewing stands) for an item and highlights matches.
 */
public final class LootSearchHack extends Hack
        implements UpdateListener, RenderListener, ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting query =
                new TextFieldSetting("Search for", "", 32);
        private final SliderSetting range =
                new SliderSetting("Range", 16, 8, 32, 4);

        private final List<AxisAlignedBB> found =
                new ArrayList<AxisAlignedBB>();
        private int matchingStacks;
        private int matchingContainers;
        private int scanTimer;

        public LootSearchHack()
        {
                super("LootSearch",
                        "Searches every loaded container (chest, shulker,"
                                + " furnace, hopper, dispenser, brewing"
                                + " stand) for an item and highlights the"
                                + " matches.");
                setCategory(Category.ITEMS);
                addSettings(query, range);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                {
                        found.clear();
                        return;
                }

                if(--scanTimer > 0)
                        return;
                scanTimer = 20;

                found.clear();
                matchingStacks = 0;
                matchingContainers = 0;

                String q = query.getValue().toLowerCase().trim();
                if(q.isEmpty())
                        return;

                double rangeSq = range.getValue() * range.getValue();
                for(TileEntity te : mc.world.loadedTileEntityList)
                {
                        if(!(te instanceof IInventory))
                                continue;
                        // skip anything that isn't a synced container block
                        if(!(te instanceof net.minecraft.tileentity.TileEntityLockable))
                                continue;

                        if(te.getDistanceSq(p.posX, p.posY, p.posZ)
                                > rangeSq)
                                continue;

                        IInventory inv = (IInventory)te;
                        int size = inv.getSizeInventory();
                        int hits = 0;
                        for(int i = 0; i < size; i++)
                        {
                                ItemStack stack = inv.getStackInSlot(i);
                                if(stack.isEmpty())
                                        continue;
                                if(!matches(stack, q))
                                        continue;
                                hits++;
                        }

                        if(hits > 0)
                        {
                                found.add(new AxisAlignedBB(te.getPos()));
                                matchingContainers++;
                                matchingStacks += hits;
                        }
                }
        }

        private boolean matches(ItemStack stack, String q)
        {
                if(stack.getDisplayName().toLowerCase().contains(q))
                        return true;
                // also match the item id (e.g. "diamond")
                if(stack.getItem().getRegistryName() != null
                        && stack.getItem().getRegistryName()
                                .getPath().contains(q))
                        return true;
                return false;
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
        public void onScreenRender(float partialTicks)
        {
                if(matchingContainers == 0 || mc.player == null)
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                mc.fontRenderer.drawStringWithShadow(
                        "\u00a7bLootSearch: \u00a7f" + matchingStacks
                                + " matching stacks in "
                                + matchingContainers + " containers",
                        2, sr.getScaledHeight() - 52, 0xFFFFFF);
        }

        @Override
        protected void onEnable()
        {
                scanTimer = 0;
                found.clear();
                matchingStacks = 0;
                matchingContainers = 0;
                if(mc.player != null
                        && !query.getValue().trim().isEmpty())
                        mc.player.sendMessage(new TextComponentString(
                                "\u00a7b[LootSearch] Searching for \""
                                        + query.getValue().trim() + "\""));
        }

        @Override
        protected void onDisable()
        {
                found.clear();
                matchingStacks = 0;
                matchingContainers = 0;
        }
}
