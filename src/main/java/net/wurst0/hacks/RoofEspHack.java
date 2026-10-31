/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI RoofEspHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;

/**
 * Highlights the bedrock roof of the nether (y >= 126) so you can
 * find spots to portal/gate up onto it. Nether only.
 */
public final class RoofEspHack extends Hack
	implements UpdateListener, RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 16, 8, 48, 4);

	private final List<BlockPos> found = new ArrayList<BlockPos>();
	private int scanTimer;

	public RoofEspHack()
	{
		super("RoofEsp",
			"Highlights the nether roof so you can find a spot to"
				+ " get on top of it.");
		setCategory(Category.RENDER);
		addSetting(range);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null || p.dimension != -1)
		{
			found.clear();
			return;
		}

		if(--scanTimer > 0)
			return;
		scanTimer = 60;

		found.clear();
		int r = range.getValueI();
		BlockPos center = new BlockPos(p.posX, 127, p.posZ);
		Iterator<BlockPos> itr = BlockPos.getAllInBox(
			center.add(-r, -2, -r), center.add(r, 0, r)).iterator();

		while(itr.hasNext())
		{
			BlockPos pos = itr.next();
			IBlockState state = mc.world.getBlockState(pos);
			if(state.getBlock() == Blocks.BEDROCK)
				found.add(pos);
		}
	}

	@Override
	public void onRender(float partialTicks)
	{
		if(mc.player == null || mc.world == null || found.isEmpty())
			return;

		RenderUtils.enable3D(1.5F, false);
		RenderUtils.applyCameraTransform(partialTicks);

		for(BlockPos pos : found)
			RenderUtils.drawBox(new net.minecraft.util.math.AxisAlignedBB(
				pos.getX(), pos.getY(), pos.getZ(),
				pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
				0.9F, 0.1F, 0.1F, 0.4F);

		RenderUtils.disable3D(false);
	}

	@Override
	protected void onEnable()
	{
		scanTimer = 0;
		found.clear();
	}

	@Override
	protected void onDisable()
	{
		found.clear();
	}
}
