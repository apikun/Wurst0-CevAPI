/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SearchHack (GPL-3.0-or-later)
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

public final class SearchHack extends Hack
	implements UpdateListener, RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 16, 8, 48, 4);
	private final CheckboxSetting diamonds =
		new CheckboxSetting("Diamonds", true);
	private final CheckboxSetting emeralds =
		new CheckboxSetting("Emeralds", true);
	private final CheckboxSetting gold =
		new CheckboxSetting("Gold", true);
	private final CheckboxSetting iron =
		new CheckboxSetting("Iron", true);
	private final CheckboxSetting coal =
		new CheckboxSetting("Coal", false);
	private final CheckboxSetting redstone =
		new CheckboxSetting("Redstone", false);
	private final CheckboxSetting lapis =
		new CheckboxSetting("Lapis", false);
	private final CheckboxSetting quartz =
		new CheckboxSetting("Quartz", false);

	private static final class Entry
	{
		final BlockPos pos;
		final float r;
		final float g;
		final float b;

		Entry(BlockPos pos, float r, float g, float b)
		{
			this.pos = pos;
			this.r = r;
			this.g = g;
			this.b = b;
		}
	}

	private final List<Entry> found = new ArrayList<Entry>();
	private int scanTimer;
	private net.minecraft.world.World lastWorld;

	public SearchHack()
	{
		super("Search",
			"Searches for selected ore blocks and highlights them"
				+ " through walls.");
		setCategory(Category.RENDER);
		addSettings(range, diamonds, emeralds, gold, iron, coal, redstone,
			lapis, quartz);
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
			Block block = state.getBlock();

			if(diamonds.isChecked() && block == Blocks.DIAMOND_ORE)
				found.add(new Entry(pos, 0F, 0.9F, 0.9F));
			else if(emeralds.isChecked() && block == Blocks.EMERALD_ORE)
				found.add(new Entry(pos, 0.1F, 0.9F, 0.2F));
			else if(gold.isChecked() && block == Blocks.GOLD_ORE)
				found.add(new Entry(pos, 1F, 0.85F, 0F));
			else if(iron.isChecked() && block == Blocks.IRON_ORE)
				found.add(new Entry(pos, 0.8F, 0.65F, 0.5F));
			else if(coal.isChecked() && block == Blocks.COAL_ORE)
				found.add(new Entry(pos, 0.25F, 0.25F, 0.25F));
			else if(redstone.isChecked() && (block == Blocks.REDSTONE_ORE
				|| block == Blocks.LIT_REDSTONE_ORE))
				found.add(new Entry(pos, 1F, 0.1F, 0.1F));
			else if(lapis.isChecked() && block == Blocks.LAPIS_ORE)
				found.add(new Entry(pos, 0.1F, 0.2F, 0.8F));
			else if(quartz.isChecked() && block == Blocks.QUARTZ_ORE)
				found.add(new Entry(pos, 0.95F, 0.9F, 0.85F));
		}
	}

	@Override
	public void onRender(float partialTicks)
	{
		if(mc.player == null || mc.world == null || found.isEmpty())
			return;

		RenderUtils.enable3D(1.5F, false);
		RenderUtils.applyCameraTransform(partialTicks);

		for(Entry entry : found)
			RenderUtils.drawBox(new net.minecraft.util.math.AxisAlignedBB(
				entry.pos.getX(), entry.pos.getY(), entry.pos.getZ(),
				entry.pos.getX() + 1, entry.pos.getY() + 1,
				entry.pos.getZ() + 1), entry.r, entry.g, entry.b, 0.5F);

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
