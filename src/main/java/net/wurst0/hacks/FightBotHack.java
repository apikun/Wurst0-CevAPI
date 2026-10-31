/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FightBotHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class FightBotHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 6, 3, 12, 0.5);
	private final CheckboxSetting attackPlayers =
		new CheckboxSetting("Attack players", true);
	private final CheckboxSetting attackPassives =
		new CheckboxSetting("Attack passives", true);
	private final CheckboxSetting pauseInGUIs =
		new CheckboxSetting("Pause in GUIs", true);

	private EntityLivingBase target;

	public FightBotHack()
	{
		super("FightBot",
			"Automatically walks to and attacks the nearest valid"
				+ " target. Server movement checks may apply.");
		setCategory(Category.COMBAT);
		addSettings(range, attackPlayers, attackPassives, pauseInGUIs);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		target = findTarget(p);

		if(target == null)
		{
			stopWalking(p);
			return;
		}

		rotateTo(p, target);

		double reachSq = Math.min(4.5, range.getValue())
			* Math.min(4.5, range.getValue());
		double distSq = p.getDistanceSq(target.posX, target.posY,
			target.posZ);

		if(distSq > reachSq)
		{
			walkTo(p);
			return;
		}

		stopWalking(p);

		if(p.getCooledAttackStrength(0F) < 0.95F)
			return;

		mc.playerController.attackEntity(p, target);
		p.swingArm(EnumHand.MAIN_HAND);
	}

	private EntityLivingBase findTarget(EntityPlayerSP p)
	{
		List<EntityLivingBase> candidates = new ArrayList<EntityLivingBase>();
		double rangeSq = range.getValue() * range.getValue();

		for(Entity e : mc.world.loadedEntityList)
		{
			if(!(e instanceof EntityLivingBase) || e == p)
				continue;

			EntityLivingBase living = (EntityLivingBase)e;
			if(!living.isEntityAlive() || living.getHealth() <= 0)
				continue;
			if(living instanceof EntityArmorStand)
				continue;

			if(living instanceof EntityPlayer)
			{
				if(!attackPlayers.isChecked())
					continue;
			}else if(isPassive(living) && !attackPassives.isChecked())
				continue;

			if(p.getDistanceSq(e.posX, e.posY, e.posZ) > rangeSq)
				continue;

			candidates.add(living);
		}

		if(candidates.isEmpty())
			return null;

		Collections.sort(candidates,
			Comparator.comparingDouble(
				e -> p.getDistanceSq(e.posX, e.posY, e.posZ)));
		return candidates.get(0);
	}

	private boolean isPassive(EntityLivingBase e)
	{
		return e instanceof EntityAnimal || e instanceof EntityAmbientCreature
			|| e instanceof EntitySquid || e instanceof EntityVillager
			|| e instanceof EntityGolem;
	}

	private void walkTo(EntityPlayerSP p)
	{
		if(p.movementInput == null)
			return;
		if(pauseInGUIs.isChecked() && mc.currentScreen != null)
			return;
		if(p.isRiding())
			return;

		p.movementInput.moveForward = 1;
	}

	private void stopWalking(EntityPlayerSP p)
	{
		if(p.movementInput != null)
			p.movementInput.moveForward = 0;
	}

	private void rotateTo(EntityPlayerSP p, Entity e)
	{
		double diffX = e.posX - p.posX;
		double diffY =
			(e.getEntityBoundingBox().minY + e.getEntityBoundingBox().maxY)
				/ 2D - (p.posY + p.getEyeHeight());
		double diffZ = e.posZ - p.posZ;
		double hDist = Math.sqrt(diffX * diffX + diffZ * diffZ);

		p.rotationYaw =
			(float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
		p.rotationPitch =
			MathHelper_clamp((float)-Math.toDegrees(Math.atan2(diffY, hDist)));
	}

	private static float MathHelper_clamp(float v)
	{
		return v < -90F ? -90F : (v > 90F ? 90F : v);
	}

	@Override
	protected void onDisable()
	{
		EntityPlayerSP p = mc.player;
		if(p != null)
			stopWalking(p);
		target = null;
	}
}
