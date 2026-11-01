/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NoGoZoneHack (GPL-3.0-or-later)
 * Lite adaptation: marks your position when you enable it as the
 * zone center and warns you when you enter the radius.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class NoGoZoneHack extends Hack
        implements UpdateListener, ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting radius =
                new SliderSetting("Radius", 32, 4, 256, 4);
        private final CheckboxSetting chatWarn =
                new CheckboxSetting("Chat warnings", true);

        private double centerX;
        private double centerY;
        private double centerZ;
        private boolean inside;
        private int warnCooldown;

        public NoGoZoneHack()
        {
                super("NoGoZone",
                        "Warns you when you get too close to a place you"
                                + " marked. The center is set where you stand"
                                + " when you enable it.");
                setCategory(Category.INTEL);
                addSettings(radius, chatWarn);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null)
                        return;

                if(warnCooldown > 0)
                        warnCooldown--;

                double dx = p.posX - centerX;
                double dz = p.posZ - centerZ;
                boolean nowInside = dx * dx + dz * dz
                        < radius.getValue() * radius.getValue();

                if(nowInside && !inside && chatWarn.isChecked()
                        && warnCooldown == 0)
                {
                        p.sendMessage(new TextComponentString(
                                "\u00a7c[NoGoZone] You entered the no-go zone!"));
                        warnCooldown = 200;
                }
                inside = nowInside;
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(!inside || mc.player == null)
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                String text = "\u00a7c\u00a7lNO-GO ZONE";
                int w = mc.fontRenderer.getStringWidth(text);
                mc.fontRenderer.drawStringWithShadow(text,
                        sr.getScaledWidth() / 2 - w / 2, 10, 0xFF5050);
        }

        @Override
        protected void onEnable()
        {
                EntityPlayerSP p = mc.player;
                if(p != null)
                {
                        centerX = p.posX;
                        centerY = p.posY;
                        centerZ = p.posZ;
                }
                inside = false;
                warnCooldown = 0;
        }
}
