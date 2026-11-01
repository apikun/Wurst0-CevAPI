/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FakeLagHack (GPL-3.0-or-later)
 *
 * 1.12.2 adaptation: holds ALL outgoing packets through PacketTap
 * (the original filters individual movement packets) while enabled,
 * optionally only near enemies, and auto-flushes when the max hold
 * time expires. The double flush below works around PacketTap's
 * flush-while-holding race: the second flushOutbound() releases any
 * packets that were re-captured before the hold flag dropped.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.wurst0.Category;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.PacketTap;

public final class FakeLagHack extends Hack
        implements UpdateListener, ScreenRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range = new SliderSetting("Range", 4, 1,
                10, 0.25);
        private final SliderSetting maxHold =
                new SliderSetting("Max hold time (ms)", 300, 0, 2000, 25);
        private final CheckboxSetting onlyNearEnemies =
                new CheckboxSetting("Only near enemies", true);
        private final CheckboxSetting showHud =
                new CheckboxSetting("Show HUD", true);

        private long holdStart;
        private boolean holding;

        public FakeLagHack()
        {
                super("FakeLag",
                        "Holds back outgoing packets for short bursts,"
                                + " especially near enemies, making you"
                                + " harder to hit (combat lag switch).");
                setCategory(Category.COMBAT);
                addSettings(range, maxHold, onlyNearEnemies, showHud);
        }

        private boolean shouldHold()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null || mc.getConnection() == null)
                        return false;
                if(onlyNearEnemies.isChecked() && !isEnemyNearby(p))
                        return false;
                return true;
        }

        private boolean isEnemyNearby(EntityPlayerSP self)
        {
                double rangeSq = range.getValue() * range.getValue();
                for(int i = 0; i < mc.world.playerEntities.size(); i++)
                {
                        EntityPlayer e = mc.world.playerEntities.get(i);
                        if(e == null || e == self || e.isSpectator()
                                || !e.isEntityAlive())
                                continue;
                        double dx = e.posX - self.posX;
                        double dy = e.posY - self.posY;
                        double dz = e.posZ - self.posZ;
                        if(dx * dx + dy * dy + dz * dz <= rangeSq)
                                return true;
                }
                return false;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null || mc.getConnection() == null)
                {
                        if(holding)
                                releaseHold();
                        return;
                }

                PacketTap.ensureInstalled();

                boolean want = shouldHold();
                if(want && !holding)
                {
                        holding = true;
                        holdStart = System.currentTimeMillis();
                        PacketTap.setHoldOutbound(true);
                }else if(!want && holding)
                {
                        releaseHold();
                }

                if(holding && maxHold.getValueI() > 0 && System
                        .currentTimeMillis() - holdStart >= maxHold.getValueI())
                {
                        // auto-flush on expiry, then keep holding
                        releaseHold();
                        if(shouldHold())
                        {
                                holding = true;
                                holdStart = System.currentTimeMillis();
                                PacketTap.setHoldOutbound(true);
                        }
                }
        }

        private void releaseHold()
        {
                holding = false;
                PacketTap.setHoldOutbound(false); // flushes, stops holding
                PacketTap.flushOutbound(); // releases race re-captures
        }

        @Override
        protected void onEnable()
        {
                holdStart = System.currentTimeMillis();
                PacketTap.ensureInstalled();
        }

        @Override
        protected void onDisable()
        {
                releaseHold();
        }

        @Override
        public void onScreenRender(float partialTicks)
        {
                if(!showHud.isChecked() || mc.player == null
                        || mc.getConnection() == null)
                        return;

                int held = PacketTap.heldCount();
                if(held == 0)
                        return;

                String text = "FakeLag: " + held + " packets held";
                ScaledResolution sr = new ScaledResolution(mc);
                int x = sr.getScaledWidth() / 2
                        - mc.fontRenderer.getStringWidth(text) / 2;
                mc.fontRenderer.drawStringWithShadow(text, x,
                        sr.getScaledHeight() / 2 + 32, 0xFFD080);
        }
}
