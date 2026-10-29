/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TimerHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class TimerHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting speed =
		new SliderSetting("Speed", 1, 0.1, 10, 0.05);

	public TimerHack()
	{
		super("Timer",
			"Changes how fast the game ticks (slows down or speeds up"
				+ " the whole game).");
		setCategory(Category.TOOLS);
		addSetting(speed);
	}

	@Override
	public void onUpdate()
	{
		mc.timer.tickLength = 50F / speed.getValueF();
	}

	@Override
	protected void onDisable()
	{
		mc.timer.tickLength = 50F;
	}
}
