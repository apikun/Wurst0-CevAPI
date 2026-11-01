/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.clickgui;

import java.io.IOException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiScreen;
import net.wurst0.WurstClient;

public final class ClickGuiScreen extends GuiScreen
{
        private final ClickGuiManager manager;

        public ClickGuiScreen()
        {
                manager = WurstClient.INSTANCE.getClickGuiManager();
        }

        @Override
        public void initGui()
        {
                // also runs on window resize while the GUI is open
                manager.setScreenSize(width, height);
        }

        @Override
        public void onGuiClosed()
        {
                // remember window positions across restarts
                manager.saveLayout();
                super.onGuiClosed();
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks)
        {
                drawDefaultBackground();
                for(ClickGuiWindow window : manager.getWindows())
                        window.render(mouseX, mouseY);
                super.drawScreen(mouseX, mouseY, partialTicks);

                // "Keybinds" button, top-left corner
                String label = "Keybinds";
                int bw = mc.fontRenderer.getStringWidth(label) + 8;
                boolean hover = mouseX >= 4 && mouseX < 4 + bw
                        && mouseY >= 4 && mouseY < 18;
                drawRect(4, 4, 4 + bw, 18, hover ? 0xFFA03232 : 0xF00A0A0A);
                drawRect(4, 4, 4 + bw, 5, 0xFFA03232);
                drawRect(4, 17, 4 + bw, 18, 0xFFA03232);
                drawRect(4, 4, 5, 18, 0xFFA03232);
                drawRect(3 + bw, 4, 4 + bw, 18, 0xFFA03232);
                mc.fontRenderer.drawStringWithShadow(label, 8, 8,
                        hover ? 0xFFFFFFFF : 0xFFBDBDBD);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
                throws IOException
        {
                // "Keybinds" button opens the keybind editor
                String label = "Keybinds";
                int bw = mc.fontRenderer.getStringWidth(label) + 8;
                if(mouseX >= 4 && mouseX < 4 + bw && mouseY >= 4
                        && mouseY < 18)
                {
                        mc.displayGuiScreen(new KeybindsScreen());
                        return;
                }

                // only the top-most window under the mouse gets the click,
                // so overlapping windows no longer both react to it
                boolean hit = false;
                for(int i = manager.getWindows().size() - 1; i >= 0; i--)
                {
                        ClickGuiWindow window = manager.getWindows().get(i);
                        if(window.contains(mouseX, mouseY))
                        {
                                manager.bringToFront(window);
                                window.mouseClicked(mouseX, mouseY,
                                        mouseButton);
                                hit = true;
                                break;
                        }
                }

                // clicking empty space stops text editing
                if(!hit)
                        manager.unfocusAllText();

                if(!hit)
                        super.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        protected void mouseReleased(int mouseX, int mouseY, int state)
        {
                for(ClickGuiWindow window : manager.getWindows())
                        window.mouseReleased(mouseX, mouseY, state);
                super.mouseReleased(mouseX, mouseY, state);
        }

        @Override
        protected void mouseClickMove(int mouseX, int mouseY,
                int clickedMouseButton, long timeSinceLastClick)
        {
                for(ClickGuiWindow window : manager.getWindows())
                        window.mouseClickMove(mouseX, mouseY,
                                clickedMouseButton, timeSinceLastClick);
                super.mouseClickMove(mouseX, mouseY, clickedMouseButton,
                        timeSinceLastClick);
        }

        @Override
        public void handleMouseInput() throws IOException
        {
                super.handleMouseInput();

                int delta = Mouse.getEventDWheel();
                if(delta == 0)
                        return;

                int mouseX = Mouse.getEventX() * width / mc.displayWidth;
                int mouseY = height - Mouse.getEventY() * height
                        / mc.displayHeight - 1;

                for(int i = manager.getWindows().size() - 1; i >= 0; i--)
                {
                        ClickGuiWindow window = manager.getWindows().get(i);
                        if(window.mouseWheel(mouseX, mouseY, delta))
                                break;
                }
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) throws IOException
        {
                // a focused text field eats keys first (Esc while editing
                // just stops editing instead of closing the whole GUI)
                if(manager.keyTyped(typedChar, keyCode))
                        return;

                if(keyCode == Keyboard.KEY_ESCAPE
                        || keyCode == Keyboard.KEY_RSHIFT)
                {
                        mc.displayGuiScreen(null);
                        return;
                }
                super.keyTyped(typedChar, keyCode);
        }

        @Override
        public boolean doesGuiPauseGame()
        {
                return false;
        }
}
