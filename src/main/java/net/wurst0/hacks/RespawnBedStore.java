/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI RespawnBedStore (GPL-3.0-or-later)
 * Lite adaptation: remembers bed/spawn-point block positions you
 * click on and shows the last one on the HUD.
 */
package net.wurst0.hacks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import net.minecraft.block.BlockBed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.common.Loader;
import net.wurst0.Category;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

public final class RespawnBedStore extends Hack
        implements UpdateListener, ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private BlockPos lastBed;
        private int savedCount;
        private int lookTimer;

        public RespawnBedStore()
        {
                super("RespawnBedStore",
                        "Remembers the beds you interact with and shows the"
                                + " last one on the HUD (info only).");
                setCategory(Category.INTEL);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                // when a right click happens while looking at a bed
                if(mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit
                        != RayTraceResult.Type.BLOCK)
                        return;

                BlockPos pos = mc.objectMouseOver.getBlockPos();
                if(!(mc.world.getBlockState(pos).getBlock()
                        instanceof BlockBed))
                        return;

                if(p.isSwingInProgress && lookTimer == 0
                        && p.getDistanceSq(pos) < 36)
                {
                        lastBed = pos;
                        savedCount++;
                        lookTimer = 20;
                        save(p, pos);
                }

                if(lookTimer > 0)
                        lookTimer--;
        }

        private void save(EntityPlayerSP p, BlockPos pos)
        {
                try
                {
                        File dir = new File(
                                Loader.instance().getConfigDir(), "wurst0");
                        if(!dir.exists())
                                dir.mkdirs();
                        Writer w = new OutputStreamWriter(
                                new FileOutputStream(new File(dir,
                                        "beds.txt"), true),
                                StandardCharsets.UTF_8);
                        w.write("bed " + pos.getX() + " " + pos.getY() + " "
                                + pos.getZ() + " (held item: "
                                + p.getHeldItemMainhand()
                                        .getDisplayName() + ")\r\n");
                        w.close();
                }catch(Exception e)
                {
                        // keep going silently
                }
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(lastBed == null || mc.player == null)
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                mc.fontRenderer.drawStringWithShadow(
                        "Bed: " + lastBed.getX() + " " + lastBed.getY() + " "
                                + lastBed.getZ() + " (" + savedCount
                                + " saved)", 2,
                        sr.getScaledHeight() - 62, 0xE07070);
        }

        @Override
        protected void onEnable()
        {
                lookTimer = 0;
        }
}
