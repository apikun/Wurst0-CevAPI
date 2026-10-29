/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NoFallHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

public final class NoFallHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public NoFallHack()
	{
		super("NoFall",
			"Resets your fall distance every tick. NOTE: this build is"
				+ " client-side only - the packet-based version that fully"
				+ " prevents server-side damage is planned (see"
				+ " PORT_STATUS.md).");
		setCategory(Category.MOVEMENT);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null)
			return;
		p.fallDistance = 0;
	}
}
