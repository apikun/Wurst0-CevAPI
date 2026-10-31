/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ProjectileEspHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.item.EntityExpBottle;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntitySnowball;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;

public final class ProjectileEspHack extends Hack implements RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 64, 8, 128, 1);

	public ProjectileEspHack()
	{
		super("ProjectileESP",
			"Draws a box around arrows, snowballs, eggs, ender pearls,"
				+ " potions and other projectiles.");
		setCategory(Category.RENDER);
		addSetting(range);
	}

	@Override
	public void onRender(float partialTicks)
	{
		if(mc.player == null || mc.world == null)
			return;

		double rangeSq = range.getValue() * range.getValue();

		RenderUtils.enable3D(1.5F, false);
		RenderUtils.applyCameraTransform(partialTicks);

		List<Entity> entities =
			new ArrayList<Entity>(mc.world.loadedEntityList);
		for(Entity e : entities)
		{
			if(e == null || e.getDistanceSq(mc.player.posX, mc.player.posY,
				mc.player.posZ) > rangeSq)
				continue;

			float r, g, b;
			if(e instanceof EntityArrow)
			{
				r = 1F;
				g = 0.5F;
				b = 0F;
			}else if(e instanceof EntityEnderPearl)
			{
				r = 0.5F;
				g = 0.2F;
				b = 0.7F;
			}else if(e instanceof EntityPotion)
			{
				r = 0.5F;
				g = 0.3F;
				b = 0.9F;
			}else if(e instanceof EntityExpBottle)
			{
				r = 0.4F;
				g = 1F;
				b = 0.4F;
			}else if(e instanceof EntitySnowball)
			{
				r = 0.8F;
				g = 0.9F;
				b = 1F;
			}else if(e instanceof EntityEgg)
			{
				r = 0.9F;
				g = 0.9F;
				b = 0.3F;
			}else if(e instanceof EntityFishHook)
			{
				r = 0.3F;
				g = 0.7F;
				b = 1F;
			}else
				continue;

			RenderUtils.drawBox(e.getEntityBoundingBox(), r, g, b, 0.5F);
		}

		RenderUtils.disable3D(false);
	}
}
