/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ExcavatorHack (GPL-3.0-or-later)
 * Simplified for 1.12.2: digs everything within range, top down.
 */
package net.wurst0.hacks;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

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
import net.wurst0.util.ReflectUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class ExcavatorHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 6, 2, 10, 1);
	private final SliderSetting perTick =
		new SliderSetting("Blocks per tick", 4, 1, 16, 1);
	private final CheckboxSetting swing =
		new CheckboxSetting("Swing", true);

	private Field curBlockDamageMP;

	public ExcavatorHack()
	{
		super("Excavator",
			"Digs every breakable block within range, starting from"
				+ " the top. Very obvious and slow.");
		setCategory(Category.BLOCKS);
		addSettings(range, perTick, swing);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		PlayerControllerMP pc = mc.playerController;
		if(p == null || mc.world == null || pc == null)
			return;

		if(curBlockDamageMP == null)
			curBlockDamageMP = ReflectUtils.field(PlayerControllerMP.class,
				"curBlockDamageMP", "field_78770_f");

		double rangeSq = range.getValue() * range.getValue();
		int r = (int)Math.ceil(range.getValue());

		BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);
		Iterator<BlockPos> itr = BlockPos.getAllInBox(
			center.add(-r, -r, -r), center.add(r, r, r)).iterator();

		List<BlockPos> candidates = new ArrayList<BlockPos>();
		while(itr.hasNext())
		{
			BlockPos pos = itr.next();
			IBlockState state = mc.world.getBlockState(pos);
			Material material = state.getMaterial();
			if(material == Material.AIR || material.isLiquid())
				continue;
			if(state.getBlockHardness(mc.world, pos) < 0)
				continue;
			if(ProtectHack.isProtected(pos))
				continue;
			if(p.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5,
				pos.getZ() + 0.5) > rangeSq)
				continue;
			candidates.add(pos);
		}

		if(candidates.isEmpty())
			return;

		// dig from the top down
		Collections.sort(candidates, Comparator.comparingInt(
			pos -> -pos.getY()));

		int count = 0;
		for(BlockPos pos : candidates)
		{
			if(count >= perTick.getValueI())
				break;

			pc.clickBlock(pos, EnumFacing.UP);
			if(curBlockDamageMP != null)
			{
				try
				{
					curBlockDamageMP.setFloat(pc, 1F);
				}catch(Throwable t)
				{
					// no instant break without the field
				}
			}
			pc.onPlayerDamageBlock(pos, EnumFacing.UP);
			if(swing.isChecked())
				p.swingArm(EnumHand.MAIN_HAND);
			count++;
		}
	}

	@Override
	protected void onDisable()
	{
		curBlockDamageMP = null;
	}
}
