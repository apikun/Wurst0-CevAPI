/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ThrowHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class ThrowHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting delay =
		new SliderSetting("Delay (ticks)", 2, 1, 20, 1);

	private int timer;

	public ThrowHack()
	{
		super("Throw",
			"Throws the items in your hand (Q drops one item at a"
				+ " time, this drops your whole stack at once).");
		setCategory(Category.FUN);
		addSetting(delay);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null)
			return;

		if(p.inventory.getCurrentItem().isEmpty())
			return;

		if(timer > 0)
		{
			timer--;
			return;
		}
		timer = delay.getValueI();

		p.dropItem(true);
	}
}
