/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoMineHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
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

public final class AutoMineHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting swing =
                new CheckboxSetting("Swing", true);

        private BlockPos lastPos;

        public AutoMineHack()
        {
                super("AutoMine",
                        "Automatically mines whatever block your crosshair is"
                                + " pointing at, as if you were holding down the attack"
                                + " button without any delay.");
                setCategory(Category.BLOCKS);
                addSetting(swing);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                PlayerControllerMP pc = mc.playerController;
                if(p == null || mc.world == null || pc == null)
                        return;
                if(mc.objectMouseOver == null
                        || mc.objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK)
                {
                        lastPos = null;
                        return;
                }

                BlockPos pos = mc.objectMouseOver.getBlockPos();
                if(pos == null)
                        return;

                IBlockState state = mc.world.getBlockState(pos);
                if(state.getMaterial() == Material.AIR
                        || state.getBlockHardness(mc.world, pos) < 0)
                {
                        lastPos = null;
                        return;
                }

                EnumFacing side = mc.objectMouseOver.sideHit;
                if(!pos.equals(lastPos))
                {
                        pc.clickBlock(pos, side);
                        lastPos = pos;
                }
                pc.onPlayerDamageBlock(pos, side);

                if(swing.isChecked())
                        p.swingArm(EnumHand.MAIN_HAND);
        }

        @Override
        protected void onDisable()
        {
                lastPos = null;
        }
}
