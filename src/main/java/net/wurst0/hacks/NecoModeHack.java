/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NecoModeHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.wurst0.Category;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * Draws a pair of cat ears on the top edge of your screen.
 * Purely cosmetic.
 */
public final class NecoModeHack extends Hack implements ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting pink =
                new CheckboxSetting("Pink", true);

        public NecoModeHack()
        {
                super("NecoMode",
                        "Puts cat ears on your screen. Nya~");
                setCategory(Category.FUN);
                addSetting(pink);
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(mc.player == null)
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                int w = sr.getScaledWidth();
                int h = sr.getScaledHeight();

                int earH = Math.max(8, Math.min(20, h / 22));
                int earW = earH * 2;
                int margin = Math.max(2, w / 64);

                int inner = pink.isChecked() ? 0xFFEC7FB4 : 0xFFDDDDDD;
                int border = 0xFF3A3A3A;

                drawEar(2 + margin, 0, earW, earH, inner, border);
                drawEar(w - earW - 2 - margin, 0, earW, earH, inner,
                        border);
        }

        private void drawEar(int x, int y, int w, int h, int fill,
                int border)
        {
                // outline (stepped triangle, 2px steps)
                for(int row = 0; row < h; row += 2)
                {
                        int inset = (row * (w / 2 - 2)) / h;
                        int left = x + inset;
                        int right = x + w - inset;
                        Gui.drawRect(left - 2, y + row, right + 2,
                                y + row + 2, border);
                }
                // fill
                for(int row = 0; row < h; row += 2)
                {
                        int inset = (row * (w / 2 - 2)) / h + 2;
                        int left = x + inset;
                        int right = x + w - inset;
                        if(right > left)
                                Gui.drawRect(left, y + row, right,
                                        y + row + 2, fill);
                }
        }
}
