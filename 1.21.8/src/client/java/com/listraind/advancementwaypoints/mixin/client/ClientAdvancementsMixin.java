package com.listraind.advancementwaypoints.mixin.client;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.advancement.AdvancementInjector;
import com.listraind.advancementwaypoints.advancement.WaypointData;
import com.listraind.advancementwaypoints.api.IAdvancementInjector;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementsScreen;
import com.listraind.advancementwaypoints.config.ConfigIO;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(ClientAdvancements.class)
public abstract class ClientAdvancementsMixin implements IAdvancementInjector {

    @Unique private final Set<ResourceLocation> injected = new HashSet<>();
    @Unique private final Map<ResourceLocation, float[]> vanillaOriginals = new HashMap<>();
    @Unique private long lastInjectMs = 0L;
    @Unique private static final long INJECT_DEBOUNCE_MS = 250L;

    @Final @Shadow private AdvancementTree tree;
    @Final @Shadow private Map<AdvancementHolder, AdvancementProgress> progress;
    @Shadow private ClientAdvancements.Listener listener;

    @Inject(method = "update", at = @At("RETURN"))
    private void onUpdate(ClientboundUpdateAdvancementsPacket pkt, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        if (now - lastInjectMs < INJECT_DEBOUNCE_MS) return;
        lastInjectMs = now;
        advWaypoint_inject();
    }

    @Override
    public void advWaypoint_inject() {
        try {
            if (!injected.isEmpty()) {
                Set<ResourceLocation> present = new HashSet<>();
                for (ResourceLocation id : injected) {
                    AdvancementNode n = tree.get(id);
                    if (n != null) {
                        progress.remove(n.holder());
                        present.add(id);
                    }
                }
                if (!present.isEmpty()) tree.remove(present);
                injected.clear();
            }

            for (var e : vanillaOriginals.entrySet()) {
                AdvancementNode n = tree.get(e.getKey());
                if (n != null) n.holder().value().display().ifPresent(d -> {
                    float[] o = e.getValue();
                    d.setLocation(o[0], o[1]);
                });
            }
            vanillaOriginals.clear();

            applyOverrides();

            AdvancementInjector.LoadResult result = AdvancementInjector.load(tree);
            if (result.advancements().isEmpty()) { refreshUI(); return; }

            List<AdvancementHolder> holders = new ArrayList<>();
            for (WaypointData w : result.advancements()) {
                ResourceLocation id = w.resourceLocation();
                Optional<ResourceLocation> bg = w.background() != null && !w.background().isEmpty()
                        ? Optional.of(ResourceLocation.parse(w.background())) : Optional.empty();

                DisplayInfo display = new DisplayInfo(
                        w.itemStack(), Component.literal(w.title()), Component.literal(w.description()),
                        bg.map(ClientAsset::new), w.frameType(), true, true, false
                );
                display.setLocation(w.x(), w.y());

                Map<String, Criterion<?>> criteria = Map.of(
                        "auto", new Criterion<>(new ImpossibleTrigger(), new ImpossibleTrigger.TriggerInstance())
                );
                Optional<ResourceLocation> parent = w.parent() != null && !w.parent().isEmpty()
                        ? Optional.of(ResourceLocation.parse(w.parent())) : Optional.empty();

                holders.add(new AdvancementHolder(id, new Advancement(
                        parent, Optional.of(display), AdvancementRewards.EMPTY,
                        criteria, new AdvancementRequirements(List.of(List.of("auto"))),
                        false, Optional.empty()
                )));
                injected.add(id);
            }

            tree.addAll(holders);

            for (AdvancementHolder h : holders) {
                AdvancementNode n = tree.get(h.id());
                if (n != null) {
                    AdvancementProgress p = new AdvancementProgress();
                    p.update(n.advancement().requirements());
                    p.grantProgress("auto");
                    progress.put(n.holder(), p);
                }
            }

            for (var e : result.vanillaOverrides().entrySet()) {
                ResourceLocation vid = ResourceLocation.parse(e.getKey());
                AdvancementNode n = tree.get(vid);
                if (n != null) n.holder().value().display().ifPresent(d -> {
                    vanillaOriginals.put(vid, new float[]{d.getX(), d.getY()});
                    d.setLocation(e.getValue()[0], e.getValue()[1]);
                });
            }

            refreshUI();
        } catch (Exception e) {
            com.listraind.advancementwaypoints.AdvancementWaypoints.LOGGER.error("advWaypoint_inject failed", e);
        }
    }

    @Unique
    private void applyOverrides() {
        for (JsonObject o : WaypointStorage.loadOverrides()) {
            String idStr = ConfigIO.str(o, "id", "");
            if (idStr.isEmpty()) continue;
            AdvancementNode n = tree.get(ResourceLocation.parse(idStr));
            if (n == null) continue;

            n.holder().value().display().ifPresent(d -> {
                String title = ConfigIO.nullable(o, "title");
                if (title != null) ((DisplayInfoAccessor) d).advWp_setTitle(Component.literal(title));
                String desc = ConfigIO.nullable(o, "description");
                if (desc != null) ((DisplayInfoAccessor) d).advWp_setDescription(Component.literal(desc));
                String icon = ConfigIO.nullable(o, "icon");
                if (icon != null) {
                    try {
                        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(icon));
                        item.ifPresent(i -> ((DisplayInfoAccessor) d).advWp_setIcon(new ItemStack(i)));
                    } catch (Exception e) {
                        ((DisplayInfoAccessor) d).advWp_setIcon(new ItemStack(Items.PAPER));
                    }
                }
            });
        }
    }

    @Unique
    private void refreshUI() {
        if (listener == null) return;

        for (AdvancementNode n : tree.nodes()) {
            AdvancementProgress p = progress.get(n.holder());
            if (p == null) {
                p = new AdvancementProgress();
                p.update(n.advancement().requirements());
            }
            listener.onUpdateAdvancementProgress(n, p);
        }

        if (listener instanceof IBetterAdvancementsScreen screen) {
            screen.advWp_recalculateAll();
        }
    }

}