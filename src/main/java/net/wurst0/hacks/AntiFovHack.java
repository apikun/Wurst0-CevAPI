/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiFovHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class AntiFovHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting fov = new SliderSetting("FOV", 90, 30, 110, 1);

	public AntiFovHack()
	{
		super("AntiFov",
			"Locks your field of view so speed effects and items can't"
				+ " change it.");
		setCategory(Category.RENDER);
		addSetting(fov);
	}

	@Override
	public void onUpdate()
	{
		if(mc.gameSettings != null
			&& mc.gameSettings.fovSetting != fov.getValueF())
			mc.gameSettings.fovSetting = fov.getValueF();
	}
}
