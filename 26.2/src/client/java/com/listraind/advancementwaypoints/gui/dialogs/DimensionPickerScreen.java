package com.listraind.advancementwaypoints.gui.dialogs;

import com.listraind.advancementwaypoints.advancement.CoordParser;
import com.listraind.advancementwaypoints.gui.base.BaseModScreen;
import net.minecraft.network.chat.Component;

public class DimensionPickerScreen extends BaseModScreen {
   private static final int[] COLORS = new int[]{43520, 11141120, 16733525, 11141290};
   private final WaypointFormScreen parent;

   public DimensionPickerScreen(WaypointFormScreen parent) {
      super(Component.translatable("advwp.picker.dimension.title"), 170, 125);
      this.parent = parent;
      this.titleColor = -1;
   }

   protected void initContent() {
      int bw = 160;
      int bh = 20;
      int gap = 4;

      for(int i = 0; i < 4; ++i) {
         int dimIndex = i;
         int color = COLORS[dimIndex];
         this.addCenteredButton(Component.translatable(CoordParser.DIM_LABEL_KEYS[dimIndex]).withStyle((s) -> s.withColor(color)), 25 + dimIndex * (bh + gap), bw, bh, (b) -> {
            this.parent.addDimRow(dimIndex);
            this.minecraft.gui.setScreen(this.parent);
         });
      }

   }

   public void onClose() {
      this.minecraft.gui.setScreen(this.parent);
   }
}
