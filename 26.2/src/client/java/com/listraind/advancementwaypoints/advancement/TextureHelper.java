package com.listraind.advancementwaypoints.advancement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class TextureHelper {

    public static final String DEFAULT_BG = "minecraft:gui/advancements/backgrounds/stone";

    public record BlockFace(Identifier textureId, Component name) {}

    private static final Map<String, String> SPECIAL_TEXTURE_MAP = Map.ofEntries(
            Map.entry("snow_block", "snow"),
            Map.entry("magma_block", "magma"),
            Map.entry("smooth_quartz", "quartz_block_bottom"),
            Map.entry("smooth_sandstone", "sandstone_top"),
            Map.entry("smooth_red_sandstone", "red_sandstone_top"),
            Map.entry("smooth_basalt", "basalt_side")
    );

    private static final Map<Identifier, int[]> TEXTURE_DIMS_CACHE = new ConcurrentHashMap<>();

    public static int[] getTextureDimensions(Identifier textureFilePath) {
        return TEXTURE_DIMS_CACHE.computeIfAbsent(textureFilePath, path -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                try {
                    var res = mc.getResourceManager().getResource(path);
                    if (res.isPresent()) {
                        try (InputStream is = res.get().open()) {
                            byte[] header = new byte[24];
                            int read = is.readNBytes(header, 0, 24);
                            if (read >= 24 && header[0] == (byte) 0x89 && header[1] == (byte) 0x50 && header[2] == (byte) 0x4E && header[3] == (byte) 0x47) {
                                int w = ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16) | ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
                                int h = ((header[20] & 0xFF) << 24) | ((header[21] & 0xFF) << 16) | ((header[22] & 0xFF) << 8) | (header[23] & 0xFF);
                                if (w > 0 && h > 0) {
                                    return new int[]{w, h};
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            return new int[]{16, 16};
        });
    }

    public static void blitBackground(GuiGraphicsExtractor g, RenderPipeline pipeline, Identifier texture, int x, int y, int width, int height) {
        Identifier textureFile = toTextureFilePath(texture);
        int[] dims = getTextureDimensions(textureFile);
        int imgW = dims[0];
        int imgH = dims[1];
        if (imgW <= 0 || imgH <= 0) {
            g.blit(pipeline, texture, x, y, 0.0F, 0.0F, width, height, width, height);
            return;
        }

        int frameCount = Math.max(1, imgH / imgW);
        float v = 0.0F;
        if (frameCount > 1) {
            int frameIndex = (int) ((System.currentTimeMillis() / 150L) % frameCount);
            v = (float) (frameIndex * width);
        }
        g.blit(pipeline, texture, x, y, 0.0F, v, width, height, width, width * frameCount);
    }

    public static List<BlockFace> getBlockFaces(Item item) {
        if (!(item instanceof BlockItem bi)) {
            Identifier def = Identifier.parse(DEFAULT_BG);
            return List.of(new BlockFace(def, Component.translatable("advwp.face.main")));
        }

        Block block = bi.getBlock();
        Identifier blockKey = BuiltInRegistries.BLOCK.getKey(block);
        String ns = blockKey.getNamespace();
        String path = blockKey.getPath();

        Minecraft mc = Minecraft.getInstance();
        Map<Identifier, Component> faceMap = new LinkedHashMap<>();

        Map<String, String> modelTextures = new LinkedHashMap<>();
        collectModelTextures(mc, Identifier.fromNamespaceAndPath(ns, "models/block/" + path + ".json"), modelTextures, 0);
        collectModelTextures(mc, Identifier.fromNamespaceAndPath(ns, "models/item/" + path + ".json"), modelTextures, 0);

        for (var entry : modelTextures.entrySet()) {
            String roleKey = entry.getKey();
            String target = resolveTextureVariable(entry.getValue(), modelTextures, 0);
            if (target != null && !target.isEmpty() && !target.endsWith("_overlay")) {
                Identifier texClean = toCleanAssetId(Identifier.parse(target));
                if (textureFileExists(mc, toTextureFilePath(texClean))) {
                    Component name = roleFromKey(roleKey, texClean.getPath());
                    if (!faceMap.containsKey(texClean)) {
                        faceMap.put(texClean, name);
                    }
                }
            }
        }

        if (SPECIAL_TEXTURE_MAP.containsKey(path)) {
            String mapped = SPECIAL_TEXTURE_MAP.get(path);
            Identifier mappedId = Identifier.fromNamespaceAndPath(ns, "block/" + mapped);
            if (textureFileExists(mc, toTextureFilePath(mappedId)) && !faceMap.containsKey(mappedId)) {
                faceMap.put(mappedId, roleFromKey(null, mappedId.getPath()));
            }
        }

        String cleanPath = path;
        if (cleanPath.startsWith("waxed_")) {
            cleanPath = cleanPath.substring(6);
        }
        if (cleanPath.startsWith("infested_")) {
            cleanPath = cleanPath.substring(9);
        }

        String[] suffixes = {"", "_top", "_side", "_front", "_bottom", "_end", "_back", "_lines", "_chiseled"};
        for (String suffix : suffixes) {
            Identifier id = Identifier.fromNamespaceAndPath(ns, "block/" + cleanPath + suffix);
            if (textureFileExists(mc, toTextureFilePath(id)) && !faceMap.containsKey(id)) {
                faceMap.put(id, roleFromKey(suffix.isEmpty() ? null : suffix.substring(1), id.getPath()));
            }
        }

        List<BlockFace> result = new ArrayList<>();
        for (var entry : faceMap.entrySet()) {
            result.add(new BlockFace(entry.getKey(), entry.getValue()));
        }

        result.sort(Comparator.comparingInt(f -> getRoleOrder(f.name())));

        if (result.isEmpty()) {
            Identifier def = Identifier.parse(DEFAULT_BG);
            result.add(new BlockFace(def, Component.translatable("advwp.face.main")));
        }

        return result;
    }

    private static String resolveTextureVariable(String val, Map<String, String> textures, int depth) {
        if (val == null || depth > 5) return null;
        if (val.startsWith("#")) {
            String varName = val.substring(1);
            return resolveTextureVariable(textures.get(varName), textures, depth + 1);
        }
        return val;
    }

    private static void collectModelTextures(Minecraft mc, Identifier modelPath, Map<String, String> out, int depth) {
        if (mc == null || mc.getResourceManager() == null || depth > 5) return;
        try {
            var resOpt = mc.getResourceManager().getResource(modelPath);
            if (resOpt.isEmpty()) return;
            try (InputStream is = resOpt.get().open();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("textures") && json.get("textures").isJsonObject()) {
                    JsonObject tex = json.getAsJsonObject("textures");
                    for (var entry : tex.entrySet()) {
                        String key = entry.getKey();
                        if ("particle".equals(key) || "overlay".equals(key)) continue;
                        String val = entry.getValue().getAsString();
                        if (!out.containsKey(key)) {
                            out.put(key, val);
                        }
                    }
                }
                if (json.has("parent")) {
                    String parentStr = json.get("parent").getAsString();
                    Identifier parentId = Identifier.parse(parentStr);
                    Identifier parentModelPath = Identifier.fromNamespaceAndPath(parentId.getNamespace(), "models/" + parentId.getPath() + ".json");
                    collectModelTextures(mc, parentModelPath, out, depth + 1);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static int getRoleOrder(Component name) {
        String key = name.getString();
        if (key.equals(Component.translatable("advwp.face.top").getString())) return 10;
        if (key.equals(Component.translatable("advwp.face.front").getString())) return 20;
        if (key.equals(Component.translatable("advwp.face.side").getString())) return 30;
        if (key.equals(Component.translatable("advwp.face.back").getString())) return 40;
        if (key.equals(Component.translatable("advwp.face.end").getString())) return 50;
        if (key.equals(Component.translatable("advwp.face.bottom").getString())) return 60;
        if (key.equals(Component.translatable("advwp.face.inside").getString())) return 70;
        if (key.equals(Component.translatable("advwp.face.lines").getString())) return 80;
        if (key.equals(Component.translatable("advwp.face.chiseled").getString())) return 90;
        return 100;
    }

    private static Component roleFromKey(String key, String texturePath) {
        if (key != null) {
            String k = key.toLowerCase();
            if (k.equals("top") || k.equals("up")) {
                return Component.translatable("advwp.face.top");
            } else if (k.equals("bottom") || k.equals("down")) {
                return Component.translatable("advwp.face.bottom");
            } else if (k.equals("side") || k.equals("east") || k.equals("west")) {
                return Component.translatable("advwp.face.side");
            } else if (k.equals("front") || k.equals("north")) {
                return Component.translatable("advwp.face.front");
            } else if (k.equals("back") || k.equals("south")) {
                return Component.translatable("advwp.face.back");
            } else if (k.equals("end")) {
                return Component.translatable("advwp.face.end");
            } else if (k.equals("inside") || k.equals("inner")) {
                return Component.translatable("advwp.face.inside");
            }
        }

        if (texturePath != null) {
            String p = texturePath.toLowerCase();
            if (p.endsWith("_top") || p.endsWith("/up") || p.endsWith("_up")) {
                return Component.translatable("advwp.face.top");
            } else if (p.endsWith("_bottom") || p.endsWith("_down") || p.endsWith("/down")) {
                return Component.translatable("advwp.face.bottom");
            } else if (p.endsWith("_side")) {
                return Component.translatable("advwp.face.side");
            } else if (p.endsWith("_front") || p.endsWith("_north")) {
                return Component.translatable("advwp.face.front");
            } else if (p.endsWith("_back") || p.endsWith("_south")) {
                return Component.translatable("advwp.face.back");
            } else if (p.endsWith("_end")) {
                return Component.translatable("advwp.face.end");
            } else if (p.endsWith("_inside") || p.endsWith("_inner")) {
                return Component.translatable("advwp.face.inside");
            } else if (p.endsWith("_lines")) {
                return Component.translatable("advwp.face.lines");
            } else if (p.endsWith("_chiseled")) {
                return Component.translatable("advwp.face.chiseled");
            }
        }

        String name = texturePath != null ? texturePath : (key != null ? key : "");
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) name = name.substring(lastSlash + 1);
        name = name.replace('_', ' ');
        if (!name.isEmpty()) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return Component.literal(name);
    }

    public static Component getFaceName(Identifier faceId) {
        return roleFromKey(null, faceId.getPath());
    }

    public static Identifier resolveBlockTexture(Item item) {
        List<BlockFace> all = getBlockFaces(item);
        return all.isEmpty() ? Identifier.parse(DEFAULT_BG) : all.get(0).textureId();
    }

    public static Identifier toCleanAssetId(Identifier id) {
        String path = id.getPath();
        if (path.startsWith("textures/")) {
            path = path.substring(9);
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        return Identifier.fromNamespaceAndPath(id.getNamespace(), path);
    }

    public static Identifier toTextureFilePath(Identifier cleanId) {
        String path = cleanId.getPath();
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }
        return Identifier.fromNamespaceAndPath(cleanId.getNamespace(), path);
    }

    public static boolean textureFileExists(Minecraft mc, Identifier textureFilePath) {
        if (mc == null || mc.getResourceManager() == null) return true;
        try {
            return mc.getResourceManager().getResource(textureFilePath).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public static Optional<ClientAsset.ResourceTexture> parseBackground(String bgStr) {
        if (bgStr == null || bgStr.isEmpty()) return Optional.empty();
        try {
            Identifier rawId = Identifier.parse(bgStr);
            Identifier cleanId = toCleanAssetId(rawId);
            Identifier textureFile = toTextureFilePath(cleanId);

            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getResourceManager() != null && !textureFileExists(mc, textureFile)) {
                String blockName = cleanId.getPath();
                if (blockName.startsWith("block/")) {
                    blockName = blockName.substring(6);
                }
                var itemOpt = BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath(cleanId.getNamespace(), blockName));
                if (itemOpt.isPresent()) {
                    cleanId = resolveBlockTexture(itemOpt.get().value());
                    textureFile = toTextureFilePath(cleanId);
                } else {
                    cleanId = Identifier.parse(DEFAULT_BG);
                    textureFile = toTextureFilePath(cleanId);
                }
            }

            return Optional.of(new ClientAsset.ResourceTexture(cleanId, textureFile));
        } catch (Exception e) {
            Identifier def = Identifier.parse(DEFAULT_BG);
            return Optional.of(new ClientAsset.ResourceTexture(def, toTextureFilePath(def)));
        }
    }
}
