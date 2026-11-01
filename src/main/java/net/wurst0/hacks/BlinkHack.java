/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BlinkHack (GPL-3.0-or-later)
 *
 * 1.12.2 adaptation: holds ALL outgoing packets through PacketTap
 * while enabled and flushes everything on disable (pure blink).
 * The double flush below works around PacketTap's flush-while-
 * holding race: the second flushOutbound() releases any packets
 * that were re-captured before the hold flag dropped.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.wurst0.Category;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.PacketTap;

public final class BlinkHack extends Hack
        implements UpdateListener, ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting limit = new SliderSetting("Limit", 0, 0,
                500, 1); // 0 = no limit

        private static final String HUD_TEXT_PREFIX = "Blink: ";

        public BlinkHack()
        {
                super("Blink",
                        "Suspends all outgoing packets while enabled,"
                                + " making you appear frozen, then sends"
                                + " them all at once when disabled.");
                setCategory(Category.MOVEMENT);
                addSetting(limit);
        }

        @Override
        public void onUpdate()
        {
                if(mc.getConnection() == null || mc.player == null)
                {
                        if(isEnabled())
                                setEnabled(false);
                        return;
                }

                PacketTap.ensureInstalled();

                int maxPackets = limit.getValueI();
                if(maxPackets > 0
                        && PacketTap.heldCount() >= maxPackets)
                {
                        // auto-restart: flush everything, resume holding
                        PacketTap.setHoldOutbound(false);
                        PacketTap.flushOutbound();
                        PacketTap.setHoldOutbound(true);
                }
        }

        @Override
        protected void onEnable()
        {
                PacketTap.ensureInstalled();
                PacketTap.setHoldOutbound(true);
        }

        @Override
        protected void onDisable()
        {
                PacketTap.setHoldOutbound(false); // flushes, stops holding
                PacketTap.flushOutbound(); // releases race re-captures
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(mc.player == null || mc.getConnection() == null)
                        return;

                int held = PacketTap.heldCount();
                String text;
                if(limit.getValueI() == 0)
                        text = HUD_TEXT_PREFIX + held + " packets held";
                else
                        text = HUD_TEXT_PREFIX + held + "/" + limit.getValueI()
                                + " packets held";

                ScaledResolution sr = new ScaledResolution(mc);
                int x = sr.getScaledWidth() / 2
                        - mc.fontRenderer.getStringWidth(text) / 2;
                mc.fontRenderer.drawStringWithShadow(text, x,
                        sr.getScaledHeight() / 2 + 32, 0x80FF80);
        }
}
