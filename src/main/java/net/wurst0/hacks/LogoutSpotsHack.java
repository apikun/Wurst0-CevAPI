/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI LogoutSpotsHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;

/**
 * Remembers where other players were standing when they left the
 * world and draws a box (plus a tracer) there, so you can see who
 * logged out nearby. Keeps the last 20 spots per session; name
 * labels at the spots are not possible yet (no world-space text
 * renderer in this port).
 */
public final class LogoutSpotsHack extends Hack
	implements UpdateListener, RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();
	private static final int MAX_SPOTS = 20;

	// last known position of every tracked player, keyed by name
	private final Map<String, double[]> seen =
		new LinkedHashMap<String, double[]>();
	private final List<double[]> spots = new ArrayList<double[]>();
	private int scanTimer;
	private net.minecraft.world.World lastWorld;

	public LogoutSpotsHack()
	{
		super("LogoutSpots",
			"Marks where other players logged out, so you can tell who"
				+ " left the area.");
		setCategory(Category.INTEL);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(mc.world != lastWorld)
		{
			lastWorld = mc.world;
			seen.clear();
			spots.clear();
			scanTimer = 0;
		}

		// update the known positions (cheap, runs every 10 ticks)
		if(--scanTimer > 0)
			return;
		scanTimer = 10;

		Set<String> online = new HashSet<String>();
		for(EntityPlayer e : mc.world.playerEntities)
		{
			if(e == p)
				continue;
			online.add(e.getName());
			seen.put(e.getName(),
				new double[]{e.posX, e.posY, e.posZ});
		}

		// everyone we saw but can no longer see has logged out
		Iterator<Map.Entry<String, double[]>> itr =
			seen.entrySet().iterator();
		while(itr.hasNext())
		{
			Map.Entry<String, double[]> e = itr.next();
			if(online.contains(e.getKey()))
				continue;

			spots.add(e.getValue());
			itr.remove();

			if(spots.size() > MAX_SPOTS)
				spots.remove(0);
		}
	}

	@Override
	public void onRender(float partialTicks)
	{
		if(mc.player == null || mc.world == null || spots.isEmpty())
			return;

		EntityPlayerSP p = mc.player;
		double px = p.posX;
		double py = p.posY + p.getEyeHeight();
		double pz = p.posZ;

		RenderUtils.enable3D(1.5F, false);
		RenderUtils.applyCameraTransform(partialTicks);

		for(double[] spot : spots)
		{
			double x = spot[0];
			double y = spot[1];
			double z = spot[2];

			RenderUtils.drawBox(new AxisAlignedBB(x - 0.35, y, z - 0.35,
				x + 0.35, y + 1.9, z + 0.35), 0.3F, 0.9F, 1F, 0.5F);
			RenderUtils.drawLine(px, py, pz, x, y + 1, z, 0.3F, 0.9F, 1F,
				0.5F);
		}

		RenderUtils.disable3D(false);
	}

	@Override
	protected void onEnable()
	{
		seen.clear();
		spots.clear();
		scanTimer = 0;
	}
}
