/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI UiUtilsHack (GPL-3.0-or-later)
 * Lite adaptation: the original's slot-number overlay (via a GUI
 * state singleton) is drawn here through ScreenRenderListener with
 * reflection for GuiContainer.guiLeft/guiTop; the original's GUI
 * restore key became a simple GUI debug HUD line (scaled size,
 * GUI scale, mouse position, current screen).
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.inventory.Slot;
import net.wurst0.Category;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.ReflectUtils;

import org.lwjgl.input.Mouse;

/**
 * Draws container slot numbers over each slot and a small GUI
 * debug HUD (scaled size, GUI scale, mouse position, current
 * screen) while enabled.
 */
public final class UiUtilsHack extends Hack implements ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting slotOverlay =
                new CheckboxSetting("Slot overlay", true);
        private final CheckboxSetting hoverOnly =
                new CheckboxSetting("Slot overlay on hover", false);
        private final SliderSetting offsetX =
                new SliderSetting("Slot overlay offset X", 2, -20, 20, 1);
        private final SliderSetting offsetY =
                new SliderSetting("Slot overlay offset Y", 2, -20, 20, 1);
        private final CheckboxSetting debugHud =
                new CheckboxSetting("GUI debug HUD", true);

        public UiUtilsHack()
        {
                super("UI-Utils",
                        "Draws container slot numbers over each slot"
                                + " and a small GUI debug HUD (scaled"
                                + " size, GUI scale, mouse position,"
                                + " current screen).");
                setCategory(Category.TOOLS);
                addSettings(slotOverlay, hoverOnly, offsetX, offsetY,
                        debugHud);
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                ScaledResolution sr = new ScaledResolution(mc);
                int mouseX = Mouse.getX() * sr.getScaledWidth()
                        / mc.displayWidth;
                int mouseY = sr.getScaledHeight()
                        - Mouse.getY() * sr.getScaledHeight()
                                / mc.displayHeight - 1;

                if(slotOverlay.isChecked() && mc.currentScreen
                        instanceof GuiContainer)
                {
                        GuiContainer container =
                                (GuiContainer)mc.currentScreen;
                        int guiLeft = reflectionInt(container,
                                "guiLeft", "field_147003_i");
                        int guiTop = reflectionInt(container,
                                "guiTop", "field_147009_r");
                        if(guiLeft != Integer.MIN_VALUE
                                && guiTop != Integer.MIN_VALUE)
                        {
                                for(Object o : container.inventorySlots
                                        .inventorySlots)
                                {
                                        Slot slot = (Slot)o;
                                        drawSlotNumber(slot, guiLeft,
                                                guiTop, mouseX, mouseY);
                                }
                        }
                }

                if(debugHud.isChecked())
                {
                        String screen = mc.currentScreen == null
                                ? "none"
                                : mc.currentScreen.getClass()
                                        .getSimpleName();
                        mc.fontRenderer.drawStringWithShadow(
                                "GUI " + sr.getScaledWidth() + "x"
                                        + sr.getScaledHeight()
                                        + " scale="
                                        + mc.gameSettings.guiScale
                                        + " mouse=" + mouseX + ","
                                        + mouseY + " screen=" + screen,
                                2, 2, 0xFFB0E0FF);
                }
        }

        private void drawSlotNumber(Slot slot, int guiLeft, int guiTop,
                int mouseX, int mouseY)
        {
                int x = guiLeft + slot.xPos
                        + offsetX.getValueI();
                int y = guiTop + slot.yPos
                        + offsetY.getValueI();

                if(hoverOnly.isChecked()
                        && (mouseX < x || mouseX > x + 16
                                || mouseY < y || mouseY > y + 16))
                        return;

                mc.fontRenderer.drawStringWithShadow(
                        Integer.toString(slot.slotNumber), x, y,
                        0xFFEFEFEF);
        }

        private static int reflectionInt(Object instance,
                String mcpName, String srgName)
        {
                try
                {
                        java.lang.reflect.Field field =
                                ReflectUtils.field(
                                        instance.getClass(), mcpName,
                                        srgName);
                        if(field != null)
                                return field.getInt(instance);
                }catch(Throwable t)
                {
                        // fall through
                }
                return Integer.MIN_VALUE;
        }
}
