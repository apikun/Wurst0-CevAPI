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

	private static final List<Object> heldOutbound =
		new ArrayList<Object>();
	private static final List<DelayedPacket> delayedOutbound =
		new ArrayList<DelayedPacket>();

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
				conn.sendPacket((net.minecraft.network.Packet<?>)packet);
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
			if(!(msg instanceof net.minecraft.network.Packet))
			{
				super.write(ctx, msg, promise);
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

			super.write(ctx, msg, promise);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception
		{
			InboundFilter filter = inboundFilter;
			if(filter != null && msg instanceof net.minecraft.network.Packet)
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
		if(delayedOutbound.isEmpty())
			return;

		long now = Sys.getTime();
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
	}
}
