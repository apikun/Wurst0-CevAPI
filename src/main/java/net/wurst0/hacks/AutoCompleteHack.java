/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoCompleteHack (GPL-3.0-or-later)
 * Lite adaptation: the 26.x OpenAI/GPT suggestion pipeline is replaced
 * by a ".complete <prefix>" command that lists matching player names
 * and words seen in chat (no internet, no API key).
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.events.ChatOutputEvent;
import net.wurst0.event.listeners.ChatInputListener;
import net.wurst0.event.listeners.ChatOutputListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Type ".complete <prefix>" while enabled to get chat suggestions
 * (matching player names and words heard in chat).
 */
public final class AutoCompleteHack extends Hack
        implements ChatInputListener, ChatOutputListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting playerNames =
                new CheckboxSetting("Include player names", true);
        private final CheckboxSetting chatWords =
                new CheckboxSetting("Include chat words", true);
        private final SliderSetting maxSuggestions =
                new SliderSetting("Max suggestions", 8, 1, 15, 1);

        /** recent tokens heard in chat, most recent last */
        private final List<String> history =
                new ArrayList<String>();

        public AutoCompleteHack()
        {
                super("AutoComplete",
                        "Suggests completions for chat: type"
                                + " \"\u00a76.complete <prefix>\u00a7r\" to"
                                + " list matching player names and recent"
                                + " chat words.");
                setCategory(Category.CHAT);
                addSettings(playerNames, chatWords, maxSuggestions);
        }

        @Override
        public void onReceivedMessage(ChatInputEvent event)
        {
                if(!chatWords.isChecked() || event.getText() == null)
                        return;

                String text = event.getText().replaceAll("\u00a7.", "");
                for(String token : text.split("[^A-Za-z0-9_]"))
                {
                        if(token.length() < 3 || token.length() > 16)
                                continue;
                        history.remove(token);
                        history.add(token);
                }
                while(history.size() > 300)
                        history.remove(0);
        }

        @Override
        public void onSentMessage(ChatOutputEvent event)
        {
                String text = event.getText() == null ? ""
                        : event.getText().trim();
                if(!text.startsWith(".complete "))
                        return;

                event.setCancelled(true);

                String prefix = text.substring(".complete ".length())
                        .trim().toLowerCase();
                if(prefix.isEmpty())
                {
                        print("\u00a7e[AutoComplete] Usage: .complete"
                                + " <prefix>");
                        return;
                }

                Set<String> suggestions = new LinkedHashSet<String>();

                if(playerNames.isChecked() && mc.getConnection() != null)
                {
                        for(NetworkPlayerInfo info :
                                mc.getConnection().getPlayerInfoMap())
                        {
                                String name = info.getGameProfile() == null
                                        ? null : info.getGameProfile()
                                                .getName();
                                if(name != null && name.toLowerCase()
                                        .startsWith(prefix))
                                        suggestions.add(name);
                        }
                }

                if(chatWords.isChecked())
                {
                        // newest history wins: walk backwards
                        for(int i = history.size() - 1; i >= 0
                                && suggestions.size() < maxSuggestions
                                        .getValueI() + 16; i--)
                        {
                                String word = history.get(i);
                                if(word.toLowerCase().startsWith(prefix))
                                        suggestions.add(word);
                        }
                }

                if(suggestions.isEmpty())
                {
                        print("\u00a77[AutoComplete] No matches for \""
                                + prefix + "\".");
                        return;
                }

                List<String> out = new ArrayList<String>(
                        suggestions);
                while(out.size() > maxSuggestions.getValueI())
                        out.remove(out.size() - 1);

                StringBuilder sb = new StringBuilder(
                        "\u00a7a[AutoComplete] ");
                for(int i = 0; i < out.size(); i++)
                {
                        if(i > 0)
                                sb.append("\u00a77, ");
                        sb.append("\u00a7f").append(out.get(i));
                }
                print(sb.toString());
        }

        private void print(String line)
        {
                if(mc.player != null)
                        mc.player.sendMessage(
                                new TextComponentString(line));
        }

        @Override
        protected void onEnable()
        {
                history.clear();
        }
}
