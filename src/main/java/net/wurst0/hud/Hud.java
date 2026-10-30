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

                FontRenderer fr = mc.fontRenderer;

                // watermark, bottom-left
                String watermark = "Wurst0 " + WurstClient.VERSION
                        + " (MC " + WurstClient.MC_VERSION + ")";
                fr.drawStringWithShadow(watermark, 2,
                        new ScaledResolution(mc).getScaledHeight()
                                - fr.FONT_HEIGHT - 1, 0x20FF20);

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
                                return fr.getStringWidth(o2) - fr.getStringWidth(o1);
                        }
                });

                int y = 2;
                for(String name : enabled)
                {
                        int x = new ScaledResolution(mc).getScaledWidth()
                                - fr.getStringWidth(name) - 2;
                        fr.drawStringWithShadow(name, x, y, 0xF0F0F0);
                        y += fr.FONT_HEIGHT + 1;
                }
        }
}
