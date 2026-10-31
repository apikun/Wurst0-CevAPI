/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NoWebHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

/**
 * Counteracts the cobweb slowdown. The web dampens your motion to 25%
 * during the move step, so we scale it back up while standing inside
 * one (approximate: server movement checks still apply).
 */
public final class NoWebHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public NoWebHack()
	{
		super("NoWeb",
			"Counteracts the slowdown of cobwebs (approximate:"
				+ " multiplies your motion back up while inside one).");
		setCategory(Category.MOVEMENT);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(!isInWeb(p))
			return;

		// undo the 0.25 motion multiplier the web applies each move
		p.motionX = compensate(p.motionX, 1.0);
		p.motionZ = compensate(p.motionZ, 1.0);
		// falling in a web is capped to 0.05 by the game - undo that
		// without turning the web into a slingshot
		p.motionY = compensate(p.motionY, 0.5);
	}

	private static double compensate(double v, double cap)
	{
		double c = v / 0.25;
		return Math.max(-cap, Math.min(cap, c));
	}

	private boolean isInWeb(EntityPlayerSP p)
	{
		BlockPos feet = new BlockPos(p.posX, p.posY + 0.1, p.posZ);
		BlockPos head = new BlockPos(p.posX, p.posY + p.getEyeHeight(),
			p.posZ);
		return mc.world.getBlockState(feet).getBlock() == Blocks.WEB
			|| mc.world.getBlockState(head).getBlock() == Blocks.WEB;
	}
}
