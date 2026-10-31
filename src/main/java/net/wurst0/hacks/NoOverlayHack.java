/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NoOverlayHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.wurst0.Category;
import net.wurst0.event.events.OverlayEvent;
import net.wurst0.event.listeners.OverlayListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * 1.12.2 note: Forge exposes the pumpkin blur as ElementType.HELMET
 * and the portal overlay as ElementType.PORTAL. The fire/water
 * overlays are not separate element types in 1.12.2, so they can
 * only be removed with a mixin (not implemented yet).
 */
public final class NoOverlayHack extends Hack implements OverlayListener
{
	private final CheckboxSetting pumpkin =
		new CheckboxSetting("Pumpkin", true);
	private final CheckboxSetting portal =
		new CheckboxSetting("Portal", true);

	public NoOverlayHack()
	{
		super("NoOverlay",
			"Removes screen overlays such as the pumpkin blur and"
				+ " the nether portal overlay.");
		setCategory(Category.RENDER);
		addSettings(pumpkin, portal);
	}

	@Override
	public void onOverlayRender(OverlayEvent event)
	{
		String type = event.getType();
		if(pumpkin.isChecked() && "HELMET".equals(type))
			event.setCancelled(true);
		else if(portal.isChecked() && "PORTAL".equals(type))
			event.setCancelled(true);
	}
}
