/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoStealHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class AutoStealHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting delay =
		new SliderSetting("Delay (ticks)", 4, 0, 20, 1);
	private final CheckboxSetting closeWhenDone =
		new CheckboxSetting("Close when done", true);

	private int timer;

	public AutoStealHack()
	{
		super("AutoSteal",
			"Automatically takes everything out of chests you open.");
		setCategory(Category.ITEMS);
		addSettings(delay, closeWhenDone);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.playerController == null)
			return;
		if(!(mc.currentScreen instanceof GuiChest))
			return;
		if(!(p.openContainer instanceof ContainerChest))
			return;

		if(timer > 0)
		{
			timer--;
			return;
		}
		timer = delay.getValueI();

		ContainerChest chest = (ContainerChest)p.openContainer;
		IInventory inv = chest.getLowerChestInventory();
		int size = inv.getSizeInventory();

		for(int i = 0; i < size; i++)
		{
			if(inv.getStackInSlot(i).isEmpty())
				continue;

			mc.playerController.windowClick(p.openContainer.windowId, i, 0,
				ClickType.QUICK_MOVE, p);
			return;
		}

		// chest is empty
		if(closeWhenDone.isChecked())
		{
			p.closeScreen();
			mc.displayGuiScreen(null);
		}
	}
}
