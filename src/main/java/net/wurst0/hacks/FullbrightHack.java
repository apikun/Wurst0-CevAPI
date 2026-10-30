/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FullbrightHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;

public final class FullbrightHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final ModeSetting method = new ModeSetting("Method",
                new String[]{"Gamma", "NightVision"}, 0);
        private final SliderSetting gamma =
                new SliderSetting("Gamma", 8, 1, 15, 0.5);

        private float oldGamma = 1F;

        public FullbrightHack()
        {
                super("Fullbright",
                        "Makes everything visible in the dark. Gamma raises the"
                                + " brightness slider beyond its limit, NightVision applies"
                                + " a local night vision effect.");
                setCategory(Category.RENDER);
                addSettings(method, gamma);
        }

        @Override
        protected void onEnable()
        {
                oldGamma = mc.gameSettings.gammaSetting;
        }

        @Override
        public void onUpdate()
        {
                if(method.getSelected().equals("Gamma"))
                {
                        mc.gameSettings.gammaSetting = gamma.getValueF();
                        return;
                }

                EntityPlayerSP p = mc.player;
                if(p != null && !p.isPotionActive(MobEffects.NIGHT_VISION))
                        p.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION,
                                36000, 0, false, false));
        }

        @Override
        protected void onDisable()
        {
                if(method.getSelected().equals("Gamma"))
                        mc.gameSettings.gammaSetting = oldGamma;
                else if(mc.player != null)
                        mc.player.removePotionEffect(MobEffects.NIGHT_VISION);
        }
}
