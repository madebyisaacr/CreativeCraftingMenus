package dev.chililisoup.creativecraftingmenus.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Simple placeholder tab for decorated pots.
 * For now this just adds the button; it does not implement any custom UI or behavior.
 */
public class DecoratedPotsMenuTab extends CreativeMenuTab<DecoratedPotsMenuTab.DecoratedPotsTabMenu> {

    public DecoratedPotsMenuTab(Component displayName, Supplier<ItemStack> iconGenerator, String id) {
        super(displayName, iconGenerator, id);
    }

    @Override
    DecoratedPotsTabMenu createMenu(Player player) {
        return new DecoratedPotsTabMenu(player);
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


