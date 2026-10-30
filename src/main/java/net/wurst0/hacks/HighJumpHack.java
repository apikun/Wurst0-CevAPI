/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI HighJumpHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class HighJumpHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting height =
                new SliderSetting("Height", 1, 0.42, 5, 0.02);

        public HighJumpHack()
        {
                super("HighJump", "Makes you jump higher than normal.");
                setCategory(Category.MOVEMENT);
                addSetting(height);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;
                if(!p.onGround || !mc.gameSettings.keyBindJump.isKeyDown())
                        return;
                if(p.isInWater() || p.isOnLadder() || p.isRiding())
                        return;

                p.motionY = height.getValue();
        }
}
