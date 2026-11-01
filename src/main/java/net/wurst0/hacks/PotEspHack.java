/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI PotEspHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.wurst0.Category;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;

/**
 * Draws a box around every living entity that is currently under
 * the influence of a potion effect. Green for beneficial potions,
 * red for harmful ones, orange for neutral.
 */
public final class PotEspHack extends Hack
        implements UpdateListener, RenderListener
{
        private static final class Entry
        {
                final Entity entity;
                final float r;
                final float g;
                final float b;

                Entry(Entity entity, float r, float g, float b)
                {
                        this.entity = entity;
                        this.r = r;
                        this.g = g;
                        this.b = b;
                }
        }

        private final Minecraft mc = Minecraft.getMinecraft();
        private final List<Entry> entries = new ArrayList<Entry>();

        public PotEspHack()
        {
                super("PotEsp",
                        "Highlights entities that are under a potion effect"
                                + " (green = buff, red = debuff, orange = other).");
                setCategory(Category.RENDER);
        }

        @Override
        public void onUpdate()
        {
                entries.clear();
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                for(Entity e : mc.world.loadedEntityList)
                {
                        if(!(e instanceof EntityLivingBase))
                                continue;
                        if(e == p)
                                continue;
                        if(e.isDead)
                                continue;

                        Map<Potion, PotionEffect> map =
                                ((EntityLivingBase)e).getActivePotionMap();
                        if(map == null || map.isEmpty())
                                continue;

                        boolean bad = false;
                        boolean good = false;
                        for(PotionEffect effect : map.values())
                        {
                                if(effect.getPotion().isBadEffect())
                                        bad = true;
                                else
                                        good = true;
                        }

                        if(bad && !good)
                                entries.add(new Entry(e, 1F, 0.2F, 0.2F));
                        else if(good && !bad)
                                entries.add(new Entry(e, 0.2F, 1F, 0.3F));
                        else
                                entries.add(new Entry(e, 1F, 0.6F, 0.1F));
                }
        }

        @Override
        public void onRender(float partialTicks)
        {
                if(entries.isEmpty() || mc.player == null)
                        return;

                RenderUtils.enable3D(1.5F, false);
                RenderUtils.applyCameraTransform(partialTicks);
                for(Entry entry : entries)
                {
                        AxisAlignedBB box = entry.entity
                                .getEntityBoundingBox().grow(0.1);
                        RenderUtils.drawBox(box, entry.r, entry.g, entry.b, 0.5F);
                }
                RenderUtils.disable3D(false);
        }

        @Override
        protected void onDisable()
        {
                entries.clear();
        }
}
