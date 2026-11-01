/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI PacketDelayHack (GPL-3.0-or-later)
 *
 * 1.12.2 adaptation: delays outgoing (C2S) packets via the PacketTap
 * netty pipeline. The original also queues incoming (S2C) packets;
 * PacketTap has no inbound queue, so S2C delay is not supported here.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.wurst0.Category;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.PacketTap;

public final class PacketDelayHack extends Hack
        implements UpdateListener, ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting delay =
                new SliderSetting("Delay (ms)", 100, 0, 1000, 25);
        private final CheckboxSetting showStatus =
                new CheckboxSetting("Show status text", true);

        public PacketDelayHack()
        {
                super("PacketDelay",
                        "Delays outgoing packets (a manual lag switch)."
                                + " Only C2S packets can be delayed; S2C"
                                + " delay is not supported by PacketTap.");
                setCategory(Category.TOOLS);
                addSettings(delay, showStatus);
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
                PacketTap.setOutboundDelay(delay.getValueI());
        }

        @Override
        protected void onEnable()
        {
                PacketTap.ensureInstalled();
                PacketTap.setOutboundDelay(delay.getValueI());
        }

        @Override
        protected void onDisable()
        {
                PacketTap.setOutboundDelay(0);
                PacketTap.flushOutbound();
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(!showStatus.isChecked() || mc.player == null
                        || mc.getConnection() == null)
                        return;

                String text = "PacketDelay | queued: " + PacketTap.heldCount()
                        + " | delay: " + delay.getValueI() + " ms";
                ScaledResolution sr = new ScaledResolution(mc);
                int x = sr.getScaledWidth() / 2
                        - mc.fontRenderer.getStringWidth(text) / 2;
                mc.fontRenderer.drawStringWithShadow(text, x,
                        sr.getScaledHeight() / 2 + 12, 0xFFFFFFFF);
        }
}
