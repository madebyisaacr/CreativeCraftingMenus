package dev.chililisoup.creativecraftingmenus.gui;

import dev.chililisoup.creativecraftingmenus.gui.components.DyesGrid;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class FireworksMenuTab extends CreativeMenuTab<FireworksMenuTab.FireworksTabMenu> {
    private static final int DYES_GRID_LEFT = 99;
    private static final int DYES_GRID_TOP = 16;

    private DyeColor selectedColor = DyeColor.WHITE;

    public FireworksMenuTab(Component displayName, Supplier<ItemStack> iconGenerator, String id) {
        super(displayName, iconGenerator, id);
    }

    @Override
    FireworksTabMenu createMenu(Player player) {
        return new FireworksTabMenu(player);
    }

    @Override
    public void render(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (this.screen == null) return;
        int left = this.screen.leftPos + DYES_GRID_LEFT;
        int top = this.screen.topPos + DYES_GRID_TOP;
        DyesGrid.renderDyes(guiGraphics, left, top, mouseX, mouseY, this.selectedColor);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent) {
        if (this.screen == null) return false;
        return DyesGrid.getClickedDye(
                this.screen.leftPos + DYES_GRID_LEFT,
                this.screen.topPos + DYES_GRID_TOP,
                mouseButtonEvent.x(),
                mouseButtonEvent.y(),
                this.selectedColor
        ) != null;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (this.screen == null) return false;
        DyeColor clicked = DyesGrid.getClickedDye(
                this.screen.leftPos + DYES_GRID_LEFT,
                this.screen.topPos + DYES_GRID_TOP,
                mouseButtonEvent.x(),
                mouseButtonEvent.y(),
                this.selectedColor
        );
        if (clicked != null) {
            this.selectedColor = clicked;
            return true;
        }
        return false;
    }

    public static class FireworksTabMenu extends CreativeMenuTab.CreativeTabMenu<FireworksTabMenu> {
        private static final ItemStack FIREWORK_ROCKET = Items.FIREWORK_ROCKET.getDefaultInstance();

        private final ResultContainer fireworkSlot = new ResultContainer();

        FireworksTabMenu(Player player) {
            super(player);
            this.addSlot(new Slot(this.fireworkSlot, 0, 166, 33) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }

                @Override
                public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                    super.onTake(player, stack);
                    this.container.setItem(0, FIREWORK_ROCKET.copy());
                }
            });
            this.fireworkSlot.setItem(0, FIREWORK_ROCKET.copy());
        }

        @Override
        FireworksTabMenu copyWithPlayer(@NotNull Player player) {
            return this.copyContentsTo(new FireworksTabMenu(player));
        }

        @Override
        public @NotNull ItemStack quickMoveFromInventory(@NotNull Player player, int slotIndex) {
            return ItemStack.EMPTY;
        }
    }
}
