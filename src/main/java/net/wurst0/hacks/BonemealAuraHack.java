/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BonemealAuraHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.Iterator;

import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Automatically uses bone meal on every growable block (crops,
 * saplings, ...) in range.
 */
public final class BonemealAuraHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 4, 2, 6, 1);
        private final SliderSetting delay =
                new SliderSetting("Delay (ticks)", 4, 0, 20, 1);
        private final CheckboxSetting checkCollide =
                new CheckboxSetting("Check line of sight", true);

        private int timer;

        public BonemealAuraHack()
        {
                super("BonemealAura",
                        "Automatically bone-meals crops, saplings and other"
                                + " growable blocks around you.");
                setCategory(Category.BLOCKS);
                addSettings(range, delay, checkCollide);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(delay.getValueI() > 0 && --timer > 0)
                        return;
                timer = delay.getValueI();

                int slot = findBonemeal(p);
                if(slot == -1)
                        return;

                int r = range.getValueI();
                BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);

                Iterator<BlockPos> itr = BlockPos.getAllInBox(
                        center.add(-r, -r, -r), center.add(r, r, r))
                        .iterator();
                while(itr.hasNext())
                {
                        BlockPos pos = itr.next();
                        IBlockState state = mc.world.getBlockState(pos);
                        if(!(state.getBlock() instanceof IGrowable))
                                continue;

                        IGrowable growable = (IGrowable)state.getBlock();
                        if(!growable.canGrow(mc.world, pos, state,
                                mc.world.isRemote))
                                continue;

                        if(checkCollide.isChecked())
                        {
                                net.minecraft.util.math.RayTraceResult trace =
                                        mc.world.rayTraceBlocks(
                                                p.getPositionVector()
                                                        .add(0,
                                                                p.getEyeHeight(),
                                                                0),
                                                new net.minecraft.util.math.Vec3d(
                                                        pos.getX() + 0.5,
                                                        pos.getY() + 0.5,
                                                        pos.getZ() + 0.5));
                                if(trace != null && trace.typeOfHit
                                        == net.minecraft.util.math.RayTraceResult.Type.BLOCK
                                        && !trace.getBlockPos().equals(pos))
                                        continue;
                        }

                        if(p.inventory.currentItem != slot)
                        {
                                p.inventory.currentItem = slot;
                                mc.playerController.updateController();
                                return;
                        }

                        mc.playerController.processRightClickBlock(p, mc.world, pos, EnumFacing.UP, net.minecraft.util.math.Vec3d.ZERO, EnumHand.MAIN_HAND);
                        p.swingArm(EnumHand.MAIN_HAND);
                        return;
                }
        }

        private int findBonemeal(EntityPlayerSP p)
        {
                for(int i = 0; i < 9; i++)
                {
                        ItemStack stack = p.inventory.mainInventory.get(i);
                        if(!stack.isEmpty()
                                && stack.getItem() == Items.DYE
                                && stack.getItemDamage() == 15)
                                return i;
                }
                return -1;
        }

        @Override
        protected void onEnable()
        {
                timer = 0;
        }
}
