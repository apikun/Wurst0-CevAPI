/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI PlayerSonarHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Sonar ping: every interval it lists every player in range with
 * distance and compass direction. Lite port: chat output only (the
 * original also renders ESP for surfaced pings).
 */
public final class PlayerSonarHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 128, 16, 256, 16);
	private final SliderSetting interval =
		new SliderSetting("Interval (s)", 10, 1, 120, 1);

	private int timer;

	public PlayerSonarHack()
	{
		super("PlayerSonar",
			"Periodically pings the area and reports every player in"
				+ " range with distance and direction.");
		setCategory(Category.INTEL);
		addSettings(range, interval);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(timer > 0)
		{
			timer--;
			return;
		}
		timer = (int)Math.round(interval.getValue() * 20.0);

		List<EntityPlayer> players = new ArrayList<EntityPlayer>(
			mc.world.playerEntities);
		StringBuilder sb = new StringBuilder();
		int count = 0;
		double rangeSq = range.getValue() * range.getValue();

		for(EntityPlayer other : players)
		{
			if(other == p)
				continue;
			double dist = other.getDistanceSq(p.posX, p.posY, p.posZ);
			if(dist > rangeSq)
				continue;

			double d = Math.sqrt(dist);
			String dir = compassDirection(other.posX - p.posX,
				other.posZ - p.posZ);
			if(count > 0)
				sb.append(", ");
			sb.append("\u00a7e").append(other.getName())
				.append("\u00a7r ").append(String.format("%.0f", d))
				.append("m ").append(dir);
			count++;
		}

		if(count == 0)
			p.sendMessage(new TextComponentString(
				"\u00a78[Sonar]\u00a7r no players in range."));
		else
			p.sendMessage(new TextComponentString(
				"\u00a78[Sonar]\u00a7r " + count + " player"
					+ (count == 1 ? "" : "s") + ": " + sb.toString()));
	}

	private static String compassDirection(double dx, double dz)
	{
		// 0 = south (+Z), 90 = west (-X) like vanilla yaw
		float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
		int octant = MathHelper.floor((yaw + 22.5F) / 45.0F) & 7;
		return new String[]{"S", "SW", "W", "NW", "N", "NE", "E",
			"SE"}[octant];
	}

	@Override
	protected void onEnable()
	{
		timer = 0;
	}
}
