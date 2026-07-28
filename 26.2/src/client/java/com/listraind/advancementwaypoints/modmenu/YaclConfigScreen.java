package com.listraind.advancementwaypoints.modmenu;

import com.listraind.advancementwaypoints.config.ModConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class YaclConfigScreen {
   public static Screen create(Screen parent) {
      ModConfig config = ModConfig.getInstance();
      Option<Boolean> chatScannerOpt = Option.<Boolean>createBuilder()
            .name(Component.translatable("advwp.config.chat_scanner"))
            .description(OptionDescription.of(Component.translatable("advwp.config.chat_scanner.desc")))
            .binding(true, config::isEnableChatScanner, (Consumer<Boolean>) config::setEnableChatScanner)
            .controller(TickBoxControllerBuilder::create)
            .build();
      ConfigCategory mainCategory = ConfigCategory.createBuilder()
            .name(Component.translatable("advwp.config.category.general"))
            .option(chatScannerOpt)
            .build();
      return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("advwp.config.title"))
            .category(mainCategory)
            .save(config::save)
            .build()
            .generateScreen(parent);
   }
}
