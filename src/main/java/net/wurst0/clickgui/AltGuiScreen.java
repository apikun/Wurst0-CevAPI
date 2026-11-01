/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 * New file: minimal list-style alternate GUI for AltGuiHack
 * (Meteor-inspired dark theme). Standalone GuiScreen, no changes to
 * existing classes.
 */
package net.wurst0.clickgui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiScreen;
import net.wurst0.Category;
import net.wurst0.WurstClient;
import net.wurst0.feature.Hack;
import net.wurst0.hacks.TooManyHaxHack;

public final class AltGuiScreen extends GuiScreen
{
        private static final int ROW_HEIGHT = 14;
        private static final int TAB_HEIGHT = 12;

        private final List<String> categoryNames =
                new ArrayList<String>();
        private final List<int[]> tabRects =
                new ArrayList<int[]>();
        private final List<int[]> rowRects =
                new ArrayList<int[]>();
        private final List<Hack> visibleHacks =
                new ArrayList<Hack>();

        private int selectedCategory;
        private int scrollOffset;

        public AltGuiScreen()
        {
                categoryNames.add("All");
                for(Category category : Category.values())
                {
                        if(category == Category.FAVORITES
                                || category == Category.COMMANDS)
                                continue;
                        categoryNames.add(category.getName());
                }
        }

        @Override
        public void initGui()
        {
                refreshList();
        }

        private void refreshList()
        {
                visibleHacks.clear();
                String filter = categoryNames.get(selectedCategory);
                TooManyHaxHack tooManyHax =
                        (TooManyHaxHack)WurstClient.INSTANCE.getHacks()
                                .getByName("TooManyHax");
                for(Hack hack : WurstClient.INSTANCE.getHacks().getList())
                {
                        if(!filter.equals("All") && !hack.getCategory()
                                .getName().equals(filter))
                                continue;
                        if(tooManyHax != null
                                && tooManyHax.isHidden(hack.getName()))
                                continue;
                        visibleHacks.add(hack);
                }
        }

        @Override
        public void drawScreen(int mouseX, int mouseY,
                float partialTicks)
        {
                // dark theme background (Meteor-style)
                drawRect(0, 0, width, height, 0xE00D0F13);
                drawCenteredString(fontRenderer, "AltGui", width / 2, 5,
                        0xE9EDF5);

                // category tabs
                tabRects.clear();
                int x = 6;
                int tabY = 17;
                for(int i = 0; i < categoryNames.size(); i++)
                {
                        String name = categoryNames.get(i);
                        int w = fontRenderer.getStringWidth(name) + 8;
                        if(x + w > width - 4)
                                break;
                        boolean selected = i == selectedCategory;
                        boolean hover = mouseX >= x && mouseX < x + w
                                && mouseY >= tabY
                                && mouseY < tabY + TAB_HEIGHT;
                        int bg = selected ? 0xFF8100DD
                                : (hover ? 0xFF242D3C : 0xFF151A22);
                        drawRect(x, tabY, x + w, tabY + TAB_HEIGHT, bg);
                        fontRenderer.drawStringWithShadow(name, x + 4,
                                tabY + 2,
                                selected ? 0xFFFFFFFF : 0xFF97A2B5);
                        tabRects.add(new int[]{x, tabY, x + w,
                                tabY + TAB_HEIGHT, i});
                        x += w + 3;
                }

                // hack rows
                rowRects.clear();
                int listTop = tabY + TAB_HEIGHT + 5;
                int rowsVisible =
                        Math.max(1, (height - listTop - 16) / ROW_HEIGHT);
                int maxScroll = Math.max(0,
                        visibleHacks.size() - rowsVisible);
                if(scrollOffset > maxScroll)
                        scrollOffset = maxScroll;

                int y = listTop - scrollOffset * ROW_HEIGHT;
                for(int i = 0; i < visibleHacks.size(); i++)
                {
                        if(y + ROW_HEIGHT < listTop)
                        {
                                y += ROW_HEIGHT;
                                continue;
                        }
                        if(y > height - 16)
                                break;

                        Hack hack = visibleHacks.get(i);
                        boolean enabled = hack.isEnabled();
                        boolean hover = mouseX >= 4 && mouseX <= width - 4
                                && mouseY >= y
                                && mouseY < y + ROW_HEIGHT;

                        int rowBg = enabled ? 0xFF8D238E
                                : (i % 2 == 0 ? 0xFF1B212C : 0xFF202733);
                        if(hover)
                                rowBg = enabled ? 0xFF992EFE
                                        : 0xFF2A3140;
                        drawRect(4, y, width - 4, y + ROW_HEIGHT - 1,
                                rowBg);

                        // checkbox square
                        drawRect(7, y + 3, 13, y + 9,
                                enabled ? 0xFFEA00FF : 0xFF5F6C82);
                        fontRenderer.drawStringWithShadow(hack.getName(),
                                18, y + 3, enabled ? 0xFFFFFFFF
                                        : 0xFFE9EDF5);

                        String category =
                                hack.getCategory().getName();
                        int catX = width - 6
                                - fontRenderer.getStringWidth(category);
                        fontRenderer.drawStringWithShadow(category,
                                catX, y + 3, 0xFF97A2B5);

                        rowRects.add(new int[]{y, i});
                        y += ROW_HEIGHT;
                }

                String footer = "click = toggle  |  esc = close  |  "
                        + visibleHacks.size() + " hacks";
                fontRenderer.drawStringWithShadow(footer, 6, height - 11,
                        0xFF97A2B5);

                super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY,
                int mouseButton) throws IOException
        {
                for(int[] tab : tabRects)
                {
                        if(mouseX >= tab[0] && mouseX < tab[2]
                                && mouseY >= tab[1] && mouseY < tab[3])
                        {
                                selectedCategory = tab[4];
                                scrollOffset = 0;
                                refreshList();
                                return;
                        }
                }

                for(int[] row : rowRects)
                {
                        if(mouseY >= row[0]
                                && mouseY < row[0] + ROW_HEIGHT)
                        {
                                Hack hack = visibleHacks.get(row[1]);
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
