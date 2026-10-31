/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI MobOwnersHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityTameable;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Shows the owner's name above tamed animals (wolves, cats, parrots
 * - anything tameable). Owner names resolve from the tab list; when
 * the owner is offline, a short UUID is shown instead.
 */
public final class MobOwnersHack extends Hack implements RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 64, 8, 128, 8);
	private final SliderSetting scale =
		new SliderSetting("Label scale", 1, 0.5, 2, 0.05);
	private final CheckboxSetting showUuid =
		new CheckboxSetting("Show UUID when unknown", true);

	public MobOwnersHack()
	{
		super("MobOwners",
			"Shows which player owns the tamed animals around you.");
		setCategory(Category.RENDER);
		addSettings(range, scale, showUuid);
	}

	@Override
	public void onRender(float partialTicks)
	{
		if(mc.player == null || mc.world == null)
			return;

		double rangeSq = range.getValue() * range.getValue();
		List<Entity> entities =
			new ArrayList<Entity>(mc.world.loadedEntityList);

		RenderUtils.enable3D(1, false);
		RenderUtils.applyCameraTransform(partialTicks);
		RenderManager rm = mc.getRenderManager();

		for(Entity e : entities)
		{
			if(!(e instanceof EntityTameable) || !e.isEntityAlive())
				continue;
			EntityTameable tame = (EntityTameable)e;
			if(!tame.isTamed())
				continue;
			if(e.getDistanceSq(mc.player.posX, mc.player.posY,
				mc.player.posZ) > rangeSq)
				continue;

			String ownerName = resolveOwnerName(tame);
			if(ownerName == null)
			{
				if(!showUuid.isChecked())
					continue;
				UUID id = tame.getOwnerId();
				ownerName = id == null ? "unknown"
					: id.toString().substring(0, 8);
			}

			String text = "\u00a7bOwner: \u00a7f" + ownerName;
			double ex = e.lastTickPosX
				+ (e.posX - e.lastTickPosX) * partialTicks;
			double ey = e.lastTickPosY
				+ (e.posY - e.lastTickPosY) * partialTicks;
			double ez = e.lastTickPosZ
				+ (e.posZ - e.lastTickPosZ) * partialTicks;

			GlStateManager.pushMatrix();
			GlStateManager.translate(ex, ey + e.height + 0.35, ez);
			GlStateManager.rotate(-rm.playerViewY, 0F, 1F, 0F);
			GlStateManager.rotate(rm.playerViewX, 1F, 0F, 0F);
			float s = 0.025F * scale.getValueF();
			GlStateManager.scale(-s, -s, s);
			GlStateManager.disableLighting();

			int w = mc.fontRenderer.getStringWidth(text);
			mc.fontRenderer.drawStringWithShadow(text, -w / 2F, 0F,
				0xFF55FFFF);

			GlStateManager.enableLighting();
			GlStateManager.popMatrix();
		}

		RenderUtils.disable3D(false);
	}

	private String resolveOwnerName(EntityTameable tame)
	{
		// 1: the owner entity itself, when online
		Entity owner = tame.getOwner();
		if(owner != null)
			return owner.getName();

		// 2: tab list lookup by UUID
		UUID id = tame.getOwnerId();
		if(id != null && mc.getConnection() != null)
		{
			for(NetworkPlayerInfo info :
				mc.getConnection().getPlayerInfoMap())
			{
				if(id.equals(info.getGameProfile().getId()))
					return info.getGameProfile().getName();
			}
		}
		return null;
	}
}
