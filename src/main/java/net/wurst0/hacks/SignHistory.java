/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SignHistory (GPL-3.0-or-later)
 * Lite adaptation: scans loaded sign tile entities and logs their
 * text to config/wurst0/signs.txt.
 */
package net.wurst0.hacks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.Loader;
import net.wurst0.Category;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.util.ReflectUtils;

public final class SignHistory extends Hack
        implements UpdateListener, ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting showCount =
                new CheckboxSetting("Show count on HUD", true);

        private final Set<String> seen = new HashSet<String>();
        private int logged;
        private int scanTimer;
        private File logFile;
        private final SimpleDateFormat fmt =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public SignHistory()
        {
                super("SignHistory",
                        "Records the text of every sign you walk past into"
                                + " config/wurst0/signs.txt.");
                setCategory(Category.INTEL);
                addSetting(showCount);
        }

        private String[] readSignText(TileEntitySign sign)
        {
                Field f = ReflectUtils.field(TileEntitySign.class,
                        "signText", "field_145915_a");
                if(f == null)
                        return null;
                try
                {
                        ITextComponent[] comps =
                                (ITextComponent[])f.get(sign);
                        if(comps == null)
                                return null;
                        String[] lines = new String[comps.length];
                        for(int i = 0; i < comps.length; i++)
                                lines[i] = comps[i] == null ? ""
                                        : comps[i].getUnformattedText();
                        return lines;
                }catch(Throwable t)
                {
                        return null;
                }
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(--scanTimer > 0)
                        return;
                scanTimer = 60;

                for(TileEntity te : mc.world.loadedTileEntityList)
                {
                        if(!(te instanceof TileEntitySign))
                                continue;

                        String[] lines = readSignText((TileEntitySign)te);
                        if(lines == null)
                                continue;

                        StringBuilder sb = new StringBuilder();
                        for(String line : lines)
                                sb.append(line.trim()).append(" / ");
                        String content = sb.toString();
                        if(content.trim().replace(" /", "").isEmpty())
                                continue;

                        String key = te.getPos().getX() + ","
                                + te.getPos().getY() + ","
                                + te.getPos().getZ() + "|" + content;
                        if(!seen.add(key))
                                continue;

                        try
                        {
                                if(logFile == null)
                                {
                                        File dir = new File(Loader.instance()
                                                .getConfigDir(), "wurst0");
                                        if(!dir.exists())
                                                dir.mkdirs();
                                        logFile = new File(dir, "signs.txt");
                                }
                                Writer w = new OutputStreamWriter(
                                        new FileOutputStream(logFile, true),
                                        StandardCharsets.UTF_8);
                                w.write(fmt.format(new Date()) + " | "
                                        + te.getPos().getX() + " "
                                        + te.getPos().getY() + " "
                                        + te.getPos().getZ() + " | "
                                        + content + "\r\n");
                                w.close();
                                logged++;
                        }catch(Exception e)
                        {
                                // keep going silently
                        }
                }
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(!showCount.isChecked() || mc.player == null || logged == 0)
                        return;

                ScaledResolution sr = new ScaledResolution(mc);
                mc.fontRenderer.drawStringWithShadow(
                        "Signs logged: " + logged, 2,
                        sr.getScaledHeight() - 52, 0xC0A050);
        }

        @Override
        protected void onEnable()
        {
                scanTimer = 0;
        }

        @Override
        protected void onDisable()
        {
                seen.clear();
        }
}
