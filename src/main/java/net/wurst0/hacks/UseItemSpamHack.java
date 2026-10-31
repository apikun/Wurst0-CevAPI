/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI UseItemSpamHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.ReflectCalls;

/**
 * Spams the right-click action with the held item at a fixed
 * interval (fireballs, projectiles, throwables, etc.). Uses the
 * shared rightClickMouse reflection (MCP + SRG dual name).
 */
public final class UseItemSpamHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting interval =
		new SliderSetting("Interval (ticks)", 4, 1, 40, 1);
	private final CheckboxSetting onlyWithItem =
		new CheckboxSetting("Only with item", true);

	private int timer;

	public UseItemSpamHack()
	{
		super("UseItemSpam",
			"Right-clicks with your held item as fast as the interval"
				+ " allows.");
		setCategory(Category.ITEMS);
		addSettings(interval, onlyWithItem);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.playerController == null)
			return;

		if(onlyWithItem.isChecked() && p.getHeldItemMainhand().isEmpty())
			return;

		if(--timer > 0)
			return;
		timer = interval.getValueI();

		ReflectCalls.rightClickMouse();
	}

	@Override
	protected void onEnable()
	{
		timer = 0;
	}
}
