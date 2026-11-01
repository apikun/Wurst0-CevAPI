/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoScoreboardHack (GPL-3.0-or-later)
 * Adapted to 1.12.2: sidebar titles are plain text and sidebar lines
 * are fake player names (max 40 chars) with legacy \u00a7 color codes;
 * the 1.13 team-suffix trick and the permission-hold no-op were
 * dropped.
 */
package net.wurst0.hacks;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

public final class AutoScoreboardHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private static final int MAX_TITLE = 32;
        private static final int MAX_LINE = 40;

        private static final String[] COLOR_NAMES = {"black", "dark_blue",
                "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold",
                "gray", "blue", "green", "aqua", "red", "light_purple",
                "yellow", "white"};
        private static final String[] COLOR_CODES = {"\u00a70", "\u00a71",
                "\u00a72", "\u00a73", "\u00a74", "\u00a75", "\u00a76",
                "\u00a77", "\u00a78", "\u00a79", "\u00a7b", "\u00a7c",
                "\u00a7d", "\u00a7e", "\u00a7f"};

        private final TextFieldSetting title =
                new TextFieldSetting("Title", "Trolled!");
        private final TextFieldSetting titleColor =
                new TextFieldSetting("Title color", "dark_red");
        private final TextFieldSetting content = new TextFieldSetting(
                "Content",
                "Wurst0;1.12.2;Destroyed by {player};{date}", 512);
        private final TextFieldSetting contentColor =
                new TextFieldSetting("Content color", "red");
        private final CheckboxSetting useDelay =
                new CheckboxSetting("Use command delay", false);
        private final SliderSetting delay =
                new SliderSetting("Command delay (ticks)", 2, 1, 20, 1);

        private final Queue<String> queue = new ArrayDeque<String>();
        private int ticks;

        public AutoScoreboardHack()
        {
                super("AutoScoreboard",
                        "Replaces the server sidebar with your configured"
                                + " title and content lines ({player} and {date}"
                                + " are replaced). 1.12.2 limits: title max 32"
                                + " chars, lines max 40 chars and no spaces"
                                + " (spaces become underscores).");
                setCategory(Category.CREATIVE_OP);
                addSettings(title, titleColor, content, contentColor,
                        useDelay, delay);
        }

        @Override
        protected void onEnable()
        {
                queue.clear();
                ticks = 0;

                EntityPlayerSP p = mc.player;
                String self = p == null ? "" : p.getGameProfile().getName();
                String objective = "cevapi_" + UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 8);

                String scoreboardTitle = colorCode(titleColor.getValue())
                        + title.getValue().replace("{player}", self)
                                .replace("{date}", dateString());
                if(scoreboardTitle.length() > MAX_TITLE)
                        scoreboardTitle =
                                scoreboardTitle.substring(0, MAX_TITLE);

                queue.add("scoreboard objectives add " + objective
                        + " dummy " + scoreboardTitle);
                queue.add("scoreboard objectives setdisplay sidebar "
                        + objective);

                String[] lines = content.getValue().split(";");
                int score = lines.length;
                for(String line : lines)
                {
                        String name = line.trim().replace("{player}", self)
                                .replace("{date}", dateString());
                        if(name.isEmpty())
                        {
                                score--;
                                continue;
                        }
                        name = colorCode(contentColor.getValue()) + name;
                        // 1.12.2 scoreboard player names are single tokens.
                        name = name.replace(" ", "_");
                        if(name.length() > MAX_LINE)
                                name = name.substring(0, MAX_LINE);
                        queue.add("scoreboard players set " + name + " "
                                + objective + " " + score);
                        score--;
                }
        }

        @Override
        protected void onDisable()
        {
                queue.clear();
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                        return;
                if(queue.isEmpty())
                {
                        setEnabled(false);
                        return;
                }
                if(useDelay.isChecked() && ticks++ < delay.getValueI())
                        return;
                ticks = 0;

                String command = queue.poll();
                // 1.12.2 servers reject chat packets longer than 256 chars.
                if(command.length() + 1 <= 256)
                        p.sendChatMessage("/" + command);
        }

        private static String dateString()
        {
                return new java.text.SimpleDateFormat("yyyy-MM-dd")
                        .format(new java.util.Date());
        }

        private static String colorCode(String name)
        {
                if(name == null)
                        return "";
                for(int i = 0; i < COLOR_NAMES.length; i++)
                        if(COLOR_NAMES[i].equalsIgnoreCase(name.trim()))
                                return COLOR_CODES[i];
                return "";
        }
}
