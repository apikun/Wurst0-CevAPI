/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0;

import org.lwjgl.input.Keyboard;

import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.wurst0.event.events.KeyEvent;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.events.UpdateEvent;

public final class ForgeEvents
{
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event)
        {
                if(event.phase != TickEvent.Phase.END)
                        return;

                WurstClient.INSTANCE.getEventManager().fire(new UpdateEvent());

                // save config at most once per tick when something changed
                try
                {
                        WurstClient wurst = WurstClient.INSTANCE;
                        if(wurst.getFileManager() != null
                                && wurst.getFileManager().isDirty())
                        {
                                wurst.getFileManager().save(wurst.getHacks());
                                wurst.getFileManager().saveKeybinds(wurst.getHacks());
                                wurst.getFileManager().clearDirty();
                        }
                }catch(Throwable t)
                {
                        t.printStackTrace();
                }
        }

        @SubscribeEvent
        public void onRenderWorldLast(RenderWorldLastEvent event)
        {
                WurstClient.INSTANCE.getEventManager()
                        .fire(new RenderEvent(event.getPartialTicks()));
        }

        @SubscribeEvent
        public void onKeyInput(KeyInputEvent event)
        {
                // 1.12.2's KeyInputEvent carries no key data; read the current
                // event from LWJGL while vanilla's keyboard loop is dispatching it.
                int keyCode = Keyboard.getEventKey();
                boolean state = Keyboard.getEventKeyState();
                if(keyCode == Keyboard.KEY_NONE || !state)
                        return;

                WurstClient.INSTANCE.getKeybindManager().handleKeyPress(keyCode);
                WurstClient.INSTANCE.getEventManager()
                        .fire(new KeyEvent(keyCode, true));
        }

        @SubscribeEvent
        public void onRenderOverlay(RenderGameOverlayEvent.Post event)
        {
                if(event.getType() != RenderGameOverlayEvent.ElementType.ALL)
                        return;

                WurstClient.INSTANCE.getHud().render(event.getPartialTicks());
        }
}
