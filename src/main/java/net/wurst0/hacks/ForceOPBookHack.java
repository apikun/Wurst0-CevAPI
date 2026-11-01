/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ForceOPBookHack (GPL-3.0-or-later)
 * Adapted to 1.12.2: the creative item data component API does not
 * exist, so the book is built as 1.12.2 written-book NBT (page JSON
 * with a clickEvent run_command) and swapped into the hotbar with a
 * CPacketCreativeInventoryAction. Note: most 1.13+ servers patched
 * command clicks from untrusted books; this still works on 1.12.2
 * vanilla and old Paper/Spigot builds.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.play.client.CPacketCreativeInventoryAction;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.TextFieldSetting;

public final class ForceOPBookHack extends Hack
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting anyCommand =
                new CheckboxSetting("Any command", false);
        private final TextFieldSetting command =
                new TextFieldSetting("Command", "kill @e", 256);
        private final TextFieldSetting title =
                new TextFieldSetting("Book title", "Book and Quill");
        private final TextFieldSetting author =
                new TextFieldSetting("Book author", "Book and Quill");
        private final TextFieldSetting text =
                new TextFieldSetting("Page text", "Click me", 1024);

        public ForceOPBookHack()
        {
                super("ForceOPBook",
                        "Puts a written book in your hotbar whose page runs"
                                + " /op <you> (or any configured command) when"
                                + " clicked. Needs creative mode.");
                setCategory(Category.CREATIVE_OP);
                addSettings(anyCommand, command, title, author, text);
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
                                "\u00a7c[ForceOPBook]\u00a7r Creative mode"
                                        + " required."));
                        setEnabled(false);
                        return;
                }

                String run = anyCommand.isChecked()
                        ? command.getValue().trim()
                        : "op " + p.getGameProfile().getName();
                if(run.isEmpty())
                        run = "op " + p.getGameProfile().getName();
                // 1.12.2 chat packets only run commands with a leading "/".
                if(!run.startsWith("/"))
                        run = "/" + run;

                NBTTagCompound tag = new NBTTagCompound();
                tag.setString("title", title.getValue());
                tag.setString("author", author.getValue());
                NBTTagList pages = new NBTTagList();
                String page = "{\"text\":\"" + jsonEscape(text.getValue())
                        + "\",\"clickEvent\":{\"action\":\"run_command\""
                        + ",\"value\":\"" + jsonEscape(run) + "\"}}";
                pages.appendTag(new NBTTagString(page));
                tag.setTag("pages", pages);

                ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
                stack.setTagCompound(tag);

                int slot = 36 + p.inventory.currentItem;
                p.connection.sendPacket(
                        new CPacketCreativeInventoryAction(slot, stack));
                p.sendMessage(new TextComponentString(
                        "\u00a7a[ForceOPBook]\u00a7r Book created in your"
                                + " selected hotbar slot."));
                setEnabled(false);
        }

        private static String jsonEscape(String s)
        {
                if(s == null)
                        return "";
                return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
}
