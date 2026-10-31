/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.event;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.events.ChatOutputEvent;
import net.wurst0.event.events.DamageEvent;
import net.wurst0.event.events.HandRenderEvent;
import net.wurst0.event.events.KeyEvent;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.events.UpdateEvent;
import net.wurst0.event.listeners.ChatInputListener;
import net.wurst0.event.listeners.ChatOutputListener;
import net.wurst0.event.listeners.DamageListener;
import net.wurst0.event.listeners.HandRenderListener;
import net.wurst0.event.listeners.KeyPressListener;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;

public final class EventManager
{
        private final Set<Object> listeners =
                Collections.newSetFromMap(new ConcurrentHashMap<Object, Boolean>());

        public void add(Object listener)
        {
                listeners.add(listener);
        }

        public void remove(Object listener)
        {
                listeners.remove(listener);
        }

        public void fire(UpdateEvent event)
        {
                for(Object l : listeners)
                        if(l instanceof UpdateListener)
                                ((UpdateListener)l).onUpdate();
        }

        public void fire(RenderEvent event)
        {
                for(Object l : listeners)
                        if(l instanceof RenderListener)
                                ((RenderListener)l).onRender(event.getPartialTicks());
        }

        public void fire(KeyEvent event)
        {
                for(Object l : listeners)
                        if(l instanceof KeyPressListener)
                                ((KeyPressListener)l).onKeyPress(event.getKeyCode(),
                                        event.getState());
        }

        public void fire(ChatInputEvent event)
        {
                for(Object l : listeners)
                        if(l instanceof ChatInputListener)
                                ((ChatInputListener)l).onReceivedMessage(event);
        }

        public void fire(ChatOutputEvent event)
        {
                for(Object l : listeners)
                        if(l instanceof ChatOutputListener)
                                ((ChatOutputListener)l).onSentMessage(event);
        }

        public void fire(DamageEvent event)
        {
                for(Object l : listeners)
                        if(l instanceof DamageListener)
                                ((DamageListener)l).onDamage(event);
        }

        public void fire(ScreenRenderEvent event)
        {
                for(Object l : listeners)
                        if(l instanceof ScreenRenderListener)
                                ((ScreenRenderListener)l)
                                        .onScreenRender(event.getPartialTicks());
        }

        public void fire(net.wurst0.event.events.ChunkLoadEvent event)
        {
                for(Object l : listeners)
                        if(l instanceof net.wurst0.event.listeners.ChunkLoadListener)
                                ((net.wurst0.event.listeners.ChunkLoadListener)l)
                                        .onChunkLoad(event.getChunkX(), event.getChunkZ());
        }

        public void fire(HandRenderEvent event)
        {
                for(Object l : listeners)
                        if(l instanceof HandRenderListener)
                                ((HandRenderListener)l).onHandRender(event);
        }
}
