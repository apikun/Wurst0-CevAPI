/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI DamageEspHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;

/**
 * Highlights every entity that is currently taking damage with an
 * orange box (visible through walls).
 */
public final class DamageEspHack extends Hack implements RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 64, 8, 128, 8);

	public DamageEspHack()
	{
		super("DamageESP",
			"Shows an ESP box around entities that are currently"
				+ " taking damage.");
		setCategory(Category.RENDER);
		addSetting(range);
	}

	@Override
	public void onRender(float partialTicks)
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		double rangeSq = range.getValue() * range.getValue();
		List<Entity> entities =
			new ArrayList<Entity>(mc.world.loadedEntityList);

		RenderUtils.enable3D(1.5F, true);
		RenderUtils.applyCameraTransform(partialTicks);

		for(Entity e : entities)
		{
			if(!(e instanceof EntityLivingBase) || e == p)
				continue;
			EntityLivingBase base = (EntityLivingBase)e;
			if(base.hurtTime <= 0 && base.maxHurtTime <= 0)
				continue;
			if(e.getDistanceSq(p.posX, p.posY, p.posZ) > rangeSq)
				continue;

			double ex = base.lastTickPosX
				+ (base.posX - base.lastTickPosX) * partialTicks;
			double ey = base.lastTickPosY
				+ (base.posY - base.lastTickPosY) * partialTicks;
			double ez = base.lastTickPosZ
				+ (base.posZ - base.lastTickPosZ) * partialTicks;
			AxisAlignedBB box = new AxisAlignedBB(
				ex - 0.45, ey, ez - 0.45, ex + 0.45,
				ey + base.height, ez + 0.45);
			RenderUtils.drawBox(box, 1F, 0.5F, 0F, 0.9F);
		}

		RenderUtils.disable3D(true);
	}
}
