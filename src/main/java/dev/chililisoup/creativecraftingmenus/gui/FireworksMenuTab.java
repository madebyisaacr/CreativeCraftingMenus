package dev.chililisoup.creativecraftingmenus.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
