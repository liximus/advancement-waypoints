package com.listraind.advancementwaypoints.mixin.compat;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BetterAdvancementsMixinPlugin implements IMixinConfigPlugin {

    private static final String BETTER_ADVANCEMENTS_CLASS =
            "betteradvancements.common.gui.BetterAdvancementsScreen";

    private boolean isBetterAdvancementsLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        isBetterAdvancementsLoaded = net.fabricmc.loader.api.FabricLoader
                .getInstance()
                .isModLoaded("betteradvancements");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return isBetterAdvancementsLoaded;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
    }
}