/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI PearlEspHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;

public final class PearlEspHack extends Hack implements RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 64, 8, 128, 1);

	public PearlEspHack()
	{
		super("PearlESP",
			"Draws a box around thrown ender pearls so you can see"
				+ " where they land.");
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
			if(!(e instanceof EntityEnderPearl))
				continue;
			if(e.getDistanceSq(mc.player.posX, mc.player.posY,
				mc.player.posZ) > rangeSq)
				continue;

			RenderUtils.drawBox(e.getEntityBoundingBox(), 0.5F, 0.2F,
				0.7F, 0.5F);
		}

		RenderUtils.disable3D(false);
	}
}
