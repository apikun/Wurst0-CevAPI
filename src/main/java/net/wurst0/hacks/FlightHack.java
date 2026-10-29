/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FlightHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;

public final class FlightHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final ModeSetting mode =
		new ModeSetting("Mode", new String[]{"Simple"}, 0);
	private final SliderSetting speed =
		new SliderSetting("Speed", 1, 0.05, 5, 0.05);

	public FlightHack()
	{
		super("Flight", "Lets you fly. Simple mode flies in the direction"
			+ " you look and ignores gravity.");
		setCategory(Category.MOVEMENT);
		addSetting(mode, speed);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		float speedF = speed.getValueF();
		double yawRad = Math.toRadians(p.rotationYaw);
		double forward = p.movementInput.moveForward;
		double strafe = p.movementInput.moveStrafe;
		double norm = Math.sqrt(forward * forward + strafe * strafe);
		if(norm < 0.01)
		{
			forward = 0;
			strafe = 0;
		}else
		{
			forward /= norm;
			strafe /= norm;
		}

		p.motionX =
			(-Math.sin(yawRad) * forward - Math.cos(yawRad) * strafe)
				* speedF;
		p.motionZ =
			(Math.cos(yawRad) * forward - Math.sin(yawRad) * strafe)
				* speedF;
		p.motionY = mc.gameSettings.keyBindJump.isKeyDown() ? 0.5
			: mc.gameSettings.keyBindSneak.isKeyDown() ? -0.5 : 0;

		p.fallDistance = 0;
	}
}
