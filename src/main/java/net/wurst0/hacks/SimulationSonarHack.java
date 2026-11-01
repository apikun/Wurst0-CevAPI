/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SimulationSonarHack (GPL-3.0-or-later)
 * Lite adaptation: the 26.x packet-level simulation evidence is not
 * available on 1.12.2, so this sonar measures client-visible entity
 * motion per chunk instead (farm / mob-activity detector).
 */
package net.wurst0.hacks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Measures entity motion per chunk and highlights the chunk with the
 * most activity ("simulation sonar" for farms and mob systems).
 */
public final class SimulationSonarHack extends Hack
        implements UpdateListener, RenderListener, ScreenRenderListener
{
        private static final class ChunkActivity
        {
                double score;
                long lastActive;
                double lastX;
                double lastY;
                double lastZ;
        }

        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting sensitivity =
                new SliderSetting("Sensitivity", 1, 0.1, 3, 0.1);
        private final SliderSetting decaySeconds =
                new SliderSetting("Decay time (s)", 10, 2, 60, 1);
        private final SliderSetting alertThreshold =
                new SliderSetting("Alert threshold", 50, 5, 500, 5);
        private final CheckboxSetting showBox =
                new CheckboxSetting("Show chunk box", true);
        private final CheckboxSetting showHudInfo =
                new CheckboxSetting("HUD info", true);
        private final CheckboxSetting chatAlerts =
                new CheckboxSetting("Chat alerts", true);

        private final Map<Long, ChunkActivity> activity =
                new HashMap<Long, ChunkActivity>();

        private long topChunkKey;
        private double topScore;
        private double topX;
        private double topY;
        private double topZ;
        private long lastAlert;

        public SimulationSonarHack()
        {
                super("SimulationSonar",
                        "Highlights chunks with the most entity activity"
                                + " (client-side motion sonar adapted from"
                                + " the 26.x simulation evidence sonar).");
                setCategory(Category.INTEL);
                addSettings(sensitivity, decaySeconds, alertThreshold,
                        showBox, showHudInfo, chatAlerts);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                {
                        activity.clear();
                        return;
                }

                long now = System.currentTimeMillis();
                double decayPerTick = 1.0
                        / (decaySeconds.getValue() * 20.0);

                // decay
                Iterator<Map.Entry<Long, ChunkActivity>> itr = activity
                        .entrySet().iterator();
                while(itr.hasNext())
                {
                        ChunkActivity chunk = itr.next().getValue();
                        chunk.score -= chunk.score * decayPerTick;
                        if(chunk.score < 0.1
                                || now - chunk.lastActive > 60000L)
                                itr.remove();
                }

                // measure motion
                Iterator<Entity> entities = mc.world.loadedEntityList
                        .iterator();
                while(entities.hasNext())
                {
                        Entity e = entities.next();
                        if(e == p || e.isDead
                                || !(e instanceof EntityLivingBase))
                                continue;

                        double dx = e.posX - e.lastTickPosX;
                        double dy = e.posY - e.lastTickPosY;
                        double dz = e.posZ - e.lastTickPosZ;
                        double motion = (Math.abs(dx) + Math.abs(dy)
                                + Math.abs(dz)) * sensitivity.getValue();
                        if(motion < 0.001)
                                continue;

                        long key = chunkKey(e.chunkCoordX, e.chunkCoordZ);
                        ChunkActivity chunk = activity.get(key);
                        if(chunk == null)
                        {
                                chunk = new ChunkActivity();
                                activity.put(key, chunk);
                        }
                        chunk.score += motion;
                        chunk.lastActive = now;
                        chunk.lastX = e.posX;
                        chunk.lastY = e.posY;
                        chunk.lastZ = e.posZ;
                }

                // find the hottest chunk
                topScore = 0;
                topChunkKey = Long.MIN_VALUE;
                for(Map.Entry<Long, ChunkActivity> entry : activity
                        .entrySet())
                {
                        if(entry.getValue().score > topScore)
                        {
                                topScore = entry.getValue().score;
                                topChunkKey = entry.getKey();
                                topX = entry.getValue().lastX;
                                topY = entry.getValue().lastY;
                                topZ = entry.getValue().lastZ;
                        }
                }

                if(chatAlerts.isChecked() && topScore >= alertThreshold
                        .getValue() && now - lastAlert > 30000L)
                {
                        lastAlert = now;
                        p.sendMessage(new TextComponentString(
                                "\u00a7e[SimulationSonar] Activity"
                                        + String.format(
                                                " %.0f at chunk %d, %d"
                                                        + " (%.0f, %.0f,"
                                                        + " %.0f)",
                                                topScore, chunkX(topChunkKey),
                                                chunkZ(topChunkKey), topX,
                                                topY, topZ)));
                }
        }

        private static long chunkKey(int x, int z)
        {
                return ((long)x << 32) ^ (z & 0xFFFFFFFFL);
        }

        private static int chunkX(long key)
        {
                return (int)(key >> 32);
        }

        private static int chunkZ(long key)
        {
                return (int)(key & 0xFFFFFFFFL);
        }

        @Override
        public void onRender(float partialTicks)
        {
                if(!showBox.isChecked() || topChunkKey == Long.MIN_VALUE
                        || mc.player == null)
                        return;

                int cx = chunkX(topChunkKey);
                int cz = chunkZ(topChunkKey);
                AxisAlignedBB box = new AxisAlignedBB(cx << 4, topY - 4,
                        cz << 4, (cx << 4) + 16, topY + 6, (cz << 4) + 16);

                RenderUtils.enable3D(2F, false);
                RenderUtils.applyCameraTransform(partialTicks);
                RenderUtils.drawBox(box, 1F, 0.9F, 0.2F, 0.7F);
                RenderUtils.disable3D(false);
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(!showHudInfo.isChecked() || mc.player == null
                        || topChunkKey == Long.MIN_VALUE)
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                mc.fontRenderer.drawStringWithShadow(String.format(
                        "\u00a7eSonar: \u00a7fchunk %d, %d (activity %.0f)",
                        chunkX(topChunkKey), chunkZ(topChunkKey), topScore),
                        2, sr.getScaledHeight() - 62, 0xFFFFFF);
        }

        public double getTopScore()
        {
                return topScore;
        }

        public int getTopChunkX()
        {
                return topChunkKey == Long.MIN_VALUE ? 0
                        : chunkX(topChunkKey);
        }

        public int getTopChunkZ()
        {
                return topChunkKey == Long.MIN_VALUE ? 0
                        : chunkZ(topChunkKey);
        }

        public boolean isHudInfoEnabled()
        {
                return showHudInfo.isChecked();
        }

        @Override
        protected void onEnable()
        {
                activity.clear();
                topChunkKey = Long.MIN_VALUE;
                topScore = 0;
                lastAlert = 0;
        }

        @Override
        protected void onDisable()
        {
                activity.clear();
                topChunkKey = Long.MIN_VALUE;
                topScore = 0;
        }
}
