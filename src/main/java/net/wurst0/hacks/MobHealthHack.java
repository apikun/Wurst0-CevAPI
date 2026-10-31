/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI MobHealthHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.math.RayTraceResult;
import net.wurst0.Category;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * Shows the health of the entity under your crosshair right below
 * the middle of the screen.
 */
public final class MobHealthHack extends Hack implements ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting hostileOnly =
                new CheckboxSetting("Hostile mobs only", false);

        public MobHealthHack()
        {
                super("MobHealth",
                        "Shows the health of the entity you are looking at.");
                setCategory(Category.RENDER);
                addSetting(hostileOnly);
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(mc.player == null || mc.world == null
                        || mc.objectMouseOver == null)
                        return;

                if(mc.objectMouseOver.typeOfHit != RayTraceResult.Type.ENTITY)
                        return;

                Entity e = mc.objectMouseOver.entityHit;
                if(!(e instanceof EntityLivingBase) || e instanceof EntityArmorStand)
                        return;

                EntityLivingBase living = (EntityLivingBase)e;
                if(hostileOnly.isChecked()
                        && !(living instanceof net.minecraft.entity.monster.IMob))
                        return;

                FontRenderer fr = mc.fontRenderer;
                ScaledResolution sr = new ScaledResolution(mc);

                String text = living.getName() + ": "
                        + (int)Math.ceil(living.getHealth()) + " / "
                        + (int)Math.ceil(living.getMaxHealth());

                int w = fr.getStringWidth(text);
                fr.drawStringWithShadow(text, sr.getScaledWidth() / 2 - w / 2,
                        sr.getScaledHeight() / 2 + 12, 0xFF60FF60);
        }
}
