/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 *
 * KeybindsScreen: GUI keybind editor. Lists every hack with its current
 * bind; click a row, press the key you want (Escape clears the bind).
 * Changes are saved to config/wurst0/keybinds.json immediately.
 * Opened from the "Keybinds" button in the ClickGUI or via ".binds gui".
 */
package net.wurst0.clickgui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.wurst0.WurstClient;
import net.wurst0.feature.Hack;

public final class KeybindsScreen extends GuiScreen
{
        // same dark/crimson palette as the ClickGUI
        private static final int PANEL_BG = 0xF00A0A0A;
        private static final int HEADER_BG = 0xFFA03232;
        private static final int BORDER = 0xFFA03232;
        private static final int ENABLED_COLOR = 0xFFC33B3B;
        private static final int DISABLED_COLOR = 0xFFBDBDBD;
        private static final int ROW_H = 14;

        private final List<Hack> hacks = new ArrayList<Hack>();
        private Hack bindingHack;
        private int scrollY;
        private int panelY;
        private int panelH;
        private int panelW;

        public KeybindsScreen()
        {
                for(Hack hack : WurstClient.INSTANCE.getHacks().getList())
                        hacks.add(hack);
        }

        @Override
        public void initGui()
        {
                ScaledResolution sr = new ScaledResolution(mc);
                panelW = Math.min(220, sr.getScaledWidth() - 16);
                panelH = Math.min(hacks.size() * ROW_H + 42,
                        sr.getScaledHeight() - 16);
                panelY = (sr.getScaledHeight() - panelH) / 2;
                scrollY = 0;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks)
        {
                drawDefaultBackground();
                drawRect(width / 2 - panelW / 2 - 1, panelY - 1,
                        width / 2 + panelW / 2 + 1, panelY + panelH + 1,
                        BORDER);
                drawRect(width / 2 - panelW / 2, panelY,
                        width / 2 + panelW / 2, panelY + panelH, PANEL_BG);
                drawRect(width / 2 - panelW / 2, panelY,
                        width / 2 + panelW / 2, panelY + 14, HEADER_BG);

                String title = bindingHack == null ? "Keybinds"
                        : "Press a key for " + bindingHack.getName()
                                + " (ESC = clear)";
                drawCenteredString(mc.fontRenderer, title, width / 2,
                        panelY + 4, 0xFFFFFFFF);

                int listTop = panelY + 16;
                int listBottom = panelY + panelH - 16;
                int rowW = panelW - 4;
                int rowX = width / 2 - panelW / 2 + 2;
                int y = listTop - scrollY;

                for(Hack hack : hacks)
                {
                        if(y + ROW_H > listTop && y < listBottom)
                        {
                                boolean hover = mouseX >= rowX
                                        && mouseX < rowX + rowW
                                        && mouseY >= y && mouseY < y + ROW_H
                                        && bindingHack == null;
                                if(hover)
                                        drawRect(rowX, y, rowX + rowW,
                                                y + ROW_H, 0x28C33B3B);

                                String key = hack.getKeybind() == -1
                                        ? "none" : Keyboard.getKeyName(
                                                hack.getKeybind());
                                mc.fontRenderer.drawStringWithShadow(
                                        hack.getName(), rowX + 3, y + 3,
                                        hack.isEnabled() ? ENABLED_COLOR
                                                : DISABLED_COLOR);
                                mc.fontRenderer.drawStringWithShadow(key,
                                        rowX + rowW
                                                - mc.fontRenderer
                                                        .getStringWidth(key)
                                                - 3,
                                        y + 3, bindingHack == hack
                                                ? 0xFFFFFF00 : 0xFF909090);
                        }
                        y += ROW_H;
                }

                // Done button
                String done = "Done";
                int dw = mc.fontRenderer.getStringWidth(done) + 10;
                int dx = width / 2 - dw / 2;
                int dy = panelY + panelH - 14;
                boolean dHover = mouseX >= dx && mouseX < dx + dw
                        && mouseY >= dy && mouseY < dy + 12;
                drawRect(dx, dy, dx + dw, dy + 12,
                        dHover ? 0xFFA03232 : 0xF00A0A0A);
                drawCenteredString(mc.fontRenderer, done, width / 2, dy + 2,
                        0xFFFFFFFF);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
                throws IOException
        {
                if(mouseButton != 0)
                        return;

                // Done button
                String done = "Done";
                int dw = mc.fontRenderer.getStringWidth(done) + 10;
                int dx = width / 2 - dw / 2;
                int dy = panelY + panelH - 14;
                if(mouseX >= dx && mouseX < dx + dw && mouseY >= dy
                        && mouseY < dy + 12)
                {
                        save();
                        mc.displayGuiScreen(new ClickGuiScreen());
                        return;
                }

                if(bindingHack != null)
                        return; // waiting for a key press

                int listTop = panelY + 16;
                int listBottom = panelY + panelH - 16;
                int rowW = panelW - 4;
                int rowX = width / 2 - panelW / 2 + 2;
                if(mouseX < rowX || mouseX >= rowX + rowW)
                        return;
                if(mouseY < listTop || mouseY >= listBottom)
                        return;

                int index = (mouseY - listTop + scrollY) / ROW_H;
                if(index >= 0 && index < hacks.size())
                        bindingHack = hacks.get(index);
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) throws IOException
        {
                if(bindingHack != null)
                {
                        // Escape clears the bind, any other key binds it
                        bindingHack.setKeybind(
                                keyCode == Keyboard.KEY_ESCAPE ? -1 : keyCode);
                        bindingHack = null;
                        save();
                        return;
                }

                if(keyCode == Keyboard.KEY_ESCAPE
                        || keyCode == Keyboard.KEY_RSHIFT)
                {
                        save();
                        mc.displayGuiScreen(null);
                        return;
                }
                super.keyTyped(typedChar, keyCode);
        }

        @Override
        public void handleMouseInput() throws IOException
        {
                super.handleMouseInput();

                int delta = Mouse.getEventDWheel();
                if(delta == 0)
                        return;

                int contentH = hacks.size() * ROW_H;
                int viewH = panelH - 32;
                scrollY = Math.max(0, Math.min(scrollY
                        - (delta > 0 ? 15 : -15), contentH - viewH));
                if(contentH <= viewH)
                        scrollY = 0;
        }

        private void save()
        {
                WurstClient.INSTANCE.getFileManager()
                        .saveKeybinds(
                                WurstClient.INSTANCE.getHacks());
        }

        @Override
        public boolean doesGuiPauseGame()
        {
                return false;
        }
}
