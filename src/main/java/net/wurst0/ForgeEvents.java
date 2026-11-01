/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0;
import org.lwjgl.input.Keyboard;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.events.ChatOutputEvent;
import net.wurst0.event.events.ChunkLoadEvent;
import net.wurst0.event.events.DamageEvent;
import net.wurst0.event.events.FogEvent;
import net.wurst0.event.events.HandRenderEvent;
import net.wurst0.event.events.KeyEvent;
import net.wurst0.event.events.OverlayEvent;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.events.UpdateEvent;
import net.wurst0.feature.Hack;
public final class ForgeEvents
{
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event)
        {
                if(event.phase != TickEvent.Phase.END)
                        return;
                WurstClient.INSTANCE.getEventManager().fire(new UpdateEvent());
                // PacketTap: releases delayed outbound packets
                net.wurst0.util.PacketTap.tick();
                // save config at most once per tick when something changed
                try
                {
                        WurstClient wurst = WurstClient.INSTANCE;
                        if(wurst.getFileManager() != null
                                && wurst.getFileManager().isDirty())
                        {
                                wurst.getFileManager().save(wurst.getHacks());
                                wurst.getFileManager().saveKeybinds(wurst.getHacks());
                                wurst.getFileManager().clearDirty();
                        }
                }catch(Throwable t)
                {
                        t.printStackTrace();
                }
        }
        @SubscribeEvent
        public void onRenderWorldLast(RenderWorldLastEvent event)
        {
                WurstClient.INSTANCE.getEventManager()
                        .fire(new RenderEvent(event.getPartialTicks()));
        }
        @SubscribeEvent
        public void onKeyInput(KeyInputEvent event)
        {
                // 1.12.2's KeyInputEvent carries no key data; read the current
                // event from LWJGL while vanilla's keyboard loop is dispatching it.
                int keyCode = Keyboard.getEventKey();
                boolean state = Keyboard.getEventKeyState();
                if(keyCode == Keyboard.KEY_NONE || !state)
                        return;
                WurstClient.INSTANCE.getKeybindManager().handleKeyPress(keyCode);
                WurstClient.INSTANCE.getEventManager()
                        .fire(new KeyEvent(keyCode, true));
        }
        @SubscribeEvent
        public void onRenderOverlay(RenderGameOverlayEvent.Post event)
        {
                if(event.getType() != RenderGameOverlayEvent.ElementType.ALL)
                        return;
                WurstClient.INSTANCE.getHud().render(event.getPartialTicks());
                WurstClient.INSTANCE.getEventManager()
                        .fire(new ScreenRenderEvent(event.getPartialTicks()));
        }
        @SubscribeEvent
        public void onChatReceived(ClientChatReceivedEvent event)
        {
                if(mc.player == null)
                        return;
                ChatInputEvent wurstEvent = new ChatInputEvent(
                        event.getMessage().getFormattedText());
                WurstClient.INSTANCE.getEventManager().fire(wurstEvent);
                if(wurstEvent.isCancelled())
                        event.setCanceled(true);
                else if(!wurstEvent.getText()
                        .equals(event.getMessage().getFormattedText()))
                        event.setMessage(
                                (ITextComponent)new TextComponentString(
                                        wurstEvent.getText()));
        }
        @SubscribeEvent
        public void onChatSent(ClientChatEvent event)
        {
                ChatOutputEvent wurstEvent =
                        new ChatOutputEvent(event.getMessage());
                WurstClient.INSTANCE.getEventManager().fire(wurstEvent);
                if(wurstEvent.isCancelled())
                        event.setCanceled(true);
                else if(!wurstEvent.getText().equals(event.getMessage()))
                        event.setMessage(wurstEvent.getText());
        }
        @SubscribeEvent
        public void onLivingHurt(LivingHurtEvent event)
        {
                if(mc.player == null || event.getEntity() != mc.player)
                        return;
                DamageEvent wurstEvent = new DamageEvent(event.getSource(),
                        event.getAmount());
                WurstClient.INSTANCE.getEventManager().fire(wurstEvent);
                if(wurstEvent.isCancelled())
                        event.setCanceled(true);
                else if(wurstEvent.getAmount() != event.getAmount())
                        event.setAmount(wurstEvent.getAmount());
        }
        @SubscribeEvent
        public void onChunkLoad(ChunkEvent.Load event)
        {
                if(mc.world == null
                        || event.getChunk().getWorld() != mc.world)
                        return;
                WurstClient.INSTANCE.getEventManager()
                        .fire(new ChunkLoadEvent(event.getChunk().x,
                                event.getChunk().z));
        }
        @SubscribeEvent
        public void onHandRender(RenderSpecificHandEvent event)
        {
                if(mc.player == null)
                        return;
                HandRenderEvent wurstEvent = new HandRenderEvent();
                WurstClient.INSTANCE.getEventManager().fire(wurstEvent);
                if(wurstEvent.isCancelled())
                        event.setCanceled(true);
        }
        @SubscribeEvent
        public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event)
        {
                if(mc.player == null)
                        return;
                OverlayEvent wurstEvent = new OverlayEvent(
                        event.getType().name());
                WurstClient.INSTANCE.getEventManager().fire(wurstEvent);
                if(wurstEvent.isCancelled())
                        event.setCanceled(true);
        }
        @SubscribeEvent
        public void onFogDensity(EntityViewRenderEvent.FogDensity event)
        {
                if(mc.player == null)
                        return;
                FogEvent wurstEvent = new FogEvent();
                WurstClient.INSTANCE.getEventManager().fire(wurstEvent);
                if(wurstEvent.isCancelled())
                {
                        event.setDensity(wurstEvent.getDensity());
                        event.setCanceled(true);
                }
        }
        @SubscribeEvent
        public void onItemToss(ItemTossEvent event)
        {
                if(mc.player == null || event.getPlayer() != mc.player)
                        return;
                // AntiDrop gate: cancelling here stops both the item spawn
                // and the C07 drop packet, because the client call chain
                // (EntityPlayerSP.dropItem -> ForgeHooks.onPlayerTossEvent)
                // returns null when the event is cancelled.
                Hack antiDrop = WurstClient.INSTANCE.getHacks()
                        .getByName("AntiDrop");
                if(antiDrop != null && antiDrop.isEnabled())
                        event.setCanceled(true);
        }
        @SubscribeEvent
        public void onPlaySound(net.minecraftforge.client.event.sound.PlaySoundEvent event)
        {
                if(mc.player == null)
                        return;
                Hack soundMute = WurstClient.INSTANCE.getHacks()
                        .getByName("SoundMute");
                if(soundMute != null && soundMute.isEnabled()
                                && ((net.wurst0.hacks.SoundMuteHack)soundMute)
                                        .shouldMute(event.getSound() == null ? ""
                                        : event.getSound().getSoundLocation().getPath()))
                        event.setResultSound(null);
        }
        @SubscribeEvent
        public void onRightClickEmpty(
                net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickEmpty event)
        {
                if(mc.player == null || event.getEntityPlayer() != mc.player)
                        return;
                Hack liquids = WurstClient.INSTANCE.getHacks()
                        .getByName("Liquids");
                if(liquids != null && liquids.isEnabled())
                        ((net.wurst0.hacks.LiquidsHack)liquids).handleEmptyClick();
        }
        @SubscribeEvent
        public void onExplosionDetonate(ExplosionEvent.Detonate event)
        {
                if(mc.player == null)
                        return;
                Hack antiBlast = WurstClient.INSTANCE.getHacks()
                        .getByName("AntiBlast");
                if(antiBlast != null && antiBlast.isEnabled())
                        ((net.wurst0.hacks.AntiBlastHack)antiBlast)
                                .handleDetonate(event);
        }
        private final net.minecraft.client.Minecraft mc =
                net.minecraft.client.Minecraft.getMinecraft();
}
