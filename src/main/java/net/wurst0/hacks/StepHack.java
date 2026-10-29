/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI StepHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class StepHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting height =
		new SliderSetting("Height", 1, 0.5, 10, 0.5);

	public StepHack()
	{
		super("Step",
			"Lets you step up full blocks (and more) without jumping.");
		setCategory(Category.MOVEMENT);
		addSetting(height);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null)
			return;
		p.stepHeight = height.getValueF();
	}

	@Override
	protected void onDisable()
	{
		if(mc.player != null)
			mc.player.stepHeight = 0.6F;
	}
}
