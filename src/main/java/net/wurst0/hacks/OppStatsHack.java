/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI OppStatsHack (GPL-3.0-or-later)
 * Lite adaptation: the 26.x dossier screen becomes an in-memory
 * dossier (JSON file per server under config/wurst0/) plus a HUD list
 * of the nearest tracked players.
 */
package net.wurst0.hacks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.Loader;
import net.wurst0.Category;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Builds a per-server dossier of every player you meet: name, first
 * and last seen, join count, health, armor and held item. Persisted as
 * config/wurst0/oppstats-<server>.json.
 */
public final class OppStatsHack extends Hack
        implements UpdateListener, ScreenRenderListener
{
        private static final class Record
        {
                String name = "unknown";
                long firstSeen;
                long lastSeen;
                int joins;
                float lastHealth = -1F;
                int lastArmor = -1;
                String lastHeld = "N/A";
                double lastDistance = -1;
                boolean online;
        }

        private final Minecraft mc = Minecraft.getMinecraft();
        private final Gson gson = new Gson();

        private final SliderSetting trackRange =
                new SliderSetting("Track range", 64, 16, 128, 16);
        private final SliderSetting hudLines =
                new SliderSetting("HUD lines", 5, 0, 12, 1);
        private final CheckboxSetting joinLeaveAlerts =
                new CheckboxSetting("Join/leave alerts", true);

        private final Map<UUID, Record> records =
                new HashMap<UUID, Record>();
        private final Set<UUID> onlineTab = new HashSet<UUID>();

        private String serverKey = "";
        private File dataFile;
        private boolean dirty;
        private int timer;
        private int saveTimer;

        public OppStatsHack()
        {
                super("OppStats",
                        "Tracks players per server (health, armor, gear,"
                                + " joins) into a dossier file and shows"
                                + " the nearest ones on the HUD.");
                setCategory(Category.INTEL);
                addSettings(trackRange, hudLines, joinLeaveAlerts);
        }

        @Override
        public void onUpdate()
        {
                if(mc.player == null)
                {
                        if(!onlineTab.isEmpty() || !records.isEmpty())
                                saveIfNeeded();
                        onlineTab.clear();
                        return;
                }

                if(--timer > 0)
                        return;
                timer = 20;

                String keyNow = resolveServerKey();
                if(!keyNow.equals(serverKey))
                {
                        saveIfNeeded();
                        records.clear();
                        onlineTab.clear();
                        serverKey = keyNow;
                        dataFile = resolveDataFile(keyNow);
                        load();
                }
                if(dataFile == null)
                        dataFile = resolveDataFile(serverKey);

                long now = System.currentTimeMillis();

                // tab list: joins / leaves
                Set<UUID> nowOnline = new HashSet<UUID>();
                if(mc.getConnection() != null)
                {
                        for(NetworkPlayerInfo info : mc.getConnection()
                                .getPlayerInfoMap())
                        {
                                if(info.getGameProfile() == null)
                                        continue;
                                nowOnline.add(info.getGameProfile().getId());
                        }
                }

                for(UUID uuid : nowOnline)
                {
                        if(onlineTab.contains(uuid))
                                continue;
                        Record rec = recordFor(uuid, nameOf(uuid), now);
                        rec.joins++;
                        rec.online = true;
                        if(joinLeaveAlerts.isChecked()
                                && !onlineTab.isEmpty())
                                alert("\u00a7a[OppStats] " + rec.name
                                        + " joined ("
                                        + rec.joins + ". time).");
                }

                for(UUID uuid : onlineTab)
                {
                        if(nowOnline.contains(uuid))
                                continue;
                        Record rec = records.get(uuid);
                        if(rec != null)
                                rec.online = false;
                        if(joinLeaveAlerts.isChecked())
                                alert("\u00a7e[OppStats] "
                                        + (rec == null ? "A player" : rec.name)
                                        + " left.");
                }
                onlineTab.clear();
                onlineTab.addAll(nowOnline);

                // live stats for loaded players in range
                double rangeSq = trackRange.getValue() * trackRange.getValue();
                for(Object obj : mc.world.playerEntities)
                {
                        if(!(obj instanceof EntityPlayer))
                                continue;
                        EntityPlayer other = (EntityPlayer)obj;
                        if(other == mc.player)
                                continue;

                        double distSq = other.getDistanceSq(mc.player.posX,
                                mc.player.posY, mc.player.posZ);
                        if(distSq > rangeSq)
                                continue;

                        Record rec = recordFor(other.getUniqueID(),
                                other.getName(), now);
                        rec.lastSeen = now;
                        rec.lastHealth = other.getHealth();
                        rec.lastArmor = other.getTotalArmorValue();
                        rec.lastDistance = Math.sqrt(distSq);
                        ItemStack held = other.getHeldItemMainhand();
                        rec.lastHeld = held == null || held.isEmpty()
                                ? "N/A" : held.getDisplayName();
                        rec.online = true;
                        dirty = true;
                }

                if(++saveTimer >= 30)
                {
                        saveTimer = 0;
                        saveIfNeeded();
                }
        }

        private Record recordFor(UUID uuid, String name, long now)
        {
                Record rec = records.get(uuid);
                if(rec == null)
                {
                        rec = new Record();
                        rec.firstSeen = now;
                        rec.lastSeen = now;
                        records.put(uuid, rec);
                        dirty = true;
                }
                if(name != null && !name.isEmpty())
                        rec.name = name;
                return rec;
        }

        private String nameOf(UUID uuid)
        {
                if(mc.getConnection() == null)
                        return null;
                for(NetworkPlayerInfo info : mc.getConnection()
                        .getPlayerInfoMap())
                        if(info.getGameProfile() != null && info
                                .getGameProfile().getId().equals(uuid))
                                return info.getGameProfile().getName();
                return null;
        }

        private String resolveServerKey()
        {
                ServerData server = mc.getCurrentServerData();
                return server == null ? "singleplayer" : server.serverIP;
        }

        private List<Record> nearestOnline(int max)
        {
                List<Record> list = new ArrayList<Record>();
                for(Record rec : records.values())
                        if(rec.online && rec.lastDistance >= 0)
                                list.add(rec);
                list.sort(new java.util.Comparator<Record>()
                {
                        @Override
                        public int compare(Record a, Record b)
                        {
                                return Double.compare(a.lastDistance,
                                        b.lastDistance);
                        }
                });
                return list.size() > max
                        ? list.subList(0, max) : list;
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(mc.player == null || hudLines.getValueI() <= 0)
                        return;

                List<Record> list = nearestOnline(
                        hudLines.getValueI());
                if(list.isEmpty())
                        return;

                int y = 2;
                mc.fontRenderer.drawStringWithShadow(
                        "\u00a7eOppStats \u00a77(" + records.size()
                                + " tracked)", 2, y, 0xFFFFFF);
                y += 10;

                for(Record rec : list)
                {
                        String line = String.format(
                                "\u00a7f%s \u00a77%.0fhp armor=%d %s %.0fm",
                                rec.name, rec.lastHealth, rec.lastArmor,
                                rec.lastHeld, rec.lastDistance);
                        mc.fontRenderer.drawStringWithShadow(line, 2, y,
                                0xE0E0E0);
                        y += 10;
                        if(y > 120)
                                break;
                }
        }

        private void alert(String message)
        {
                if(mc.player != null)
                        mc.player.sendMessage(
                                new TextComponentString(message));
        }

        private File resolveDataFile(String key)
        {
                try
                {
                        File dir = new File(
                                Loader.instance().getConfigDir(), "wurst0");
                        if(!dir.exists())
                                dir.mkdirs();
                        String safe = key.replaceAll("[^A-Za-z0-9._-]", "_");
                        return new File(dir, "oppstats-" + safe + ".json");
                }catch(Throwable t)
                {
                        return null;
                }
        }

        private void saveIfNeeded()
        {
                if(!dirty || dataFile == null)
                        return;
                dirty = false;

                try
                {
                        JsonObject root = new JsonObject();
                        root.addProperty("server", serverKey);
                        JsonArray arr = new JsonArray();
                        for(Map.Entry<UUID, Record> entry : records
                                .entrySet())
                        {
                                Record rec = entry.getValue();
                                JsonObject obj = new JsonObject();
                                obj.addProperty("uuid",
                                        entry.getKey().toString());
                                obj.addProperty("name", rec.name);
                                obj.addProperty("firstSeen", rec.firstSeen);
                                obj.addProperty("lastSeen", rec.lastSeen);
                                obj.addProperty("joins", rec.joins);
                                obj.addProperty("lastHealth", rec.lastHealth);
                                obj.addProperty("lastArmor", rec.lastArmor);
                                obj.addProperty("lastHeld", rec.lastHeld);
                                arr.add(obj);
                        }
                        root.add("players", arr);

                        Writer w = new OutputStreamWriter(
                                new FileOutputStream(dataFile),
                                StandardCharsets.UTF_8);
                        try
                        {
                                gson.toJson(root, w);
                        }finally
                        {
                                w.close();
                        }
                }catch(Throwable t)
                {
                        // disk problems must never crash the client
                }
        }

        private void load()
        {
                if(dataFile == null || !dataFile.exists())
                        return;

                try
                {
                        Reader reader = new InputStreamReader(
                                new FileInputStream(dataFile),
                                StandardCharsets.UTF_8);
                        JsonObject root;
                        try
                        {
                                root = new JsonParser().parse(reader)
                                        .getAsJsonObject();
                        }finally
                        {
                                reader.close();
                        }

                        JsonArray arr = root.getAsJsonArray("players");
                        if(arr == null)
                                return;

                        long now = System.currentTimeMillis();
                        for(JsonElement element : arr)
                        {
                                JsonObject obj = element.getAsJsonObject();
                                try
                                {
                                        UUID uuid = UUID.fromString(obj
                                                .get("uuid").getAsString());
                                        Record rec = new Record();
                                        rec.name = obj.get("name")
                                                .getAsString();
                                        rec.firstSeen = obj.get("firstSeen")
                                                .getAsLong();
                                        rec.lastSeen = obj.get("lastSeen")
                                                .getAsLong();
                                        rec.joins = obj.get("joins")
                                                .getAsInt();
                                        rec.lastHealth = obj
                                                .get("lastHealth")
                                                .getAsFloat();
                                        rec.lastArmor = obj.get("lastArmor")
                                                .getAsInt();
                                        rec.lastHeld = obj.get("lastHeld")
                                                .getAsString();
                                        records.put(uuid, rec);
                                }catch(Throwable ignored)
                                {
                                        // skip malformed entries
                                }
                        }
                        for(Record rec : records.values())
                                if(now - rec.lastSeen < 60000L)
                                        rec.online = false;
                }catch(Throwable t)
                {
                        // unreadable file: start a fresh dossier
                }
        }

        @Override
        protected void onEnable()
        {
                timer = 0;
                saveTimer = 0;
                serverKey = resolveServerKey();
                dataFile = resolveDataFile(serverKey);
                records.clear();
                onlineTab.clear();
                load();
        }

        @Override
        protected void onDisable()
        {
                saveIfNeeded();
        }
}
