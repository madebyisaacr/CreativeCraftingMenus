package dev.chililisoup.creativecraftingmenus.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class FireworksMenuTab extends CreativeMenuTab<FireworksMenuTab.FireworksTabMenu> {
    public FireworksMenuTab(Component displayName, Supplier<ItemStack> iconGenerator, String id) {
        super(displayName, iconGenerator, id);
    }

    @Override
    FireworksTabMenu createMenu(Player player) {
        return new FireworksTabMenu(player);
    }

    public static class FireworksTabMenu extends CreativeMenuTab.CreativeTabMenu<FireworksTabMenu> {
        FireworksTabMenu(Player player) {
            super(player);
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
