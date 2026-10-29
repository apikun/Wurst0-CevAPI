/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI XRayHack (GPL-3.0-or-later)
 * 1.12.2 note: this build implements X-Ray in ESP mode (highlights ore
 * blocks through walls). True block-hiding X-Ray requires a mixin into
 * Block.isOpaqueCube and is planned (see PORT_STATUS.md).
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class XRayHack extends Hack implements UpdateListener,
	RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 24, 8, 64, 1);
	private final CheckboxSetting coal = new CheckboxSetting("Coal", true);
	private final CheckboxSetting iron = new CheckboxSetting("Iron", true);
	private final CheckboxSetting gold = new CheckboxSetting("Gold", true);
	private final CheckboxSetting diamond =
		new CheckboxSetting("Diamond", true);
	private final CheckboxSetting emerald =
		new CheckboxSetting("Emerald", true);
	private final CheckboxSetting redstone =
		new CheckboxSetting("Redstone", true);
	private final CheckboxSetting lapis =
		new CheckboxSetting("Lapis", true);

	private final List<BlockPos> found = new ArrayList<BlockPos>();
	private final List<float[]> colors = new ArrayList<float[]>();
	private int scanTimer;

	public XRayHack()
	{
		super("X-Ray",
			"Highlights valuable ores through walls (ESP mode).");
		setCategory(Category.RENDER);
		addSetting(range, coal, iron, gold, diamond, emerald, redstone,
			lapis);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(scanTimer++ % 10 != 0)
			return;

		scan(p);
	}

	private void scan(EntityPlayerSP p)
	{
		found.clear();
		colors.clear();

		int r = range.getValueI();
		BlockPos center =
			new BlockPos(p.posX, p.posY, p.posZ);
		Iterable<BlockPos> box = BlockPos.getAllInBox(
			center.add(-r, -r, -r), center.add(r, r, r));

		for(BlockPos pos : box)
		{
			IBlockState state = mc.world.getBlockState(pos);
			Block block = state.getBlock();

			if(block == Blocks.COAL_ORE && coal.isChecked())
				add(pos, 0.15F, 0.15F, 0.15F);
			else if(block == Blocks.IRON_ORE && iron.isChecked())
				add(pos, 0.85F, 0.7F, 0.55F);
			else if(block == Blocks.GOLD_ORE && gold.isChecked())
				add(pos, 0.95F, 0.85F, 0.2F);
			else if(block == Blocks.DIAMOND_ORE && diamond.isChecked())
				add(pos, 0.2F, 0.95F, 0.95F);
			else if(block == Blocks.EMERALD_ORE && emerald.isChecked())
				add(pos, 0.2F, 0.95F, 0.3F);
			else if((block == Blocks.REDSTONE_ORE
				|| block == Blocks.LIT_REDSTONE_ORE)
				&& redstone.isChecked())
				add(pos, 0.95F, 0.2F, 0.2F);
			else if(block == Blocks.LAPIS_ORE && lapis.isChecked())
				add(pos, 0.2F, 0.3F, 0.95F);
		}
	}

	private void add(BlockPos pos, float r, float g, float b)
	{
		found.add(pos);
		colors.add(new float[]{r, g, b});
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
			RenderUtils.drawBox(new net.minecraft.util.math.AxisAlignedBB(
				found.get(i)), c[0], c[1], c[2], 0.6F);
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
