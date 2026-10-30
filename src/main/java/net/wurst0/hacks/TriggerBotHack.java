/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TriggerBotHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class TriggerBotHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 3.5, 1, 6, 0.05);
	private final SliderSetting speed =
		new SliderSetting("Speed", 8, 1, 20, 0.5);
	private final CheckboxSetting swing = new CheckboxSetting("Swing", true);

	private int cooldown;

	public TriggerBotHack()
	{
		super("TriggerBot",
			"Automatically attacks whatever entity your crosshair is"
				+ " pointing at.");
		setCategory(Category.COMBAT);
		addSettings(range, speed, swing);
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

		RayTraceResult hit = mc.objectMouseOver;
		if(hit == null || hit.typeOfHit != RayTraceResult.Type.ENTITY
			|| !(hit.entityHit instanceof EntityLivingBase))
			return;

		EntityLivingBase target = (EntityLivingBase)hit.entityHit;
		if(target == p || !target.isEntityAlive()
			|| target instanceof EntityArmorStand)
			return;

		double rangeSq = range.getValue() * range.getValue();
		if(p.getDistanceSq(target.posX, target.posY, target.posZ) > rangeSq)
			return;

		mc.playerController.attackEntity(p, target);
		if(swing.isChecked())
			p.swingArm(EnumHand.MAIN_HAND);

		cooldown = Math.max(1, (int)Math.round(20D / speed.getValue()));
	}
}
