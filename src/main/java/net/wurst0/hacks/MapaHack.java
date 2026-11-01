/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI MapaHack (GPL-3.0-or-later)
 * Lite adaptation: draws the held filled map on the HUD.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.MapData;
import net.minecraft.init.Items;
import net.minecraft.block.material.MapColor;
import net.wurst0.Category;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class MapaHack extends Hack implements ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting size =
                new SliderSetting("Size", 96, 48, 192, 16);
        private final SliderSetting opacity =
                new SliderSetting("Opacity", 80, 20, 100, 5);

        public MapaHack()
        {
                super("Mapa",
                        "Draws the filled map you are holding onto the HUD.");
                setCategory(Category.RENDER);
                addSettings(size, opacity);
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                ItemStack stack = p.getHeldItemMainhand();
                if(stack.isEmpty() || stack.getItem() != Items.FILLED_MAP)
                        stack = p.getHeldItemOffhand();
                if(stack.isEmpty() || stack.getItem() != Items.FILLED_MAP)
                        return;

                MapData data = ((ItemMap)stack.getItem())
                        .getMapData(stack, mc.world);
                if(data == null)
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                int s = size.getValueI();
                int x = sr.getScaledWidth() - s - 4;
                int y = 4;

                GlStateManager.pushMatrix();
                GlStateManager.enableBlend();
                GlStateManager.disableTexture2D();
                GlStateManager.color(1, 1, 1, 1);
                GlStateManager.translate(x, y, 0);
                GlStateManager.scale((float)s / 128F, (float)s / 128F, 1);

                float alpha = opacity.getValueF() / 100F;
                Tessellator tess = Tessellator.getInstance();
                BufferBuilder buf = tess.getBuffer();
                buf.begin(7, DefaultVertexFormats.POSITION_COLOR);

                for(int py = 0; py < 128; py += 2)
                {
                        for(int px = 0; px < 128; px += 2)
                        {
                                int idx = data.colors[py * 128 + px] & 0xFF;
                                MapColor color = MapColor.COLORS[idx >> 2];
                                int rgb = color == null ? 0
                                        : color.colorValue;
                                float bright = (idx & 3) == 0 ? 0.71F
                                        : (idx & 3) == 1 ? 0.86F
                                        : (idx & 3) == 2 ? 1.0F
                                        : 0.53F;
                                float r = ((rgb >> 16) & 0xFF) / 255F
                                        * bright;
                                float g = ((rgb >> 8) & 0xFF) / 255F
                                        * bright;
                                float b = (rgb & 0xFF) / 255F * bright;
                                float a = rgb == 0 ? 0.15F : alpha;

                                buf.pos(px, py, 0).color(r, g, b, a)
                                        .endVertex();
                                buf.pos(px, py + 2, 0).color(r, g, b, a)
                                        .endVertex();
                                buf.pos(px + 2, py + 2, 0).color(r, g, b, a)
                                        .endVertex();
                                buf.pos(px + 2, py, 0).color(r, g, b, a)
                                        .endVertex();
                        }
                }
                tess.draw();

                GlStateManager.enableTexture2D();
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();

                // border
                net.minecraft.client.gui.Gui.drawRect(x - 1, y - 1,
                        x + s + 1, y, 0xFF333333);
                net.minecraft.client.gui.Gui.drawRect(x - 1, y + s,
                        x + s + 1, y + s + 1, 0xFF333333);
                net.minecraft.client.gui.Gui.drawRect(x - 1, y,
                        x, y + s, 0xFF333333);
                net.minecraft.client.gui.Gui.drawRect(x + s, y,
                        x + s + 1, y + s, 0xFF333333);
        }
}
