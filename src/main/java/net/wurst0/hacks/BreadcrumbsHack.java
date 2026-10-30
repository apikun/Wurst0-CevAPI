/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BreadcrumbsHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayDeque;
import java.util.Iterator;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;

public final class BreadcrumbsHack extends Hack
	implements UpdateListener, RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting maxLength =
		new SliderSetting("Max length", 500, 50, 2000, 50);

	private final ArrayDeque<double[]> points = new ArrayDeque<double[]>();

	public BreadcrumbsHack()
	{
		super("Breadcrumbs",
			"Draws a trail behind you as you move, so you can see"
				+ " exactly where you have been.");
		setCategory(Category.INTEL);
		addSetting(maxLength);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		double[] last =
			points.isEmpty() ? null : points.peekLast();
		if(last != null && p.getDistanceSq(last[0], last[1],
			last[2]) < 0.0025D)
			return;

		points.addLast(new double[]{p.posX, p.posY, p.posZ});

		int max = maxLength.getValueI();
		while(points.size() > max)
			points.removeFirst();
	}

	@Override
	public void onRender(float partialTicks)
	{
		if(mc.player == null || mc.world == null || points.size() < 2)
			return;

		RenderUtils.enable3D(1.5F, false);
		RenderUtils.applyCameraTransform(partialTicks);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_LINE_STRIP,
			DefaultVertexFormats.POSITION_COLOR);

		Iterator<double[]> itr = points.iterator();
		while(itr.hasNext())
		{
			double[] point = itr.next();
			buffer.pos(point[0], point[1], point[2])
				.color(0.25F, 1F, 0.25F, 0.75F).endVertex();
		}

		tessellator.draw();

		RenderUtils.disable3D(false);
	}

	@Override
	protected void onEnable()
	{
		points.clear();
	}

	@Override
	protected void onDisable()
	{
		points.clear();
	}
}
