/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI KillPotionHack (GPL-3.0-or-later)
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
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.ModeSetting;

/**
 * Creative-only: replaces the item in your hand with a potion that
 * deals absurd instant damage (+ wither), enough to kill anything in
 * one hit. Runs once per activation; must be in creative mode.
 */
public final class KillPotionHack extends Hack
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final ModeSetting type = new ModeSetting("Type",
		new String[]{"Drinkable", "Splash", "Lingering"}, 0);
	private final CheckboxSetting wither =
		new CheckboxSetting("Wither on top", true);

	public KillPotionHack()
	{
		super("KillPotion",
			"Creative-only. Gives you a potion that instantly kills"
				+ " whatever drinks it.");
		setCategory(Category.ITEMS);
		addSettings(type, wither);
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
				"\u00a7c[KillPotion]\u00a7r Creative mode required."));
			setEnabled(false);
			return;
		}

		ItemStack potion = new ItemStack(getPotionItem(), 1, 0);
		NBTTagList effects = new NBTTagList();

		// Instant Damage, amplifier 124 (level ~125)
		effects.appendTag(effect(7, 124, 1, true));
		if(wither.isChecked())
			effects.appendTag(effect(20, 124, 200, true));

		NBTTagCompound tag = new NBTTagCompound();
		tag.setTag("CustomPotionEffects", effects);
		potion.setTagCompound(tag);

		mc.playerController.sendSlotPacket(potion,
			p.inventory.currentItem + 36);
		p.sendMessage(new TextComponentString(
			"\u00a7a[KillPotion]\u00a7r Potion placed in your hand."));
		setEnabled(false);
	}

	private static NBTTagCompound effect(int id, int amplifier,
		int duration, boolean particles)
	{
		NBTTagCompound eff = new NBTTagCompound();
		eff.setInteger("Id", id);
		eff.setInteger("Amplifier", amplifier);
		eff.setInteger("Duration", duration);
		eff.setByte("Ambient", (byte)0);
		eff.setByte("ShowParticles", particles ? (byte)1 : (byte)0);
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
