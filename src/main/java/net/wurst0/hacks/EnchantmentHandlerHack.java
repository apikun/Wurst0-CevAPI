/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI EnchantmentHandlerHack (GPL-3.0-or-later)
 * Lite adaptation: the 26.x data-component container panel becomes a
 * HUD summary of the enchantments on your gear, read from the 1.12.2
 * NBT enchant ids.
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.ModeSetting;

/**
 * Lists the enchantments (from item NBT) on your held item and armor
 * pieces in a compact HUD panel.
 */
public final class EnchantmentHandlerHack extends Hack
        implements UpdateListener, ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final ModeSetting scope = new ModeSetting("Scope",
                new String[]{"Held item", "Armor", "Held + Armor"}, 2);
        private final CheckboxSetting showOnHud =
                new CheckboxSetting("Show on HUD", true);

        private final List<String> lines = new ArrayList<String>();
        private int rebuildTimer;

        public EnchantmentHandlerHack()
        {
                super("EnchantmentHandler",
                        "Shows all enchantments on your held item and"
                                + " armor in a HUD panel.");
                setCategory(Category.ITEMS);
                addSettings(scope, showOnHud);
        }

        @Override
        public void onUpdate()
        {
                if(--rebuildTimer > 0)
                        return;
                rebuildTimer = 10;
                rebuild();
        }

        private void rebuild()
        {
                lines.clear();
                EntityPlayerSP p = mc.player;
                if(p == null)
                        return;

                String s = scope.getSelected();
                boolean held = s.equals("Held item")
                        || s.equals("Held + Armor");
                boolean armor = s.equals("Armor")
                        || s.equals("Held + Armor");

                if(held)
                        addEntries(p, "Held",
                                p.getHeldItemMainhand());

                if(armor)
                {
                        for(int i = 0; i < 4; i++)
                        {
                                ItemStack stack =
                                        p.inventory.armorInventory.get(i);
                                if(stack == null || stack.isEmpty())
                                        continue;
                                addEntries(p, armorSlotName(i), stack);
                        }
                }
        }

        private String armorSlotName(int slot)
        {
                // armorInventory: 0=boots, 1=legs, 2=chest, 3=helmet
                switch(slot)
                {
                        case 3:
                                return "Head";
                        case 2:
                                return "Chest";
                        case 1:
                                return "Legs";
                        default:
                                return "Feet";
                }
        }

        private void addEntries(EntityPlayer p, String label,
                ItemStack stack)
        {
                if(stack == null || stack.isEmpty())
                        return;

                Map<Enchantment, Integer> enchants =
                        EnchantmentHelper.getEnchantments(stack);
                if(enchants.isEmpty())
                        return;

                lines.add("\u00a7b" + label + ": "
                        + stack.getDisplayName());
                for(Map.Entry<Enchantment, Integer> entry :
                        enchants.entrySet())
                        lines.add("  \u00a7f" + entry.getKey()
                                .getTranslatedName(entry.getValue()));
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(!showOnHud.isChecked() || lines.isEmpty()
                        || mc.player == null)
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                int width = 0;
                for(String line : lines)
                        width = Math.max(width, mc.fontRenderer
                                .getStringWidth(line));
                if(width == 0)
                        return;

                int x = sr.getScaledWidth() - width - 3;
                int y = 3;
                for(String line : lines)
                {
                        mc.fontRenderer.drawStringWithShadow(line, x, y,
                                0xE0E0E0);
                        y += 10;
                        if(y > sr.getScaledHeight() - 10)
                                break;
                }
        }

        @Override
        protected void onEnable()
        {
                rebuildTimer = 0;
                lines.clear();
        }

        @Override
        protected void onDisable()
        {
                lines.clear();
        }
}
