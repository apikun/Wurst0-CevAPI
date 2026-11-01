/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI HandOfGodHack (GPL-3.0-or-later)
 * Adapted to 1.12.2: the "execute at @s run" wrapper was dropped
 * (relative coords already run at your position) and the replace
 * filter needs an explicit data value in 1.12.2. Lightning uses the
 * 1.12.2 "lightning_bolt" entity id.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

public final class HandOfGodHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting block =
                new TextFieldSetting("Block", "lava");
        private final TextFieldSetting replaceBlock =
                new TextFieldSetting("Replace block", "grass");
        private final SliderSetting width =
                new SliderSetting("Width", 17, 1, 90, 1);
        private final SliderSetting height =
                new SliderSetting("Height", 11, 1, 90, 1);
        private final SliderSetting depth =
                new SliderSetting("Depth", 17, 1, 90, 1);
        private final CheckboxSetting replace =
                new CheckboxSetting("Replace", false);
        private final CheckboxSetting lightning =
                new CheckboxSetting("Lightning", true);
        private final CheckboxSetting automatic =
                new CheckboxSetting("Automatic", false);
        private final SliderSetting delay =
                new SliderSetting("Delay (ticks)", 3, 3, 20, 1);

        private int ticks;

        public HandOfGodHack()
        {
                super("HandOfGod",
                        "Fills a big area around you with a block and"
                                + " optionally strikes it with lightning."
                                + " Needs OP.");
                setCategory(Category.CREATIVE_OP);
                addSettings(block, replaceBlock, width, height, depth,
                        replace, lightning, automatic, delay);
        }

        @Override
        protected void onEnable()
        {
                ticks = delay.getValueI();
                invoke();
        }

        @Override
        protected void onDisable()
        {
                ticks = 0;
        }

        @Override
        public void onUpdate()
        {
                if(automatic.isChecked() && ticks++ >= delay.getValueI())
                {
                        ticks = 0;
                        invoke();
                }
        }

        private void invoke()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                        return;

                String fill = "fill ~-" + width.getValueI() + " ~-"
                        + height.getValueI() + " ~-" + depth.getValueI()
                        + " ~" + width.getValueI() + " ~"
                        + height.getValueI() + " ~" + depth.getValueI()
                        + " " + block.getValue().trim();
                if(replace.isChecked())
                {
                        // 1.12.2 fill needs the data value before "replace".
                        fill += " 0 replace " + replaceBlock.getValue().trim();
                }

                // 1.12.2 servers reject chat packets longer than 256 chars.
                if(fill.length() + 1 <= 256)
                        p.sendChatMessage("/" + fill);

                if(lightning.isChecked())
                        p.sendChatMessage("/summon lightning_bolt ~ ~ ~");
        }
}
