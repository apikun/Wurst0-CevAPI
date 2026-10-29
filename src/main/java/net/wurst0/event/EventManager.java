/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.event;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.wurst0.event.events.KeyEvent;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.events.UpdateEvent;
import net.wurst0.event.listeners.KeyPressListener;
import net.wurst0.event.listeners.RenderListener;
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
}
