/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AltGuiHack (GPL-3.0-or-later)
 * Lite adaptation: the original ships a full Meteor-style GUI
 * toolkit with per-color settings. This version opens a minimal
 * themed list screen (see AltGuiScreen) instead of re-implementing
 * the whole toolkit.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.wurst0.Category;
import net.wurst0.clickgui.AltGuiScreen;
import net.wurst0.feature.Hack;

/**
 * Opens an alternate, minimal list-style GUI: every hack on one
 * screen, filterable by category, click a row to toggle.
 */
public final class AltGuiHack extends Hack
{
        private final Minecraft mc = Minecraft.getMinecraft();

        public AltGuiHack()
        {
                super("AltGUI",
                        "Opens a minimal Meteor-style list GUI: all"
                                + " hacks on one screen, category"
                                + " filter tabs, click a row to toggle"
                                + " it.");
                setCategory(Category.OTHER);
        }

        @Override
        protected void onEnable()
        {
                mc.displayGuiScreen(new AltGuiScreen());
                setEnabled(false);
        }
}
