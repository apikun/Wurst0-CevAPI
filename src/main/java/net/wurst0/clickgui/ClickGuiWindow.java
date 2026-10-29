/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.clickgui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.wurst0.Category;
import net.wurst0.WurstClient;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.Setting;
import net.wurst0.settings.SliderSetting;

public final class ClickGuiWindow
{
	private static final int WIDTH = 106;
	private static final int TITLE_HEIGHT = 12;
	private static final int COMPONENT_HEIGHT = 12;

	private final Category category;
	private final List<Component> components =
		new ArrayList<Component>();
	private int x;
	private int y;
	private boolean dragging;
	private int dragOffsetX;
	private int dragOffsetY;

	public ClickGuiWindow(Category category, int x, int y, List<Hack> hacks)
	{
		this.category = category;
		this.x = x;
		this.y = y;

		for(Hack hack : hacks)
			components.add(new FeatureComponent(hack));
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public int getWidth()
	{
		return WIDTH;
	}

	public int getHeight()
	{
		int height = TITLE_HEIGHT;
		for(Component c : components)
			height += c.getHeight();
		return height;
	}

	private static FontHelper fr()
	{
		return FontHelper.INSTANCE;
	}

	public void render(int mouseX, int mouseY)
	{
		// window background
		Gui.drawRect(x, y, x + WIDTH, y + getHeight(), 0xC0101010);
		// title bar
		int titleColor = 0xFF000000 | (category.getColor() & 0x3F3F3F)
			<< 0 | 0xFF1E1E28;
		titleColor = 0xFF1E1E28;
		Gui.drawRect(x, y, x + WIDTH, y + TITLE_HEIGHT, titleColor);
		Gui.drawRect(x, y + TITLE_HEIGHT - 1, x + WIDTH, y + TITLE_HEIGHT,
			0xFF000000 | category.getColor());

		fr().drawShadow(category.getName(), x + 3, y + 2, 0xFFFFFFFF);

		// components
		int cy = y + TITLE_HEIGHT;
		for(Component c : components)
		{
			c.render(x, cy, mouseX, mouseY);
			cy += c.getHeight();
		}
	}

	public void mouseClicked(int mouseX, int mouseY, int mouseButton)
	{
		if(mouseX < x || mouseX > x + WIDTH || mouseY < y
			|| mouseY > y + getHeight())
			return;

		if(mouseY < y + TITLE_HEIGHT)
		{
			if(mouseButton == 0)
			{
				dragging = true;
				dragOffsetX = mouseX - x;
				dragOffsetY = mouseY - y;
			}
			return;
		}

		int cy = y + TITLE_HEIGHT;
		for(Component c : components)
		{
			if(mouseY >= cy && mouseY < cy + c.getHeight())
			{
				c.mouseClicked(mouseX, mouseY, mouseButton);
				return;
			}
			cy += c.getHeight();
		}
	}

	public void mouseReleased(int mouseX, int mouseY, int state)
	{
		dragging = false;
		for(Component c : components)
			c.mouseReleased(mouseX, mouseY, state);
	}

	public void mouseClickMove(int mouseX, int mouseY, int clickedButton,
		long timeSinceLastClick)
	{
		if(dragging)
		{
			x = mouseX - dragOffsetX;
			y = mouseY - dragOffsetY;
			return;
		}

		int cy = y + TITLE_HEIGHT;
		for(Component c : components)
		{
			if(mouseY >= cy && mouseY < cy + c.getHeight())
			{
				c.mouseClickMove(mouseX, mouseY, clickedButton);
				return;
			}
			cy += c.getHeight();
		}
	}

	// ==================== components ====================

	private abstract static class Component
	{
		abstract int getHeight();

		abstract void render(int wx, int wy, int mouseX, int mouseY);

		void mouseClicked(int mouseX, int mouseY, int mouseButton)
		{}

		void mouseReleased(int mouseX, int mouseY, int state)
		{}

		void mouseClickMove(int mouseX, int mouseY, int clickedButton)
		{}

		static boolean hovered(int wx, int wy, int mouseX, int mouseY)
		{
			return mouseX >= wx && mouseX < wx + WIDTH && mouseY >= wy
				&& mouseY < wy + COMPONENT_HEIGHT;
		}
	}

	private final class FeatureComponent extends Component
	{
		private final Hack hack;
		private final List<Component> children =
			new ArrayList<Component>();
		private boolean expanded;

		FeatureComponent(Hack hack)
		{
			this.hack = hack;

			for(Setting setting : hack.getSettings())
			{
				if(setting instanceof CheckboxSetting)
					children.add(new CheckboxComponent(
						(CheckboxSetting)setting));
				else if(setting instanceof SliderSetting)
					children.add(new SliderComponent((SliderSetting)setting));
				else if(setting instanceof ModeSetting)
					children.add(new ModeComponent((ModeSetting)setting));
			}
		}

		@Override
		int getHeight()
		{
			int height = COMPONENT_HEIGHT;
			if(expanded)
				for(Component c : children)
					height += c.getHeight();
			return height;
		}

		@Override
		void render(int wx, int wy, int mouseX, int mouseY)
		{
			boolean hov = hovered(wx, wy, mouseX, mouseY);
			if(hov)
				Gui.drawRect(wx, wy, wx + WIDTH, wy + COMPONENT_HEIGHT,
					0x30FFFFFF);

			if(hack.isEnabled())
				Gui.drawRect(wx, wy + 2, wx + 2, wy + COMPONENT_HEIGHT - 2,
					0xFF00E000);

			int color = hack.isEnabled() ? 0xFF00FF00 : 0xFFD0D0D0;
			fr().drawShadow(hack.getName(), wx + 5, wy + 2, color);

			if(!children.isEmpty())
				fr().drawShadow(expanded ? "-" : "+", wx + WIDTH - 8,
					wy + 2, 0xFF909090);

			int cy = wy + COMPONENT_HEIGHT;
			if(expanded)
			{
				Gui.drawRect(wx + 1, cy, wx + 2,
					cy + getHeight() - COMPONENT_HEIGHT, 0xFF303030);
				for(Component c : children)
				{
					c.render(wx, cy, mouseX, mouseY);
					cy += c.getHeight();
				}
			}
		}

		@Override
		void mouseClicked(int mouseX, int mouseY, int mouseButton)
		{
			if(mouseButton == 0)
				hack.toggle();
			else if(mouseButton == 1 && !children.isEmpty())
				expanded = !expanded;

			WurstClient.INSTANCE.getFileManager().markDirty();
		}

		@Override
		void mouseReleased(int mouseX, int mouseY, int state)
		{
			for(Component c : children)
				c.mouseReleased(mouseX, mouseY, state);
		}

		@Override
		void mouseClickMove(int mouseX, int mouseY, int clickedButton)
		{
			if(!expanded)
				return;

			int cy = ClickGuiWindow.this.y + TITLE_HEIGHT
				+ COMPONENT_HEIGHT;
			for(Component c : children)
			{
				if(mouseY >= cy && mouseY < cy + c.getHeight())
				{
					c.mouseClickMove(mouseX, mouseY, clickedButton);
					return;
				}
				cy += c.getHeight();
			}
		}
	}

	private final class CheckboxComponent extends Component
	{
		private final CheckboxSetting setting;

		CheckboxComponent(CheckboxSetting setting)
		{
			this.setting = setting;
		}

		@Override
		int getHeight()
		{
			return COMPONENT_HEIGHT;
		}

		@Override
		void render(int wx, int wy, int mouseX, int mouseY)
		{
			if(hovered(wx, wy, mouseX, mouseY))
				Gui.drawRect(wx, wy, wx + WIDTH, wy + COMPONENT_HEIGHT,
					0x30FFFFFF);

			// checkbox
			Gui.drawRect(wx + 5, wy + 3, wx + 10, wy + 8, 0xFF404040);
			if(setting.isChecked())
				Gui.drawRect(wx + 6, wy + 4, wx + 9, wy + 7, 0xFF00E000);

			fr().drawShadow(setting.getName(), wx + 14, wy + 2, 0xFFD0D0D0);
		}

		@Override
		void mouseClicked(int mouseX, int mouseY, int mouseButton)
		{
			if(mouseButton == 0)
			{
				setting.toggle();
				WurstClient.INSTANCE.getFileManager().markDirty();
			}
		}
	}

	private final class SliderComponent extends Component
	{
		private final SliderSetting setting;
		private boolean draggingSlider;

		SliderComponent(SliderSetting setting)
		{
			this.setting = setting;
		}

		@Override
		int getHeight()
		{
			return COMPONENT_HEIGHT;
		}

		@Override
		void render(int wx, int wy, int mouseX, int mouseY)
		{
			if(hovered(wx, wy, mouseX, mouseY))
				Gui.drawRect(wx, wy, wx + WIDTH, wy + COMPONENT_HEIGHT,
					0x30FFFFFF);

			// slider bar
			Gui.drawRect(wx + 5, wy + 9, wx + WIDTH - 5, wy + 10, 0xFF404040);

			double fraction =
				(setting.getValue() - setting.getMinimum())
					/ (setting.getMaximum() - setting.getMinimum());
			fraction = Math.max(0, Math.min(1, fraction));
			Gui.drawRect(wx + 5, wy + 9,
				wx + 5 + (int)((WIDTH - 10) * fraction), wy + 10,
				0xFF00C0C0);

			String text = setting.getName() + ": "
				+ formatValue(setting.getValue());
			fr().drawShadow(text, wx + 5, wy + 1, 0xFFD0D0D0);
		}

		private String formatValue(double value)
		{
			int decimals = setting.getDecimals();
			return decimals == 0 ? Integer.toString((int)value)
				: String.format("%." + decimals + "f", value);
		}

		@Override
		void mouseClicked(int mouseX, int mouseY, int mouseButton)
		{
			if(mouseButton == 0)
				draggingSlider = true;
			updateValue(mouseX);
		}

		@Override
		void mouseReleased(int mouseX, int mouseY, int state)
		{
			draggingSlider = false;
			WurstClient.INSTANCE.getFileManager().markDirty();
		}

		@Override
		void mouseClickMove(int mouseX, int mouseY, int clickedButton)
		{
			if(draggingSlider)
				updateValue(mouseX);
		}

		private void updateValue(int mouseX)
		{
			double fraction = (double)(mouseX - 5 - ClickGuiWindow.this.x)
				/ (WIDTH - 10);
			fraction = Math.max(0, Math.min(1, fraction));
			double value = setting.getMinimum()
				+ fraction * (setting.getMaximum() - setting.getMinimum());
			setting.setValue(value);
		}
	}

	private final class ModeComponent extends Component
	{
		private final ModeSetting setting;

		ModeComponent(ModeSetting setting)
		{
			this.setting = setting;
		}

		@Override
		int getHeight()
		{
			return COMPONENT_HEIGHT;
		}

		@Override
		void render(int wx, int wy, int mouseX, int mouseY)
		{
			if(hovered(wx, wy, mouseX, mouseY))
				Gui.drawRect(wx, wy, wx + WIDTH, wy + COMPONENT_HEIGHT,
					0x30FFFFFF);

			fr().drawShadow(setting.getName() + ": " + setting.getSelected(),
				wx + 5, wy + 2, 0xFFD0D0D0);
		}

		@Override
		void mouseClicked(int mouseX, int mouseY, int mouseButton)
		{
			if(mouseButton == 0)
			{
				setting.cycle();
				WurstClient.INSTANCE.getFileManager().markDirty();
			}
		}
	}

	/** Small helper so the window code doesn't drag in GlStateManager text. */
	private static final class FontHelper
	{
		static final FontHelper INSTANCE = new FontHelper();

		void drawShadow(String text, int x, int y, int color)
		{
			Minecraft.getMinecraft().fontRendererObj
				.drawStringWithShadow(text, x, y, color);
			GlStateManager.color(1, 1, 1, 1);
		}
	}
}
