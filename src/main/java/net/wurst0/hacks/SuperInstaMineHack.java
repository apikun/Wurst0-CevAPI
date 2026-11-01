/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SuperInstaMineHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.Iterator;

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
import net.wurst0.settings.SliderSetting;

/**
 * Like AirMiner, but designed for big tunnels: digs a lot of
 * blocks per tick in a cube around the block you are looking at.
 */
public final class SuperInstaMineHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting blocksPerTick =
                new SliderSetting("Blocks per tick", 8, 1, 32, 1);
        private final SliderSetting cube =
                new SliderSetting("Cube size", 2, 1, 4, 1);
        private final CheckboxSetting swing =
                new CheckboxSetting("Swing", true);

        public SuperInstaMineHack()
        {
                super("SuperInstaMine",
                        "Digs a whole cube around your crosshair target,"
                                + " several blocks per tick.");
                setCategory(Category.BLOCKS);
                addSettings(blocksPerTick, cube, swing);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                PlayerControllerMP pc = mc.playerController;
                int max = blocksPerTick.getValueI();
                int r = cube.getValueI();

                BlockPos center;
                if(mc.objectMouseOver != null
                        && mc.objectMouseOver.typeOfHit
                                == net.minecraft.util.math.RayTraceResult.Type.BLOCK)
                        center = mc.objectMouseOver.getBlockPos();
                else
                        center = new BlockPos(p.posX, p.posY, p.posZ)
                                .offset(EnumFacing.DOWN);

                int dug = 0;
                Iterator<BlockPos> itr = BlockPos.getAllInBox(
                        center.add(-r, -r, -r), center.add(r, r, r))
                        .iterator();

                while(itr.hasNext() && dug < max)
                {
                        BlockPos pos = itr.next();
                        IBlockState state = mc.world.getBlockState(pos);
                        Material m = state.getMaterial();
                        if(m == Material.AIR || m.isLiquid())
                                continue;
                        if(state.getBlockHardness(mc.world, pos) < 0)
                                continue; // unbreakable (bedrock etc.)
                        if(ProtectHack.isProtected(pos))
                                continue; // protected sphere

                        pc.clickBlock(pos, EnumFacing.UP);
                        pc.onPlayerDamageBlock(pos, EnumFacing.UP);
                        if(swing.isChecked())
                                p.swingArm(EnumHand.MAIN_HAND);
                        dug++;
                }
        }
}
