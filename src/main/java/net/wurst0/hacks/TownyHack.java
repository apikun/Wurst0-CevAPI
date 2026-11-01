/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TownyHack (GPL-3.0-or-later)
 * Lite adaptation: the fork has no PlayerAttacksEntityListener, so
 * attack start/stop is detected from the attack key state, and the
 * Towny confirmation is matched loosely from incoming chat via the
 * fork's ChatInputListener.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.listeners.ChatInputListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

/**
 * Runs the configured Towny command (e.g. "/t toggle pvp") when you
 * start attacking and again shortly after you stop, so plugin PvP
 * is only enabled while you are actually fighting. Confirmation is
 * detected from the server's chat response.
 */
public final class TownyHack extends Hack
        implements UpdateListener, ChatInputListener
{
        private static final int MIN_INTERVAL_TICKS = 40;
        private static final int CONFIRM_TIMEOUT_TICKS = 100;
        private static final int MAX_RETRIES = 3;

        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting toggleCommand =
                new TextFieldSetting("Command", "t toggle pvp");
        private final SliderSetting attackEndDelay =
                new SliderSetting("End delay (ticks)", 1, 0, 100, 1);

        private boolean attackKeyWasDown;
        private boolean pvpToggledOn;
        private boolean awaitingConfirm;
        private int ticksSinceCommand;
        private int ticksSinceAttackReleased;
        private int retryCount;

        public TownyHack()
        {
                super("Towny",
                        "Automatically toggles plugin PvP (via the"
                                + " configured command) when you start"
                                + " attacking, and toggles it back when"
                                + " you stop fighting.");
                setCategory(Category.OTHER);
                addSettings(toggleCommand, attackEndDelay);
        }

        @Override
        protected void onEnable()
        {
                attackKeyWasDown = false;
                pvpToggledOn = false;
                awaitingConfirm = false;
                ticksSinceCommand = Integer.MAX_VALUE / 2;
                ticksSinceAttackReleased = 0;
                retryCount = 0;
        }

        @Override
        protected void onDisable()
        {
                // never leave PvP enabled behind us
                if(pvpToggledOn && mc.player != null)
                        sendCommand(mc.player);
                pvpToggledOn = false;
                awaitingConfirm = false;
                attackKeyWasDown = false;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null)
                        return;

                if(ticksSinceCommand < Integer.MAX_VALUE / 2)
                        ticksSinceCommand++;

                if(awaitingConfirm
                        && ticksSinceCommand > CONFIRM_TIMEOUT_TICKS)
                {
                        awaitingConfirm = false;
                        if(retryCount < MAX_RETRIES)
                                sendCommand(p);
                }

                boolean attackDown =
                        mc.gameSettings.keyBindAttack.isKeyDown();
                boolean pressed = attackDown && !attackKeyWasDown;
                attackKeyWasDown = attackDown;

                if(pressed && !pvpToggledOn)
                {
                        if(!awaitingConfirm
                                && ticksSinceCommand
                                        >= MIN_INTERVAL_TICKS)
                        {
                                sendCommand(p);
                                pvpToggledOn = true;
                        }
                        ticksSinceAttackReleased = 0;
                }else if(!attackDown && pvpToggledOn)
                {
                        ticksSinceAttackReleased++;
                        if(ticksSinceAttackReleased
                                > attackEndDelay.getValueI())
                        {
                                sendCommand(p);
                                pvpToggledOn = false;
                                ticksSinceAttackReleased = 0;
                        }
                }
        }

        @Override
        public void onReceivedMessage(ChatInputEvent event)
        {
                if(!awaitingConfirm)
                        return;
                String text = event.getText();
                if(text == null)
                        return;
                String lower = text.toLowerCase();
                if(lower.contains("pvp") || lower.contains("towny"))
                {
                        awaitingConfirm = false;
                        retryCount = 0;
                }
        }

        private void sendCommand(EntityPlayerSP p)
        {
                String cmd = toggleCommand.getValue().trim();
                if(cmd.isEmpty())
                        return;
                if(!cmd.startsWith("/"))
                        cmd = "/" + cmd;
                p.sendChatMessage(cmd);
                awaitingConfirm = true;
                ticksSinceCommand = 0;
                retryCount++;
        }
}
