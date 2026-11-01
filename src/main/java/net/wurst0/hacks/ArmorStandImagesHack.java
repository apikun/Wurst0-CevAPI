/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ArmorStandImagesHack (GPL-3.0-or-later)
 * REDUCED SCOPE: the original converts a whole PNG file into a grid
 * of colored pixel displays, which needs 1.13+ JSON custom names
 * (1.12.2 CustomName is a plain, uncolored string). This port instead
 * places a single armor stand wearing your skin's player head at the
 * block you are looking at, using 1.12.2 skull NBT.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.wurst0.Category;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

public final class ArmorStandImagesHack extends Hack
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private static final String MARKER = "CEVAPI_HEAD";

        private final CheckboxSetting clearPrevious =
                new CheckboxSetting("Clear previous", true);
        private final CheckboxSetting noGravity =
                new CheckboxSetting("No gravity", true);

        public ArmorStandImagesHack()
        {
                super("ArmorStandImages",
                        "Places an armor stand with your skin's player head"
                                + " at the block you look at (reduced port; the"
                                + " original PNG-to-pixel-display pipeline needs"
                                + " 1.13+ colored names).");
                setCategory(Category.CREATIVE_OP);
                addSettings(clearPrevious, noGravity);
        }

        @Override
        protected void onEnable()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                {
                        setEnabled(false);
                        return;
                }

                if(clearPrevious.isChecked())
                        p.sendChatMessage("/kill @e[type=armor_stand,name="
                                + MARKER + "]");

                int x = MathHelper.floor(p.posX);
                int y = MathHelper.floor(p.posY);
                int z = MathHelper.floor(p.posZ);
                RayTraceResult hit = mc.objectMouseOver;
                if(hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK)
                {
                        BlockPos pos = hit.getBlockPos();
                        x = pos.getX();
                        y = pos.getY() + 1;
                        z = pos.getZ();
                }

                String self = p.getGameProfile().getName();
                String command = "summon armor_stand " + x + " " + y + " "
                        + z + " {ShowArms:1b"
                        + (noGravity.isChecked() ? ",NoGravity:1b" : "")
                        + ",CustomName:\"" + MARKER + "\""
                        + ",ArmorItems:[{},{},{},{id:\"minecraft:skull\""
                        + ",Count:1b,Damage:3b,tag:{SkullOwner:\""
                        + nbtEscape(self) + "\"}}]}";

                // 1.12.2 servers reject chat packets longer than 256 chars.
                if(command.length() + 1 <= 256)
                        p.sendChatMessage("/" + command);

                setEnabled(false);
        }

        private static String nbtEscape(String s)
        {
                if(s == null)
                        return "";
                return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
}
