/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI MobEspHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;

public final class MobEspHack extends Hack implements RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 32, 1, 128, 1);

	public MobEspHack()
	{
		super("MobESP",
			"Draws a box around every mob so you can see them through"
				+ " walls.");
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
			if(!(e instanceof EntityLivingBase) || e == mc.player
				|| e instanceof EntityPlayer)
				continue;
			if(!e.isEntityAlive())
				continue;
			if(e.getDistanceSq(mc.player.posX, mc.player.posY,
				mc.player.posZ) > rangeSq)
				continue;

			RenderUtils.drawBox(e.getEntityBoundingBox(), 0.2F, 1F, 1F,
				0.5F);
		}

		RenderUtils.disable3D(false);
	}
}
