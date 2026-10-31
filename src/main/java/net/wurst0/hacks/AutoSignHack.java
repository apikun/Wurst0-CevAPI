/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoSignHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.network.play.client.CPacketUpdateSign;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.TextFieldSetting;
import net.wurst0.util.ReflectUtils;

/**
 * Instantly writes your stored text onto every sign you open and
 * closes the editor for you. Set the text in the field, one line
 * per sign row (max 4). With "Overwrite" off, signs that already
 * have text are left alone.
 */
public final class AutoSignHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final TextFieldSetting text = new TextFieldSetting("Text",
		"Line1\\nLine2\\nLine3\\nLine4");
	private final CheckboxSetting overwrite =
		new CheckboxSetting("Overwrite", true);

	private Field tileField;
	private Field textField;

	public AutoSignHack()
	{
		super("AutoSign",
			"Writes your text on every sign you open. Use \\n to"
				+ " split the four lines.");
		setCategory(Category.BLOCKS);
		addSettings(text, overwrite);
	}

	@Override
	public void onUpdate()
	{
		if(mc.player == null || mc.getConnection() == null)
			return;
		if(!(mc.currentScreen instanceof GuiEditSign))
			return;

		GuiEditSign gui = (GuiEditSign)mc.currentScreen;
		TileEntitySign tile = getTileSign(gui);
		if(tile == null)
			return;

		ITextComponent[] lines = getSignText(tile);
		if(lines == null || lines.length != 4)
			return;

		if(!overwrite.isChecked() && !isEmptySign(lines))
			return;

		String[] wanted = text.getValue().split("\\\\n");
		for(int i = 0; i < 4; i++)
		{
			String line = i < wanted.length ? wanted[i] : "";
			if(line.length() > 45)
				line = line.substring(0, 45);
			lines[i] = new TextComponentString(line);
		}

		tile.markDirty();
		mc.getConnection().sendPacket(new CPacketUpdateSign(tile.getPos(),
			lines));
		mc.displayGuiScreen(null);
	}

	private TileEntitySign getTileSign(GuiEditSign gui)
	{
		if(tileField == null)
			tileField = ReflectUtils.field(GuiEditSign.class,
				"tileSign", "field_146848_f");
		if(tileField == null)
			return null;

		try
		{
			return (TileEntitySign)tileField.get(gui);
		}catch(Exception e)
		{
			return null;
		}
	}

	private ITextComponent[] getSignText(TileEntitySign tile)
	{
		if(textField == null)
			textField = ReflectUtils.field(TileEntitySign.class,
				"signText", "field_145916_j");
		if(textField == null)
			return null;

		try
		{
			return (ITextComponent[])textField.get(tile);
		}catch(Exception e)
		{
			return null;
		}
	}

	private static boolean isEmptySign(ITextComponent[] lines)
	{
		for(ITextComponent line : lines)
			if(line != null && !line.getUnformattedText().isEmpty())
				return false;
		return true;
	}
}
