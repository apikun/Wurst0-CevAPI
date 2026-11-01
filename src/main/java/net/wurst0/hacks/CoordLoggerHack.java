/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI CoordLoggerHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.Loader;
import net.wurst0.Category;
import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.ChatInputListener;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * Scans chat messages for coordinate patterns (like "/home 123 64
 * -456") and logs them to config/wurst0/coords.txt with a
 * timestamp.
 */
public final class CoordLoggerHack extends Hack
        implements ChatInputListener, ScreenRenderListener
{
        private static final Pattern COORDS = Pattern.compile(
                "(-?\\d{1,6})[ ,;:]+(-?\\d{1,3})[ ,;:]+(-?\\d{1,6})");

        private final Minecraft mc = Minecraft.getMinecraft();
        private final CheckboxSetting showCount =
                new CheckboxSetting("Show count on HUD", true);

        private int logged;
        private File logFile;
        private final SimpleDateFormat fmt =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public CoordLoggerHack()
        {
                super("CoordLogger",
                        "Logs coordinates found in chat messages to a file.");
                setCategory(Category.INTEL);
                addSetting(showCount);
        }

        @Override
        public void onReceivedMessage(ChatInputEvent event)
        {
                if(mc.player == null)
                        return;

                String text = event.getText();
                if(text == null)
                        return;

                Matcher m = COORDS.matcher(text);
                if(!m.find())
                        return;

                String line = fmt.format(new Date()) + " | "
                        + text.replaceAll("\u00a7.", "") + "\r\n";

                try
                {
                        if(logFile == null)
                        {
                                File dir = new File(
                                        Loader.instance().getConfigDir(),
                                        "wurst0");
                                if(!dir.exists())
                                        dir.mkdirs();
                                logFile = new File(dir, "coords.txt");
                        }
                        Writer w = new OutputStreamWriter(
                                new FileOutputStream(logFile, true),
                                StandardCharsets.UTF_8);
                        w.write(line);
                        w.close();
                        logged++;
                }catch(Exception e)
                {
                        // disk full etc. - silently keep going
                }

                mc.player.sendMessage(new TextComponentString(
                        "\u00a7a[CoordLogger] Logged coordinates (total: "
                                + logged + ")"));
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(!showCount.isChecked() || mc.player == null || logged == 0)
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                mc.fontRenderer.drawStringWithShadow(
                        "Coords logged: " + logged, 2,
                        sr.getScaledHeight() - 42, 0x50C050);
        }

        @Override
        protected void onEnable()
        {
                // keep counting across sessions within the same game run
        }
}
