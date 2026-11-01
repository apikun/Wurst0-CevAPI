/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiCheatDetectHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.listeners.ChatInputListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * Watches the chat for anti-cheat kick/warning messages ("moved too
 * quickly", "flying is not enabled"...) and highlights them so you
 * know which hack got you flagged before you get kicked.
 */
public final class AntiCheatDetectHack extends Hack
        implements ChatInputListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting cancelOriginal =
                new CheckboxSetting("Hide original message", false);
        private final CheckboxSetting onlyKicks =
                new CheckboxSetting("Only kicks/bans", false);

        private final Pattern[] warnings =
                {
                        Pattern.compile("moved too quickly", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("moved wrongly", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("flying is not enabled",
                                Pattern.CASE_INSENSITIVE),
                        Pattern.compile("fly hack", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("speed hack", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("kicked for", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("banned for", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("anti.?cheat", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("unfair advantage",
                                Pattern.CASE_INSENSITIVE),
                };

        private final Pattern[] kicks =
                {
                        Pattern.compile("kicked", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("banned", Pattern.CASE_INSENSITIVE),
                };

        private int detections;

        public AntiCheatDetectHack()
        {
                super("AntiCheatDetect",
                        "Detects anti-cheat warnings in the chat and marks"
                                + " them clearly.");
                setCategory(Category.INTEL);
                addSettings(cancelOriginal, onlyKicks);
        }

        @Override
        public void onReceivedMessage(ChatInputEvent event)
        {
                String text = event.getText();
                if(text == null || text.isEmpty())
                        return;

                boolean hit = false;
                boolean isKick = false;

                for(Pattern p : kicks)
                {
                        if(p.matcher(text).find())
                        {
                                hit = true;
                                isKick = true;
                                break;
                        }
                }

                if(!hit && !onlyKicks.isChecked())
                {
                        for(Pattern p : warnings)
                        {
                                if(p.matcher(text).find())
                                {
                                        hit = true;
                                        break;
                                }
                        }
                }

                if(!hit)
                        return;

                detections++;
                mc.player.sendMessage(new TextComponentString(
                        "\u00a7c\u00a7l[AntiCheatDetect #" + detections
                                + "]" + (isKick ? " \u00a74KICK/BAN:" : ":")
                                + " \u00a77 see previous message"));

                if(cancelOriginal.isChecked())
                        event.setCancelled(true);
        }
}
