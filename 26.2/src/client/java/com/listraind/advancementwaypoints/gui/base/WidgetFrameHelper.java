package com.listraind.advancementwaypoints.gui.base;

import com.listraind.advancementwaypoints.config.WaypointStorage;
import com.listraind.advancementwaypoints.navigator.Navigator;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.resources.Identifier;

public final class WidgetFrameHelper {
   private static final Identifier TASK_SELECTED = Identifier.fromNamespaceAndPath("advancement-waypoints", "advancements/task_frame_selected");
   private static final Identifier GOAL_SELECTED = Identifier.fromNamespaceAndPath("advancement-waypoints", "advancements/goal_frame_selected");
   private static final Identifier CHALLENGE_SELECTED = Identifier.fromNamespaceAndPath("advancement-waypoints", "advancements/challenge_frame_selected");
   private static final Identifier TASK_HIDDEN = Identifier.fromNamespaceAndPath("advancement-waypoints", "advancements/task_frame_hidden");
   private static final Identifier GOAL_HIDDEN = Identifier.fromNamespaceAndPath("advancement-waypoints", "advancements/goal_frame_hidden");
   private static final Identifier CHALLENGE_HIDDEN = Identifier.fromNamespaceAndPath("advancement-waypoints", "advancements/challenge_frame_hidden");

   private WidgetFrameHelper() {
   }

   public static Identifier resolveSprite(Identifier original, AdvancementNode node, DisplayInfo display) {
      if (node != null && display != null) {
         String idStr = node.holder().id().toString();
         if (WaypointStorage.isBranchHidden(idStr)) {
            Identifier var5;
            switch (display.getType()) {
               case TASK -> var5 = TASK_HIDDEN;
               case GOAL -> var5 = GOAL_HIDDEN;
               case CHALLENGE -> var5 = CHALLENGE_HIDDEN;
               default -> throw new MatchException((String)null, (Throwable)null);
            }

            return var5;
         } else {
            Identifier currentId = Navigator.getInstance().getCurrentId();
            if (currentId != null && currentId.equals(node.holder().id())) {
               Identifier var10000;
               switch (display.getType()) {
                  case TASK -> var10000 = TASK_SELECTED;
                  case GOAL -> var10000 = GOAL_SELECTED;
                  case CHALLENGE -> var10000 = CHALLENGE_SELECTED;
                  default -> throw new MatchException((String)null, (Throwable)null);
               }

               return var10000;
            } else {
               return original;
            }
         }
      } else {
         return original;
      }
   }
}
