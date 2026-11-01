/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI VoiderPlusHack (GPL-3.0-or-later)
 * Adapted to 1.12.2: height sliders use the 0-255 world height and
 * block coordinates come from the player's position. The client-side
 * "Permission level hold" no-op and the never-implemented "Teleport
 * forward" option of the original were dropped.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.MathHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

public final class VoiderPlusHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting block =
                new TextFieldSetting("Block", "air");
        private final SliderSetting radius =
                new SliderSetting("Radius", 45, 1, 90, 1);
        private final CheckboxSetting usePlayerY =
                new CheckboxSetting("Use player Y", true);
        private final SliderSetting playerHeight =
                new SliderSetting("Player height", 0, -64, 128, 1);
        private final SliderSetting minHeight =
                new SliderSetting("Minimum height", 0, 0, 255, 1);
        private final SliderSetting maxHeight =
                new SliderSetting("Maximum height", 128, 0, 255, 1);
        private final CheckboxSetting disableWhenDone =
                new CheckboxSetting("Disable when done", false);

        private int currentY;
        private int endY;
        private int direction;

        public VoiderPlusHack()
        {
                super("Voider+",
                        "Fills horizontal layers around you with the"
                                + " configured block (default air), one layer"
                                + " per tick, walking down to the minimum"
                                + " height. Needs OP.");
                setCategory(Category.CREATIVE_OP);
                addSettings(block, radius, usePlayerY, playerHeight,
                        minHeight, maxHeight, disableWhenDone);
        }

        @Override
        protected void onEnable()
        {
                EntityPlayerSP p = mc.player;
                if(p == null)
                        return;

                currentY = usePlayerY.isChecked()
                        ? MathHelper.floor(p.posY)
                                + playerHeight.getValueI()
                        : maxHeight.getValueI();
                if(currentY < 0)
                        currentY = 0;
                if(currentY > 255)
                        currentY = 255;
                endY = minHeight.getValueI();
                direction = currentY >= endY ? -1 : 1;
        }

        @Override
        protected void onDisable()
        {
                currentY = 0;
                endY = 0;
                direction = 0;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                        return;

                if((direction < 0 && currentY < endY)
                        || (direction > 0 && currentY > endY))
                {
                        if(disableWhenDone.isChecked())
                                setEnabled(false);
                        return;
                }

                int x = MathHelper.floor(p.posX);
                int z = MathHelper.floor(p.posZ);
                int r = radius.getValueI();
                String command = "fill " + (x - r) + " " + currentY + " "
                        + (z - r) + " " + (x + r) + " " + currentY + " "
                        + (z + r) + " " + block.getValue().trim();

                // 1.12.2 servers reject chat packets longer than 256 chars.
                if(command.length() + 1 <= 256)
                        p.sendChatMessage("/" + command);

                currentY += direction;
        }
}
