/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ElytraBounceHack (GPL-3.0-or-later)
 * Lite adaptation: bounces you off the ground before the vanilla
 * landing flag kills the glide.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.RayTraceResult;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class ElytraBounceHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting strength =
                new SliderSetting("Strength", 0.8, 0.3, 1.5, 0.1);
        private final SliderSetting triggerHeight =
                new SliderSetting("Trigger height", 1.5, 0.5, 4, 0.5);

        private boolean wasFlying;

        public ElytraBounceHack()
        {
                super("ElytraBounce",
                        "Bounces you off the ground while elytra flying"
                                + " instead of landing.");
                setCategory(Category.MOVEMENT);
                addSettings(strength, triggerHeight);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                boolean flying = p.isElytraFlying();

                if(flying)
                {
                        // pre-bounce shortly before touching the ground
                        RayTraceResult trace = mc.world.rayTraceBlocks(
                                p.getPositionVector(),
                                p.getPositionVector().add(0,
                                        -triggerHeight.getValue(), 0));
                        if(trace != null
                                && trace.typeOfHit
                                        == RayTraceResult.Type.BLOCK
                                && p.motionY < -0.5)
                        {
                                p.motionY = strength.getValue();
                                p.fallDistance = 0;
                        }
                }
                else if(wasFlying && p.motionY < 0)
                {
                        // just left glide state: jump back into it
                        p.motionY = strength.getValue();
                        p.fallDistance = 0;
                }

                wasFlying = flying;
        }

        @Override
        protected void onDisable()
        {
                wasFlying = false;
        }
}
