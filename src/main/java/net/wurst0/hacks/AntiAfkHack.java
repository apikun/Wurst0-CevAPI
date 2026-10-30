/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiAfkHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumHand;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class AntiAfkHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting interval =
                new SliderSetting("Interval (seconds)", 15, 5, 120, 5);

        private int timer;

        public AntiAfkHack()
        {
                super("AntiAfk",
                        "Makes small movements and swings your arm at regular"
                                + " intervals so servers don't kick you for being AFK.");
                setCategory(Category.OTHER);
                addSetting(interval);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(timer-- > 0)
                        return;

                timer = interval.getValueI() * 20;

                // small head movement + a swing is enough for most AFK checks
                p.rotationYaw += (float)((Math.random() - 0.5) * 4);
                p.rotationPitch =
                        (float)((Math.random() - 0.5) * 2);
                p.swingArm(EnumHand.MAIN_HAND);
        }
}
