/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BoatFlyHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityBoat;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class BoatFlyHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting speed =
                new SliderSetting("Speed", 1, 0.1, 5, 0.1);

        public BoatFlyHack()
        {
                super("BoatFly",
                        "Allows you to fly with boats. Press the jump key to fly up"
                                + " and the sneak key to fly down.");
                setCategory(Category.MOVEMENT);
                addSetting(speed);
        }

        @Override
        public void onUpdate()
        {
                if(mc.player == null)
                        return;
                if(!(mc.player.getRidingEntity() instanceof EntityBoat))
                        return;

                EntityBoat boat = (EntityBoat)mc.player.getRidingEntity();
                boat.fallDistance = 0;

                if(mc.gameSettings.keyBindJump.isKeyDown())
                        boat.motionY = speed.getValue();
                else if(mc.gameSettings.keyBindSneak.isKeyDown())
                        boat.motionY = -speed.getValue();
                else
                        boat.motionY = 0;
        }
}
