/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI LavaWaterEspHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
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
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Highlights lava (and optionally water) source and flowing blocks
 * through walls, so you never mine into a lava pocket again.
 */
public final class LavaWaterEspHack extends Hack
	implements UpdateListener, RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 16, 8, 48, 4);
	private final CheckboxSetting lava =
		new CheckboxSetting("Lava", true);
	private final CheckboxSetting water =
		new CheckboxSetting("Water", false);

	private final List<BlockPos> lavaBlocks = new ArrayList<BlockPos>();
	private final List<BlockPos> waterBlocks = new ArrayList<BlockPos>();
	private int scanTimer;

	public LavaWaterEspHack()
	{
		super("LavaWaterEsp",
			"Highlights lava and water blocks through walls.");
		setCategory(Category.RENDER);
		addSettings(range, lava, water);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(--scanTimer > 0)
			return;
		scanTimer = 40;

		lavaBlocks.clear();
		waterBlocks.clear();
		int r = range.getValueI();
		BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);
		Iterator<BlockPos> itr = BlockPos.getAllInBox(
			center.add(-r, -r, -r), center.add(r, r, r)).iterator();

		while(itr.hasNext())
		{
			BlockPos pos = itr.next();
			IBlockState state = mc.world.getBlockState(pos);
			Block block = state.getBlock();

			if(lava.isChecked() && (block == Blocks.LAVA
				|| block == Blocks.FLOWING_LAVA))
				lavaBlocks.add(pos);
			else if(water.isChecked() && (block == Blocks.WATER
				|| block == Blocks.FLOWING_WATER))
				waterBlocks.add(pos);
		}
	}

	@Override
	public void onRender(float partialTicks)
	{
		if(mc.player == null || mc.world == null)
			return;
		if(lavaBlocks.isEmpty() && waterBlocks.isEmpty())
			return;

		RenderUtils.enable3D(1.5F, false);
		RenderUtils.applyCameraTransform(partialTicks);

		for(BlockPos pos : lavaBlocks)
			RenderUtils.drawBox(new net.minecraft.util.math.AxisAlignedBB(
				pos.getX(), pos.getY(), pos.getZ(),
				pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
				1F, 0.3F, 0.05F, 0.4F);

		for(BlockPos pos : waterBlocks)
			RenderUtils.drawBox(new net.minecraft.util.math.AxisAlignedBB(
				pos.getX(), pos.getY(), pos.getZ(),
				pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
				0.2F, 0.4F, 1F, 0.4F);

		RenderUtils.disable3D(false);
	}

	@Override
	protected void onEnable()
	{
		scanTimer = 0;
		lavaBlocks.clear();
		waterBlocks.clear();
	}

	@Override
	protected void onDisable()
	{
		lavaBlocks.clear();
		waterBlocks.clear();
	}
}
