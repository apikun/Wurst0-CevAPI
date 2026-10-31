/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SafeWalkHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * Stops you from walking off edges. Approximate port: when the block
 * support under your predicted next position is missing, the horizontal
 * motion is cancelled (the original patches the sneaking collision
 * flag via a mixin, which we cannot do yet).
 */
public final class SafeWalkHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final CheckboxSetting sneak =
		new CheckboxSetting("Sneak at edge", false);

	public SafeWalkHack()
	{
		super("SafeWalk",
			"Stops you from walking off blocks, like sneaking would.");
		setCategory(Category.MOVEMENT);
		addSetting(sneak);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;
		if(!p.onGround || p.isSneaking())
			return;

		double nx = p.posX + p.motionX;
		double nz = p.posZ + p.motionZ;

		if(hasSupport(nx, nz, p.posY))
			return;

		p.motionX = 0;
		p.motionZ = 0;

		if(sneak.isChecked())
		{
			p.setSneaking(true);
			if(p.movementInput != null)
				p.movementInput.sneak = true;
		}
	}

	private boolean hasSupport(double x, double z, double y)
	{
		// check the four corners of the player's footprint one block
		// below the predicted position
		int bx = MathHelper.floor(x);
		int by = MathHelper.floor(y) - 1;
		int bz = MathHelper.floor(z);
		double w = 0.3;

		int[][] corners = new int[][]{
			{MathHelper.floor(x - w), MathHelper.floor(z - w)},
			{MathHelper.floor(x - w), MathHelper.floor(z + w)},
			{MathHelper.floor(x + w), MathHelper.floor(z - w)},
			{MathHelper.floor(x + w), MathHelper.floor(z + w)}};

		for(int[] c : corners)
		{
			BlockPos pos = new BlockPos(c[0], by, c[1]);
			IBlockState state = mc.world.getBlockState(pos);
			Material m = state.getMaterial();
			if(m != Material.AIR && !m.isLiquid())
				return true;
		}
		// also the center column (bx/bz may differ from all corners)
		BlockPos center = new BlockPos(bx, by, bz);
		IBlockState cs = mc.world.getBlockState(center);
		Material cm = cs.getMaterial();
		return cm != Material.AIR && !cm.isLiquid();
	}

	@Override
	protected void onDisable()
	{
		if(mc.player != null && mc.player.movementInput != null)
			mc.player.movementInput.sneak =
				mc.player.isSneaking();
	}
}
