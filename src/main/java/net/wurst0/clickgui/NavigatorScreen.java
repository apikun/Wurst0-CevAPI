/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 * New file: searchable hack list screen for NavigatorHack.
 * Standalone GuiScreen, no changes to existing classes.
 */
package net.wurst0.clickgui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.wurst0.WurstClient;
import net.wurst0.feature.Hack;
import net.wurst0.hacks.TooManyHaxHack;

public final class NavigatorScreen extends GuiScreen
{
        private static final int ROW_HEIGHT = 14;

        private final boolean backgroundOverlay;
        private final List<Hack> results = new ArrayList<Hack>();
        private final List<int[]> rowRects = new ArrayList<int[]>();

        private GuiTextField searchBox;
        private int scrollOffset;

        public NavigatorScreen(boolean backgroundOverlay)
        {
                this.backgroundOverlay = backgroundOverlay;
        }

        @Override
        public void initGui()
        {
                searchBox = new GuiTextField(0, fontRenderer,
                        width / 2 - 100, 22, 200, 16);
                searchBox.setFocused(true);
                searchBox.setCanLoseFocus(false);
                searchBox.setMaxStringLength(64);
                refreshResults();
        }

        private void refreshResults()
        {
                results.clear();
                scrollOffset = 0;

                String query = searchBox.getText().trim().toLowerCase();
                TooManyHaxHack tooManyHax =
                        (TooManyHaxHack)WurstClient.INSTANCE.getHacks()
                                .getByName("TooManyHax");
                for(Hack hack : WurstClient.INSTANCE.getHacks().getList())
                {
                        if(tooManyHax != null
                                && tooManyHax.isHidden(hack.getName()))
                                continue;
                        if(!query.isEmpty()
                                && !hack.getName().toLowerCase()
                                        .contains(query)
                                && !hack.getDescription().toLowerCase()
                                        .contains(query))
                                continue;
                        results.add(hack);
                }
        }

        @Override
        public void drawScreen(int mouseX, int mouseY,
                float partialTicks)
        {
                if(backgroundOverlay)
                        drawDefaultBackground();
                else
                        drawRect(0, 0, width, height, 0x90501E42);

                drawCenteredString(fontRenderer, "Navigator", width / 2,
                        6, 0xFFFFFFFF);

                searchBox.drawTextBox();

                rowRects.clear();
                int listTop = 46;
                int rowsVisible = Math.max(1,
                        (height - listTop - 20) / ROW_HEIGHT);
                int maxScroll =
                        Math.max(0, results.size() - rowsVisible);
                if(scrollOffset > maxScroll)
                        scrollOffset = maxScroll;

                for(int i = scrollOffset; i < results.size(); i++)
                {
                        int y = listTop + (i - scrollOffset) * ROW_HEIGHT;
                        if(y + ROW_HEIGHT > height - 20)
                                break;

                        Hack hack = results.get(i);
                        boolean enabled = hack.isEnabled();
                        boolean hover = mouseX >= 4 && mouseX <= width - 4
                                && mouseY >= y
                                && mouseY < y + ROW_HEIGHT;

                        int rowBg = enabled ? 0x6000B04F
                                : (hover ? 0x50FFFFFF : 0x30181820);
                        drawRect(4, y, width - 4, y + ROW_HEIGHT - 1,
                                rowBg);

                        fontRenderer.drawStringWithShadow(hack.getName(),
                                8, y + 3, enabled ? 0xFF80FF80
                                        : 0xFFE0E0E0);

                        String desc = hack.getDescription();
                        int descX = width / 2 + 8;
                        int maxDescWidth = width - descX - 8;
                        if(fontRenderer.getStringWidth(desc)
                                > maxDescWidth)
                        {
                                while(desc.length() > 3
                                        && fontRenderer.getStringWidth(
                                                desc + "...")
                                                        > maxDescWidth)
                                        desc = desc.substring(0,
                                                desc.length() - 1);
                                desc = desc + "...";
                        }
                        fontRenderer.drawStringWithShadow(desc, descX,
                                y + 3, 0xFF97A2B5);

                        rowRects.add(new int[]{y, i});
                }

                String footer = results.size() + " result(s)  |  "
                        + "type to search  |  click = toggle  |  "
                        + "enter = first result  |  esc = close";
                fontRenderer.drawStringWithShadow(footer, 6, height - 12,
                        0xFF97A2B5);

                super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY,
                int mouseButton) throws IOException
        {
                searchBox.mouseClicked(mouseX, mouseY, mouseButton);

                for(int[] row : rowRects)
                {
                        if(mouseY >= row[0]
                                && mouseY < row[0] + ROW_HEIGHT)
                        {
                                Hack hack = results.get(row[1]);
                                if(hack != null)
                                        hack.toggle();
                                return;
                        }
                }

                super.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        public void handleMouseInput() throws IOException
        {
                super.handleMouseInput();

                int delta = Mouse.getEventDWheel();
                if(delta == 0)
                        return;

                if(delta > 0)
                        scrollOffset = Math.max(0, scrollOffset - 3);
                else
                        scrollOffset = Math.max(0, scrollOffset + 3);
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode)
                throws IOException
        {
                if(keyCode == Keyboard.KEY_ESCAPE)
                {
                        mc.displayGuiScreen(null);
                        return;
                }

                if(searchBox.textboxKeyTyped(typedChar, keyCode))
                {
                        refreshResults();
                        return;
                }

                if(keyCode == Keyboard.KEY_RETURN && !results.isEmpty())
                {
                        results.get(0).toggle();
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
