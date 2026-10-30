/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.feature;

import net.wurst0.Category;
import net.wurst0.command.CmdException;

public abstract class Command extends Feature
{
        public Command(String name, String description)
        {
                super(name, description, Category.COMMANDS);
        }

        public abstract void call(String[] args) throws CmdException;
}
