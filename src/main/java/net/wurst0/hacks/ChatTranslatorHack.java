/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ChatTranslatorHack (GPL-3.0-or-later)
 * Uses the same Google Translate endpoint as the original
 * (translate.google.com/m); virtual threads replaced by plain daemon
 * threads for Java 8. Needs internet access.
 */
package net.wurst0.hacks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.events.ChatOutputEvent;
import net.wurst0.event.listeners.ChatInputListener;
import net.wurst0.event.listeners.ChatOutputListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.TextFieldSetting;

/**
 * Translates received and/or sent chat messages with Google Translate
 * (needs internet). Received translations are printed locally; sent
 * messages are replaced with their translation.
 */
public final class ChatTranslatorHack extends Hack
        implements ChatInputListener, ChatOutputListener
{
        private static final Pattern RESULT = Pattern.compile(
                "class=\"result-container\">([^<]*)<\\/div>",
                Pattern.MULTILINE);

        private final Minecraft mc = Minecraft.getMinecraft();

        private final ModeSetting whatToTranslate =
                new ModeSetting("What to translate",
                        new String[]{"Received + sent", "Received only",
                                "Sent only"}, 0);
        private final TextFieldSetting playerLanguage =
                new TextFieldSetting("Your language", "en");
        private final TextFieldSetting otherLanguage =
                new TextFieldSetting("Other language", "zh-CN");
        private final CheckboxSetting autoDetectReceived =
                new CheckboxSetting("Detect received language", true);
        private final CheckboxSetting autoDetectSent =
                new CheckboxSetting("Detect sent language", true);

        private final AtomicInteger activeThreads =
                new AtomicInteger(0);

        public ChatTranslatorHack()
        {
                super("ChatTranslator",
                        "Translates chat messages with Google Translate"
                                + " (needs internet).");
                setCategory(Category.CHAT);
                addSettings(whatToTranslate, playerLanguage, otherLanguage,
                        autoDetectReceived, autoDetectSent);
        }

        @Override
        public void onReceivedMessage(ChatInputEvent event)
        {
                if(!includesReceived() || mc.player == null)
                        return;

                String message = event.getText();
                if(message == null)
                        return;

                String clean = message.replaceAll("\u00a7.", "").trim();
                if(clean.length() < 2
                        || clean.startsWith("[ChatTranslator]")
                        || clean.startsWith("[CT]"))
                        return;

                String from = autoDetectReceived.isChecked()
                        ? "auto" : otherLanguage.getValue().trim();
                String to = playerLanguage.getValue().trim();
                translateAsync(clean, from, to, true);
        }

        @Override
        public void onSentMessage(ChatOutputEvent event)
        {
                if(!includesSent() || mc.player == null)
                        return;

                String message = event.getText();
                if(message == null || message.startsWith("/")
                        || message.startsWith("."))
                        return;

                event.setCancelled(true);

                String from = autoDetectSent.isChecked()
                        ? "auto" : playerLanguage.getValue().trim();
                String to = otherLanguage.getValue().trim();
                translateAsync(message, from, to, false);
        }

        private boolean includesReceived()
        {
                String mode = whatToTranslate.getSelected();
                return mode.equals("Received + sent")
                        || mode.equals("Received only");
        }

        private boolean includesSent()
        {
                String mode = whatToTranslate.getSelected();
                return mode.equals("Received + sent")
                        || mode.equals("Sent only");
        }

        private void translateAsync(final String message,
                final String from, final String to,
                final boolean received)
        {
                // don't pile up threads when the API is slow
                if(activeThreads.get() >= 3)
                        return;
                activeThreads.incrementAndGet();

                Thread t = new Thread(new Runnable()
                {
                        @Override
                        public void run()
                        {
                                try
                                {
                                        String translated = translate(
                                                message, from, to);
                                        if(translated == null)
                                                translated = message;
                                        showResult(translated, received);
                                }catch(Throwable e)
                                {
                                        if(!received)
                                                showResult(message, false);
                                }finally
                                {
                                        activeThreads.decrementAndGet();
                                }
                        }
                }, "ChatTranslator");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
        }

        private void showResult(final String text, final boolean received)
        {
                mc.addScheduledTask(new Runnable()
                {
                        @Override
                        public void run()
                        {
                                if(mc.player == null)
                                        return;
                                if(received)
                                        mc.player.sendMessage(
                                                new TextComponentString(
                                                        "\u00a7b[CT] \u00a7f"
                                                                + text));
                                else
                                        mc.player.sendChatMessage(text);
                        }
                });
        }

        /**
         * Same endpoint as the original Wurst7-CevAPI GoogleTranslate:
         * https://translate.google.com/m?hl=en&sl=..&tl=..&ie=UTF-8&prev=_m&q=..
         */
        private static String translate(String text, String langFrom,
                String langTo) throws Exception
        {
                String encoded = URLEncoder.encode(text.trim(), "UTF-8");
                String urlString = String.format(
                        "https://translate.google.com/m?hl=en&sl=%s&tl=%s"
                                + "&ie=UTF-8&prev=_m&q=%s",
                        langFrom, langTo, encoded);

                HttpURLConnection connection =
                        (HttpURLConnection)new URL(urlString)
                                .openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 6.1; WOW64)"
                                + " AppleWebKit/537.11 (KHTML, like Gecko)"
                                + " Chrome/23.0.1271.95 Safari/537.11");

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(),
                                "UTF-8"));
                StringBuilder html = new StringBuilder();
                try
                {
                        String line;
                        while((line = br.readLine()) != null)
                                html.append(line).append('\n');
                }finally
                {
                        br.close();
                }

                String translated = parseHTML(html.toString());
                if(translated == null)
                        return null;

                // original text echoed back (ignoring case/whitespace)
                // means there is nothing to translate
                if(simplify(text).equals(simplify(translated)))
                        return null;

                return translated;
        }

        private static String parseHTML(String html)
        {
                Matcher matcher = RESULT.matcher(html);
                if(!matcher.find())
                        return null;

                String match = matcher.group(1);
                if(match == null || match.isEmpty())
                        return null;

                return unescapeHTML(match);
        }

        private static String simplify(String text)
        {
                StringBuilder sb = new StringBuilder();
                for(char c : text.toLowerCase().toCharArray())
                        if(!Character.isWhitespace(c))
                                sb.append(c);
                return sb.toString();
        }

        /** minimal HTML entity unescape (commons-text not bundled) */
        private static String unescapeHTML(String s)
        {
                StringBuilder sb = new StringBuilder(s.length());
                for(int i = 0; i < s.length(); i++)
                {
                        char c = s.charAt(i);
                        if(c != '&')
                        {
                                sb.append(c);
                                continue;
                        }
                        int semi = s.indexOf(';', i);
                        if(semi < 0 || semi - i > 10)
                        {
                                sb.append(c);
                                continue;
                        }
                        String entity = s.substring(i + 1, semi);
                        if(entity.equals("amp"))
                                sb.append('&');
                        else if(entity.equals("lt"))
                                sb.append('<');
                        else if(entity.equals("gt"))
                                sb.append('>');
                        else if(entity.equals("quot"))
                                sb.append('"');
                        else if(entity.equals("apos"))
                                sb.append('\'');
                        else if(entity.equals("nbsp"))
                                sb.append(' ');
                        else if(entity.startsWith("#"))
                        {
                                try
                                {
                                        int code = Integer.parseInt(
                                                entity.substring(1));
                                        sb.append((char)code);
                                }catch(NumberFormatException e)
                                {
                                        sb.append('&').append(entity)
                                                .append(';');
                                        i = semi;
                                        continue;
                                }
                        }else
                        {
                                sb.append('&').append(entity).append(';');
                                i = semi;
                                continue;
                        }
                        i = semi;
                }
                return sb.toString();
        }
}
