/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SoundMuteHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.wurst0.Category;
import net.wurst0.feature.Hack;
import net.wurst0.settings.Setting;
import net.wurst0.settings.TextFieldSetting;

/**
 * Mutes sounds whose name contains one of your comma-separated
 * patterns (e.g. "note,step,cow"). The original uses a sound picker;
 * on 1.12.2 we match on the sound event name instead.
 */
public final class SoundMuteHack extends Hack
{
        private final TextFieldSetting mutedSounds =
                new TextFieldSetting("Muted sounds", "", 256);

        public SoundMuteHack()
        {
                super("SoundMute",
                        "Mutes selected sounds. Comma-separated name patterns,"
                                + " e.g. \"note,step,cow\".");
                setCategory(Category.RENDER);
                addSetting(mutedSounds);
        }

        /**
         * Called by ForgeEvents from PlaySoundEvent.
         *
         * @return true if this sound should be muted
         */
        public boolean shouldMute(String soundName)
        {
                String raw = mutedSounds.getValue();
                if(raw == null || raw.trim().isEmpty())
                        return false;
                String lower = soundName == null ? "" : soundName.toLowerCase();
                for(String p : raw.split(","))
                {
                        String pat = p.trim().toLowerCase();
                        if(!pat.isEmpty() && lower.contains(pat))
                                return true;
                }
                return false;
        }
}
