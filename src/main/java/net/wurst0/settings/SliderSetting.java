/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.settings;

public final class SliderSetting extends Setting
{
	private double value;
	private final double minimum;
	private final double maximum;
	private final double increment;
	private final int decimals;

	public SliderSetting(String name, double value, double minimum,
		double maximum, double increment)
	{
		super(name);
		this.value = value;
		this.minimum = minimum;
		this.maximum = maximum;
		this.increment = increment;

		String incStr = Double.toString(increment);
		int dot = incStr.indexOf('.');
		decimals = dot < 0 ? 0 : incStr.length() - dot - 1;
	}

	public double getValue()
	{
		return value;
	}

	public float getValueF()
	{
		return (float)value;
	}

	public int getValueI()
	{
		return (int)value;
	}

	public double getMinimum()
	{
		return minimum;
	}

	public double getMaximum()
	{
		return maximum;
	}

	public int getDecimals()
	{
		return decimals;
	}

	public void setValue(double value)
	{
		if(increment > 0)
			value = Math.round(value / increment) * increment;
		value = Math.max(minimum, Math.min(maximum, value));
		this.value = value;
	}
}
