/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BedBreakAuraHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.Iterator;

import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Automatically breaks the closest bed in range (one or two block
 * beds - breaking either half drops the whole bed). Same dig path
 * as Nuker (clickBlock + onPlayerDamageBlock).
 */
public final class BedBreakAuraHack extends Hack
        implements UpdateListener, RenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 4.5, 2, 6, 0.1);
        private final CheckboxSetting swing =
                new CheckboxSetting("Swing", true);

        private BlockPos lastTarget;

        public BedBreakAuraHack()
        {
                super("BedBreakAura",
                        "Automatically breaks beds around you, starting with the"
                                + " closest one.");
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
                        if(!(state.getBlock() instanceof BlockBed))
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

        @Override
        public void onRender(float partialTicks)
        {
                if(lastTarget == null || mc.player == null || mc.world == null)
                        return;

                RenderUtils.enable3D(2F, false);
                RenderUtils.applyCameraTransform(partialTicks);
                RenderUtils.drawBox(new net.minecraft.util.math.AxisAlignedBB(
                        lastTarget.getX(), lastTarget.getY(), lastTarget.getZ(),
                        lastTarget.getX() + 1, lastTarget.getY() + 1,
                        lastTarget.getZ() + 1), 1F, 0.2F, 0.2F, 0.5F);
                RenderUtils.disable3D(false);
        }

        @Override
        protected void onDisable()
        {
                lastTarget = null;
        }
}
