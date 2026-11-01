/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI OpenWaterEspHack (GPL-3.0-or-later)
 * Lite adaptation: highlights your fishing bobber and the water
 * blocks around it (the "open water" concept was added in 1.13,
 * so this shows the bobber + nearby water instead).
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;

public final class OpenWaterEspHack extends Hack
        implements UpdateListener, RenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private EntityFishHook bobber;
        private final List<AxisAlignedBB> water =
                new ArrayList<AxisAlignedBB>();

        public OpenWaterEspHack()
        {
                super("OpenWaterEsp",
                        "Highlights your fishing bobber and the water blocks"
                                + " around it.");
                setCategory(Category.RENDER);
        }

        @Override
        public void onUpdate()
        {
                bobber = null;
                water.clear();
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                double bestDist = 32 * 32;
                for(EntityFishHook hook : mc.world.getEntities(
                        EntityFishHook.class, e -> true))
                {
                        if(hook.isDead)
                                continue;
                        double d = hook.getDistanceSq(p);
                        if(d < bestDist)
                        {
                                bestDist = d;
                                bobber = hook;
                        }
                }

                if(bobber == null)
                        return;

                BlockPos center = new BlockPos(bobber);
                BlockPos min = center.add(-3, -2, -3);
                BlockPos max = center.add(3, 2, 3);
                int count = 0;
                for(BlockPos pos : BlockPos.getAllInBox(min, max))
                {
                        IBlockState state = mc.world.getBlockState(pos);
                        if(state.getMaterial() != Material.WATER)
                                continue;
                        if(count++ >= 120)
                                break;
                        water.add(new AxisAlignedBB(pos));
                }
        }

        @Override
        public void onRender(float partialTicks)
        {
                if(bobber == null || mc.player == null)
                        return;

                RenderUtils.enable3D(1.5F, false);
                RenderUtils.applyCameraTransform(partialTicks);

                RenderUtils.drawBox(
                        bobber.getEntityBoundingBox().grow(0.15),
                        1F, 0.5F, 0F, 0.7F);

                for(AxisAlignedBB box : water)
                        RenderUtils.drawBox(box, 0.2F, 0.5F, 1F, 0.4F);

                RenderUtils.disable3D(false);
        }

        @Override
        protected void onDisable()
        {
                bobber = null;
                water.clear();
        }
}
