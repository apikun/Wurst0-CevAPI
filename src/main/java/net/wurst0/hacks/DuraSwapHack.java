/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI DuraSwapHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Swaps your held tool for a better one from the hotbar when the
 * current one is about to break (durability below the threshold).
 */
public final class DuraSwapHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting threshold =
		new SliderSetting("Durability (%)", 10, 1, 50, 1);

	public DuraSwapHack()
	{
		super("DuraSwap",
			"Automatically swaps your tool for a better one from the"
				+ " hotbar when it is about to break.");
		setCategory(Category.ITEMS);
		addSetting(threshold);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null)
			return;

		ItemStack held = p.inventory.getCurrentItem();
		if(held.isEmpty() || !held.isItemStackDamageable())
			return;

		int max = held.getMaxDamage();
		int left = max - held.getItemDamage();
		if(left * 100 > max * threshold.getValueI())
			return;

		// find another hotbar slot with the same item in better shape
		int best = -1;
		int bestLeft = left;
		for(int i = 0; i < 9; i++)
		{
			if(i == p.inventory.currentItem)
				continue;
			ItemStack stack = p.inventory.mainInventory.get(i);
			if(stack.isEmpty() || stack.getItem() != held.getItem())
				continue;

			int stackLeft = stack.getMaxDamage()
				- stack.getItemDamage();
			if(stackLeft > bestLeft)
			{
				bestLeft = stackLeft;
				best = i;
			}
		}

		if(best >= 0)
			p.inventory.currentItem = best;
		else
			// nothing to swap to - stop so we don't break the last tool
			setEnabled(false);
	}
}
