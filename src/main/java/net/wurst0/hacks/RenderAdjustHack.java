/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI RenderAdjustHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Lets you push render distance and FOV beyond the vanilla slider
 * limits (vanilla caps them at 16 chunks / 110 FOV in the options
 * screen, but the fields accept much larger values).
 */
public final class RenderAdjustHack extends Hack
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting renderDistance =
                new SliderSetting("Render distance", 16, 1, 64, 1);
        private final SliderSetting fov =
                new SliderSetting("FOV", 110, 30, 180, 5);
        private final CheckboxSetting restoreOnDisable =
                new CheckboxSetting("Restore on disable", true);

        private int savedDistance = -1;
        private float savedFov = -1;

        public RenderAdjustHack()
        {
                super("RenderAdjust",
                        "Removes the vanilla limits on render distance and FOV."
                                + " High distances use a lot of RAM!");
                setCategory(Category.RENDER);
                addSettings(renderDistance, fov, restoreOnDisable);
        }

        @Override
        protected void onEnable()
        {
                if(savedDistance < 0)
                        savedDistance = mc.gameSettings.renderDistanceChunks;
                if(savedFov < 0)
                        savedFov = mc.gameSettings.fovSetting;

                mc.gameSettings.renderDistanceChunks =
                        renderDistance.getValueI();
                mc.gameSettings.fovSetting = (float)fov.getValue();
                mc.gameSettings.saveOptions();
        }

        @Override
        protected void onDisable()
        {
                if(!restoreOnDisable.isChecked() || savedDistance < 0)
                        return;

                mc.gameSettings.renderDistanceChunks = savedDistance;
                mc.gameSettings.fovSetting = savedFov;
                mc.gameSettings.saveOptions();
                savedDistance = -1;
                savedFov = -1;
        }
}
