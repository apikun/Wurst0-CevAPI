/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TelemetricsHack (GPL-3.0-or-later)
 * Lite adaptation: live server/session info HUD.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

public final class TelemetricsHack extends Hack implements ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting showPing =
                new CheckboxSetting("Ping", true);
        private final CheckboxSetting showCounts =
                new CheckboxSetting("Entities/chunks", true);
        private final CheckboxSetting showPos =
                new CheckboxSetting("Coordinates", true);

        public TelemetricsHack()
        {
                super("Telemetrics",
                        "Shows server IP, ping, entity/chunk counts and your"
                                + " coordinates on the HUD.");
                setCategory(Category.INTEL);
                addSettings(showPing, showCounts, showPos);
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                int y = sr.getScaledHeight() - 32;
                int color = 0x80FF80;

                if(showPos.isChecked())
                {
                        mc.fontRenderer.drawStringWithShadow(String.format(
                                "XYZ %.1f / %.1f / %.1f",
                                p.posX, p.posY, p.posZ), 2, y, color);
                        y -= 10;
                }

                if(showCounts.isChecked())
                {
                        int entities = mc.world.loadedEntityList.size();
                        int players = 0;
                        for(int i = 0; i < mc.world.playerEntities.size(); i++)
                                players++;
                        mc.fontRenderer.drawStringWithShadow(
                                entities + " entities, " + players
                                        + " players", 2, y, color);
                        y -= 10;
                }

                if(showPing.isChecked())
                {
                        ServerData server = mc.getCurrentServerData();
                        if(server != null)
                        {
                                mc.fontRenderer.drawStringWithShadow(
                                        server.serverIP + " ("
                                                + server.pingToServer
                                                + " ms)", 2, y, color);
                        }
                        else
                        {
                                mc.fontRenderer.drawStringWithShadow(
                                        "Singleplayer", 2, y, color);
                        }
                }
        }
}
