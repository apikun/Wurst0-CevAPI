/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI RestockHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Refills hotbar stacks that fall below the configured amount by
 * QUICK_MOVE-ing matching stacks out of the main inventory. One
 * click per cycle, works with the inventory closed.
 */
public final class RestockHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting threshold =
		new SliderSetting("Refill below", 8, 1, 32, 1);
	private final SliderSetting delay =
		new SliderSetting("Delay (ticks)", 4, 0, 20, 1);

	private int timer;

	public RestockHack()
	{
		super("Restock",
			"Automatically refills your hotbar with the item you just"
				+ " used or threw.");
		setCategory(Category.ITEMS);
		addSettings(threshold, delay);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.playerController == null)
			return;

		if(timer > 0)
		{
			timer--;
			return;
		}
		timer = delay.getValueI();

		for(int hotbar = 0; hotbar < 9; hotbar++)
		{
			ItemStack hot = p.inventory.mainInventory.get(hotbar);
			if(!hot.isEmpty() && hot.getCount() >= threshold.getValueI())
				continue;
			if(!hot.isEmpty() && !hot.isStackable())
				continue;

			// find a matching stack in the main inventory (9-35)
			for(int i = 9; i < 36; i++)
			{
				ItemStack stack = p.inventory.mainInventory.get(i);
				if(stack.isEmpty())
					continue;
				if(!ItemStack.areItemsEqual(hot, stack)
					&& !hot.isEmpty())
					continue;
				if(hot.isEmpty() && !stack.isStackable())
					continue;
				if(!hot.isEmpty()
					&& hot.isItemStackDamageable()
					&& hot.getItemDamage() != stack.getItemDamage())
					continue;

				// main inventory index i maps 1:1 to container slots 9-35
				mc.playerController.windowClick(
					p.inventoryContainer.windowId, i, 0,
					ClickType.QUICK_MOVE, p);
				return;
			}
		}
	}
}
