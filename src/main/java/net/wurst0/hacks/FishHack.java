/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FishHack (GPL-3.0-or-later)
 *
 * NOTE: the original "Fish" is not a fishing bot - it is a swim
 * assist that pushes you upward while in water (SearchTags AutoSwim).
 * The 1.12.2 port keeps that exact behavior; no bobber/fishhook
 * polling is involved (see AutoFishHack for actual fishing).
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.WurstClient;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

public final class FishHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        public FishHack()
        {
                super("Fish",
                        "Swim up in water like a fish. Only works while"
                                + " you are in water and not sneaking.");
                setCategory(Category.MOVEMENT);
        }

        @Override
        protected void onEnable()
        {
                // like the original: avoid fighting the dolphin swim boost
                Hack dolphin =
                        WurstClient.INSTANCE.getHacks().getByName("Dolphin");
                if(dolphin != null)
                        dolphin.setEnabled(false);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null)
                        return;

                if(!p.isInWater() || p.isSneaking())
                        return;

                p.motionY += 0.005;
        }
}
