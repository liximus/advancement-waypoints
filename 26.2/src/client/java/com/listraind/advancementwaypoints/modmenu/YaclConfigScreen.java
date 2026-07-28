package com.listraind.advancementwaypoints.modmenu;

import com.listraind.advancementwaypoints.config.ModConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class YaclConfigScreen {

   private static Option<Boolean> enableNavigationOpt;
   private static Option<ModConfig.HudMode> hudModeOpt;
   private static Option<ModConfig.HudPosition> hudPositionOpt;
   private static Option<Boolean> showDistanceOpt;
   private static Option<Boolean> showItemOpt;

   public static Screen create(Screen parent) {
      ModConfig config = ModConfig.getInstance();

      Option<Boolean> chatScannerOpt = Option.<Boolean>createBuilder()
            .name(Component.translatable("advwp.config.chat_scanner"))
            .description(OptionDescription.of(Component.translatable("advwp.config.chat_scanner.desc")))
            .binding(true, config::isEnableChatScanner, (Consumer<Boolean>) config::setEnableChatScanner)
            .controller(TickBoxControllerBuilder::create)
            .build();

      boolean navEnabled = config.isEnableNavigation();
      boolean isLocatorMode = config.getHudMode() == ModConfig.HudMode.LOCATOR;

      hudPositionOpt = Option.<ModConfig.HudPosition>createBuilder()
            .name(Component.translatable("advwp.config.hud_position"))
            .description(OptionDescription.of(Component.translatable("advwp.config.hud_position.desc")))
            .binding(ModConfig.HudPosition.BOTTOM_RIGHT, config::getHudPosition, (Consumer<ModConfig.HudPosition>) config::setHudPosition)
            .controller(opt -> EnumControllerBuilder.create(opt).enumClass(ModConfig.HudPosition.class))
            .available(navEnabled && !isLocatorMode)
            .build();

      showDistanceOpt = Option.<Boolean>createBuilder()
            .name(Component.translatable("advwp.config.show_distance_on_locator"))
            .description(OptionDescription.of(Component.translatable("advwp.config.show_distance_on_locator.desc")))
            .binding(true, config::isShowDistanceOnLocator, (Consumer<Boolean>) config::setShowDistanceOnLocator)
            .controller(TickBoxControllerBuilder::create)
            .available(navEnabled && isLocatorMode)
            .build();

      showItemOpt = Option.<Boolean>createBuilder()
            .name(Component.translatable("advwp.config.show_item_on_locator"))
            .description(OptionDescription.of(Component.translatable("advwp.config.show_item_on_locator.desc")))
            .binding(true, config::isShowItemOnLocator, (Consumer<Boolean>) config::setShowItemOnLocator)
            .controller(TickBoxControllerBuilder::create)
            .available(navEnabled && isLocatorMode)
            .build();

      hudModeOpt = Option.<ModConfig.HudMode>createBuilder()
            .name(Component.translatable("advwp.config.hud_mode"))
            .description(OptionDescription.of(Component.translatable("advwp.config.hud_mode.desc")))
            .binding(ModConfig.HudMode.ARROW, config::getHudMode, (Consumer<ModConfig.HudMode>) config::setHudMode)
            .controller(opt -> EnumControllerBuilder.create(opt).enumClass(ModConfig.HudMode.class))
            .available(navEnabled)
            .listener((opt, mode) -> {
               boolean currentNavState = enableNavigationOpt != null ? enableNavigationOpt.pendingValue() : config.isEnableNavigation();
               updateAvailability(currentNavState, mode);
            })
            .build();

      enableNavigationOpt = Option.<Boolean>createBuilder()
            .name(Component.translatable("advwp.config.enable_navigation"))
            .description(OptionDescription.of(Component.translatable("advwp.config.enable_navigation.desc")))
            .binding(true, config::isEnableNavigation, (Consumer<Boolean>) config::setEnableNavigation)
            .controller(TickBoxControllerBuilder::create)
            .listener((opt, enabled) -> {
               ModConfig.HudMode currentMode = hudModeOpt != null ? hudModeOpt.pendingValue() : config.getHudMode();
               updateAvailability(enabled, currentMode);
            })
            .build();

      ConfigCategory generalCategory = ConfigCategory.createBuilder()
            .name(Component.translatable("advwp.config.category.general"))
            .option(chatScannerOpt)
            .build();

      ConfigCategory navigationCategory = ConfigCategory.createBuilder()
            .name(Component.translatable("advwp.config.category.navigation"))
            .option(enableNavigationOpt)
            .option(hudModeOpt)
            .option(hudPositionOpt)
            .option(showDistanceOpt)
            .option(showItemOpt)
            .build();

      return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("advwp.config.title"))
            .category(generalCategory)
            .category(navigationCategory)
            .save(config::save)
            .build()
            .generateScreen(parent);
   }

   private static void updateAvailability(boolean navEnabled, ModConfig.HudMode mode) {
      if (hudModeOpt != null) hudModeOpt.setAvailable(navEnabled);
      boolean isLoc = mode == ModConfig.HudMode.LOCATOR;
      if (hudPositionOpt != null) hudPositionOpt.setAvailable(navEnabled && !isLoc);
      if (showDistanceOpt != null) showDistanceOpt.setAvailable(navEnabled && isLoc);
      if (showItemOpt != null) showItemOpt.setAvailable(navEnabled && isLoc);
   }
}
