/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ScaffoldWalkHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

public final class ScaffoldWalkHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting autoSwitch =
                new CheckboxSetting("Auto-switch", true);
        private final CheckboxSetting swing = new CheckboxSetting("Swing", true);

        public ScaffoldWalkHack()
        {
                super("ScaffoldWalk",
                        "Automatically places blocks under your feet as you walk,"
                                + " so you can bridge across gaps without stopping.");
                setCategory(Category.BLOCKS);
                addSettings(autoSwitch, swing);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;
                if(mc.currentScreen != null)
                        return;

                BlockPos below = new BlockPos(p.posX, p.posY - 1, p.posZ);
                if(!mc.world.isAirBlock(below))
                        return;

                // Scaffold needs a solid block to click on: the one underneath
                // the empty spot we want to fill.
                BlockPos support = below.down();
                if(mc.world.isAirBlock(support))
                        return;

                int slot = findBlockSlot(p);
                if(slot == -1)
                        return;

                if(p.inventory.currentItem != slot)
                {
                        if(!autoSwitch.isChecked())
                                return;
                        p.inventory.currentItem = slot;
                        mc.playerController.updateController();
                }

                Vec3d hitVec = new Vec3d(support.getX() + 0.5D,
                        support.getY() + 1D, support.getZ() + 0.5D);
                mc.playerController.processRightClickBlock(p, mc.world, support,
                        EnumFacing.UP, hitVec, EnumHand.MAIN_HAND);

                if(swing.isChecked())
                        p.swingArm(EnumHand.MAIN_HAND);
        }

        private int findBlockSlot(EntityPlayerSP p)
        {
                ItemStack held = p.inventory.getCurrentItem();
                if(!held.isEmpty() && held.getItem() instanceof ItemBlock)
                        return p.inventory.currentItem;

                for(int i = 0; i < 9; i++)
                {
                        ItemStack stack = p.inventory.mainInventory.get(i);
                        if(!stack.isEmpty() && stack.getItem() instanceof ItemBlock)
                                return i;
                }
                return -1;
        }
}
