/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoTotemHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Moves a totem of undying into the offhand when your health drops
 * below the configured threshold, using a 3-click swap through the
 * inventory container (works with the GUI closed).
 */
public final class AutoTotemHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting health =
		new SliderSetting("Health", 6, 2, 20, 1);
	private final SliderSetting delay =
		new SliderSetting("Delay (ticks)", 2, 0, 20, 1);

	private int timer;

	public AutoTotemHack()
	{
		super("AutoTotem",
			"Automatically puts a totem of undying into your offhand"
				+ " when your health is low.");
		setCategory(Category.COMBAT);
		addSettings(health, delay);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.playerController == null)
			return;
		if(p.getHealth() > health.getValueF())
			return;
		if(!p.getHeldItemOffhand().isEmpty()
			&& p.getHeldItemOffhand().getItem() == Items.TOTEM_OF_UNDYING)
			return;

		if(timer > 0)
		{
			timer--;
			return;
		}
		timer = delay.getValueI();

		// find a totem in the main inventory (hotbar last)
		int invSlot = -1;
		for(int i = 9; i < 36 && invSlot < 0; i++)
			if(isTotem(p.inventory.mainInventory.get(i)))
				invSlot = i;
		for(int i = 0; i < 9 && invSlot < 0; i++)
			if(isTotem(p.inventory.mainInventory.get(i)))
				invSlot = i;

		if(invSlot < 0)
			return;

		// map inventory index -> container slot (0-8 hotbar = 36-44)
		int containerSlot = invSlot < 9 ? invSlot + 36 : invSlot;
		int windowId = p.inventoryContainer.windowId;

		// pickup totem, swap with offhand (slot 45), put leftover back
		mc.playerController.windowClick(windowId, containerSlot, 0,
			ClickType.PICKUP, p);
		mc.playerController.windowClick(windowId, 45, 0,
			ClickType.PICKUP, p);
		mc.playerController.windowClick(windowId, containerSlot, 0,
			ClickType.PICKUP, p);
	}

	private static boolean isTotem(ItemStack stack)
	{
		return !stack.isEmpty()
			&& stack.getItem() == Items.TOTEM_OF_UNDYING;
	}
}
