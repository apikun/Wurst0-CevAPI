/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI CriticalsHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.ModeSetting;

public final class CriticalsHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final ModeSetting mode = new ModeSetting("Mode",
		new String[]{"Packet", "MiniJump", "FullJump"}, 0);

	private boolean wasAttacking;

	public CriticalsHack()
	{
		super("Criticals", "Makes all your hits critical hits."
			+ " Packet mode fakes the jump by sending packets without"
			+ " moving you at all, MiniJump does a tiny real jump and"
			+ " FullJump jumps normally.");
		setCategory(Category.COMBAT);
		addSetting(mode);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		boolean attacking = mc.gameSettings.keyBindAttack.isKeyDown()
			&& mc.objectMouseOver != null
			&& mc.objectMouseOver.typeOfHit == RayTraceResult.Type.ENTITY
			&& mc.objectMouseOver.entityHit != null;

		if(attacking && !wasAttacking && p.onGround && !p.isInWater()
			&& !p.isInLava())
			doCritical(p);

		wasAttacking = attacking;
	}

	private void doCritical(EntityPlayerSP p)
	{
		switch(mode.getSelected())
		{
			case "Packet":
				p.connection.sendPacket(new CPacketPlayer.Position(
					p.posX, p.posY + 0.0625D, p.posZ, false));
				p.connection.sendPacket(new CPacketPlayer.Position(
					p.posX, p.posY, p.posZ, false));
				p.connection.sendPacket(new CPacketPlayer.Position(
					p.posX, p.posY + 1.0E-4D, p.posZ, false));
				p.connection.sendPacket(new CPacketPlayer.Position(
					p.posX, p.posY, p.posZ, false));
				break;

			case "MiniJump":
				p.motionY = 0.1D;
				p.fallDistance += 0.1F;
				break;

			case "FullJump":
				p.motionY = 0.42D;
				if(p.isSprinting())
				{
					float yawRad = (float)Math.toRadians(p.rotationYaw);
					p.motionX -= Math.sin(yawRad) * 0.2F;
					p.motionZ += Math.cos(yawRad) * 0.2F;
				}
				break;
		}
	}
}
