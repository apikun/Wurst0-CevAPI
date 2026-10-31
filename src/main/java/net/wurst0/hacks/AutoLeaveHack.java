/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoLeaveHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.gui.GuiMainMenu;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class AutoLeaveHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting health =
		new SliderSetting("Health", 4, 1, 20, 1);

	public AutoLeaveHack()
	{
		super("AutoLeave",
			"Automatically leaves the server when your health is"
				+ " low.");
		setCategory(Category.OTHER);
		addSetting(health);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null)
			return;

		if(p.getHealth() > health.getValueF())
			return;

		// disconnect the same way AutoReconnect rejoins
		WorldClient world = mc.world;
		world.sendQuittingDisconnectingPacket();
		mc.loadWorld((WorldClient)null);
		mc.displayGuiScreen(new GuiMainMenu());

		setEnabled(false);
	}
}
