/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI RemoteEnderChestHack (GPL-3.0-or-later)
 *
 * 1.12.2 adaptation of the "linked ender chest" trick: when you open
 * an ender chest GUI, the screen is hidden without sending the close
 * packet, so the server keeps the container open and the contents
 * stay synced. A hotkey re-opens/hides the saved GUI later, anywhere
 * in the world. Unlike the original there are no screen mixins, so
 * pressing ESC on the re-opened GUI closes the container for real
 * (vanilla behavior) and breaks the link. Auto-totem and key-swap
 * options are not ported (they need modern hooks).
 */
package net.wurst0.hacks;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.InventoryEnderChest;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.TextFieldSetting;

public final class RemoteEnderChestHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting toggleKey =
                new TextFieldSetting("Toggle key", "X");
        private final CheckboxSetting chatFeedback =
                new CheckboxSetting("Chat feedback", true);

        private GuiChest savedScreen;
        private int savedContainerId = -1;
        private boolean linked;
        private boolean lastToggleKeyDown;

        public RemoteEnderChestHack()
        {
                super("RemoteEChest",
                        "Lets you keep your ender chest contents synced and"
                                + " re-open the GUI anywhere via a hotkey."
                                + " Open an ender chest once to link it.");
                setCategory(Category.OTHER);
                addSettings(toggleKey, chatFeedback);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                {
                        breakLink(false, null);
                        return;
                }

                if(!linked)
                {
                        tryLink(p);
                        return;
                }

                // server closed the container under us
                if(p.openContainer == null
                        || p.openContainer.windowId != savedContainerId)
                {
                        breakLink(false, "EChest link broken (container"
                                + " closed).");
                        return;
                }

                // opening any other container screen breaks the link
                if(mc.currentScreen != null && mc.currentScreen != savedScreen
                        && mc.currentScreen instanceof GuiContainer)
                {
                        breakLink(false,
                                "EChest link broken (other container opened).");
                        return;
                }

                handleToggleKey();
        }

        private void tryLink(EntityPlayerSP p)
        {
                if(!(mc.currentScreen instanceof GuiChest))
                        return;

                GuiChest screen = (GuiChest)mc.currentScreen;
                if(!(screen.inventorySlots instanceof ContainerChest))
                        return;

                ContainerChest container = (ContainerChest)screen.inventorySlots;
                if(!(container.getLowerChestInventory()
                        instanceof InventoryEnderChest))
                        return;

                savedScreen = screen;
                savedContainerId = p.openContainer.windowId;
                linked = true;
                mc.displayGuiScreen(null); // hide, but never send close!
                if(chatFeedback.isChecked())
                        mc.player.sendMessage(new TextComponentString(
                                "\u00a7a[RemoteEChest] Link created! Press "
                                        + toggleKey.getValue()
                                        + " to toggle the GUI."));
        }

        private void handleToggleKey()
        {
                int keyCode = Keyboard.getKeyIndex(toggleKey.getValue());
                if(keyCode == Keyboard.KEY_NONE)
                        return;

                boolean down = Keyboard.isKeyDown(keyCode);
                boolean justPressed = down && !lastToggleKeyDown;
                lastToggleKeyDown = down;
                if(!justPressed)
                        return;

                if(mc.currentScreen == savedScreen && savedScreen != null)
                {
                        // hide without closing the container
                        mc.displayGuiScreen(null);
                }else if(mc.currentScreen == null && savedScreen != null)
                {
                        mc.displayGuiScreen(savedScreen);
                }
        }

        private void breakLink(boolean sendClose, String message)
        {
                boolean wasLinked = linked;
                linked = false;

                if(sendClose && wasLinked && savedContainerId >= 0
                        && mc.getConnection() != null)
                        mc.getConnection().sendPacket(
                                new CPacketCloseWindow(savedContainerId));

                if(savedScreen != null && mc.currentScreen == savedScreen)
                        mc.displayGuiScreen(null);

                savedScreen = null;
                savedContainerId = -1;

                if(message != null && wasLinked && mc.player != null
                        && chatFeedback.isChecked())
                        mc.player.sendMessage(
                                new TextComponentString("\u00a7c[RemoteEChest] "
                                        + message));
        }

        @Override
        protected void onEnable()
        {
                breakLink(false, null);
        }

        @Override
        protected void onDisable()
        {
                breakLink(true, null);
        }
}
