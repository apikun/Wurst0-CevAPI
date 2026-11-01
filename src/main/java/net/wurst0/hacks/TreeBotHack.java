/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TreeBotHack (GPL-3.0-or-later)
 * Lite adaptation: walks to the closest tree and chops the whole
 * trunk (simple state machine, no pathfinding).
 */
package net.wurst0.hacks;

import java.util.Iterator;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class TreeBotHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 24, 8, 48, 4);
        private final CheckboxSetting jump =
                new CheckboxSetting("Jump on collision", true);

        private BlockPos currentLog;
        private BlockPos currentTreeBase;

        public TreeBotHack()
        {
                super("TreeBot",
                        "Finds the nearest tree, walks to it and chops the"
                                + " whole trunk (lite version).");
                setCategory(Category.OTHER);
                addSettings(range, jump);
        }

        private boolean isLog(BlockPos pos)
        {
                Block block = mc.world.getBlockState(pos).getBlock();
                return block == Blocks.LOG || block == Blocks.LOG2;
        }

        private BlockPos findNearestLog(EntityPlayerSP p)
        {
                double bestSq = range.getValue() * range.getValue();
                BlockPos best = null;
                BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);

                Iterator<BlockPos> itr = BlockPos.getAllInBox(
                        center.add(-16, -4, -16),
                        center.add(16, 8, 16)).iterator();
                while(itr.hasNext())
                {
                        BlockPos pos = itr.next();
                        if(!isLog(pos))
                                continue;
                        if(mc.world.getTileEntity(pos) != null)
                                continue;
                        double d = pos.distanceSq(center);
                        if(d < bestSq)
                        {
                                bestSq = d;
                                best = pos;
                        }
                }
                return best;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                // maintain current target
                if(currentLog != null && !isLog(currentLog))
                        currentLog = null;

                if(currentLog == null)
                {
                        currentLog = findNearestLog(p);
                        if(currentLog == null)
                                return;
                        currentTreeBase = currentLog;
                }

                // face the log
                double dx = currentLog.getX() + 0.5 - p.posX;
                double dz = currentLog.getZ() + 0.5 - p.posZ;
                double dy = currentLog.getY() + 0.5
                        - (p.posY + p.getEyeHeight());
                float yaw = (float)(MathHelper.atan2(dz, dx)
                        * 180.0 / Math.PI) - 90F;
                float pitch = (float)(MathHelper.atan2(dy,
                        MathHelper.sqrt(dx * dx + dz * dz))
                        * 180.0 / Math.PI);
                p.rotationYaw = interpolateAngle(p.rotationYaw, yaw);
                p.rotationPitch = MathHelper.clamp(pitch, -70F, 70F);

                double distSq = dx * dx + dz * dz;

                if(distSq > 3.2 * 3.2)
                {
                        // walk toward the tree
                        Vec3d look = p.getLookVec();
                        p.motionX = look.x * 0.14;
                        p.motionZ = look.z * 0.14;
                        if(jump.isChecked() && p.collidedHorizontally
                                && p.onGround)
                                p.jump();
                        return;
                }

                // chop: top-down is not needed; mine the current log,
                // then target the one above
                mc.playerController.clickBlock(currentLog, EnumFacing.UP);
                mc.playerController.onPlayerDamageBlock(currentLog,
                        EnumFacing.UP);
                p.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
        }

        private static float interpolateAngle(float from, float to)
        {
                float delta = MathHelper.wrapDegrees(to - from);
                return from + MathHelper.clamp(delta, -30F, 30F);
        }

        @Override
        protected void onDisable()
        {
                currentLog = null;
                currentTreeBase = null;
        }
}
