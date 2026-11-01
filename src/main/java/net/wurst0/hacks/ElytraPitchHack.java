/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ElytraPitchHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Locks or clamps your pitch while elytra flying, so you can
 * glide in a straight line without accidentally nose-diving.
 */
public final class ElytraPitchHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final ModeSetting mode = new ModeSetting("Mode",
                new String[]{"Lock", "Clamp"}, 0);
        private final SliderSetting pitch =
                new SliderSetting("Pitch", 0, -90, 90, 5);
        private final SliderSetting clampRange =
                new SliderSetting("Clamp range", 30, 5, 90, 5);

        public ElytraPitchHack()
        {
                super("ElytraPitch",
                        "Locks your pitch while flying with an elytra.");
                setCategory(Category.MOVEMENT);
                addSettings(mode, pitch, clampRange);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || !p.isElytraFlying())
                        return;

                float target = (float)pitch.getValue();
                if(mode.getSelected().equals("Lock"))
                        p.rotationPitch = target;
                else
                {
                        float range = (float)clampRange.getValue();
                        if(p.rotationPitch > target + range)
                                p.rotationPitch = target + range;
                        if(p.rotationPitch < target - range)
                                p.rotationPitch = target - range;
                }
        }
}
