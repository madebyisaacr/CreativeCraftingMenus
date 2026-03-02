package dev.chililisoup.creativecraftingmenus.gui;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.chililisoup.creativecraftingmenus.CreativeCraftingMenus;
import dev.chililisoup.creativecraftingmenus.gui.components.DropdownSelector;
import dev.chililisoup.creativecraftingmenus.gui.components.NameLoreEditBox;
import dev.chililisoup.creativecraftingmenus.util.MenuHelper;
import dev.chililisoup.creativecraftingmenus.util.FullTextParser;
import dev.chililisoup.creativecraftingmenus.util.ServerResourceProvider;
import dev.chililisoup.creativecraftingmenus.util.VersionHelper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//? if < 1.21.11 {
/*import net.minecraft.Util;
 *///?} else {
import net.minecraft.util.Util;
//?}

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.minecraft.client.gui.screens.inventory.StonecutterScreen.SCROLLER_DISABLED_SPRITE;
import static net.minecraft.client.gui.screens.inventory.StonecutterScreen.SCROLLER_SPRITE;

public class AnvilMenuTab extends CreativeMenuTab<AnvilMenuTab.AnvilTabMenu> {
    protected static final Identifier DELETE_BUTTON = CreativeCraftingMenus.id("widget/delete_button");
    protected static final Identifier DELETE_BUTTON_HIGHLIGHTED = CreativeCraftingMenus.id("widget/delete_button_highlighted");
    protected static final Identifier ADD_ANVIL_BUTTON = CreativeCraftingMenus.id("widget/add_anvil_button");
    protected static final Identifier ADD_ANVIL_BUTTON_HIGHLIGHTED = CreativeCraftingMenus.id("widget/add_anvil_button_highlighted");

    private Page.RenderFunction pageRenderer = Page.RenderFunction.EMPTY;
    private Page selectedPage = Page.NAME_LORE;
    private float scrollOffs;
    private boolean scrolling;
    private int startIndex;
    private @Nullable NameLoreEditBox nameLoreBox;
    private @Nullable DropdownSelector<Holder<Enchantment>> enchantSelector;

    public AnvilMenuTab(Component displayName, Supplier<ItemStack> iconGenerator, String id) {
        super(displayName, iconGenerator, id);
    }

    @Override
    AnvilTabMenu createMenu(Player player) {
        return new AnvilTabMenu(player);
    }

    @Override
    public void subInit() {
        this.scrolling = false;
        if (this.screen == null) return;
        if (this.nameLoreBox != null) this.screen.removeWidget(this.nameLoreBox);
        if (this.enchantSelector != null) this.screen.removeWidget(this.enchantSelector);

        this.nameLoreBox = new NameLoreEditBox(
                screen.getFont(),
                screen.leftPos + 51,
                screen.topPos + 14,
                123,
                56
        );

        this.nameLoreBox.setValueListener(this::onNameLoreChanged);
        this.screen.addRenderableWidget(this.nameLoreBox);

        this.enchantSelector = new DropdownSelector<>(
                screen.leftPos + 63,
                screen.leftPos + 51,
                screen.topPos + 14,
                111,
                14,
                56
        );
        this.screen.addRenderableWidget(this.enchantSelector);

        this.update();
    }

    @Override
    public void drawTitle(TitleDrawer titleDrawer, int x, int y, int color) {
        super.drawTitle(titleDrawer, x, y - 2, color);
    }

    @Override
    public void render(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (this.menu == null || this.nameLoreBox == null) return;

        for (int i = 0; i < Page.values().length; i++) {
            Page page = Page.values()[i];
            int x = screen.leftPos + 33;
            int y = screen.topPos + 13 + i * 20;
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

            guiGraphics.renderItem(page.icon, x, y + 1);
        }

        if (this.selectedPage != Page.NAME_LORE && (this.enchantSelector == null || !this.enchantSelector.visible || !this.enchantSelector.open))
            this.renderScrollBar(screen, guiGraphics, mouseX, mouseY);
        this.renderPageContents(screen, guiGraphics, mouseX, mouseY);
    }

    private void renderScrollBar(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = screen.leftPos + 177;
        int y = screen.topPos + 14 + (int) (41F * this.scrollOffs);

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

        int left = screen.leftPos + 51;
        int top = screen.topPos + 14;

        this.pageRenderer.render(guiGraphics, left, top, mouseX, mouseY);
    }

    private static Page.RenderFunction getNameLorePageRenderer(AnvilMenuTab instance) {
        if (instance.menu == null) return Page.RenderFunction.EMPTY;

        return (guiGraphics, left, top, mouseX, mouseY) -> {
            int x = left + 113;
            int y = top - 11;

            boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + 11 && mouseY < y + 11;
            if (hovered) {
                guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                guiGraphics.setComponentTooltipForNextFrame(
                        Minecraft.getInstance().font,
                        List.of(
                                Component.translatable("container.creative_crafting_menus.anvil.name_lore.help.1"),
                                Component.translatable("container.creative_crafting_menus.anvil.name_lore.help.2")
                        ),
                        mouseX,
                        mouseY
                );
            }

            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    hovered ? BUTTON_HIGHLIGHTED : BUTTON,
                    x,
                    y,
                    11,
                    11
            );

            guiGraphics.drawString(Minecraft.getInstance().font, "?", x + 3, y + 2, -1);
        };
    }

    private static Page.RenderFunction getEnchantmentsPageRenderer(AnvilMenuTab instance) {
        if (instance.menu == null || instance.enchantSelector == null || instance.menu.resultSlots.isEmpty())
            return Page.RenderFunction.EMPTY;

        ArrayList<Object2IntMap.@Nullable Entry<Holder<Enchantment>>> enchants =
                new ArrayList<>(instance.menu.getSortedEnchantments());
        enchants.add(null);

        return (guiGraphics, left, top, mouseX, mouseY) -> {
            if (instance.enchantSelector.visible && instance.enchantSelector.open)
                return;

            instance.enchantSelector.visible = false;
            for (int i = instance.startIndex; i < enchants.size() && i < 4 + instance.startIndex; i++) {
                int y = top + (i - instance.startIndex) * 14;

                Object2IntMap.@Nullable Entry<Holder<Enchantment>> enchant = enchants.get(i);
                Component label = enchant != null ?
                        enchant.getKey().value().description() :
                        Component.translatable("container.creative_crafting_menus.anvil.add_enchantment");

                boolean anyHovered = mouseX >= left && mouseY >= y && mouseX < left + 124 && mouseY < y + 14;
                boolean addDeleteHovered = anyHovered && mouseX < left + 12;

                if (enchant != null) {
                    int level = enchant.getIntValue();
                    boolean upVisible = level < 255;
                    boolean downVisible = level > 1;

                    boolean labelHovered = anyHovered && !addDeleteHovered && mouseX < left + 93;
                    boolean levelButtonHovered = anyHovered && !addDeleteHovered && mouseX >= left + 93 && mouseX < left + 113;
                    boolean upHovered = upVisible && anyHovered && !addDeleteHovered &&
                            mouseX >= left + 114 && mouseX < left + 123 && mouseY >= y && mouseY < y + 7;
                    boolean downHovered = downVisible && anyHovered && !addDeleteHovered &&
                            mouseX >= left + 114 && mouseX < left + 123 && mouseY >= y + 7 && mouseY < y + 14;

                    if (anyHovered) {
                        if (labelHovered) {
                            guiGraphics.setTooltipForNextFrame(label, mouseX, mouseY);
                        } else if (addDeleteHovered) {
                            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                            guiGraphics.setTooltipForNextFrame(
                                    Component.translatable("container.creative_crafting_menus.anvil.remove_enchantment"),
                                    mouseX,
                                    mouseY
                            );
                        } else if (upHovered || downHovered) {
                            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                            guiGraphics.setTooltipForNextFrame(
                                    Component.translatable(
                                            "container.creative_crafting_menus.anvil." + (upHovered ? "increase" : "decrease") + "_enchantment_level"
                                    ),
                                    mouseX,
                                    mouseY
                            );
                        } else if (levelButtonHovered) {
                            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                        }
                    }

                    guiGraphics.blitSprite(
                            RenderPipelines.GUI_TEXTURED,
                            addDeleteHovered ? DELETE_BUTTON_HIGHLIGHTED : DELETE_BUTTON,
                            left,
                            y,
                            12,
                            14
                    );

                    guiGraphics.blitSprite(
                            RenderPipelines.GUI_TEXTURED,
                            levelButtonHovered ? BUTTON_HIGHLIGHTED : BUTTON,
                            left + 93,
                            y,
                            20,
                            14
                    );

                    if (upVisible) {
                        guiGraphics.blitSprite(
                                RenderPipelines.GUI_TEXTURED,
                                upHovered ? ARROW_UP_HIGHLIGHTED : ARROW_UP,
                                left + 114,
                                y + 1,
                                9,
                                6
                        );
                    }

                    if (downVisible) {
                        guiGraphics.blitSprite(
                                RenderPipelines.GUI_TEXTURED,
                                downHovered ? ARROW_DOWN_HIGHLIGHTED : ARROW_DOWN,
                                left + 114,
                                y + 7,
                                9,
                                6
                        );
                    }

                    guiGraphics.drawCenteredString(
                            Minecraft.getInstance().font,
                            String.valueOf(level),
                            left + 103,
                            y + 3,
                            -1
                    );

                    VersionHelper.drawScrollingString(
                            guiGraphics,
                            label,
                            left + 15,
                            left + 15,
                            left + 90,
                            y,
                            y + 14
                    );
                } else {
                    instance.enchantSelector.visible = true;
                    instance.enchantSelector.setY(y);

                    if (addDeleteHovered) {
                        guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                        guiGraphics.setTooltipForNextFrame(label, mouseX, mouseY);
                    }

                    guiGraphics.blitSprite(
                            RenderPipelines.GUI_TEXTURED,
                            addDeleteHovered ? ADD_ANVIL_BUTTON_HIGHLIGHTED : ADD_ANVIL_BUTTON,
                            left,
                            y,
                            12,
                            14
                    );
                }
            }
        };
    }

    private static @Nullable Runnable checkNameLorePageClicked(AnvilMenuTab instance, int left, int top, MouseButtonEvent mouseButtonEvent) {
        if (instance.menu == null) return null;

        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();
        int x = left + 113;
        int y = top - 11;

        if (mouseX >= x && mouseY >= y && mouseX < x + 11 && mouseY < y + 11) return () -> {
            URI url = URI.create("https://placeholders.pb4.eu/user/quicktext/");

            Minecraft.getInstance().setScreen(new ConfirmLinkScreen(open -> {
                if (open) Util.getPlatform().openUri(url);
                Minecraft.getInstance().setScreen(instance.screen);
            }, url.toString(), false));
        };

        return null;
    }

    private static @Nullable Runnable checkEnchantmentsPageClicked(AnvilMenuTab instance, int left, int top, MouseButtonEvent mouseButtonEvent) {
        if (instance.menu == null || instance.enchantSelector == null || instance.menu.resultSlots.isEmpty())
            return null;

        if (instance.enchantSelector.visible && instance.enchantSelector.open)
            return null;

        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();

        ArrayList<Object2IntMap.@Nullable Entry<Holder<Enchantment>>> enchants =
                new ArrayList<>(instance.menu.getSortedEnchantments());
        enchants.add(null);

        for (int i = instance.startIndex; i < enchants.size() && i < 4 + instance.startIndex; i++) {
            int y = top + (i - instance.startIndex) * 14;
            Object2IntMap.@Nullable Entry<Holder<Enchantment>> enchant = enchants.get(i);

            boolean anyHovered = mouseX >= left && mouseY >= y && mouseX < left + 124 && mouseY < y + 14;
            if (!anyHovered) continue;
            boolean addDeleteHovered = mouseX < left + 12;

            if (enchant != null) {
                int level = enchant.getIntValue();
                boolean upVisible = level < 255;
                boolean downVisible = level > 1;

                boolean levelButtonHovered = !addDeleteHovered && mouseX >= left + 93 && mouseX < left + 113;
                boolean upHovered = upVisible && !addDeleteHovered &&
                        mouseX >= left + 114 && mouseX < left + 123 && mouseY >= y && mouseY < y + 7;
                boolean downHovered = downVisible && !addDeleteHovered &&
                        mouseX >= left + 114 && mouseX < left + 123 && mouseY >= y + 7 && mouseY < y + 14;

                if (addDeleteHovered) return () -> instance.menu.removeEnchantment(enchant.getKey());
                else if (upHovered || downHovered) return () -> {
                    int change = (upHovered ? 1 : -1) * (mouseButtonEvent.hasShiftDown() ? 10 : 1);
                    instance.menu.setEnchantment(enchant.getKey(), Math.max(level + change, 1));
                };
                else if (levelButtonHovered) return () -> {
                    int change = (mouseButtonEvent.button() == 1 ? -1 : 1) * (mouseButtonEvent.hasShiftDown() ? 10 : 1);
                    instance.menu.setEnchantment(enchant.getKey(), Math.max(level + change, 1));
                };
            } else if (addDeleteHovered) return () -> {
                Holder<Enchantment> selected = instance.enchantSelector.value();
                if (selected != null) instance.menu.addEnchantment(selected);
            };
        }

        return null;
    }

    private @Nullable Page checkPageClicked(double mouseX, double mouseY) {
        if (this.screen == null) return null;

        for (int i = 0; i < Page.values().length; i++) {
            Page page = Page.values()[i];
            if (page == this.selectedPage) continue;

            int x = this.screen.leftPos + 33;
            int y = this.screen.topPos + 13 + i * 20;
            if (mouseX >= x && mouseY >= y && mouseX < x + 16 && mouseY < y + 18)
                return page;
        }

        return null;
    }

    private @Nullable Runnable checkPageContentsClicked(MouseButtonEvent mouseButtonEvent) {
        return this.screen != null ? this.selectedPage.clickChecker.check(
                this,
                this.screen.leftPos + 51,
                this.screen.topPos + 14,
                mouseButtonEvent
        ) : null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent) {
        if (this.screen == null) return false;

        int x = this.screen.leftPos + 177;
        int y = this.screen.topPos + 14;
        if (mouseButtonEvent.x() >= x && mouseButtonEvent.x() < x + 12 && mouseButtonEvent.y() >= y && mouseButtonEvent.y() < y + 56)
            this.scrolling = true;

        if (checkPageClicked(mouseButtonEvent.x(), mouseButtonEvent.y()) != null) return true;
        return checkPageContentsClicked(mouseButtonEvent) != null;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        this.scrolling = false;
        if (this.menu == null) return false;

        Page page = checkPageClicked(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (page != null) {
            this.selectedPage = page;
            this.scrollOffs = 0F;
            this.update();
            return true;
        }

        Runnable onClick = checkPageContentsClicked(mouseButtonEvent);
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
        if (this.screen == null || !this.scrolling || !this.isScrollBarActive() || this.selectedPage == Page.NAME_LORE)
            return false;

        if (this.enchantSelector != null && this.enchantSelector.visible && this.enchantSelector.open)
            return false;

        int top = this.screen.topPos + 14;
        int bottom = top + 56;
        this.scrollOffs = ((float) mouseButtonEvent.y() - top - 7.5F) / (bottom - top - 15F);
        this.scrollOffs = Mth.clamp(this.scrollOffs, 0F, 1F);
        this.startIndex = (int) (this.scrollOffs * this.getOffscreenRows() + 0.5) * this.selectedPage.getColumns.apply(this);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.selectedPage == Page.NAME_LORE)
            return this.nameLoreBox != null && this.nameLoreBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

        if (this.enchantSelector != null && this.enchantSelector.visible && this.enchantSelector.open)
            return this.enchantSelector.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

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
        if (keyEvent.isEscape() || this.screen == null || this.nameLoreBox == null) return false;
        return this.nameLoreBox.keyPressed(keyEvent) || (this.nameLoreBox.isActive() && this.nameLoreBox.isFocused() && this.nameLoreBox.visible);
    }

    private void onNameLoreChanged(String nameLore) {
        if (this.selectedPage == Page.NAME_LORE && this.menu != null)
            this.menu.setNameLore(nameLore);
    }

    @Override
    public void remove() {
        this.selectedPage = Page.NAME_LORE;
        this.pageRenderer = Page.RenderFunction.EMPTY;
        this.scrollOffs = 0F;
        this.startIndex = 0;
        if (this.screen != null) {
            if (this.nameLoreBox != null) this.screen.removeWidget(this.nameLoreBox);
            if (this.enchantSelector != null) this.screen.removeWidget(this.enchantSelector);
        }
        this.nameLoreBox = null;
        this.enchantSelector = null;
        super.remove();
    }

    @Override
    public void dispose() {
        this.selectedPage = Page.NAME_LORE;
        this.pageRenderer = Page.RenderFunction.EMPTY;
        this.scrollOffs = 0F;
        this.startIndex = 0;
        super.dispose();
    }

    private void update() {
        if (this.menu == null || this.nameLoreBox == null || this.enchantSelector == null || this.screen == null)
            return;

        this.startIndex = (int) (this.scrollOffs * this.getOffscreenRows() + 0.5) * this.selectedPage.getColumns.apply(this);
        this.pageRenderer = this.selectedPage.rendererSupplier.apply(this);

        this.nameLoreBox.visible = this.selectedPage == Page.NAME_LORE;
        if (!this.nameLoreBox.visible && this.screen.getFocused() == this.nameLoreBox)
            this.screen.clearFocus();

        String nameLore = this.menu.getNameLore();
        this.nameLoreBox.setValue(nameLore == null ? "" : nameLore);
        this.nameLoreBox.isEditable = nameLore != null;

        this.enchantSelector.updateEntries(
                ServerResourceProvider.getRegistryElements(Registries.ENCHANTMENT).stream().filter(enchant ->
                        !this.menu.hasEnchantment(enchant)
                ).map(enchant ->
                        new DropdownSelector.Entry<>(enchant.value().description(), (Holder<Enchantment>) enchant)
                ).toList()
        );
        this.enchantSelector.visible = false;
        this.enchantSelector.open = false;
    }

    private enum Page {
        NAME_LORE(
                Component.translatable("container.creative_crafting_menus.anvil.name_lore"),
                Items.OAK_SIGN.getDefaultInstance(),
                AnvilMenuTab::getNameLorePageRenderer,
                AnvilMenuTab::checkNameLorePageClicked,
                instance -> 0,
                instance -> 1
        ),
        ENCHANTMENTS(
                Component.translatable("container.creative_crafting_menus.anvil.enchantments"),
                Items.ENCHANTED_BOOK.getDefaultInstance(),
                AnvilMenuTab::getEnchantmentsPageRenderer,
                AnvilMenuTab::checkEnchantmentsPageClicked,
                instance -> instance.menu != null ?
                        instance.menu.resultSlots.getItem(0).getOrDefault(
                                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY
                        ).entrySet().size() - 3 : 0,
                instance -> 1
        );

        private final Component tooltip;
        private final ItemStack icon;
        private final Function<AnvilMenuTab, RenderFunction> rendererSupplier;
        private final ClickChecker clickChecker;
        private final Function<AnvilMenuTab, Integer> getOffscreenRows;
        private final Function<AnvilMenuTab, Integer> getColumns;

        Page(
                final Component tooltip,
                final ItemStack icon,
                final Function<AnvilMenuTab, RenderFunction> rendererSupplier,
                final ClickChecker clickChecker,
                final Function<AnvilMenuTab, Integer> getOffscreenRows,
                final Function<AnvilMenuTab, Integer> getColumns
                ) {
            this.tooltip = tooltip;
            this.icon = icon;
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
            @Nullable Runnable check(AnvilMenuTab instance, int left, int top, MouseButtonEvent mouseButtonEvent);
        }
    }

    public class AnvilTabMenu extends CreativeTabMenu<AnvilTabMenu> {
        private final Container inputSlots;
        private final ResultContainer resultSlots = new ResultContainer();

        AnvilTabMenu(Player player) {
            super(player);
            this.inputSlots = MenuHelper.simpleContainer(this, 1);
            this.addSlot(new Slot(this.inputSlots, 0, 9, 14));
            this.addSlot(MenuHelper.resultSlot(this, this.resultSlots, 0, 9, 54));
        }

        @Override
        AnvilTabMenu copyWithPlayer(@NotNull Player player) {
            return this.copyContentsTo(new AnvilTabMenu(player));
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

        public @Nullable String getNameLore() {
            ItemStack itemStack = this.resultSlots.getItem(0);
            if (itemStack.isEmpty()) return null;

            ArrayList<String> nameLore = new ArrayList<>();

            Component customName = itemStack.getCustomName();
            Component itemName = itemStack.getItemName();
            Component name = customName == null ?
                    itemName.copy().withStyle(itemName.getStyle().withItalic(
                            itemName.getStyle().italic != null && itemName.getStyle().italic
                    )) :
                    customName;
            nameLore.add(FullTextParser.decompose(name));

            ItemLore lore = itemStack.get(DataComponents.LORE);
            if (lore != null) lore.lines().forEach(line -> nameLore.add(FullTextParser.decompose(line)));

            return String.join("\n", nameLore);
        }

        public void setNameLore(String nameLore) {
            ItemStack itemStack = this.resultSlots.getItem(0);
            if (itemStack.isEmpty()) return;

            if (StringUtil.isBlank(nameLore)) {
                itemStack.remove(DataComponents.CUSTOM_NAME);
                itemStack.set(DataComponents.LORE, ItemLore.EMPTY);
                return;
            }

            ArrayList<String> lines = new ArrayList<>(List.of(nameLore.split("\n")));
            if (lines.isEmpty()) return;

            String name = lines.removeFirst();
            ItemStack inputStack = this.inputSlots.getItem(0);
            itemStack.set(
                    DataComponents.CUSTOM_NAME,
                    inputStack.getHoverName().getString().equals(name) ?
                            inputStack.get(DataComponents.CUSTOM_NAME) :
                            FullTextParser.formatText(name)
            );

            if (lines.isEmpty()) itemStack.set(DataComponents.LORE, ItemLore.EMPTY);
            else {
                ItemLore oldLore = inputStack.get(DataComponents.LORE);
                List<Component> oldLines = oldLore != null ? oldLore.lines() : List.of();

                ArrayList<Component> lore = new ArrayList<>();
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (oldLines.size() > i) {
                        Component oldLine = oldLore.lines().get(i);
                        lore.add(oldLine.getString().equals(line) ? oldLine : FullTextParser.formatText(line));
                    } else lore.add(FullTextParser.formatText(line));
                }

                itemStack.set(DataComponents.LORE, new ItemLore(lore));
            }
        }

        private static HolderSet<Enchantment> getTagOrEmpty(@Nullable HolderLookup.Provider provider) {
            if (provider != null) {
                Optional<HolderSet.Named<Enchantment>> optional = provider.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.TOOLTIP_ORDER);
                if (optional.isPresent()) return optional.get();
            }

            return HolderSet.direct();
        }

        public List<Object2IntMap.@Nullable Entry<Holder<Enchantment>>> getSortedEnchantments() {
            ItemEnchantments enchants = this.resultSlots.getItem(0).get(DataComponents.ENCHANTMENTS);
            if (enchants == null) return List.of();

            ArrayList<Object2IntMap.@Nullable Entry<Holder<Enchantment>>> sorted = new ArrayList<>();
            HolderSet<Enchantment> sortedEnchantSet = getTagOrEmpty(ServerResourceProvider.registryAccess());

            for (Holder<Enchantment> enchant : sortedEnchantSet) {
                int level = enchants.getLevel(enchant);
                if (enchants.getLevel(enchant) > 0) sorted.add(
                        //? if < 1.21.11 {
                        /*(Object2IntMap.Entry<Holder<Enchantment>>) Map
                        *///?} else
                        Object2IntMap
                        .entry(enchant, level)
                );
            }

            for (Object2IntMap.Entry<Holder<Enchantment>> enchant : enchants.entrySet())
                if (!sortedEnchantSet.contains(enchant.getKey())) sorted.add(enchant);

            return sorted;
        }

        public boolean hasEnchantment(Holder<Enchantment> enchant) {
            ItemEnchantments enchants = this.resultSlots.getItem(0).get(DataComponents.ENCHANTMENTS);
            if (enchants == null) return false;
            for (Object2IntMap.Entry<Holder<Enchantment>> contained : enchants.entrySet())
                if (contained.getKey().equals(enchant)) return true;
            return false;
        }

        public void setEnchantment(Holder<Enchantment> enchant, int level) {
            ItemStack itemStack = this.resultSlots.getItem(0);
            ItemEnchantments enchants = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchants);
            mutable.set(enchant, level);

            itemStack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
            AnvilMenuTab.this.update();
        }

        public void addEnchantment(Holder<Enchantment> enchant) {
            this.setEnchantment(enchant, enchant.value().getMaxLevel());
        }

        public void removeEnchantment(Holder<Enchantment> enchant) {
            this.setEnchantment(enchant, 0);
        }

        @Override
        public void slotsChanged(@NotNull Container container) {
            if (container == this.inputSlots) {
                ItemStack result = this.inputSlots.getItem(0).copy();
                this.resultSlots.setItem(0, result);
                AnvilMenuTab.this.update();
            } else if (container == this.resultSlots) {
                this.inputSlots.clearContent();
                AnvilMenuTab.this.update();
            }
        }

        @Override
        public void removed(@NotNull Player player) {
            if (player.hasInfiniteMaterials()) for (int i = 0; i < this.inputSlots.getContainerSize(); i++)
                player.getInventory().placeItemBackInInventory(this.inputSlots.removeItemNoUpdate(i));
        }
    }
}
