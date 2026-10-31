/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TrollPotionHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.feature.Hack;
import net.wurst0.settings.ModeSetting;

/**
 * Creative-only: replaces the held item with a potion carrying a
 * chaotic mix of effects (the classic "troll potion"). Runs once per
 * activation.
 */
public final class TrollPotionHack extends Hack
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final ModeSetting type = new ModeSetting("Type",
		new String[]{"Splash", "Lingering", "Drinkable"}, 0);

	public TrollPotionHack()
	{
		super("TrollPotion",
			"Creative-only. Gives you a potion with a huge stack of"
				+ " mixed effects.");
		setCategory(Category.ITEMS);
		addSetting(type);
	}

	@Override
	protected void onEnable()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.playerController == null)
		{
			setEnabled(false);
			return;
		}

		if(!mc.playerController.isInCreativeMode())
		{
			p.sendMessage(new TextComponentString(
				"\u00a7c[TrollPotion]\u00a7r Creative mode required."));
			setEnabled(false);
			return;
		}

		ItemStack potion = new ItemStack(getPotionItem(), 1, 0);
		NBTTagList effects = new NBTTagList();
		int min = 20 * 60; // one minute in ticks

		effects.appendTag(effect(1, 9, min * 8)); // Speed X
		effects.appendTag(effect(3, 9, min * 8)); // Haste X
		effects.appendTag(effect(5, 9, min * 8)); // Strength X
		effects.appendTag(effect(8, 9, min * 8)); // Jump Boost X
		effects.appendTag(effect(10, 4, min * 2)); // Regeneration V
		effects.appendTag(effect(11, 4, min * 8)); // Resistance V
		effects.appendTag(effect(12, 0, min * 8)); // Fire Resistance
		effects.appendTag(effect(13, 0, min * 8)); // Water Breathing
		effects.appendTag(effect(16, 0, min * 8)); // Night Vision
		effects.appendTag(effect(21, 9, min * 8)); // Health Boost X
		effects.appendTag(effect(22, 4, min * 8)); // Absorption V
		effects.appendTag(effect(24, 0, min)); // Glowing
		effects.appendTag(effect(26, 9, min * 8)); // Luck X

		NBTTagCompound tag = new NBTTagCompound();
		tag.setTag("CustomPotionEffects", effects);
		potion.setTagCompound(tag);

		mc.playerController.sendSlotPacket(potion,
			p.inventory.currentItem + 36);
		p.sendMessage(new TextComponentString(
			"\u00a7a[TrollPotion]\u00a7r Potion placed in your hand."));
		setEnabled(false);
	}

	private static NBTTagCompound effect(int id, int amplifier,
		int duration)
	{
		NBTTagCompound eff = new NBTTagCompound();
		eff.setInteger("Id", id);
		eff.setInteger("Amplifier", amplifier);
		eff.setInteger("Duration", duration);
		eff.setByte("Ambient", (byte)0);
		eff.setByte("ShowParticles", (byte)1);
		return eff;
	}

	private net.minecraft.item.Item getPotionItem()
	{
		if(type.getSelected().equals("Splash"))
			return Items.SPLASH_POTION;
		if(type.getSelected().equals("Lingering"))
			return Items.LINGERING_POTION;
		return Items.POTIONITEM;
	}
}
