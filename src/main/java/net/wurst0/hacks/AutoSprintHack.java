/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoSprintHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

public final class AutoSprintHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final CheckboxSetting allDirections =
		new CheckboxSetting("AllDirections", false);

	public AutoSprintHack()
	{
		super("AutoSprint",
			"Makes you sprint automatically while moving forward.");
		setCategory(Category.MOVEMENT);
		addSetting(allDirections);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;
		if(p.isSneaking() || p.isRiding())
			return;

		boolean moving = p.movementInput.moveForward > 0
			|| (allDirections.isChecked()
				&& (p.movementInput.moveForward != 0
					|| p.movementInput.moveStrafe != 0));

		if(moving && !p.isSprinting())
			p.setSprinting(true);
	}
}
