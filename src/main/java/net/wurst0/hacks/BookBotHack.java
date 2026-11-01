/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BookBotHack (GPL-3.0-or-later)
 * Lite adaptation: writes the pages from a text file into a
 * writable book using the vanilla MC|BEdit / MC|BSign payloads,
 * so no GUI automation is needed.
 */
package net.wurst0.hacks;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraftforge.fml.common.Loader;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.TextFieldSetting;

/**
 * Reads config/wurst0/bookbot.txt and writes it into the book you
 * are holding (50 pages x 256 chars max, vanilla limits).
 */
public final class BookBotHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting title =
                new TextFieldSetting("Title", "Book", 32);
        private final CheckboxSetting sign =
                new CheckboxSetting("Sign when done", false);

        private boolean done;

        public BookBotHack()
        {
                super("BookBot",
                        "Writes config/wurst0/bookbot.txt into the writable"
                                + " book you are holding.");
                setCategory(Category.ITEMS);
                addSettings(title, sign);
        }

        private List<String> readPages()
        {
                List<String> lines = new ArrayList<String>();
                try
                {
                        File dir = new File(
                                Loader.instance().getConfigDir(), "wurst0");
                        File file = new File(dir, "bookbot.txt");
                        if(!file.isFile())
                                return lines;

                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(
                                        new FileInputStream(file),
                                        StandardCharsets.UTF_8));
                        String line;
                        while((line = reader.readLine()) != null)
                                lines.add(line);
                        reader.close();
                }catch(Exception e)
                {
                        // fall through: empty book
                }
                return lines;
        }

        private static List<String> wrapPages(List<String> lines)
        {
                List<String> pages = new ArrayList<String>();
                StringBuilder page = new StringBuilder();
                for(String line : lines)
                {
                        if(line.length() > 256)
                                line = line.substring(0, 256);
                        if(page.length() + line.length() + 1 > 200)
                        {
                                pages.add(page.toString());
                                page = new StringBuilder();
                                if(pages.size() >= 50)
                                        return pages;
                        }
                        page.append(line).append('\n');
                }
                if(page.length() > 0 && pages.size() < 50)
                        pages.add(page.toString());
                return pages;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || done)
                        return;

                ItemStack held = p.getHeldItemMainhand();
                if(held.isEmpty() || held.getItem() != Items.WRITABLE_BOOK)
                        return;

                List<String> pages = wrapPages(readPages());
                if(pages.isEmpty())
                        return;

                NBTTagCompound tag =
                        held.hasTagCompound() ? held.getTagCompound()
                                : new NBTTagCompound();
                NBTTagList pagesTag = new NBTTagList();
                for(String page : pages)
                        pagesTag.appendTag(new NBTTagString(page));
                tag.setTag("pages", pagesTag);

                if(sign.isChecked())
                        tag.setString("title", title.getValue());
                if(!tag.hasKey("author"))
                        tag.setString("author", p.getName());

                ItemStack out = held.copy();
                out.setTagCompound(tag);

                String channel = sign.isChecked() ? "MC|BSign"
                        : "MC|BEdit";
                PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
                buf.writeItemStack(out);
                p.connection.sendPacket(
                        new CPacketCustomPayload(channel, buf));
                done = true;
        }

        @Override
        protected void onEnable()
        {
                done = false;
        }
}
