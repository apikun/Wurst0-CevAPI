/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI PlayerEspHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.wurst0.Category;
import net.wurst0.WurstClient;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;

public final class PlayerEspHack extends Hack implements RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 32, 1, 128, 1);

	private final ModeSetting style =
		new ModeSetting("Style",
			new String[]{"Box", "Lines", "Both"}, 0);

	public PlayerEspHack()
	{
		super("PlayerESP",
			"Draws a box around every player so you can see them"
				+ " through walls.");
		setCategory(Category.RENDER);
		addSettings(range, style);
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
			if(!(e instanceof EntityPlayer) || e == mc.player)
				continue;
			if(!e.isEntityAlive())
				continue;
			if(e.getDistanceSq(mc.player.posX, mc.player.posY,
				mc.player.posZ) > rangeSq)
				continue;

			if(!"Box".equals(style.getSelected()))
			{
				double px = mc.player.lastTickPosX + (mc.player.posX - mc.player.lastTickPosX) * partialTicks;
				double py = mc.player.lastTickPosY + (mc.player.posY - mc.player.lastTickPosY) * partialTicks;
				double pz = mc.player.lastTickPosZ + (mc.player.posZ - mc.player.lastTickPosZ) * partialTicks;
				double ex = e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks;
				double ey = e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTicks;
				double ez = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks;
				RenderUtils.drawLine(px, py + mc.player.getEyeHeight(), pz,
					ex, ey + e.getEyeHeight() * 0.9D, ez,
					1F, 0.2F, 0.2F, 0.75F);
			}
			if(!"Lines".equals(style.getSelected()))
				RenderUtils.drawBox(e.getEntityBoundingBox(), 1F, 0.2F, 0.2F, 0.5F);
		}

		RenderUtils.disable3D(false);
	}
}
