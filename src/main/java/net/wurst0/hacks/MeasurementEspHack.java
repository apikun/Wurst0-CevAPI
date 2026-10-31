/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI MeasurementEspHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;

/**
 * Measures the distance between you and the block/entity under your
 * crosshair: renders a line and the distance. The start point is
 * locked when you enable the hack - toggle it off/on to re-lock.
 * Lite port: no block-corner mode offsets, plain center distance.
 */
public final class MeasurementEspHack extends Hack implements RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final CheckboxSetting blockMode =
		new CheckboxSetting("Block mode", true);

	private Vec3d pointA;
	private String labelA;

	public MeasurementEspHack()
	{
		super("MeasurementESP",
			"Shows the distance between two points. Enable it to lock"
				+ " the first point at your position.");
		setCategory(Category.RENDER);
		addSetting(blockMode);
	}

	@Override
	public void onRender(float partialTicks)
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(pointA == null)
			pointA = new Vec3d(p.posX, p.posY, p.posZ);

		Vec3d pointB;
		RayTraceResult hit = mc.objectMouseOver;
		if(blockMode.isChecked() && hit != null
			&& hit.typeOfHit == RayTraceResult.Type.BLOCK)
		{
			BlockPos pos = hit.getBlockPos();
			pointB = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5,
				pos.getZ() + 0.5);
		}else if(hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY
			&& hit.entityHit != null)
			pointB = new Vec3d(hit.entityHit.posX,
				hit.entityHit.posY + hit.entityHit.height * 0.5,
				hit.entityHit.posZ);
		else
			pointB = new Vec3d(p.posX, p.posY, p.posZ);

		double dx = pointB.x - pointA.x;
		double dy = pointB.y - pointA.y;
		double dz = pointB.z - pointA.z;
		double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

		RenderUtils.enable3D(1.5F, true);
		RenderUtils.applyCameraTransform(partialTicks);
		RenderUtils.drawLine(pointA.x, pointA.y, pointA.z, pointB.x,
			pointB.y, pointB.z, 0.4F, 1F, 0.4F, 0.9F);
		RenderUtils.disable3D(true);

		// screen-space distance readout under the crosshair
		String text = String.format("%.2f m  (%.1f, %.1f, %.1f)",
			Double.valueOf(dist), Double.valueOf(dx), Double.valueOf(dy),
			Double.valueOf(dz));
		net.minecraft.client.gui.ScaledResolution sr =
			new net.minecraft.client.gui.ScaledResolution(mc);
		int cx = sr.getScaledWidth() / 2;
		int cy = sr.getScaledHeight() / 2 + 14;
		mc.fontRenderer.drawStringWithShadow(text,
			cx - mc.fontRenderer.getStringWidth(text) / 2F, cy,
			0xFF66FF66);

		// world-space marker at point A
		RenderUtils.enable3D(1, true);
		RenderUtils.applyCameraTransform(partialTicks);
		AxisAlignedBB marker = new AxisAlignedBB(pointA.x - 0.1,
			pointA.y - 0.1, pointA.z - 0.1, pointA.x + 0.1,
			pointA.y + 0.1, pointA.z + 0.1);
		RenderUtils.drawBox(marker, 0.4F, 1F, 0.4F, 0.9F);
		RenderUtils.disable3D(true);
	}

	@Override
	protected void onDisable()
	{
		pointA = null;
	}

	public void relock()
	{
		EntityPlayerSP p = mc.player;
		if(p != null)
			pointA = new Vec3d(p.posX, p.posY, p.posZ);
	}
}
