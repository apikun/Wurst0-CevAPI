/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI KaboomHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Sends bursts of START/ABORT digging packets around you each tick.
 * On servers with weak or no anti-cheat this can insta-break blocks
 * in the area. The "Sound & particles" option controls whether the
 * START_DESTROY packets (which trigger the block-break effects) are
 * sent as well.
 */
public final class KaboomHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting power =
		new SliderSetting("Power", 6, 1, 32, 1);
	private final CheckboxSetting effects =
		new CheckboxSetting("Sound & particles", true);

	public KaboomHack()
	{
		super("Kaboom",
			"Sends fake block-breaking packets around you. Only works on"
				+ " servers with weak or no anti-cheat.");
		setCategory(Category.BLOCKS);
		addSettings(power, effects);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || p.connection == null)
			return;

		BlockPos base = new BlockPos(p.posX, p.posY, p.posZ);
		Random rand = p.getRNG();
		int n = power.getValueI();

		for(int i = 0; i < n; i++)
		{
			BlockPos pos = base.add(rand.nextInt(5) - 2,
				rand.nextInt(3) - 1, rand.nextInt(5) - 2);

			if(effects.isChecked())
				p.connection.sendPacket(new CPacketPlayerDigging(
					CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos,
					EnumFacing.DOWN));

			p.connection.sendPacket(new CPacketPlayerDigging(
				CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, pos,
				EnumFacing.DOWN));
		}
	}
}
