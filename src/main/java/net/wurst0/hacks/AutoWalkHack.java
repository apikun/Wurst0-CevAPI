/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoWalkHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

public final class AutoWalkHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting pauseInGUIs =
                new CheckboxSetting("Pause in GUIs", true);

        public AutoWalkHack()
        {
                super("AutoWalk",
                        "Walks forward automatically, as if the W key was held"
                                + " down.");
                setCategory(Category.MOVEMENT);
                addSetting(pauseInGUIs);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;
                if(p.movementInput == null)
                        return;
                if(pauseInGUIs.isChecked() && mc.currentScreen != null)
                        return;
                if(p.isRiding())
                        return;

                p.movementInput.moveForward = 1;
        }
}
