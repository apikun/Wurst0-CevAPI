/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BoatPhaseHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.util.math.MathHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * While riding a boat: disables the boat's client-side collision
 * and drives it through walls. Most servers will rubber-band you
 * back; works fully on vanilla/lax servers.
 */
public final class BoatPhaseHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting speed =
                new SliderSetting("Speed", 0.5, 0.1, 1.5, 0.1);

        private EntityBoat phasedBoat;

        public BoatPhaseHack()
        {
                super("BoatPhase",
                        "Lets you drive a boat through walls."
                                + " May rubber-band on strict servers.");
                setCategory(Category.MOVEMENT);
                addSetting(speed);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || !p.isRiding())
                        return;

                Entity riding = p.getRidingEntity();
                if(!(riding instanceof EntityBoat))
                        return;

                EntityBoat boat = (EntityBoat)riding;
                phasedBoat = boat;
                boat.noClip = true;

                float forward = 0;
                if(mc.gameSettings.keyBindForward.isKeyDown())
                        forward += 1;
                if(mc.gameSettings.keyBindBack.isKeyDown())
                        forward -= 1;

                if(forward != 0)
                {
                        float s = speed.getValueF() * forward;
                        float yaw = p.rotationYaw;
                        float sin = MathHelper.sin(yaw * 0.017453292F);
                        float cos = MathHelper.cos(yaw * 0.017453292F);
                        boat.motionX = s * -sin;
                        boat.motionZ = s * cos;
                }
                else
                {
                        boat.motionX = 0;
                        boat.motionZ = 0;
                }

                if(mc.gameSettings.keyBindJump.isKeyDown())
                        boat.motionY = 0.3;
                else if(mc.gameSettings.keyBindSneak.isKeyDown())
                        boat.motionY = -0.3;
                else
                        boat.motionY *= 0.6;
        }

        @Override
        protected void onDisable()
        {
                if(phasedBoat != null)
                        phasedBoat.noClip = false;
                phasedBoat = null;
        }
}
