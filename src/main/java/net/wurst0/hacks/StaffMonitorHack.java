/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI StaffMonitorHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

/**
 * Watches the tab list and alerts you (chat + sound) when a player
 * whose name matches one of your patterns joins - useful for spotting
 * staff accounts with ranks in their names.
 */
public final class StaffMonitorHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final TextFieldSetting patterns =
		new TextFieldSetting("Name patterns", "admin,mod,owner,helper,staff",
			256);
	private final CheckboxSetting alertOnEveryJoin =
		new CheckboxSetting("Alert on every join", false);
	private final SliderSetting sound =
		new SliderSetting("Sound volume", 1, 0, 1, 0.05);

	private final Set<UUID> known = new HashSet<UUID>();
	private int timer;

	public StaffMonitorHack()
	{
		super("StaffMonitor",
			"Notifies you when staff members join the server (name"
				+ " pattern match on the tab list).");
		setCategory(Category.CHAT);
		addSettings(patterns, alertOnEveryJoin, sound);
	}

	@Override
	public void onUpdate()
	{
		if(mc.player == null || mc.getConnection() == null)
		{
			known.clear();
			return;
		}

		if(timer > 0)
		{
			timer--;
			return;
		}
		timer = 20; // poll once per second

		List<NetworkPlayerInfo> entries =
			new ArrayList<NetworkPlayerInfo>(
				mc.getConnection().getPlayerInfoMap());
		Set<UUID> current = new HashSet<UUID>();

		for(NetworkPlayerInfo info : entries)
		{
			UUID id = info.getGameProfile().getId();
			current.add(id);
			if(known.contains(id))
				continue;

			String name = info.getGameProfile().getName();
			boolean matches = matchesPattern(name);
			boolean alert = matches || alertOnEveryJoin.isChecked();
			if(known.isEmpty())
				continue; // first snapshot: no alerts, just baseline

			if(alert)
			{
				String tag = matches ? "\u00a7c[StaffMonitor]\u00a7r "
					: "\u00a77[StaffMonitor]\u00a7r ";
				mc.player.sendMessage(new TextComponentString(tag
					+ "\u00a7e" + name + "\u00a7r joined the server."
					+ (matches ? " \u00a7c(pattern match)" : "")));
				if(sound.getValue() > 0 && matches)
					mc.player.playSound(
						net.minecraft.init.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
						sound.getValueF(), 1.5F);
			}
		}

		known.clear();
		known.addAll(current);
	}

	private boolean matchesPattern(String name)
	{
		String raw = patterns.getValue();
		if(raw.trim().isEmpty())
			return false;
		String lower = name.toLowerCase();
		for(String p : raw.split(","))
		{
			String pat = p.trim().toLowerCase();
			if(!pat.isEmpty() && lower.contains(pat))
				return true;
		}
		return false;
	}

	@Override
	protected void onEnable()
	{
		known.clear();
		timer = 0;
	}

	@Override
	protected void onDisable()
	{
		known.clear();
	}
}
