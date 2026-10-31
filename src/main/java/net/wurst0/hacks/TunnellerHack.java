/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TunnellerHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * Digs a 2-block-tall tunnel straight ahead in the direction you
 * are facing (eye level first, then feet level), optionally walking
 * forward while both blocks are clear. Same dig path as Nuker.
 * Bedrock and other unbreakable blocks stop it.
 */
public final class TunnellerHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final CheckboxSetting walk =
		new CheckboxSetting("Walk forward", true);
	private final CheckboxSetting swing =
		new CheckboxSetting("Swing", true);

	private BlockPos lastDug;

	public TunnellerHack()
	{
		super("Tunneller",
			"Digs a two block tall tunnel straight ahead, optionally"
				+ " walking forward as it goes.");
		setCategory(Category.BLOCKS);
		addSettings(walk, swing);
	}

	/**
	 * @return true if the block position is clear after this call
	 */
	private boolean clearOrDig(EntityPlayerSP p, BlockPos pos)
	{
		IBlockState state = mc.world.getBlockState(pos);
		Material material = state.getMaterial();
		if(material == Material.AIR || material.isLiquid())
			return true;
		if(state.getBlockHardness(mc.world, pos) < 0)
			return false; // unbreakable - stop here

		PlayerControllerMP pc = mc.playerController;
		if(!pos.equals(lastDug))
		{
			pc.clickBlock(pos, EnumFacing.UP);
			lastDug = pos;
		}
		pc.onPlayerDamageBlock(pos, EnumFacing.UP);

		if(swing.isChecked())
			p.swingArm(EnumHand.MAIN_HAND);
		return false;
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		EnumFacing dir = p.getHorizontalFacing();
		BlockPos feet = new BlockPos(p.posX, p.posY, p.posZ);
		BlockPos eyeAhead = feet.up().offset(dir, 1);
		BlockPos feetAhead = feet.offset(dir, 1);

		// dig eye level first to avoid suffocation, then feet level
		boolean clear = clearOrDig(p, eyeAhead);
		if(clear)
			clear = clearOrDig(p, feetAhead);

		if(clear && walk.isChecked() && mc.currentScreen == null
			&& !p.isRiding() && p.movementInput != null)
			p.movementInput.moveForward = 1;
	}

	@Override
	protected void onDisable()
	{
		lastDug = null;
		if(mc.player != null && mc.player.movementInput != null)
			mc.player.movementInput.moveForward = 0;
	}
}
