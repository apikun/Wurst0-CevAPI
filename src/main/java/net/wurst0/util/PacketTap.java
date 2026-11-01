/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Shared packet-tap infrastructure.
 *
 * Injects a ChannelDuplexHandler ("wurst0_tap") into the client
 * NetworkManager's netty pipeline, sitting right before "packet_handler".
 * That gives us:
 *  - inbound: channelRead() sees every server->client packet first and can
 *    drop it (used by Backtrack for rubber-banding).
 *  - outbound: write() sees every client->server packet before the encoder
 *    and can hold (queue) it (used by Blink, FakeLag, PacketDelay,
 *    PacketRate).
 *
 * Grim-safe features (activated by GrimBypassHack):
 *  - VITAL passthrough: transaction confirmations (CPacketConfirmTransaction)
 *    and keepalive responses (CPacketKeepAlive) are never held, even while
 *    Blink/FakeLag are holding everything else. Grim's TransactionOrder and
 *    floating-transaction tracking never sees them stall.
 *  - Post-safe ordering: Grim's Post check flags ATTACK / USE_ITEM / DIG /
 *    CLICK_WINDOW / ABILITIES / HELD_ITEM / ENTITY_ACTION packets that are
 *    sent after a "flying" (tick) packet but before the next transaction
 *    response. We queue those and release them right after a transaction
 *    response passes (or right before the next flying packet), which lands
 *    them in the exact slot vanilla clients use.
 *  - Hold timeout: while holding, a cyclic flush bounds how long position
 *    packets may stall, dodging Grim's BadPacketsR ("stopped sending
 *    position updates while still responding to transactions").
 *
 * Java 8, Forge 1.12.2. No mixins: pure netty pipeline manipulation.
 */
package net.wurst0.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.Sys;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketConfirmTransaction;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketKeepAlive;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerAbilities;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;

public final class PacketTap
{
        public static final int INBOUND_PASS = 0;
        public static final int INBOUND_DROP = 1;

        public interface InboundFilter
        {
                int onInbound(Object packet);
        }

        private static final String HANDLER_NAME = "wurst0_tap";

        private static volatile boolean installed;
        private static volatile boolean holdingOutbound;
        private static volatile long outboundDelayMs;
        private static volatile InboundFilter inboundFilter;

        // grim-safe state
        private static volatile boolean vitalPassthrough;
        private static volatile boolean postSafe;
        private static volatile long holdTimeoutMs;
        private static volatile boolean flyingSinceTrans;

        private static final List<Object> heldOutbound =
                new ArrayList<Object>();
        private static final List<DelayedPacket> delayedOutbound =
                new ArrayList<DelayedPacket>();
        private static final List<Object> postQueue =
                new ArrayList<Object>();
        private static long holdStartTime;

        private PacketTap()
        {}

        /** Installs the tap if not present. Safe to call every tick. */
        public static void ensureInstalled()
        {
                Minecraft mc = Minecraft.getMinecraft();
                NetHandlerPlayClient conn = mc.getConnection();
                if(conn == null)
                {
                        installed = false;
                        return;
                }

                if(installed)
                        return;

                try
                {
                        NetworkManager manager = conn.getNetworkManager();
                        Channel channel = manager.channel();
                        if(channel == null || channel.pipeline() == null)
                                return;

                        if(channel.pipeline().get(HANDLER_NAME) != null)
                        {
                                installed = true;
                                return;
                        }

                        channel.eventLoop().execute(new Runnable()
                        {
                                @Override
                                public void run()
                                {
                                        try
                                        {
                                                Channel ch = manager.channel();
                                                if(ch != null && ch.pipeline() != null
                                                        && ch.pipeline().get(HANDLER_NAME) == null)
                                                {
                                                        ch.pipeline().addBefore("packet_handler",
                                                                HANDLER_NAME, new TapHandler());
                                                }
                                                installed = true;
                                        }catch(Throwable t)
                                        {
                                                System.err.println("[Wurst0] PacketTap install "
                                                        + "failed: " + t);
                                        }
                                }
                        });
                }catch(Throwable t)
                {
                        System.err.println("[Wurst0] PacketTap error: " + t);
                }
        }

        public static boolean isInstalled()
        {
                return installed;
        }

        /** Starts/stops holding all outbound packets. On stop, flushes. */
        public static void setHoldOutbound(boolean hold)
        {
                if(hold && !holdingOutbound)
                        holdStartTime = now();
                if(!hold && holdingOutbound)
                        flushOutbound();
                holdingOutbound = hold;
        }

        public static boolean isHoldingOutbound()
        {
                return holdingOutbound;
        }

        /** Sets a per-packet outbound delay in ms (0 = off). */
        public static void setOutboundDelay(long delayMs)
        {
                outboundDelayMs = delayMs;
        }

        public static long getOutboundDelay()
        {
                return outboundDelayMs;
        }

        public static void setInboundFilter(InboundFilter filter)
        {
                inboundFilter = filter;
        }

        /**
         * Grim-safe: never hold transaction/keepalive responses
         * (Blink & FakeLag stay transparent to Grim's transaction tracking).
         */
        public static void setVitalPassthrough(boolean on)
        {
                vitalPassthrough = on;
        }

        /**
         * Grim-safe: reorder action packets into the post-transaction slot
         * vanilla clients use (defeats Grim's Post check).
         */
        public static void setPostSafe(boolean on)
        {
                postSafe = on;
                if(!on)
                        flushPostQueue();
        }

        /**
         * Grim-safe: while holding, auto-flush every N ms so position
         * packets never stall for long (defeats BadPacketsR). 0 = off.
         */
        public static void setHoldTimeoutMs(long ms)
        {
                holdTimeoutMs = ms;
        }

        /** True if this outbound packet must never be held or delayed. */
        public static boolean isVitalPacket(Object msg)
        {
                return msg instanceof CPacketConfirmTransaction
                        || msg instanceof CPacketKeepAlive;
        }

        /**
         * Packets Grim's Post check watches for when they appear between
         * a flying packet and the next transaction response.
         */
        public static boolean isPostSuspectPacket(Object msg)
        {
                return msg instanceof CPacketUseEntity
                        || msg instanceof CPacketAnimation
                        || msg instanceof CPacketPlayerDigging
                        || msg instanceof CPacketClickWindow
                        || msg instanceof CPacketPlayerAbilities
                        || msg instanceof CPacketHeldItemChange
                        || msg instanceof CPacketEntityAction
                        || msg instanceof CPacketPlayerTryUseItem
                        || msg instanceof CPacketPlayerTryUseItemOnBlock;
        }

        public static boolean isFlyingPacket(Object msg)
        {
                return msg instanceof CPacketPlayer;
        }

        /** Sends all held outbound packets immediately, in order. */
        public static synchronized void flushOutbound()
        {
                Iterator<Object> itr = heldOutbound.iterator();
                while(itr.hasNext())
                {
                        sendLater(itr.next());
                        itr.remove();
                }
                Iterator<DelayedPacket> itr2 = delayedOutbound.iterator();
                while(itr2.hasNext())
                {
                        sendLater(itr2.next().packet);
                        itr2.remove();
                }
        }

        /** Number of packets currently held/delayed outbound. */
        public static synchronized int heldCount()
        {
                return heldOutbound.size() + delayedOutbound.size();
        }

        private static synchronized void queuePost(Object packet)
        {
                postQueue.add(packet);
                if(postQueue.size() > 64)
                        flushPostQueue();
        }

        private static synchronized void flushPostQueue()
        {
                Iterator<Object> itr = postQueue.iterator();
                while(itr.hasNext())
                {
                        sendLater(itr.next());
                        itr.remove();
                }
        }

        private static void sendLater(Object packet)
        {
                try
                {
                        Minecraft mc = Minecraft.getMinecraft();
                        NetHandlerPlayClient conn = mc.getConnection();
                        if(conn != null)
                                // bypass our own tap: write directly through the network
                                // manager is fine, the tap forwards on its own too, but the
                                // netHandler path is the simplest safe entry point
                                conn.sendPacket((Packet<?>)packet);
                }catch(Throwable t)
                {
                        System.err.println("[Wurst0] PacketTap flush error: " + t);
                }
        }

        private static final class DelayedPacket
        {
                final Object packet;
                final long dueTime;

                DelayedPacket(Object packet, long dueTime)
                {
                        this.packet = packet;
                        this.dueTime = dueTime;
                }
        }

        private static final class TapHandler extends ChannelDuplexHandler
        {
                @Override
                public void write(ChannelHandlerContext ctx, Object msg,
                        ChannelPromise promise) throws Exception
                {
                        if(!(msg instanceof Packet))
                        {
                                super.write(ctx, msg, promise);
                                return;
                        }

                        // 1) vital packets always pass first, even while holding:
                        //    Grim must see transaction/keepalive answers instantly.
                        if(vitalPassthrough && isVitalPacket(msg))
                        {
                                super.write(ctx, msg, promise);
                                if(postSafe)
                                        flushPostQueue();
                                return;
                        }

                        long delay = outboundDelayMs;
                        if(holdingOutbound)
                        {
                                synchronized(PacketTap.class)
                                {
                                        heldOutbound.add(msg);
                                }
                                promise.setSuccess();
                                return;
                        }

                        // 2) post-safe ordering: land action packets after a
                        //    transaction response, never after a flying packet.
                        if(postSafe && isPostSuspectPacket(msg) && flyingSinceTrans)
                        {
                                queuePost(msg);
                                promise.setSuccess();
                                return;
                        }

                        if(delay > 0)
                        {
                                synchronized(PacketTap.class)
                                {
                                        delayedOutbound.add(new DelayedPacket(msg,
                                                Sys.getTime() + delay * Sys.getTimerResolution()
                                                        / 1000));
                                }
                                promise.setSuccess();
                                return;
                        }

                        // 3) before a flying packet goes out, drain leftover
                        //    action packets so they stay in the safe window.
                        if(isFlyingPacket(msg))
                        {
                                if(postSafe && !postQueue.isEmpty())
                                        flushPostQueue();
                                flyingSinceTrans = true;
                        }

                        super.write(ctx, msg, promise);
                }

                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg)
                        throws Exception
                {
                        InboundFilter filter = inboundFilter;
                        if(filter != null && msg instanceof Packet)
                        {
                                int action = INBOUND_PASS;
                                try
                                {
                                        action = filter.onInbound(msg);
                                }catch(Throwable t)
                                {
                                        System.err.println("[Wurst0] inbound filter error: " + t);
                                }
                                if(action == INBOUND_DROP)
                                        return; // swallow: never reaches NetworkManager
                        }
                        super.channelRead(ctx, msg);
                }
        }

        /** Ticks delayed outbound packets (call once per client tick). */
        public static synchronized void tick()
        {
                if(delayedOutbound.isEmpty() && holdTimeoutMs <= 0)
                        return;

                long now = now();
                Iterator<DelayedPacket> itr = delayedOutbound.iterator();
                while(itr.hasNext())
                {
                        DelayedPacket dp = itr.next();
                        if(now >= dp.dueTime)
                        {
                                sendLater(dp.packet);
                                itr.remove();
                        }
                }

                // cyclic hold flush: never let position packets stall too long
                if(holdingOutbound && holdTimeoutMs > 0
                        && now - holdStartTime >= holdTimeoutMs)
                {
                        flushOutbound();
                        holdStartTime = now;
                }
        }

        private static long now()
        {
                return Sys.getTime() * 1000L / Sys.getTimerResolution();
        }
}
