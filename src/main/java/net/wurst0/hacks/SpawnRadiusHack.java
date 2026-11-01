/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SpawnRadiusHack (GPL-3.0-or-later)
 * Lite adaptation: the original renders exact per-spawner range
 * circles (26.x render pipeline). This port keeps the spawner idea in
 * a HUD readout (scanned tile entities + reflected
 * activatingRangeFromPlayer) and adds the spawnRadius gamerule setter
 * plus a periodic /gamerule spawnRadius query, whose chat answer
 * shows the current value.
 */
package net.wurst0.hacks;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class SpawnRadiusHack extends Hack
        implements UpdateListener, ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private static final Field RANGE_FIELD;
        static
        {
                Field field = null;
                try
                {
                        field = net.minecraft.tileentity.MobSpawnerBaseLogic.class
                                .getDeclaredField("activatingRangeFromPlayer");
                        field.setAccessible(true);
                }catch(Exception ignored)
                {
                        field = null;
                }
                RANGE_FIELD = field;
        }

        private final SliderSetting newRadius =
                new SliderSetting("New radius", 10, 0, 100, 1);
        private final CheckboxSetting applyOnEnable =
                new CheckboxSetting("Apply on enable", true);
        private final CheckboxSetting repeatQuery =
                new CheckboxSetting("Repeat query", true);
        private final SliderSetting queryInterval =
                new SliderSetting("Query interval (s)", 10, 1, 60, 1);

        private int queryTimer;

        public SpawnRadiusHack()
        {
                super("SpawnRadius",
                        "HUD showing nearby mob spawners and their activation"
                                + " ranges; can set the spawnRadius gamerule and"
                                + " queries its current value in chat.");
                setCategory(Category.RENDER);
                addSettings(newRadius, applyOnEnable, repeatQuery,
                        queryInterval);
        }

        @Override
        protected void onEnable()
        {
                queryTimer = 0;

                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                        return;

                if(applyOnEnable.isChecked())
                {
                        p.sendChatMessage("/gamerule spawnRadius "
                                + newRadius.getValueI());
                        p.sendMessage(new TextComponentString(
                                "\u00a7a[SpawnRadius]\u00a7r Sent /gamerule"
                                        + " spawnRadius "
                                        + newRadius.getValueI()));
                }
                queryCurrent(p);
                queryTimer = queryInterval.getValueI() * 20;
        }

        @Override
        protected void onDisable()
        {
                queryTimer = 0;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                        return;

                if(!repeatQuery.isChecked())
                        return;
                if(queryTimer-- > 0)
                        return;
                queryTimer = queryInterval.getValueI() * 20;
                queryCurrent(p);
        }

        private void queryCurrent(EntityPlayerSP p)
        {
                // vanilla replies "Gamerule spawnRadius is currently: N"
                p.sendChatMessage("/gamerule spawnRadius");
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                int y = sr.getScaledHeight() - 42;
                int color = 0x80FF80;

                mc.fontRenderer.drawStringWithShadow(
                        "SpawnRadius target: " + newRadius.getValueI()
                                + " (applied: "
                                + (applyOnEnable.isChecked() ? "yes" : "no")
                                + ")", 2, y, color);
                y -= 10;

                int total = 0;
                int active = 0;
                int maxRange = 0;
                for(Object obj : mc.world.loadedTileEntityList)
                {
                        if(!(obj instanceof TileEntityMobSpawner))
                                continue;
                        total++;
                        TileEntityMobSpawner spawner =
                                (TileEntityMobSpawner)obj;
                        int range = readRange(spawner);
                        if(range > maxRange)
                                maxRange = range;

                        BlockPos pos = spawner.getPos();
                        double dx = pos.getX() + 0.5 - p.posX;
                        double dy = pos.getY() + 0.5 - p.posY;
                        double dz = pos.getZ() + 0.5 - p.posZ;
                        if(dx * dx + dy * dy + dz * dz
                                <= (double)range * range)
                                active++;
                }

                mc.fontRenderer.drawStringWithShadow(
                        "Spawners: " + active + "/" + total
                                + " in range (max " + maxRange + ")",
                        2, y, color);
        }

        private static int readRange(TileEntityMobSpawner spawner)
        {
                if(RANGE_FIELD == null)
                        return 16;
                try
                {
                        return RANGE_FIELD
                                .getInt(spawner.getSpawnerBaseLogic());
                }catch(Exception ignored)
                {
                        return 16;
                }
        }
}
