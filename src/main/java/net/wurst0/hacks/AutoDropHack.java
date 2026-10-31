/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoDropHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class AutoDropHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting interval =
		new SliderSetting("Interval (ticks)", 20, 5, 200, 5);
	private final CheckboxSetting rottenFlesh =
		new CheckboxSetting("Rotten flesh", true);
	private final CheckboxSetting spiderEye =
		new CheckboxSetting("Spider eye", true);
	private final CheckboxSetting poisonousPotato =
		new CheckboxSetting("Poisonous potato", true);
	private final CheckboxSetting deadBush =
		new CheckboxSetting("Dead bush", true);
	private final CheckboxSetting cobblestone =
		new CheckboxSetting("Cobblestone", false);
	private final CheckboxSetting dirt =
		new CheckboxSetting("Dirt", false);

	private int timer;

	public AutoDropHack()
	{
		super("AutoDrop",
			"Automatically drops unwanted items from your inventory.");
		setCategory(Category.ITEMS);
		addSettings(interval, rottenFlesh, spiderEye, poisonousPotato,
			deadBush, cobblestone, dirt);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.playerController == null)
			return;
		if(p.openContainer != p.inventoryContainer)
			return;

		if(--timer > 0)
			return;
		timer = interval.getValueI();

		// main inventory container slots: 9..35 storage, 36..44 hotbar
		for(int slot = 9; slot < 45; slot++)
		{
			ItemStack stack = p.inventoryContainer.getSlot(slot).getStack();
			if(!isJunk(stack))
				continue;

			// THROW with button 1 drops the whole stack
			mc.playerController.windowClick(
				p.inventoryContainer.windowId, slot, 1, ClickType.THROW, p);
			return;
		}
	}

	private boolean isJunk(ItemStack stack)
	{
		if(stack.isEmpty())
			return false;

		Item item = stack.getItem();
		if(rottenFlesh.isChecked() && item == Items.ROTTEN_FLESH)
			return true;
		if(spiderEye.isChecked() && item == Items.SPIDER_EYE)
			return true;
		if(poisonousPotato.isChecked()
			&& item == Items.POISONOUS_POTATO)
			return true;
		if(deadBush.isChecked()
			&& item == Item.getItemFromBlock(Blocks.DEADBUSH))
			return true;
		if(cobblestone.isChecked()
			&& item == Item.getItemFromBlock(Blocks.COBBLESTONE))
			return true;
		if(dirt.isChecked()
			&& item == Item.getItemFromBlock(Blocks.DIRT))
			return true;

		return false;
	}
}
