/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.wurst0.WurstClient;
import net.wurst0.feature.Hack;

public final class Hud
{
        private final Minecraft mc = Minecraft.getMinecraft();

        public void render(float partialTicks)
        {
                if(mc.player == null || mc.gameSettings.showDebugInfo)
                        return;

                // HideWurst makes the HUD invisible (screenshots etc.)
                Hack hideWurst = WurstClient.INSTANCE.getHacks()
                        .getByName("HideWurst");
                if(hideWurst != null && hideWurst.isEnabled())
                        return;

                FontRenderer fr = mc.fontRenderer;
                ScaledResolution sr = new ScaledResolution(mc);

                // watermark, bottom-left (rainbow when RainbowUi is on)
                int wmColor = 0x20FF20;
                Hack rainbowUi = WurstClient.INSTANCE.getHacks()
                        .getByName("RainbowUi");
                if(rainbowUi != null && rainbowUi.isEnabled())
                        wmColor = java.awt.Color.HSBtoRGB(
                                (System.currentTimeMillis() % 4000) / 4000F,
                                0.65F, 1F);

                String watermark = "Wurst0 " + WurstClient.VERSION
                        + " (MC " + WurstClient.MC_VERSION + ")";
                fr.drawStringWithShadow(watermark, 2,
                        sr.getScaledHeight() - fr.FONT_HEIGHT - 1, wmColor);

                // array of enabled hacks, top-right
                List<String> enabled = new ArrayList<String>();
                for(Hack hack : WurstClient.INSTANCE.getHacks().getList())
                        if(hack.isEnabled())
                                enabled.add(hack.getName());

                Collections.sort(enabled, new Comparator<String>()
                {
                        @Override
                        public int compare(String o1, String o2)
                        {
                                return fr.getStringWidth(o2)
                                        - fr.getStringWidth(o1);
                        }
                });

                // on small screens / high GUI scales the list is capped so
                // it can never run past the bottom of the screen
                int lineHeight = fr.FONT_HEIGHT + 1;
                int maxLines =
                        Math.max(1, (sr.getScaledHeight() - 16) / lineHeight);
                int overflow = enabled.size() - maxLines;

                int y = 2;
                int shown = overflow > 0 ? maxLines - 1 : enabled.size();
                for(int i = 0; i < shown; i++)
                {
                        String name = enabled.get(i);
                        int x = sr.getScaledWidth() - fr.getStringWidth(name)
                                - 2;
                        fr.drawStringWithShadow(name, x, y, 0xF0F0F0);
                        y += lineHeight;
                }

                if(overflow > 0)
                {
                        String more = "+" + (overflow + 1) + " more";
                        int x = sr.getScaledWidth() - fr.getStringWidth(more)
                                - 2;
                        fr.drawStringWithShadow(more, x, y, 0x909090);
                }
        }
}
