/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI HealthTagsHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;

/**
 * Shows name + health tags above every hostile mob, visible through
 * walls (same billboard approach as NameTags, red text).
 */
public final class HealthTagsHack extends Hack implements RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 64, 8, 128, 8);
	private final SliderSetting scale =
		new SliderSetting("Scale", 1, 0.5, 2, 0.05);

	public HealthTagsHack()
	{
		super("HealthTags",
			"Shows the health of every hostile mob, visible through"
				+ " walls.");
		setCategory(Category.RENDER);
		addSettings(range, scale);
	}

	@Override
	public void onRender(float partialTicks)
	{
		if(mc.player == null || mc.world == null)
			return;

		double rangeSq = range.getValue() * range.getValue();

		RenderUtils.enable3D(1, false);
		RenderUtils.applyCameraTransform(partialTicks);
		RenderManager rm = mc.getRenderManager();

		List<Entity> entities =
			new ArrayList<Entity>(mc.world.loadedEntityList);
		for(Entity e : entities)
		{
			if(!(e instanceof EntityLivingBase) || e == mc.player)
				continue;
			if(!(e instanceof IMob) || !e.isEntityAlive())
				continue;
			if(e.getDistanceSq(mc.player.posX, mc.player.posY,
				mc.player.posZ) > rangeSq)
				continue;

			EntityLivingBase base = (EntityLivingBase)e;
			String text = base.getName() + " ("
				+ (int)base.getHealth() + "/" + (int)base.getMaxHealth()
				+ " HP)";

			double ex = base.lastTickPosX
				+ (base.posX - base.lastTickPosX) * partialTicks;
			double ey = base.lastTickPosY
				+ (base.posY - base.lastTickPosY) * partialTicks;
			double ez = base.lastTickPosZ
				+ (base.posZ - base.lastTickPosZ) * partialTicks;

			GlStateManager.pushMatrix();
			GlStateManager.translate(ex, ey + base.height + 0.3, ez);
			GlStateManager.rotate(-rm.playerViewY, 0F, 1F, 0F);
			GlStateManager.rotate(rm.playerViewX, 1F, 0F, 0F);
			float s = 0.025F * scale.getValueF();
			GlStateManager.scale(-s, -s, s);
			GlStateManager.disableLighting();

			int w = mc.fontRenderer.getStringWidth(text);
			mc.fontRenderer.drawStringWithShadow(text, -w / 2F, 0F,
				0xFFFF5050);

			GlStateManager.enableLighting();
			GlStateManager.popMatrix();
		}

		RenderUtils.disable3D(false);
	}
}
