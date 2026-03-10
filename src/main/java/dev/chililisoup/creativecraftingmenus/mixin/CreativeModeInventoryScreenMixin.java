package dev.chililisoup.creativecraftingmenus.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.chililisoup.creativecraftingmenus.CreativeCraftingMenus;
import dev.chililisoup.creativecraftingmenus.config.ModConfig;
import dev.chililisoup.creativecraftingmenus.gui.CreativeMenuTab;
import dev.chililisoup.creativecraftingmenus.reg.CreativeMenuTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeInventoryListener;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >= 1.21.11 {
import com.mojang.blaze3d.platform.cursor.CursorTypes;
//?}

import java.util.List;

@Mixin(value = CreativeModeInventoryScreen.class, priority = 999)
public abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.@NotNull ItemPickerMenu> {
    @Unique private static final Identifier SELECTED_MENU_TAB = CreativeCraftingMenus.id("container/creative_menu_tab_selected");
    @Unique private static final Identifier UNSELECTED_MENU_TAB = CreativeCraftingMenus.id("container/creative_menu_tab_unselected");
    @Unique private static final int COLUMN_TAB_COUNT = 3;

    public CreativeModeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Shadow protected abstract void selectTab(CreativeModeTab tab);
    @Shadow protected abstract boolean checkTabClicked(CreativeModeTab tab, double x, double y);
    @Shadow protected abstract int getTabX(CreativeModeTab tab);
    @Shadow protected abstract int getTabY(CreativeModeTab tab);
    @Shadow public abstract boolean isInventoryOpen();
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private CreativeInventoryListener listener;
    @Shadow private EditBox searchBox;

    @Unique
    private void resetHeight() {
        this.imageHeight = 136;
        this.topPos = (this.height - this.imageHeight) / 2;
        if (this.searchBox != null) this.searchBox.setY(this.topPos + 6);
    }

    @Inject(
            method = "init", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/CreativeModeTabs;getDefaultTab()Lnet/minecraft/world/item/CreativeModeTab;",
            ordinal = 0
    ))
    private void menuTabInit(CallbackInfo ci) {
        //? if < 1.21.11
        //if (this.minecraft == null) return;

        if (selectedTab instanceof CreativeMenuTab<?> menuTab)
            menuTab.init(this, this.minecraft.player);
    }

    @Inject(
            method = "init", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/InventoryMenu;removeSlotListener(Lnet/minecraft/world/inventory/ContainerListener;)V"
    ))
    private void menuTabSubInit(CallbackInfo ci) {
        if (selectedTab instanceof CreativeMenuTab<?> menuTab)
            menuTab.subInit();
    }

    @Inject(method = "selectTab", at = @At("HEAD"))
    private void updateMenuTabs(CreativeModeTab tab, CallbackInfo ci) {
        //? if < 1.21.11
        //if (this.minecraft == null) return;

        if (selectedTab == tab) return;
        if (selectedTab instanceof CreativeMenuTab<?> oldTab)
            oldTab.remove();
        if (this.listener != null && tab instanceof CreativeMenuTab<?> newTab)
            newTab.init(this, this.minecraft.player);
    }

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void makeAdjustments(CreativeModeTab tab, CallbackInfo ci) {
        if (tab instanceof CreativeMenuTab<?> newTab && Minecraft.getInstance().screen == this) {
            this.imageHeight = 166;
            this.topPos = (this.height - this.imageHeight) / 2;
            if (this.listener != null) newTab.subInit();
        } else this.resetHeight();
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void cleanup(CallbackInfo ci) {
        this.resetHeight();
    }

    @Inject(method = "getTabX", at = @At("HEAD"), cancellable = true)
    private void getMenuTabX(CreativeModeTab tab, CallbackInfoReturnable<Integer> cir) {
        if (tab instanceof CreativeMenuTab) {
            int tabSpacingX = ModConfig.HANDLER.instance().tabSpacingX;
            List<CreativeMenuTab<?>> filtered = CreativeMenuTabs.MENU_TABS.stream().filter(CreativeMenuTab::shouldDisplay).toList();
            int index = filtered.indexOf(tab);

            // First COLUMN_TAB_COUNT tabs on the left of the inventory, all others on the right
            if (index >= 0 && index < COLUMN_TAB_COUNT) {
                cir.setReturnValue(-26 - tabSpacingX);
            } else {
                cir.setReturnValue(this.imageWidth + tabSpacingX);
            }
        }
    }

    @Inject(method = "getTabY", at = @At("HEAD"), cancellable = true)
    private void getMenuTabY(CreativeModeTab tab, CallbackInfoReturnable<Integer> cir) {
        if (tab instanceof CreativeMenuTab) {
            int tabSpacingY = ModConfig.HANDLER.instance().tabSpacingY;
            List<CreativeMenuTab<?>> filtered = CreativeMenuTabs.MENU_TABS.stream().filter(CreativeMenuTab::shouldDisplay).toList();
            int count = filtered.size();
            int index = filtered.indexOf(tab);

            if (index < 0) return;

            int leftCount = Math.min(COLUMN_TAB_COUNT, count);
            int rightCount = Math.max(0, count - leftCount);
            int rows = Math.max(leftCount, rightCount);
            int totalHeight = (rows - 1) * tabSpacingY + rows * 26;
            int top = (this.height - totalHeight) / 2;

            int rowIndex = index < leftCount ? index : index - leftCount;
            cir.setReturnValue(top + rowIndex * (26 + tabSpacingY) - this.topPos);
        }
    }

    @Inject(method = "renderTabButton", at = @At("HEAD"), cancellable = true)
    //? if < 1.21.11 {
    /*private void renderMenuTabButton(GuiGraphics guiGraphics, CreativeModeTab tab, CallbackInfo ci) {
    *///?} else
    private void renderMenuTabButton(GuiGraphics guiGraphics, int mouseX, int mouseY, CreativeModeTab tab, CallbackInfo ci) {
        if (!(tab instanceof CreativeMenuTab)) return;

        boolean selected = tab == selectedTab && Minecraft.getInstance().screen == this;
        int x = this.getTabX(tab) + this.leftPos;
        int y = this.getTabY(tab) + this.topPos;
        Identifier sprite = selected ? SELECTED_MENU_TAB : UNSELECTED_MENU_TAB;

        //? if >= 1.21.11 {
        if (!selected && mouseX > x && mouseY > y && mouseX < x + 26 && mouseY < y + 26)
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        //?}

        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 26, 26);
        guiGraphics.renderItem(tab.getIconItem(), x + 5, y + 5);

        ci.cancel();
    }

    @WrapOperation(
            method = "renderLabels", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
    ))
    private void drawMenuTabLabels(GuiGraphics guiGraphics, Font font, Component title, int x, int y, int color, boolean bl, Operation<Void> original) {
        if (selectedTab instanceof CreativeMenuTab<?> menuTab) {
            menuTab.drawTitle(
                    (mX, mY, mColor) -> original.call(guiGraphics, font, title, mX, mY, mColor, bl),
                    x,
                    y,
                    color
            );
            guiGraphics.drawString(font, this.playerInventoryTitle, 9, this.imageHeight - 94, color, bl);
        } else {
            original.call(guiGraphics, font, title, x, y, color, bl);
        }
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void renderTabMenu(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (selectedTab instanceof CreativeMenuTab<?> menuTab)
            menuTab.render(this, guiGraphics, partialTick, mouseX, mouseY);
    }

    @Inject(method = "checkTabClicked", at = @At("HEAD"), cancellable = true)
    private void checkMenuTabClicked(CreativeModeTab tab, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (tab instanceof CreativeMenuTab) {
            int x = this.getTabX(tab);
            int y = this.getTabY(tab);
            cir.setReturnValue(mouseX >= x && mouseX <= x + 26 && mouseY >= y && mouseY <= y + 26);
        }
    }

    @Inject(method = "checkTabHovering", at = @At("HEAD"), cancellable = true)
    private void checkMenuTabHovering(
            GuiGraphics guiGraphics,
            CreativeModeTab tab,
            int mouseX,
            int mouseY,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(tab instanceof CreativeMenuTab)) return;
        if (this.isHovering(this.getTabX(tab) + 3, this.getTabY(tab) + 3, 21, 21, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(this.font, tab.getDisplayName(), mouseX, mouseY);
            cir.setReturnValue(true);
        } else cir.setReturnValue(false);
    }

    @WrapOperation(
            method = "slotClicked", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/InventoryMenu;clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V")
    )
    private void modifyInventoryClick(
            InventoryMenu instance,
            int slotIndex,
            int mouseButton,
            ClickType clickType,
            Player player,
            Operation<Void> original,
            @Local(argsOnly = true) @Nullable Slot slot
    ) {
        if (!(selectedTab instanceof CreativeMenuTab<?> menuTab)) {
            original.call(instance, slotIndex, mouseButton, clickType, player);
        } else if (clickType == ClickType.QUICK_MOVE && (mouseButton == 0 || mouseButton == 1) && slotIndex >= 0) {
            CreativeMenuTab.CreativeTabMenu<?> tabMenu = menuTab.getMenu();
            if (slot != null && slot.container instanceof Inventory) {
                ItemStack itemStack = tabMenu.quickMoveFromInventory(player, slotIndex);
                while (!itemStack.isEmpty() && ItemStack.isSameItem(slot.getItem(), itemStack))
                    itemStack = tabMenu.quickMoveFromInventory(player, slotIndex);
            } else tabMenu.clicked(slotIndex, mouseButton, clickType, player);
        } else this.menu.clicked(slot == null ? slotIndex : slot.index, mouseButton, clickType, player);
    }

    @Definition(id = "destroyItemSlot", field = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;destroyItemSlot:Lnet/minecraft/world/inventory/Slot;")
    @Expression("? == this.destroyItemSlot")
    @WrapOperation(method = "slotClicked", at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 0))
    private boolean clearMenuTabSlots(Object left, Object right, Operation<Boolean> original, @Local(argsOnly = true) ClickType clickType) {
        boolean base = original.call(left, right);
        if (!base) return false;
        if (!(selectedTab instanceof CreativeMenuTab)) return true;
        if (clickType == ClickType.QUICK_MOVE)
            this.menu.slots.forEach(slot -> {
                if (slot.mayPlace(ItemStack.EMPTY))
                    slot.set(ItemStack.EMPTY);
            });
        return true;
    }

    @Inject(
            method = "keyPressed", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/CreativeModeTab;getType()Lnet/minecraft/world/item/CreativeModeTab$Type;"),
            cancellable = true
    )
    private void menuTabKeyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (selectedTab instanceof CreativeMenuTab<?> menuTab && menuTab.keyPressed(keyEvent))
            cir.setReturnValue(true);
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void allowMenuTabTyping(CharacterEvent characterEvent, CallbackInfoReturnable<Boolean> cir) {
        if (selectedTab instanceof CreativeMenuTab)
            cir.setReturnValue(super.charTyped(characterEvent));
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private static void menuTabMouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (selectedTab instanceof CreativeMenuTab<?> menuTab && menuTab.mouseClicked(mouseButtonEvent))
            cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void menuTabMouseReleased(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        if (selectedTab instanceof CreativeMenuTab<?> menuTab && menuTab.mouseReleased(mouseButtonEvent))
            cir.setReturnValue(true);
    }

    @Inject(
            method = "mouseReleased", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/CreativeModeTabs;tabs()Ljava/util/List;"),
            cancellable = true
    )
    private void checkTabReleased(
            MouseButtonEvent mouseButtonEvent,
            CallbackInfoReturnable<Boolean> cir,
            @Local(ordinal = 0) double x,
            @Local(ordinal = 1) double y
    ) {
        for (CreativeMenuTab<?> menuTab : CreativeMenuTabs.MENU_TABS.stream().filter(CreativeMenuTab::shouldDisplay).toList()) {
            if (this.checkTabClicked(menuTab, x, y)) {
                this.selectTab(menuTab);
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(
            method = "mouseDragged", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z"),
            cancellable = true
    )
    private void menuTabDragged(MouseButtonEvent mouseButtonEvent, double a, double b, CallbackInfoReturnable<Boolean> cir) {
        if (selectedTab instanceof CreativeMenuTab<?> menuTab && menuTab.mouseDragged(mouseButtonEvent))
            cir.setReturnValue(true);
    }

    @WrapOperation(
            method = "mouseScrolled", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;mouseScrolled(DDDD)Z")
    )
    private boolean menuTabScrolled(
            CreativeModeInventoryScreen instance,
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY,
            Operation<Boolean> original
    ) {
        if (original.call(instance, mouseX, mouseY, scrollX, scrollY)) return true;
        return selectedTab instanceof CreativeMenuTab<?> menuTab && menuTab.mouseScrolled(
                mouseX,
                mouseY,
                scrollX,
                scrollY
        );
    }

    @WrapOperation(
            method = "selectTab", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/NonNullList;size()I"
    ))
    private int positionMenuTabInventorySlots(NonNullList<?> instance, Operation<Integer> original, @Local AbstractContainerMenu abstractContainerMenu) {
        if (!(selectedTab instanceof CreativeMenuTab)) return original.call(instance);

        for (int i = 9; i < 45; i++) {
            CreativeModeInventoryScreen.SlotWrapper wrapped = new CreativeModeInventoryScreen.SlotWrapper(
                    abstractContainerMenu.slots.get(i),
                    i,
                    9 + (i % 9) * 18,
                    i >= 36 ? 142 : 66 + (i / 9) * 18
            );
            wrapped.index = this.menu.slots.size();
            this.menu.slots.add(wrapped);
        }

        return 0;
    }

    @Definition(id = "add", method = "Lnet/minecraft/core/NonNullList;add(Ljava/lang/Object;)Z")
    @Definition(id = "destroyItemSlot", field = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;destroyItemSlot:Lnet/minecraft/world/inventory/Slot;")
    @Expression("?.add(this.destroyItemSlot)")
    @WrapOperation(
            method = "selectTab", at = @At(
            value = "MIXINEXTRAS:EXPRESSION"
    ))
    private boolean addMenuTabSlots(NonNullList<@NotNull Object> instance, Object o, Operation<Boolean> original) {
        boolean result = original.call(instance, o);
        if (selectedTab instanceof CreativeMenuTab<?> menuTab)
            menuTab.getMenu().slots.forEach(slot -> {
                CreativeModeInventoryScreen.SlotWrapper wrapped =
                        new CreativeModeInventoryScreen.SlotWrapper(slot, slot.index, slot.x, slot.y);
                wrapped.index = this.menu.slots.size();
                this.menu.slots.add(wrapped);
            });
        return result;
    }

    @Inject(method = "isInventoryOpen", at = @At("HEAD"), cancellable = true)
    private void ignoreMenuTabs(CallbackInfoReturnable<Boolean> cir) {
        if (selectedTab instanceof CreativeMenuTab) cir.setReturnValue(false);
    }

    @WrapOperation(
            method = "selectTab", at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/world/Container;III)Lnet/minecraft/world/inventory/Slot;"
    ))
    private static Slot moveDestroyItemSlot(Container container, int index, int x, int y, Operation<Slot> original) {
        if (selectedTab instanceof CreativeMenuTab) y += 30;
        return original.call(container, index, x, y);
    }

    @WrapOperation(
            method = "selectTab", at = @At(
            value = "NEW",
            target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;"
    ))
    private static CreativeModeInventoryScreen.SlotWrapper moveSlots(Slot slot, int index, int x, int y, Operation<CreativeModeInventoryScreen.SlotWrapper> original) {
        return original.call(slot, index, x, y);
    }

    @WrapOperation(
            method = "renderBg", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"
    ))
    private static void movePaperDoll(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int a, float b, float c, float d, LivingEntity livingEntity, Operation<Void> original) {
        if (selectedTab instanceof CreativeMenuTab) return;
        original.call(guiGraphics, x1, y1, x2, y2, a, b, c, d, livingEntity);
    }

}
