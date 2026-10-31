/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TooManyHaxHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.wurst0.Category;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.TextFieldSetting;

/**
 * Hides selected hacks from the ClickGUI (and optionally from the HUD
 * arraylist) - for when you want to stream or record without showing
 * every feature. "Hide only" leaves them visible in the HUD.
 */
public final class TooManyHaxHack extends Hack
{
	private final TextFieldSetting hidden =
		new TextFieldSetting("Hidden hacks",
			"comma,separated,names", 512);
	private final CheckboxSetting hideOnly =
		new CheckboxSetting("Hide only (still show in HUD)", false);

	public TooManyHaxHack()
	{
		super("TooManyHax",
			"Hides selected features from the ClickGUI so nobody can"
				+ " see how many hax you have.");
		setCategory(Category.OTHER);
		addSettings(hidden, hideOnly);
	}

	/**
	 * @return true if the given feature name should be hidden from the
	 *         ClickGUI while TooManyHax is enabled
	 */
	public boolean isHidden(String featureName)
	{
		return isBlockedByList(featureName, false);
	}

	/**
	 * @return true if the given feature name should also be hidden
	 *         from the HUD arraylist
	 */
	public boolean isHiddenEverywhere(String featureName)
	{
		return isBlockedByList(featureName, true);
	}

	private boolean isBlockedByList(String featureName,
		boolean hudCheck)
	{
		if(!isEnabled())
			return false;
		if(hudCheck && hideOnly.isChecked())
			return false;
		String raw = hidden.getValue();
		if(raw.trim().isEmpty())
			return false;
		for(String n : raw.split(","))
		{
			if(n.trim().equalsIgnoreCase(featureName))
				return true;
		}
		return false;
	}

	/** For the HUD filter: comma list parsing shared with isHidden. */
	public List<String> getHiddenNames()
	{
		List<String> names = new ArrayList<String>();
		for(String n : hidden.getValue().split(","))
			if(!n.trim().isEmpty())
				names.add(n.trim());
		return names;
	}
}
