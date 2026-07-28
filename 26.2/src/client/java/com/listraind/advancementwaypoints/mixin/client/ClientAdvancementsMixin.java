package com.listraind.advancementwaypoints.mixin.client;

import com.google.gson.JsonObject;
import com.listraind.advancementwaypoints.AdvancementWaypoints;
import com.listraind.advancementwaypoints.advancement.AdvancementInjector;
import com.listraind.advancementwaypoints.advancement.WaypointData;
import com.listraind.advancementwaypoints.api.IAdvancementInjector;
import com.listraind.advancementwaypoints.compat.IBetterAdvancementsScreen;
import com.listraind.advancementwaypoints.config.ConfigIO;
import com.listraind.advancementwaypoints.config.WaypointStorage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientAdvancements.class})
public abstract class ClientAdvancementsMixin implements IAdvancementInjector {
   @Unique
   private final Set<Identifier> injected = new HashSet();
   @Unique
   private final Map<Identifier, float[]> vanillaOriginals = new HashMap();
   @Unique
   private long lastInjectMs = 0L;
   @Unique
   private static final long INJECT_DEBOUNCE_MS = 250L;
   @Final
   @Shadow
   private AdvancementTree tree;
   @Final
   @Shadow
   private Map<AdvancementHolder, AdvancementProgress> progress;
   @Shadow
   private ClientAdvancements.Listener listener;

   @Inject(
      method = {"update"},
      at = {@At("RETURN")}
   )
   private void onUpdate(ClientboundUpdateAdvancementsPacket pkt, CallbackInfo ci) {
      long now = System.currentTimeMillis();
      if (now - this.lastInjectMs >= 250L) {
         this.lastInjectMs = now;
         this.advWaypoint_inject();
      }
   }

   public void advWaypoint_inject() {
      try {
         this.clearInjected();
         this.applyOverrides();
         AdvancementInjector.LoadResult result = AdvancementInjector.load(this.tree);
         List<AdvancementHolder> holders = this.buildCustomHolders(result);
         if (holders.isEmpty()) {
            this.refreshUI();
            return;
         }

         this.addHoldersToTree(holders);
         this.applyVanillaShifts(result);
         this.refreshUI();
      } catch (Exception e) {
         AdvancementWaypoints.LOGGER.error("advWaypoint_inject failed", e);
      }

   }

   @Unique
   private void clearInjected() {
      if (!this.injected.isEmpty()) {
         Set<Identifier> present = new HashSet();

         for(Identifier id : this.injected) {
            AdvancementNode n = this.tree.get(id);
            if (n != null) {
               this.progress.remove(n.holder());
               present.add(id);
            }
         }

         if (!present.isEmpty()) {
            this.tree.remove(present);
         }

         this.injected.clear();
      }

      for(Map.Entry<Identifier, float[]> e : this.vanillaOriginals.entrySet()) {
         AdvancementNode n = this.tree.get((Identifier)e.getKey());
         if (n != null) {
            n.holder().value().display().ifPresent((d) -> {
               float[] o = (float[])e.getValue();
               d.setLocation(o[0], o[1]);
            });
         }
      }

      this.vanillaOriginals.clear();
   }

   @Unique
   private List<AdvancementHolder> buildCustomHolders(AdvancementInjector.LoadResult result) {
      if (result.advancements().isEmpty()) {
         return List.of();
      } else {
         List<AdvancementHolder> holders = new ArrayList();

         for(WaypointData w : result.advancements()) {
            Identifier id = w.resourceLocation();
            Optional<Identifier> bg = w.background() != null && !w.background().isEmpty() ? Optional.of(Identifier.parse(w.background())) : Optional.empty();
            ItemStack stack = w.itemStack();
            if (stack == null || stack.isEmpty()) {
               stack = new ItemStack(Items.PAPER);
            }

            DisplayInfo display = new DisplayInfo(ItemStackTemplate.fromStack(stack), Component.literal(w.title()), Component.literal(w.description()), bg.map(ClientAsset.ResourceTexture::new), w.frameType(), true, true, false);
            display.setLocation(w.x(), w.y());
            Map<String, Criterion<?>> criteria = Map.of("auto", new Criterion(new ImpossibleTrigger(), new ImpossibleTrigger.TriggerInstance()));
            Optional<Identifier> parent = w.parent() != null && !w.parent().isEmpty() ? Optional.of(Identifier.parse(w.parent())) : Optional.empty();
            holders.add(new AdvancementHolder(id, new Advancement(parent, Optional.of(display), AdvancementRewards.EMPTY, criteria, new AdvancementRequirements(List.of(List.of("auto"))), false, Optional.empty())));
            this.injected.add(id);
         }

         return holders;
      }
   }

   @Unique
   private void addHoldersToTree(List<AdvancementHolder> holders) {
      this.tree.addAll(holders);

      for(AdvancementHolder h : holders) {
         AdvancementNode n = this.tree.get(h.id());
         if (n != null) {
            AdvancementProgress p = new AdvancementProgress();
            p.update(n.advancement().requirements());
            p.grantProgress("auto");
            this.progress.put(n.holder(), p);
         }
      }

   }

   @Unique
   private void applyVanillaShifts(AdvancementInjector.LoadResult result) {
      for(Map.Entry<String, float[]> e : result.vanillaOverrides().entrySet()) {
         Identifier vid = Identifier.parse((String)e.getKey());
         AdvancementNode n = this.tree.get(vid);
         if (n != null) {
            n.holder().value().display().ifPresent((d) -> {
               this.vanillaOriginals.put(vid, new float[]{d.getX(), d.getY()});
               d.setLocation(((float[])e.getValue())[0], ((float[])e.getValue())[1]);
            });
         }
      }

   }

   @Unique
   private void applyOverrides() {
      for(JsonObject o : WaypointStorage.loadOverrides()) {
         String idStr = ConfigIO.str(o, "id", "");
         if (!idStr.isEmpty()) {
            AdvancementNode n = this.tree.get(Identifier.parse(idStr));
            if (n != null) {
               n.holder().value().display().ifPresent((d) -> {
                  String title = ConfigIO.nullable(o, "title");
                  if (title != null) {
                     ((DisplayInfoAccessor)d).advWp_setTitle(Component.literal(title));
                  }

                  String desc = ConfigIO.nullable(o, "description");
                  if (desc != null) {
                     ((DisplayInfoAccessor)d).advWp_setDescription(Component.literal(desc));
                  }

                  String icon = ConfigIO.nullable(o, "icon");
                  if (icon != null) {
                     try {
                        Optional<Holder.Reference<Item>> item = BuiltInRegistries.ITEM.get(Identifier.parse(icon));
                        item.ifPresent((i) -> ((DisplayInfoAccessor)d).advWp_setIcon(new ItemStackTemplate((Item)i.value())));
                     } catch (Exception var6) {
                        ((DisplayInfoAccessor)d).advWp_setIcon(new ItemStackTemplate(Items.PAPER));
                     }
                  }

               });
            }
         }
      }

   }

   @Unique
   private void refreshUI() {
      if (this.listener != null) {
         for(AdvancementNode n : this.tree.nodes()) {
            AdvancementProgress p = (AdvancementProgress)this.progress.get(n.holder());
            if (p == null) {
               p = new AdvancementProgress();
               p.update(n.advancement().requirements());
            }

            this.listener.onUpdateAdvancementProgress(n, p);
         }

         ClientAdvancements.Listener var5 = this.listener;
         if (var5 instanceof IBetterAdvancementsScreen) {
            IBetterAdvancementsScreen screen = (IBetterAdvancementsScreen)var5;
            screen.advWp_recalculateAll();
         }

      }
   }
}
