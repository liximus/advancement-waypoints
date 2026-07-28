package com.listraind.advancementwaypoints.mixin.client;

import com.listraind.advancementwaypoints.ChatScanner;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({ChatComponent.class})
public class ChatComponentMixin {
   @ModifyVariable(
      method = {"addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private Component onAddMessage(Component message) {
      return ChatScanner.scanAndHighlight(message);
   }

   @ModifyVariable(
      method = {"addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private Component onAddPlayerMessage(Component message) {
      return ChatScanner.scanAndHighlight(message);
   }

   @ModifyVariable(
      method = {"addClientSystemMessage(Lnet/minecraft/network/chat/Component;)V"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private Component onAddClientSystemMessage(Component message) {
      return ChatScanner.scanAndHighlight(message);
   }

   @ModifyVariable(
      method = {"addServerSystemMessage(Lnet/minecraft/network/chat/Component;)V"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private Component onAddServerSystemMessage(Component message) {
      return ChatScanner.scanAndHighlight(message);
   }
}
