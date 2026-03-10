package dev.chililisoup.creativecraftingmenus.gui;

import dev.chililisoup.creativecraftingmenus.CreativeCraftingMenus;
import dev.chililisoup.creativecraftingmenus.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class CreativeMenuTab<M extends CreativeMenuTab.CreativeTabMenu<M>> extends CreativeModeTab {
    protected static final Identifier SELECTED_TAB = CreativeCraftingMenus.id("widget/inner_tab_selected");
    protected static final Identifier HIGHLIGHTED_TAB = CreativeCraftingMenus.id("widget/inner_tab_highlighted");
    protected static final Identifier UNSELECTED_TAB = CreativeCraftingMenus.id("widget/inner_tab_unselected");
    protected static final Identifier BUTTON_SELECTED = CreativeCraftingMenus.id("widget/button_selected");
    protected static final Identifier BUTTON_HIGHLIGHTED = CreativeCraftingMenus.id("widget/button_highlighted");
    protected static final Identifier BUTTON = CreativeCraftingMenus.id("widget/button_unselected");
    protected static final Identifier BUTTON_DISABLED = CreativeCraftingMenus.id("widget/button_disabled");
    protected static final Identifier ARROW_UP = CreativeCraftingMenus.id("widget/arrow_up");
    protected static final Identifier ARROW_UP_HIGHLIGHTED = CreativeCraftingMenus.id("widget/arrow_up_highlighted");
    protected static final Identifier ARROW_DOWN = CreativeCraftingMenus.id("widget/arrow_down");
    protected static final Identifier ARROW_DOWN_HIGHLIGHTED = CreativeCraftingMenus.id("widget/arrow_down_highlighted");
    protected static final Identifier ARROW_BACK = CreativeCraftingMenus.id("widget/arrow_back");
    protected static final Identifier ARROW_BACK_HIGHLIGHTED = CreativeCraftingMenus.id("widget/arrow_back_highlighted");

    protected @Nullable AbstractContainerScreen<?> screen;
    protected @Nullable M menu = null;
    private boolean hidden = false;
    public final String id;

    CreativeMenuTab(Component displayName, Supplier<ItemStack> iconGenerator, String id) {
        //noinspection DataFlowIssue
        super(null, -1, Type.INVENTORY, displayName, iconGenerator, CreativeModeTab.Builder.EMPTY_GENERATOR);
        this.id = id;
    }

    abstract M createMenu(Player player);

    public final void init(AbstractContainerScreen<?> screen, Player player) {
        this.screen = screen;
        if (this.menu == null)
            this.menu = this.createMenu(player);
        else if (this.menu.player != player)
            this.menu = this.menu.copyWithPlayer(player);
    }

    public void subInit() {}

    /**
     * Whether this tab participates in the shared-input behavior when
     * switching between creative menu tabs. Tabs that return true here
     * are considered to have an input slot whose contents can be carried
     * over to another compatible tab.
     */
    public boolean keepInputOnTabSwitch() {
        return false;
    }

    /**
     * Extracts the current input item from this tab, if any. Implementations
     * should remove the item from their input slot and update any dependent
     * state, returning the removed stack. The default implementation has
     * no input and returns {@link ItemStack#EMPTY}.
     */
    public ItemStack extractInputItem() {
        return ItemStack.EMPTY;
    }

    /**
     * Attempts to accept the given stack as this tab's input item. If the
     * stack is accepted into the tab's input slot, implementations should
     * return {@code true}; otherwise, they should return {@code false}
     * and leave the stack untouched.
     */
    public boolean acceptInputItem(ItemStack stack) {
        return false;
    }

    /**
     * Fully remove this tab, disposing its menu and returning any items
     * according to the menu implementation. This is used when switching away
     * from the tab or when the creative screen is closed.
     */
    public void remove() {
        if (this.menu != null) {
            this.menu.removed(this.menu.player);
            this.menu = null;
        }
        this.screen = null;
    }

    public void dispose() {
        this.screen = null;
        this.menu = null;
    }

    public boolean keyPressed(KeyEvent keyEvent) {
        return false;
    }

    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent) {
        return false;
    }

    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    @Override
    public boolean shouldDisplay() {
        return !hidden && super.shouldDisplay() && !ModConfig.HANDLER.instance().disabledTabs.contains(this.id);
    }

    public void hide() {
        this.hidden = true;
    }

    public void show() {
        this.hidden = false;
    }

    public @NotNull M getMenu() {
        assert this.menu != null;
        return this.menu;
    }

    public interface TitleDrawer {
        void draw(int x, int y, int color);
    }

    public void drawTitle(TitleDrawer titleDrawer, int x, int y, int color) {
        titleDrawer.draw(x, y, color);
    }

    public void render(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {}

    public static abstract class CreativeTabMenu<M extends CreativeTabMenu<M>> extends AbstractContainerMenu {
        protected final Player player;

        CreativeTabMenu(Player player) {
            super(null, 0);
            this.player = player;
        }

        abstract M copyWithPlayer(@NotNull Player player);

        protected M copyContentsTo(M other) {
            other.initializeContents(
                    this.getStateId(),
                    this.slots.stream().map(Slot::getItem).toList(),
                    this.getCarried()
            );

            return other;
        }

        @Override
        public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
            ItemStack resultStack = ItemStack.EMPTY;
            if (slotIndex < 0 || this.slots.size() <= slotIndex) return resultStack;
            Slot slot = this.slots.get(slotIndex);
            if (!slot.hasItem()) return resultStack;

            ItemStack slotStack = slot.getItem();
            resultStack = slotStack.copy();

            if (!this.player.inventoryMenu.moveItemStackTo(slotStack, 9, 45, true))
                return ItemStack.EMPTY;

            if (slotStack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (slotStack.getCount() == resultStack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, resultStack);

            return resultStack;
        }

        public abstract @NotNull ItemStack quickMoveFromInventory(@NotNull Player player, int slotIndex);

        @Override
        protected @NotNull Slot addSlot(Slot slot) {
            slot.index = this.slots.size();
            this.slots.add(slot);
            return slot;
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return true;
        }

        @Override
        public @NotNull ItemStack getCarried() {
            return this.player.inventoryMenu.getCarried();
        }

        @Override
        public void setCarried(@NotNull ItemStack itemStack) {
            this.player.inventoryMenu.setCarried(itemStack);
        }

        @Override
        public void removed(@NotNull Player player) {}
    }
    
    public interface MenuTabConstructor<M extends CreativeMenuTab.CreativeTabMenu<M>, T extends CreativeMenuTab<M>> {
        T accept(
                Component component,
                Supplier<ItemStack> supplier,
                String id
        );
    }

    public static class Builder<M extends CreativeMenuTab.CreativeTabMenu<M>, T extends CreativeMenuTab<M>> extends CreativeModeTab.Builder {
        MenuTabConstructor<M, T> constructor;
        private boolean hasDisplayName = false;
        private @Nullable String id;

        public Builder(MenuTabConstructor<M, T> constructor) {
            //noinspection DataFlowIssue
            super(null, -1);
            this.constructor = constructor;
        }

        public @NotNull Builder<M, T> id(@NotNull String id) {
            this.id = id;
            return this;
        }

        @Override
        public @NotNull Builder<M, T> title(@NotNull Component displayName) {
            hasDisplayName = true;
            super.title(displayName);
            return this;
        }

        @Override
        public @NotNull Builder<M, T> icon(@NotNull Supplier<ItemStack> iconGenerator) {
            super.icon(iconGenerator);
            return this;
        }

        @Override
        public @NotNull Builder<M, T> backgroundTexture(@NotNull Identifier backgroundTexture) {
            super.backgroundTexture(backgroundTexture);
            return this;
        }

        @Override
        public @NotNull T build() {
            if (!hasDisplayName)
                throw new IllegalStateException("No display name set for Creative Tab Menu");
            if (this.id == null)
                throw new IllegalStateException("No id set for Creative Tab Menu");

            T menuTab = constructor.accept(this.displayName, this.iconGenerator, this.id);
            menuTab.alignedRight = true;
            menuTab.canScroll = false;
            menuTab.showTitle = this.showTitle;
            menuTab.backgroundTexture = this.backgroundTexture;
            return menuTab;
        }
    }
}
