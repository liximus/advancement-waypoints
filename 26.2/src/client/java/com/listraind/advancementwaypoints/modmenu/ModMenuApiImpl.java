package com.listraind.advancementwaypoints.modmenu;

import com.listraind.advancementwaypoints.config.ModConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuApiImpl implements ModMenuApi {
   public ConfigScreenFactory<?> getModConfigScreenFactory() {
      return ModConfig::createConfigScreen;
   }
}
