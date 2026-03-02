package dev.chililisoup.creativecraftingmenus.gui;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.chililisoup.creativecraftingmenus.util.ServerResourceProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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

    private static final int SLOT_SIZE = 16;
    private static final int[] SLOT_X = {113, 94, 132, 113};  // front, left, right, back
    private static final int[] SLOT_Y = {54, 35, 35, 16};

    private final ArrayList<Item> sherdItems = new ArrayList<>();
    private final Item @Nullable [] slotSherds = new Item[4];  // front, left, right, back
    private int selectedSlotIndex = 0;  // front selected by default
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
        this.renderSlotButtons(screen, guiGraphics, mouseX, mouseY);
    }

    private void renderSlotButtons(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (int i = 0; i < 4; i++) {
            int x = screen.leftPos + SLOT_X[i];
            int y = screen.topPos + SLOT_Y[i];
            boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + SLOT_SIZE && mouseY < y + SLOT_SIZE;
            boolean selected = i == this.selectedSlotIndex;
            if (selected) {
                guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF6E82A3);  // selected: #6E82A3
            } else if (hovered) {
                guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFFC0C0C0);  // hover: #C0C0C0
            }
            Item sherd = this.slotSherds[i];
            if (sherd != null) {
                guiGraphics.renderItem(sherd.getDefaultInstance(), x, y);
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

        if (checkSlotButtonClicked(mouseButtonEvent.x(), mouseButtonEvent.y()) != null) return true;
        return checkGridClicked(mouseButtonEvent.x(), mouseButtonEvent.y()) != null;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        this.scrolling = false;

        Integer slotClicked = checkSlotButtonClicked(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (slotClicked != null) {
            this.selectedSlotIndex = slotClicked;
            this.selectedIndex = getGridIndexForSlotSherd(this.slotSherds[slotClicked]);
            this.gridContents = getGridContents();
            return true;
        }

        Integer clicked = checkGridClicked(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (clicked != null) {
            this.selectedIndex = clicked;
            this.slotSherds[this.selectedSlotIndex] = clicked == 0 ? null : this.sherdItems.get(clicked - 1);
            this.gridContents = getGridContents();
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

    public static class DecoratedPotsTabMenu extends CreativeMenuTab.CreativeTabMenu<DecoratedPotsTabMenu> {
        private final ResultContainer decoratedPotSlot = new ResultContainer();

        DecoratedPotsTabMenu(Player player) {
            super(player);
            this.addSlot(new Slot(this.decoratedPotSlot, 0, 162, 57) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    // Do not allow placing anything into this slot.
                    return false;
                }

                @Override
                public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                    // Immediately refill with a new decorated pot when taken.
                    this.container.setItem(0, Items.DECORATED_POT.getDefaultInstance());
                }
            });
            // Initialize with a decorated pot.
            this.decoratedPotSlot.setItem(0, Items.DECORATED_POT.getDefaultInstance());
        }

        @Override
        DecoratedPotsTabMenu copyWithPlayer(@NotNull Player player) {
            return this.copyContentsTo(new DecoratedPotsTabMenu(player));
        }

        @Override
        public @NotNull ItemStack quickMoveFromInventory(@NotNull Player player, int slotIndex) {
            // For now, this tab does not interact with inventory items.
            // Returning EMPTY means shift-clicking from inventory won't move items into this tab.
            return ItemStack.EMPTY;
        }
    }
}


