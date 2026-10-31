/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI RadarHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.wurst0.Category;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class RadarHack extends Hack implements ScreenRenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting size =
		new SliderSetting("Size", 128, 64, 256, 16);
	private final SliderSetting range =
		new SliderSetting("Range", 64, 16, 128, 8);
	private final CheckboxSetting players =
		new CheckboxSetting("Players", true);
	private final CheckboxSetting mobs =
		new CheckboxSetting("Mobs", true);
	private final CheckboxSetting items =
		new CheckboxSetting("Items", false);

	public RadarHack()
	{
		super("Radar",
			"Draws a minimap-style radar showing nearby entities.");
		setCategory(Category.RENDER);
		addSettings(size, range, players, mobs, items);
	}

	@Override
	public void onScreenRender(float partialTicks)
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		ScaledResolution sr = new ScaledResolution(mc);
		int r = size.getValueI() / 2;
		int x = sr.getScaledWidth() - r - 16;
		int y = sr.getScaledHeight() - r - 16;
		double scale = (double)r / range.getValue();
		double rangeSq = (double)range.getValue() * range.getValue();

		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, 0);
		GlStateManager.disableDepth();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA,
			GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

		// background circle
		GlStateManager.color(0F, 0F, 0F, 0.55F);
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glVertex2d(0, 0);
		for(int i = 0; i <= 64; i++)
		{
			double a = Math.PI * 2 * i / 64D;
			GL11.glVertex2d(Math.cos(a) * r, Math.sin(a) * r);
		}
		GL11.glEnd();

		// border
		GlStateManager.color(1F, 1F, 1F, 0.8F);
		GL11.glLineWidth(1F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		for(int i = 0; i <= 64; i++)
		{
			double a = Math.PI * 2 * i / 64D;
			GL11.glVertex2d(Math.cos(a) * r, Math.sin(a) * r);
		}
		GL11.glEnd();

		// self marker
		GlStateManager.color(1F, 1F, 1F, 1F);
		GL11.glBegin(GL11.GL_TRIANGLES);
		GL11.glVertex2d(0, -4);
		GL11.glVertex2d(-3, 3);
		GL11.glVertex2d(3, 3);
		GL11.glEnd();

		// entity dots
		for(Entity e : mc.world.loadedEntityList)
		{
			if(e == p)
				continue;

			float[] color = colorFor(e);
			if(color == null)
				continue;

			double distSq = p.getDistanceSq(e.posX, e.posY, e.posZ);
			if(distSq > rangeSq)
				continue;

			double dx = e.posX - p.posX;
			double dz = e.posZ - p.posZ;

			// rotate so that "up" on the radar is where the player looks
			double yawRad = Math.toRadians(p.rotationYaw);
			double fwdX = -Math.sin(yawRad);
			double fwdZ = Math.cos(yawRad);
			double rightX = -fwdZ;
			double rightZ = fwdX;

			double sx = (dx * rightX + dz * rightZ) * scale;
			double sy = -(dx * fwdX + dz * fwdZ) * scale;

			GlStateManager.color(color[0], color[1], color[2], 1F);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2d(sx - 2, sy - 2);
			GL11.glVertex2d(sx - 2, sy + 2);
			GL11.glVertex2d(sx + 2, sy + 2);
			GL11.glVertex2d(sx + 2, sy - 2);
			GL11.glEnd();
		}

		GlStateManager.color(1F, 1F, 1F, 1F);
		GlStateManager.enableDepth();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	private float[] colorFor(Entity e)
	{
		if(e instanceof EntityArmorStand)
			return null;

		if(e instanceof EntityPlayer)
			return players.isChecked() ? new float[]{1F, 0.2F, 0.2F} : null;

		if(e instanceof EntityItem)
			return items.isChecked() ? new float[]{1F, 0.9F, 0.2F} : null;

		if(e instanceof EntityMob)
			return mobs.isChecked() ? new float[]{1F, 0.5F, 0F} : null;

		if(e instanceof EntityLivingBase && !(e instanceof EntityAnimal)
			&& !(e instanceof EntityAmbientCreature)
			&& !(e instanceof EntitySquid) && !(e instanceof EntityVillager)
			&& !(e instanceof EntityGolem))
			return mobs.isChecked() ? new float[]{1F, 0.5F, 0F} : null;

		return mobs.isChecked() ? new float[]{0.2F, 1F, 0.3F} : null;
	}
}
