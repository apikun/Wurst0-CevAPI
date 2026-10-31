/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SneakHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * Keeps the sneaking flag set while enabled, so you never fall off
 * edges and your nametag stays hidden. Approximate port: the flag is
 * forced every tick (server sees it via the C0B entity action that
 * vanilla sends when isSneaking changes), but the movement-input
 * overrides of the original need a mixin.
 */
public final class SneakHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final CheckboxSetting hideNametag =
		new CheckboxSetting("Hide nametag", true);

	public SneakHack()
	{
		super("Sneak",
			"Keeps you in sneak mode so you don't fall off blocks and"
				+ " your nametag stays hidden.");
		setCategory(Category.MOVEMENT);
		addSetting(hideNametag);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;
		if(mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer))
			return;

		p.setSneaking(true);
		if(p.movementInput != null)
			p.movementInput.sneak = true;
	}

	@Override
	protected void onDisable()
	{
		if(mc.player != null)
		{
			mc.player.setSneaking(false);
			if(mc.player.movementInput != null)
				mc.player.movementInput.sneak = false;
		}
	}

	public boolean shouldHideNametag()
	{
		return hideNametag.isChecked();
	}
}
