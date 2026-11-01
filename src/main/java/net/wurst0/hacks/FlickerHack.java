/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FlickerHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Randomly strobes your gamma setting. Harmless fun (and a great
 * way to annoy everyone watching your stream).
 */
public final class FlickerHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();
        private final Random rand = new Random();

        private final SliderSetting speed =
                new SliderSetting("Speed", 5, 1, 20, 1);

        private int timer;
        private float savedGamma = -1;

        public FlickerHack()
        {
                super("Flicker",
                        "Randomly flickers your screen brightness."
                                + " Photosensitive players: do not use.");
                setCategory(Category.FUN);
                addSetting(speed);
        }

        @Override
        public void onUpdate()
        {
            if(--timer > 0)
                    return;
            timer = speed.getValueI();

            // mostly dark, occasional bright flash
            if(rand.nextInt(6) == 0)
                    mc.gameSettings.gammaSetting =
                            20F + rand.nextFloat() * 80F;
            else
                    mc.gameSettings.gammaSetting = rand.nextFloat() * 1.5F;
        }

        @Override
        protected void onEnable()
        {
                savedGamma = mc.gameSettings.gammaSetting;
                timer = 0;
        }

        @Override
        protected void onDisable()
        {
                if(savedGamma >= 0)
                        mc.gameSettings.gammaSetting = savedGamma;
                savedGamma = -1;
        }
}
