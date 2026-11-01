/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * GrimBypass: best-effort evasion helpers for the GrimAC anti-cheat,
 * built from an audit of Grim's open-source checks.
 *
 * What this actually does on a 1.12.2 client (and which Grim check it targets):
 *  - Transaction passthrough: CPacketConfirmTransaction / CPacketKeepAlive
 *    responses are never held by Blink/FakeLag/PacketDelay
 *    -> TransactionOrder, floating-transaction setbacks, BadPacketsO.
 *  - Post-safe ordering: attack / use-item / dig / click-window / abilities /
 *    held-item / entity-action packets are released right after a
 *    transaction response, the slot vanilla clients use
 *    -> Post ("sent packets out of the expected movement tick order").
 *  - Hold cap: while packets are held, a cyclic flush bounds how long
 *    position packets stall
 *    -> BadPacketsR ("stopped sending position updates while still
 *    responding to transactions").
 *  - Reach shield: combat hacks cap their attack range at ~2.95 blocks,
 *    inside Grim's 3.0 interpolated-hitbox Reach limit
 *    -> Reach ("attacked an entity from too far away").
 *  - Rotation smoothing: clamps the visible snap per tick, so flicks never
 *    exceed the >320-degree yaw snap AimModulo360 flags; pitch stays in
 *    [-90, 90] for BadPacketsD.
 *  - Idle packets: keeps a vanilla-shaped C03 flow when nothing happens.
 *  - Timer cap: clamps TimerHack's speed into Grim's tolerance
 *    -> Timer ("this usually flags 1.01 on low ping extremely quickly").
 *  - Flag alert: watches chat for Grim violation patterns, shows a HUD
 *    alert and can auto-disable fast movement hacks.
 *
 * GrimAC updates frequently; no bypass is guaranteed against every
 * version. Use responsibly and follow server rules.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.MathHelper;
import net.wurst0.Category;
import net.wurst0.WurstClient;
import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.listeners.ChatInputListener;
import net.wurst0.event.listeners.OverlayListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.PacketTap;

public final class GrimBypassHack extends Hack
        implements UpdateListener, ChatInputListener, OverlayListener
{
        private static GrimBypassHack instance;

        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting transactionPassthrough =
                new CheckboxSetting("Transaction passthrough", true);
        private final CheckboxSetting postSafeOrder =
                new CheckboxSetting("Post-safe ordering", true);
        private final CheckboxSetting holdCap =
                new CheckboxSetting("Hold cap", true);
        private final SliderSetting holdCapMs =
                new SliderSetting("Hold cap (ms)", 600, 100, 2000, 50);
        private final CheckboxSetting reachShield =
                new CheckboxSetting("Reach shield", true);
        private final CheckboxSetting idlePackets =
                new CheckboxSetting("Idle packets", true);
        private final CheckboxSetting smoothRotations =
                new CheckboxSetting("Smooth rotations", true);
        private final CheckboxSetting capTimer =
                new CheckboxSetting("Cap Timer speed", true);
        private final CheckboxSetting alertOnFlag =
                new CheckboxSetting("Alert on Grim flag", true);
        private final CheckboxSetting autoDisable =
                new CheckboxSetting("Auto-disable on kick", false);
        private final SliderSetting maxRotation =
                new SliderSetting("Max rotation per tick", 25, 5, 60, 1);

        private int idleCounter;
        private float lastYaw;
        private float lastPitch;
        private long lastFlagTime;
        private String lastFlagReason = "";

        public GrimBypassHack()
        {
                super("GrimBypass",
                        "Best-effort GrimAC evasion built from a Grim source"
                                + " audit: transaction passthrough, post-safe"
                                + " packet ordering, hold cap, reach shield,"
                                + " rotation smoothing, timer cap and kick"
                                + " alerts. No guarantee - Grim updates often.");
                setCategory(Category.OTHER);
                addSettings(transactionPassthrough, postSafeOrder, holdCap,
                        holdCapMs, reachShield, smoothRotations, maxRotation,
                        capTimer, idlePackets, alertOnFlag, autoDisable);
                instance = this;
        }

        /**
         * True when combat hacks should behave Grim-safe (range cap,
         * forced swing). Checked by Killaura & friends.
         */
        public static boolean isShieldActive()
        {
                return instance != null && instance.isEnabled()
                        && instance.reachShield.isChecked();
        }

        /**
         * Effective attack range: the requested range clamped into Grim's
         * reach check (3.0 blocks to the interpolated hitbox surface, with
         * a small margin for interpolation error).
         */
        public static double capAttackRange(double wanted)
        {
                return isShieldActive() ? Math.min(wanted, 2.95) : wanted;
        }

        /** True when attacks must always emit a swing animation. */
        public static boolean forceSwing()
        {
                return isShieldActive();
        }

        @Override
        protected void onEnable()
        {
                lastYaw = mc.player != null ? mc.player.rotationYaw : 0F;
                lastPitch = mc.player != null ? mc.player.rotationPitch : 0F;
                lastFlagTime = 0;
                PacketTap.ensureInstalled();
                applyTapSettings();
        }

        @Override
        protected void onDisable()
        {
                TimerHack.grimCap = false;
                PacketTap.setVitalPassthrough(false);
                PacketTap.setPostSafe(false);
                PacketTap.setHoldTimeoutMs(0);
        }

        private void applyTapSettings()
        {
                PacketTap.ensureInstalled();
                PacketTap.setVitalPassthrough(
                        transactionPassthrough.isChecked());
                PacketTap.setPostSafe(postSafeOrder.isChecked());
                PacketTap.setHoldTimeoutMs(holdCap.isChecked()
                        ? (long)holdCapMs.getValue() : 0L);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null)
                        return;

                applyTapSettings();

                // 1) timer cap
                TimerHack.grimCap = capTimer.isChecked();

                // 2) idle packets: one vanilla-shaped C03 per second
                if(idlePackets.isChecked() && !p.isRiding())
                {
                        idleCounter++;
                        if(idleCounter >= 20)
                        {
                                idleCounter = 0;
                                p.connection.sendPacket(new CPacketPlayer
                                        .Position(p.posX, p.getEntityBoundingBox()
                                                .minY, p.posZ, p.onGround));
                        }
                }

                // 3) rotation smoothing: clamp the visible snap per tick,
                //    keep pitch inside BadPacketsD bounds
                if(smoothRotations.isChecked())
                {
                        float maxYaw = maxRotation.getValueF();
                        float dYaw = MathHelper.wrapDegrees(
                                p.rotationYaw - lastYaw);
                        float dPitch = p.rotationPitch - lastPitch;
                        if(Math.abs(dYaw) > maxYaw)
                        {
                                p.rotationYaw = lastYaw
                                        + maxYaw * Math.signum(dYaw);
                                p.prevRotationYaw = p.rotationYaw
                                        - maxYaw * 0.5F;
                        }
                        if(Math.abs(dPitch) > maxYaw)
                        {
                                p.rotationPitch = lastPitch
                                        + maxYaw * Math.signum(dPitch);
                                p.prevRotationPitch = p.rotationPitch
                                        - maxYaw * 0.5F;
                        }
                        p.rotationPitch = MathHelper.clamp(p.rotationPitch,
                                -90F, 90F);
                }
                lastYaw = p.rotationYaw;
                lastPitch = p.rotationPitch;
        }

        @Override
        public void onReceivedMessage(ChatInputEvent event)
        {
                String msg = event.getText();
                if(msg == null || msg.isEmpty())
                        return;

                String lower = msg.toLowerCase();
                boolean grim = lower.contains("grimac")
                        || lower.contains("grim")
                        || lower.contains("anti-cheat")
                        || lower.contains("anticheat")
                        || lower.contains("unfair advantage");
                if(!grim)
                        return;

                lastFlagTime = System.currentTimeMillis();
                lastFlagReason = msg.length() > 60
                        ? msg.substring(0, 60) : msg;

                if(alertOnFlag.isChecked())
                {
                        mc.player.sendMessage(new net.minecraft.util.text
                                .TextComponentString(
                                "\u00a7c[GrimBypass]\u00a77 " + lastFlagReason));
                }

                if(autoDisable.isChecked())
                {
                        String[] names = {"Flight", "Speed", "Timer",
                                "Jesus", "Step", "HighJump", "Blink",
                                "FakeLag", "PacketDelay"};
                        for(String name : names)
                        {
                                Hack h = WurstClient.INSTANCE.getHacks()
                                        .getByName(name);
                                if(h != null && h.isEnabled())
                                        h.setEnabled(false);
                        }
                }
        }

        @Override
        public void onOverlayRender(net.wurst0.event.events.OverlayEvent event)
        {
                if(lastFlagTime == 0)
                        return;

                long ago = System.currentTimeMillis() - lastFlagTime;
                if(ago > 10000)
                        return;

                net.minecraft.client.gui.ScaledResolution sr =
                        new net.minecraft.client.gui.ScaledResolution(mc);
                String text = "\u00a7cGrim flag " + (ago / 1000L) + "s ago";
                int x = sr.getScaledWidth() / 2
                        - mc.fontRenderer.getStringWidth(text) / 2;
                mc.fontRenderer.drawStringWithShadow(text,
                        x, sr.getScaledHeight() - 44, 0xFFC33B3B);
        }
}
