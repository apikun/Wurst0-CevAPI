/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI XpGuiHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.wurst0.Category;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Shows your exact XP stats (level, progress, total points) as a
 * small HUD line, useful because the vanilla bar is hard to read.
 */
public final class XpGuiHack extends Hack implements ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting opacity =
                new SliderSetting("Opacity", 70, 20, 100, 5);
        private final CheckboxSetting showTotal =
                new CheckboxSetting("Show total", true);

        public XpGuiHack()
        {
                super("XpGui",
                        "Displays your XP level, progress percentage and total"
                                + " experience points on the HUD.");
                setCategory(Category.RENDER);
                addSettings(opacity, showTotal);
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                EntityPlayerSP p = mc.player;
                if(p == null)
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                int alpha = Math.min(0xFF, opacity.getValueI() * 0xFF / 100)
                        << 24;
                int color = 0x00FFA0 | alpha;

                String text = "Lvl " + p.experienceLevel
                        + " (" + (int)(p.experience * 100) + "%)";
                if(showTotal.isChecked())
                        text += " - " + p.experienceTotal + " XP";

                mc.fontRenderer.drawStringWithShadow(text,
                        2, sr.getScaledHeight() - 22, color);
        }
}
