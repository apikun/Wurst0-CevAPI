/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI PlaceNBreakHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Places a block and instantly starts breaking it again, in an
 * alternating loop. Classic trick for testing pistons/sand and for
 * building-and-removing scaffolds fast.
 */
public final class PlaceNBreakHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting interval =
                new SliderSetting("Interval (ticks)", 2, 1, 20, 1);
        private final CheckboxSetting swing =
                new CheckboxSetting("Swing", true);

        private int timer;
        private boolean phasePlace;
        private BlockPos lastPos;

        public PlaceNBreakHack()
        {
                super("PlaceNBreak",
                        "Places a block and immediately breaks it again,"
                                + " alternating while you hold the use key.");
                setCategory(Category.BLOCKS);
                addSettings(interval, swing);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(!mc.gameSettings.keyBindUseItem.isKeyDown()
                        && !mc.gameSettings.keyBindAttack.isKeyDown())
                        return;

                if(--timer > 0)
                        return;
                timer = interval.getValueI();

                if(mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit
                        != RayTraceResult.Type.BLOCK)
                        return;

                PlayerControllerMP pc = mc.playerController;
                BlockPos pos = mc.objectMouseOver.getBlockPos();
                EnumFacing side = mc.objectMouseOver.sideHit;

                if(phasePlace)
                {
                        pc.processRightClickBlock(p, mc.world, pos, side, mc.objectMouseOver.hitVec, EnumHand.MAIN_HAND);
                        lastPos = pos;
                }
                else
                {
                        BlockPos breakPos =
                                lastPos != null
                                        && mc.world.getBlockState(lastPos)
                                                .getMaterial()
                                                != Material.AIR
                                                ? lastPos : pos;
                        Material mat =
                                mc.world.getBlockState(breakPos).getMaterial();
                        if(mat != Material.AIR && !mat.isLiquid())
                        {
                                pc.clickBlock(breakPos, EnumFacing.UP);
                                pc.onPlayerDamageBlock(breakPos, EnumFacing.UP);
                                if(swing.isChecked())
                                        p.swingArm(EnumHand.MAIN_HAND);
                        }
                }
                phasePlace = !phasePlace;
        }

        @Override
        protected void onEnable()
        {
                timer = 0;
                phasePlace = true;
                lastPos = null;
        }
}
