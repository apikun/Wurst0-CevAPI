/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ProphuntEspHack (GPL-3.0-or-later)
 * Lite adaptation: on 1.12.2 the fake-block box is drawn around any
 * invisible EntityLiving (excluding armor stands), which is where
 * prophunt disguise plugins hide their players' mobs.
 */
package net.wurst0.hacks;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.math.AxisAlignedBB;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;

/**
 * Draws a pulsing fake block-shaped box around every invisible mob,
 * exposing prophunt disguises. Ignores armor stands and entities
 * closer than half a block.
 */
public final class ProphuntEspHack extends Hack implements RenderListener
{
	private static final AxisAlignedBB FAKE_BLOCK_BOX =
		new AxisAlignedBB(-0.5, 0, -0.5, 0.5, 1, 0.5);

	private final Minecraft mc = Minecraft.getMinecraft();

	public ProphuntEspHack()
	{
		super("ProphuntESP",
			"Draws a fake block-shaped box around any invisible mob,"
				+ " exposing prophunt disguises.");
		setCategory(Category.RENDER);
	}

	@Override
	public void onRender(float partialTicks)
	{
		if(mc.player == null || mc.world == null)
			return;

		ArrayList<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
		for(int i = 0; i < mc.world.loadedEntityList.size(); i++)
		{
			Entity entity = mc.world.loadedEntityList.get(i);
			if(!(entity instanceof EntityLiving)
				|| entity instanceof EntityArmorStand)
				continue;

			if(!entity.isInvisible())
				continue;

			if(mc.player.getDistanceSq(entity) < 0.25)
				continue;

			boxes.add(FAKE_BLOCK_BOX.offset(entity.posX, entity.posY,
				entity.posZ));
		}

		if(boxes.isEmpty())
			return;

		// pulsing red, same as upstream
		float alpha = 0.5F + 0.25F * (float)Math.sin(
			System.currentTimeMillis() % 1000L / 500F * Math.PI);

		RenderUtils.enable3D(1.5F, false);
		RenderUtils.applyCameraTransform(partialTicks);

		drawSolidBoxes(boxes, 1F, 0F, 0F, alpha * 0.35F);
		for(int i = 0; i < boxes.size(); i++)
			RenderUtils.drawBox(boxes.get(i), 1F, 0F, 0F, alpha);

		RenderUtils.disable3D(false);
	}

	private void drawSolidBoxes(ArrayList<AxisAlignedBB> boxes, float r,
		float g, float b, float a)
	{
		if(a <= 0)
			return;

		GlStateManager.disableCull();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

		for(int i = 0; i < boxes.size(); i++)
			addBoxFaces(buffer, boxes.get(i), r, g, b, a);

		tessellator.draw();
		GlStateManager.enableCull();
	}

	private void addBoxFaces(BufferBuilder buffer, AxisAlignedBB box,
		float r, float g, float b, float a)
	{
		double x1 = box.minX;
		double y1 = box.minY;
		double z1 = box.minZ;
		double x2 = box.maxX;
		double y2 = box.maxY;
		double z2 = box.maxZ;

		quad(buffer, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2,
			r, g, b, a); // bottom
		quad(buffer, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2,
			r, g, b, a); // top
		quad(buffer, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1,
			r, g, b, a); // north
		quad(buffer, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2,
			r, g, b, a); // south
		quad(buffer, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1,
			r, g, b, a); // west
		quad(buffer, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1,
			r, g, b, a); // east
	}

	private void quad(BufferBuilder buffer, double x1, double y1,
		double z1, double x2, double y2, double z2, double x3,
		double y3, double z3, double x4, double y4, double z4,
		float r, float g, float b, float a)
	{
		buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
		buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
		buffer.pos(x3, y3, z3).color(r, g, b, a).endVertex();
		buffer.pos(x4, y4, z4).color(r, g, b, a).endVertex();
	}
}
