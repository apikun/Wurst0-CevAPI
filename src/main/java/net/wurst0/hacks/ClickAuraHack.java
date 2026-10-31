/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ClickAuraHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
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

/**
 * Like Killaura, but without any rotation or targeting logic: it
 * simply hits everything valid in range once per attack cooldown.
 * Same attack path as Killaura (playerController.attackEntity).
 */
public final class ClickAuraHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 4.5, 1, 6, 0.05);
	private final SliderSetting speed =
		new SliderSetting("Speed", 10, 0.5, 20, 0.5);
	private final CheckboxSetting swing = new CheckboxSetting("Swing", true);
	private final CheckboxSetting attackPlayers =
		new CheckboxSetting("Attack players", true);
	private final CheckboxSetting attackPassives =
		new CheckboxSetting("Attack passives", false);

	private int cooldown;

	public ClickAuraHack()
	{
		super("ClickAura",
			"Attacks everything in reach without turning your head."
				+ " Less suspicious than Killaura.");
		setCategory(Category.COMBAT);
		addSettings(range, speed, swing, attackPlayers, attackPassives);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(cooldown > 0)
		{
			cooldown--;
			return;
		}

		List<EntityLivingBase> targets = getTargets(p);
		if(targets.isEmpty())
			return;

		for(EntityLivingBase target : targets)
			mc.playerController.attackEntity(p, target);

		if(swing.isChecked())
			p.swingArm(EnumHand.MAIN_HAND);

		cooldown = Math.max(1, (int)Math.round(20D / speed.getValue()));
	}

	private List<EntityLivingBase> getTargets(EntityPlayerSP p)
	{
		List<EntityLivingBase> result = new ArrayList<EntityLivingBase>();
		double rangeSq = range.getValue() * range.getValue();

		List<Entity> loaded = new ArrayList<Entity>(mc.world.loadedEntityList);
		for(Entity e : loaded)
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

			result.add(living);
		}
		return result;
	}

	private boolean isPassive(EntityLivingBase e)
	{
		return e instanceof EntityAnimal || e instanceof EntityAmbientCreature
			|| e instanceof EntitySquid || e instanceof EntityVillager
			|| e instanceof EntityGolem;
	}

	@Override
	protected void onDisable()
	{
		cooldown = 0;
	}
}
