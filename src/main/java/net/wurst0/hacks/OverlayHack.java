/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI OverlayHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.lang.reflect.Field;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.wurst0.Category;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.util.ReflectUtils;

/**
 * Highlights the block you are currently breaking and shows the
 * dig progress. Lite port: Wurst7 reads the dig progress from a
 * private PlayerControllerMP field via mixin; here the progress is
 * mirrored through reflection with a hardness-based fallback.
 */
public final class OverlayHack extends Hack
        implements UpdateListener, RenderListener, ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting showProgress =
                new CheckboxSetting("Show progress", true);
        private final CheckboxSetting whiteBox =
                new CheckboxSetting("White box", true);

        private BlockPos digPos;
        private float progress;
        private long digStart;
        private IBlockState digState;

        public OverlayHack()
        {
                super("Overlay",
                        "Highlights the block you are currently mining and"
                                + " shows how far along it is.");
                setCategory(Category.RENDER);
                addSettings(showProgress, whiteBox);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(!mc.gameSettings.keyBindAttack.isKeyDown()
                        || mc.objectMouseOver == null
                        || mc.objectMouseOver.typeOfHit
                                != RayTraceResult.Type.BLOCK)
                {
                        digPos = null;
                        progress = 0;
                        return;
                }

                BlockPos pos = mc.objectMouseOver.getBlockPos();
                if(!pos.equals(digPos))
                {
                        digPos = pos;
                        digState = mc.world.getBlockState(pos);
                        digStart = System.currentTimeMillis();
                }

                Float reflected = reflectProgress();
                if(reflected != null)
                        progress = reflected;
                else
                {
                        // fallback: estimate from time spent + hardness
                        float hardness = digState == null ? 1.5F
                                : digState.getBlockHardness(mc.world, pos);
                        long ms = System.currentTimeMillis() - digStart;
                        progress = hardness <= 0 ? 1F
                                : Math.min(1F, ms / (hardness * 1000F));
                }
        }

        private Float reflectProgress()
        {
                Field f = ReflectUtils.field(
                        net.minecraft.client.multiplayer.PlayerControllerMP.class,
                        "curBlockDamageMP", "field_78770_f");
                if(f == null)
                        return null;
                try
                {
                        return (Float)f.get(mc.playerController);
                }catch(Throwable t)
                {
                        return null;
                }
        }

        @Override
        public void onRender(float partialTicks)
        {
                if(digPos == null || !whiteBox.isChecked())
                        return;
                if(mc.player == null || mc.world == null)
                        return;

                RenderUtils.enable3D(1.5F, false);
                RenderUtils.applyCameraTransform(partialTicks);
                RenderUtils.drawBox(new AxisAlignedBB(digPos),
                        1F, 1F, 1F, 0.6F + 0.3F * progress);
                RenderUtils.disable3D(false);
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(digPos == null || !showProgress.isChecked() || progress <= 0)
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                String text = (int)(progress * 100) + "%";
                int x = sr.getScaledWidth() / 2 - 10;
                int y = sr.getScaledHeight() / 2 + 20;

                mc.fontRenderer.drawStringWithShadow(text, x, y, 0xFFFF00);
                int barW = 40;
                mc.fontRenderer.drawStringWithShadow("|",
                        x + 16, y, 0xFFFF00);
                net.minecraft.client.gui.Gui.drawRect(x, y + 10,
                        x + barW, y + 12, 0x80000000);
                net.minecraft.client.gui.Gui.drawRect(x, y + 10,
                        x + (int)(barW * progress), y + 12, 0xFFFFFF00);
        }

        @Override
        protected void onDisable()
        {
                digPos = null;
                progress = 0;
        }
}
