/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NavigatorHack (GPL-3.0-or-later)
 * Lite adaptation: opens a simple searchable hack list screen
 * (see NavigatorScreen) instead of the full Navigator feature
 * database.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.wurst0.Category;
import net.wurst0.clickgui.NavigatorScreen;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * Opens a searchable list of all hacks: type to filter, click a
 * result to toggle it.
 */
public final class NavigatorHack extends Hack
{
        private final Minecraft mc = Minecraft.getMinecraft();

        public final CheckboxSetting backgroundOverlay =
                new CheckboxSetting("Background overlay", true);

        public NavigatorHack()
        {
                super("Navigator",
                        "Opens a searchable list of all hacks: type to"
                                + " filter, click a result to toggle it,"
                                + " Enter toggles the first result.");
                setCategory(Category.OTHER);
                addSetting(backgroundOverlay);
        }

        @Override
        protected void onEnable()
        {
                mc.displayGuiScreen(new NavigatorScreen(
                        backgroundOverlay.isChecked()));
                setEnabled(false);
        }
}
