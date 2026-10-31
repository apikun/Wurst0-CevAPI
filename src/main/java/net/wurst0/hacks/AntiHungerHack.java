/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiHungerHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.CPacketPlayer;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

/**
 * Approximate port: sends an extra onGround=true position packet
 * each tick, which makes the server apply ground friction to the
 * hunger calculations and reduces exhaustion from jumping/moving.
 * The sprint-packet suppression part of Wurst7 needs a packet
 * cancel hook (mixin) and is not implemented yet.
 */
public final class AntiHungerHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public AntiHungerHack()
	{
		super("AntiHunger",
			"Slows down hunger by faking on-ground status to the"
				+ " server.");
		setCategory(Category.MOVEMENT);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.isSingleplayer())
			return;
		if(p.onGround)
			return;

		p.connection.sendPacket(new CPacketPlayer.Position(p.posX,
			p.posY, p.posZ, true));
	}
}
