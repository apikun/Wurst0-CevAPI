/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoFarmHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.Iterator;

import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class AutoFarmHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 4.5, 1, 5, 0.1);
        private final CheckboxSetting swing =
                new CheckboxSetting("Swing", true);

        private BlockPos lastTarget;

        public AutoFarmHack()
        {
                super("AutoFarm",
                        "Automatically harvests fully grown crops around you.");
                setCategory(Category.BLOCKS);
                addSettings(range, swing);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                double rangeSq = range.getValue() * range.getValue();
                int r = (int)Math.ceil(range.getValue());

                BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);
                Iterator<BlockPos> itr = BlockPos.getAllInBox(
                        center.add(-r, -r, -r), center.add(r, r, r)).iterator();

                BlockPos best = null;
                double bestDist = rangeSq;

                while(itr.hasNext())
                {
                        BlockPos pos = itr.next();
                        IBlockState state = mc.world.getBlockState(pos);
                        if(!isMature(state))
                                continue;

                        double dist = p.getDistanceSq(pos.getX() + 0.5,
                                pos.getY() + 0.5, pos.getZ() + 0.5);
                        if(dist <= bestDist)
                        {
                                bestDist = dist;
                                best = pos;
                        }
                }

                if(best == null)
                {
                        lastTarget = null;
                        return;
                }

                PlayerControllerMP pc = mc.playerController;
                if(!best.equals(lastTarget))
                {
                        pc.clickBlock(best, EnumFacing.UP);
                        lastTarget = best;
                }
                pc.onPlayerDamageBlock(best, EnumFacing.UP);

                if(swing.isChecked())
                        p.swingArm(EnumHand.MAIN_HAND);
        }

        private boolean isMature(IBlockState state)
        {
                if(state.getMaterial() == Material.AIR)
                        return false;

                if(state.getBlock() instanceof BlockCrops)
                        return state.getValue(BlockCrops.AGE) == 7;

                if(state.getBlock() instanceof BlockNetherWart)
                        return state.getValue(BlockNetherWart.AGE) == 3;

                return false;
        }

        @Override
        protected void onDisable()
        {
                lastTarget = null;
        }
}
