/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NoPumpkinHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.feature.Hack;

/**
 * Removes the pumpkin blur overlay while wearing a carved pumpkin,
 * without hiding other helmet overlays (unlike NoOverlay).
 */
public final class NoPumpkinHack extends Hack
{
	public NoPumpkinHack()
	{
		super("NoPumpkin",
			"Removes the pumpkin overlay while wearing a carved"
				+ " pumpkin.");
		setCategory(Category.RENDER);
	}

	/**
	 * Called by ForgeEvents for the HELMET overlay element.
	 *
	 * @return true to cancel the vanilla helmet overlay
	 */
	public boolean shouldCancelHelmetOverlay()
	{
		if(mc.player == null)
			return false;
		ItemStack helmet =
			mc.player.inventory.armorItemInSlot(3);
		if(helmet.isEmpty()
			|| !(helmet.getItem() instanceof ItemBlock))
			return false;
		return ((ItemBlock)helmet.getItem()).getBlock() == Blocks.PUMPKIN;
	}

	private final net.minecraft.client.Minecraft mc =
		net.minecraft.client.Minecraft.getMinecraft();
}
