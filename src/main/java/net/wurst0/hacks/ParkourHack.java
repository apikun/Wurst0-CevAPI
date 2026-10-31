/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ParkourHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

public final class ParkourHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final CheckboxSetting onlyOnGround =
		new CheckboxSetting("Only while on ground", true);

	public ParkourHack()
	{
		super("Parkour",
			"Automatically jumps when you are about to walk off a"
				+ " block. Useful for parkour maps.");
		setCategory(Category.MOVEMENT);
		addSetting(onlyOnGround);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;
		if(!p.onGround)
			return;
		if(p.isSneaking() || p.isRiding() || p.isInWater())
			return;
		if(mc.currentScreen != null)
			return;

		double motionX = p.motionX;
		double motionZ = p.motionZ;
		double hSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
		if(hSpeed < 0.1)
			return;

		// look ~4 ticks ahead
		BlockPos ahead = new BlockPos(p.posX + motionX * 4,
			p.posY - 1, p.posZ + motionZ * 4);
		if(!mc.world.isAirBlock(ahead))
			return;

		// make sure we actually stand on something right now
		BlockPos below = new BlockPos(p.posX, p.posY - 0.5, p.posZ);
		if(mc.world.isAirBlock(below))
			return;

		p.jump();
	}
}
