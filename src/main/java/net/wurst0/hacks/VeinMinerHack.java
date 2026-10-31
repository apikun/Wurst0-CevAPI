/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI VeinMinerHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * While you mine an ore, all connected blocks of the same ore type
 * are queued and broken one per tick (closest first), up to the
 * limit. Uses the same dig path as Nuker.
 */
public final class VeinMinerHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting limit =
		new SliderSetting("Limit", 16, 4, 64, 4);
	private final CheckboxSetting swing =
		new CheckboxSetting("Swing", true);

	private final Queue<BlockPos> vein = new ArrayDeque<BlockPos>();
	private final Set<BlockPos> queued = new HashSet<BlockPos>();
	private BlockPos minedBlock;

	public VeinMinerHack()
	{
		super("VeinMiner",
			"Mines whole ore veins for you while you break one"
				+ " block of the vein.");
		setCategory(Category.BLOCKS);
		addSettings(limit, swing);
	}

	private static boolean isOre(Block block)
	{
		return block == Blocks.COAL_ORE || block == Blocks.IRON_ORE
			|| block == Blocks.GOLD_ORE || block == Blocks.DIAMOND_ORE
			|| block == Blocks.EMERALD_ORE || block == Blocks.LAPIS_ORE
			|| block == Blocks.REDSTONE_ORE
			|| block == Blocks.LIT_REDSTONE_ORE
			|| block == Blocks.QUARTZ_ORE;
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null || mc.playerController == null)
			return;

		// seed from the crosshair while the player is breaking an ore
		if(mc.objectMouseOver != null
			&& mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK
			&& mc.gameSettings.keyBindAttack.isKeyDown()
			&& mc.playerController.getIsHittingBlock())
		{
			BlockPos pos = mc.objectMouseOver.getBlockPos();
			if(pos != null && isOre(mc.world.getBlockState(pos).getBlock()))
				collectVein(pos);
		}

		// drop stale targets that turned into air
		while(!vein.isEmpty() && mc.world.isAirBlock(vein.peek()))
		{
			BlockPos done = vein.poll();
			queued.remove(done);
			if(minedBlock != null && minedBlock.equals(done))
				minedBlock = null;
		}

		BlockPos next = vein.poll();
		if(next == null)
		{
			minedBlock = null;
			return;
		}
		queued.remove(next);

		PlayerControllerMP pc = mc.playerController;
		if(!next.equals(minedBlock))
		{
			pc.clickBlock(next, EnumFacing.UP);
			minedBlock = next;
		}
		pc.onPlayerDamageBlock(next, EnumFacing.UP);

		if(swing.isChecked())
			p.swingArm(EnumHand.MAIN_HAND);
	}

	private void collectVein(BlockPos start)
	{
		Block target = mc.world.getBlockState(start).getBlock();
		int max = limit.getValueI();

		Set<BlockPos> visited = new HashSet<BlockPos>();
		Queue<BlockPos> bfs = new ArrayDeque<BlockPos>();
		bfs.add(start);
		visited.add(start);

		while(!bfs.isEmpty() && queued.size() < max)
		{
			BlockPos cur = bfs.poll();
			for(EnumFacing dir : EnumFacing.VALUES)
			{
				BlockPos next = cur.offset(dir);
				if(!visited.add(next))
					continue;
				if(queued.size() >= max)
					return;

				IBlockState state = mc.world.getBlockState(next);
				if(state.getBlock() != target)
					continue;

				if(queued.add(next))
					vein.add(next);
				bfs.add(next);
			}
		}
	}

	@Override
	protected void onDisable()
	{
		vein.clear();
		queued.clear();
		minedBlock = null;
	}
}
