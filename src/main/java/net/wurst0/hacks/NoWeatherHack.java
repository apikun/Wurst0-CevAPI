/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NoWeatherHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

public final class NoWeatherHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public NoWeatherHack()
	{
		super("NoWeather",
			"Disables rain and thunder visually, so you can always see"
				+ " the sky clearly.");
		setCategory(Category.RENDER);
	}

	@Override
	public void onUpdate()
	{
		World world = mc.world;
		if(world == null)
			return;

		world.setRainStrength(0F);
		world.setThunderStrength(0F);
	}
}
