/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI MobSpawnEspHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.wurst0.Category;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;

/**
 * Highlights blocks that hostile mobs can spawn on: solid top
 * surface, two air blocks above and a block light level of 7 or
 * less. Uses the client's own light data, so torches placed after
 * the last scan may lag one refresh behind.
 */
public final class MobSpawnEspHack extends Hack
	implements UpdateListener, RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 16, 8, 24, 4);

	private final List<BlockPos> found = new ArrayList<BlockPos>();
	private int scanTimer;

	public MobSpawnEspHack()
	{
		super("MobSpawnESP",
			"Highlights blocks where hostile mobs can spawn.");
		setCategory(Category.RENDER);
		addSetting(range);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(--scanTimer > 0)
			return;
		scanTimer = 80;

		found.clear();
		int r = range.getValueI();
		BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);
		Iterator<BlockPos> itr = BlockPos.getAllInBox(
			center.add(-r, -r, -r), center.add(r, r, r)).iterator();

		while(itr.hasNext())
		{
			BlockPos pos = itr.next();

			IBlockState state = mc.world.getBlockState(pos);
			if(!state.isNormalCube())
				continue;

			BlockPos up1 = pos.up();
			BlockPos up2 = pos.up(2);
			if(!mc.world.isAirBlock(up1) || !mc.world.isAirBlock(up2))
				continue;

			if(mc.world.getLightFor(EnumSkyBlock.BLOCK, up1) > 7)
				continue;

			found.add(pos);
		}
	}

	@Override
	public void onRender(float partialTicks)
	{
		if(mc.player == null || mc.world == null || found.isEmpty())
			return;

		RenderUtils.enable3D(1F, false);
		RenderUtils.applyCameraTransform(partialTicks);

		for(BlockPos pos : found)
			RenderUtils.drawBox(new net.minecraft.util.math.AxisAlignedBB(
				pos.getX(), pos.getY(), pos.getZ(),
				pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
				0.9F, 0.9F, 0.9F, 0.35F);

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
