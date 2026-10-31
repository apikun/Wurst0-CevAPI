/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BonemealAuraHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
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
 * Uses bone meal on the closest growable crop around you. Hold bone
 * meal in your main hand. Uses the same artificial-raytrace
 * right-click mechanism as Tillaura (no aiming needed).
 * The MultiMeal option from Wurst7 is not ported yet.
 */
public final class BonemealAuraHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 5, 1, 6, 0.5);
	private final SliderSetting delay =
		new SliderSetting("Delay (ticks)", 5, 0, 20, 1);

	private int timer;

	public BonemealAuraHack()
	{
		super("BonemealAura",
			"Uses bone meal on the closest growable crop around you."
				+ " Hold the bone meal in your main hand.");
		setCategory(Category.BLOCKS);
		addSettings(range, delay);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null || mc.playerController == null)
			return;

		if(timer-- > 0)
			return;
		timer = delay.getValueI();

		ItemStack held = p.getHeldItemMainhand();
		if(held.getItem() != Items.DYE || held.getMetadata() != 15)
			return;

		BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);
		int r = Math.max(1, (int)Math.round(range.getValue()));

		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;

		for(BlockPos pos : BlockPos.getAllInBox(center.add(-r, -r, -r),
			center.add(r, r, r)))
		{
			IBlockState state = mc.world.getBlockState(pos);
			if(!(state.getBlock() instanceof IGrowable))
				continue;

			IGrowable growable = (IGrowable)state.getBlock();
			if(!growable.canGrow(mc.world, pos, state, true))
				continue;
			if(!growable.canUseBonemeal(mc.world, mc.world.rand, pos,
				state))
				continue;

			double dist = pos.distanceSq(center);
			if(dist < bestDist)
			{
				bestDist = dist;
				best = pos;
			}
		}

		if(best == null)
			return;

		RayTraceResult ray = new RayTraceResult(
			new Vec3d(best.getX() + 0.5, best.getY() + 0.5,
				best.getZ() + 0.5),
			EnumFacing.UP, best);
		mc.playerController.processRightClickBlock(p,
			(WorldClient)mc.world, best, EnumFacing.UP, ray.hitVec,
			EnumHand.MAIN_HAND);
	}

	@Override
	protected void onEnable()
	{
		timer = 0;
	}
}
