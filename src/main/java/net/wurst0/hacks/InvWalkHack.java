/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI InvWalkHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiRepair;
import net.minecraft.client.settings.KeyBinding;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * Lets you keep walking while an inventory screen is open. While a
 * GuiContainer is up, vanilla stops reading the movement keys, so
 * this hack polls them directly and feeds the movement input - the
 * server just sees you walking.
 */
public final class InvWalkHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting jump =
                new CheckboxSetting("Jump", true);
        private final CheckboxSetting sneak =
                new CheckboxSetting("Sneak", false);

        public InvWalkHack()
        {
                super("InvWalk",
                        "Lets you walk around while looking at your inventory.");
                setCategory(Category.MOVEMENT);
                addSettings(jump, sneak);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || p.movementInput == null)
                        return;

                if(!(mc.currentScreen instanceof GuiContainer))
                        return;
                // don't walk while the user is renaming items in an anvil
                if(mc.currentScreen instanceof GuiRepair)
                        return;

                KeyBinding fwd = mc.gameSettings.keyBindForward;
                KeyBinding back = mc.gameSettings.keyBindBack;
                KeyBinding left = mc.gameSettings.keyBindLeft;
                KeyBinding right = mc.gameSettings.keyBindRight;

                p.movementInput.moveForward = (fwd.isKeyDown() ? 1 : 0)
                        - (back.isKeyDown() ? 1 : 0);
                p.movementInput.moveStrafe = (right.isKeyDown() ? 1 : 0)
                        - (left.isKeyDown() ? 1 : 0);

                if(jump.isChecked())
                        p.movementInput.jump =
                                mc.gameSettings.keyBindJump.isKeyDown();
                if(sneak.isChecked())
                        p.movementInput.sneak =
                                mc.gameSettings.keyBindSneak.isKeyDown();
        }
}
