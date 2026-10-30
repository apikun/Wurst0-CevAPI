/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FastLadderHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class FastLadderHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting speed =
                new SliderSetting("Speed", 0.5, 0.1, 2, 0.05);

        public FastLadderHack()
        {
                super("FastLadder", "Lets you climb up ladders and vines faster.");
                setCategory(Category.MOVEMENT);
                addSetting(speed);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;
                if(!p.isOnLadder())
                        return;

                if(p.moveForward > 0 || mc.gameSettings.keyBindJump.isKeyDown())
                        p.motionY = speed.getValue();
        }
}
