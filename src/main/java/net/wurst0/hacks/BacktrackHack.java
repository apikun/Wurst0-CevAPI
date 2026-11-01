/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BacktrackHack (GPL-3.0-or-later)
 *
 * 1.12.2 adaptation: the original queues enemy movement packets and
 * replays them after a delay. Queued S2C replay is not possible with
 * PacketTap (it can only pass or drop inbound packets), so this port
 * rubber-bands instead: after you take damage it drops the server's
 * SPacketPlayerPosLook packets for a short window. The server keeps
 * its authoritative position and rejects our movement meanwhile; when
 * the window ends, the next PosLook gets through and snaps us back -
 * pulling the fight toward past positions. Keep windows short.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.PacketTap;

public final class BacktrackHack extends Hack
        implements UpdateListener, PacketTap.InboundFilter
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting delayTicks = new SliderSetting(
                "Delay (ticks)", 10, 1, 40, 1);

        private volatile boolean dropping;
        private int windowTicks;
        private int prevHurtTime;

        public BacktrackHack()
        {
                super("Backtrack",
                        "Rubber-bands you toward past positions after being"
                                + " hit by dropping position packets for a"
                                + " short time, desyncing attackers.");
                setCategory(Category.COMBAT);
                addSetting(delayTicks);
        }

        @Override
        public int onInbound(Object packet)
        {
                if(dropping && packet instanceof SPacketPlayerPosLook)
                        return PacketTap.INBOUND_DROP;
                return PacketTap.INBOUND_PASS;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                {
                        dropping = false;
                        windowTicks = 0;
                        prevHurtTime = 0;
                        return;
                }

                PacketTap.ensureInstalled();
                PacketTap.setInboundFilter(this);

                // start a window when we just took damage
                if(prevHurtTime == 0 && p.hurtTime > 0)
                {
                        windowTicks = delayTicks.getValueI();
                        dropping = true;
                }
                prevHurtTime = p.hurtTime;

                if(windowTicks > 0 && --windowTicks == 0)
                        dropping = false;
        }

        @Override
        protected void onEnable()
        {
                windowTicks = 0;
                prevHurtTime = 0;
                PacketTap.ensureInstalled();
        }

        @Override
        protected void onDisable()
        {
                dropping = false;
                windowTicks = 0;
                // the filter slot is global: hand it back
                PacketTap.setInboundFilter(null);
        }
}
