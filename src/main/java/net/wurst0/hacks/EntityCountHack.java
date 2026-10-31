/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI EntityCountHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.wurst0.Category;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.feature.Hack;

/**
 * Shows how many entities are currently loaded, split into
 * players / hostiles / passives / items. Counts are refreshed
 * every 10 ticks.
 */
public final class EntityCountHack extends Hack implements ScreenRenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private int total;
	private int players;
	private int hostiles;
	private int passives;
	private int items;
	private int refreshTimer;

	public EntityCountHack()
	{
		super("EntityCount",
			"Shows the number of loaded entities around you.");
		setCategory(Category.TOOLS);
	}

	private void refresh()
	{
		total = players = hostiles = passives = items = 0;
		if(mc.world == null)
			return;

		List<Entity> loaded = mc.world.loadedEntityList;
		for(Entity e : loaded)
		{
			if(!(e instanceof EntityPlayer) && !e.isEntityAlive())
				continue;

			total++;
			if(e instanceof EntityPlayer)
				players++;
			else if(e instanceof IMob)
				hostiles++;
			else if(e instanceof EntityItem)
				items++;
			else if(e instanceof EntityAnimal || e instanceof EntityAmbientCreature
				|| e instanceof EntitySquid || e instanceof EntityWaterMob)
				passives++;
		}
	}

	@Override
	public void onScreenRender(float partialTicks)
	{
		if(mc.world == null || mc.player == null
			|| mc.gameSettings.showDebugInfo)
			return;

		if(--refreshTimer <= 0)
		{
			refresh();
			refreshTimer = 10;
		}

		FontRenderer fr = mc.fontRenderer;
		ScaledResolution sr = new ScaledResolution(mc);
		String[] lines = {
			"Entities: " + total,
			"Players: " + players + " Hostile: " + hostiles,
			"Passive: " + passives + " Items: " + items
		};

		int y = 2;
		for(String line : lines)
		{
			int x = sr.getScaledWidth() - fr.getStringWidth(line) - 2;
			fr.drawStringWithShadow(line, x, y, 0xA0A0A0);
			y += fr.FONT_HEIGHT + 1;
		}
	}

	@Override
	protected void onEnable()
	{
		refreshTimer = 0;
	}
}
