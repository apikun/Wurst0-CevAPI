/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI LiquidsHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.feature.Hack;

/**
 * Lets you place blocks on and against liquids: normally the crosshair
 * ignores liquids entirely (right-clicking water is an "empty click").
 * We re-raytrace with liquids included and send the right-click on the
 * liquid block; the server accepts it because water/lava source blocks
 * are replaceable for placement.
 */
public final class LiquidsHack extends Hack
{
        private final Minecraft mc = Minecraft.getMinecraft();

        public LiquidsHack()
        {
                super("Liquids",
                        "Allows you to place blocks on liquids like water and lava.");
                setCategory(Category.BLOCKS);
        }

        /** Called by ForgeEvents on PlayerInteractEvent.RightClickEmpty. */
        public void handleEmptyClick()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null || mc.playerController == null)
                        return;

                ItemStack held = p.getHeldItemMainhand();
                if(held.isEmpty() || !(held.getItem() instanceof ItemBlock))
                        return;

                float partialTicks = 1.0F;
                Vec3d eye = p.getPositionEyes(partialTicks);
                Vec3d look = p.getLook(partialTicks);
                double reach = mc.playerController.getBlockReachDistance();
                Vec3d end = eye.add(look.scale(reach));

                // include liquids in the trace (stopOnLiquid = true)
                RayTraceResult hit = mc.world.rayTraceBlocks(eye, end, true,
                        false, false);
                if(hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK)
                        return;

                BlockPos pos = hit.getBlockPos();
                IBlockState state = mc.world.getBlockState(pos);
                if(state.getMaterial() != Material.WATER
                        && state.getMaterial() != Material.LAVA)
                        return;

                EnumFacing face =
                        hit.sideHit == null ? EnumFacing.UP : hit.sideHit;
                Vec3d hitVec = hit.hitVec == null
                        ? new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5,
                                pos.getZ() + 0.5)
                        : hit.hitVec;

                mc.playerController.processRightClickBlock(p,
                        (WorldClient)mc.world, pos, face, hitVec, EnumHand.MAIN_HAND);
        }
}
