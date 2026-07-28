package com.listraind.advancementwaypoints.handler;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.advancement.AdvancementInjector;
import com.listraind.advancementwaypoints.advancement.WaypointData;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementsScreen;
import com.listraind.advancementwaypoints.config.ConfigIO;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.mixin.client.DisplayInfoAccessor;
import net.minecraft.advancements.*;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

import java.util.*;

public class AdvancementTreeManager {

    private final AdvancementTree tree;
    private final Map<AdvancementHolder, AdvancementProgress> progress;
    private final ClientAdvancements.Listener listener;

    private final Set<Identifier> injected = new HashSet<>();
    private final Map<Identifier, float[]> vanillaOriginals = new HashMap<>();

    public AdvancementTreeManager(AdvancementTree tree, Map<AdvancementHolder, AdvancementProgress> progress, ClientAdvancements.Listener listener) {
        this.tree = tree;
        this.progress = progress;
        this.listener = listener;
    }

    public boolean isCompatible(AdvancementTree tree, Map<AdvancementHolder, AdvancementProgress> progress, ClientAdvancements.Listener listener) {
        return this.tree == tree && this.progress == progress && this.listener == listener;
    }

    public void inject() {
        try {
            clearInjected();
            applyOverrides();

            AdvancementInjector.LoadResult result = AdvancementInjector.load(tree);
            List<AdvancementHolder> holders = buildCustomHolders(result);
            if (holders.isEmpty()) {
                refreshUI();
                return;
            }

            addHoldersToTree(holders);
            applyVanillaShifts(result);
            refreshUI();
        } catch (Exception e) {
            AdvancementWaypoints.LOGGER.error("AdvancementTreeManager.inject failed", e);
        }
    }

    private void clearInjected() {
        if (!injected.isEmpty()) {
            Set<Identifier> present = new HashSet<>();
            for (Identifier id : injected) {
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
    }

    private List<AdvancementHolder> buildCustomHolders(AdvancementInjector.LoadResult result) {
        if (result.advancements().isEmpty()) return List.of();

        List<AdvancementHolder> holders = new ArrayList<>();
        for (WaypointData w : result.advancements()) {
            Identifier id = w.resourceLocation();
            Optional<Identifier> bg = w.background() != null && !w.background().isEmpty()
                    ? Optional.of(Identifier.parse(w.background())) : Optional.empty();

            ItemStack stack = w.itemStack();
            if (stack == null || stack.isEmpty()) {
                stack = new ItemStack(Items.PAPER);
            }

            DisplayInfo display = new DisplayInfo(
                    ItemStackTemplate.fromStack(stack), Component.literal(w.title()), Component.literal(w.description()),
                    bg.map(ClientAsset.ResourceTexture::new), w.frameType(), true, true, false
            );
            display.setLocation(w.x(), w.y());

            Map<String, Criterion<?>> criteria = Map.of(
                    "auto", new Criterion<>(new ImpossibleTrigger(), new ImpossibleTrigger.TriggerInstance())
            );
            Optional<Identifier> parent = w.parent() != null && !w.parent().isEmpty()
                    ? Optional.of(Identifier.parse(w.parent())) : Optional.empty();

            holders.add(new AdvancementHolder(id, new Advancement(
                    parent, Optional.of(display), AdvancementRewards.EMPTY,
                    criteria, new AdvancementRequirements(List.of(List.of("auto"))),
                    false, Optional.empty()
            )));
            injected.add(id);
        }
        return holders;
    }

    private void addHoldersToTree(List<AdvancementHolder> holders) {
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
    }

    private void applyVanillaShifts(AdvancementInjector.LoadResult result) {
        for (var e : result.vanillaOverrides().entrySet()) {
            Identifier vid = Identifier.parse(e.getKey());
            AdvancementNode n = tree.get(vid);
            if (n != null) n.holder().value().display().ifPresent(d -> {
                vanillaOriginals.put(vid, new float[]{d.getX(), d.getY()});
                d.setLocation(e.getValue()[0], e.getValue()[1]);
            });
        }
    }

    private void applyOverrides() {
        for (JsonObject o : WaypointStorage.loadOverrides()) {
            String idStr = ConfigIO.str(o, "id", "");
            if (idStr.isEmpty()) continue;
            AdvancementNode n = tree.get(Identifier.parse(idStr));
            if (n == null) continue;

            n.holder().value().display().ifPresent(d -> {
                String title = ConfigIO.nullable(o, "title");
                if (title != null) ((DisplayInfoAccessor) d).advWp_setTitle(Component.literal(title));
                String desc = ConfigIO.nullable(o, "description");
                if (desc != null) ((DisplayInfoAccessor) d).advWp_setDescription(Component.literal(desc));
                String icon = ConfigIO.nullable(o, "icon");
                if (icon != null) {
                    try {
                        var item = BuiltInRegistries.ITEM.get(Identifier.parse(icon));
                        item.ifPresent(i -> ((DisplayInfoAccessor) d).advWp_setIcon(new ItemStackTemplate(i.value())));
                    } catch (Exception e) {
                        ((DisplayInfoAccessor) d).advWp_setIcon(new ItemStackTemplate(Items.PAPER));
                    }
                }
            });
        }
    }

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
