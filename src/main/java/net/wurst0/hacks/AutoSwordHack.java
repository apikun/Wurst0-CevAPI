/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoSwordHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.Collection;

import com.google.common.collect.Multimap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

public final class AutoSwordHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public AutoSwordHack()
	{
		super("AutoSword",
			"Automatically equips the weapon with the highest attack"
				+ " damage from your hotbar when you attack.");
		setCategory(Category.COMBAT);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;
		if(mc.currentScreen != null)
			return;
		if(!mc.gameSettings.keyBindAttack.isKeyDown())
			return;

		float bestDamage = getAttackDamage(p.inventory.getCurrentItem());
		int bestSlot = -1;

		for(int i = 0; i < 9; i++)
		{
			float damage = getAttackDamage(p.inventory.mainInventory.get(i));
			if(damage > bestDamage)
			{
				bestDamage = damage;
				bestSlot = i;
			}
		}

		if(bestSlot != -1 && bestSlot != p.inventory.currentItem)
		{
			p.inventory.currentItem = bestSlot;
			mc.playerController.updateController();
		}
	}

	private float getAttackDamage(ItemStack stack)
	{
		if(stack.isEmpty())
			return 0;

		Multimap<String, AttributeModifier> modifiers =
			stack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND);
		if(modifiers == null)
			return 0;

		Collection<AttributeModifier> mods =
			modifiers.get(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
		float total = 0;
		for(AttributeModifier mod : mods)
			total += mod.getAmount();
		return total;
	}
}
