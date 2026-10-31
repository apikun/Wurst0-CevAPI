/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI CrystalAuraHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Attacks every end crystal in reach on a cooldown - the 1.12.2
 * crystal-PvP bread and butter. Lite port: no auto placement of new
 * crystals (no support-face solver here yet).
 */
public final class CrystalAuraHack extends Hack
	implements UpdateListener, RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 4.5, 1, 6, 0.05);
	private final SliderSetting speed =
		new SliderSetting("Speed", 8, 0.5, 20, 0.5);
	private final CheckboxSetting checkLOS =
		new CheckboxSetting("Check line of sight", true);
	private final CheckboxSetting swing =
		new CheckboxSetting("Swing", true);
	private final CheckboxSetting render =
		new CheckboxSetting("Render target", true);

	private EntityEnderCrystal target;
	private int cooldown;

	public CrystalAuraHack()
	{
		super("CrystalAura",
			"Automatically attacks every end crystal in reach."
				+ " Works without looking at the crystals.");
		setCategory(Category.COMBAT);
		addSettings(range, speed, checkLOS, swing, render);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(cooldown > 0)
		{
			cooldown--;
			return;
		}

		List<Entity> crystals =
			new ArrayList<Entity>(mc.world.loadedEntityList);
		double rangeSq = range.getValue() * range.getValue();

		EntityEnderCrystal best = null;
		double bestDist = Double.MAX_VALUE;
		for(Entity e : crystals)
		{
			if(!(e instanceof EntityEnderCrystal)
				|| !e.isEntityAlive())
				continue;
			double dist = e.getDistanceSq(p.posX, p.posY, p.posZ);
			if(dist > rangeSq || dist >= bestDist)
				continue;
			if(checkLOS.isChecked() && !hasLineOfSight(p, e))
				continue;
			bestDist = dist;
			best = (EntityEnderCrystal)e;
		}

		target = best;
		if(best == null)
			return;

		mc.playerController.attackEntity(p, best);
		if(swing.isChecked())
			p.swingArm(EnumHand.MAIN_HAND);

		// cooldown scaled from attacks-per-second
		cooldown = (int)Math.max(1, Math.round(20.0 / speed.getValue()));
	}

	private boolean hasLineOfSight(EntityPlayerSP p, Entity e)
	{
		Vec3d eyes = p.getPositionEyes(1.0F);
		Vec3d targetVec = new Vec3d(e.posX, e.posY + e.height * 0.5,
			e.posZ);
		RayTraceResult hit = mc.world.rayTraceBlocks(eyes, targetVec,
			false, true, false);
		return hit == null
			|| hit.typeOfHit != RayTraceResult.Type.BLOCK;
	}

	@Override
	public void onRender(float partialTicks)
	{
		if(!render.isChecked() || target == null || mc.player == null)
			return;

		double ex = target.lastTickPosX
			+ (target.posX - target.lastTickPosX) * partialTicks;
		double ey = target.lastTickPosY
			+ (target.posY - target.lastTickPosY) * partialTicks;
		double ez = target.lastTickPosZ
			+ (target.posZ - target.lastTickPosZ) * partialTicks;
		AxisAlignedBB box = new AxisAlignedBB(
			ex - 0.5, ey, ez - 0.5, ex + 0.5,
			ey + target.height + 0.5, ez + 0.5);

		RenderUtils.enable3D(1.5F, true);
		RenderUtils.applyCameraTransform(partialTicks);
		RenderUtils.drawBox(box, 1F, 0.2F, 0.9F, 0.85F);
		RenderUtils.disable3D(true);
	}

	@Override
	protected void onDisable()
	{
		target = null;
		cooldown = 0;
	}
}
