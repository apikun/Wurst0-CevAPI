/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TiredHack (GPL-3.0-or-later)
 * Lite adaptation: upstream sends a drooping head-rotation packet;
 * 1.12.2 forks have no packet-output event, so this draws a sleepy
 * dark vignette over the screen (strength slider) instead.
 */
package net.wurst0.hacks;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.wurst0.Category;
import net.wurst0.event.events.OverlayEvent;
import net.wurst0.event.listeners.OverlayListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Makes you look like you haven't slept in days: a soft dark
 * vignette closes in from the edges of the screen.
 */
public final class TiredHack extends Hack implements OverlayListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting strength =
                new SliderSetting("Strength", 0.6, 0.1, 1, 0.05);

        public TiredHack()
        {
                super("Tired",
                        "Draws dark circles around the edges of your screen for a"
                                + " sleepy look.");
                setCategory(Category.FUN);
                addSetting(strength);
        }

        @Override
        public void onOverlayRender(OverlayEvent event)
        {
                // only draw once per frame, on the ALL pass before the HUD
                if(!"ALL".equals(event.getType()))
                        return;

                if(mc.player == null)
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                int w = sr.getScaledWidth();
                int h = sr.getScaledHeight();

                int edge = (int)(strength.getValueF() * 190F);
                int dim = (int)(strength.getValueF() * 40F);

                GlStateManager.enableBlend();
                GlStateManager.disableTexture2D();
                GlStateManager.tryBlendFuncSeparate(
                        GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ZERO);

                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.getBuffer();
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

                // faint dim over the whole screen
                vertex(buffer, 0, 0, dim);
                vertex(buffer, w, 0, dim);
                vertex(buffer, w, h, dim);
                vertex(buffer, 0, h, dim);

                // soft gradient bands fading in from each edge
                int top = (int)(h * 0.3F);
                int left = (int)(w * 0.3F);

                vertex(buffer, 0, 0, edge);
                vertex(buffer, w, 0, edge);
                vertex(buffer, w, top, 0);
                vertex(buffer, 0, top, 0); // top band

                vertex(buffer, 0, h, edge);
                vertex(buffer, w, h, edge);
                vertex(buffer, w, h - top, 0);
                vertex(buffer, 0, h - top, 0); // bottom band

                vertex(buffer, 0, 0, edge);
                vertex(buffer, left, 0, 0);
                vertex(buffer, left, h, 0);
                vertex(buffer, 0, h, edge); // left band

                vertex(buffer, w, 0, edge);
                vertex(buffer, w - left, 0, 0);
                vertex(buffer, w - left, h, 0);
                vertex(buffer, w, h, edge); // right band

                tessellator.draw();

                GlStateManager.enableTexture2D();
                GlStateManager.disableBlend();
                GlStateManager.color(1F, 1F, 1F, 1F);
        }

        /** Pushes one position+color vertex (opaque near-black). */
        private void vertex(BufferBuilder buffer, int x, int y, int alpha)
        {
                buffer.pos(x, y, 0).color(4, 4, 10, alpha).endVertex();
        }
}
