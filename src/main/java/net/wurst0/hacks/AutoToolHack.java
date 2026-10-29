/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoToolHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

public final class AutoToolHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public AutoToolHack()
	{
		super("AutoTool",
			"Automatically switches to the best tool in your hotbar"
				+ " for the block you are looking at or breaking.");
		setCategory(Category.BLOCKS);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;
		if(mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit
			!= RayTraceResult.Type.BLOCK)
			return;

		BlockPos pos = mc.objectMouseOver.getBlockPos();
		if(pos == null)
			return;

		IBlockState state = mc.world.getBlockState(pos);
		int current = p.inventory.currentItem;

		int best = current;
		float bestScore = score(p.inventory.getStackInSlot(current), state);

		for(int i = 0; i < 9; i++)
		{
			float score = score(p.inventory.getStackInSlot(i), state);
			if(score > bestScore)
			{
				bestScore = score;
				best = i;
			}
		}

		if(best != current)
			p.inventory.currentItem = best;
	}

	private float score(ItemStack stack, IBlockState state)
	{
		if(stack == null || stack.isEmpty())
			return -1;

		if(stack.getMaxDamage() > 0
			&& stack.getMaxDamage() - stack.getItemDamage() <= 1)
			return -1;

		float efficiency = stack.getItem().getStrVsBlock(stack, state);
		if(stack.canHarvestBlock(state))
			efficiency += 1000;

		return efficiency;
	}
}
