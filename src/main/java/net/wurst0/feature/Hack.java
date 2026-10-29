/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.feature;

import net.wurst0.Category;
import net.wurst0.WurstClient;
import net.wurst0.event.EventManager;

public abstract class Hack extends Feature
{
	private boolean enabled;
	private int keybind = -1;

	public Hack(String name, String description)
	{
		super(name, description, Category.OTHER);
	}

	public final boolean isEnabled()
	{
		return enabled;
	}

	public final void setEnabled(boolean enabled)
	{
		if(this.enabled == enabled)
			return;

		this.enabled = enabled;

		EventManager events = WurstClient.INSTANCE.getEventManager();
		if(enabled)
		{
			events.add(this);
			onEnable();
		}else
		{
			events.remove(this);
			onDisable();
		}

		WurstClient.INSTANCE.getFileManager().markDirty();
	}

	public final void toggle()
	{
		setEnabled(!enabled);
	}

	protected void onEnable()
	{}

	protected void onDisable()
	{}

	public int getKeybind()
	{
		return keybind;
	}

	public void setKeybind(int keybind)
	{
		this.keybind = keybind;
	}
}
