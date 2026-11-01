/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ElytraWalkHack (GPL-3.0-or-later)
 * Lite adaptation: keeps you hovering just above the ground while
 * gliding and moves you at walking speed from your movement keys.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class ElytraWalkHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting walkSpeed =
                new SliderSetting("Walk speed", 0.25, 0.05, 0.6, 0.05);
        private final SliderSetting hoverHeight =
                new SliderSetting("Hover height", 1, 0.5, 3, 0.5);

        public ElytraWalkHack()
        {
                super("ElytraWalk",
                        "Lets you walk on the ground while elytra flying"
                                + " (low-altitude hover).");
                setCategory(Category.MOVEMENT);
                addSettings(walkSpeed, hoverHeight);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null || !p.isElytraFlying())
                        return;

                // hold altitude
                RayTraceResult trace = mc.world.rayTraceBlocks(
                        p.getPositionVector(),
                        p.getPositionVector().add(0,
                                -hoverHeight.getValue() - 1, 0));
                if(trace != null
                        && trace.typeOfHit == RayTraceResult.Type.BLOCK)
                {
                        double groundDist = p.posY
                                - trace.hitVec.y;
                        if(groundDist < hoverHeight.getValue())
                                p.motionY = 0.08;
                        else
                                p.motionY = 0;
                }

                // move from input
                float forward = 0;
                float strafe = 0;
                if(mc.gameSettings.keyBindForward.isKeyDown())
                        forward += 1;
                if(mc.gameSettings.keyBindBack.isKeyDown())
                        forward -= 1;
                if(mc.gameSettings.keyBindLeft.isKeyDown())
                        strafe += 1;
                if(mc.gameSettings.keyBindRight.isKeyDown())
                        strafe -= 1;

                if(forward == 0 && strafe == 0)
                {
                        p.motionX *= 0.6;
                        p.motionZ *= 0.6;
                        return;
                }

                float speed = walkSpeed.getValueF();
                float yaw = p.rotationYaw;
                float dist = MathHelper.sqrt(forward * forward
                        + strafe * strafe);
                if(dist < 1)
                        dist = 1;
                float f = forward / dist * speed;
                float s = strafe / dist * speed;
                float sin = MathHelper.sin(yaw * 0.017453292F);
                float cos = MathHelper.cos(yaw * 0.017453292F);

                p.motionX = f * -sin + s * cos;
                p.motionZ = f * cos + s * sin;
                p.fallDistance = 0;
        }
}
