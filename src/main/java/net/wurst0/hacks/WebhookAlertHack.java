/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI WebhookAlertHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.wurst0.Category;
import net.wurst0.event.listeners.ChatInputListener;
import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

/**
 * Sends client-side alerts to a Discord webhook: mentions of your
 * aliases in chat and players entering your render range. Requests
 * run on a background thread so gameplay never blocks.
 */
public final class WebhookAlertHack extends Hack
        implements ChatInputListener, UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting webhookUrl =
                new TextFieldSetting("Webhook URL", "", 512);
        private final TextFieldSetting mentionAliases =
                new TextFieldSetting("Mention aliases", "apikun", 256);
        private final CheckboxSetting mentions =
                new CheckboxSetting("Mentions", true);
        private final CheckboxSetting playerEnterRange =
                new CheckboxSetting("Player enters range", true);
        private final SliderSetting range =
                new SliderSetting("Range", 32, 8, 128, 8);

        private final List<UUID> seenPlayers = new ArrayList<java.util.UUID>();
        private int timer;

        public WebhookAlertHack()
        {
                super("WebhookAlert",
                        "Sends selected client-side alerts to a Discord webhook.");
                setCategory(Category.OTHER);
                addSettings(webhookUrl, mentionAliases, mentions,
                        playerEnterRange, range);
        }

        @Override
        public void onReceivedMessage(ChatInputEvent event)
        {
                if(!mentions.isChecked())
                        return;
                String text = event.getText();
                String raw = mentionAliases.getValue();
                if(raw.trim().isEmpty())
                        return;
                for(String alias : raw.split(","))
                {
                        String a = alias.trim();
                        if(!a.isEmpty() && text.toLowerCase().contains(a.toLowerCase()))
                        {
                                post("**[Wurst0]** Mention in chat: " + text);
                                break;
                        }
                }
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                {
                        seenPlayers.clear();
                        return;
                }
                if(!playerEnterRange.isChecked())
                        return;

                if(timer > 0)
                {
                        timer--;
                        return;
                }
                timer = 20;

                double rangeSq = range.getValue() * range.getValue();
                List<EntityPlayer> players =
                        new ArrayList<EntityPlayer>(mc.world.playerEntities);
                List<java.util.UUID> current =
                        new ArrayList<java.util.UUID>();

                for(EntityPlayer other : players)
                {
                        if(other == p)
                                continue;
                        current.add(other.getUniqueID());
                        if(seenPlayers.contains(other.getUniqueID()))
                                continue;
                        if(other.getDistanceSq(p.posX, p.posY, p.posZ) <= rangeSq)
                                post("**[Wurst0]** Player **" + other.getName()
                                        + "** entered your range ("
                                        + (int)other.posX + " " + (int)other.posY + " "
                                        + (int)other.posZ + ")");
                }

                seenPlayers.clear();
                seenPlayers.addAll(current);
        }

        private void post(final String content)
        {
                final String url = webhookUrl.getValue().trim();
                if(url.isEmpty() || !url.startsWith("http"))
                        return;
                final String json = "{\"content\":\""
                        + escape(content) + "\"}";

                Thread t = new Thread(new Runnable()
                {
                        @Override
                        public void run()
                        {
                                try
                                {
                                        HttpURLConnection conn =
                                                (HttpURLConnection)new URL(url)
                                                        .openConnection();
                                        conn.setRequestMethod("POST");
                                        conn.setDoOutput(true);
                                        conn.setConnectTimeout(5000);
                                        conn.setReadTimeout(5000);
                                        conn.setRequestProperty("Content-Type",
                                                "application/json");
                                        OutputStream out = conn.getOutputStream();
                                        out.write(json.getBytes(StandardCharsets.UTF_8));
                                        out.flush();
                                        out.close();
                                        conn.getResponseCode();
                                }catch(Throwable ignored)
                                {}
                        }
                });
                t.setDaemon(true);
                t.start();
        }

        private static String escape(String s)
        {
                return s.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", " ")
                        .replace("\r", " ");
        }

        @Override
        protected void onDisable()
        {
                seenPlayers.clear();
        }
}
