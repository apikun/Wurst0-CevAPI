/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI MiningEvidenceHack (GPL-3.0-or-later)
 * Lite adaptation: no BlockBreakingProgressListener exists in this
 * fork, so evidence is built purely from dropped-item trails (the
 * original's core signal); lines/markers are drawn with the fork's
 * simple RenderUtils primitives.
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Detects trails of dropped items underground and renders them as
 * "mining evidence" (somebody has been mining here).
 */
public final class MiningEvidenceHack extends Hack
        implements UpdateListener, RenderListener
{
        private static final class Drop
        {
                final Vec3d pos;
                final String item;
                final long at;

                Drop(Vec3d pos, String item, long at)
                {
                        this.pos = pos;
                        this.item = item;
                        this.at = at;
                }
        }

        private static final class Trail
        {
                final List<Drop> points = new ArrayList<Drop>();
        }

        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting scanDistance =
                new SliderSetting("Scan distance", 48, 16, 96, 8);
        private final SliderSetting joinDistance =
                new SliderSetting("Join distance", 6, 2, 24, 1);
        private final SliderSetting minDrops =
                new SliderSetting("Min drops in trail", 3, 2, 12, 1);
        private final SliderSetting lifetimeMinutes =
                new SliderSetting("Evidence lifetime (min)", 30, 1, 120, 1);
        private final CheckboxSetting showLines =
                new CheckboxSetting("Show lines", true);
        private final CheckboxSetting showMarkers =
                new CheckboxSetting("Show markers", true);
        private final CheckboxSetting showTracers =
                new CheckboxSetting("Show tracers", false);

        private final List<Drop> drops = new ArrayList<Drop>();
        private final List<Trail> trails = new ArrayList<Trail>();
        private int scanTimer;

        public MiningEvidenceHack()
        {
                super("MiningEvidence",
                        "Finds trails of dropped items that mark other"
                                + " players' mining tunnels.");
                setCategory(Category.INTEL);
                addSettings(scanDistance, joinDistance, minDrops,
                        lifetimeMinutes, showLines, showMarkers,
                        showTracers);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                {
                        drops.clear();
                        trails.clear();
                        return;
                }

                long now = System.currentTimeMillis();
                long maxAge = lifetimeMinutes.getValueI() * 60000L;

                // forget old evidence
                Iterator<Drop> dropItr = drops.iterator();
                while(dropItr.hasNext())
                        if(now - dropItr.next().at > maxAge)
                                dropItr.remove();

                if(--scanTimer <= 0)
                {
                        scanTimer = 20;
                        collectDrops(p, now);
                        buildTrails();
                }
        }

        private void collectDrops(EntityPlayerSP p, long now)
        {
                double rangeSq = scanDistance.getValue()
                        * scanDistance.getValue();

                for(net.minecraft.entity.Entity e :
                        mc.world.loadedEntityList)
                {
                        if(!(e instanceof EntityItem) || e.isDead)
                                continue;

                        if(p.getDistanceSq(e.posX, e.posY, e.posZ) > rangeSq)
                                continue;

                        ItemStack stack =
                                ((EntityItem)e).getItem();
                        if(stack == null || stack.isEmpty())
                                continue;

                        Vec3d pos = new Vec3d(e.posX, e.posY, e.posZ);

                        // dedupe by proximity + item (items linger a while)
                        boolean known = false;
                        for(Drop drop : drops)
                        {
                                if(drop.item.equals(stack.getDisplayName())
                                        && drop.pos.squareDistanceTo(pos) < 1.0)
                                {
                                        known = true;
                                        break;
                                }
                        }
                        if(known)
                                continue;

                        drops.add(new Drop(pos,
                                stack.getDisplayName(), now));
                }

                while(drops.size() > 600)
                        drops.remove(0);
        }

        private void buildTrails()
        {
                trails.clear();

                // group drops by item, then chain them greedily
                Map<String, List<Drop>> byItem =
                        new LinkedHashMap<String, List<Drop>>();
                for(Drop drop : drops)
                {
                        List<Drop> list = byItem.get(drop.item);
                        if(list == null)
                        {
                                list = new ArrayList<Drop>();
                                byItem.put(drop.item, list);
                        }
                        list.add(drop);
                }

                double join = joinDistance.getValue();
                double joinSq = join * join;
                int min = minDrops.getValueI();

                for(List<Drop> list : byItem.values())
                {
                        List<Drop> remaining =
                                new ArrayList<Drop>(list);
                        while(!remaining.isEmpty())
                        {
                                Trail trail = new Trail();
                                Drop current = remaining.remove(0);
                                trail.points.add(current);

                                boolean extended = true;
                                while(extended)
                                {
                                        extended = false;
                                        Drop best = null;
                                        int bestIndex = -1;
                                        double bestDistSq = joinSq;

                                        for(int i = 0; i < remaining
                                                .size(); i++)
                                        {
                                                Drop candidate = remaining
                                                        .get(i);
                                                double distSq = current.pos
                                                        .squareDistanceTo(
                                                                candidate.pos);
                                                if(distSq <= bestDistSq)
                                                {
                                                        bestDistSq = distSq;
                                                        best = candidate;
                                                        bestIndex = i;
                                                }
                                        }

                                        if(best != null)
                                        {
                                                remaining.remove(bestIndex);
                                                trail.points.add(best);
                                                current = best;
                                                extended = true;
                                        }
                                }

                                if(trail.points.size() >= min)
                                        trails.add(trail);
                                if(trails.size() >= 64)
                                        return;
                        }
                }
        }

        @Override
        public void onRender(float partialTicks)
        {
                if(trails.isEmpty() || mc.player == null)
                        return;

                RenderUtils.enable3D(2F, false);
                RenderUtils.applyCameraTransform(partialTicks);

                for(Trail trail : trails)
                {
                        if(showLines.isChecked())
                        {
                                for(int i = 1; i < trail.points.size(); i++)
                                {
                                        Vec3d a = trail.points.get(i - 1).pos;
                                        Vec3d b = trail.points.get(i).pos;
                                        RenderUtils.drawLine(a.x, a.y, a.z,
                                                b.x, b.y, b.z,
                                                1F, 0.78F, 0.25F, 0.8F);
                                }
                        }

                        if(showMarkers.isChecked())
                        {
                                for(Drop drop : trail.points)
                                {
                                        BlockPos block =
                                                new BlockPos(drop.pos);
                                        RenderUtils.drawBox(
                                                new AxisAlignedBB(block)
                                                        .grow(0.05),
                                                1F, 0.86F, 0.47F, 0.5F);
                                }
                        }

                        if(showTracers.isChecked())
                        {
                                Vec3d end = trail.points
                                        .get(trail.points.size() - 1).pos;
                                EntityPlayerSP p = mc.player;
                                RenderUtils.drawLine(
                                        p.posX, p.posY + p.getEyeHeight(),
                                        p.posZ, end.x, end.y, end.z,
                                        1F, 0.25F, 0.25F, 0.8F);
                        }
                }

                RenderUtils.disable3D(false);
        }

        @Override
        protected void onEnable()
        {
                scanTimer = 0;
                drops.clear();
                trails.clear();
        }

        @Override
        protected void onDisable()
        {
                drops.clear();
                trails.clear();
        }
}
