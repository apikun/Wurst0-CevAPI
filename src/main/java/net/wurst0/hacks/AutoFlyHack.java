/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoFlyHack (GPL-3.0-or-later)
 *
 * The original is a huge chunk-grid exploration pather (26.x elytra/
 * abilities API). This 1.12.2 lite adaptation keeps the core intent:
 * creative flight automation - it keeps you flying, locks your
 * altitude to a slider and gives you vertical control with the
 * jump/sneak keys.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class AutoFlyHack extends Hack implements UpdateListener
{
        private static final float DEFAULT_FLY_SPEED = 0.05F;

        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting keepFlying =
                new CheckboxSetting("Keep flying", true);
        private final CheckboxSetting lockAltitude =
                new CheckboxSetting("Lock altitude", false);
        private final SliderSetting altitude =
                new SliderSetting("Altitude (Y)", 80, 0, 255, 1);
        private final SliderSetting flySpeed =
                new SliderSetting("Fly speed", 1, 0.5, 10, 0.5);

        public AutoFlyHack()
        {
                super("AutoFly",
                        "Creative flight automation: keeps you flying,"
                                + " locks your altitude and lets you adjust"
                                + " the target height with jump/sneak. Lite"
                                + " adaptation of the 26.x pathing original.");
                setCategory(Category.MOVEMENT);
                addSettings(keepFlying, lockAltitude, altitude, flySpeed);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(keepFlying.isChecked() && p.capabilities.allowFlying
                        && !p.capabilities.isFlying)
                {
                        p.capabilities.isFlying = true;
                        p.sendPlayerAbilities();
                }

                if(!p.capabilities.isFlying)
                        return;

                double targetY = altitude.getValue();

                // vertical control: jump/sneak adjusts the target altitude
                GameSettings gs = mc.gameSettings;
                if(gs.keyBindJump.isKeyDown())
                        targetY += 0.5;
                if(gs.keyBindSneak.isKeyDown())
                        targetY -= 0.5;
                if(targetY != altitude.getValue())
                        altitude.setValue(targetY);

                if(lockAltitude.isChecked())
                {
                        double diff = (targetY - p.posY) * 0.05;
                        if(diff > 0.5)
                                diff = 0.5;
                        if(diff < -0.5)
                                diff = -0.5;
                        p.motionY = diff;
                }
        }

        @Override
        protected void onEnable()
        {
                EntityPlayerSP p = mc.player;
                if(p != null)
                        p.capabilities.setFlySpeed(
                                DEFAULT_FLY_SPEED * flySpeed.getValueF());
        }

        @Override
        protected void onDisable()
        {
                EntityPlayerSP p = mc.player;
                if(p != null)
                        p.capabilities.setFlySpeed(DEFAULT_FLY_SPEED);
        }
}
