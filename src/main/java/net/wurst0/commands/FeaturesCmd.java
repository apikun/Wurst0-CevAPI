/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FeaturesCmd (GPL-3.0-or-later)
 */
package net.wurst0.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.wurst0.Category;
import net.wurst0.WurstClient;
import net.wurst0.command.CmdException;
import net.wurst0.feature.Command;
import net.wurst0.feature.Feature;
import net.wurst0.feature.Hack;

public final class FeaturesCmd extends Command
{
	public FeaturesCmd()
	{
		super("features",
			"Lists all features, optionally filtered by category."
				+ " Usage: .features [category]");
	}

	@Override
	public void call(String[] args) throws CmdException
	{
		List<Feature> features = new ArrayList<Feature>();
		features.addAll(WurstClient.INSTANCE.getHacks().getList());
		features.addAll(WurstClient.INSTANCE.getCmds().getList());

		if(args.length == 1)
		{
			Category category = findCategory(args[0]);
			List<Feature> filtered = new ArrayList<Feature>();
			for(Feature feature : features)
				if(feature.getCategory() == category)
					filtered.add(feature);
			features = filtered;
		}

		Collections.sort(features, new java.util.Comparator<Feature>()
		{
			@Override
			public int compare(Feature a, Feature b)
			{
				return a.getName().compareToIgnoreCase(b.getName());
			}
		});

		StringBuilder builder = new StringBuilder();
		for(Feature feature : features)
		{
			if(builder.length() > 0)
				builder.append(", ");
			builder.append(feature.getName());
		}

		WurstClient.INSTANCE.getCmdProcessor().chat(
			features.size() + " features: " + builder.toString());
	}

	private Category findCategory(String name) throws CmdException
	{
		for(Category category : Category.values())
			if(category.getName().equalsIgnoreCase(name))
				return category;
		throw new CmdException("No category named \"" + name
			+ "\" exists.");
	}
}
