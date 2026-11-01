/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI CustomTotemHack (GPL-3.0-or-later)
 * Lite adaptation: no runtime resource-pack reload on 1.12.2, so the
 * totem's baked atlas sprite ("minecraft:items/totem_of_undying") is
 * overwritten in place with the local player's skin face (read back
 * from GPU memory, mipmap chain rebuilt) and re-uploaded whenever the
 * atlas is re-stitched (resource reload). No kill-tracking / name
 * fetch, that needs the modern resource pipeline.
 */
package net.wurst0.hacks;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.util.ReflectUtils;

/**
 * Turns every totem of undying into a miniature portrait of your own
 * skin's face. Purely client-side; re-applies itself automatically
 * after resource reloads and restores the vanilla texture on disable.
 */
public final class CustomTotemHack extends Hack
        implements UpdateListener, IResourceManagerReloadListener
{
        private static final String TOTEM_SPRITE = "minecraft:items/totem_of_undying";
        private static final int CHECK_INTERVAL = 10;

        private final Minecraft mc = Minecraft.getMinecraft();

        private Field framesField; // TextureAtlasSprite.framesTextureData
        private int[][] originalData; // vanilla totem pixels for restore
        private int[][] swappedData; // face pixels currently applied
        private int checkTimer;
        private boolean listenerRegistered;

        public CustomTotemHack()
        {
                super("CustomTotem",
                        "Replaces the totem of undying texture with your own skin's"
                                + " face. Survives resource reloads and restores the"
                                + " vanilla texture when disabled.");
                setCategory(Category.FUN);
        }

        @Override
        protected void onEnable()
        {
                originalData = null;
                swappedData = null;
                checkTimer = 0;

                if(!listenerRegistered)
                {
                        ((IReloadableResourceManager)mc.getResourceManager())
                                .registerReloadListener(this);
                        listenerRegistered = true;
                }

                apply();
        }

        @Override
        protected void onDisable()
        {
                restore();
                originalData = null;
                swappedData = null;
        }

        @Override
        public void onUpdate()
        {
                if(mc.player == null || mc.world == null)
                        return;

                if(++checkTimer < CHECK_INTERVAL)
                        return;
                checkTimer = 0;

                TextureAtlasSprite sprite = findSprite();
                if(sprite == null || isApplied(sprite))
                        return;

                apply();
        }

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager)
        {
                if(!isEnabled())
                        return;

                // the atlas was re-stitched: the sprite object and its data are
                // fresh vanilla copies now
                originalData = null;
                swappedData = null;
                apply();
        }

        private TextureAtlasSprite findSprite()
        {
                TextureMap map = mc.getTextureMapBlocks();
                if(map == null)
                        return null;

                TextureAtlasSprite sprite = map.getAtlasSprite(TOTEM_SPRITE);
                if(sprite == null || sprite.getIconWidth() <= 0)
                        return null;

                return sprite;
        }

        private void apply()
        {
                if(!(mc.player instanceof AbstractClientPlayer))
                        return;

                TextureAtlasSprite sprite = findSprite();
                if(sprite == null)
                        return;

                List<int[][]> frames = getFrames(sprite);
                if(frames == null || frames.isEmpty() || frames.get(0) == null
                        || frames.get(0) == swappedData)
                        return;

                int[][] face = readSkinFace(sprite.getIconWidth(),
                        sprite.getIconHeight(), frames.get(0).length);
                if(face == null)
                        return;

                if(originalData == null)
                        originalData = frames.get(0);

                frames.set(0, face);
                upload(sprite, face);
                swappedData = face;
        }

        private void restore()
        {
                if(originalData == null)
                        return;

                TextureAtlasSprite sprite = findSprite();
                if(sprite == null)
                        return;

                List<int[][]> frames = getFrames(sprite);
                if(frames == null || frames.isEmpty())
                        return;

                frames.set(0, originalData);
                upload(sprite, originalData);
        }

        private boolean isApplied(TextureAtlasSprite sprite)
        {
                List<int[][]> frames = getFrames(sprite);
                return frames != null && !frames.isEmpty()
                        && frames.get(0) == swappedData;
        }

        @SuppressWarnings("unchecked")
        private List<int[][]> getFrames(TextureAtlasSprite sprite)
        {
                try
                {
                        if(framesField == null)
                                framesField = ReflectUtils.field(
                                        TextureAtlasSprite.class, "framesTextureData",
                                        "field_110976_a");

                        if(framesField == null)
                                return null;

                        return (List<int[][]>)framesField.get(sprite);
                }catch(Throwable t)
                {
                        return null;
                }
        }

        /** Binds the block atlas and uploads the given mipmap chain. */
        private void upload(TextureAtlasSprite sprite, int[][] data)
        {
                try
                {
                        GlStateManager.bindTexture(
                                mc.getTextureMapBlocks().getGlTextureId());
                        TextureUtil.uploadTextureMipmap(data, sprite.getIconWidth(),
                                sprite.getIconHeight(), sprite.getOriginX(),
                                sprite.getOriginY(), false, false);
                }catch(Throwable t)
                {
                        t.printStackTrace();
                }
        }

        /**
         * Reads the local player's skin from GPU memory and scales the
         * face region into a full mipmap chain matching the totem sprite.
         * Returns null while the skin texture is still downloading.
         */
        private int[][] readSkinFace(int spriteW, int spriteH,
                int mipmapLevels)
        {
                try
                {
                        ResourceLocation skin =
                                ((AbstractClientPlayer)mc.player).getLocationSkin();
                        if(skin == null)
                                return null;

                        mc.getTextureManager().bindTexture(skin);
                        int skinW = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,
                                0, GL11.GL_TEXTURE_WIDTH);
                        int skinH = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,
                                0, GL11.GL_TEXTURE_HEIGHT);
                        if(skinW <= 0 || skinH <= 0)
                                return null; // not uploaded yet, retry later

                        ByteBuffer pixels = ByteBuffer
                                .allocateDirect(skinW * skinH * 4)
                                .order(ByteOrder.nativeOrder());
                        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                                GL11.GL_UNSIGNED_BYTE, pixels);

                        // the face sits at (8,8)-(16,16) in a 64x64 skin layout
                        float scale = skinW / 64F;
                        int faceX = (int)(8 * scale);
                        int faceY = (int)(8 * scale);
                        int faceSize = Math.max(1, (int)(8 * scale));

                        int[] level0 = new int[spriteW * spriteH];
                        for(int y = 0; y < spriteH; y++)
                        {
                                for(int x = 0; x < spriteW; x++)
                                {
                                        int sx = Math.min(skinW - 1,
                                                faceX + x * faceSize / spriteW);
                                        int sy = Math.min(skinH - 1,
                                                faceY + y * faceSize / spriteH);
                                        int i = (sy * skinW + sx) * 4;
                                        int r = pixels.get(i) & 0xFF;
                                        int g = pixels.get(i + 1) & 0xFF;
                                        int b = pixels.get(i + 2) & 0xFF;
                                        int a = pixels.get(i + 3) & 0xFF;
                                        level0[y * spriteW + x] =
                                                (a << 24) | (r << 16) | (g << 8) | b;
                                }
                        }

                        int[][] data = new int[mipmapLevels][];
                        data[0] = level0;
                        for(int mip = 1; mip < mipmapLevels; mip++)
                        {
                                int w = spriteW >> mip;
                                int h = spriteH >> mip;
                                if(w <= 0 || h <= 0)
                                {
                                        // uploadTextureMipmap() stops before this level
                                        data[mip] = new int[0];
                                        continue;
                                }
                                data[mip] = downsample(data[mip - 1],
                                        spriteW >> (mip - 1), spriteH >> (mip - 1), w, h);
                        }

                        return data;
                }catch(Throwable t)
                {
                        return null;
                }
        }

        private int[] downsample(int[] src, int srcW, int srcH, int dstW,
                int dstH)
        {
                int[] dst = new int[dstW * dstH];
                for(int y = 0; y < dstH; y++)
                {
                        for(int x = 0; x < dstW; x++)
                        {
                                int r = 0;
                                int g = 0;
                                int b = 0;
                                int a = 0;
                                int n = 0;
                                for(int dy = 0; dy < 2; dy++)
                                {
                                        for(int dx = 0; dx < 2; dx++)
                                        {
                                                int sx = Math.min(srcW - 1, x * 2 + dx);
                                                int sy = Math.min(srcH - 1, y * 2 + dy);
                                                int c = src[sy * srcW + sx];
                                                r += c >> 16 & 0xFF;
                                                g += c >> 8 & 0xFF;
                                                b += c & 0xFF;
                                                a += c >> 24 & 0xFF;
                                                n++;
                                        }
                                }
                                dst[y * dstW + x] = a / n << 24 | r / n << 16
                                        | g / n << 8 | b / n;
                        }
                }
                return dst;
        }
}
