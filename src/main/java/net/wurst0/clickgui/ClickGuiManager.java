/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.clickgui;

import java.util.ArrayList;
import java.util.List;

import net.wurst0.Category;
import net.wurst0.feature.Hack;
import net.wurst0.hacks.Hacks;

public final class ClickGuiManager
{
        private final List<ClickGuiWindow> windows = new ArrayList<ClickGuiWindow>();

        public ClickGuiManager(Hacks hacks)
        {
                int i = 0;
                for(Category category : Category.values())
                {
                        if(category == Category.FAVORITES || category == Category.COMMANDS)
                                continue;

                        List<Hack> categoryHacks = new ArrayList<Hack>();
                        for(Hack hack : hacks.getList())
                                if(hack.getCategory() == category)
                                        categoryHacks.add(hack);

                        if(categoryHacks.isEmpty())
                                continue;

                        // provisional cascade positions; the real layout
                        // happens in setScreenSize() when the GUI opens
                        int x = 8 + i * 26;
                        int y = 8 + i * 26;
                        windows.add(new ClickGuiWindow(category, x, y, categoryHacks));
                        i++;
                }
        }

        /**
         * Called every time the ClickGUI opens (and on window resize). Lays
         * the windows out in columns that fit the screen, so the GUI works
         * on small screens and high GUI scales too. Windows the user has
         * dragged keep their position, they are just clamped back into view.
         */
        public void setScreenSize(int screenWidth, int screenHeight)
        {
                int columns = Math.max(1, Math.min(windows.size(),
                        (screenWidth - 8) / (ClickGuiWindow.WIDTH + 4)));
                int[] yCursor = new int[columns];

                for(ClickGuiWindow window : windows)
                {
                        window.updateScreenSize(screenWidth, screenHeight);

                        if(window.wasUserMoved())
                        {
                                window.clampIntoBounds();
                                continue;
                        }

                        // place into the column with the least content so
                        // the columns fill up evenly
                        int best = 0;
                        for(int i = 1; i < columns; i++)
                                if(yCursor[i] < yCursor[best])
                                        best = i;

                        window.setPosition(4 + best * (window.getWidth() + 4),
                                yCursor[best] + 4);
                        yCursor[best] += Math.min(window.getContentHeight(),
                                screenHeight - 8) + 6;
                }
        }

        /** Puts the given window on top of the others (render + click order). */
        public void bringToFront(ClickGuiWindow window)
        {
                if(windows.size() > 1 && windows.get(windows.size() - 1) != window)
                {
                        windows.remove(window);
                        windows.add(window);
                }
        }

        /**
         * Sends a key press to the focused text field, if any.
         *
         * @return true if a text field consumed the key
         */
        public boolean keyTyped(char typedChar, int keyCode)
        {
                for(ClickGuiWindow window : windows)
                        if(window.keyTyped(typedChar, keyCode))
                                return true;
                return false;
        }

        public void unfocusAllText()
        {
                for(ClickGuiWindow window : windows)
                        window.unfocusTextFields();
        }

        public List<ClickGuiWindow> getWindows()
        {
                return windows;
        }
}
