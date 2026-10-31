/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoFishHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.projectile.EntityFishHook;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.util.ReflectUtils;
import net.wurst0.settings.SliderSetting;

public final class AutoFishHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting delay =
		new SliderSetting("Recast delay", 15, 5, 60, 5);

	private java.lang.reflect.Method rightClickMouseMethod;
	private Field caughtEntityField;
	private int recastTimer;
	private boolean waitingForBite;

	public AutoFishHack()
	{
		super("AutoFish",
			"Automatically catches fish and casts the rod back out.");
		setCategory(Category.OTHER);
		addSetting(delay);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(recastTimer > 0)
		{
			recastTimer--;
			return;
		}

		EntityFishHook hook = p.fishEntity;

		if(hook == null)
		{
			// rod not in use: cast
			doRightClick();
			recastTimer = delay.getValueI();
			waitingForBite = true;
			return;
		}

		if(!waitingForBite)
			return;

		// detect a catch: vanilla sets caughtEntity while reeling in
		if(caughtEntityField == null)
			caughtEntityField = ReflectUtils.field(EntityFishHook.class,
				"caughtEntity", "field_146043_c");
		if(caughtEntityField == null)
			return;

		try
		{
			Object caught = caughtEntityField.get(hook);
			if(caught != null)
			{
				// reel in
				doRightClick();
				waitingForBite = false;
				recastTimer = delay.getValueI();
			}
		}catch(Throwable t)
		{
			// reflection failed
		}
	}

	private void doRightClick()
	{
		if(rightClickMouseMethod == null)
			rightClickMouseMethod = ReflectUtils.method(
				net.minecraft.client.Minecraft.class,
				"rightClickMouse", "func_147121_ag");
		if(rightClickMouseMethod == null)
			return;
		try
		{
			rightClickMouseMethod.invoke(mc);
		}catch(Throwable t)
		{
			// reflection failed
		}
	}

	@Override
	protected void onDisable()
	{
		waitingForBite = false;
		recastTimer = 0;
	}
}
