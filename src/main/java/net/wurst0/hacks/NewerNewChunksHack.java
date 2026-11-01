/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NewerNewChunksHack (GPL-3.0-or-later)
 * Lite adaptation: green borders = chunks that were already loaded
 * when the hack was enabled, red borders = chunks that streamed in
 * afterwards (i.e. generated/visited after you joined).
 */
package net.wurst0.hacks;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.AxisAlignedBB;
import net.wurst0.Category;
import net.wurst0.event.events.ChunkLoadEvent;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.listeners.ChunkLoadListener;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;

public final class NewerNewChunksHack extends Hack
        implements UpdateListener, RenderListener, ChunkLoadListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final Set<Long> oldChunks = new HashSet<Long>();
        private final Set<Long> newChunks = new HashSet<Long>();

        private int graceTicks;
        private net.minecraft.world.World lastWorld;

        public NewerNewChunksHack()
        {
                super("NewerNewChunks",
                        "Marks chunks loaded after you enabled it in red and"
                                + " pre-loaded chunks in green.");
                setCategory(Category.RENDER);
        }

        private static long key(int x, int z)
        {
                return ((long)x << 32) ^ (z & 0xFFFFFFFFL);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null)
                        return;

                if(mc.world != lastWorld)
                {
                        lastWorld = mc.world;
                        oldChunks.clear();
                        newChunks.clear();
                        graceTicks = 100;
                }

                if(graceTicks > 0)
                        graceTicks--;
        }

        @Override
        public void onChunkLoad(int chunkX, int chunkZ)
        {
                if(oldChunks.size() + newChunks.size() > 8000)
                        return;

                if(graceTicks > 0)
                        oldChunks.add(key(chunkX, chunkZ));
                else
                        newChunks.add(key(chunkX, chunkZ));
        }

        @Override
        public void onRender(float partialTicks)
        {
                if(mc.player == null
                        || (oldChunks.isEmpty() && newChunks.isEmpty()))
                        return;

                RenderUtils.enable3D(1.5F, false);
                RenderUtils.applyCameraTransform(partialTicks);

                for(long k : oldChunks)
                {
                        int x = (int)(k >> 32);
                        int z = (int)(k & 0xFFFFFFFFL);
                        RenderUtils.drawBox(new AxisAlignedBB(
                                x * 16, 40, z * 16, x * 16 + 16, 52,
                                z * 16 + 16), 0.3F, 0.9F, 0.3F, 0.5F);
                }
                for(long k : newChunks)
                {
                        int x = (int)(k >> 32);
                        int z = (int)(k & 0xFFFFFFFFL);
                        RenderUtils.drawBox(new AxisAlignedBB(
                                x * 16, 40, z * 16, x * 16 + 16, 52,
                                z * 16 + 16), 1F, 0.2F, 0.2F, 0.6F);
                }

                RenderUtils.disable3D(false);
        }

        @Override
        protected void onEnable()
        {
                graceTicks = 100;
                oldChunks.clear();
                newChunks.clear();
                lastWorld = mc.world;
        }

        @Override
        protected void onDisable()
        {
                oldChunks.clear();
                newChunks.clear();
        }
}
