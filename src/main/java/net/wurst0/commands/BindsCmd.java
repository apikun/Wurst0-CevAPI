/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.commands;

import org.lwjgl.input.Keyboard;

import net.wurst0.WurstClient;
import net.wurst0.command.CmdException;
import net.wurst0.feature.Command;
import net.wurst0.feature.Hack;

public final class BindsCmd extends Command
{
        public BindsCmd()
        {
                super("binds", "Lists all keybinds. Usage: .binds | .binds gui");
        }

        @Override
        public void call(String[] args) throws CmdException
        {
                if(args.length == 1 && args[0].equalsIgnoreCase("gui"))
                {
                        net.minecraft.client.Minecraft.getMinecraft()
                                .addScheduledTask(() -> net.minecraft.client.Minecraft
                                        .getMinecraft().displayGuiScreen(
                                                new net.wurst0.clickgui.KeybindsScreen()));
                        return;
                }

                if(args.length != 0)
                        throw new CmdException("Usage: .binds | .binds gui");

                boolean any = false;
                for(Hack hack : WurstClient.INSTANCE.getHacks().getList())
                        if(hack.getKeybind() != -1)
                        {
                                any = true;
                                WurstClient.INSTANCE.getCmdProcessor().chat(
                                        hack.getName() + ": "
                                                + Keyboard.getKeyName(hack.getKeybind()));
                        }

                if(!any)
                        WurstClient.INSTANCE.getCmdProcessor()
                                .chat("No keybinds set. Use .bind <hack> <key>.");
        }
}
