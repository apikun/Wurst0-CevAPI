/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TimerHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class TimerHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting speed =
                new SliderSetting("Speed", 1, 0.1, 10, 0.05);

        private Timer cachedTimer;

        public TimerHack()
        {
                super("Timer",
                        "Changes how fast the game ticks (slows down or speeds up"
                                + " the whole game).");
                setCategory(Category.TOOLS);
                addSetting(speed);
        }

        private Timer getTimer()
        {
                if(cachedTimer == null)
                        // field_71428_T = Minecraft.timer (SRG); "timer" = MCP name.
                        // NEVER use field_71467_ac here: that SRG id belongs to
                        // rightClickDelayTimer (an int), not the Timer instance.
                        cachedTimer = ReflectionHelper.getPrivateValue(
                                Minecraft.class, mc, "field_71428_T", "timer");
                return cachedTimer;
        }

        @Override
        public void onUpdate()
        {
                Timer timer = getTimer();
                if(timer == null)
                        return;
                float f = speed.getValueF();
                if(grimCap)
                        f = Math.min(f, 1.01F); // GrimAC timer tolerance
                ReflectionHelper.setPrivateValue(Timer.class, timer,
                        50F / f, "field_74281_c", "tickLength");
        }

        /** Set by GrimBypassHack to clamp timer speed into Grim tolerance. */
        public static boolean grimCap;

        @Override
        protected void onDisable()
        {
                Timer timer = getTimer();
                if(timer == null)
                        return;
                ReflectionHelper.setPrivateValue(Timer.class, timer, 50F,
                        "field_74281_c", "tickLength");
        }
}
