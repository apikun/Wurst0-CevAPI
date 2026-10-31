/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AirMinerHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.Iterator;

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

/**
 * Mines blocks in a small sphere around you without any aiming - made
 * for mining while flying/gliding. Lite port: digs the closest
 * breakable blocks near the player, up to N per tick.
 */
public final class AirMinerHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 2, 1, 4, 0.5);
        private final SliderSetting blocksPerTick =
                new SliderSetting("Blocks per tick", 1, 1, 5, 1);
        private final CheckboxSetting swing =
                new CheckboxSetting("Swing", false);

        private BlockPos lastTarget;

        public AirMinerHack()
        {
                super("AirMiner",
                        "Mines blocks around you without aiming, e.g. while flying.");
                setCategory(Category.OTHER);
                addSettings(range, blocksPerTick, swing);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                PlayerControllerMP pc = mc.playerController;
                int max = blocksPerTick.getValueI();
                int dug = 0;

                for(int tick = 0; tick < max; tick++)
                {
                        BlockPos best = findNext(p);
                        if(best == null)
                                break;

                        if(!best.equals(lastTarget))
                        {
                                pc.clickBlock(best, EnumFacing.UP);
                                lastTarget = best;
                        }
                        pc.onPlayerDamageBlock(best, EnumFacing.UP);
                        if(swing.isChecked())
                                p.swingArm(EnumHand.MAIN_HAND);
                        dug++;

                        // instant-break blocks finish immediately; keep going
                        IBlockState state = mc.world.getBlockState(best);
                        if(state.getMaterial() != Material.AIR
                                && state.getBlockHardness(mc.world, best) > 0)
                                break; // slow block: one per tick
                }

                if(dug == 0)
                        lastTarget = null;
        }

        private BlockPos findNext(EntityPlayerSP p)
        {
                double rangeSq = range.getValue() * range.getValue();
                int r = (int)Math.ceil(range.getValue());
                BlockPos center = new BlockPos(p.posX, p.posY + 0.5, p.posZ);

                Iterator<BlockPos> itr = BlockPos.getAllInBox(
                        center.add(-r, -r, -r), center.add(r, r, r)).iterator();

                BlockPos best = null;
                double bestDist = rangeSq;
                while(itr.hasNext())
                {
                        BlockPos pos = itr.next();
                        IBlockState state = mc.world.getBlockState(pos);
                        Material m = state.getMaterial();
                        if(m == Material.AIR || m.isLiquid())
                                continue;
                        if(state.getBlockHardness(mc.world, pos) < 0)
                                continue;

                        double dist = center.distanceSq(pos);
                        if(dist <= bestDist && !pos.equals(lastTarget))
                        {
                                bestDist = dist;
                                best = pos;
                        }
                }
                return best;
        }

        @Override
        protected void onDisable()
        {
                lastTarget = null;
        }
}
