/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI PacketRateHack (GPL-3.0-or-later)
 *
 * 1.12.2 adaptation: measures and displays inbound (S2C) packets per
 * second using the PacketTap inbound filter, plus the number of
 * currently held/delayed outbound packets. The original also counts
 * and rate-limits outgoing packets; PacketTap has no outbound
 * counting hook, so C2S rate measurement is not supported here.
 */
package net.wurst0.hacks;

import java.util.ArrayDeque;
import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.wurst0.Category;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.util.PacketTap;

public final class PacketRateHack extends Hack
        implements UpdateListener, ScreenRenderListener, PacketTap.InboundFilter
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting showHud =
                new CheckboxSetting("Show HUD", true);

        /** Arrival timestamps of inbound packets (sliding 1s window). */
        private final ArrayDeque<Long> inboundTimes = new ArrayDeque<Long>();

        public PacketRateHack()
        {
                super("PacketRate",
                        "Measures and displays inbound packets per second"
                                + " on the HUD. Outbound packets are not"
                                + " counted (no PacketTap hook); the held"
                                + " outbound queue size is shown instead.");
                setCategory(Category.TOOLS);
                addSetting(showHud);
        }

        @Override
        public int onInbound(Object packet)
        {
                synchronized(inboundTimes)
                {
                        inboundTimes.addLast(
                                Long.valueOf(System.currentTimeMillis()));
                }
                return PacketTap.INBOUND_PASS;
        }

        private int countInboundPerSecond()
        {
                long cutoff = System.currentTimeMillis() - 1000;
                int count = 0;
                synchronized(inboundTimes)
                {
                        Iterator<Long> itr = inboundTimes.iterator();
                        while(itr.hasNext())
                                if(itr.next().longValue() >= cutoff)
                                        count++;
                }
                return count;
        }

        private void pruneInbound()
        {
                long cutoff = System.currentTimeMillis() - 1000;
                synchronized(inboundTimes)
                {
                        while(!inboundTimes.isEmpty()
                                && inboundTimes.peekFirst().longValue() < cutoff)
                                inboundTimes.removeFirst();
                }
        }

        @Override
        public void onUpdate()
        {
                if(mc.getConnection() == null)
                {
                        if(isEnabled())
                                setEnabled(false);
                        return;
                }

                PacketTap.ensureInstalled();
                PacketTap.setInboundFilter(this);
                pruneInbound();
        }

        @Override
        protected void onEnable()
        {
                synchronized(inboundTimes)
                {
                        inboundTimes.clear();
                }
                PacketTap.ensureInstalled();
                PacketTap.setInboundFilter(this);
        }

        @Override
        protected void onDisable()
        {
                // the filter slot is global: hand it back
                PacketTap.setInboundFilter(null);
                synchronized(inboundTimes)
                {
                        inboundTimes.clear();
                }
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(!showHud.isChecked() || mc.player == null
                        || mc.getConnection() == null)
                        return;

                int pps = countInboundPerSecond();
                int held = PacketTap.heldCount();
                String text = "PacketRate: " + pps + " pps (S2C)";
                if(held > 0)
                        text += ", " + held + " held (C2S)";
                ScaledResolution sr = new ScaledResolution(mc);
                int x = sr.getScaledWidth() / 2
                        - mc.fontRenderer.getStringWidth(text) / 2;
                mc.fontRenderer.drawStringWithShadow(text, x,
                        sr.getScaledHeight() / 2 + 22, 0x80FF80);
        }
}
