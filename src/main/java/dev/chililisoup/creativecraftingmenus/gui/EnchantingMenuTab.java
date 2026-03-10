package dev.chililisoup.creativecraftingmenus.gui;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.chililisoup.creativecraftingmenus.CreativeCraftingMenus;
import dev.chililisoup.creativecraftingmenus.gui.components.DropdownSelector;
import dev.chililisoup.creativecraftingmenus.util.MenuHelper;
import dev.chililisoup.creativecraftingmenus.util.ServerResourceProvider;
import dev.chililisoup.creativecraftingmenus.util.VersionHelper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

import static net.minecraft.client.gui.screens.inventory.StonecutterScreen.SCROLLER_DISABLED_SPRITE;
import static net.minecraft.client.gui.screens.inventory.StonecutterScreen.SCROLLER_SPRITE;
import static net.minecraft.client.gui.screens.inventory.LoomScreen.*;

public class EnchantingMenuTab extends CreativeMenuTab<EnchantingMenuTab.EnchantingTabMenu> {
    private static final int LIST_LEFT = 32;
    private static final int LIST_WIDTH = 133;
    private static final int SELECTOR_WIDTH = LIST_WIDTH + 9;

    protected static final Identifier DELETE_BUTTON = CreativeCraftingMenus.id("widget/delete_button");
    protected static final Identifier DELETE_BUTTON_HIGHLIGHTED = CreativeCraftingMenus.id("widget/delete_button_highlighted");
    protected static final Identifier ADD_ENCHANTING_BUTTON = CreativeCraftingMenus.id("widget/add_anvil_button");
    protected static final Identifier ADD_ENCHANTING_BUTTON_HIGHLIGHTED = CreativeCraftingMenus.id("widget/add_anvil_button_highlighted");

    private float scrollOffs;
    private boolean scrolling;
    private int startIndex;
    private @Nullable DropdownSelector<Holder<Enchantment>> enchantSelector;

    private static Component getEnchantmentLevelComponent(int level) {
        if (level >= 1 && level <= 10) {
            return Component.translatable("enchantment.level." + level);
        }
        return Component.literal(String.valueOf(level));
    }

    public EnchantingMenuTab(Component displayName, Supplier<ItemStack> iconGenerator, String id) {
        super(displayName, iconGenerator, id);
    }

    @Override
    public boolean keepInputOnTabSwitch() {
        return true;
    }

    @Override
    public ItemStack extractInputItem() {
        if (this.menu == null) return ItemStack.EMPTY;
        EnchantingTabMenu m = this.menu;
        ItemStack stack = m.inputSlots.removeItemNoUpdate(0);
        if (!stack.isEmpty()) m.slotsChanged(m.inputSlots);
        return stack;
    }

    @Override
    public boolean acceptInputItem(ItemStack stack) {
        if (this.menu == null || stack.isEmpty()) return false;
        EnchantingTabMenu m = this.menu;
        m.inputSlots.setItem(0, stack.copy());
        m.slotsChanged(m.inputSlots);
        return true;
    }

    @Override
    EnchantingTabMenu createMenu(Player player) {
        return new EnchantingTabMenu(player);
    }

    @Override
    public void subInit() {
        this.scrolling = false;
        if (this.screen == null) return;
        if (this.enchantSelector != null) this.screen.removeWidget(this.enchantSelector);

        this.enchantSelector = new DropdownSelector<>(
                screen.leftPos + LIST_LEFT + 12,
                screen.leftPos + LIST_LEFT,
                screen.topPos + 14,
                SELECTOR_WIDTH - 12,
                14,
                56
        );
        this.enchantSelector.setPlaceholder(Component.translatable(
                "container.creative_crafting_menus.enchanting.add_enchantment"
        ));
        this.enchantSelector.setSelectionCallback(enchant -> {
            if (this.menu != null) this.menu.addEnchantment(enchant);
        });
        this.screen.addRenderableWidget(this.enchantSelector);

        this.update();
    }

    @Override
    public void drawTitle(TitleDrawer titleDrawer, int x, int y, int color) {
        super.drawTitle(titleDrawer, x, y - 2, color);
    }

    @Override
    public void render(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (this.menu == null) return;

        if (this.enchantSelector == null || !this.enchantSelector.visible || !this.enchantSelector.open)
            this.renderScrollBar(screen, guiGraphics, mouseX, mouseY);
        this.renderEnchantments(screen, guiGraphics, mouseX, mouseY);
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

    private void renderEnchantments(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.menu == null || this.enchantSelector == null || this.menu.resultSlots.isEmpty())
            return;

        int left = screen.leftPos + LIST_LEFT;
        int top = screen.topPos + 14;

        ArrayList<Object2IntMap.@Nullable Entry<Holder<Enchantment>>> enchants =
                new ArrayList<>(this.menu.getSortedEnchantments());
        enchants.add(null);

        if (this.enchantSelector.visible && this.enchantSelector.open)
            return;

        this.enchantSelector.visible = false;
        for (int i = this.startIndex; i < enchants.size() && i < 4 + this.startIndex; i++) {
            int y = top + (i - this.startIndex) * 14;

            Object2IntMap.@Nullable Entry<Holder<Enchantment>> enchant = enchants.get(i);
            Component label = enchant != null ?
                    enchant.getKey().value().description() :
                    Component.translatable("container.creative_crafting_menus.enchanting.add_enchantment");

            boolean anyHovered = mouseX >= left && mouseY >= y && mouseX < left + SELECTOR_WIDTH && mouseY < y + 14;
            boolean addDeleteHovered = anyHovered && mouseX < left + 12;

            if (enchant != null) {
                int level = enchant.getIntValue();
                boolean upVisible = level < 255;
                boolean downVisible = level > 1;

                boolean upHovered = upVisible && !addDeleteHovered &&
                        mouseX >= left + LIST_WIDTH && mouseX < left + LIST_WIDTH + 9 &&
                        mouseY >= y && mouseY < y + 7;
                boolean downHovered = downVisible && !addDeleteHovered &&
                        mouseX >= left + LIST_WIDTH && mouseX < left + LIST_WIDTH + 9 &&
                        mouseY >= y + 7 && mouseY < y + 14;
                boolean labelHovered = anyHovered && !addDeleteHovered && !upHovered && !downHovered &&
                        mouseX < left + LIST_WIDTH - 20;

                Component tooltipLabel = label.copy().append(" ").append(getEnchantmentLevelComponent(level));

                if (anyHovered) {
                    if (addDeleteHovered || upHovered || downHovered) guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                    if (upHovered) {
                        guiGraphics.setTooltipForNextFrame(
                                Component.translatable("container.creative_crafting_menus.enchanting.increase_level"),
                                mouseX,
                                mouseY
                        );
                    } else if (downHovered) {
                        guiGraphics.setTooltipForNextFrame(
                                Component.translatable("container.creative_crafting_menus.enchanting.decrease_level"),
                                mouseX,
                                mouseY
                        );
                    } else if (addDeleteHovered || labelHovered) {
                        guiGraphics.setTooltipForNextFrame(
                                addDeleteHovered
                                        ? Component.translatable("container.creative_crafting_menus.enchanting.remove_enchantment")
                                        : tooltipLabel,
                                mouseX,
                                mouseY
                        );
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

                if (upVisible) guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        upHovered ? ARROW_UP_HIGHLIGHTED : ARROW_UP,
                        left + LIST_WIDTH,
                        y + 1,
                        9,
                        6
                );

                if (downVisible) guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        downHovered ? ARROW_DOWN_HIGHLIGHTED : ARROW_DOWN,
                        left + LIST_WIDTH,
                        y + 7,
                        9,
                        6
                );

                guiGraphics.drawCenteredString(
                        Minecraft.getInstance().font,
                        getEnchantmentLevelComponent(level).getString(),
                        left + LIST_WIDTH - 10,
                        y + 3,
                        -1
                );

                VersionHelper.drawScrollingString(
                        guiGraphics,
                        label,
                        left + 15,
                        left + 15,
                        left + LIST_WIDTH - 23,
                        y,
                        y + 14
                );
            } else {
                this.enchantSelector.visible = true;
                this.enchantSelector.setY(y);

                if (anyHovered) {
                    guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                }

                guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    addDeleteHovered ? ADD_ENCHANTING_BUTTON_HIGHLIGHTED : ADD_ENCHANTING_BUTTON,
                    left,
                    y,
                    12,
                    14
                );
            }
        }
    }

    private @Nullable Runnable checkEnchantmentsClicked(MouseButtonEvent mouseButtonEvent) {
        if (this.menu == null || this.enchantSelector == null || this.screen == null || this.menu.resultSlots.isEmpty())
            return null;

        if (this.enchantSelector.visible && this.enchantSelector.open)
            return null;

        int left = this.screen.leftPos + LIST_LEFT;
        int top = this.screen.topPos + 14;
        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();

        ArrayList<Object2IntMap.@Nullable Entry<Holder<Enchantment>>> enchants =
                new ArrayList<>(this.menu.getSortedEnchantments());
        enchants.add(null);

        for (int i = this.startIndex; i < enchants.size() && i < 4 + this.startIndex; i++) {
            int y = top + (i - this.startIndex) * 14;
            Object2IntMap.@Nullable Entry<Holder<Enchantment>> enchant = enchants.get(i);

            boolean anyHovered = mouseX >= left && mouseY >= y && mouseX < left + SELECTOR_WIDTH && mouseY < y + 14;
            if (!anyHovered) continue;
            boolean addDeleteHovered = mouseX < left + 12;

            if (enchant != null) {
                int level = enchant.getIntValue();
                boolean upVisible = level < 255;
                boolean downVisible = level > 1;

                boolean upHovered = upVisible && !addDeleteHovered &&
                        mouseX >= left + LIST_WIDTH && mouseX < left + LIST_WIDTH + 9 &&
                        mouseY >= y && mouseY < y + 7;
                boolean downHovered = downVisible && !addDeleteHovered &&
                        mouseX >= left + LIST_WIDTH && mouseX < left + LIST_WIDTH + 9 &&
                        mouseY >= y + 7 && mouseY < y + 14;

                if (addDeleteHovered) return () -> this.menu.removeEnchantment(enchant.getKey());
                else if (upHovered) return () -> {
                    int change = mouseButtonEvent.hasShiftDown() ? 10 : 1;
                    int newLevel = Mth.clamp(enchant.getIntValue() + change, 1, 255);
                    this.menu.setEnchantment(enchant.getKey(), newLevel);
                };
                else if (downHovered) return () -> {
                    int change = mouseButtonEvent.hasShiftDown() ? 10 : 1;
                    int newLevel = Mth.clamp(enchant.getIntValue() - change, 1, 255);
                    this.menu.setEnchantment(enchant.getKey(), newLevel);
                };
            } else if (anyHovered) return () -> {
                if (this.enchantSelector != null) {
                    this.enchantSelector.open = true;
                    this.enchantSelector.setScrollAmount(0.0);
                }
            };
        }

        return null;
    }

    private boolean isScrollBarActive() {
        return this.getOffscreenRows() > 0;
    }

    private int getOffscreenRows() {
        if (this.menu == null) return 0;
        return Math.max(this.menu.resultSlots.getItem(0).getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY
        ).entrySet().size() - 3, 0);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent) {
        if (this.screen == null) return false;

        int x = this.screen.leftPos + 177;
        int y = this.screen.topPos + 14;
        if (mouseButtonEvent.x() >= x && mouseButtonEvent.x() < x + 12 && mouseButtonEvent.y() >= y && mouseButtonEvent.y() < y + 56)
            this.scrolling = true;

        Runnable onClick = checkEnchantmentsClicked(mouseButtonEvent);
        if (onClick != null) {
            onClick.run();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        this.scrolling = false;
        if (this.menu == null) return false;

        return checkEnchantmentsClicked(mouseButtonEvent) != null;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent) {
        if (this.screen == null || !this.scrolling || !this.isScrollBarActive())
            return false;

        if (this.enchantSelector != null && this.enchantSelector.visible && this.enchantSelector.open)
            return false;

        int top = this.screen.topPos + 14;
        int bottom = top + 56;
        this.scrollOffs = ((float) mouseButtonEvent.y() - top - 7.5F) / (bottom - top - 15F);
        this.scrollOffs = Mth.clamp(this.scrollOffs, 0F, 1F);
        this.startIndex = (int) (this.scrollOffs * this.getOffscreenRows() + 0.5);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.enchantSelector != null && this.enchantSelector.visible && this.enchantSelector.open)
            return this.enchantSelector.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

        if (this.isScrollBarActive()) {
            int offscreenRows = this.getOffscreenRows();
            float deltaY = (float) scrollY / offscreenRows;
            this.scrollOffs = Mth.clamp(this.scrollOffs - deltaY, 0F, 1F);
            this.startIndex = (int) (this.scrollOffs * offscreenRows + 0.5);
        }

        return true;
    }

    @Override
    public void remove() {
        this.scrollOffs = 0F;
        this.startIndex = 0;
        if (this.screen != null && this.enchantSelector != null)
            this.screen.removeWidget(this.enchantSelector);
        this.enchantSelector = null;
        super.remove();
    }

    @Override
    public void dispose() {
        this.scrollOffs = 0F;
        this.startIndex = 0;
        super.dispose();
    }

    private void update() {
        if (this.menu == null || this.enchantSelector == null || this.screen == null)
            return;

        this.startIndex = (int) (this.scrollOffs * this.getOffscreenRows() + 0.5);
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

    public class EnchantingTabMenu extends CreativeMenuTab.CreativeTabMenu<EnchantingTabMenu> {
        private final Container inputSlots;
        private final ResultContainer resultSlots = new ResultContainer();

        EnchantingTabMenu(Player player) {
            super(player);
            this.inputSlots = MenuHelper.simpleContainer(this, 1);
            this.addSlot(new Slot(this.inputSlots, 0, 9, 14));
            this.addSlot(MenuHelper.resultSlot(this, this.resultSlots, 0, 9, 54));
        }

        @Override
        EnchantingTabMenu copyWithPlayer(@NotNull Player player) {
            return this.copyContentsTo(new EnchantingTabMenu(player));
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
            EnchantingMenuTab.this.update();
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
                EnchantingMenuTab.this.update();
            } else if (container == this.resultSlots) {
                this.inputSlots.clearContent();
                EnchantingMenuTab.this.update();
            }
        }

        @Override
        public void removed(@NotNull Player player) {
            if (player.hasInfiniteMaterials()) for (int i = 0; i < this.inputSlots.getContainerSize(); i++)
                player.getInventory().placeItemBackInInventory(this.inputSlots.removeItemNoUpdate(i));
        }
    }
}
