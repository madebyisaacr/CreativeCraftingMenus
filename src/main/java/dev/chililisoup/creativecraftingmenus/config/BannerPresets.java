package dev.chililisoup.creativecraftingmenus.config;

import dev.chililisoup.creativecraftingmenus.util.ServerResourceProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BannerPresets {
    private static boolean LOADED = false;
    private static final HashMap<String, PresetGroupItem> GROUPS = new HashMap<>();
    private static final LinkedHashMap<String, PresetGroupItem> BUILT_IN_GROUPS = new LinkedHashMap<>();

    public static @Nullable PresetGroupItem get(String key) {
        return GROUPS.get(key);
    }

    public static @Nullable PresetGroupItem getBuiltIn(String key) {
        return BUILT_IN_GROUPS.get(key);
    }

    public static boolean has(String key) {
        return GROUPS.containsKey(key);
    }

    public static boolean isGroupEmpty(String key) {
        PresetGroupItem group = get(key);
        return group == null || group.banners().isEmpty();
    }

    public static void createGroup(String key) {
        if (!has(key)) GROUPS.put(key, new PresetGroupItem(List.of()));
    }

    public static boolean deleteGroup(String key) {
        if (!has(key)) return false;
        GROUPS.remove(key);
        return true;
    }

    public static boolean renameGroup(String from, String to) {
        if (from.equals(to) || !has(from) || has(to)) return false;
        GROUPS.put(to, GROUPS.get(from));
        GROUPS.remove(from);
        return true;
    }

    public static int size() {
        return GROUPS.size();
    }

    public static int builtInSize() {
        return BUILT_IN_GROUPS.size();
    }

    public static Set<Map.Entry<String, PresetGroupItem>> entries() {
        return GROUPS.entrySet();
    }

    public static Set<Map.Entry<String, PresetGroupItem>> builtInEntries() {
        return BUILT_IN_GROUPS.entrySet();
    }

    public static List<PresetGroupItem> allGroups() {
        ArrayList<PresetGroupItem> allGroups = new ArrayList<>(GROUPS.values());
        allGroups.addAll(BUILT_IN_GROUPS.values());
        return allGroups;
    }

    public static void load() {
        if (LOADED) return;
        deserialize(ModConfig.HANDLER.instance().bannerPresets);

        Map.of(
                "letters", DefaultBannerPresets.LETTERS,
                "numbers", DefaultBannerPresets.NUMBERS,
                "symbols", DefaultBannerPresets.SYMBOLS,
                "arrows", DefaultBannerPresets.ARROWS
        ).forEach((key, group) -> BUILT_IN_GROUPS.put(key, deserializeGroup(group)));

        LOADED = true;
    }

    public static void unload() {
        if (!LOADED) return;
        ModConfig.HANDLER.instance().bannerPresets = serialize();
        ModConfig.HANDLER.save();
        GROUPS.clear();
        BUILT_IN_GROUPS.clear();
        LOADED = false;
    }

    private static Map<String, BannerPresetItem.SerializedBannerPresetItem[]> serialize() {
        return GROUPS.keySet().stream().collect(Collectors.toMap(
                key -> key,
                key -> GROUPS.get(key).serialize()
        ));
    }

    private static void deserialize(Map<String, BannerPresetItem.SerializedBannerPresetItem[]> serializedGroups) {
        GROUPS.clear();
        GROUPS.putAll(
                serializedGroups.keySet().stream().collect(Collectors.toMap(
                        key -> key,
                        key -> deserializeGroup(serializedGroups.get(key))
                ))
        );
    }

    private static PresetGroupItem deserializeGroup(BannerPresetItem.SerializedBannerPresetItem[] serializedGroup) {
        return new PresetGroupItem(
                Arrays.stream(serializedGroup).map(BannerPresetItem.SerializedBannerPresetItem::deserialize).toList()
        );
    }

    public record PresetGroupItem(ArrayList<BannerPresetItem> banners) {
        public PresetGroupItem(List<BannerPresetItem> presets) {
            this(new ArrayList<>(presets));
        }

        public boolean addBanner(BannerPresetItem banner) {
            if (!this.banners.contains(banner)) return this.banners.add(banner);
            return false;
        }

        public boolean removeBanner(BannerPresetItem banner) {
            int index = this.banners.indexOf(banner);
            if (index >= 0) this.banners.remove(index);
            return index >= 0;
        }

        private BannerPresetItem.SerializedBannerPresetItem[] serialize() {
            return this.banners.stream().map(BannerPresetItem::serialize)
                    .toArray(BannerPresetItem.SerializedBannerPresetItem[]::new);
        }

        public static class GroupBuilder {
            ArrayList<BannerPresetItem.SerializedBannerPresetItem> banners = new ArrayList<>();

            public GroupBuilder banner(
                    DyeColor bannerColor,
                    Function<
                            BannerPresetItem.SerializedBannerPresetItem.BannerBuilder,
                            BannerPresetItem.SerializedBannerPresetItem.BannerBuilder
                            > bannerBuilder
            ) {
                BannerPresetItem.SerializedBannerPresetItem.BannerBuilder builder =
                        new BannerPresetItem.SerializedBannerPresetItem.BannerBuilder(bannerColor);
                this.banners.add(bannerBuilder.apply(builder).build());
                return this;
            }

            public BannerPresetItem.SerializedBannerPresetItem[] build() {
                return this.banners.toArray(BannerPresetItem.SerializedBannerPresetItem[]::new);
            }
        }
    }

    public static class BannerPresetItem {
        public final DyeColor color;
        public final BannerPatternLayers layers;
        private final ItemStack item;

        BannerPresetItem(DyeColor color, BannerPatternLayers layers) {
            this.color = color;
            this.layers = layers;
            this.item = BannerBlock.byColor(this.color).asItem().getDefaultInstance();
            this.item.set(DataComponents.BANNER_PATTERNS, layers);
        }

        public static BannerPresetItem of(ItemStack itemStack) {
            return new BannerPresetItem(
                    itemStack.getItem() instanceof BannerItem banner ?
                            banner.getColor() :
                            DyeColor.WHITE,
                    itemStack.getOrDefault(
                            DataComponents.BANNER_PATTERNS,
                            BannerPatternLayers.EMPTY
                    )
            );
        }

        public ItemStack item() {
            return this.item.copy();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof BannerPresetItem presetItem)) return false;
            if (this.color != presetItem.color) return false;
            if (this.layers.layers().size() != presetItem.layers.layers().size()) return false;
            for (int i = 0; i < this.layers.layers().size(); i++) {
                BannerPatternLayers.Layer layer = this.layers.layers().get(i);
                BannerPatternLayers.Layer otherLayer = presetItem.layers.layers().get(i);
                if (layer.color() != otherLayer.color()) return false;
                if (!layer.pattern().getRegisteredName().equals(otherLayer.pattern().getRegisteredName())) return false;
            }
            return true;
        }

        private SerializedBannerPresetItem serialize() {
            return new SerializedBannerPresetItem(this);
        }

        public record SerializedBannerPresetItem(String color, SerializedBannerLayer[] layers) {
            public SerializedBannerPresetItem(BannerPresetItem presetItem) {
                this(
                        presetItem.color.getName(),
                        presetItem.layers.layers().stream().map(layer -> new SerializedBannerLayer(
                                layer.color().getName(),
                                layer.pattern().getRegisteredName()
                        )).toArray(SerializedBannerLayer[]::new)
                );
            }

            private BannerPresetItem deserialize() {
                return new BannerPresetItem(
                        DyeColor.byName(this.color, DyeColor.WHITE),
                        new BannerPatternLayers(Arrays.stream(this.layers)
                                .map(SerializedBannerLayer::deserialize)
                                .filter(Objects::nonNull)
                                .toList()
                        )
                );
            }

            public record SerializedBannerLayer(String color, String id) {
                public BannerPatternLayers.@Nullable Layer deserialize() {
                    Holder<@NotNull BannerPattern> patternHolder = ServerResourceProvider.getRegistryElement(
                            Registries.BANNER_PATTERN,
                            Identifier.tryParse(this.id)
                    );

                    return patternHolder == null ? null : new BannerPatternLayers.Layer(
                            patternHolder,
                            DyeColor.byName(this.color, DyeColor.WHITE)
                    );
                }
            }

            public static class BannerBuilder {
                DyeColor bannerColor;
                ArrayList<SerializedBannerLayer> layers = new ArrayList<>();

                public BannerBuilder(DyeColor bannerColor) {
                    this.bannerColor = bannerColor;
                }

                public BannerBuilder layer(DyeColor color, ResourceKey<@NotNull BannerPattern> pattern) {
                    this.layers.add(new SerializedBannerLayer(color.getName(), pattern.identifier().toString()));
                    return this;
                }

                public SerializedBannerPresetItem build() {
                    return new SerializedBannerPresetItem(
                            this.bannerColor.getName(),
                            this.layers.toArray(SerializedBannerLayer[]::new)
                    );
                }
            }
        }
    }
}
