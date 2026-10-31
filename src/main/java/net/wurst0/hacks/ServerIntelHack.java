/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ServerIntelHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.wurst0.Category;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.feature.Hack;

/**
 * Shows intel about the current server on screen: brand, IP,
 * dimension and difficulty. Lite port (the original also hooks into
 * dupe-db APIs, which don't apply to 1.12.2).
 */
public final class ServerIntelHack extends Hack
        implements ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        public ServerIntelHack()
        {
                super("ServerIntel",
                        "Shows server brand, IP, dimension and difficulty on"
                                + " screen.");
                setCategory(Category.INTEL);
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(mc.player == null || mc.isGamePaused())
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                int y = 2;
                int x = sr.getScaledWidth() / 2
                        - mc.fontRenderer.getStringWidth("ServerIntel") / 2;

                String brand = mc.player.getServerBrand();
                String ip = mc.getCurrentServerData() == null ? "singleplayer"
                        : mc.getCurrentServerData().serverIP;
                String dim = dimensionName(mc.player.dimension);
                String diff = mc.world.getDifficulty() == null ? "?"
                        : mc.world.getDifficulty().name();

                draw(x, y, "ServerIntel", 0xFF00FFFF);
                y += 10;
                draw(x, y, "Brand: " + (brand == null ? "?" : brand), 0xFFFFFFFF);
                y += 10;
                draw(x, y, "IP: " + ip, 0xFFFFFFFF);
                y += 10;
                draw(x, y, "Dimension: " + dim, 0xFFFFFFFF);
                y += 10;
                draw(x, y, "Difficulty: " + diff, 0xFFFFFFFF);
        }

        private void draw(int x, int y, String text, int color)
        {
                int w = mc.fontRenderer.getStringWidth(text);
                int cx = mc.currentScreen == null
                        ? x : Math.max(2, Math.min(x, mc.currentScreen.width - w - 2));
                Gui.drawRect(cx - 1, y - 1, cx + w + 1, y + 9, 0x80000000);
                mc.fontRenderer.drawStringWithShadow(text, cx, y, color);
        }

        private static String dimensionName(int dim)
        {
                if(dim == -1)
                        return "Nether";
                if(dim == 1)
                        return "End";
                return "Overworld";
        }
}
