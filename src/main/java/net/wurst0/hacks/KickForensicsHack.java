/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI KickForensicsHack (GPL-3.0-or-later)
 *
 * The original records packets around a disconnect and writes a
 * forensics report. This 1.12.2 adaptation keeps the useful core:
 * it keeps a rolling chat history and, when the disconnect screen
 * appears, appends timestamp, server, kick reason/message and the
 * last N chat lines to config/wurst0/kicks.txt.
 */
package net.wurst0.hacks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.text.ITextComponent;
import net.wurst0.Category;
import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.ChatInputListener;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.ReflectUtils;

public final class KickForensicsHack extends Hack
        implements UpdateListener, ChatInputListener, ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting chatHistory =
                new SliderSetting("Chat history", 50, 10, 200, 10);
        private final CheckboxSetting showStatus =
                new CheckboxSetting("Show status", true);

        private final ArrayDeque<String> chatLines = new ArrayDeque<String>();
        private boolean hadPlayer;
        private String lastServer = "unknown";
        private int kicksLogged;
        private File logFile;

        private java.lang.reflect.Field reasonField;
        private java.lang.reflect.Field messageField;

        private final SimpleDateFormat fmt =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public KickForensicsHack()
        {
                super("KickForensics",
                        "Records kick reasons and the last chat lines to"
                                + " config/wurst0/kicks.txt whenever you"
                                + " get disconnected.");
                setCategory(Category.TOOLS);
                addSettings(chatHistory, showStatus);
        }

        @Override
        public void onReceivedMessage(ChatInputEvent event)
        {
                String text = event.getText();
                if(text == null)
                        return;

                chatLines.addLast(text);
                while(chatLines.size() > chatHistory.getValueI())
                        chatLines.removeFirst();
        }

        @Override
        public void onUpdate()
        {
                if(mc.player != null)
                {
                        hadPlayer = true;
                        ServerData data = mc.getCurrentServerData();
                        if(data != null)
                                lastServer = data.serverIP;
                        else
                                lastServer = "Singleplayer";
                        return;
                }

                if(!hadPlayer)
                        return;

                hadPlayer = false;
                if(mc.currentScreen instanceof GuiDisconnected)
                        writeReport((GuiDisconnected)mc.currentScreen);
        }

        private void writeReport(GuiDisconnected screen)
        {
                if(logFile == null)
                {
                        File dir = new File(
                                net.minecraftforge.fml.common.Loader.instance()
                                        .getConfigDir(),
                                "wurst0");
                        if(!dir.exists())
                                dir.mkdirs();
                        logFile = new File(dir, "kicks.txt");
                }

                StringBuilder sb = new StringBuilder();
                sb.append("=== kick at ").append(fmt.format(new Date()))
                        .append(" ===\r\n");
                sb.append("server: ").append(lastServer).append("\r\n");
                sb.append("reason: ").append(getReason(screen))
                        .append("\r\n");
                sb.append("message: ").append(getMessage(screen))
                        .append("\r\n");
                sb.append("chat history:\r\n");
                Iterator<String> itr = chatLines.iterator();
                while(itr.hasNext())
                {
                        String line = itr.next()
                                .replaceAll("\u00a7.", "");
                        sb.append("  ").append(line).append("\r\n");
                }
                sb.append("\r\n");

                try
                {
                        Writer w = new OutputStreamWriter(
                                new FileOutputStream(logFile, true),
                                StandardCharsets.UTF_8);
                        w.write(sb.toString());
                        w.close();
                        kicksLogged++;
                }catch(Exception e)
                {
                        // disk full etc. - silently keep going
                }
        }

        private String getReason(GuiDisconnected screen)
        {
                if(reasonField == null)
                        reasonField = ReflectUtils.field(GuiDisconnected.class,
                                "reason", "field_146306_a");
                return readStringField(reasonField, screen);
        }

        private String getMessage(GuiDisconnected screen)
        {
                if(messageField == null)
                        messageField = ReflectUtils.field(
                                GuiDisconnected.class, "message",
                                "field_146304_f");
                if(messageField == null)
                        return "?";
                try
                {
                        Object value = messageField.get(screen);
                        if(value instanceof ITextComponent)
                                return ((ITextComponent)value)
                                        .getFormattedText();
                }catch(Throwable t)
                {
                        // fall through
                }
                return "?";
        }

        private String readStringField(java.lang.reflect.Field field,
                Object target)
        {
                if(field == null)
                        return "?";
                try
                {
                        Object value = field.get(target);
                        return value == null ? "?" : String.valueOf(value);
                }catch(Throwable t)
                {
                        return "?";
                }
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(!showStatus.isChecked() || kicksLogged == 0
                        || mc.player == null)
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                mc.fontRenderer.drawStringWithShadow(
                        "Kicks logged: " + kicksLogged, 2,
                        sr.getScaledHeight() - 52, 0x50C050);
        }

        @Override
        protected void onEnable()
        {
                hadPlayer = mc.player != null;
        }

        @Override
        protected void onDisable()
        {
                chatLines.clear();
        }
}
