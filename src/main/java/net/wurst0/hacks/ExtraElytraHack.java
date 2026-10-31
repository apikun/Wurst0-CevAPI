/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ExtraElytraHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Automatically swaps your nearly-broken elytra for a fresh one
 * from the inventory, using the same 3-click swap as AutoTotem
 * (works with the GUI closed, chestplate slot = container slot 6).
 */
public final class ExtraElytraHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting durability =
		new SliderSetting("Durability threshold (%)", 10, 1, 50, 1);
	private final SliderSetting delay =
		new SliderSetting("Delay (ticks)", 2, 0, 20, 1);

	private int timer;

	public ExtraElytraHack()
	{
		super("ExtraElytra",
			"Automatically replaces your elytra before it breaks.");
		setCategory(Category.MOVEMENT);
		addSettings(durability, delay);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.playerController == null)
			return;

		ItemStack chest = p.inventory.armorInventory.get(2);
		if(!isBrokenElytra(chest))
			return;

		if(timer > 0)
		{
			timer--;
			return;
		}
		timer = delay.getValueI();

		// find a healthy replacement elytra (main inventory first,
		// then hotbar)
		int invSlot = -1;
		for(int i = 9; i < 36 && invSlot < 0; i++)
			if(isHealthyElytra(p.inventory.mainInventory.get(i)))
				invSlot = i;
		for(int i = 0; i < 9 && invSlot < 0; i++)
			if(isHealthyElytra(p.inventory.mainInventory.get(i)))
				invSlot = i;

		if(invSlot < 0)
			return;

		// map inventory index -> container slot (hotbar 0-8 = 36-44)
		int containerSlot = invSlot < 9 ? invSlot + 36 : invSlot;
		int windowId = p.inventoryContainer.windowId;

		// pickup new elytra, swap with chestplate (slot 6), put
		// the broken one back into the inventory
		mc.playerController.windowClick(windowId, containerSlot, 0,
			ClickType.PICKUP, p);
		mc.playerController.windowClick(windowId, 6, 0,
			ClickType.PICKUP, p);
		mc.playerController.windowClick(windowId, containerSlot, 0,
			ClickType.PICKUP, p);
	}

	private boolean isBrokenElytra(ItemStack stack)
	{
		if(stack.isEmpty() || !(stack.getItem() instanceof ItemElytra))
			return false;

		int max = stack.getMaxDamage();
		int left = max - stack.getItemDamage();
		int threshold = max * durability.getValueI() / 100;
		return left <= Math.max(1, threshold)
			|| stack.getItemDamage() >= max - 1;
	}

	private boolean isHealthyElytra(ItemStack stack)
	{
		if(stack.isEmpty() || !(stack.getItem() instanceof ItemElytra))
			return false;
		return stack.getItemDamage() < stack.getMaxDamage() - 1;
	}

	@Override
	protected void onEnable()
	{
		timer = 0;
	}
}
