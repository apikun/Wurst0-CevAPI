/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BarrierEspHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;

public final class BarrierEspHack extends Hack
	implements UpdateListener, RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 16, 8, 48, 4);

	private final List<BlockPos> found = new ArrayList<BlockPos>();
	private int scanTimer;
	private net.minecraft.world.World lastWorld;

	public BarrierEspHack()
	{
		super("BarrierESP", "Draws a box around every barrier block so you can see the usually invisible barriers.");
		setCategory(Category.RENDER);
		addSetting(range);
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
			scanTimer = 0;
		}

		if(--scanTimer > 0)
			return;
		scanTimer = 40;

		found.clear();
		int r = Math.min(range.getValueI(), 32);
		BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);
		Iterator<BlockPos> itr = BlockPos.getAllInBox(
			center.add(-r, -r, -r), center.add(r, r, r)).iterator();

		while(itr.hasNext())
		{
			BlockPos pos = itr.next();
			IBlockState state = mc.world.getBlockState(pos);
			if(state.getBlock() == net.minecraft.init.Blocks.BARRIER)
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
				pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1,
				pos.getY() + 1, pos.getZ() + 1), 1F, 0F, 0F, 0.5F);

		RenderUtils.disable3D(false);
	}

	@Override
	protected void onEnable()
	{
		scanTimer = 0;
		found.clear();
	}
}
