/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ElytraFlightHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Takes over the elytra physics while gliding: horizontal speed is
 * applied in look direction, jump/sneak control altitude, and fall
 * distance is cleared so you never take fall damage. In 1.12.2 the
 * gliding motion is client-simulated, so this works without packet
 * hooks.
 */
public final class ElytraFlightHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final ModeSetting mode = new ModeSetting("Mode",
		new String[]{"Normal", "Hover"}, 0);
	private final SliderSetting speed =
		new SliderSetting("Speed", 1.5, 0.1, 5, 0.1);

	public ElytraFlightHack()
	{
		super("ElytraFlight",
			"Allows you to fly with the elytra without having to"
				+ " swoop. Jump to fly up, sneak to fly down.");
		setCategory(Category.MOVEMENT);
		addSettings(mode, speed);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || !p.isElytraFlying())
			return;

		double sp = speed.getValue();
		double fw = p.movementInput.moveForward;
		double st = p.movementInput.moveStrafe;

		// horizontal movement in look direction
		double yawRad = Math.toRadians(p.rotationYaw);
		double sinY = Math.sin(yawRad), cosY = Math.cos(yawRad);
		double vx = st * cosY - fw * sinY;
		double vz = -st * sinY - fw * cosY;
		double len = Math.sqrt(vx * vx + vz * vz);
		if(len > 1e-4)
		{
			vx /= len;
			vz /= len;
			p.motionX = vx * sp;
			p.motionZ = vz * sp;
		}else
		{
			p.motionX *= 0.5;
			p.motionZ *= 0.5;
		}

		// altitude control
		if(mc.gameSettings.keyBindJump.isKeyDown())
			p.motionY = sp * 0.8;
		else if(mc.gameSettings.keyBindSneak.isKeyDown())
			p.motionY = -sp * 0.8;
		else if("Hover".equals(mode.getSelected()))
			p.motionY = 0;
		else
			p.motionY *= 0.4;

		p.fallDistance = 0;
	}
}
