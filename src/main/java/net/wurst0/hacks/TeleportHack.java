/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TeleportHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.MathHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Periodically teleports you a fixed distance. Note: strict servers
 * detect large steps as "moved too quickly" and kick/rubber-band;
 * keep the step small on protected servers.
 */
public final class TeleportHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final ModeSetting mode = new ModeSetting("Mode",
                new String[]{"Forward", "Backward", "Up", "Down"}, 0);
        private final SliderSetting step =
                new SliderSetting("Step", 4, 0.5, 32, 0.5);
        private final SliderSetting delay =
                new SliderSetting("Delay (ticks)", 10, 0, 40, 1);

        private int timer;

        public TeleportHack()
        {
                super("Teleport",
                        "Teleports you a fixed distance in the selected"
                                + " direction, in regular steps.");
                setCategory(Category.MOVEMENT);
                addSettings(mode, step, delay);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(delay.getValueI() > 0 && --timer > 0)
                        return;
                timer = delay.getValueI();

                double amount = step.getValue();
                String m = mode.getSelected();
                if(m.equals("Forward") || m.equals("Backward"))
                {
                        float yaw = p.rotationYaw;
                        float pitch = p.rotationPitch;
                        double dx = -MathHelper.sin(yaw * 0.017453292F)
                                * MathHelper.cos(pitch * 0.017453292F);
                        double dz = MathHelper.cos(yaw * 0.017453292F)
                                * MathHelper.cos(pitch * 0.017453292F);
                        double dy = -MathHelper.sin(pitch * 0.017453292F);
                        if(m.equals("Backward"))
                        {
                                dx = -dx;
                                dz = -dz;
                        }
                        p.setPositionAndUpdate(p.posX + dx * amount,
                                p.posY + dy * amount, p.posZ + dz * amount);
                }
                else if(m.equals("Up"))
                {
                        p.setPositionAndUpdate(p.posX, p.posY + amount,
                                p.posZ);
                }
                else
                {
                        p.setPositionAndUpdate(p.posX, p.posY - amount,
                                p.posZ);
                }
        }

        @Override
        protected void onEnable()
        {
                timer = 0;
        }
}
