/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NoClipHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Client-side noclip: disables collision on your player and lets
 * you fly through blocks with WASD. Purely client-side - without
 * spectator/flight permission on the server it will rubber-band.
 */
public final class NoClipHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting speed =
                new SliderSetting("Speed", 0.5, 0.05, 2, 0.05);

        public NoClipHack()
        {
                super("NoClip",
                        "Allows you to fly through walls client-side. The server"
                                + " will pull you back unless you have permission.");
                setCategory(Category.MOVEMENT);
                addSetting(speed);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                p.noClip = true;
                p.onGround = false;
                p.fallDistance = 0;

                double s = speed.getValue();
                double forward = p.movementInput != null
                        ? p.movementInput.moveForward : 0;
                double strafe = p.movementInput != null
                        ? p.movementInput.moveStrafe : 0;

                double yaw = Math.toRadians(p.rotationYaw);
                double dx = -Math.sin(yaw) * forward + Math.cos(yaw) * strafe;
                double dz = Math.cos(yaw) * forward + Math.sin(yaw) * strafe;
                double len = Math.sqrt(dx * dx + dz * dz);
                if(len > 1)
                {
                        dx /= len;
                        dz /= len;
                }

                p.motionX = dx * s;
                p.motionZ = dz * s;

                if(mc.gameSettings.keyBindJump.isKeyDown())
                        p.motionY = s * 0.75;
                else if(p.movementInput != null && p.movementInput.sneak)
                        p.motionY = -s * 0.75;
                else
                        p.motionY = 0;
        }

        @Override
        protected void onDisable()
        {
                if(mc.player != null)
                {
                        mc.player.noClip = false;
                        mc.player.motionX = 0;
                        mc.player.motionY = 0;
                        mc.player.motionZ = 0;
                }
        }
}
