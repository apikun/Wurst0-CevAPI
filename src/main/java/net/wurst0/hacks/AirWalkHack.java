/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AirWalkHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class AirWalkHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting jumps =
                new SliderSetting("Air jumps", 3, 1, 10, 1);
        private final CheckboxSetting resetOnGround =
                new CheckboxSetting("Reset on ground", true);

        private int used;
        private boolean wasJumping;

        public AirWalkHack()
        {
                super("AirWalk",
                        "Lets you jump in mid-air a limited number of times before"
                                + " touching the ground again.");
                setCategory(Category.MOVEMENT);
                addSettings(jumps, resetOnGround);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                boolean jumping = mc.gameSettings.keyBindJump.isKeyDown();

                if(p.onGround && !p.isInWater())
                {
                        if(resetOnGround.isChecked())
                                used = 0;
                        wasJumping = jumping;
                        return;
                }

                if(jumping && !wasJumping && used < jumps.getValueI()
                        && !p.isInWater() && !p.isOnLadder())
                {
                        p.motionY = 0.42D;
                        p.fallDistance = 0;
                        used++;
                }

                wasJumping = jumping;
        }
}
