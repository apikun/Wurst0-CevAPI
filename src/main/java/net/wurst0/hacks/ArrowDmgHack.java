/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ArrowDmgHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemBow;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.ReflectUtils;

/**
 * Boosts the speed (and therefore damage) of arrows you shoot: right
 * after release, the arrow's motion is multiplied. Approximate port -
 * strict servers may reject the resulting arrow trajectory.
 */
public final class ArrowDmgHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting strength =
		new SliderSetting("Strength", 2, 1.5, 5, 0.25);

	private boolean wasDrawing;

	public ArrowDmgHack()
	{
		super("ArrowDMG",
			"Multiplies the speed of your arrows right after you"
				+ " release the bow.");
		setCategory(Category.COMBAT);
		addSetting(strength);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		boolean drawing = p.isHandActive()
			&& p.getActiveItemStack() != null
			&& p.getActiveItemStack().getItem() instanceof ItemBow;

		if(wasDrawing && !drawing)
			boostArrows(p);

		wasDrawing = drawing;
	}

	private void boostArrows(EntityPlayerSP p)
	{
		List<Entity> entities =
			new ArrayList<Entity>(mc.world.loadedEntityList);
		for(Entity e : entities)
		{
			if(!(e instanceof EntityArrow))
				continue;
			EntityArrow arrow = (EntityArrow)e;
			if(arrow.ticksExisted > 2)
				continue;
			Entity shooter = getShootingEntity(arrow);
			if(shooter != p)
				continue;

			double mult = strength.getValue();
			arrow.motionX *= mult;
			arrow.motionY *= mult;
			arrow.motionZ *= mult;
			arrow.velocityChanged = true;
			p.sendMessage(new TextComponentString(
				"\u00a7a[ArrowDMG]\u00a7r arrow boosted x"
					+ strength.getValue()));
		}
	}

	private static Field shootingEntityField;

	private static Entity getShootingEntity(EntityArrow arrow)
	{
		try
		{
			if(shootingEntityField == null)
			{
				shootingEntityField = ReflectUtils.field(
					EntityArrow.class, "shootingEntity",
					"field_70250_c");
				if(shootingEntityField == null)
					return null;
				shootingEntityField.setAccessible(true);
			}
			Object o = shootingEntityField.get(arrow);
			return o instanceof Entity ? (Entity)o : null;
		}catch(Throwable t)
		{
			return null;
		}
	}

	@Override
	protected void onDisable()
	{
		wasDrawing = false;
	}
}
