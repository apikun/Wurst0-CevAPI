/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.commands;

import java.util.Arrays;
import java.util.List;

import net.wurst0.command.CmdException;
import net.wurst0.feature.Command;
import net.wurst0.feature.Feature;

public final class CmdList
{
        public final HelpCmd help = new HelpCmd();
        public final TCmd t = new TCmd();
        public final BindCmd bind = new BindCmd();
        public final BindsCmd binds = new BindsCmd();
        public final SayCmd say = new SayCmd();
        public final VClipCmd vclip = new VClipCmd();
        public final JumpCmd jump = new JumpCmd();
        public final GmCmd gm = new GmCmd();
        public final SetModeCmd setmode = new SetModeCmd();
        public final SetSliderCmd setslider = new SetSliderCmd();
        public final SetCheckboxCmd setcheckbox = new SetCheckboxCmd();
        public final FeaturesCmd features = new FeaturesCmd();

        private final List<Command> cmds =
                Arrays.asList(help, t, bind, binds, say, vclip, jump, gm,
                        setmode, setslider, setcheckbox, features);

        public List<Command> getList()
        {
                return cmds;
        }

        public Command getByName(String name)
        {
                for(Command cmd : cmds)
                {
                        if(cmd.getName().equalsIgnoreCase(name))
                                return cmd;
                        for(String alias : getAliases(cmd))
                                if(alias.equalsIgnoreCase(name))
                                        return cmd;
                }
                return null;
        }

        private static String[] getAliases(Feature cmd)
        {
                if(cmd instanceof TCmd)
                        return new String[]{"toggle"};
                if(cmd instanceof BindCmd)
                        return new String[]{"b"};
                return new String[0];
        }
}
