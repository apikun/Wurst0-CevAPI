/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ElytraDiveHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * While elytra flying: gradually points you straight down and adds
 * forward thrust, up to a configurable maximum speed.
 */
public final class ElytraDiveHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting boost =
                new SliderSetting("Boost per tick", 0.08, 0.01, 0.3, 0.01);
        private final SliderSetting maxSpeed =
                new SliderSetting("Max speed", 2.5, 0.5, 5, 0.1);

        public ElytraDiveHack()
        {
                super("ElytraDive",
                        "Dives straight down while flying and keeps boosting"
                                + " up to the max speed.");
                setCategory(Category.MOVEMENT);
                addSettings(boost, maxSpeed);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || !p.isElytraFlying())
                        return;

                if(p.rotationPitch < 88F)
                {
                        p.rotationPitch +=
                                Math.min(6F, 88F - p.rotationPitch);
                }

                Vec3d look = p.getLookVec();
                double b = boost.getValue();
                p.motionX += look.x * b;
                p.motionY += look.y * b;
                p.motionZ += look.z * b;

                double speed = Math.sqrt(p.motionX * p.motionX
                        + p.motionY * p.motionY + p.motionZ * p.motionZ);
                double max = maxSpeed.getValue();
                if(speed > max)
                {
                        p.motionX = p.motionX / speed * max;
                        p.motionY = p.motionY / speed * max;
                        p.motionZ = p.motionZ / speed * max;
                }
        }
}
