/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ElytraInfoHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemElytra;
import net.wurst0.Category;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.feature.Hack;

/**
 * HUD overlay showing the durability of the worn elytra and, while
 * flying, the current speed. Drawn in the bottom-right corner.
 */
public final class ElytraInfoHack extends Hack implements ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        public ElytraInfoHack()
        {
                super("ElytraInfo",
                        "Shows the elytra's durability and your flight speed.");
                setCategory(Category.RENDER);
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(mc.player == null || mc.gameSettings.showDebugInfo)
                        return;

                ItemStack chest = mc.player.inventory.armorInventory.get(2);
                if(chest.isEmpty() || !(chest.getItem() instanceof ItemElytra))
                        return;

                FontRenderer fr = mc.fontRenderer;
                ScaledResolution sr = new ScaledResolution(mc);
                int y = sr.getScaledHeight() - fr.FONT_HEIGHT - 2;

                int max = chest.getMaxDamage();
                int left = max - chest.getItemDamage();
                int percent = left * 100 / max;

                int color;
                if(percent > 50)
                        color = 0x40FF40;
                else if(percent > 20)
                        color = 0xFFFF40;
                else
                        color = 0xFF4040;

                String dur = "Elytra: " + left + "/" + max + " (" + percent + "%)";
                fr.drawStringWithShadow(dur,
                        sr.getScaledWidth() - fr.getStringWidth(dur) - 2, y, color);

                if(mc.player.isElytraFlying())
                {
                        double hSpeed = Math.sqrt(mc.player.motionX * mc.player.motionX
                                + mc.player.motionZ * mc.player.motionZ) * 20D;
                        String speed = String.format("Speed: %.1f m/s  Y: %.1f",
                                hSpeed, mc.player.motionY * 20D);
                        y -= fr.FONT_HEIGHT + 1;
                        fr.drawStringWithShadow(speed,
                                sr.getScaledWidth() - fr.getStringWidth(speed) - 2, y,
                                0x40C0FF);
                }
        }
}
