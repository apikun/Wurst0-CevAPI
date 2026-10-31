/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI MassTpaHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class MassTpaHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting delay =
		new SliderSetting("Delay (ticks)", 20, 5, 100, 5);
	private final CheckboxSetting tpahere =
		new CheckboxSetting("Use /tpahere", false);

	private final ArrayDeque<String> queue = new ArrayDeque<String>();

	public MassTpaHack()
	{
		super("MassTpa",
			"Sends a teleport request to every player online."
				+ " Disables itself when done.");
		setCategory(Category.CHAT);
		addSettings(delay, tpahere);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.getConnection() == null)
			return;

		if(queue.isEmpty())
		{
			// send the last request, then finish
			p.sendMessage(new TextComponentString(
				"[Wurst0] MassTpa finished (" + sentCount
					+ " requests sent)."));
			setEnabled(false);
			return;
		}

		if(--timer > 0)
			return;
		timer = delay.getValueI();

		String name = queue.poll();
		p.sendChatMessage((tpahere.isChecked() ? "/tpahere " : "/tpa ")
			+ name);
		sentCount++;
	}

	private int timer;
	private int sentCount;

	@Override
	protected void onEnable()
	{
		queue.clear();
		sentCount = 0;
		timer = 0;

		EntityPlayerSP p = mc.player;
		if(p == null || mc.getConnection() == null)
			return;

		String self = p.getGameProfile().getName();
		Collection<NetworkPlayerInfo> infoMap =
			mc.getConnection().getPlayerInfoMap();
		List<String> names = new ArrayList<String>();
		for(NetworkPlayerInfo info : infoMap)
		{
			String name = info.getGameProfile().getName();
			if(name.equals(self))
				continue;
			names.add(name);
		}

		queue.addAll(names);
		p.sendMessage(new TextComponentString("[Wurst0] MassTpa: sending "
			+ names.size() + " requests..."));
	}

	@Override
	protected void onDisable()
	{
		queue.clear();
	}
}
