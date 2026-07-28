package com.listraind.advancementwaypoints.mixin.client;

import com.listraind.advancementwaypoints.api.IAdvancementInjector;
import com.listraind.advancementwaypoints.handler.AdvancementTreeManager;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ClientAdvancements.class)
public abstract class ClientAdvancementsMixin implements IAdvancementInjector {

    @Final @Shadow private AdvancementTree tree;
    @Final @Shadow private Map<AdvancementHolder, AdvancementProgress> progress;
    @Shadow private ClientAdvancements.Listener listener;

    @Unique private static final long INJECT_DEBOUNCE_MS = 250L;
    @Unique private long lastInjectMs = 0L;
    @Unique private AdvancementTreeManager advWp_treeManager;

    @Inject(method = "update", at = @At("RETURN"))
    private void onUpdate(ClientboundUpdateAdvancementsPacket pkt, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        if (now - lastInjectMs < INJECT_DEBOUNCE_MS) return;
        lastInjectMs = now;
        advWaypoint_inject();
    }

    @Override
    public void advWaypoint_inject() {
        if (advWp_treeManager == null || !advWp_treeManager.isCompatible(tree, progress, listener)) {
            advWp_treeManager = new AdvancementTreeManager(tree, progress, listener);
        }
        advWp_treeManager.inject();
    }
}
