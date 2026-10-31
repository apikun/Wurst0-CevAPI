/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NewChunksHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.AxisAlignedBB;
import net.wurst0.Category;
import net.wurst0.event.events.ChunkLoadEvent;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.listeners.ChunkLoadListener;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;

public final class NewChunksHack extends Hack
	implements ChunkLoadListener, RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	// every chunk ever seen this session
	private final Set<Long> seen = new HashSet<Long>();
	// chunks marked as new and still worth rendering
	private final Set<Long> fresh = new HashSet<Long>();

	public NewChunksHack()
	{
		super("NewChunks",
			"Highlights chunks that were generated or loaded for the"
				+ " first time in this session.");
		setCategory(Category.INTEL);
	}

	@Override
	public void onChunkLoad(int chunkX, int chunkZ)
	{
		long key = key(chunkX, chunkZ);
		if(seen.add(key))
		{
			if(fresh.size() < 4096)
				fresh.add(key);
		}
	}

	@Override
	public void onRender(float partialTicks)
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null || fresh.isEmpty())
			return;

		RenderUtils.enable3D(1F, false);
		RenderUtils.applyCameraTransform(partialTicks);

		for(Long key : fresh)
		{
			int cx = (int)(key >> 32);
			int cz = (int)(key & 0xFFFFFFFFL);
			double x0 = cx << 4;
			double z0 = cz << 4;

			// translucent floor marker at sea level bottom
			RenderUtils.drawBox(new AxisAlignedBB(x0, 0, z0, x0 + 16, 0.5,
				z0 + 16), 0.1F, 1F, 0.2F, 0.25F);
		}

		RenderUtils.disable3D(false);
	}

	private static long key(int chunkX, int chunkZ)
	{
		return ((long)chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
	}

	@Override
	protected void onEnable()
	{
		fresh.clear();
	}

	@Override
	protected void onDisable()
	{
		fresh.clear();
		seen.clear();
	}
}
