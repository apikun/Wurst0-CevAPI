/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.event.listeners;

import net.wurst0.event.events.ChatOutputEvent;

public interface ChatOutputListener
{
        public void onSentMessage(ChatOutputEvent event);
}
