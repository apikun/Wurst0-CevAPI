/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NBTEditorHack (GPL-3.0-or-later)
 * REDUCED SCOPE (GUI-less port): the original's editor screen, preset
 * system and /data-merge world/entity editing (1.13+ commands) are not
 * portable. This port applies a raw item-stack NBT string to your
 * selected hotbar slot in creative mode via CPacketCreativeInventoryAction.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.client.CPacketCreativeInventoryAction;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.feature.Hack;
import net.wurst0.settings.TextFieldSetting;

public final class NBTEditorHack extends Hack
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting nbt = new TextFieldSetting("NBT",
                "{id:\"minecraft:diamond_sword\",Count:1b,Damage:0,"
                        + "tag:{display:{Name:\"OP Sword\",Lore:[\"Wurst0\"]},"
                        + "ench:[{id:16s,lvl:127s}]}}",
                1024);

        public NBTEditorHack()
        {
                super("NBTEditor",
                        "Replaces your selected hotbar item in creative mode"
                                + " with the configured item-stack NBT"
                                + " (1.12.2 SNBT syntax).");
                setCategory(Category.CREATIVE_OP);
                addSettings(nbt);
        }

        @Override
        protected void onEnable()
        {
                EntityPlayerSP p = mc.player;
                if(p == null)
                {
                        setEnabled(false);
                        return;
                }
                if(!p.capabilities.isCreativeMode)
                {
                        p.sendMessage(new TextComponentString(
                                "\u00a7c[NBTEditor]\u00a7r Creative mode"
                                        + " required."));
                        setEnabled(false);
                        return;
                }

                String value = nbt.getValue().trim();
                if(value.isEmpty())
                {
                        p.sendMessage(new TextComponentString(
                                "\u00a7c[NBTEditor]\u00a7r NBT is empty."));
                        setEnabled(false);
                        return;
                }

                ItemStack stack;
                try
                {
                        NBTTagCompound tag =
                                JsonToNBT.getTagFromJson(value);
                        stack = new ItemStack(tag);
                }catch(NBTException e)
                {
                        p.sendMessage(new TextComponentString(
                                "\u00a7c[NBTEditor]\u00a7r NBT parse error: "
                                        + e.getMessage()));
                        setEnabled(false);
                        return;
                }catch(Exception e)
                {
                        p.sendMessage(new TextComponentString(
                                "\u00a7c[NBTEditor]\u00a7r Invalid item NBT: "
                                        + e.getMessage()));
                        setEnabled(false);
                        return;
                }

                if(stack.isEmpty())
                {
                        p.sendMessage(new TextComponentString(
                                "\u00a7c[NBTEditor]\u00a7r Resulting stack is"
                                        + " empty (missing Count:1b?)."));
                        setEnabled(false);
                        return;
                }

                int slot = 36 + p.inventory.currentItem;
                p.connection.sendPacket(
                        new CPacketCreativeInventoryAction(slot, stack));
                p.sendMessage(new TextComponentString(
                        "\u00a7a[NBTEditor]\u00a7r Item applied to slot "
                                + (slot + 1) + "."));
                setEnabled(false);
        }
}
