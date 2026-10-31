/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoSoupHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.util.ReflectUtils;
import net.wurst0.settings.SliderSetting;

public final class AutoSoupHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting health =
		new SliderSetting("Health", 10, 1, 19.5, 0.5);

	public AutoSoupHack()
	{
		super("AutoSoup",
			"Automatically eats mushroom stew from your hotbar when your"
				+ " health is low.");
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

		if(p.getHealth() + p.getAbsorptionAmount() > health.getValue())
			return;

		int slot = findSoup(p);
		if(slot == -1)
			return;

		if(p.inventory.currentItem != slot)
		{
			p.inventory.currentItem = slot;
			mc.playerController.updateController();
			return;
		}

		doRightClick();
	}

	private int findSoup(EntityPlayerSP p)
	{
		for(int i = 0; i < 9; i++)
		{
			ItemStack stack = p.inventory.mainInventory.get(i);
			if(!stack.isEmpty() && stack.getItem() == Items.MUSHROOM_STEW)
				return i;
		}
		return -1;
	}

	private static java.lang.reflect.Method rightClickMouseMethod;

	private void doRightClick()
	{
		if(rightClickMouseMethod == null)
			rightClickMouseMethod = ReflectUtils.method(
				net.minecraft.client.Minecraft.class,
				"rightClickMouse", "func_147121_ag");
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
