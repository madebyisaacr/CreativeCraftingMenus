package dev.chililisoup.creativecraftingmenus.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

        DecoratedPotsTabMenu(Player player) {
            super(player);
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


