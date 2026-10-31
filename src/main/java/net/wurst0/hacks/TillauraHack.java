/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TillauraHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemHoe;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Hoes nearby dirt/grass blocks by calling
 * PlayerControllerMP.processRightClickBlock with an artificial
 * raytrace - no crosshair aiming needed.
 */
public final class TillauraHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 5, 1, 6, 0.5);
	private final SliderSetting delay =
		new SliderSetting("Delay (ticks)", 4, 0, 20, 1);

	private int timer;

	public TillauraHack()
	{
		super("Tillaura",
			"Automatically tills dirt and grass blocks around you with"
				+ " your hoe.");
		setCategory(Category.BLOCKS);
		addSettings(range, delay);
	}

	private static boolean isTillable(Block block)
	{
		return block == Blocks.DIRT || block == Blocks.GRASS
			|| block == Blocks.GRASS_PATH;
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(!(p.getHeldItemMainhand().getItem() instanceof ItemHoe))
			return;

		if(timer > 0)
		{
			timer--;
			return;
		}
		timer = delay.getValueI();

		int r = range.getValueI();
		BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);

		List<BlockPos> candidates = new ArrayList<BlockPos>();
		Iterator<BlockPos> itr = BlockPos.getAllInBox(
			center.add(-r, -1, -r), center.add(r, 1, r)).iterator();
		while(itr.hasNext())
		{
			BlockPos pos = itr.next();
			if(isTillable(mc.world.getBlockState(pos).getBlock()))
				candidates.add(pos);
		}

		if(candidates.isEmpty())
			return;

		// closest first
		candidates.sort(new java.util.Comparator<BlockPos>()
		{
			@Override
			public int compare(BlockPos a, BlockPos b)
			{
				double da = a.distanceSq(center);
				double db = b.distanceSq(center);
				return Double.compare(da, db);
			}
		});

		BlockPos pos = candidates.get(0);
		RayTraceResult ray = new RayTraceResult(
			new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5,
				pos.getZ() + 0.5),
			EnumFacing.UP, pos);
		mc.playerController.processRightClickBlock(p,
			(net.minecraft.client.multiplayer.WorldClient)mc.world, pos,
			EnumFacing.UP, ray.hitVec, EnumHand.MAIN_HAND);
	}
}
