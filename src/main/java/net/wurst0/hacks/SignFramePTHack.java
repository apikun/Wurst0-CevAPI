/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SignFramePTHack (GPL-3.0-or-later)
 * Lite adaptation: no RightClickEvent exists in this fork, so the
 * pass-through is driven by the use key (press + hold) and mirrors
 * vanilla's right-click cooldown via reflection.
 */
package net.wurst0.hacks;

import net.minecraft.block.BlockSign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.KeyPressListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * Forwards right-clicks through item frames and signs to the block
 * behind them (e.g. opens a chest covered by a frame or wall sign).
 * Sneak to use the frame/sign normally.
 */
public final class SignFramePTHack extends Hack
        implements UpdateListener, KeyPressListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting framesEnabled =
                new CheckboxSetting("Frames", true);
        private final CheckboxSetting signsEnabled =
                new CheckboxSetting("Signs", true);

        public SignFramePTHack()
        {
                super("SignFramePT",
                        "Right-clicks pass through item frames and signs"
                                + " to the block behind them. Sneak for"
                                + " the normal interaction.");
                setCategory(Category.ITEMS);
                addSettings(framesEnabled, signsEnabled);
        }

        @Override
        public void onKeyPress(int keyCode, boolean state)
        {
                if(!state || mc.player == null)
                        return;
                if(keyCode != mc.gameSettings.keyBindUseItem.getKeyCode())
                        return;

                // suppress the vanilla click this tick so the block behind
                // gets our packet first (vanilla would rotate the frame)
                if(isAimingAtCover())
                        setRightClickDelay(4);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null
                        || mc.playerController == null)
                        return;

                if(!mc.gameSettings.keyBindUseItem.isKeyDown())
                        return;
                if(p.isSneaking())
                        return;
                if(getRightClickDelay() > 0)
                        return;

                if(!isAimingAtCover())
                        return;

                // keep vanilla suppressed and click the block behind with
                // vanilla's own 4-tick repeat rhythm
                if(clickBlockBehind(p))
                        setRightClickDelay(4);
        }

        private boolean isAimingAtCover()
        {
                RayTraceResult hit = mc.objectMouseOver;
                if(hit == null)
                        return false;

                if(hit.typeOfHit == RayTraceResult.Type.ENTITY
                        && framesEnabled.isChecked())
                {
                        // only filled frames forward (empty ones are
                        // interacted with normally)
                        if(!(hit.entityHit instanceof EntityItemFrame))
                                return false;
                        EntityItemFrame frame =
                                (EntityItemFrame)hit.entityHit;
                        return !frame.getDisplayedItem().isEmpty();
                }

                if(hit.typeOfHit == RayTraceResult.Type.BLOCK
                        && signsEnabled.isChecked())
                {
                        BlockPos pos = hit.getBlockPos();
                        if(pos == null)
                                return false;
                        return mc.world.getBlockState(pos)
                                .getBlock() instanceof BlockSign;
                }
                return false;
        }

        private boolean clickBlockBehind(EntityPlayerSP p)
        {
                RayTraceResult cover = mc.objectMouseOver;
                if(cover == null)
                        return false;

                Vec3d coverPos;
                BlockPos coverBlock = null;
                if(cover.typeOfHit == RayTraceResult.Type.ENTITY)
                {
                        EntityItemFrame frame =
                                (EntityItemFrame)cover.entityHit;
                        coverPos = frame.getPositionVector();
                }else
                {
                        coverBlock = cover.getBlockPos();
                        coverPos = new Vec3d(
                                coverBlock.getX() + 0.5,
                                coverBlock.getY() + 0.5,
                                coverBlock.getZ() + 0.5);
                }

                Vec3d eyes = new Vec3d(p.posX,
                        p.posY + p.getEyeHeight(), p.posZ);
                Vec3d look = p.getLookVec();
                double dist = eyes.distanceTo(coverPos);

                // start slightly past the cover so the ray skips it
                for(double offset = 0.1; offset <= 0.8; offset += 0.3)
                {
                        Vec3d start = eyes.add(
                                look.scale(dist + offset));
                        Vec3d end = start.add(look.scale(2.0));
                        RayTraceResult behind = mc.world.rayTraceBlocks(
                                start, end, false, true, false);
                        if(behind == null || behind.typeOfHit
                                != RayTraceResult.Type.BLOCK)
                                continue;

                        BlockPos pos = behind.getBlockPos();
                        if(pos == null || pos.equals(coverBlock))
                                continue;
                        if(mc.world.isAirBlock(pos))
                                continue;

                        mc.playerController.processRightClickBlock(p,
                                mc.world, pos, behind.sideHit,
                                Vec3d.ZERO, EnumHand.MAIN_HAND);
                        p.swingArm(EnumHand.MAIN_HAND);
                        return true;
                }

                // nothing interactable behind the cover (e.g. wall)
                return false;
        }

        private int getRightClickDelay()
        {
                try
                {
                        Integer delay = ReflectionHelper.getPrivateValue(
                                Minecraft.class, mc,
                                "rightClickDelayTimer",
                                "field_71467_ac");
                        return delay == null ? 0 : delay.intValue();
                }catch(Throwable t)
                {
                        return 0;
                }
        }

        private void setRightClickDelay(int value)
        {
                try
                {
                        ReflectionHelper.setPrivateValue(Minecraft.class,
                                mc, value, "rightClickDelayTimer",
                                "field_71467_ac");
                }catch(Throwable t)
                {
                        // without the field the vanilla click still runs
                }
        }

        @Override
        protected void onEnable()
        {
        }

        @Override
        protected void onDisable()
        {
        }
}
