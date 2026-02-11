package dev.chililisoup.creativecraftingmenus.util;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class MenuHelper {
    public static SimpleContainer simpleContainer(AbstractContainerMenu menu, int size) {
        return new SimpleContainer(size) {
            @Override
            public void setChanged() {
                super.setChanged();
                menu.slotsChanged(this);
            }
        };
    }

    public static Slot resultSlot(
            AbstractContainerMenu menu,
            ResultContainer resultSlots,
            int slot,
            int x,
            int y
    ) {
        return new Slot(resultSlots, slot, x, y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack itemStack) {
                return false;
            }

            @Override
            public void onTake(@NotNull Player player, @NotNull ItemStack itemStack) {
                menu.slotsChanged(resultSlots);
            }
        };
    }
}
