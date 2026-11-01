/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BedrockEscapeHack (GPL-3.0-or-later)
 *
 * The original scans for bedrock escape shafts and tunnels through
 * the bedrock layer with spoofed movement packets (plus boat tricks
 * for the 1.21 nether roof). This 1.12.2 lite adaptation keeps the
 * core escape: it scans straight up or down for the nearest valid
 * pocket on the other side of the bedrock and teleports there with
 * filler movement packets + one PositionRotation packet. No anticheat
 * will tolerate this on strict servers.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;

public final class BedrockEscapeHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final ModeSetting direction = new ModeSetting("Direction",
                new String[]{"Up", "Down"}, 0);
        private final SliderSetting scanRange =
                new SliderSetting("Scan range", 32, 8, 64, 1);
        private final SliderSetting packetSpam =
                new SliderSetting("Teleport packets", 4, 1, 10, 1);
        private final SliderSetting interval =
                new SliderSetting("Interval (s)", 3, 1, 10, 1);

        private int timer;
        private int cooldown;

        public BedrockEscapeHack()
        {
                super("BedrockEscape",
                        "Escapes through bedrock by scanning for a pocket"
                                + " straight up or down and teleporting there"
                                + " with spoofed movement packets.");
                setCategory(Category.MOVEMENT);
                addSettings(direction, scanRange, packetSpam, interval);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                WorldClient world = mc.world;
                if(p == null || world == null || mc.getConnection() == null)
                        return;

                if(cooldown > 0)
                {
                        cooldown--;
                        return;
                }

                if(timer > 0)
                {
                        timer--;
                        return;
                }
                timer = interval.getValueI() * 20;

                BlockPos dest = findDestination(p, world);
                if(dest == null)
                        return;

                performTeleport(p, dest);
                cooldown = 100; // 5s between attempts
        }

        private BlockPos findDestination(EntityPlayerSP p, WorldClient world)
        {
                int x = (int)Math.floor(p.posX);
                int z = (int)Math.floor(p.posZ);
                boolean up = "Up".equals(direction.getSelected());
                int range = scanRange.getValueI();

                for(int d = 2; d <= range; d++)
                {
                        int y = (int)Math.floor(p.posY) + (up ? d : -d);
                        if(y < 1 || y > 255)
                                continue;

                        BlockPos feet = new BlockPos(x, y, z);
                        BlockPos head = feet.up();
                        BlockPos ground = feet.down();
                        if(!world.isAirBlock(feet) || !world.isAirBlock(head))
                                continue;
                        if(world.isAirBlock(ground))
                                continue;

                        // must actually cross bedrock between here and there
                        if(!crossesBedrock(world, x, z, y, up))
                                continue;

                        return feet;
                }
                return null;
        }

        private boolean crossesBedrock(WorldClient world, int x, int z, int y,
                boolean up)
        {
                int playerY = (int)Math.floor(mc.player.posY);
                if(up)
                {
                        for(int i = playerY + 1; i < y; i++)
                                if(world.getBlockState(new BlockPos(x, i, z))
                                        .getBlock() == net.minecraft.init.Blocks.BEDROCK)
                                        return true;
                }else
                {
                        for(int i = playerY - 1; i > y; i--)
                                if(world.getBlockState(new BlockPos(x, i, z))
                                        .getBlock() == net.minecraft.init.Blocks.BEDROCK)
                                        return true;
                }
                return false;
        }

        private void performTeleport(EntityPlayerSP p, BlockPos dest)
        {
                double dx = dest.getX() + 0.5;
                double dy = dest.getY();
                double dz = dest.getZ() + 0.5;

                // filler packets at the current position (mimics the original)
                for(int i = 0; i < packetSpam.getValueI(); i++)
                        mc.getConnection().sendPacket(new CPacketPlayer.Position(
                                p.posX, p.posY, p.posZ, p.onGround));

                mc.getConnection().sendPacket(new CPacketPlayer.PositionRotation(
                        dx, dy, dz, p.rotationYaw, p.rotationPitch, false));

                p.setPositionAndUpdate(dx, dy, dz);
                p.fallDistance = 0F;
                mc.player.sendMessage(new TextComponentString(
                        "\u00a7a[BedrockEscape] Teleported to " + dx + " "
                                + dy + " " + dz + "."));
        }

        @Override
        protected void onEnable()
        {
                timer = 0;
                cooldown = 0;
        }
}
