package dev.chililisoup.creativecraftingmenus.gui;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.chililisoup.creativecraftingmenus.CreativeCraftingMenus;
import dev.chililisoup.creativecraftingmenus.config.BannerPresets;
import dev.chililisoup.creativecraftingmenus.gui.components.DyesGrid;
import dev.chililisoup.creativecraftingmenus.util.ServerResourceProvider;
import dev.chililisoup.creativecraftingmenus.util.VersionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

//? if < 1.21.11 {
/*import net.minecraft.client.model.BannerFlagModel;
*///?} else {
import net.minecraft.client.model.object.banner.BannerFlagModel;
 //?}

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.minecraft.client.gui.screens.inventory.LoomScreen.*;

public class LoomMenuTab extends CreativeMenuTab<LoomMenuTab.LoomTabMenu> {
    protected static final Identifier ADD_GROUP_BUTTON = CreativeCraftingMenus.id("widget/add_group_button");
    protected static final Identifier ADD_GROUP_BUTTON_HIGHLIGHTED = CreativeCraftingMenus.id("widget/add_group_button_highlighted");
    protected static final Identifier LAYERS_ICON = CreativeCraftingMenus.id("icon/layers_icon");

    private final ArrayList<Holder.Reference<@NotNull BannerPattern>> patterns = new ArrayList<>();
    private Page.RenderFunction pageRenderer = Page.RenderFunction.EMPTY;
    private Page selectedPage = Page.PATTERN;
    private int selectedLayer = -1;
    private @Nullable String selectedPresetGroup = null;
    private int cachedLayerSize = 0;
    private float scrollOffs;
    private boolean scrolling;
    private int startIndex;
    private BannerFlagModel flag;
    private BannerFlagModel smallFlag;
    private ItemStack randomPresetBanner = Items.WHITE_BANNER.getDefaultInstance();
    private long randomPresetBannerTimer = System.currentTimeMillis();
    private boolean builtInGroups = true;
    private @Nullable EditBox groupNameBox;

    public LoomMenuTab(Component displayName, Supplier<ItemStack> iconGenerator, String id) {
        super(displayName, iconGenerator, id);
    }

    @Override
    LoomTabMenu createMenu(Player player) {
        return new LoomTabMenu(player);
    }

    @Override
    public void subInit() {
        this.scrolling = false;
        if (this.screen == null) return;
        if (this.groupNameBox != null) this.screen.removeWidget(this.groupNameBox);

        this.groupNameBox = new EditBox(
                screen.getFont(),
                screen.leftPos + 31,
                screen.topPos + 13,
                49,
                14,
                Component.empty()
        ) {
            @Override
            public boolean isVisible() {
                return this.isEditable() && super.isVisible();
            }
        };

        this.groupNameBox.setTextColor(-1);
        this.groupNameBox.setTextColorUneditable(-1);
        //? if >= 1.21.11
        this.groupNameBox.setInvertHighlightedTextColor(false);
        this.groupNameBox.setBordered(true);
        this.groupNameBox.setResponder(this::onGroupNameChanged);
        this.screen.addRenderableWidget(this.groupNameBox);

        ModelPart modelPart = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG);
        ModelPart flagPart = modelPart.getChild("flag");
        this.flag = new BannerFlagModel(modelPart);
        this.smallFlag = new BannerFlagModel(modelPart) {
            @Override
            public void setupAnim(@NotNull Float value) {
                super.setupAnim(value);
                flagPart.offsetScale(new Vector3f(-0.55F, -0.525F, 0F));
                flagPart.y += 21F;
            }
        };

        if (patterns.isEmpty())
            patterns.addAll(ServerResourceProvider.getRegistryElements(Registries.BANNER_PATTERN));

        BannerPresets.load();
        this.update();
    }

    private void newRandomPresetBanner() {
        ArrayList<BannerPresets.BannerPresetItem> banners = new ArrayList<>();
        BannerPresets.allGroups().forEach(group -> banners.addAll(group.banners()));
        if (!banners.isEmpty()) this.randomPresetBanner = banners.get(Mth.floor(Math.random() * banners.size())).item();
        this.randomPresetBannerTimer = System.currentTimeMillis();
    }

    @Override
    public void drawTitle(TitleDrawer titleDrawer, int x, int y, int color) {
        super.drawTitle(titleDrawer, x, y - 2, color);
    }

    @Override
    public void render(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (this.menu == null || this.groupNameBox == null) return;

        ItemStack itemStack = this.menu.resultSlots.getItem(0);
        Item item = itemStack.getItem();
        if (!(item instanceof BannerItem bannerItem)) return;
        DyeColor bannerColor = bannerItem.getColor();
        guiGraphics.submitBannerPatternRenderState(
                this.flag,
                bannerColor,
                itemStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY),
                screen.leftPos + 166,
                screen.topPos + 8,
                screen.leftPos + 186,
                screen.topPos + 48
        );

        if (System.currentTimeMillis() - this.randomPresetBannerTimer > 2000)
            this.newRandomPresetBanner();

        for (int i = 0; i < Page.values().length; i++) {
            Page page = Page.values()[i];
            int x = screen.leftPos + 6;
            int y = screen.topPos + 12 + i * 20;
            boolean selected = page == this.selectedPage;

            boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + 16 && mouseY < y + 18;
            if (hovered) {
                if (!selected) guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                guiGraphics.setTooltipForNextFrame(page.tooltip, mouseX, mouseY);
            }

            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    selected ? SELECTED_TAB : (hovered ? HIGHLIGHTED_TAB : UNSELECTED_TAB),
                    x,
                    y,
                    16,
                    18
            );

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(x, y + 1);
            page.iconRenderer.accept(this, guiGraphics);
            guiGraphics.pose().popMatrix();
        }

        this.renderScrollBar(screen, guiGraphics, mouseX, mouseY);
        this.renderButtons(screen, guiGraphics, mouseX, mouseY);
        this.renderPageContents(screen, guiGraphics, mouseX, mouseY);

        if (this.selectedPage != Page.PRESETS)
            DyesGrid.renderDyes(guiGraphics, screen.leftPos + 100, screen.topPos + 21, mouseX, mouseY,
                    this.menu.getColors().get(this.selectedLayer + 1));
        else this.renderSecondaryPresetsPageContents(screen, guiGraphics, mouseX, mouseY);
    }

    private void renderBannerOnButton(GuiGraphics guiGraphics, int x, int y, TextureAtlasSprite textureAtlasSprite) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x + 4, y + 2);
        float u0 = textureAtlasSprite.getU0();
        float u1 = u0 + (textureAtlasSprite.getU1() - textureAtlasSprite.getU0()) * 21F / 64F;
        float size = textureAtlasSprite.getV1() - textureAtlasSprite.getV0();
        float v0 = textureAtlasSprite.getV0() + size / 64F;
        float v1 = v0 + size * 40F / 64F;
        guiGraphics.fill(0, 0, 5, 10, DyeColor.GRAY.getTextureDiffuseColor());
        guiGraphics.blit(textureAtlasSprite.atlasLocation(), 0, 0, 5, 10, u0, u1, v0, v1);
        guiGraphics.pose().popMatrix();
    }

    private void renderScrollBar(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = screen.leftPos + 83;
        int y = screen.topPos + 13 + (int) (41F * this.scrollOffs);

        guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                this.isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE,
                x,
                y,
                12,
                15
        );

        if (mouseX >= x && mouseX < x + 12 && mouseY >= y && mouseY < y + 15)
            guiGraphics.requestCursor(this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
    }

    private void renderButtons(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int left = screen.leftPos + 99;
        int top = screen.topPos + 8;

        boolean presetsPage = this.selectedPage == Page.PRESETS;
        for (int i = 0; i < 3; i++) {
            String name = switch (i) {
                case 0 -> "add";
                case 1 -> "remove";
                default -> "clear";
            };

            int x = left + i * 20;

            boolean deletableGroup = presetsPage &&
                    i == 2 && this.selectedPresetGroup != null &&
                    BannerPresets.isGroupEmpty(this.selectedPresetGroup);

            boolean disabled = presetsPage ?
                    this.builtInGroups || (i == 2 && !deletableGroup) || this.selectedPresetGroup == null :
                    i == 1 && this.selectedLayer < 0;
            boolean hovered = !disabled && mouseX >= x && mouseY >= top && mouseX < x + 18 && mouseY < top + 13;
            if (hovered) {
                guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                guiGraphics.setTooltipForNextFrame(
                        Component.translatable(
                                "container.creative_crafting_menus.loom." + name + "_button" + (presetsPage ? ".presets" : "")
                        ),
                        mouseX,
                        mouseY
                );
            }

            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    CreativeCraftingMenus.id(String.format(
                            "widget/%s_button%s",
                            name,
                            hovered ? "_highlighted" : (disabled ? "_disabled" : "")
                    )),
                    x,
                    top,
                    18,
                    11
            );
        }
    }

    private void renderPageContents(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.menu == null) return;

        int left = screen.leftPos + 24;
        int top = screen.topPos + 13;

        this.pageRenderer.render(guiGraphics, left, top, mouseX, mouseY);
    }

    private static Page.RenderFunction getPatternPageRenderer(LoomMenuTab instance) {
        if (instance.menu == null) return Page.RenderFunction.EMPTY;

        DyeColor dyeColor = instance.menu.getColors().get(instance.selectedLayer + 1);

        List<BannerPatternLayers.Layer> layers = instance.menu.getLayers();
        Holder<@NotNull BannerPattern> selectedPattern =
                (instance.selectedLayer < 0 || layers.isEmpty()) ?
                        null : layers.get(instance.selectedLayer).pattern();

        return (guiGraphics, left, top, mouseX, mouseY) -> {
            for (int i = instance.startIndex; i < instance.patterns.size() && i < 16 + instance.startIndex; i++) {
                int x = left + ((i - instance.startIndex) % 4) * 14;
                int y = top + ((i - instance.startIndex) / 4) * 14;

                Holder<@NotNull BannerPattern> pattern = instance.patterns.get(i);
                boolean disabled = selectedPattern == null;
                boolean selected = selectedPattern == pattern;
                boolean hovered = !disabled && mouseX >= x && mouseY >= y && mouseX < x + 14 && mouseY < y + 14;

                if (hovered) {
                    if (!selected) guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                    guiGraphics.setTooltipForNextFrame(
                            Component.translatable(pattern.value().translationKey() + "." + dyeColor.getName()),
                            mouseX,
                            mouseY
                    );
                }

                guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        disabled ? BUTTON_DISABLED : (selected ? BUTTON_SELECTED : (hovered ? BUTTON_HIGHLIGHTED : BUTTON)),
                        x,
                        y,
                        14,
                        14
                );

                instance.renderBannerOnButton(guiGraphics, x, y, guiGraphics.getSprite(Sheets.getBannerMaterial(pattern)));
            }
        };
    }

    private static Page.RenderFunction getLayersPageRenderer(LoomMenuTab instance) {
        if (instance.menu == null) return Page.RenderFunction.EMPTY;

        List<DyeColor> colors = instance.menu.getColors();
        ArrayList<BannerPatternLayers.@Nullable Layer> layers = new ArrayList<>();
        layers.add(null);
        layers.addAll(instance.menu.getLayers());

        return (guiGraphics, left, top, mouseX, mouseY) -> {
            for (int i = instance.startIndex; i < layers.size() && i < 4 + instance.startIndex; i++) {
                int y = top + (i - instance.startIndex) * 14;

                BannerPatternLayers.@Nullable Layer layer = layers.get(i);
                Holder<@NotNull BannerPattern> pattern = layer == null ? instance.patterns.getFirst() : layer.pattern();
                DyeColor dyeColor = colors.get(i);

                boolean selected = instance.selectedLayer == i - 1;
                boolean upVisible = i > 1;
                boolean downVisible = i > 0 && i < layers.size() - 1;
                boolean upHovered = upVisible &&
                        mouseX >= left + 45 && mouseY >= y && mouseX < left + 56 && mouseY < y + 7;
                boolean downHovered = downVisible && !upHovered &&
                        mouseX >= left + 45 && mouseY >= y + 7 && mouseX < left + 56 && mouseY < y + 14;
                boolean hovered = !upHovered && !downHovered &&
                        mouseX >= left && mouseY >= y && mouseX < left + 56 && mouseY < y + 14;

                if (hovered) {
                    if (!selected) guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                    guiGraphics.setTooltipForNextFrame(
                            i == 0 ?
                                    instance.menu.getBannerItem().getName() :
                                    Component.translatable(pattern.value().translationKey() + "." + dyeColor.getName()),
                            mouseX,
                            mouseY
                    );
                }

                if (upHovered || downHovered) {
                    guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                    guiGraphics.setTooltipForNextFrame(
                            Component.translatable(
                                    "container.creative_crafting_menus.loom.move_layer_" + (upHovered ? "up" : "down")
                            ),
                            mouseX,
                            mouseY
                    );
                }

                guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        selected ? BUTTON_SELECTED : (hovered ? BUTTON_HIGHLIGHTED : BUTTON),
                        left,
                        y,
                        56,
                        14
                );

                if (upVisible) guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        upHovered ? ARROW_UP_HIGHLIGHTED : ARROW_UP,
                        left + 46,
                        y + 1,
                        9,
                        6
                );

                if (downVisible) guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        downHovered ? ARROW_DOWN_HIGHLIGHTED : ARROW_DOWN,
                        left + 46,
                        y + 7,
                        9,
                        6
                );

                guiGraphics.drawString(Minecraft.getInstance().font, (i > 0) ? String.valueOf(i) : "-", left + 3, y + 3, 0xFFFFFFFF);
                instance.renderBannerOnButton(guiGraphics, left + 14, y, guiGraphics.getSprite(Sheets.getBannerMaterial(pattern)));
                DyesGrid.renderDyeIcon(guiGraphics, dyeColor, left + 28, y);
            }
        };
    }

    private static Page.RenderFunction getPresetsPageRenderer(LoomMenuTab instance) {
        if (instance.menu == null) return Page.RenderFunction.EMPTY;

        if (instance.selectedPresetGroup == null) {
            ArrayList<Map.@Nullable Entry<String, BannerPresets.PresetGroupItem>> groups =
                    new ArrayList<>((
                            instance.builtInGroups ?
                                    BannerPresets.builtInEntries() :
                                    BannerPresets.entries()
                    ).stream().toList());
            if (!instance.builtInGroups) groups.add(null);

            return (guiGraphics, left, top, mouseX, mouseY) -> {
                for (int i = instance.startIndex; i < groups.size() && i < 4 + instance.startIndex; i++) {
                    int y = top + (i - instance.startIndex) * 14;

                    Map.@Nullable Entry<String, BannerPresets.PresetGroupItem> group = groups.get(i);
                    Component label = group != null ?
                            Component.literal(group.getKey()) :
                            Component.translatable("container.creative_crafting_menus.loom.new_preset_group");

                    boolean hovered = mouseX >= left && mouseY >= y && mouseX < left + 56 && mouseY < y + 14;
                    if (hovered) {
                        guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                        guiGraphics.setTooltipForNextFrame(label, mouseX, mouseY);
                    }

                    guiGraphics.blitSprite(
                            RenderPipelines.GUI_TEXTURED,
                            group != null ?
                                    (hovered ? BUTTON_HIGHLIGHTED : BUTTON) :
                                    (hovered ? ADD_GROUP_BUTTON_HIGHLIGHTED : ADD_GROUP_BUTTON),
                            left,
                            y,
                            56,
                            14
                    );

                    if (group != null) VersionHelper.drawScrollingString(
                            guiGraphics,
                            label,
                            left + 3,
                            left + 3,
                            left + 53,
                            y,
                            y + 14
                    );
                }
            };
        }

        return (guiGraphics, left, top, mouseX, mouseY) -> {
            boolean hovered = mouseX >= left && mouseY >= top && mouseX < left + 7 && mouseY < top + 14;
            if (hovered) {
                guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                guiGraphics.setTooltipForNextFrame(Component.translatable("gui.back"), mouseX, mouseY);
            }

            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BUTTON_SELECTED, left, top, 56, 14);

            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    hovered ? ARROW_BACK_HIGHLIGHTED : ARROW_BACK,
                    left + 1,
                    top + 1,
                    5,
                    12
            );

            if (instance.builtInGroups) VersionHelper.drawScrollingString(
                    guiGraphics,
                    Component.literal(instance.selectedPresetGroup),
                    left + 8,
                    left + 8,
                    left + 53,
                    top,
                    top + 14
            );

            BannerPresets.PresetGroupItem group = instance.builtInGroups ?
                    BannerPresets.getBuiltIn(instance.selectedPresetGroup) :
                    BannerPresets.get(instance.selectedPresetGroup);
            if (group == null) return;

            for (int i = instance.startIndex; i < group.banners().size() && i < 10 + instance.startIndex; i++) {
                int x = left + ((i - instance.startIndex) % 5) * 11;
                int y = top + 14 + ((i - instance.startIndex) / 5) * 21;

                BannerPresets.BannerPresetItem banner = group.banners().get(i);

                boolean bannerHovered = !hovered && mouseX >= x && mouseY >= y && mouseX < x + 11 && mouseY < y + 21;
                if (bannerHovered) guiGraphics.requestCursor(CursorTypes.POINTING_HAND);

                guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        bannerHovered ? BUTTON_HIGHLIGHTED : BUTTON,
                        x,
                        y,
                        11,
                        21
                );

                guiGraphics.submitBannerPatternRenderState(
                        instance.smallFlag, banner.color, banner.layers, (x + 1), (y + 1), (x + 10), (y + 20)
                );
            }
        };
    }

    private void renderSecondaryPresetsPageContents(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.menu == null) return;

        int left = screen.leftPos + 100;
        int top = screen.topPos + 21;

        for (int i = 0; i < 2; i++) {
            int y = top + i * 14;

            Component label = i == 0 ?
                    Component.translatable("container.creative_crafting_menus.loom.built_in_presets") :
                    Component.translatable("container.creative_crafting_menus.loom.user_presets");

            boolean selected = (i == 0) == this.builtInGroups;
            boolean hovered = mouseX >= left && mouseY >= y && mouseX < left + 56 && mouseY < y + 14;
            if (hovered) {
                if (!selected) guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                guiGraphics.setTooltipForNextFrame(label, mouseX, mouseY);
            }

            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    selected ? BUTTON_SELECTED : (hovered ? BUTTON_HIGHLIGHTED : BUTTON),
                    left,
                    y,
                    56,
                    14
            );

            VersionHelper.drawScrollingString(
                    guiGraphics,
                    label,
                    left + 3,
                    left + 3,
                    left + 53,
                    y,
                    y + 14
            );
        }
    }

    private static @Nullable Runnable checkPatternPageClicked(LoomMenuTab instance, int left, int top, double mouseX, double mouseY) {
        if (instance.menu == null) return null;

        List<BannerPatternLayers.Layer> layers = instance.menu.getLayers();
        Holder<@NotNull BannerPattern> selectedPattern =
                (instance.selectedLayer < 0 || layers.isEmpty()) ?
                        null : layers.get(instance.selectedLayer).pattern();

        for (int i = instance.startIndex; i < instance.patterns.size() && i < 16 + instance.startIndex; i++) {
            Holder<@NotNull BannerPattern> pattern = instance.patterns.get(i);
            if (selectedPattern == pattern) continue;

            int x = left + ((i - instance.startIndex) % 4) * 14;
            int y = top + ((i - instance.startIndex) / 4) * 14;
            if (mouseX >= x && mouseY >= y && mouseX < x + 14 && mouseY < y + 14)
                return () -> instance.menu.setPattern(pattern, instance.selectedLayer);
        }

        return null;
    }

    private static @Nullable Runnable checkLayersPageClicked(LoomMenuTab instance, int left, int top, double mouseX, double mouseY) {
        if (instance.menu == null) return null;

        int size = instance.menu.getLayers().size() + 1;
        for (int i = instance.startIndex; i < size && i < 4 + instance.startIndex; i++) {
            int y = top + (i - instance.startIndex) * 14;

            boolean upVisible = i > 1;
            boolean downVisible = i > 0 && i < size - 1;

            int layer = i - 1;
            boolean selected = instance.selectedLayer == layer;

            if (upVisible && mouseX >= left + 45 && mouseY >= y && mouseX < left + 56 && mouseY < y + 7)
                return () -> {
                    if (selected) instance.selectedLayer--;
                    else if (instance.selectedLayer == layer - 1)
                        instance.selectedLayer++;
                    instance.menu.moveLayer(layer, -1);
                };

            if (downVisible && mouseX >= left + 45 && mouseY >= y + 7 && mouseX < left + 56 && mouseY < y + 14)
                return () -> {
                    if (selected) instance.selectedLayer++;
                    else if (instance.selectedLayer == layer + 1)
                        instance.selectedLayer--;
                    instance.menu.moveLayer(layer, 1);
                };

            if (!selected && mouseX >= left && mouseY >= y && mouseX < left + 56 && mouseY < y + 14)
                return () -> {
                    instance.selectedLayer = layer;
                    instance.update();
                };
        }

        return null;
    }

    private static @Nullable Runnable checkPresetsPageClicked(LoomMenuTab instance, int left, int top, double mouseX, double mouseY) {
        if (instance.menu == null) return null;

        if (instance.selectedPresetGroup == null) {
            ArrayList<Map.@Nullable Entry<String, BannerPresets.PresetGroupItem>> groups =
                    new ArrayList<>((
                            instance.builtInGroups ?
                                    BannerPresets.builtInEntries() :
                                    BannerPresets.entries()
                    ).stream().toList());
            if (!instance.builtInGroups) groups.add(null);

            for (int i = instance.startIndex; i < groups.size() && i < 4 + instance.startIndex; i++) {
                int y = top + (i - instance.startIndex) * 14;
                Map.@Nullable Entry<String, BannerPresets.PresetGroupItem> group = groups.get(i);

                if (mouseX >= left && mouseY >= y && mouseX < left + 56 && mouseY < y + 14)
                    return () -> {
                        if (group != null) instance.selectedPresetGroup = group.getKey();
                        else {
                            String name = "New Group";
                            int num = 1;
                            while (BannerPresets.has(name))
                                name = "New Group (" + num++ + ")";

                            BannerPresets.createGroup(name);
                            instance.selectedPresetGroup = name;
                        }
                        instance.scrollOffs = 0F;
                        instance.update();
                    };
            }
        } else {
            if (mouseX >= left && mouseY >= top && mouseX < left + 7 && mouseY < top + 14)
                return () -> {
                    instance.selectedPresetGroup = null;
                    instance.scrollOffs = 0F;
                    instance.update();
                };

            BannerPresets.PresetGroupItem group = instance.builtInGroups ?
                    BannerPresets.getBuiltIn(instance.selectedPresetGroup) :
                    BannerPresets.get(instance.selectedPresetGroup);
            if (group == null) return null;

            for (int i = instance.startIndex; i < group.banners().size() && i < 10 + instance.startIndex; i++) {
                int x = left + ((i - instance.startIndex) % 5) * 11;
                int y = top + 14 + ((i - instance.startIndex) / 5) * 21;

                BannerPresets.BannerPresetItem banner = group.banners().get(i);
                if (mouseX >= x && mouseY >= y && mouseX < x + 11 && mouseY < y + 21)
                    return () -> {
                        instance.menu.resultSlots.setItem(0, banner.item());
                        instance.update();
                    };
            }
        }

        return null;
    }

    private int checkButtonClicked(double mouseX, double mouseY) {
        if (this.screen == null) return -1;

        boolean presetsPage = this.selectedPage == Page.PRESETS;
        if (presetsPage && this.builtInGroups) return -1;

        int left = screen.leftPos + 99;
        int top = screen.topPos + 8;

        if (presetsPage && this.selectedPresetGroup == null) return -1;
        for (int i = 0; i < 3; i++) {
            if (presetsPage) {
                if (i == 2 && !BannerPresets.isGroupEmpty(this.selectedPresetGroup)) continue;
            } else if (i == 1 && this.selectedLayer < 0) continue;

            int x = left + i * 20;
            if (mouseX >= x && mouseY >= top && mouseX < x + 18 && mouseY < top + 13)
                return i;
        }

        return -1;
    }

    private @Nullable DyeColor checkDyeClicked(double mouseX, double mouseY) {
        if (this.screen == null || this.menu == null) return null;
        return DyesGrid.getClickedDye(
                this.screen.leftPos + 100,
                this.screen.topPos + 21,
                mouseX,
                mouseY,
                this.menu.getColors().get(this.selectedLayer + 1)
        );
    }

    private @Nullable Page checkPageClicked(double mouseX, double mouseY) {
        if (this.screen == null) return null;

        for (int i = 0; i < Page.values().length; i++) {
            Page page = Page.values()[i];
            if (page == this.selectedPage) continue;

            int x = this.screen.leftPos + 6;
            int y = this.screen.topPos + 12 + i * 20;
            if (mouseX >= x && mouseY >= y && mouseX < x + 16 && mouseY < y + 18)
                return page;
        }

        return null;
    }

    private @Nullable Runnable checkPageContentsClicked(double mouseX, double mouseY) {
        return this.screen != null ? this.selectedPage.clickChecker.check(
                this,
                this.screen.leftPos + 24,
                this.screen.topPos + 13,
                mouseX,
                mouseY
        ) : null;
    }

    private @Nullable Runnable checkSecondaryPresetsPageContentsClicked(double mouseX, double mouseY) {
        if (this.screen == null || this.menu == null) return null;

        int left = this.screen.leftPos + 100;
        int top = this.screen.topPos + 21;

        for (int i = 0; i < 2; i++) {
            if ((i == 0) == this.builtInGroups) continue;

            int y = top + i * 14;
            if (mouseX >= left && mouseY >= y && mouseX < left + 56 && mouseY < y + 14)
                return () -> {
                    this.builtInGroups = !this.builtInGroups;
                    this.selectedPresetGroup = null;
                    this.scrollOffs = 0F;
                    this.update();
                };
        }

        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent) {
        if (this.screen == null) return false;

        int x = this.screen.leftPos + 83;
        int y = this.screen.topPos + 13;
        if (mouseButtonEvent.x() >= x && mouseButtonEvent.x() < x + 12 && mouseButtonEvent.y() >= y && mouseButtonEvent.y() < y + 56)
            this.scrolling = true;

        if (checkButtonClicked(mouseButtonEvent.x(), mouseButtonEvent.y()) >= 0) return true;
        if (checkPageClicked(mouseButtonEvent.x(), mouseButtonEvent.y()) != null) return true;

        if (this.selectedPage != Page.PRESETS) {
            if (checkDyeClicked(mouseButtonEvent.x(), mouseButtonEvent.y()) != null) return true;
        } else if (checkSecondaryPresetsPageContentsClicked(mouseButtonEvent.x(), mouseButtonEvent.y()) != null) return true;

        return checkPageContentsClicked(mouseButtonEvent.x(), mouseButtonEvent.y()) != null;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        this.scrolling = false;
        if (this.menu == null) return false;

        int button = checkButtonClicked(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (button >= 0) {
            if (this.selectedPage != Page.PRESETS) switch (button) {
                case 0 -> this.menu.addLayer(this.patterns.get(1));
                case 1 -> this.menu.removeLayer(this.selectedLayer);
                default -> this.menu.resetBanner();
            } else if (this.selectedPresetGroup != null) {
                BannerPresets.PresetGroupItem group = BannerPresets.get(this.selectedPresetGroup);
                if (group != null) {
                    BannerPresets.BannerPresetItem banner = BannerPresets.BannerPresetItem.of(this.menu.resultSlots.getItem(0));
                    if (button == 0) {
                        if (group.addBanner(banner))
                            this.update();
                    } else if (button == 1) {
                        if (group.removeBanner(banner))
                            this.update();
                    } else {
                        if (BannerPresets.deleteGroup(this.selectedPresetGroup)) {
                            this.selectedPresetGroup = null;
                            this.scrollOffs = 0F;
                            this.update();
                        }
                    }
                }
            }
            return true;
        }

        Page page = checkPageClicked(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (page != null) {
            this.selectedPage = page;
            this.scrollOffs = 0F;
            this.update();
            return true;
        }

        if (this.selectedPage != Page.PRESETS) {
            DyeColor color = checkDyeClicked(mouseButtonEvent.x(), mouseButtonEvent.y());
            if (color != null) {
                this.menu.setColor(color, this.selectedLayer);
                return true;
            }
        } else {
            Runnable onClick = checkSecondaryPresetsPageContentsClicked(mouseButtonEvent.x(), mouseButtonEvent.y());
            if (onClick != null) {
                onClick.run();
                return true;
            }
        }

        Runnable onClick = checkPageContentsClicked(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (onClick != null) {
            onClick.run();
            return true;
        }

        return false;
    }

    private boolean isScrollBarActive() {
        return this.getOffscreenRows() > 0;
    }

    private int getOffscreenRows() {
        return Math.max(this.selectedPage.getOffscreenRows.apply(this), 0);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent) {
        if (this.screen == null || !this.scrolling || !this.isScrollBarActive())
            return false;

        int top = this.screen.topPos + 13;
        int bottom = top + 56;
        this.scrollOffs = ((float) mouseButtonEvent.y() - top - 7.5F) / (bottom - top - 15F);
        this.scrollOffs = Mth.clamp(this.scrollOffs, 0F, 1F);
        this.startIndex = (int) (this.scrollOffs * this.getOffscreenRows() + 0.5) * this.selectedPage.getColumns.apply(this);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isScrollBarActive()) {
            int offscreenRows = this.getOffscreenRows();
            float deltaY = (float) scrollY / offscreenRows;
            this.scrollOffs = Mth.clamp(this.scrollOffs - deltaY, 0F, 1F);
            this.startIndex = (int) (this.scrollOffs * offscreenRows + 0.5) * this.selectedPage.getColumns.apply(this);
        }

        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.isEscape() || this.screen == null || this.groupNameBox == null) return false;
        if (keyEvent.isConfirmation() && this.screen.getFocused() == this.groupNameBox) {
            this.screen.clearFocus();
            return true;
        }
        return this.groupNameBox.keyPressed(keyEvent) || this.groupNameBox.canConsumeInput();
    }

    private void onGroupNameChanged(String key) {
        if (this.selectedPage != Page.PRESETS || this.builtInGroups || this.selectedPresetGroup == null) return;
        if (BannerPresets.renameGroup(this.selectedPresetGroup, key))
            this.selectedPresetGroup = key;
    }

    @Override
    public void remove() {
        this.selectedPage = Page.PATTERN;
        this.pageRenderer = Page.RenderFunction.EMPTY;
        this.selectedLayer = -1;
        this.selectedPresetGroup = null;
        this.builtInGroups = true;
        this.cachedLayerSize = 0;
        this.scrollOffs = 0F;
        this.startIndex = 0;
        if (this.screen != null && this.groupNameBox != null)
            this.screen.removeWidget(this.groupNameBox);
        this.groupNameBox = null;
        super.remove();
    }

    @Override
    public void dispose() {
        this.patterns.clear();
        BannerPresets.unload();
        this.selectedPage = Page.PATTERN;
        this.pageRenderer = Page.RenderFunction.EMPTY;
        this.selectedLayer = -1;
        this.selectedPresetGroup = null;
        this.builtInGroups = true;
        this.cachedLayerSize = 0;
        this.scrollOffs = 0F;
        this.startIndex = 0;
        super.dispose();
    }

    private void update() {
        if (this.menu == null || this.groupNameBox == null || this.screen == null) return;

        List<BannerPatternLayers.Layer> layers = this.menu.getLayers();
        if (layers.size() != this.cachedLayerSize) {
            this.selectedLayer = (this.cachedLayerSize > layers.size()) ?
                    Math.min(this.selectedLayer, layers.size() - 1) :
                    layers.size() - 1;
            this.cachedLayerSize = layers.size();
        }

        this.startIndex = (int) (this.scrollOffs * this.getOffscreenRows() + 0.5) * this.selectedPage.getColumns.apply(this);
        this.pageRenderer = this.selectedPage.rendererSupplier.apply(this);

        boolean groupNameEditable = this.selectedPage == Page.PRESETS && !this.builtInGroups && this.selectedPresetGroup != null;
        this.groupNameBox.setEditable(groupNameEditable);
        if (groupNameEditable) this.groupNameBox.setValue(this.selectedPresetGroup);
        else if (this.screen.getFocused() == this.groupNameBox) this.screen.clearFocus();
    }

    private enum Page {
        PATTERN(
                Component.translatable("container.creative_crafting_menus.loom.pattern"),
                (instance, guiGraphics) -> guiGraphics.renderItem(Items.CREEPER_BANNER_PATTERN.getDefaultInstance(), 0, 0),
                LoomMenuTab::getPatternPageRenderer,
                LoomMenuTab::checkPatternPageClicked,
                instance -> (instance.patterns.size() - 13) / 4,
                instance -> 4
        ),
        LAYERS(
                Component.translatable("container.creative_crafting_menus.loom.layers"),
                (instance, guiGraphics) -> guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED, LAYERS_ICON, 0, 0, 16, 16
                ),
                LoomMenuTab::getLayersPageRenderer,
                LoomMenuTab::checkLayersPageClicked,
                instance -> instance.menu != null ? instance.menu.getLayers().size() - 3 : 0,
                instance -> 1
        ),
        PRESETS(
                Component.translatable("container.creative_crafting_menus.loom.presets"),
                (instance, guiGraphics) -> guiGraphics.renderItem(instance.randomPresetBanner, 0, 0),
                LoomMenuTab::getPresetsPageRenderer,
                LoomMenuTab::checkPresetsPageClicked,
                instance -> {
                    if (instance.selectedPresetGroup == null)
                        return instance.builtInGroups ?
                                BannerPresets.builtInSize() - 4 :
                                BannerPresets.size() - 3;

                    BannerPresets.PresetGroupItem group = instance.builtInGroups ?
                            BannerPresets.getBuiltIn(instance.selectedPresetGroup) :
                            BannerPresets.get(instance.selectedPresetGroup);
                    return ((group != null ? group.banners().size() : 0) - 6) / 5;
                },
                instance -> instance.selectedPresetGroup != null ? 5 : 1
        );

        private final Component tooltip;
        private final BiConsumer<LoomMenuTab, GuiGraphics> iconRenderer;
        private final Function<LoomMenuTab, RenderFunction> rendererSupplier;
        private final ClickChecker clickChecker;
        private final Function<LoomMenuTab, Integer> getOffscreenRows;
        private final Function<LoomMenuTab, Integer> getColumns;

        Page(
                final Component tooltip,
                final BiConsumer<LoomMenuTab, GuiGraphics> iconRenderer,
                final Function<LoomMenuTab, RenderFunction> rendererSupplier,
                final ClickChecker clickChecker,
                final Function<LoomMenuTab, Integer> getOffscreenRows,
                final Function<LoomMenuTab, Integer> getColumns
                ) {
            this.tooltip = tooltip;
            this.iconRenderer = iconRenderer;
            this.clickChecker = clickChecker;
            this.rendererSupplier = rendererSupplier;
            this.getOffscreenRows = getOffscreenRows;
            this.getColumns = getColumns;
        }

        private interface RenderFunction {
            RenderFunction EMPTY = (guiGraphics, left, top, mouseX, mouseY) -> {};

            void render(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY);
        }

        private interface ClickChecker {
            @Nullable Runnable check(LoomMenuTab instance, int left, int top, double mouseX, double mouseY);
        }
    }

    public class LoomTabMenu extends CreativeTabMenu<LoomTabMenu> {
        private final ResultContainer resultSlots = new ResultContainer();

        LoomTabMenu(Player player) {
            super(player);

            LoomTabMenu self = this;
            this.addSlot(new Slot(this.resultSlots, 0, 168, 57) {
                @Override
                public boolean mayPlace(@NotNull ItemStack itemStack) {
                    if (itemStack.getItem() instanceof BannerItem)
                        this.container.setItem(0, itemStack.copyWithCount(1));
                    return false;
                }

                @Override
                public void onTake(@NotNull Player player, @NotNull ItemStack itemStack) {
                    self.onTake(itemStack);
                }
            });

            this.resultSlots.setItem(0, Items.WHITE_BANNER.getDefaultInstance());
        }

        @Override
        LoomTabMenu copyWithPlayer(@NotNull Player player) {
            return this.copyContentsTo(new LoomTabMenu(player));
        }

        @Override
        public @NotNull ItemStack quickMoveFromInventory(@NotNull Player player, int slotIndex) {
            Slot slot = this.player.inventoryMenu.slots.get(slotIndex);
            if (!slot.hasItem()) return ItemStack.EMPTY;

            ItemStack slotStack = slot.getItem();

            if (slotStack.getItem() instanceof BannerItem) {
                this.resultSlots.setItem(0, slotStack.copyWithCount(1));
                LoomMenuTab.this.update();
            }

            return ItemStack.EMPTY;
        }

        private void addLayer(Holder<@NotNull BannerPattern> pattern) {
            ItemStack itemStack = this.resultSlots.getItem(0);
            if (itemStack.isEmpty()) return;

            if (itemStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().size() >= 16)
                return;

            itemStack.update(
                    DataComponents.BANNER_PATTERNS,
                    BannerPatternLayers.EMPTY,
                    bannerPatternLayers -> new BannerPatternLayers.Builder().addAll(bannerPatternLayers).add(
                            pattern,
                            this.getBannerItem().getColor() == DyeColor.WHITE ? DyeColor.BLACK : DyeColor.WHITE
                    ).build()
            );
            LoomMenuTab.this.update();
        }

        private void removeLayer(int layer) {
            if (layer < 0) return;

            ItemStack itemStack = this.resultSlots.getItem(0);
            if (itemStack.isEmpty()) return;

            ArrayList<BannerPatternLayers.Layer> layers = new ArrayList<>(this.getLayers());
            if (layer >= layers.size()) return;
            layers.remove(layer);

            itemStack.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(layers));
            LoomMenuTab.this.update();
        }

        private void setPattern(Holder<@NotNull BannerPattern> pattern, int layer) {
            if (layer < 0) return;

            ItemStack itemStack = this.resultSlots.getItem(0);
            if (itemStack.isEmpty()) return;

            List<BannerPatternLayers.Layer> layers = this.getLayers();
            if (layer >= layers.size()) return;

            ArrayList<BannerPatternLayers.Layer> updated = new ArrayList<>(layers);
            updated.set(layer, new BannerPatternLayers.Layer(pattern, layers.get(layer).color()));

            itemStack.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(updated));
            LoomMenuTab.this.update();
        }

        private void setColor(DyeColor color, int layer) {
            ItemStack itemStack = this.resultSlots.getItem(0);
            if (itemStack.isEmpty()) return;

            if (layer < 0) {
                ItemStack swapped = BannerBlock.byColor(color).asItem().getDefaultInstance();
                swapped.applyComponents(itemStack.getComponentsPatch());

                this.resultSlots.setItem(0, swapped);
                LoomMenuTab.this.update();
                return;
            }

            List<BannerPatternLayers.Layer> layers = this.getLayers();
            if (layer >= layers.size()) return;

            ArrayList<BannerPatternLayers.Layer> updated = new ArrayList<>(layers);
            updated.set(layer, new BannerPatternLayers.Layer(layers.get(layer).pattern(), color));

            itemStack.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(updated));
            LoomMenuTab.this.update();
        }

        private void moveLayer(int layer, int change) {
            if (layer < 0) return;

            ItemStack itemStack = this.resultSlots.getItem(0);
            if (itemStack.isEmpty()) return;

            List<BannerPatternLayers.Layer> layers = this.getLayers();
            if (layer >= layers.size()) return;

            ArrayList<BannerPatternLayers.Layer> updated = new ArrayList<>(layers);
            BannerPatternLayers.Layer from = updated.get(layer);
            BannerPatternLayers.Layer to = updated.get(layer + change);
            updated.set(layer, to);
            updated.set(layer + change, from);

            itemStack.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(updated));
            LoomMenuTab.this.update();
        }

        private void resetBanner() {
            this.resultSlots.setItem(0, Items.WHITE_BANNER.getDefaultInstance());
            LoomMenuTab.this.update();
        }

        private List<BannerPatternLayers.Layer> getLayers() {
            return this.resultSlots.getItem(0).getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers();
        }

        private List<DyeColor> getColors() {
            ArrayList<DyeColor> list = new ArrayList<>();
            list.add(this.getBannerItem().getColor());
            list.addAll(this.getLayers().stream().map(BannerPatternLayers.Layer::color).toList());
            return list;
        }

        private BannerItem getBannerItem() {
            Item item = this.resultSlots.getItem(0).getItem();
            if (item instanceof BannerItem bannerItem) return bannerItem;

            BannerItem bannerItem = (BannerItem) Items.WHITE_BANNER;
            this.resultSlots.setItem(0, bannerItem.getDefaultInstance());
            LoomMenuTab.this.update();
            return bannerItem;
        }

        private void onTake(ItemStack itemStack) {
            ItemStack copy = itemStack.copy();
            Minecraft.getInstance().schedule(() -> this.resultSlots.setItem(0, copy));
        }
    }
}
