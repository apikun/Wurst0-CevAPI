/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI LivestreamDetectorHack (GPL-3.0-or-later)
 * Lite adaptation: java.net.http + virtual threads become
 * HttpURLConnection + daemon threads (Java 8); the ButtonSettings of
 * the original (manual scan/cancel) have no fork equivalent and were
 * dropped. Same platform checks and public-page markers.
 */
package net.wurst0.hacks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

/**
 * Periodically checks whether the players on this server are live on
 * YouTube / Twitch / TikTok / Kick (needs internet) and announces it.
 */
public final class LivestreamDetectorHack extends Hack
        implements UpdateListener
{
        private static final int TICKS_PER_MINUTE = 20 * 60;

        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting autoScan =
                new CheckboxSetting("Auto scan", true);
        private final SliderSetting scanInterval =
                new SliderSetting("Scan interval (min)", 2, 1, 30, 1);
        private final CheckboxSetting scanOnlyJoin =
                new CheckboxSetting("Scan only players who join", false);
        private final SliderSetting maxPlayers =
                new SliderSetting("Skip scan above (players)", 80, 0, 500,
                        10);
        private final TextFieldSetting youtubeApiKey =
                new TextFieldSetting("YouTube API key", "");
        private final TextFieldSetting twitchClientId =
                new TextFieldSetting("Twitch client ID", "");
        private final TextFieldSetting twitchOAuthToken =
                new TextFieldSetting("Twitch OAuth token", "");

        private final AtomicBoolean scanning =
                new AtomicBoolean(false);
        private final Map<String, Set<String>> lastLiveByPlayer =
                new HashMap<String, Set<String>>();
        private final Set<String> lastSeenPlayers =
                new HashSet<String>();

        private int ticksSinceScan;
        private boolean warnedLargeServer;

        public LivestreamDetectorHack()
        {
                super("LivestreamDetector",
                        "Checks if players on this server are streaming on"
                                + " YouTube, Twitch, TikTok or Kick"
                                + " (needs internet).");
                setCategory(Category.INTEL);
                addSettings(autoScan, scanInterval, scanOnlyJoin,
                        maxPlayers, youtubeApiKey, twitchClientId,
                        twitchOAuthToken);
        }

        @Override
        public void onUpdate()
        {
                if(mc.getConnection() == null || mc.player == null)
                {
                        lastSeenPlayers.clear();
                        return;
                }

                Set<String> names = snapshotPlayers();

                boolean joined = lastSeenPlayers.isEmpty();
                Set<String> newPlayers = new HashSet<String>(names);
                newPlayers.removeAll(lastSeenPlayers);
                lastSeenPlayers.clear();
                lastSeenPlayers.addAll(names);

                if(!autoScan.isChecked())
                        return;

                if(scanOnlyJoin.isChecked() && !joined
                        && newPlayers.isEmpty())
                        return;

                if(!scanOnlyJoin.isChecked())
                {
                        ticksSinceScan++;
                        if(!joined && ticksSinceScan < scanInterval
                                .getValueI() * TICKS_PER_MINUTE)
                                return;
                }
                ticksSinceScan = 0;

                startScan(names);
        }

        private Set<String> snapshotPlayers()
        {
                Set<String> names = new HashSet<String>();
                for(NetworkPlayerInfo info :
                        mc.getConnection().getPlayerInfoMap())
                {
                        if(info.getGameProfile() != null
                                && info.getGameProfile().getName() != null)
                                names.add(info.getGameProfile().getName());
                }
                names.remove(mc.player.getGameProfile().getName());
                return names;
        }

        private void startScan(final Set<String> names)
        {
                if(!scanning.compareAndSet(false, true))
                        return;

                int limit = maxPlayers.getValueI();
                if(limit > 0 && names.size() > limit)
                {
                        if(!warnedLargeServer)
                        {
                                warnedLargeServer = true;
                                post("LivestreamDetector: scan skipped ("
                                        + names.size()
                                        + " players, limit " + limit + ").");
                        }
                        scanning.set(false);
                        return;
                }
                warnedLargeServer = false;

                Thread t = new Thread(new Runnable()
                {
                        @Override
                        public void run()
                        {
                                try
                                {
                                        for(String name : names)
                                        {
                                                if(!isEnabled())
                                                        break;
                                                checkPlayer(name);
                                        }
                                }catch(Throwable e)
                                {
                                        e.printStackTrace();
                                }finally
                                {
                                        scanning.set(false);
                                }
                        }
                }, "LivestreamDetector");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
        }

        private void checkPlayer(String name)
        {
                if(checkYouTube(name))
                        announce(name, "YouTube", "https://www.youtube.com/"
                                + name);
                if(checkTwitch(name))
                        announce(name, "Twitch",
                                "https://www.twitch.tv/" + name);
                if(checkTikTok(name))
                        announce(name, "TikTok", "https://www.tiktok.com/@"
                                + name + "/live");
                if(checkKick(name))
                        announce(name, "Kick", "https://kick.com/" + name);
        }

        private boolean checkYouTube(String name)
        {
                String key = youtubeApiKey.getValue().trim();
                if(!key.isEmpty())
                {
                        String body = httpGet(
                                "https://www.googleapis.com/youtube/v3"
                                        + "/search?part=snippet&eventType=live"
                                        + "&type=video&maxResults=1&q="
                                        + encode(name) + "&key=" + key);
                        if(body != null && body.contains("\"videoId\""))
                                return true;
                        if(body != null)
                                return false;
                }

                String body = httpGet("https://www.youtube.com/@" + name
                        + "/live");
                return body != null && (body.contains("\"isLiveNow\":true")
                        || (body.contains("\"isLive\":true")
                                && body.contains("\"liveStreamability\""))
                        || body.contains("LIVE NOW")
                        || body.contains("Live now"));
        }

        private boolean checkTwitch(String name)
        {
                String clientId = twitchClientId.getValue().trim();
                String token = twitchOAuthToken.getValue().trim();
                if(!clientId.isEmpty() && !token.isEmpty())
                {
                        Map<String, String> headers =
                                new HashMap<String, String>();
                        headers.put("Client-Id", clientId);
                        headers.put("Authorization", token.toLowerCase()
                                .startsWith("bearer ") ? token
                                : "Bearer " + token);
                        String body = httpGet(
                                "https://api.twitch.tv/helix/streams?user_login="
                                        + encode(name), headers);
                        if(body != null)
                                return body.contains("\"data\":[{");
                }

                String body = httpGet("https://www.twitch.tv/" + name);
                return body != null
                        && (body.contains("\"isLiveBroadcast\":true")
                                || (body.contains("\"stream\"")
                                        && body.contains("\"type\":\"live\""))
                                || body.contains("\"isLive\":true"));
        }

        private boolean checkTikTok(String name)
        {
                String body = httpGet("https://www.tiktok.com/@" + name
                        + "/live");
                return body != null
                        && (body.contains("\"isLive\":true")
                                || body.contains("\"is_live\":true")
                                || (body.contains("\"liveRoomId\"")
                                        && !body.contains("\"liveRoomId\":0")
                                        && !body.contains(
                                                "\"liveRoomId\":null")));
        }

        private boolean checkKick(String name)
        {
                String body = httpGet(
                        "https://kick.com/api/v2/channels/" + encode(name));
                return body != null && body.contains("\"is_live\":true");
        }

        private static String encode(String s)
        {
                try
                {
                        return URLEncoder.encode(s, "UTF-8");
                }catch(Exception e)
                {
                        return s;
                }
        }

        private static String httpGet(String url)
        {
                return httpGet(url, null);
        }

        private static String httpGet(String url,
                Map<String, String> headers)
        {
                try
                {
                        HttpURLConnection connection =
                                (HttpURLConnection)new URL(url)
                                        .openConnection();
                        connection.setConnectTimeout(8000);
                        connection.setReadTimeout(10000);
                        connection.setRequestProperty("User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                        connection.setRequestProperty("Accept-Language",
                                "en-US,en;q=0.9");
                        if(headers != null)
                                for(Map.Entry<String, String> header :
                                        headers.entrySet())
                                        connection.setRequestProperty(
                                                header.getKey(),
                                                header.getValue());

                        int status = connection.getResponseCode();
                        if(status == 404)
                                return "";
                        if(status / 100 != 2)
                                return null;

                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream(),
                                        "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        try
                        {
                                char[] buf = new char[2048];
                                int n;
                                while((n = br.read(buf)) > 0)
                                        sb.append(buf, 0, n);
                        }finally
                        {
                                br.close();
                        }
                        // cap at 256 KB - pages are bigger than needed
                        String body = sb.toString();
                        return body.length() > 262144
                                ? body.substring(0, 262144) : body;
                }catch(Exception e)
                {
                        return null;
                }
        }

        private void announce(String name, String platform, String url)
        {
                Set<String> platforms = lastLiveByPlayer.get(name);
                if(platforms != null && platforms.contains(platform))
                        return; // already announced

                if(platforms == null)
                        platforms = new HashSet<String>();
                platforms.add(platform);
                lastLiveByPlayer.put(name, platforms);

                post("LivestreamDetector: " + name + " is live on "
                        + platform + " - " + url);
        }

        private void post(final String message)
        {
                mc.addScheduledTask(new Runnable()
                {
                        @Override
                        public void run()
                        {
                                if(mc.player != null)
                                        mc.player.sendMessage(
                                                new TextComponentString(
                                                        "\u00a7e" + message));
                        }
                });
        }

        @Override
        protected void onEnable()
        {
                ticksSinceScan = 0;
                lastLiveByPlayer.clear();
                lastSeenPlayers.clear();
                warnedLargeServer = false;
        }

        @Override
        protected void onDisable()
        {
                lastLiveByPlayer.clear();
                lastSeenPlayers.clear();
        }
}
