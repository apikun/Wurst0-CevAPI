/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraftforge.fml.common.Loader;
import net.wurst0.feature.Hack;
import net.wurst0.hacks.Hacks;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.Setting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

public final class FileManager
{
        private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        private final File settingsFile;
        private final File keybindsFile;
        private boolean dirty;

        public FileManager()
        {
                File dir = new File(Loader.instance().getConfigDir(), "wurst0");
                if(!dir.exists())
                        dir.mkdirs();
                settingsFile = new File(dir, "settings.json");
                keybindsFile = new File(dir, "keybinds.json");
        }

        /** Called by features on changes. Saves on the next update tick. */
        public void markDirty()
        {
                dirty = true;
        }

        public boolean isDirty()
        {
                return dirty;
        }

        public void clearDirty()
        {
                dirty = false;
        }

        public void load(Hacks hacks)
        {
                loadSettings(hacks);
                loadKeybinds(hacks);
        }

        public void loadSettings(Hacks hacks)
        {
                if(!settingsFile.isFile())
                        return;

                try
                {
                        BufferedReader reader =
                                new BufferedReader(new InputStreamReader(
                                        new FileInputStream(settingsFile), StandardCharsets.UTF_8));
                        JsonElement element = new JsonParser().parse(reader);
                        reader.close();

                        if(element == null || !element.isJsonObject())
                                return;

                        JsonObject root = element.getAsJsonObject();

                        for(Entry<String, JsonElement> e : root.entrySet())
                        {
                                Hack hack = hacks.getByName(e.getKey());
                                if(hack == null || !e.getValue().isJsonObject())
                                        continue;

                                JsonObject hackJson = e.getValue().getAsJsonObject();

                                if(hackJson.has("enabled"))
                                        hack.setEnabled(
                                                hackJson.get("enabled").getAsBoolean());

                                for(Setting setting : hack.getSettings())
                                {
                                        if(!hackJson.has(setting.getName()))
                                                continue;

                                        JsonElement s = hackJson.get(setting.getName());
                                        if(setting instanceof CheckboxSetting)
                                                ((CheckboxSetting)setting).setChecked(s.getAsBoolean());
                                        else if(setting instanceof SliderSetting)
                                                ((SliderSetting)setting).setValue(s.getAsDouble());
                                        else if(setting instanceof ModeSetting)
                                                ((ModeSetting)setting).setSelected(s.getAsString());
                                        else if(setting instanceof TextFieldSetting)
                                                ((TextFieldSetting)setting).setValue(s.getAsString());
                                }
                        }
                }catch(Exception e)
                {
                        System.err.println("[Wurst0] Failed to load settings:");
                        e.printStackTrace();
                }
        }

        public void save(Hacks hacks)
        {
                try
                {
                        JsonObject root = new JsonObject();

                        for(Hack hack : hacks.getList())
                        {
                                JsonObject hackJson = new JsonObject();
                                hackJson.addProperty("enabled", hack.isEnabled());

                                for(Setting setting : hack.getSettings())
                                {
                                        if(setting instanceof CheckboxSetting)
                                                hackJson.addProperty(setting.getName(),
                                                        ((CheckboxSetting)setting).isChecked());
                                        else if(setting instanceof SliderSetting)
                                                hackJson.addProperty(setting.getName(),
                                                        ((SliderSetting)setting).getValue());
                                        else if(setting instanceof ModeSetting)
                                                hackJson.addProperty(setting.getName(),
                                                        ((ModeSetting)setting).getSelected());
                                        else if(setting instanceof TextFieldSetting)
                                                hackJson.addProperty(setting.getName(),
                                                        ((TextFieldSetting)setting).getValue());
                                }

                                root.add(hack.getName(), hackJson);
                        }

                        write(settingsFile, root);
                }catch(Exception e)
                {
                        System.err.println("[Wurst0] Failed to save settings:");
                        e.printStackTrace();
                }
        }

        public void loadKeybinds(Hacks hacks)
        {
                if(!keybindsFile.isFile())
                        return;

                try
                {
                        BufferedReader reader =
                                new BufferedReader(new InputStreamReader(
                                        new FileInputStream(keybindsFile), StandardCharsets.UTF_8));
                        JsonElement element = new JsonParser().parse(reader);
                        reader.close();

                        if(element == null || !element.isJsonArray())
                                return;

                        for(JsonElement e : element.getAsJsonArray())
                        {
                                if(!e.isJsonObject())
                                        continue;
                                JsonObject kb = e.getAsJsonObject();
                                Hack hack = hacks.getByName(kb.get("hack").getAsString());
                                if(hack != null)
                                        hack.setKeybind(kb.get("key").getAsInt());
                        }
                }catch(Exception e)
                {
                        System.err.println("[Wurst0] Failed to load keybinds:");
                        e.printStackTrace();
                }
        }

        public void saveKeybinds(Hacks hacks)
        {
                try
                {
                        JsonArray array = new JsonArray();

                        for(Hack hack : hacks.getList())
                                if(hack.getKeybind() != -1)
                                {
                                        JsonObject kb = new JsonObject();
                                        kb.addProperty("hack", hack.getName());
                                        kb.addProperty("key", hack.getKeybind());
                                        array.add(kb);
                                }

                        write(keybindsFile, array);
                }catch(Exception e)
                {
                        System.err.println("[Wurst0] Failed to save keybinds:");
                        e.printStackTrace();
                }
        }

        private void write(File file, JsonElement json) throws Exception
        {
                Writer writer = new OutputStreamWriter(
                        new FileOutputStream(file), StandardCharsets.UTF_8);
                writer.write(gson.toJson(json));
                writer.close();
        }
}
