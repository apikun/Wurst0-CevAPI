/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiCactusHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.util.DamageSource;
import net.wurst0.Category;
import net.wurst0.event.events.DamageEvent;
import net.wurst0.event.listeners.DamageListener;
import net.wurst0.feature.Hack;

public final class AntiCactusHack extends Hack implements DamageListener
{
	public AntiCactusHack()
	{
		super("AntiCactus",
			"Prevents you from taking damage from cacti.");
		setCategory(Category.BLOCKS);
	}

	@Override
	public void onDamage(DamageEvent event)
	{
		if(event.getSource() == DamageSource.cactus)
			event.setCancelled(true);
	}
}
