/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SurfaceXrayHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * X-Ray variant for finding exposed ores: only highlights ore blocks
 * that have sky access, so it shows surface/cave-entrance ores you
 * could actually see without digging.
 */
public final class SurfaceXrayHack extends Hack implements UpdateListener,
	RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 32, 8, 64, 8);
	private final CheckboxSetting coal = new CheckboxSetting("Coal", true);
	private final CheckboxSetting iron = new CheckboxSetting("Iron", true);
	private final CheckboxSetting gold = new CheckboxSetting("Gold", true);
	private final CheckboxSetting diamond =
		new CheckboxSetting("Diamond", true);
	private final CheckboxSetting emerald =
		new CheckboxSetting("Emerald", true);

	private final List<BlockPos> found = new ArrayList<BlockPos>();
	private final List<float[]> colors = new ArrayList<float[]>();
	private int scanTimer;

	public SurfaceXrayHack()
	{
		super("SurfaceXray",
			"Highlights ores that are exposed to the sky - the ones"
				+ " you can mine without tunneling.");
		setCategory(Category.RENDER);
		addSettings(range, coal, iron, gold, diamond, emerald);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(scanTimer++ % 20 != 0)
			return;

		found.clear();
		colors.clear();

		int r = range.getValueI();
		BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);
		Iterable<BlockPos> box = BlockPos.getAllInBox(
			center.add(-r, -8, -r), center.add(r, 8, r));

		for(BlockPos pos : box)
		{
			IBlockState state = mc.world.getBlockState(pos);
			Block block = state.getBlock();

			float[] c = oreColor(block);
			if(c == null)
				continue;
			// must have sky access to count as "surface"
			if(!mc.world.canSeeSky(pos.up()))
				continue;

			found.add(pos);
			colors.add(c);
		}
	}

	private float[] oreColor(Block block)
	{
		if(block == Blocks.COAL_ORE && coal.isChecked())
			return new float[]{0.15F, 0.15F, 0.15F};
		if(block == Blocks.IRON_ORE && iron.isChecked())
			return new float[]{0.85F, 0.7F, 0.55F};
		if(block == Blocks.GOLD_ORE && gold.isChecked())
			return new float[]{0.95F, 0.85F, 0.2F};
		if(block == Blocks.DIAMOND_ORE && diamond.isChecked())
			return new float[]{0.2F, 0.95F, 0.95F};
		if(block == Blocks.EMERALD_ORE && emerald.isChecked())
			return new float[]{0.2F, 0.95F, 0.3F};
		return null;
	}

	@Override
	public void onRender(float partialTicks)
	{
		if(mc.player == null || mc.world == null || found.isEmpty())
			return;

		RenderUtils.enable3D(1, false);
		RenderUtils.applyCameraTransform(partialTicks);

		for(int i = 0; i < found.size(); i++)
		{
			float[] c = colors.get(i);
			AxisAlignedBB box = new AxisAlignedBB(found.get(i));
			RenderUtils.drawBox(box, c[0], c[1], c[2], 0.6F);
		}

		RenderUtils.disable3D(false);
	}

	@Override
	protected void onDisable()
	{
		found.clear();
		colors.clear();
		scanTimer = 0;
	}
}
