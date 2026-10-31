/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntisocialHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * The opposite of Follow: when a player gets closer than the
 * trigger distance, you automatically turn away and walk off.
 * Jumps over obstacles, pauses while a GUI is open.
 */
public final class AntisocialHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting trigger =
		new SliderSetting("Trigger distance", 8, 2, 32, 1);
	private final CheckboxSetting sprint =
		new CheckboxSetting("Sprint", true);

	public AntisocialHack()
	{
		super("Antisocial",
			"Automatically walks away when a player gets too close.");
		setCategory(Category.OTHER);
		addSettings(trigger, sprint);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;
		if(mc.currentScreen != null)
			return;

		EntityPlayer threat = findClosest(p);
		if(threat == null)
			return;

		// face away from the player and walk
		double diffX = p.posX - threat.posX;
		double diffZ = p.posZ - threat.posZ;
		p.rotationYaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
		p.rotationPitch = 0;

		if(p.movementInput != null && !p.isRiding())
			p.movementInput.moveForward = 1;

		if(sprint.isChecked() && !p.isSneaking())
			p.setSprinting(true);

		if(p.collidedHorizontally && p.onGround)
			p.motionY = 0.42;
	}

	private EntityPlayer findClosest(EntityPlayerSP p)
	{
		EntityPlayer best = null;
		double bestDist = trigger.getValue() * trigger.getValue();

		for(EntityPlayer e : mc.world.playerEntities)
		{
			if(e == p || !e.isEntityAlive())
				continue;

			double dist = p.getDistanceSq(e.posX, e.posY, e.posZ);
			if(dist < bestDist)
			{
				bestDist = dist;
				best = e;
			}
		}
		return best;
	}

	@Override
	protected void onDisable()
	{
		if(mc.player != null && mc.player.movementInput != null)
			mc.player.movementInput.moveForward = 0;
	}
}
