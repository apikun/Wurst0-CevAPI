/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SkinDerpHack (GPL-3.0-or-later)
 * Direct port: 1.12.2 has the same mechanism (client model-part
 * flags in GameSettings, synced to the server as player settings),
 * so each toggle flips your skin's overlay layers just like
 * MC.options.setModelPart() does upstream.
 */
package net.wurst0.hacks;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

/**
 * Randomly toggles your skin's overlay layers (jacket, sleeves,
 * pants, hat, cape) for a glitchy flicker. Other players can see
 * it too, because the flags are synced as player settings.
 */
public final class SkinDerpHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();
        private final Random random = new Random();

        public SkinDerpHack()
        {
                super("SkinDerp",
                        "Randomly toggles your skin's overlay layers for a glitchy"
                                + " look. Other players can see this!");
                setCategory(Category.FUN);
        }

        @Override
        protected void onDisable()
        {
                // restore all layers to the default (enabled) state
                if(mc.player == null || mc.world == null)
                        return;

                for(EnumPlayerModelParts part : EnumPlayerModelParts.values())
                        mc.gameSettings.setModelPartEnabled(part, true);
        }

        @Override
        public void onUpdate()
        {
                if(mc.player == null || mc.world == null)
                        return;

                if(random.nextInt(4) != 0)
                        return;

                for(EnumPlayerModelParts part : EnumPlayerModelParts.values())
                        mc.gameSettings.switchModelPartEnabled(part);
        }
}
