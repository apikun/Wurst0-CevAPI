/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * TeamUtils: shared "don't hit my teammates" logic for combat hacks.
 *
 * Two-layer detection:
 *  1. Scoreboard teams - Hypixel Bedwars (and most minigame servers) put
 *     teammates in the same scoreboard Team, so nametags share a color.
 *     If both players have a team, only the exact same team is friendly.
 *  2. Colored-nametag fallback - servers without teams usually still color
 *     team nametags. Compare the first color code of both display names.
 *     Default white/gray is ignored so uncolored names never count as
 *     teammates.
 */
package net.wurst0.util;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;

public final class TeamUtils
{
        private TeamUtils()
        {}

        /**
         * True if the target is on the player's team (and must not be
         * attacked). Only meaningful for EntityPlayer targets.
         */
        public static boolean isTeammate(EntityPlayer self,
                EntityLivingBase target)
        {
                if(!(target instanceof EntityPlayer))
                        return false;

                EntityPlayer other = (EntityPlayer)target;
                Team mine = self.getTeam();
                Team theirs = other.world.getScoreboard()
                        .getPlayersTeam(other.getName());
                if(mine != null || theirs != null)
                        return mine != null && theirs != null
                                && mine.equals(theirs);

                String myColor = nameColor(self);
                return !myColor.isEmpty()
                        && myColor.equals(nameColor(other));
        }

        /** First real color code of a player's display name, "" if none. */
        public static String nameColor(EntityPlayer pl)
        {
                if(pl == null)
                        return "";
                String s = pl.getDisplayName().getFormattedText();
                int i = s.indexOf('\u00a7');
                if(i >= 0 && i + 1 < s.length())
                {
                        char c = Character.toLowerCase(s.charAt(i + 1));
                        boolean isColor = (c >= '0' && c <= '9')
                                || (c >= 'a' && c <= 'f');
                        if(isColor && c != 'f' && c != '7')
                                return String.valueOf(c);
                }
                return "";
        }
}
