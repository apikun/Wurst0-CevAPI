/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.wurst0.Category;
import net.wurst0.settings.Setting;

public abstract class Feature
{
	private final String name;
	private final String description;
	private Category category;
	private final List<Setting> settings = new ArrayList<>();

	public Feature(String name, String description, Category category)
	{
		this.name = name;
		this.description = description;
		this.category = category;
	}

	public String getName()
	{
		return name;
	}

	public String getDescription()
	{
		return description;
	}

	public Category getCategory()
	{
		return category;
	}

	protected void setCategory(Category category)
	{
		this.category = category;
	}

	public List<Setting> getSettings()
	{
		return settings;
	}

	protected void addSetting(Setting setting)
	{
		settings.add(setting);
	}

	protected void addSettings(Setting... setts)
	{
		settings.addAll(Arrays.asList(setts));
	}
}
