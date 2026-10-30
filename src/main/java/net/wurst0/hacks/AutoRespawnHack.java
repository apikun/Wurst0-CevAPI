/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoRespawnHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

public final class AutoRespawnHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        public AutoRespawnHack()
        {
                super("AutoRespawn",
                        "Automatically respawns you as soon as you die, without"
                                + " showing the death screen.");
                setCategory(Category.COMBAT);
        }

        @Override
        public void onUpdate()
        {
                if(mc.currentScreen instanceof GuiGameOver && mc.player != null)
                        mc.player.respawnPlayer();
        }
}
