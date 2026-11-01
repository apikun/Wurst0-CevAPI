/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.clickgui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.wurst0.Category;
import net.wurst0.WurstClient;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.Setting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;
import net.wurst0.settings.TextFieldSetting;

public final class ClickGuiWindow
{
        // Rusherhack-style dark/crimson palette (reference: gui.webp)
        private static final int PANEL_BG = 0xF00A0A0A;
        private static final int HEADER_BG = 0xFFA03232;
        private static final int BORDER = 0xFFA03232;
        private static final int ENABLED_COLOR = 0xFFC33B3B;
        private static final int DISABLED_COLOR = 0xFFBDBDBD;
        private static final int COUNT_COLOR = 0xFF909090;
        private static final int HOVER_COLOR = 0x28C33B3B;
        private static final int ACCENT = 0xFF8E2B2B;

        static final int WIDTH = 106;
        static final int TITLE_HEIGHT = 12;
        static final int COMPONENT_HEIGHT = 12;
        private static final int MIN_WIDTH = 60;
        private static final int MIN_VIEW_HEIGHT = 36;
        private static final int SCROLL_STEP = 15;

        private final Category category;
        private final List<Component> components =
                new ArrayList<Component>();
        private int x;
        private int y;
        private int width = WIDTH;

        // screen size, updated on every GUI open / window resize
        private int screenWidth = 320;
        private int screenHeight = 240;
        private boolean userMoved;

        // z-order for rendering/clicks; never affects layout
        private int z;

        // collapsed = only the title bar is drawn (click the title to toggle)
        private boolean collapsed;

        // click-vs-drag discrimination on the title bar: a clean click
        // collapses/expands the window, a drag moves it
        private int titlePressX;
        private int titlePressY;
        private boolean titleDragMoved;

        // scrolling for when the content is taller than the screen allows
        private int scrollY;
        private boolean dragging;
        private int dragOffsetX;
        private int dragOffsetY;

        public ClickGuiWindow(Category category, int x, int y, List<Hack> hacks)
        {
                this.category = category;
                this.x = x;
                this.y = y;

                for(Hack hack : hacks)
                        components.add(new FeatureComponent(hack));
        }

        public int getX()
        {
                return x;
        }

        public int getY()
        {
                return y;
        }

        public int getWidth()
        {
                return width;
        }

        /** Height of the whole window as drawn (title + visible content). */
        public int getHeight()
        {
                return TITLE_HEIGHT + getViewHeight();
        }

        /** Full content height, including everything hidden by scrolling. */
        public int getContentHeight()
        {
                if(collapsed)
                        return TITLE_HEIGHT;
                int height = TITLE_HEIGHT;
                for(Component c : components)
                        height += c.getHeight();
                return height;
        }

        /**
         * How many pixels of content are visible below the title bar. Capped
         * to the space between the window top and the bottom of the screen so
         * windows never run off the bottom edge.
         */
        public int getViewHeight()
        {
                if(collapsed)
                        return 0;
                int content = contentHeight();
                int avail = screenHeight - (y + TITLE_HEIGHT) - 4;
                avail = Math.max(MIN_VIEW_HEIGHT, avail);
                return Math.min(content, avail);
        }

        private int contentHeight()
        {
                int height = 0;
                for(Component c : components)
                        height += c.getHeight();
                return height;
        }

        private int getMaxScroll()
        {
                return Math.max(0, contentHeight() - getViewHeight());
        }

        /**
         * Called when the GUI opens or the game window is resized. Adapts the
         * window width to very small screens and pulls the window back into
         * the visible area.
         */
        public void updateScreenSize(int screenWidth, int screenHeight)
        {
                this.screenWidth = screenWidth;
                this.screenHeight = screenHeight;
                width = Math.max(MIN_WIDTH, Math.min(WIDTH, screenWidth - 8));
                clampIntoBounds();
                scrollY = Math.max(0, Math.min(scrollY, getMaxScroll()));
        }

        public void clampIntoBounds()
        {
                x = Math.max(0, Math.min(x,
                        Math.max(0, screenWidth - width)));
                y = Math.max(0, Math.min(y,
                        Math.max(0, screenHeight - TITLE_HEIGHT)));
        }

        public void setPosition(int x, int y)
        {
                this.x = x;
                this.y = y;
        }

        public Category getCategory()
        {
                return category;
        }

        /**
         * Restores a saved position and flags the window as user-moved so
         * the automatic column layout leaves it alone.
         */
        public void restorePosition(int x, int y)
        {
                this.x = x;
                this.y = y;
                this.userMoved = true;
        }

        public boolean wasUserMoved()
        {
                return userMoved;
        }

        /** Collapsed = only the title bar is shown. */
        public boolean isCollapsed()
        {
                return collapsed;
        }

        public void setCollapsed(boolean collapsed)
        {
                this.collapsed = collapsed;
                scrollY = 0;
        }

        /** Z-order for stacking windows on top of each other. */
        public int getZ()
        {
                return z;
        }

        public void setZ(int z)
        {
                this.z = z;
        }

        /** Snapshot of which hacks in this window are expanded. */
        public Map<String, Boolean> getExpandedStates()
        {
                Map<String, Boolean> states =
                        new LinkedHashMap<String, Boolean>();
                for(Component c : components)
                        if(c instanceof FeatureComponent)
                        {
                                FeatureComponent fc = (FeatureComponent)c;
                                states.put(fc.hack.getName(), fc.expanded);
                        }
                return states;
        }

        /** Restores expanded/collapsed state saved in clickgui.json. */
        public void restoreExpandedStates(Map<String, Boolean> states)
        {
                for(Component c : components)
                        if(c instanceof FeatureComponent)
                        {
                                FeatureComponent fc = (FeatureComponent)c;
                                Boolean state = states.get(fc.hack.getName());
                                if(state != null)
                                        fc.expanded = state;
                        }
        }

        public boolean contains(int mouseX, int mouseY)
        {
                return mouseX >= x && mouseX < x + width && mouseY >= y
                        && mouseY < y + TITLE_HEIGHT + getViewHeight();
        }

        private static FontHelper fr()
        {
                return FontHelper.INSTANCE;
        }

        public void render(int mouseX, int mouseY)
        {
                int viewHeight = getViewHeight();
                scrollY = Math.max(0, Math.min(scrollY, getMaxScroll()));

                // window background (near-black, matches reference)
                Gui.drawRect(x, y, x + width, y + TITLE_HEIGHT + viewHeight,
                        PANEL_BG);
                // 1px crimson border around the whole panel
                Gui.drawRect(x, y, x + 1, y + TITLE_HEIGHT + viewHeight,
                        BORDER);
                Gui.drawRect(x + width - 1, y, x + width,
                        y + TITLE_HEIGHT + viewHeight, BORDER);
                Gui.drawRect(x, y + TITLE_HEIGHT + viewHeight - 1, x + width,
                        y + TITLE_HEIGHT + viewHeight, BORDER);
                // title bar (dark red)
                int titleColor = HEADER_BG;
                if(isRainbowUi())
                {
                        float hue = (System.currentTimeMillis() % 4000)
                                / 4000F;
                        titleColor = 0xFF000000 | java.awt.Color.HSBtoRGB(
                                hue, 0.65F, 0.5F);
                }
                Gui.drawRect(x + 1, y, x + width - 1, y + TITLE_HEIGHT,
                        titleColor);

                fr().drawShadow(category.getName(), x + 3, y + 2, 0xFFFFFFFF);

                // [count] badge, right-aligned like the reference
                String count = "[" + components.size() + "]";
                int cw = fr().getStringWidth(count);
                fr().drawShadow(count, x + width - cw - 3, y + 2,
                        COUNT_COLOR);

                // +/- collapse indicator, just left of the count
                fr().drawShadow(collapsed ? "+" : "-", x + width - cw - 11,
                        y + 2, COUNT_COLOR);

                if(viewHeight <= 0)
                        return;

                // content, clipped to the window and offset by the scroll
                enableScissor(x, y + TITLE_HEIGHT, width, viewHeight);
                int cy = y + TITLE_HEIGHT - scrollY;
                for(Component c : components)
                {
                        if(cy + c.getHeight() > y + TITLE_HEIGHT
                                && cy < y + TITLE_HEIGHT + viewHeight)
                                c.render(x, cy, mouseX, mouseY);
                        cy += c.getHeight();
                }
                disableScissor();

                // scrollbar
                int maxScroll = getMaxScroll();
                if(maxScroll > 0)
                {
                        int barHeight = Math.max(10, viewHeight * viewHeight
                                / (viewHeight + maxScroll));
                        int barY = y + TITLE_HEIGHT + (viewHeight - barHeight)
                                * scrollY / maxScroll;
                        Gui.drawRect(x + width - 3, y + TITLE_HEIGHT,
                                x + width - 2, y + TITLE_HEIGHT + viewHeight,
                                0x20808080);
                        Gui.drawRect(x + width - 3, barY, x + width - 2,
                                barY + barHeight, ACCENT);
                }
        }

        private static void enableScissor(int sx, int sy, int sw, int sh)
        {
                Minecraft mc = Minecraft.getMinecraft();
                ScaledResolution sr = new ScaledResolution(mc);
                double f = sr.getScaleFactor();
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor((int)Math.floor(sx * f),
                        (int)Math.floor((sr.getScaledHeight() - sy - sh) * f),
                        (int)Math.ceil(sw * f), (int)Math.ceil(sh * f));
        }

        private static void disableScissor()
        {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        public void mouseClicked(int mouseX, int mouseY, int mouseButton)
        {
                if(!contains(mouseX, mouseY))
                        return;

                // clicking anywhere in the window stops text editing,
                // the clicked text field re-focuses itself below
                unfocusTextFields();

                if(mouseY < y + TITLE_HEIGHT)
                {
                        if(mouseButton == 0)
                        {
                                dragging = true;
                                dragOffsetX = mouseX - x;
                                dragOffsetY = mouseY - y;
                                titlePressX = mouseX;
                                titlePressY = mouseY;
                                titleDragMoved = false;
                        }
                        return;
                }

                // route clicks in virtual (unscrolled) content coordinates
                int vy = mouseY + scrollY;
                int cy = y + TITLE_HEIGHT;
                for(Component c : components)
                {
                        if(vy >= cy && vy < cy + c.getHeight())
                        {
                                c.mouseClicked(x, cy, mouseX, vy, mouseButton);
                                return;
                        }
                        cy += c.getHeight();
                }
        }

        public void mouseReleased(int mouseX, int mouseY, int state)
        {
                // a clean click on the title bar (no dragging) collapses or
                // expands the window, like Wurst's own ClickGUI
                if(dragging && !titleDragMoved && state == 0)
                        setCollapsed(!collapsed);
                dragging = false;
                for(Component c : components)
                        c.mouseReleased(mouseX, mouseY, state);
        }

        public void mouseClickMove(int mouseX, int mouseY, int clickedButton,
                long timeSinceLastClick)
        {
                if(dragging)
                {
                        // only start moving after 4px so a plain click stays
                        // a collapse/expand toggle
                        if(!titleDragMoved)
                        {
                                if(Math.abs(mouseX - titlePressX) < 4
                                        && Math.abs(mouseY - titlePressY) < 4)
                                        return;
                                titleDragMoved = true;
                        }
                        x = mouseX - dragOffsetX;
                        y = mouseY - dragOffsetY;
                        clampIntoBounds();
                        userMoved = true;
                        return;
                }

                if(!contains(mouseX, mouseY))
                        return;

                int vy = mouseY + scrollY;
                int cy = y + TITLE_HEIGHT;
                for(Component c : components)
                {
                        if(vy >= cy && vy < cy + c.getHeight())
                        {
                                c.mouseClickMove(x, cy, mouseX, vy,
                                        clickedButton);
                                return;
                        }
                        cy += c.getHeight();
                }
        }

        /**
         * Scrolls this window if the mouse is over it.
         *
         * @return true if the mouse was over this window (stops the event
         *         from also scrolling windows underneath)
         */
        public boolean mouseWheel(int mouseX, int mouseY, int delta)
        {
                if(delta == 0 || !contains(mouseX, mouseY))
                        return false;

                if(collapsed)
                        return true;

                scrollY -= Integer.signum(delta) * SCROLL_STEP;
                scrollY = Math.max(0, Math.min(scrollY, getMaxScroll()));
                return true;
        }

        /**
         * Routes a key press to the focused text field, if any.
         *
         * @return true if a text field consumed the key (the GUI then
         *         ignores it, e.g. Esc while editing just stops editing)
         */
        public boolean keyTyped(char typedChar, int keyCode)
        {
                for(Component c : components)
                        if(c.keyTyped(typedChar, keyCode))
                                return true;
                return false;
        }

        public void unfocusTextFields()
        {
                for(Component c : components)
                        c.unfocusText();
        }

        private static boolean isRainbowUi()
        {
                Hack h = WurstClient.INSTANCE.getHacks()
                        .getByName("RainbowUi");
                return h != null && h.isEnabled();
        }

        // ==================== components ====================

        private abstract static class Component
        {
                abstract int getHeight();

                /**
                 * Renders the component. wx/wy are the component's top-left
                 * corner in screen space (already scroll-adjusted).
                 */
                abstract void render(int wx, int wy, int mouseX, int mouseY);

                /**
                 * Handles a click. wx/wy are the component's top-left corner
                 * in virtual content space; mouseX/mouseY use the same space
                 * (mouseY already includes the scroll offset).
                 */
                void mouseClicked(int wx, int wy, int mouseX, int mouseY,
                        int mouseButton)
                {}

                void mouseReleased(int mouseX, int mouseY, int state)
                {}

                void mouseClickMove(int wx, int wy, int mouseX, int mouseY,
                        int clickedButton)
                {}

                boolean keyTyped(char typedChar, int keyCode)
                {
                        return false;
                }

                void unfocusText()
                {}

                static boolean hovered(int w, int wx, int wy, int mouseX,
                        int mouseY)
                {
                        return mouseX >= wx && mouseX < wx + w
                                && mouseY >= wy && mouseY < wy
                                        + COMPONENT_HEIGHT;
                }
        }

        private final class FeatureComponent extends Component
        {
                private final Hack hack;
                private final List<Component> children =
                        new ArrayList<Component>();
                private boolean expanded;

                FeatureComponent(Hack hack)
                {
                        this.hack = hack;

                        for(Setting setting : hack.getSettings())
                        {
                                if(setting instanceof CheckboxSetting)
                                        children.add(new CheckboxComponent(
                                                (CheckboxSetting)setting));
                                else if(setting instanceof SliderSetting)
                                        children.add(new SliderComponent((SliderSetting)setting));
                                else if(setting instanceof ModeSetting)
                                        children.add(new ModeComponent((ModeSetting)setting));
                                else if(setting instanceof TextFieldSetting)
                                        children.add(new TextComponent(
                                                (TextFieldSetting)setting));
                        }
                }

                @Override
                boolean keyTyped(char typedChar, int keyCode)
                {
                        for(Component c : children)
                                if(c.keyTyped(typedChar, keyCode))
                                        return true;
                        return false;
                }

                @Override
                void unfocusText()
                {
                        for(Component c : children)
                                c.unfocusText();
                }

                @Override
                int getHeight()
                {
                        int height = COMPONENT_HEIGHT;
                        if(expanded)
                                for(Component c : children)
                                        height += c.getHeight();
                        return height;
                }

                @Override
                void render(int wx, int wy, int mouseX, int mouseY)
                {
                        boolean hov = hovered(width, wx, wy, mouseX, mouseY);
                        if(hov)
                                Gui.drawRect(wx, wy, wx + width,
                                        wy + COMPONENT_HEIGHT, HOVER_COLOR);

                        // enabled = red text, disabled = gray (reference style)
                        int color = hack.isEnabled() ? ENABLED_COLOR
                                : DISABLED_COLOR;
                        fr().drawShadow(hack.getName(), wx + 5, wy + 2, color);

                        if(!children.isEmpty())
                                fr().drawShadow(expanded ? "-" : "+",
                                        wx + width - 8, wy + 2, 0xFF909090);

                        int cy = wy + COMPONENT_HEIGHT;
                        if(expanded)
                        {
                                Gui.drawRect(wx + 1, cy, wx + 2,
                                        cy + getHeight() - COMPONENT_HEIGHT,
                                        0xFF303030);
                                for(Component c : children)
                                {
                                        c.render(wx, cy, mouseX, mouseY);
                                        cy += c.getHeight();
                                }
                        }
                }

                @Override
                void mouseClicked(int wx, int wy, int mouseX, int mouseY,
                        int mouseButton)
                {
                        // settings get the click first, so clicking a
                        // checkbox/slider row no longer toggles the hack
                        if(expanded)
                        {
                                int cy = wy + COMPONENT_HEIGHT;
                                for(Component c : children)
                                {
                                        if(mouseY >= cy
                                                && mouseY < cy + c.getHeight())
                                        {
                                                c.mouseClicked(wx, cy, mouseX,
                                                        mouseY, mouseButton);
                                                return;
                                        }
                                        cy += c.getHeight();
                                }
                        }

                        if(mouseButton == 0)
                                hack.toggle();
                        else if(mouseButton == 1 && !children.isEmpty())
                                expanded = !expanded;

                        WurstClient.INSTANCE.getFileManager().markDirty();
                }

                @Override
                void mouseReleased(int mouseX, int mouseY, int state)
                {
                        for(Component c : children)
                                c.mouseReleased(mouseX, mouseY, state);
                }

                @Override
                void mouseClickMove(int wx, int wy, int mouseX, int mouseY,
                        int clickedButton)
                {
                        if(!expanded)
                                return;

                        // wx/wy is THIS feature's top, so the children are
                        // hit-tested from the right origin (was previously
                        // broken for every feature but the first)
                        int cy = wy + COMPONENT_HEIGHT;
                        for(Component c : children)
                        {
                                if(mouseY >= cy && mouseY < cy + c.getHeight())
                                {
                                        c.mouseClickMove(wx, cy, mouseX,
                                                mouseY, clickedButton);
                                        return;
                                }
                                cy += c.getHeight();
                        }
                }
        }

        private final class CheckboxComponent extends Component
        {
                private final CheckboxSetting setting;

                CheckboxComponent(CheckboxSetting setting)
                {
                        this.setting = setting;
                }

                @Override
                int getHeight()
                {
                        return COMPONENT_HEIGHT;
                }

                @Override
                void render(int wx, int wy, int mouseX, int mouseY)
                {
                        if(hovered(width, wx, wy, mouseX, mouseY))
                                Gui.drawRect(wx, wy, wx + width,
                                        wy + COMPONENT_HEIGHT, HOVER_COLOR);

                        // checkbox
                        Gui.drawRect(wx + 5, wy + 3, wx + 10, wy + 8,
                                0xFF404040);
                        if(setting.isChecked())
                                Gui.drawRect(wx + 6, wy + 4, wx + 9, wy + 7,
                                        ENABLED_COLOR);

                        fr().drawShadow(setting.getName(), wx + 14, wy + 2,
                                0xFFBDBDBD);
                }

                @Override
                void mouseClicked(int wx, int wy, int mouseX, int mouseY,
                        int mouseButton)
                {
                        if(mouseButton == 0)
                        {
                                setting.toggle();
                                WurstClient.INSTANCE.getFileManager()
                                        .markDirty();
                        }
                }
        }

        private final class SliderComponent extends Component
        {
                private final SliderSetting setting;
                private boolean draggingSlider;

                SliderComponent(SliderSetting setting)
                {
                        this.setting = setting;
                }

                @Override
                int getHeight()
                {
                        return COMPONENT_HEIGHT;
                }

                @Override
                void render(int wx, int wy, int mouseX, int mouseY)
                {
                        if(hovered(width, wx, wy, mouseX, mouseY))
                                Gui.drawRect(wx, wy, wx + width,
                                        wy + COMPONENT_HEIGHT, HOVER_COLOR);

                        // slider bar
                        Gui.drawRect(wx + 5, wy + 9, wx + width - 5, wy + 10,
                                0xFF3A3A3A);

                        double fraction =
                                (setting.getValue() - setting.getMinimum())
                                        / (setting.getMaximum()
                                                - setting.getMinimum());
                        fraction = Math.max(0, Math.min(1, fraction));
                        Gui.drawRect(wx + 5, wy + 9,
                                wx + 5 + (int)((width - 10) * fraction),
                                wy + 10, ACCENT);

                        String text = setting.getName() + ": "
                                + formatValue(setting.getValue());
                        fr().drawShadow(text, wx + 5, wy + 1, 0xFFBDBDBD);
                }

                private String formatValue(double value)
                {
                        int decimals = setting.getDecimals();
                        return decimals == 0 ? Integer.toString((int)value)
                                : String.format("%." + decimals + "f", value);
                }

                @Override
                void mouseClicked(int wx, int wy, int mouseX, int mouseY,
                        int mouseButton)
                {
                        if(mouseButton == 0)
                                draggingSlider = true;
                        updateValue(mouseX);
                }

                @Override
                void mouseReleased(int mouseX, int mouseY, int state)
                {
                        draggingSlider = false;
                        WurstClient.INSTANCE.getFileManager().markDirty();
                }

                @Override
                void mouseClickMove(int wx, int wy, int mouseX, int mouseY,
                        int clickedButton)
                {
                        if(draggingSlider)
                                updateValue(mouseX);
                }

                private void updateValue(int mouseX)
                {
                        double fraction =
                                (double)(mouseX - 5 - ClickGuiWindow.this.x)
                                        / (width - 10);
                        fraction = Math.max(0, Math.min(1, fraction));
                        double value = setting.getMinimum()
                                + fraction * (setting.getMaximum()
                                        - setting.getMinimum());
                        setting.setValue(value);
                }
        }

        private final class ModeComponent extends Component
        {
                private final ModeSetting setting;

                ModeComponent(ModeSetting setting)
                {
                        this.setting = setting;
                }

                @Override
                int getHeight()
                {
                        return COMPONENT_HEIGHT;
                }

                @Override
                void render(int wx, int wy, int mouseX, int mouseY)
                {
                        if(hovered(width, wx, wy, mouseX, mouseY))
                                Gui.drawRect(wx, wy, wx + width,
                                        wy + COMPONENT_HEIGHT, HOVER_COLOR);

                        fr().drawShadow(
                                setting.getName() + ": " + setting.getSelected(),
                                wx + 5, wy + 2, 0xFFBDBDBD);
                }

                @Override
                void mouseClicked(int wx, int wy, int mouseX, int mouseY,
                        int mouseButton)
                {
                        if(mouseButton == 0)
                        {
                                setting.cycle();
                                WurstClient.INSTANCE.getFileManager()
                                        .markDirty();
                        }
                }
        }

        private final class TextComponent extends Component
        {
                private final TextFieldSetting setting;
                private boolean editing;
                private final StringBuilder buffer = new StringBuilder();

                TextComponent(TextFieldSetting setting)
                {
                        this.setting = setting;
                }

                @Override
                int getHeight()
                {
                        return 24;
                }

                @Override
                void render(int wx, int wy, int mouseX, int mouseY)
                {
                        fr().drawShadow(setting.getName(), wx + 5, wy + 1,
                                0xFFA0A0A0);

                        Gui.drawRect(wx + 4, wy + 11, wx + width - 4,
                                wy + 22, editing ? 0xFF2A1515 : 0xFF1A1A1A);
                        if(editing)
                                Gui.drawRect(wx + 4, wy + 11, wx + 5,
                                        wy + 22, ENABLED_COLOR);

                        String text = editing ? buffer.toString()
                                : setting.getValue();

                        // clip from the left so the end (where you type)
                        // always stays visible, even on tiny windows
                        int maxWidth = width - 12;
                        while(text.length() > 1 && fr()
                                .getStringWidth(text) > maxWidth)
                                text = text.substring(1);

                        if(editing
                                && System.currentTimeMillis() / 500 % 2 == 0)
                                text = text + "_";

                        fr().drawShadow(text, wx + 6, wy + 13, 0xFFF0F0F0);
                }

                @Override
                void mouseClicked(int wx, int wy, int mouseX, int mouseY,
                        int mouseButton)
                {
                        if(mouseButton == 0 && !editing)
                        {
                                editing = true;
                                buffer.setLength(0);
                                buffer.append(setting.getValue());
                        }
                }

                @Override
                boolean keyTyped(char typedChar, int keyCode)
                {
                        if(!editing)
                                return false;

                        if(keyCode == Keyboard.KEY_RETURN
                                || keyCode == Keyboard.KEY_ESCAPE)
                        {
                                stopEditing();
                                return true;
                        }

                        if(keyCode == Keyboard.KEY_BACK)
                        {
                                if(buffer.length() > 0)
                                {
                                        buffer.setLength(buffer.length() - 1);
                                        setting.setValue(buffer.toString());
                                        WurstClient.INSTANCE.getFileManager()
                                                .markDirty();
                                }
                                return true;
                        }

                        if(typedChar >= ' '
                                && buffer.length() < setting.getMaxLength())
                        {
                                buffer.append(typedChar);
                                setting.setValue(buffer.toString());
                                WurstClient.INSTANCE.getFileManager()
                                        .markDirty();
                        }
                        return true;
                }

                private void stopEditing()
                {
                        editing = false;
                        setting.setValue(buffer.toString());
                        WurstClient.INSTANCE.getFileManager().markDirty();
                }

                @Override
                void unfocusText()
                {
                        if(editing)
                                stopEditing();
                }
        }

        /** Small helper so the window code doesn't drag in GlStateManager text. */
        private static final class FontHelper
        {
                static final FontHelper INSTANCE = new FontHelper();

                void drawShadow(String text, int x, int y, int color)
                {
                        Minecraft.getMinecraft().fontRenderer
                                .drawStringWithShadow(text, x, y, color);
                        GlStateManager.color(1, 1, 1, 1);
                }

                int getStringWidth(String text)
                {
                        return Minecraft.getMinecraft().fontRenderer
                                .getStringWidth(text);
                }
        }
}
