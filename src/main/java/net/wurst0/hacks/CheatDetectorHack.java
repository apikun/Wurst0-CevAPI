/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI CheatDetectorHack (GPL-3.0-or-later)
 * Lite adaptation: heuristic motion analysis (speed / fly / step).
 */
package net.wurst0.hacks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class CheatDetectorHack extends Hack
        implements UpdateListener, RenderListener
{
        private static final class Flags
        {
                int speedTicks;
                int flyTicks;
                long lastReport;
                boolean flagged;
        }

        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting speedThreshold =
                new SliderSetting("Speed threshold", 0.62, 0.4, 2, 0.02);
        private final SliderSetting reportCooldown =
                new SliderSetting("Report cooldown (s)", 30, 5, 120, 5);
        private final CheckboxSetting markRender =
                new CheckboxSetting("Mark in world", true);

        private final Map<UUID, Flags> tracked =
                new HashMap<UUID, Flags>();
        private final Map<UUID, Entity> flaggedEntities =
                new HashMap<UUID, Entity>();

        public CheatDetectorHack()
        {
                super("CheatDetector",
                        "Heuristically flags other players moving too fast"
                                + " or flying (client-side analysis only).");
                setCategory(Category.INTEL);
                addSettings(speedThreshold, reportCooldown,
                        markRender);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                flaggedEntities.clear();
                double threshold = speedThreshold.getValue();
                long now = System.currentTimeMillis();
                long cooldownMs = reportCooldown.getValueI() * 1000L;

                Iterator<Entity> itr = mc.world.loadedEntityList.iterator();
                while(itr.hasNext())
                {
                        Entity e = itr.next();
                        if(!(e instanceof EntityPlayer) || e == p
                                || e.isDead)
                                continue;

                        EntityPlayer other = (EntityPlayer)e;
                        Flags flags = tracked.get(other.getUniqueID());
                        if(flags == null)
                        {
                                flags = new Flags();
                                tracked.put(other.getUniqueID(), flags);
                        }

                        double hspeed = Math.sqrt(
                                other.motionX * other.motionX
                                        + other.motionZ * other.motionZ);
                        boolean rising = other.motionY > 0.15
                                && !other.onGround
                                && other.posY > other.lastTickPosY + 0.1
                                && hspeed < 0.3;

                        flags.speedTicks =
                                hspeed > threshold ? flags.speedTicks + 1 : 0;
                        flags.flyTicks = rising ? flags.flyTicks + 1 : 0;

                        String verdict = null;
                        if(flags.speedTicks >= 15)
                                verdict = "speed";
                        else if(flags.flyTicks >= 25)
                                verdict = "fly";

                        if(verdict != null && now - flags.lastReport
                                > cooldownMs)
                        {
                                flags.lastReport = now;
                                p.sendMessage(new TextComponentString(
                                        "\u00a7e[CheatDetector] "
                                                + other.getName()
                                                + " suspected: " + verdict));
                        }

                        if(flags.speedTicks >= 15 || flags.flyTicks >= 25)
                        {
                                flags.flagged = true;
                                flaggedEntities.put(other.getUniqueID(), other);
                        }
                        else
                                flags.flagged = false;
                }

                if(tracked.size() > 200)
                        tracked.clear();
        }

        @Override
        public void onRender(float partialTicks)
        {
                if(!markRender.isChecked() || flaggedEntities.isEmpty()
                        || mc.player == null)
                        return;

                RenderUtils.enable3D(2F, false);
                RenderUtils.applyCameraTransform(partialTicks);
                for(Entity e : flaggedEntities.values())
                {
                        if(e.isDead)
                                continue;
                        RenderUtils.drawBox(
                                e.getEntityBoundingBox().grow(0.12),
                                1F, 0F, 0F, 0.7F);
                }
                RenderUtils.disable3D(false);
        }

        @Override
        protected void onEnable()
        {
                tracked.clear();
                flaggedEntities.clear();
        }

        @Override
        protected void onDisable()
        {
                tracked.clear();
                flaggedEntities.clear();
        }
}
