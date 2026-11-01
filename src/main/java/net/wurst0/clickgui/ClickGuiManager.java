/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.clickgui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.wurst0.Category;
import net.wurst0.feature.Hack;
import net.wurst0.hacks.Hacks;
import net.wurst0.WurstClient;

public final class ClickGuiManager
{
        private final List<ClickGuiWindow> windows = new ArrayList<ClickGuiWindow>();

        /**
         * Render/click order only. Never used for layout, so bringing a
         * window to the front no longer reshuffles the other windows'
         * positions the next time the GUI opens.
         */
        private final List<ClickGuiWindow> zOrder =
                new ArrayList<ClickGuiWindow>();

        public ClickGuiManager(Hacks hacks)
        {
                int i = 0;
                for(Category category : Category.values())
                {
                        if(category == Category.FAVORITES || category == Category.COMMANDS)
                                continue;

                        List<Hack> categoryHacks = new ArrayList<Hack>();net.wurst0.hacks.TooManyHaxHack tooManyHax = (net.wurst0.hacks.TooManyHaxHack)WurstClient.INSTANCE
                                                .getHacks().getByName("TooManyHax");
                        for(Hack hack : hacks.getList())
                                if(hack.getCategory() == category
                                                && (tooManyHax == null || !tooManyHax.isHidden(hack.getName())))
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

                // restore window positions and expanded states from a
                // previous session (config/wurst0/clickgui.json) so the
                // GUI stops re-arranging itself every time
                Map<String, int[]> saved = WurstClient.INSTANCE.getFileManager()
                        .loadClickGuiLayout();
                Map<String, Map<String, Boolean>> savedExpanded = WurstClient
                        .INSTANCE.getFileManager().loadClickGuiExpanded();
                Map<String, Boolean> savedCollapsed = WurstClient.INSTANCE
                        .getFileManager().loadClickGuiCollapsed();
                for(ClickGuiWindow window : windows)
                {
                        String name = window.getCategory().getName();
                        int[] pos = saved.get(name);
                        if(pos != null && pos.length == 2)
                                window.restorePosition(pos[0], pos[1]);
                        Map<String, Boolean> expanded = savedExpanded.get(name);
                        if(expanded != null)
                                window.restoreExpandedStates(expanded);
                        Boolean collapsed = savedCollapsed.get(name);
                        if(collapsed != null)
                                window.setCollapsed(collapsed);
                }

                zOrder.addAll(windows);
        }

        /**
         * Persists EVERY window's position (and expanded/collapsed state)
         * to clickgui.json, not just windows the user dragged. This is what
         * keeps the GUI identical between sessions: the automatic column
         * layout only ever runs on the very first launch, when
         * clickgui.json does not exist yet.
         */
        public void saveLayout()
        {
                Map<String, int[]> layout =
                        new java.util.HashMap<String, int[]>();
                Map<String, Map<String, Boolean>> expanded =
                        new java.util.HashMap<String, Map<String, Boolean>>();
                Map<String, Boolean> collapsed =
                        new java.util.HashMap<String, Boolean>();
                for(ClickGuiWindow window : windows)
                {
                        String name = window.getCategory().getName();
                        layout.put(name,
                                new int[]{window.getX(), window.getY()});
                        expanded.put(name, window.getExpandedStates());
                        collapsed.put(name, window.isCollapsed());
                }
                WurstClient.INSTANCE.getFileManager()
                        .saveClickGuiLayout(layout, expanded, collapsed);
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

        /**
         * Puts the given window on top of the others (render + click order).
         * Only changes the z-order list, never the layout list, so clicking
         * a window can no longer rearrange the GUI on the next open.
         */
        public void bringToFront(ClickGuiWindow window)
        {
                if(zOrder.size() > 1
                        && zOrder.get(zOrder.size() - 1) != window)
                {
                        zOrder.remove(window);
                        zOrder.add(window);
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

        /** Windows in render/click (z) order; layout uses category order. */
        public List<ClickGuiWindow> getWindows()
        {
                return zOrder;
        }
}
