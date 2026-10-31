/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TpAuraHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumHand;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class TpAuraHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 6, 3, 10, 0.5);
	private final SliderSetting speed =
		new SliderSetting("Speed", 8, 1, 20, 1);
	private final CheckboxSetting attackPlayers =
		new CheckboxSetting("Attack players", true);

	private int cooldown;
	private double angle;

	public TpAuraHack()
	{
		super("TpAura",
			"Teleports around the nearest player and attacks them."
				+ " Very obvious on the server side.");
		setCategory(Category.COMBAT);
		addSettings(range, speed, attackPlayers);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null || p.connection == null)
			return;

		if(cooldown > 0)
		{
			cooldown--;
			return;
		}

		EntityLivingBase target = findTarget(p);
		if(target == null)
			return;

		// teleport around the target (packet level)
		angle += Math.PI / 3D;
		double dist = 1D + Math.random();
		double x = target.posX + Math.cos(angle) * dist;
		double z = target.posZ + Math.sin(angle) * dist;

		p.connection.sendPacket(
			new CPacketPlayer.Position(x, p.posY, z, false));
		p.connection.sendPacket(new CPacketUseEntity(target));
		p.connection.sendPacket(new CPacketAnimation(EnumHand.MAIN_HAND));
		p.connection.sendPacket(new CPacketPlayer.Position(p.posX, p.posY,
			p.posZ, p.onGround));

		p.swingArm(EnumHand.MAIN_HAND);

		cooldown = Math.max(1, (int)Math.round(20D / speed.getValue()));
	}

	private EntityLivingBase findTarget(EntityPlayerSP p)
	{
		double rangeSq = range.getValue() * range.getValue();

		for(Entity e : mc.world.loadedEntityList)
		{
			if(!(e instanceof EntityPlayer) || e == p)
				continue;
			if(!attackPlayers.isChecked())
				break;

			EntityPlayer player = (EntityPlayer)e;
			if(!player.isEntityAlive() || player.getHealth() <= 0)
				continue;
			if(e instanceof EntityArmorStand)
				continue;

			if(p.getDistanceSq(e.posX, e.posY, e.posZ) <= rangeSq)
				return player;
		}
		return null;
	}

	@Override
	protected void onDisable()
	{
		cooldown = 0;
	}
}
