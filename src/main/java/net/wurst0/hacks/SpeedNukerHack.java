/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SpeedNukerHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.lang.reflect.Field;
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
import net.wurst0.util.ReflectUtils;

public final class SpeedNukerHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 4.5, 1, 5, 0.1);
        private final SliderSetting speed =
                new SliderSetting("Speed", 0.5, 0.05, 1, 0.05);
        private final CheckboxSetting swing =
                new CheckboxSetting("Swing", true);

        private BlockPos lastTarget;
        private Field curBlockDamageMP;
        private Field blockHitDelay;

        public SpeedNukerHack()
        {
                super("SpeedNuker",
                        "Like Nuker, but it breaks blocks several times faster by"
                                + " adding extra mining progress every tick.");
                setCategory(Category.BLOCKS);
                addSettings(range, speed, swing);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                PlayerControllerMP pc = mc.playerController;
                if(p == null || mc.world == null || pc == null)
                        return;

                if(curBlockDamageMP == null)
                        curBlockDamageMP = ReflectUtils.field(PlayerControllerMP.class,
                                "curBlockDamageMP", "field_78770_f");
                if(blockHitDelay == null)
                        blockHitDelay = ReflectUtils.field(PlayerControllerMP.class,
                                "blockHitDelay", "field_78781_i");

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
                        Material material = state.getMaterial();
                        if(material == Material.AIR || material.isLiquid())
                                continue;
                        if(state.getBlockHardness(mc.world, pos) < 0)
                                continue;
                        if(ProtectHack.isProtected(pos))
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

                if(!best.equals(lastTarget))
                {
                        pc.clickBlock(best, EnumFacing.UP);
                        lastTarget = best;
                }

                try
                {
                        if(blockHitDelay != null)
                                blockHitDelay.setInt(pc, 0);
                        if(curBlockDamageMP != null)
                        {
                                float progress = curBlockDamageMP.getFloat(pc);
                                curBlockDamageMP.setFloat(pc,
                                        Math.min(1F, progress + speed.getValueF()));
                        }
                }catch(Throwable t)
                {
                        // reflection failed - vanilla speed
                }

                pc.onPlayerDamageBlock(best, EnumFacing.UP);

                if(swing.isChecked())
                        p.swingArm(EnumHand.MAIN_HAND);
        }

        @Override
        protected void onDisable()
        {
                lastTarget = null;
        }
}
