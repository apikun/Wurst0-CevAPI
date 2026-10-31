/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoClickerHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.ReflectCalls;

public final class AutoClickerHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final ModeSetting mode = new ModeSetting("Mode",
                new String[]{"Left", "Right"}, 0);
        private final SliderSetting cps =
                new SliderSetting("CPS", 8, 1, 20, 1);

        private long nextClick;

        public AutoClickerHack()
        {
                super("AutoClicker",
                        "Automatically clicks your mouse while enabled.");
                setCategory(Category.OTHER);
                addSettings(mode, cps);
        }

        @Override
        public void onUpdate()
        {
                if(mc.player == null || mc.currentScreen != null)
                        return;

                long now = System.currentTimeMillis();
                long interval = 1000L / cps.getValueI();
                if(now < nextClick)
                        return;
                nextClick = now + interval;

                if("Left".equals(mode.getSelected()))
                        ReflectCalls.leftClickMouse();
                else
                        ReflectCalls.rightClickMouse();
        }
}
