/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BedrockStashHack (GPL-3.0-or-later)
 *
 * The original is not a dupe: it is a stash-finder ESP that scans the
 * bedrock roof for hidden air pockets (player-built bases inside
 * bedrock). That mechanic works the same in 1.12.2, so this port
 * scans the area around the player (budgeted per tick) and highlights
 * air blocks that are surrounded by bedrock on 3+ sides.
 */
package net.wurst0.hacks;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;

public final class BedrockStashHack extends Hack
        implements UpdateListener, RenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting radius =
                new SliderSetting("Radius (chunks)", 1, 1, 4, 1);
        private final SliderSetting verticalRange =
                new SliderSetting("Vertical range", 32, 8, 64, 1);
        private final SliderSetting renderLimit =
                new SliderSetting("Render limit", 300, 50, 2000, 10);

        private final ArrayList<BlockPos> pockets = new ArrayList<BlockPos>();

        // incremental scan cursor
        private int minX, minY, minZ, maxX, maxY, maxZ;
        private int curX, curY, curZ;
        private boolean scanning;
        private double scanCenterX, scanCenterZ;
        private int ticksSinceScanStart;

        public BedrockStashHack()
        {
                super("BedrockStash",
                        "Scans the bedrock around you for hidden air pockets"
                                + " (stash bases built inside bedrock) and"
                                + " highlights them.");
                setCategory(Category.OTHER);
                addSettings(radius, verticalRange, renderLimit);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                WorldClient world = mc.world;
                if(p == null || world == null)
                {
                        pockets.clear();
                        scanning = false;
                        return;
                }

                if(scanning)
                {
                        scanSome(world);
                        return;
                }

                double dx = p.posX - scanCenterX;
                double dz = p.posZ - scanCenterZ;
                if(pockets.isEmpty() || dx * dx + dz * dz > 64.0)
                        startScan(p);
        }

        private void startScan(EntityPlayerSP p)
        {
                int h = radius.getValueI() * 16;
                int v = verticalRange.getValueI();
                int px = (int)Math.floor(p.posX);
                int py = (int)Math.floor(p.posY);
                int pz = (int)Math.floor(p.posZ);

                minX = Math.max(0, px - h);
                maxX = Math.min(255, px + h);
                minY = Math.max(0, py - v);
                maxY = Math.min(255, py + v);
                minZ = Math.max(0, pz - h);
                maxZ = Math.min(255, pz + h);

                curX = minX;
                curY = minY;
                curZ = minZ;
                scanning = true;
                ticksSinceScanStart = 0;
                scanCenterX = p.posX;
                scanCenterZ = p.posZ;
                pockets.clear();
        }

        private void scanSome(WorldClient world)
        {
                ticksSinceScanStart++;
                int budget = 8192;
                int cap = renderLimit.getValueI() + 100;

                while(budget-- > 0)
                {
                        BlockPos pos = new BlockPos(curX, curY, curZ);
                        if(isPocket(world, pos) && pockets.size() < cap)
                                pockets.add(pos);

                        // advance cursor
                        if(++curZ > maxZ)
                        {
                                curZ = minZ;
                                if(++curY > maxY)
                                {
                                        curY = minY;
                                        if(++curX > maxX)
                                        {
                                                scanning = false;
                                                return;
                                        }
                                }
                        }
                }

                // safety valve: don't scan forever on huge ranges
                if(ticksSinceScanStart > 600)
                        scanning = false;
        }

        private boolean isPocket(WorldClient world, BlockPos pos)
        {
                if(!world.isAirBlock(pos))
                        return false;

                int bedrock = 0;
                if(isBedrock(world, pos.up()))
                        bedrock++;
                if(isBedrock(world, pos.down()))
                        bedrock++;
                if(isBedrock(world, pos.north()))
                        bedrock++;
                if(isBedrock(world, pos.south()))
                        bedrock++;
                if(isBedrock(world, pos.west()))
                        bedrock++;
                if(isBedrock(world, pos.east()))
                        bedrock++;
                return bedrock >= 3;
        }

        private boolean isBedrock(WorldClient world, BlockPos pos)
        {
                return world.getBlockState(pos).getBlock()
                        == net.minecraft.init.Blocks.BEDROCK;
        }

        @Override
        public void onRender(float partialTicks)
        {
                if(mc.player == null || mc.world == null || pockets.isEmpty())
                        return;

                int limit = renderLimit.getValueI();
                int count = Math.min(limit, pockets.size());

                RenderUtils.enable3D(1.0F, false);
                RenderUtils.applyCameraTransform(partialTicks);
                for(int i = 0; i < count; i++)
                {
                        BlockPos pos = pockets.get(i);
                        RenderUtils.drawBox(new AxisAlignedBB(pos), 0.4F, 0.9F,
                                1.0F, 0.7F);
                }
                RenderUtils.disable3D(false);
        }

        @Override
        protected void onEnable()
        {
                pockets.clear();
                scanning = false;
        }

        @Override
        protected void onDisable()
        {
                pockets.clear();
                scanning = false;
        }
}
