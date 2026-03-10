package dev.chililisoup.creativecraftingmenus.gui;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.datafixers.util.Pair;
import dev.chililisoup.creativecraftingmenus.CreativeCraftingMenus;
import dev.chililisoup.creativecraftingmenus.util.ServerResourceProvider;
import dev.chililisoup.creativecraftingmenus.util.MenuHelper;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ProvidesTrimMaterial;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.minecraft.client.gui.screens.inventory.SmithingScreen.ARMOR_STAND_ANGLE;
import static net.minecraft.client.gui.screens.inventory.SmithingScreen.ARMOR_STAND_TRANSLATION;
import static net.minecraft.client.gui.screens.inventory.StonecutterScreen.SCROLLER_DISABLED_SPRITE;
import static net.minecraft.client.gui.screens.inventory.StonecutterScreen.SCROLLER_SPRITE;

public class SmithingMenuTab extends CreativeMenuTab<SmithingMenuTab.SmithingTabMenu> {
    private static final Identifier PLACEHOLDER_TRIM = CreativeCraftingMenus.id("icon/placeholder_trim_smithing_template");
    private static final Set<TagKey<@NotNull Item>> ARMOR_TRIM_TAGS = Set.of(
            ItemTags.HEAD_ARMOR,
            ItemTags.CHEST_ARMOR,
            ItemTags.LEG_ARMOR,
            ItemTags.FOOT_ARMOR
    );

    private static final Set<TagKey<@NotNull Item>> MATERIAL_SWAP_TAGS = Set.of(
            ItemTags.SWORDS,
            ItemTags.PICKAXES,
            ItemTags.AXES,
            ItemTags.SHOVELS,
            ItemTags.HOES,
            ItemTags.HEAD_ARMOR,
            ItemTags.CHEST_ARMOR,
            ItemTags.LEG_ARMOR,
            ItemTags.FOOT_ARMOR,
            //? if >= 1.21.11 {
            ItemTags.SPEARS,
            ConventionalItemTags.HORSE_ARMORS,
            ConventionalItemTags.NAUTILUS_ARMORS,
            //?}
            ItemTags.SIGNS,
            ItemTags.HANGING_SIGNS,
            ItemTags.WOODEN_SHELVES,
            ConventionalItemTags.POTIONS
    );

    private final ArmorStandRenderState armorStandPreview = new ArmorStandRenderState();
    private final ArrayList<Pair<Holder.@Nullable Reference<@NotNull TrimPattern>, ItemStack>> trimPatterns = new ArrayList<>();
    private final ArrayList<Pair<Holder.@Nullable Reference<@NotNull TrimMaterial>, ItemStack>> trimMaterials = new ArrayList<>();
    private List<Page.PageItem> pageContents = List.of();
    private Page selectedPage = Page.TRIM_PATTERN;
    private float scrollOffs;
    private boolean scrolling;
    private int startIndex;

    public SmithingMenuTab(Component displayName, Supplier<ItemStack> iconGenerator, String id) {
        super(displayName, iconGenerator, id);

        this.armorStandPreview.entityType = EntityType.ARMOR_STAND;
        this.armorStandPreview.showBasePlate = false;
        this.armorStandPreview.showArms = true;
        this.armorStandPreview.xRot = 25F;
        this.armorStandPreview.bodyRot = 210F;
        this.armorStandPreview.elytraRotX = Mth.PI / 12F;
        this.armorStandPreview.elytraRotY = 0F;
        this.armorStandPreview.elytraRotZ = -this.armorStandPreview.elytraRotX;

        //? if < 1.21.11 {
        /*this.armorStandPreview.lightCoords = 0xF000F0;
        *///?}
    }

    @Override
    public boolean keepInputOnTabSwitch() {
        return true;
    }

    @Override
    public ItemStack extractInputItem() {
        if (this.menu == null) return ItemStack.EMPTY;
        SmithingTabMenu m = this.menu;
        ItemStack stack = m.inputSlots.removeItemNoUpdate(0);
        if (!stack.isEmpty()) m.slotsChanged(m.inputSlots);
        return stack;
    }

    @Override
    public boolean acceptInputItem(ItemStack stack) {
        if (this.menu == null || stack.isEmpty()) return false;
        SmithingTabMenu m = this.menu;
        m.inputSlots.setItem(0, stack.copy());
        m.slotsChanged(m.inputSlots);
        return true;
    }

    @Override
    SmithingTabMenu createMenu(Player player) {
        return new SmithingTabMenu(player);
    }

    @Override
    public void subInit() {
        this.scrolling = false;

        if (trimPatterns.isEmpty()) {
            trimPatterns.add(Pair.of(null, Items.BARRIER.getDefaultInstance()));
            trimPatterns.addAll(ServerResourceProvider.getRegistryElements(Registries.TRIM_PATTERN).stream().map(
                    patternRef -> Pair.of(
                            patternRef,
                            BuiltInRegistries.ITEM.get(
                                    patternRef.value().assetId().withSuffix("_armor_trim_smithing_template")
                            ).map(ref -> ref.value().getDefaultInstance()).orElse(ItemStack.EMPTY)
                    )
            ).toList());
        }

        if (trimMaterials.isEmpty()) {
            trimMaterials.add(Pair.of(null, Items.BARRIER.getDefaultInstance()));

            List<Item> materialItems = ServerResourceProvider.getFromComponent(DataComponents.PROVIDES_TRIM_MATERIAL);

            trimMaterials.addAll(ServerResourceProvider.getRegistryElements(Registries.TRIM_MATERIAL).stream().map(
                    patternRef -> {
                        ItemStack iconStack = ItemStack.EMPTY;
                        for (Item item : materialItems) {
                            ProvidesTrimMaterial materialProvider = item.components().get(DataComponents.PROVIDES_TRIM_MATERIAL);
                            if (materialProvider == null) continue;
                            Optional<ResourceKey<@NotNull TrimMaterial>> key = materialProvider.material().key();
                            if (key.isPresent() && patternRef.is(key.get())) {
                                iconStack = item.getDefaultInstance();
                                break;
                            }
                        }

                        return Pair.of(patternRef, iconStack);
                    }
            ).toList());
        }

        if (this.pageContents.isEmpty()) this.pageContents = this.selectedPage.contentsSupplier.apply(this);
    }

    @Override
    public void render(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        this.renderScrollBar(screen, guiGraphics, mouseX, mouseY);

        guiGraphics.submitEntityRenderState(
                this.armorStandPreview,
                30F,
                ARMOR_STAND_TRANSLATION, ARMOR_STAND_ANGLE,
                null,
                screen.leftPos + screen.imageWidth - 60,
                screen.topPos + 10,
                screen.leftPos + screen.imageWidth - 8,
                screen.topPos + 80
        );

        List<Page> visiblePages = this.getVisiblePages();
        for (int i = 0; i < visiblePages.size(); i++) {
            Page page = visiblePages.get(i);
            int x = screen.leftPos + 33;
            int y = screen.topPos + 15 + i * 19;
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
            guiGraphics.renderItem(page.getIcon(this), x, y + 1);
        }

        this.renderPageContents(screen, guiGraphics, mouseX, mouseY);
    }

    private void renderScrollBar(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = screen.leftPos + 118;
        int y = screen.topPos + 16 + (int) (39F * this.scrollOffs);

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

    private void renderPageContents(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.menu == null) return;

        for (int i = this.startIndex; i < this.pageContents.size() && i < 12 + this.startIndex; i++) {
            int x = screen.leftPos + 51 + ((i - this.startIndex) % 4) * 16;
            int y = screen.topPos + 16 + ((i - this.startIndex) / 4) * 18;

            Page.PageItem item = this.pageContents.get(i);

            boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + 16 && mouseY < y + 18;
            if (hovered) {
                if (!item.selected) guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                guiGraphics.setTooltipForNextFrame(item.tooltip, mouseX, mouseY);
            }

            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    item.selected ? BUTTON_SELECTED : (hovered ? BUTTON_HIGHLIGHTED : BUTTON),
                    x,
                    y,
                    16,
                    18
            );

            item.iconRenderer.render(guiGraphics, x, y + 1);
        }
    }

    private static List<Page.PageItem> getTrimPatternPageContents(SmithingMenuTab instance) {
        if (instance.menu == null) return List.of();
        ArmorTrim trim = instance.menu.getSlot(1).getItem().get(DataComponents.TRIM);

        return instance.trimPatterns.stream().map(template -> {
            @Nullable Holder<@NotNull TrimPattern> pattern = template.getFirst();
            return new Page.PageItem(
                    pattern == null ?
                            Component.translatable("gui.none") :
                            pattern.value().description(),
                    (guiGraphics, x, y) -> {
                        ItemStack itemStack = template.getSecond();
                        if (itemStack.isEmpty())
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, PLACEHOLDER_TRIM, x, y, 16, 16);
                        else guiGraphics.renderItem(itemStack, x, y);
                    },
                    trim == null ?
                            pattern == null :
                            trim.pattern() == template.getFirst()
            );
        }).toList();
    }

    private static List<Page.PageItem> getTrimMaterialPageContents(SmithingMenuTab instance) {
        if (instance.menu == null) return List.of();
        ArmorTrim trim = instance.menu.getSlot(1).getItem().get(DataComponents.TRIM);

        return instance.trimMaterials.stream().map(template -> {
            @Nullable Holder<@NotNull TrimMaterial> material = template.getFirst();
            return new Page.PageItem(
                    material == null ?
                            Component.translatable("gui.none") :
                            material.value().description().copy().withStyle(ChatFormatting.WHITE),
                    (guiGraphics, x, y) -> {
                        ItemStack itemStack = template.getSecond();
                        if (itemStack.isEmpty())
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, PLACEHOLDER_TRIM, x, y, 16, 16);
                        else guiGraphics.renderItem(itemStack, x, y);
                    },
                    trim == null ?
                            material == null :
                            material != null && trim.material().value() == material.value()
            );
        }).toList();
    }

    private static List<Page.PageItem> getItemMaterialPageContents(SmithingMenuTab instance) {
        if (instance.menu == null) return List.of();

        ItemStack itemStack = instance.menu.getSlot(1).getItem();
        List<TagKey<@NotNull Item>> tags = itemStack.getTags().toList();
        if (itemStack.is(Items.TIPPED_ARROW)) {
            tags = new ArrayList<>(tags);
            tags.add(ConventionalItemTags.POTIONS);
        }

        for (TagKey<@NotNull Item> tag : MATERIAL_SWAP_TAGS) {
            if (tags.contains(tag)) {
                List<Item> list = ServerResourceProvider.getFromTag(tag);
                if (tag.equals(ConventionalItemTags.POTIONS)) {
                    list = new ArrayList<>(list);
                    list.add(Items.TIPPED_ARROW);
                }

                return list.stream().map(
                        tagItem -> new Page.PageItem(
                                tagItem.getName(),
                                (guiGraphics, x, y) -> guiGraphics.renderItem(tagItem.getDefaultInstance(), x, y),
                                itemStack.is(tagItem)
                        )
                ).toList();
            }
        }

        return List.of();
    }

    private static List<Runnable> getTrimPatternPageClickActions(SmithingMenuTab instance) {
        if (instance.menu == null) return List.of();
        return instance.trimPatterns.stream().map(template ->
                (Runnable) () -> instance.menu.setTrimPattern(template.getFirst())
        ).toList();
    }

    private static List<Runnable> getTrimMaterialPageClickActions(SmithingMenuTab instance) {
        if (instance.menu == null) return List.of();
        return instance.trimMaterials.stream().map(template ->
                (Runnable) () -> instance.menu.setTrimMaterial(template.getFirst())
        ).toList();
    }

    private static List<Runnable> getItemMaterialPageClickActions(SmithingMenuTab instance) {
        if (instance.menu == null) return List.of();

        ItemStack itemStack = instance.menu.getSlot(1).getItem();
        List<TagKey<@NotNull Item>> tags = itemStack.getTags().toList();
        if (itemStack.is(Items.TIPPED_ARROW)) {
            tags = new ArrayList<>(tags);
            tags.add(ConventionalItemTags.POTIONS);
        }

        for (TagKey<@NotNull Item> tag : MATERIAL_SWAP_TAGS) {
            if (tags.contains(tag)) {
                List<Item> list = ServerResourceProvider.getFromTag(tag);
                if (tag.equals(ConventionalItemTags.POTIONS)) {
                    list = new ArrayList<>(list);
                    list.add(Items.TIPPED_ARROW);
                }

                return list.stream().map(
                        tagItem -> (Runnable) () -> instance.menu.swapBaseItem(tagItem)
                ).toList();
            }
        }

        return List.of();
    }

    private @Nullable Page checkPageClicked(double mouseX, double mouseY) {
        if (this.screen == null) return null;

        List<Page> visiblePages = this.getVisiblePages();
        for (int i = 0; i < visiblePages.size(); i++) {
            Page page = visiblePages.get(i);
            if (page == this.selectedPage) continue;

            int x = this.screen.leftPos + 33;
            int y = this.screen.topPos + 15 + i * 19;
            if (mouseX >= x && mouseY >= y && mouseX < x + 16 && mouseY < y + 18)
                return page;
        }

        return null;
    }

    private @Nullable Runnable checkPageContentsClicked(double mouseX, double mouseY) {
        if (this.screen == null) return null;

        List<Runnable> contents = this.selectedPage.clickActionSupplier.apply(this);
        for (int i = this.startIndex; i < contents.size() && i < 12 + this.startIndex; i++) {
            int x = this.screen.leftPos + 51 + ((i - this.startIndex) % 4) * 16;
            int y = this.screen.topPos + 16 + ((i - this.startIndex) / 4) * 18;
            if (mouseX >= x && mouseY >= y && mouseX < x + 16 && mouseY < y + 18)
                return contents.get(i);
        }

        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent) {
        if (this.screen == null) return false;

        int x = this.screen.leftPos + 118;
        int y = this.screen.topPos + 16;
        if (mouseButtonEvent.x() >= x && mouseButtonEvent.x() < x + 12 && mouseButtonEvent.y() >= y && mouseButtonEvent.y() < y + 54)
            this.scrolling = true;

        if (checkPageClicked(mouseButtonEvent.x(), mouseButtonEvent.y()) != null) return true;
        return checkPageContentsClicked(mouseButtonEvent.x(), mouseButtonEvent.y()) != null;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        this.scrolling = false;

        Page page = checkPageClicked(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (page != null) {
            this.selectedPage = page;
            this.pageContents = this.selectedPage.contentsSupplier.apply(this);
            this.scrollOffs = 0F;
            this.startIndex = 0;
            return true;
        }

        Runnable onClick = checkPageContentsClicked(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (onClick != null) {
            onClick.run();
            return true;
        }

        return false;
    }

    private boolean canHaveArmorTrim(ItemStack itemStack) {
        if (itemStack.isEmpty()) return true;
        for (TagKey<@NotNull Item> tag : ARMOR_TRIM_TAGS) {
            if (itemStack.is(tag)) return true;
        }
        return false;
    }

    private List<Page> getVisiblePages() {
        if (this.menu == null) return List.of(Page.TRIM_PATTERN, Page.TRIM_MATERIAL, Page.ITEM_MATERIAL);
        ItemStack item = this.menu.getSlot(1).getItem();
        if (canHaveArmorTrim(item)) {
            return List.of(Page.TRIM_PATTERN, Page.TRIM_MATERIAL, Page.ITEM_MATERIAL);
        }
        return List.of(Page.ITEM_MATERIAL);
    }

    private ItemStack getTrimPatternTabIcon() {
        if (this.menu == null) return Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultInstance();
        ArmorTrim trim = this.menu.getSlot(1).getItem().get(DataComponents.TRIM);
        if (trim == null) return Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultInstance();
        TrimPattern patternValue = trim.pattern().value();
        for (Pair<Holder.@Nullable Reference<@NotNull TrimPattern>, ItemStack> pair : this.trimPatterns) {
            if (pair.getFirst() != null && pair.getFirst().value() == patternValue) {
                ItemStack icon = pair.getSecond();
                return icon.isEmpty() ? Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultInstance() : icon;
            }
        }
        return Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultInstance();
    }

    private ItemStack getTrimMaterialTabIcon() {
        if (this.menu == null) return Items.IRON_INGOT.getDefaultInstance();
        ArmorTrim trim = this.menu.getSlot(1).getItem().get(DataComponents.TRIM);
        TrimMaterial materialValue = trim != null ? trim.material().value() : null;
        if (materialValue == null) return Items.IRON_INGOT.getDefaultInstance();
        for (Pair<Holder.@Nullable Reference<@NotNull TrimMaterial>, ItemStack> pair : this.trimMaterials) {
            if (pair.getFirst() != null && pair.getFirst().value() == materialValue) {
                ItemStack icon = pair.getSecond();
                return icon.isEmpty() ? Items.IRON_INGOT.getDefaultInstance() : icon;
            }
        }
        return Items.IRON_INGOT.getDefaultInstance();
    }

    private boolean isScrollBarActive() {
        return this.pageContents.size() > 12;
    }

    private int getOffscreenRows() {
        return (this.pageContents.size() - 9) / 4;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent) {
        if (this.screen == null || !this.scrolling || !this.isScrollBarActive())
            return false;

        int top = this.screen.topPos + 16;
        int bottom = top + 54;
        this.scrollOffs = ((float) mouseButtonEvent.y() - top - 7.5F) / (bottom - top - 15F);
        this.scrollOffs = Mth.clamp(this.scrollOffs, 0F, 1F);
        this.startIndex = (int) (this.scrollOffs * this.getOffscreenRows() + 0.5) * 4;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isScrollBarActive()) {
            int offscreenRows = this.getOffscreenRows();
            float deltaY = (float) scrollY / offscreenRows;
            this.scrollOffs = Mth.clamp(this.scrollOffs - deltaY, 0F, 1F);
            this.startIndex = (int) (this.scrollOffs * offscreenRows + 0.5) * 4;
        }

        return true;
    }

    @Override
    public void remove() {
        this.updateItem(ItemStack.EMPTY);
        this.selectedPage = Page.TRIM_PATTERN;
        this.pageContents = List.of();
        this.scrollOffs = 0F;
        this.startIndex = 0;
        super.remove();
    }

    @Override
    public void dispose() {
        this.trimPatterns.clear();
        this.trimMaterials.clear();
        this.selectedPage = Page.TRIM_PATTERN;
        this.pageContents = List.of();
        this.scrollOffs = 0F;
        this.startIndex = 0;
        super.dispose();
    }

    private void updateItem(ItemStack itemStack) {
        if (!this.canHaveArmorTrim(itemStack) && (this.selectedPage == Page.TRIM_PATTERN || this.selectedPage == Page.TRIM_MATERIAL)) {
            this.selectedPage = Page.ITEM_MATERIAL;
        }
        this.pageContents = this.selectedPage.contentsSupplier.apply(this);

        //? if < 1.21.11 {
        /*this.armorStandPreview.leftHandItem.clear();
        *///?} else {
        this.armorStandPreview.leftHandItemStack = ItemStack.EMPTY;
        this.armorStandPreview.leftHandItemState.clear();
        //?}

        this.armorStandPreview.headEquipment = ItemStack.EMPTY;
        this.armorStandPreview.headItem.clear();
        this.armorStandPreview.chestEquipment = ItemStack.EMPTY;
        this.armorStandPreview.legsEquipment = ItemStack.EMPTY;
        this.armorStandPreview.feetEquipment = ItemStack.EMPTY;
        this.armorStandPreview.bodyRot = itemStack.has(DataComponents.GLIDER) ? 30F : 210F;
        if (itemStack.isEmpty()) return;

        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        EquipmentSlot equipmentSlot = equippable != null ? equippable.slot() : EquipmentSlot.OFFHAND;
        ItemModelResolver itemModelResolver = Minecraft.getInstance().getItemModelResolver();
        switch (equipmentSlot) {
            case HEAD -> {
                if (HumanoidArmorLayer.shouldRender(itemStack, EquipmentSlot.HEAD))
                    this.armorStandPreview.headEquipment = itemStack.copy();
                else itemModelResolver.updateForTopItem(
                        this.armorStandPreview.headItem,
                        itemStack,
                        ItemDisplayContext.HEAD,
                        null,
                        null,
                        0
                );
            }
            case CHEST -> this.armorStandPreview.chestEquipment = itemStack.copy();
            case LEGS -> this.armorStandPreview.legsEquipment = itemStack.copy();
            case FEET -> this.armorStandPreview.feetEquipment = itemStack.copy();
            default -> {
                //? if >= 1.21.11
                this.armorStandPreview.leftHandItemStack = itemStack.copy();
                itemModelResolver.updateForTopItem(
                        //? if < 1.21.11 {
                        /*this.armorStandPreview.leftHandItem,
                        *///?} else
                        this.armorStandPreview.leftHandItemState,
                        itemStack,
                        ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                        null,
                        null,
                        0
                );
            }
        }
    }

    private enum Page {
        TRIM_PATTERN(
                Component.translatable("container.creative_crafting_menus.smithing.trim_pattern"),
                SmithingMenuTab::getTrimPatternTabIcon,
                SmithingMenuTab::getTrimPatternPageContents,
                SmithingMenuTab::getTrimPatternPageClickActions
        ),
        TRIM_MATERIAL(
                Component.translatable("container.creative_crafting_menus.smithing.trim_material"),
                SmithingMenuTab::getTrimMaterialTabIcon,
                SmithingMenuTab::getTrimMaterialPageContents,
                SmithingMenuTab::getTrimMaterialPageClickActions
        ),
        ITEM_MATERIAL(
                Component.translatable("container.creative_crafting_menus.smithing.item_material"),
                tab -> Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE.getDefaultInstance(),
                SmithingMenuTab::getItemMaterialPageContents,
                SmithingMenuTab::getItemMaterialPageClickActions
        );

        private final Component tooltip;
        private final Function<SmithingMenuTab, ItemStack> iconSupplier;
        private final Function<SmithingMenuTab, List<PageItem>> contentsSupplier;
        private final Function<SmithingMenuTab, List<Runnable>> clickActionSupplier;

        Page(
                final Component tooltip,
                final Function<SmithingMenuTab, ItemStack> iconSupplier,
                final Function<SmithingMenuTab, List<PageItem>> contentsSupplier,
                final Function<SmithingMenuTab, List<Runnable>> clickActionSupplier
        ) {
            this.tooltip = tooltip;
            this.iconSupplier = iconSupplier;
            this.clickActionSupplier = clickActionSupplier;
            this.contentsSupplier = contentsSupplier;
        }

        private ItemStack getIcon(SmithingMenuTab tab) {
            return this.iconSupplier.apply(tab);
        }

        private interface RenderFunction {
            void render(GuiGraphics guiGraphics, int x, int y);
        }

        private record PageItem(Component tooltip, RenderFunction iconRenderer, boolean selected) {}
    }

    public class SmithingTabMenu extends CreativeTabMenu<SmithingTabMenu> {
        private final Container inputSlots;
        private final ResultContainer resultSlots = new ResultContainer();

        SmithingTabMenu(Player player) {
            super(player);
            this.inputSlots = MenuHelper.simpleContainer(this, 1);
            this.addSlot(new Slot(this.inputSlots, 0, 9, 16));
            this.addSlot(MenuHelper.resultSlot(this, this.resultSlots, 0, 9, 54));
        }

        @Override
        SmithingTabMenu copyWithPlayer(@NotNull Player player) {
            return this.copyContentsTo(new SmithingTabMenu(player));
        }

        @Override
        public @NotNull ItemStack quickMoveFromInventory(@NotNull Player player, int slotIndex) {
            ItemStack resultStack = ItemStack.EMPTY;
            Slot slot = this.player.inventoryMenu.slots.get(slotIndex);
            if (!slot.hasItem()) return resultStack;

            ItemStack slotStack = slot.getItem();
            resultStack = slotStack.copy();

            if (!this.moveItemStackTo(slotStack, 0, 1, false))
                return ItemStack.EMPTY;

            if (slotStack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (slotStack.getCount() == resultStack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, slotStack);

            return resultStack;
        }

        private @Nullable ArmorTrim getArmorTrim() {
            return Optional.ofNullable(this.resultSlots.getItem(0).get(DataComponents.TRIM))
                    .orElse(this.inputSlots.getItem(0).get(DataComponents.TRIM));
        }

        private void setTrimPattern(@Nullable Holder<@NotNull TrimPattern> pattern) {
            ItemStack result = this.resultSlots.getItem(0);
            if (result.isEmpty()) return;

            if (pattern == null) result.remove(DataComponents.TRIM);
            else {
                ArmorTrim armorTrim = this.getArmorTrim();

                Holder<@NotNull TrimMaterial> material = armorTrim == null ?
                        ServerResourceProvider.getRegistryElements(Registries.TRIM_MATERIAL).getFirst() :
                        armorTrim.material();

                result.applyComponents(DataComponentMap.builder().set(
                        DataComponents.TRIM,
                        new ArmorTrim(material, pattern)
                ).build());
            }

            SmithingMenuTab.this.updateItem(result);
        }

        private void setTrimMaterial(@Nullable Holder<@NotNull TrimMaterial> material) {
            ItemStack result = this.resultSlots.getItem(0);
            if (result.isEmpty()) return;

            if (material == null) result.remove(DataComponents.TRIM);
            else {
                ArmorTrim armorTrim = this.getArmorTrim();

                Holder<@NotNull TrimPattern> pattern = armorTrim == null ?
                        ServerResourceProvider.getRegistryElements(Registries.TRIM_PATTERN).getFirst() :
                        armorTrim.pattern();

                result.applyComponents(DataComponentMap.builder().set(
                        DataComponents.TRIM,
                        new ArmorTrim(material, pattern)
                ).build());
            }

            SmithingMenuTab.this.updateItem(result);
        }

        private void swapBaseItem(Item base) {
            ItemStack result = this.resultSlots.getItem(0);
            if (result.isEmpty()) return;

            ItemStack swapped = base.getDefaultInstance();
            swapped.applyComponents(result.getComponentsPatch());

            this.resultSlots.setItem(0, swapped);
            SmithingMenuTab.this.updateItem(swapped);
        }

        @Override
        public void slotsChanged(@NotNull Container container) {
            if (container == this.inputSlots) {
                ItemStack result = this.inputSlots.getItem(0).copy();
                this.resultSlots.setItem(0, result);
                SmithingMenuTab.this.updateItem(result);
            } else if (container == this.resultSlots) {
                this.inputSlots.clearContent();
                SmithingMenuTab.this.updateItem(ItemStack.EMPTY);
            }
        }

        @Override
        public void removed(@NotNull Player player) {
            if (player.hasInfiniteMaterials()) for (int i = 0; i < this.inputSlots.getContainerSize(); i++)
                player.getInventory().placeItemBackInInventory(this.inputSlots.removeItemNoUpdate(i));
        }
    }
}
