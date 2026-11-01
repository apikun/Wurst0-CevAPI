/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI HideModMenuHack (GPL-3.0-or-later)
 *
 * The original hides mods inside the ModMenu screen (Fabric). On
 * 1.12.2 Forge the equivalent is the "Mods" button that Forge adds to
 * the main menu (GuiMainMenu.modButton, button id 6). While enabled,
 * this hack makes that button invisible each tick; Forge re-creates
 * it in initGui, so the hack simply keeps hiding it.
 */
package net.wurst0.hacks;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.util.ReflectUtils;

public final class HideModMenuHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private Field modButtonField;

        public HideModMenuHack()
        {
                super("HideModMenu",
                        "Hides the Mods button that Forge adds to the main"
                                + " menu.");
                setCategory(Category.TOOLS);
        }

        @Override
        public void onUpdate()
        {
                if(!(mc.currentScreen instanceof GuiMainMenu))
                        return;

                if(modButtonField == null)
                        modButtonField = ReflectUtils.field(GuiMainMenu.class,
                                "modButton", "modButton");
                if(modButtonField == null)
                        return;

                try
                {
                        Object button = modButtonField.get(null);
                        if(button instanceof GuiButton)
                                ((GuiButton)button).visible = false;
                }catch(Throwable t)
                {
                        // ignore - the button may not exist momentarily
                }
        }
}
