/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;

public final class RenderUtils
{
	private static final Minecraft mc = Minecraft.getMinecraft();

	private RenderUtils()
	{}

	public static void applyCameraTransform(float partialTicks)
	{
		EntityPlayer p = mc.player;
		double x =
			p.lastTickPosX + (p.posX - p.lastTickPosX) * partialTicks;
		double y =
			p.lastTickPosY + (p.posY - p.lastTickPosY) * partialTicks;
		double z =
			p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * partialTicks;
		GlStateManager.translate(-x, -y, -z);
	}

	public static void enable3D(float lineWidth, boolean depth)
	{
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(
			GlStateManager.SourceFactor.SRC_ALPHA,
			GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
			GlStateManager.SourceFactor.ONE,
			GlStateManager.DestFactor.ZERO);
		GlStateManager.disableTexture2D();
		GlStateManager.glLineWidth(lineWidth);
		if(!depth)
			GlStateManager.disableDepth();
	}

	public static void disable3D(boolean depth)
	{
		if(!depth)
			GlStateManager.enableDepth();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
		GlStateManager.color(1, 1, 1, 1);
	}

	public static void drawBox(AxisAlignedBB box, float r, float g, float b,
		float a)
	{
		RenderGlobal.drawSelectionBoundingBox(box, r, g, b, a);
	}

	public static void drawLine(double x1, double y1, double z1, double x2,
		double y2, double z2, float r, float g, float b, float a)
	{
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
		buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
		tessellator.draw();
	}
}
