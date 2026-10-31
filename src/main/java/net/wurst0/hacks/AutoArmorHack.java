/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoArmorHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayDeque;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

public final class AutoArmorHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	// queued clicks: [containerSlot, button, clickType]
	private final ArrayDeque<int[]> clickQueue = new ArrayDeque<int[]>();
	private int clickDelay;

	public AutoArmorHack()
	{
		super("AutoArmor",
			"Automatically equips the best armor from your inventory.");
		setCategory(Category.COMBAT);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null || mc.playerController == null)
			return;
		if(p.openContainer != p.inventoryContainer)
			return;

		// run the queued clicks, one every other tick
		if(!clickQueue.isEmpty())
		{
			if(--clickDelay > 0)
				return;
			clickDelay = 2;

			int[] click = clickQueue.poll();
			mc.playerController.windowClick(p.inventoryContainer.windowId,
				click[0], click[1], ClickType.values()[click[2]], p);
			return;
		}

		for(int armorIdx = 0; armorIdx < 4; armorIdx++)
		{
			ItemStack current = p.inventory.armorInventory.get(armorIdx);
			float bestScore = score(current, armorIdx);
			int bestSlot = -1;

			for(int slot = 9; slot < 45; slot++)
			{
				ItemStack stack =
					p.inventoryContainer.getSlot(slot).getStack();
				if(stack.isEmpty())
					continue;

				float s = score(stack, armorIdx);
				if(s > bestScore)
				{
					bestScore = s;
					bestSlot = slot;
				}
			}

			if(bestSlot == -1)
				continue;

			// armor container slots are 5..8 (5 = head, 8 = feet)
			int armorSlot = 8 - armorIdx;

			// pick up the better piece, swap it into the armor slot,
			// then return the displaced piece to the inventory slot
			clickQueue.add(new int[]{bestSlot, 0, ClickType.PICKUP.ordinal()});
			clickQueue.add(new int[]{armorSlot, 0, ClickType.PICKUP.ordinal()});
			clickQueue.add(new int[]{bestSlot, 0, ClickType.PICKUP.ordinal()});
			clickDelay = 2;
			return;
		}
	}

	private float score(ItemStack stack, int armorIdx)
	{
		if(stack.isEmpty() || !(stack.getItem() instanceof ItemArmor))
			return -1F;

		ItemArmor armor = (ItemArmor)stack.getItem();
		if(armor.armorType.getSlotIndex() != armorIdx)
			return -1F;

		float reduce = armor.getArmorMaterial()
			.getDamageReductionAmount(armor.armorType);
		int prot = EnchantmentHelper.getEnchantmentLevel(
			Enchantments.PROTECTION, stack);
		return reduce + prot * 1.5F;
	}

	@Override
	protected void onDisable()
	{
		clickQueue.clear();
	}
}
