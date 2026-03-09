package dev.chililisoup.creativecraftingmenus.gui;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.chililisoup.creativecraftingmenus.CreativeCraftingMenus;
import dev.chililisoup.creativecraftingmenus.util.ServerResourceProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;

import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

import static net.minecraft.client.gui.screens.inventory.StonecutterScreen.SCROLLER_DISABLED_SPRITE;
import static net.minecraft.client.gui.screens.inventory.StonecutterScreen.SCROLLER_SPRITE;

public class DecoratedPotsMenuTab extends CreativeMenuTab<DecoratedPotsMenuTab.DecoratedPotsTabMenu> {
    private static final int GRID_LEFT = 9;
    private static final int GRID_TOP = 16;
    private static final int SCROLLBAR_LEFT = GRID_LEFT + 4 * 16 + 3;
    private static final int SCROLLBAR_TOP = GRID_TOP;
    private static final int SCROLLBAR_HEIGHT = 54;
    private static final int GRID_COLUMNS = 4;
    private static final int VISIBLE_ROWS = 3;
    private static final int VISIBLE_ITEMS = GRID_COLUMNS * VISIBLE_ROWS;
    private static final int ITEM_WIDTH = 16;
    private static final int ITEM_HEIGHT = 18;

    private static final Identifier DECORATED_POT_SLOT_SELECTED = CreativeCraftingMenus.id("widget/decorated_pot_slot_selected");
    private static final Identifier SELECT_ALL_BUTTON = CreativeCraftingMenus.id("widget/decorated_pot_select_all_button");
    private static final Identifier SELECT_ALL_BUTTON_HIGHLIGHTED = CreativeCraftingMenus.id("widget/decorated_pot_select_all_button_highlighted");
    private static final Identifier SELECT_ALL_BUTTON_SELECTED = CreativeCraftingMenus.id("widget/decorated_pot_select_all_button_selected");
    private static final int SELECT_ALL_BUTTON_X = 114;
    private static final int SELECT_ALL_BUTTON_Y = 36;
    private static final int SELECT_ALL_BUTTON_SIZE = 14;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_VISUAL_SIZE = 16;
    private static final int SLOT_VISUAL_OFFSET = (SLOT_SIZE - SLOT_VISUAL_SIZE) / 2;  // 1, centers 16 in 18
    private static final int[] SLOT_X = {112, 93, 131, 112};  // front, left, right, back
    private static final int[] SLOT_Y = {53, 34, 34, 15};
    private static final String[] SLOT_KEYS = {
            "container.creative_crafting_menus.decorated_pots.side.front",
            "container.creative_crafting_menus.decorated_pots.side.left",
            "container.creative_crafting_menus.decorated_pots.side.right",
            "container.creative_crafting_menus.decorated_pots.side.back"
    };

    private final ArrayList<Item> sherdItems = new ArrayList<>();
    private final Item @Nullable [] slotSherds = new Item[4];  // front, left, right, back
    private int selectedSlotIndex = 0;  // front selected by default; -1 means "all sides"
    private List<GridItem> gridContents = List.of();
    private int selectedIndex = 0;
    private float scrollOffs;
    private boolean scrolling;
    private int startIndex;

    public DecoratedPotsMenuTab(Component displayName, Supplier<ItemStack> iconGenerator, String id) {
        super(displayName, iconGenerator, id);
    }

    @Override
    DecoratedPotsTabMenu createMenu(Player player) {
        return new DecoratedPotsTabMenu(player);
    }

    @Override
    public void subInit() {
        this.scrolling = false;

        if (sherdItems.isEmpty()) {
            sherdItems.addAll(ServerResourceProvider.getFromTag(ItemTags.DECORATED_POT_SHERDS));
        }

        this.gridContents = getGridContents();
    }

    private void loadSherdsFromPot(ItemStack stack) {
        if (!stack.is(Items.DECORATED_POT)) return;
        PotDecorations decorations = stack.get(DataComponents.POT_DECORATIONS);
        if (decorations == null) {
            for (int i = 0; i < 4; i++) this.slotSherds[i] = null;
        } else {
            // slotSherds order: front, left, right, back
            this.slotSherds[0] = itemIfSherd(decorations.front().orElse(null));
            this.slotSherds[1] = itemIfSherd(decorations.left().orElse(null));
            this.slotSherds[2] = itemIfSherd(decorations.right().orElse(null));
            this.slotSherds[3] = itemIfSherd(decorations.back().orElse(null));
        }
        this.updateSelectedIndex();
    }

    private static @Nullable Item itemIfSherd(Item item) {
        // BRICK is used for blank sides in PotDecorations; any other item is a sherd
        return (item == null || item == Items.BRICK) ? null : item;
    }

    private ItemStack createDecoratedPotItemStack() {
        Item front = this.slotSherds[0];
        Item left = this.slotSherds[1];
        Item right = this.slotSherds[2];
        Item back = this.slotSherds[3];

        boolean anySherd = front != null || left != null || right != null || back != null;
        if (!anySherd) {
            return Items.DECORATED_POT.getDefaultInstance();
        }

        Item backItem = back != null ? back : Items.BRICK;
        Item leftItem = left != null ? left : Items.BRICK;
        Item rightItem = right != null ? right : Items.BRICK;
        Item frontItem = front != null ? front : Items.BRICK;

        PotDecorations potDecorations = new PotDecorations(backItem, leftItem, rightItem, frontItem);
        return DecoratedPotBlockEntity.createDecoratedPotItem(potDecorations);
    }

    private List<GridItem> getGridContents() {
        List<GridItem> list = new ArrayList<>();
        list.add(new GridItem(
                Component.translatable("gui.none"),
                (guiGraphics, x, y) -> guiGraphics.renderItem(Items.BARRIER.getDefaultInstance(), x, y),
                0 == this.selectedIndex
        ));
        for (int i = 0; i < sherdItems.size(); i++) {
            Item item = sherdItems.get(i);
            list.add(new GridItem(
                    item.getName(item.getDefaultInstance()),
                    (guiGraphics, x, y) -> guiGraphics.renderItem(item.getDefaultInstance(), x, y),
                    i + 1 == this.selectedIndex
            ));
        }
        return list;
    }

    @Override
    public void render(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        this.renderScrollBar(screen, guiGraphics, mouseX, mouseY);
        this.renderGrid(screen, guiGraphics, mouseX, mouseY);
        this.renderSelectAllButton(screen, guiGraphics, mouseX, mouseY);
        this.renderDecoratedPotPreview(screen, guiGraphics);
        this.renderSlotButtons(screen, guiGraphics, mouseX, mouseY);
    }

    private void renderDecoratedPotPreview(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics) {
        if (this.menu == null) return;

        ItemStack potStack = this.menu.decoratedPotSlot.getItem(0);
        if (potStack.isEmpty()) {
            potStack = this.createDecoratedPotItemStack();
        }

        // Target area: 40x40 at (150, 9) relative to the container origin.
        int areaX = screen.leftPos + 150;
        int areaY = screen.topPos + 9;
        float scale = 40F / 16F; // Default item render is 16x16

        guiGraphics.pose().pushMatrix();
        // Translate to the center of the 40x40 area, then render the item centered.
        guiGraphics.pose().translate(areaX + 20F, areaY + 20F);
        guiGraphics.pose().scale(scale);
        guiGraphics.renderItem(potStack, -8, -8);
        guiGraphics.pose().popMatrix();
    }

    private void renderSelectAllButton(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = screen.leftPos + SELECT_ALL_BUTTON_X;
        int y = screen.topPos + SELECT_ALL_BUTTON_Y;
        boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + SELECT_ALL_BUTTON_SIZE && mouseY < y + SELECT_ALL_BUTTON_SIZE;

        Identifier sprite = this.selectedSlotIndex == -1 ? SELECT_ALL_BUTTON_SELECTED : (hovered ? SELECT_ALL_BUTTON_HIGHLIGHTED : SELECT_ALL_BUTTON);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, SELECT_ALL_BUTTON_SIZE, SELECT_ALL_BUTTON_SIZE);

        if (hovered) {
            guiGraphics.setComponentTooltipForNextFrame(
                    screen.getFont(),
                    List.of(Component.translatable("container.creative_crafting_menus.decorated_pots.side.all")),
                    (int) mouseX,
                    (int) mouseY
            );
        }
    }

    private void renderSlotButtons(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (int i = 0; i < 4; i++) {
            int x = screen.leftPos + SLOT_X[i];
            int y = screen.topPos + SLOT_Y[i];
            int vx = x + SLOT_VISUAL_OFFSET;
            int vy = y + SLOT_VISUAL_OFFSET;
            boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + SLOT_SIZE && mouseY < y + SLOT_SIZE;
            boolean selected = i == this.selectedSlotIndex;
            if (selected) {
                guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    DECORATED_POT_SLOT_SELECTED,
                    x,
                    y,
                    18,
                    18
                );
            } else if (hovered) {
                guiGraphics.fill(vx, vy, vx + SLOT_VISUAL_SIZE, vy + SLOT_VISUAL_SIZE, 0xFFC0C0C0);  // hover: #C0C0C0

                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable(SLOT_KEYS[i]));
                Item sherd = this.slotSherds[i];
                if (sherd != null) {
                    tooltip.add(sherd.getName(sherd.getDefaultInstance()).copy().withStyle(ChatFormatting.GRAY));
                }
                guiGraphics.setComponentTooltipForNextFrame(screen.getFont(), tooltip, (int) mouseX, (int) mouseY);
            }

            Item sherd = this.slotSherds[i];
            if (sherd != null) {
                guiGraphics.renderItem(sherd.getDefaultInstance(), vx, vy);
            }
        }
    }

    private int getGridIndexForSlotSherd(@Nullable Item sherd) {
        if (sherd == null) return 0;
        for (int i = 0; i < this.sherdItems.size(); i++) {
            if (this.sherdItems.get(i) == sherd) return i + 1;
        }
        return 0;
    }

    private void updateSelectedIndex() {
        if (this.selectedSlotIndex == -1) {
            Item common = this.slotSherds[0];
            for (int i = 1; i < this.slotSherds.length; i++) {
                if (this.slotSherds[i] != common) {
                    this.selectedIndex = -1;
                    this.gridContents = getGridContents();
                    return;
                }
            }
            this.selectedIndex = getGridIndexForSlotSherd(common);
        } else if (this.selectedSlotIndex >= 0 && this.selectedSlotIndex < this.slotSherds.length) {
            this.selectedIndex = getGridIndexForSlotSherd(this.slotSherds[this.selectedSlotIndex]);
        } else {
            this.selectedIndex = 0;
        }
        this.gridContents = getGridContents();
    }

    private boolean checkSelectAllButtonClicked(double mouseX, double mouseY) {
        if (this.screen == null) return false;
        int x = this.screen.leftPos + SELECT_ALL_BUTTON_X;
        int y = this.screen.topPos + SELECT_ALL_BUTTON_Y;
        return mouseX >= x && mouseY >= y && mouseX < x + SELECT_ALL_BUTTON_SIZE && mouseY < y + SELECT_ALL_BUTTON_SIZE;
    }

    private @Nullable Integer checkSlotButtonClicked(double mouseX, double mouseY) {
        if (this.screen == null) return null;

        for (int i = 0; i < 4; i++) {
            int x = this.screen.leftPos + SLOT_X[i];
            int y = this.screen.topPos + SLOT_Y[i];
            if (mouseX >= x && mouseY >= y && mouseX < x + SLOT_SIZE && mouseY < y + SLOT_SIZE)
                return i;
        }
        return null;
    }

    private void renderScrollBar(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = screen.leftPos + SCROLLBAR_LEFT;
        int y = screen.topPos + SCROLLBAR_TOP + (int) (39F * this.scrollOffs);

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

    private void renderGrid(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.menu == null) return;

        for (int i = this.startIndex; i < this.gridContents.size() && i < VISIBLE_ITEMS + this.startIndex; i++) {
            int x = screen.leftPos + GRID_LEFT + ((i - this.startIndex) % GRID_COLUMNS) * ITEM_WIDTH;
            int y = screen.topPos + GRID_TOP + ((i - this.startIndex) / GRID_COLUMNS) * ITEM_HEIGHT;

            GridItem item = this.gridContents.get(i);

            boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + ITEM_WIDTH && mouseY < y + ITEM_HEIGHT;
            if (hovered) {
                if (!item.selected) guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                guiGraphics.setTooltipForNextFrame(item.tooltip, mouseX, mouseY);
            }

            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    item.selected ? BUTTON_SELECTED : (hovered ? BUTTON_HIGHLIGHTED : BUTTON),
                    x,
                    y,
                    ITEM_WIDTH,
                    ITEM_HEIGHT
            );

            item.iconRenderer.render(guiGraphics, x, y + 1);
        }
    }

    private @Nullable Integer checkGridClicked(double mouseX, double mouseY) {
        if (this.screen == null) return null;

        for (int i = this.startIndex; i < this.gridContents.size() && i < VISIBLE_ITEMS + this.startIndex; i++) {
            int x = this.screen.leftPos + GRID_LEFT + ((i - this.startIndex) % GRID_COLUMNS) * ITEM_WIDTH;
            int y = this.screen.topPos + GRID_TOP + ((i - this.startIndex) / GRID_COLUMNS) * ITEM_HEIGHT;
            if (mouseX >= x && mouseY >= y && mouseX < x + ITEM_WIDTH && mouseY < y + ITEM_HEIGHT)
                return i;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent) {
        if (this.screen == null) return false;

        int x = this.screen.leftPos + SCROLLBAR_LEFT;
        int y = this.screen.topPos + SCROLLBAR_TOP;
        if (mouseButtonEvent.x() >= x && mouseButtonEvent.x() < x + 12 && mouseButtonEvent.y() >= y && mouseButtonEvent.y() < y + SCROLLBAR_HEIGHT)
            this.scrolling = true;

        if (checkSelectAllButtonClicked(mouseButtonEvent.x(), mouseButtonEvent.y())) return true;
        if (checkSlotButtonClicked(mouseButtonEvent.x(), mouseButtonEvent.y()) != null) return true;
        return checkGridClicked(mouseButtonEvent.x(), mouseButtonEvent.y()) != null;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        this.scrolling = false;

        if (checkSelectAllButtonClicked(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            this.selectedSlotIndex = (this.selectedSlotIndex == -1) ? 0 : -1;
            this.updateSelectedIndex();
            return true;
        }

        Integer slotClicked = checkSlotButtonClicked(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (slotClicked != null) {
            this.selectedSlotIndex = slotClicked;
            this.updateSelectedIndex();
            return true;
        }

        Integer clicked = checkGridClicked(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (clicked != null) {
            Item sherd = clicked == 0 ? null : this.sherdItems.get(clicked - 1);
            if (this.selectedSlotIndex == -1) {
                for (int i = 0; i < this.slotSherds.length; i++) {
                    this.slotSherds[i] = sherd;
                }
            } else {
                this.slotSherds[this.selectedSlotIndex] = sherd;
            }
            this.updateSelectedIndex();
            if (this.menu != null) {
                this.menu.decoratedPotSlot.setItem(0, this.createDecoratedPotItemStack());
            }
            return true;
        }

        return false;
    }

    private boolean isScrollBarActive() {
        return this.gridContents.size() > VISIBLE_ITEMS;
    }

    private int getOffscreenRows() {
        return Math.max(0, (this.gridContents.size() - 9) / GRID_COLUMNS);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent) {
        if (this.screen == null || !this.scrolling || !this.isScrollBarActive())
            return false;

        int top = this.screen.topPos + SCROLLBAR_TOP;
        int bottom = top + SCROLLBAR_HEIGHT;
        this.scrollOffs = ((float) mouseButtonEvent.y() - top - 7.5F) / (bottom - top - 15F);
        this.scrollOffs = Mth.clamp(this.scrollOffs, 0F, 1F);
        this.startIndex = (int) (this.scrollOffs * Math.max(0, this.getOffscreenRows()) + 0.5) * GRID_COLUMNS;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isScrollBarActive()) {
            int offscreenRows = this.getOffscreenRows();
            if (offscreenRows <= 0) return true;
            float deltaY = (float) scrollY / offscreenRows;
            this.scrollOffs = Mth.clamp(this.scrollOffs - deltaY, 0F, 1F);
            this.startIndex = (int) (this.scrollOffs * offscreenRows + 0.5) * GRID_COLUMNS;
        }
        return true;
    }

    @Override
    public void remove() {
        this.gridContents = List.of();
        this.scrollOffs = 0F;
        this.startIndex = 0;
        this.selectedSlotIndex = 0;
        super.remove();
    }

    @Override
    public void dispose() {
        this.sherdItems.clear();
        this.gridContents = List.of();
        this.scrollOffs = 0F;
        this.startIndex = 0;
        this.selectedSlotIndex = 0;
        super.dispose();
    }

    private record GridItem(Component tooltip, IconRenderer iconRenderer, boolean selected) {
        private interface IconRenderer {
            void render(GuiGraphics guiGraphics, int x, int y);
        }
    }

    public class DecoratedPotsTabMenu extends CreativeMenuTab.CreativeTabMenu<DecoratedPotsTabMenu> {
        private final ResultContainer decoratedPotSlot = new ResultContainer();

        DecoratedPotsTabMenu(Player player) {
            super(player);
            this.addSlot(new Slot(this.decoratedPotSlot, 0, 162, 57) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    if (stack.is(Items.DECORATED_POT)) {
                        this.container.setItem(0, stack.copyWithCount(1));
                        DecoratedPotsMenuTab.this.loadSherdsFromPot(stack);
                        return false;
                    }
                    return false;
                }

                @Override
                public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                    // Immediately refill with a decorated pot matching the selected sherds.
                    this.container.setItem(0, DecoratedPotsMenuTab.this.createDecoratedPotItemStack());
                }
            });
            // Initialize with a decorated pot matching the selected sherds (or default if none).
            this.decoratedPotSlot.setItem(0, DecoratedPotsMenuTab.this.createDecoratedPotItemStack());
        }

        @Override
        DecoratedPotsTabMenu copyWithPlayer(@NotNull Player player) {
            return this.copyContentsTo(new DecoratedPotsTabMenu(player));
        }

        @Override
        public @NotNull ItemStack quickMoveFromInventory(@NotNull Player player, int slotIndex) {
            Slot slot = this.player.inventoryMenu.slots.get(slotIndex);
            if (!slot.hasItem()) return ItemStack.EMPTY;

            ItemStack slotStack = slot.getItem();
            if (slotStack.is(Items.DECORATED_POT)) {
                this.decoratedPotSlot.setItem(0, slotStack.copyWithCount(1));
                DecoratedPotsMenuTab.this.loadSherdsFromPot(slotStack);
            }
            return ItemStack.EMPTY;
        }
    }
}


