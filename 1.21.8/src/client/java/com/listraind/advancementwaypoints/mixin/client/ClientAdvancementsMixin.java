package com.listraind.advancementwaypoints.mixin.client;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.AdvancementWaypointsClient.ParsedAdvancement;
import com.listraind.advancementwaypoints.advancementMixinHelpers.ICustomAdvancementApplier;
import com.listraind.advancementwaypoints.config.AdvancementLoader;
import com.listraind.advancementwaypoints.config.ConfigManager;
import com.listraind.advancementwaypoints.mixin.client.DisplayInfoAccessor;
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
public abstract class ClientAdvancementsMixin implements ICustomAdvancementApplier {

    @Unique private final Set<ResourceLocation> advWp_injected = new HashSet<>();
    @Unique private final Map<ResourceLocation, float[]> advWp_vanillaOriginals = new HashMap<>();

    @Final @Shadow private AdvancementTree tree;
    @Final @Shadow private Map<AdvancementHolder, AdvancementProgress> progress;
    @Shadow private ClientAdvancements.Listener listener;
    @Shadow public abstract void setListener(ClientAdvancements.Listener listener);

    @Inject(method = "update", at = @At("RETURN"))
    private void onUpdate(ClientboundUpdateAdvancementsPacket packet, CallbackInfo ci) {
        this.advWaypoint_injectCustomAdvancements();
    }

    public void advWaypoint_injectCustomAdvancements() {
        try {
            if (!advWp_injected.isEmpty()) {
                Set<ResourceLocation> toRemove = new HashSet<>();
                for (ResourceLocation id : advWp_injected) {
                    AdvancementNode node = this.tree.get(id);
                    if (node != null) {
                        this.progress.remove(node.holder());
                        toRemove.add(id);
                    }
                }
                if (!toRemove.isEmpty()) {
                    this.tree.remove(toRemove);
                }
                advWp_injected.clear();
            }

            for (var entry : advWp_vanillaOriginals.entrySet()) {
                AdvancementNode node = this.tree.get(entry.getKey());
                if (node != null) node.holder().value().display().ifPresent(d -> {
                    float[] o = entry.getValue();
                    d.setLocation(o[0], o[1]);
                });
            }
            advWp_vanillaOriginals.clear();

            applyOverrides();

            AdvancementLoader.LoadResult result = AdvancementLoader.loadAll(this.tree);
            if (result.advancements.isEmpty()) { refreshUI(); return; }

            List<AdvancementHolder> holders = new ArrayList<>();
            for (ParsedAdvancement pa : result.advancements) {
                ResourceLocation id = pa.resourceLocation();
                Optional<ResourceLocation> bgLoc = pa.background() != null && !pa.background().isEmpty()
                        ? Optional.of(ResourceLocation.parse(pa.background())) : Optional.empty();

                DisplayInfo display = new DisplayInfo(
                        pa.itemStack(), Component.literal(pa.title()), Component.literal(pa.description()),
                        bgLoc.map(ClientAsset::new), pa.frameType(), true, true, false
                );
                display.setLocation(pa.x(), pa.y());

                Map<String, Criterion<?>> criteria = Map.of(
                        "auto", new Criterion<>(new ImpossibleTrigger(), new ImpossibleTrigger.TriggerInstance())
                );
                Optional<ResourceLocation> parentId = pa.parent() != null && !pa.parent().isEmpty()
                        ? Optional.of(ResourceLocation.parse(pa.parent())) : Optional.empty();

                holders.add(new AdvancementHolder(id, new Advancement(
                        parentId, Optional.of(display), AdvancementRewards.EMPTY,
                        criteria, new AdvancementRequirements(List.of(List.of("auto"))),
                        false, Optional.empty()
                )));
                advWp_injected.add(id);
            }

            this.tree.addAll(holders);

            for (AdvancementHolder holder : holders) {
                AdvancementNode node = this.tree.get(holder.id());
                if (node != null) {
                    AdvancementProgress prog = new AdvancementProgress();
                    prog.update(node.advancement().requirements());
                    prog.grantProgress("auto");
                    this.progress.put(node.holder(), prog);
                }
            }

            for (var entry : result.vanillaOverrides.entrySet()) {
                ResourceLocation vid = ResourceLocation.parse(entry.getKey());
                AdvancementNode node = this.tree.get(vid);
                if (node != null) node.holder().value().display().ifPresent(d -> {
                    advWp_vanillaOriginals.put(vid, new float[]{d.getX(), d.getY()});
                    float[] pos = entry.getValue();
                    d.setLocation(pos[0], pos[1]);
                });
            }

            refreshUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Unique
    private void applyOverrides() {
        List<JsonObject> overrides = ConfigManager.loadOverrides();
        if (overrides.isEmpty()) return;

        for (JsonObject o : overrides) {
            if (!o.has("id")) continue;
            String idStr = o.get("id").getAsString();
            ResourceLocation id = ResourceLocation.parse(idStr);
            AdvancementNode node = this.tree.get(id);
            if (node == null) continue;

            node.holder().value().display().ifPresent(display -> {
                if (o.has("title")) {
                    ((DisplayInfoAccessor) display).pepe_setTitle(Component.literal(o.get("title").getAsString()));
                }
                if (o.has("description")) {
                    ((DisplayInfoAccessor) display).pepe_setDescription(Component.literal(o.get("description").getAsString()));
                }
                if (o.has("icon")) {
                    String iconStr = o.get("icon").getAsString();
                    try {
                        var itemOpt = BuiltInRegistries.ITEM.get(ResourceLocation.parse(iconStr));
                        itemOpt.ifPresent(item -> ((DisplayInfoAccessor) display).pepe_setIcon(new ItemStack(item)));
                    } catch (Exception e) {
                        ((DisplayInfoAccessor) display).pepe_setIcon(new ItemStack(Items.PAPER));
                    }
                }
            });
        }
    }

    @Unique
    private void refreshUI() {
        if (this.listener != null) {
            for (ResourceLocation id : advWp_injected) {
                AdvancementNode node = this.tree.get(id);
                if (node != null) {
                    this.listener.onUpdateAdvancementProgress(node, this.progress.get(node.holder()));
                }
            }
        }
    }
}