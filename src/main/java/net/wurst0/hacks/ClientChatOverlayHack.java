/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ClientChatOverlayHack (GPL-3.0-or-later)
 * Lite adaptation: draws a client-side overlay panel of captured chat
 * lines with fade-out (the 26.x chat-routing engine has no 1.12.2
 * equivalent hook).
 */
package net.wurst0.hacks;

import java.util.ArrayDeque;
import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.wurst0.Category;
import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.listeners.ChatInputListener;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

/**
 * Shows recently received chat messages in a separate client-side
 * overlay that fades out, independent of the vanilla chat HUD.
 */
public final class ClientChatOverlayHack extends Hack
        implements ChatInputListener, ScreenRenderListener
{
        private static final class Line
        {
                final String text;
                final long at;

                Line(String text, long at)
                {
                        this.text = text;
                        this.at = at;
                }
        }

        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting maxLines =
                new SliderSetting("Max lines", 10, 3, 20, 1);
        private final SliderSetting fadeOut =
                new SliderSetting("Fade-out time (s)", 10, 1, 60, 1);
        private final SliderSetting opacity =
                new SliderSetting("Opacity %", 85, 10, 100, 5);
        private final SliderSetting offsetX =
                new SliderSetting("X offset", 2, -200, 400, 2);
        private final SliderSetting offsetY =
                new SliderSetting("Y offset", 2, -200, 200, 2);
        private final TextFieldSetting filter =
                new TextFieldSetting("Filter (contains)", "");
        private final CheckboxSetting showWhileChatting =
                new CheckboxSetting("Show while chat open", false);

        private final ArrayDeque<Line> lines = new ArrayDeque<Line>();

        public ClientChatOverlayHack()
        {
                super("ClientChatOverlay",
                        "Captures chat into its own fading overlay panel"
                                + " drawn on top of the HUD.");
                setCategory(Category.CHAT);
                addSettings(maxLines, fadeOut, opacity, offsetX, offsetY,
                        filter, showWhileChatting);
        }

        @Override
        public void onReceivedMessage(ChatInputEvent event)
        {
                if(event.getText() == null)
                        return;

                String f = filter.getValue().trim().toLowerCase();
                if(!f.isEmpty() && !event.getText().toLowerCase()
                        .contains(f))
                        return;

                lines.addLast(new Line(event.getText(),
                        System.currentTimeMillis()));
                while(lines.size() > 60)
                        lines.removeFirst();
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(lines.isEmpty() || mc.player == null)
                        return;

                if(!showWhileChatting.isChecked()
                        && mc.currentScreen != null)
                        return;

                long now = System.currentTimeMillis();
                long fadeMs = fadeOut.getValueI() * 1000L;
                int max = maxLines.getValueI();

                ScaledResolution sr = new ScaledResolution(mc);
                int x = offsetX.getValueI();
                int y = sr.getScaledHeight() - 42
                        + offsetY.getValueI();
                int baseAlpha = (opacity.getValueI() * 255) / 100;

                // collect the visible window first (newest at the bottom)
                ArrayDeque<Line> visible = new ArrayDeque<Line>();
                Iterator<Line> itr = lines.descendingIterator();
                while(itr.hasNext() && visible.size() < max)
                {
                        Line line = itr.next();
                        long age = now - line.at;
                        if(age > fadeMs)
                                continue;
                        visible.addFirst(line);
                }

                for(Line line : visible)
                {
                        long age = now - line.at;
                        long remaining = fadeMs - age;
                        int alpha = baseAlpha;
                        // fade over the last 2 seconds
                        if(remaining < 2000)
                                alpha = (int)((baseAlpha * remaining) / 2000);
                        if(alpha < 8)
                                continue;

                        int color = (alpha << 24) | 0xFFFFFF;
                        mc.fontRenderer.drawStringWithShadow(line.text, x, y,
                                color);
                        y -= 10;
                }
        }

        @Override
        protected void onEnable()
        {
                lines.clear();
        }

        @Override
        protected void onDisable()
        {
                lines.clear();
        }
}
