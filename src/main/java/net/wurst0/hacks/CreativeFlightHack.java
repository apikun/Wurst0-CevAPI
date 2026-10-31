/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI CreativeFlightHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

/**
 * Vanilla-style creative flight: forces the flying player ability and
 * keeps it synced with sendPlayerAbilities() (C13). Works out of the
 * box in creative mode and on servers that allow the abilities packet
 * (e.g. after being briefly put into creative). On survival-only
 * vanilla servers the server will push you back down.
 */
public final class CreativeFlightHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private boolean hadAllowFlying;

	public CreativeFlightHack()
	{
		super("CreativeFlight",
			"Allows you to fly like in creative mode by forcing the"
				+ " vanilla flying ability.");
		setCategory(Category.MOVEMENT);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		p.capabilities.isFlying = true;
		if(!p.capabilities.allowFlying)
		{
			p.capabilities.allowFlying = true;
			p.sendPlayerAbilities();
		}
	}

	@Override
	protected void onEnable()
	{
		if(mc.player != null)
			hadAllowFlying = mc.player.capabilities.allowFlying;
	}

	@Override
	protected void onDisable()
	{
		EntityPlayerSP p = mc.player;
		if(p == null)
			return;

		p.capabilities.isFlying = false;
		// keep the ability if the server already gave it (creative) -
		// only remove what we added ourselves
		if(!hadAllowFlying && !p.isCreative())
		{
			p.capabilities.allowFlying = false;
			p.sendPlayerAbilities();
		}
	}
}
