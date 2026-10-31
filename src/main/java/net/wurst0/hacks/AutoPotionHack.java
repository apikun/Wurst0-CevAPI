/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoPotionHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionTypes;
import net.minecraft.potion.PotionUtils;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.util.ReflectUtils;
import net.wurst0.settings.SliderSetting;

public final class AutoPotionHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting health =
		new SliderSetting("Health", 12, 2, 19.5, 0.5);

	private java.lang.reflect.Method rightClickMouseMethod;
	private int timer;

	public AutoPotionHack()
	{
		super("AutoPotion",
			"Throws splash healing potions when your health is low.");
		setCategory(Category.COMBAT);
		addSetting(health);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;
		if(p.openContainer != p.inventoryContainer)
			return;
		if(p.isHandActive())
			return;

		if(timer > 0)
		{
			timer--;
			return;
		}

		if(p.getHealth() + p.getAbsorptionAmount() > health.getValue())
			return;

		// find a splash healing potion in the hotbar
		int slot = findPotion(p, 0, 9);
		if(slot == -1)
		{
			// pull one out of the main inventory
			int invSlot = findPotion(p, 9, 36);
			if(invSlot == -1)
				return;
			mc.playerController.windowClick(p.inventoryContainer.windowId,
				invSlot + 9, 0, ClickType.SWAP, p);
			timer = 5;
			return;
		}

		if(p.inventory.currentItem != slot)
		{
			p.inventory.currentItem = slot;
			mc.playerController.updateController();
			return;
		}

		doRightClick();
		timer = 15;
	}

	private int findPotion(EntityPlayerSP p, int from, int to)
	{
		for(int i = from; i < to; i++)
		{
			ItemStack stack = p.inventory.mainInventory.get(i);
			if(!isHealingSplash(stack))
				continue;
			return i;
		}
		return -1;
	}

	private boolean isHealingSplash(ItemStack stack)
	{
		if(stack.isEmpty() || stack.getItem() != Items.SPLASH_POTION)
			return false;

		PotionType type = PotionUtils.getPotionFromItem(stack);
		return type == PotionTypes.HEALING
			|| type == PotionTypes.STRONG_HEALING;
	}

	private void doRightClick()
	{
		if(rightClickMouseMethod == null)
			rightClickMouseMethod = ReflectUtils.method(
				net.minecraft.client.Minecraft.class,
				"rightClickMouse", "func_71007_j");
		if(rightClickMouseMethod == null)
			return;
		try
		{
			rightClickMouseMethod.invoke(mc);
		}catch(Throwable t)
		{
			// reflection failed
		}
	}
}
