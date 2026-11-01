/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI OutreachHack (GPL-3.0-or-later)
 * Lite adaptation: breaks or places blocks beyond the vanilla
 * reach by re-raytracing up to the configured distance and driving
 * the same PlayerControllerMP dig path the game uses.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.material.Material;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;

public final class OutreachHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final ModeSetting mode = new ModeSetting("Mode",
                new String[]{"Break", "Place", "Both"}, 0);
        private final SliderSetting reach =
                new SliderSetting("Reach", 12, 6, 24, 1);

        private BlockPos lastBreak;

        public OutreachHack()
        {
                super("Outreach",
                        "Breaks and places blocks beyond the vanilla reach"
                                + " limit (client-side dig packets).");
                setCategory(Category.BLOCKS);
                addSettings(mode, reach);
        }

        private RayTraceResult extendedRay(EntityPlayerSP p)
        {
                Vec3d eyes = p.getPositionVector()
                        .add(0, p.getEyeHeight(), 0);
                Vec3d dir = p.getLookVec();
                Vec3d end = eyes.add(
                        dir.x * reach.getValue(),
                        dir.y * reach.getValue(),
                        dir.z * reach.getValue());
                return mc.world.rayTraceBlocks(eyes, end, false,
                        true, false);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(!mc.gameSettings.keyBindAttack.isKeyDown()
                        && !mc.gameSettings.keyBindUseItem.isKeyDown())
                {
                        lastBreak = null;
                        return;
                }

                RayTraceResult trace = extendedRay(p);
                if(trace == null
                        || trace.typeOfHit != RayTraceResult.Type.BLOCK)
                        return;

                BlockPos pos = trace.getBlockPos();
                EnumFacing side = trace.sideHit;
                PlayerControllerMP pc = mc.playerController;
                String m = mode.getSelected();

                boolean wantBreak = mc.gameSettings.keyBindAttack.isKeyDown()
                        && !m.equals("Place");
                boolean wantPlace = mc.gameSettings.keyBindUseItem.isKeyDown()
                        && !m.equals("Break");

                if(wantBreak)
                {
                        Material mat = mc.world.getBlockState(pos)
                                .getMaterial();
                        if(mat != Material.AIR && !mat.isLiquid())
                        {
                                if(!pos.equals(lastBreak))
                                {
                                        pc.clickBlock(pos, side);
                                        lastBreak = pos;
                                }
                                pc.onPlayerDamageBlock(pos, side);
                                p.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
                        }
                }
                else if(wantPlace)
                {
                        if(mc.world.getBlockState(pos).getMaterial()
                                != Material.AIR)
                        {
                                pc.processRightClickBlock(p, mc.world,
                                        pos, side, trace.hitVec,
                                        net.minecraft.util.EnumHand.MAIN_HAND);
                                p.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
                        }
                }
        }

        @Override
        protected void onDisable()
        {
                lastBreak = null;
        }
}
