/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BuildRandomHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
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
import net.wurst0.settings.SliderSetting;

public final class BuildRandomHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 5, 1, 6, 0.5);
        private final SliderSetting interval =
                new SliderSetting("Place delay (ticks)", 2, 0, 20, 1);
        private final CheckboxSetting autoSwitch =
                new CheckboxSetting("Auto-switch", true);
        private final CheckboxSetting swing = new CheckboxSetting("Swing", true);

        private int timer;

        public BuildRandomHack()
        {
                super("BuildRandom",
                        "Randomly places blocks from your hotbar all around you,"
                                + " like Wurst's classic BuildRandom.");
                setCategory(Category.BLOCKS);
                addSettings(range, interval, autoSwitch, swing);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;
                if(timer-- > 0)
                        return;

                ThreadLocalRandom random = ThreadLocalRandom.current();
                int r = range.getValueI();
                BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);

                for(int attempt = 0; attempt < 16; attempt++)
                {
                        BlockPos pos = center.add(random.nextInt(-r, r + 1),
                                random.nextInt(-r, r + 1), random.nextInt(-r, r + 1));
                        if(!mc.world.isAirBlock(pos))
                                continue;

                        EnumFacing face = findSupportFace(pos);
                        if(face == null)
                                continue;

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

                        BlockPos support = pos.offset(face);
                        EnumFacing clickFace = face.getOpposite();
                        Vec3d hitVec = new Vec3d(
                                support.getX() + 0.5 + clickFace.getXOffset() * 0.5,
                                support.getY() + 0.5 + clickFace.getYOffset() * 0.5,
                                support.getZ() + 0.5 + clickFace.getZOffset() * 0.5);

                        mc.playerController.processRightClickBlock(p, mc.world,
                                support, clickFace, hitVec, EnumHand.MAIN_HAND);
                        if(swing.isChecked())
                                p.swingArm(EnumHand.MAIN_HAND);

                        timer = interval.getValueI();
                        return;
                }

                timer = interval.getValueI();
        }

        private EnumFacing findSupportFace(BlockPos pos)
        {
                for(EnumFacing face : EnumFacing.values())
                {
                        IBlockState state = mc.world.getBlockState(pos.offset(face));
                        if(state.getMaterial() != Material.AIR
                                && state.getMaterial().isSolid())
                                return face;
                }
                return null;
        }

        private int findBlockSlot(EntityPlayerSP p)
        {
                for(int i = 0; i < 9; i++)
                {
                        ItemStack stack = p.inventory.mainInventory.get(i);
                        if(!stack.isEmpty() && stack.getItem() instanceof ItemBlock)
                                return i;
                }
                return -1;
        }
}
