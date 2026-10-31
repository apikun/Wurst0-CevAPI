/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI HandNoClipHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.events.HandRenderEvent;
import net.wurst0.event.listeners.HandRenderListener;
import net.wurst0.feature.Hack;

public final class HandNoClipHack extends Hack implements HandRenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public HandNoClipHack()
	{
		super("HandNoClip",
			"Hides your hand when it would clip through walls"
				+ " (e.g. when your head is inside a block).");
		setCategory(Category.BLOCKS);
	}

	@Override
	public void onHandRender(HandRenderEvent event)
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		BlockPos eyePos = new BlockPos(p.posX, p.posY + p.getEyeHeight(),
			p.posZ);
		IBlockState state = mc.world.getBlockState(eyePos);
		if(state.isOpaqueCube())
			event.setCancelled(true);
	}
}
