/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI DamageDetectHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Prints a chat line whenever an entity near you takes damage or
 * dies. Lite port of the original health-watch logic.
 */
public final class DamageDetectHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 32, 8, 64, 8);
	private final CheckboxSetting showDeaths =
		new CheckboxSetting("Show deaths", true);

	private final Map<Integer, Float> lastHealth =
		new HashMap<Integer, Float>();
	private final Map<Integer, Integer> hurtCooldown =
		new HashMap<Integer, Integer>();

	public DamageDetectHack()
	{
		super("DamageDetect",
			"Tells you when entities near you take damage.");
		setCategory(Category.OTHER);
		addSettings(range, showDeaths);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
		{
			lastHealth.clear();
			hurtCooldown.clear();
			return;
		}

		double rangeSq = range.getValue() * range.getValue();

		// tick cooldowns
		Iterator<Map.Entry<Integer, Integer>> cd =
			hurtCooldown.entrySet().iterator();
		while(cd.hasNext())
		{
			Map.Entry<Integer, Integer> e = cd.next();
			if(e.getValue() <= 1)
				cd.remove();
			else
				e.setValue(e.getValue() - 1);
		}

		List<Entity> entities =
			new ArrayList<Entity>(mc.world.loadedEntityList);
		for(Entity e : entities)
		{
			if(!(e instanceof EntityLivingBase) || e == p)
				continue;
			if(!e.isEntityAlive())
				continue;
			if(e.getDistanceSq(p.posX, p.posY, p.posZ) > rangeSq)
				continue;

			EntityLivingBase base = (EntityLivingBase)e;
			Integer id = Integer.valueOf(e.getEntityId());
			Float prev = lastHealth.get(id);

			if(prev != null)
			{
				float ph = prev.floatValue();
				if(base.hurtTime > 0 && !hurtCooldown.containsKey(id))
				{
					float delta = ph - base.getHealth();
					if(delta > 0.01F)
					{
						hurtCooldown.put(id, Integer.valueOf(60));
						p.sendMessage(new TextComponentString(
							"\u00a77[DamageDetect]\u00a7r " + base.getName()
								+ " \u00a7ctook " + String.format("%.1f", delta)
								+ " damage\u00a7r ("
								+ String.format("%.1f", base.getHealth())
								+ "/" + String.format("%.1f",
									base.getMaxHealth()) + ")"));
					}
				}
				if(showDeaths.isChecked() && ph > 0
					&& !base.isEntityAlive())
					p.sendMessage(new TextComponentString(
						"\u00a77[DamageDetect]\u00a7r " + base.getName()
							+ " \u00a74died\u00a7r."));
			}

			lastHealth.put(id, Float.valueOf(base.getHealth()));
		}
	}

	@Override
	protected void onDisable()
	{
		lastHealth.clear();
		hurtCooldown.clear();
	}
}
