/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TrajectoriesHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.item.ItemLingeringPotion;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemSplashPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;

public final class TrajectoriesHack extends Hack implements RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public TrajectoriesHack()
	{
		super("Trajectories",
			"Predicts the flight path of arrows and throwable items,"
				+ " showing where they will land.");
		setCategory(Category.RENDER);
	}

	@Override
	public void onRender(float partialTicks)
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		ItemStack stack = p.getHeldItemMainhand();
		if(stack.isEmpty())
			return;

		double velocity;
		double gravity;
		if(stack.getItem() instanceof ItemBow)
		{
			float charge = p.isHandActive()
				? (stack.getMaxItemUseDuration()
					- p.getItemInUseCount()) / 20F : 1F;
			charge = (charge * charge + charge * 2F) / 3F;
			if(charge > 1F)
				charge = 1F;
			if(charge < 0.1F)
				charge = 0.1F;
			velocity = charge * 3F;
			gravity = 0.05D;
		}else if(stack.getItem() instanceof ItemSnowball
			|| stack.getItem() instanceof ItemEnderPearl
			|| stack.getItem() instanceof ItemEgg
			|| stack.getItem() instanceof ItemSplashPotion
			|| stack.getItem() instanceof ItemLingeringPotion
			|| stack.getItem() instanceof ItemExpBottle)
		{
			velocity = 1.5D;
			gravity = 0.03D;
		}else
			return;

		Vec3d eye = p.getPositionEyes(partialTicks);
		Vec3d look = p.getLook(partialTicks);
		Vec3d motion = new Vec3d(look.x * velocity,
			look.y * velocity, look.z * velocity);

		Vec3d pathStart = null;
		Vec3d pathEnd = null;
		Vec3d landing = null;
		Vec3d pos = eye;

		for(int i = 0; i < 200; i++)
		{
			Vec3d next = new Vec3d(pos.x + motion.x,
				pos.y + motion.y, pos.z + motion.z);

			RayTraceResult hit = mc.world.rayTraceBlocks(pos, next);
			if(hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK)
			{
				pathEnd = hit.hitVec;
				landing = hit.hitVec;
				break;
			}

			if(pathStart == null)
				pathStart = pos;
			pathEnd = next;
			pos = next;

			motion = new Vec3d(motion.x * 0.99D,
				(motion.y - gravity) * 0.99D, motion.z * 0.99D);
		}

		if(pathStart == null || pathEnd == null)
			return;

		RenderUtils.enable3D(1.5F, false);
		RenderUtils.applyCameraTransform(partialTicks);

		// draw the arc as short segments
		Vec3d prev = pathStart;
		int steps = 10;
		double dx = (pathEnd.x - pathStart.x) / steps;
		double dy = (pathEnd.y - pathStart.y) / steps;
		double dz = (pathEnd.z - pathStart.z) / steps;
		for(int i = 1; i <= steps; i++)
		{
			Vec3d cur = new Vec3d(pathStart.x + dx * i,
				pathStart.y + dy * i, pathStart.z + dz * i);
			RenderUtils.drawLine(prev.x, prev.y, prev.z,
				cur.x, cur.y, cur.z, 1F, 1F, 0F, 0.75F);
			prev = cur;
		}

		if(landing != null)
		{
			AxisAlignedBB box = new AxisAlignedBB(landing.x - 0.25D,
				landing.y - 0.25D, landing.z - 0.25D,
				landing.x + 0.25D, landing.y + 0.25D,
				landing.z + 0.25D);
			RenderUtils.drawBox(box, 1F, 0F, 0F, 0.75F);
		}

		RenderUtils.disable3D(false);
	}
}
