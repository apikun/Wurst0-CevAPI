/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI EntityControlHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Steer any vehicle (boat, pig, horse, minecart) with your normal
 * movement keys, aiming where you look. Works client-side; servers
 * without vehicle sanity checks will follow along.
 */
public final class EntityControlHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting speed =
                new SliderSetting("Speed", 0.6, 0.1, 2, 0.1);

        public EntityControlHack()
        {
                super("EntityControl",
                        "Lets you control rideable entities with WASD,"
                                + " including boats and horses without saddles.");
                setCategory(Category.MOVEMENT);
                addSetting(speed);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || !p.isRiding())
                        return;

                Entity vehicle = p.getRidingEntity();
                if(vehicle == null || vehicle.isDead)
                        return;

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

                float s = speed.getValueF();
                float yaw = p.rotationYaw;
                float sin = MathHelper.sin(yaw * 0.017453292F);
                float cos = MathHelper.cos(yaw * 0.017453292F);

                if(forward != 0 || strafe != 0)
                {
                        float dist = MathHelper.sqrt(forward * forward
                                + strafe * strafe);
                        if(dist < 1)
                                dist = 1;
                        float f = forward / dist * s;
                        float st = strafe / dist * s;

                        vehicle.motionX = f * -sin + st * cos;
                        vehicle.motionZ = f * cos + st * sin;
                }

                if(mc.gameSettings.keyBindJump.isKeyDown())
                        vehicle.motionY = 0.5;
                else if(mc.gameSettings.keyBindSneak.isKeyDown())
                        vehicle.motionY = -0.3;

                // face the vehicle where you look (client side)
                vehicle.rotationYaw = yaw;
        }
}
