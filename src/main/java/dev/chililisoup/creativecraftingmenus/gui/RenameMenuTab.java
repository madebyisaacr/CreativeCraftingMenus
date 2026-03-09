package dev.chililisoup.creativecraftingmenus.gui;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.chililisoup.creativecraftingmenus.gui.components.NameLoreEditBox;
import dev.chililisoup.creativecraftingmenus.util.MenuHelper;
import dev.chililisoup.creativecraftingmenus.util.FullTextParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.*;
import java.util.function.Supplier;

public class RenameMenuTab extends CreativeMenuTab<RenameMenuTab.RenameTabMenu> {
    private @Nullable NameLoreEditBox nameLoreBox;

    public RenameMenuTab(Component displayName, Supplier<ItemStack> iconGenerator, String id) {
        super(displayName, iconGenerator, id);
    }

    @Override
    RenameTabMenu createMenu(Player player) {
        return new RenameTabMenu(player);
    }

    @Override
    public void subInit() {
        if (this.screen == null) return;
        if (this.nameLoreBox != null) this.screen.removeWidget(this.nameLoreBox);

        this.nameLoreBox = new NameLoreEditBox(
                screen.getFont(),
                screen.leftPos + 51,
                screen.topPos + 14,
                123,
                56
        );

        this.nameLoreBox.setValueListener(this::onNameLoreChanged);
        this.screen.addRenderableWidget(this.nameLoreBox);

        this.update();
    }

    @Override
    public void drawTitle(TitleDrawer titleDrawer, int x, int y, int color) {
        super.drawTitle(titleDrawer, x, y - 2, color);
    }

    @Override
    public void render(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (this.menu == null) return;

        int left = screen.leftPos + 51;
        int top = screen.topPos + 14;
        int x = left + 113;
        int y = top - 11;

        boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + 11 && mouseY < y + 11;
        if (hovered) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
            guiGraphics.setComponentTooltipForNextFrame(
                    Minecraft.getInstance().font,
                    List.of(
                            Component.translatable("container.creative_crafting_menus.rename.name_lore.help.1"),
                            Component.translatable("container.creative_crafting_menus.rename.name_lore.help.2")
                    ),
                    mouseX,
                    mouseY
            );
        }

        guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                hovered ? BUTTON_HIGHLIGHTED : BUTTON,
                x,
                y,
                11,
                11
        );

        guiGraphics.drawString(Minecraft.getInstance().font, "?", x + 3, y + 2, -1);
    }

    private @Nullable Runnable checkNameLoreClicked(MouseButtonEvent mouseButtonEvent) {
        if (this.menu == null || this.screen == null) return null;

        int left = this.screen.leftPos + 51;
        int top = this.screen.topPos + 14;
        int x = left + 113;
        int y = top - 11;
        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();

        if (mouseX >= x && mouseY >= y && mouseX < x + 11 && mouseY < y + 11) return () -> {
            URI url = URI.create("https://placeholders.pb4.eu/user/quicktext/");

            Minecraft.getInstance().setScreen(new ConfirmLinkScreen(open -> {
                if (open) Util.getPlatform().openUri(url);
                Minecraft.getInstance().setScreen(this.screen);
            }, url.toString(), false));
        };

        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent) {
        return checkNameLoreClicked(mouseButtonEvent) != null;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (this.menu == null) return false;

        Runnable onClick = checkNameLoreClicked(mouseButtonEvent);
        if (onClick != null) {
            onClick.run();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.nameLoreBox != null && this.nameLoreBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.isEscape() || this.screen == null || this.nameLoreBox == null) return false;
        return this.nameLoreBox.keyPressed(keyEvent) || (this.nameLoreBox.isActive() && this.nameLoreBox.isFocused() && this.nameLoreBox.visible);
    }

    private void onNameLoreChanged(String nameLore) {
        if (this.menu != null)
            this.menu.setNameLore(nameLore);
    }

    @Override
    public void remove() {
        if (this.screen != null && this.nameLoreBox != null)
            this.screen.removeWidget(this.nameLoreBox);
        this.nameLoreBox = null;
        super.remove();
    }

    private void update() {
        if (this.menu == null || this.nameLoreBox == null || this.screen == null)
            return;

        String nameLore = this.menu.getNameLore();
        this.nameLoreBox.setValue(nameLore == null ? "" : nameLore);
        this.nameLoreBox.isEditable = nameLore != null;
    }

    public class RenameTabMenu extends CreativeMenuTab.CreativeTabMenu<RenameTabMenu> {
        private final Container inputSlots;
        private final ResultContainer resultSlots = new ResultContainer();

        RenameTabMenu(Player player) {
            super(player);
            this.inputSlots = MenuHelper.simpleContainer(this, 1);
            this.addSlot(new Slot(this.inputSlots, 0, 9, 14));
            this.addSlot(MenuHelper.resultSlot(this, this.resultSlots, 0, 9, 54));
        }

        @Override
        RenameTabMenu copyWithPlayer(@NotNull Player player) {
            return this.copyContentsTo(new RenameTabMenu(player));
        }

        @Override
        public @NotNull ItemStack quickMoveFromInventory(@NotNull Player player, int slotIndex) {
            ItemStack resultStack = ItemStack.EMPTY;
            Slot slot = this.player.inventoryMenu.slots.get(slotIndex);
            if (!slot.hasItem()) return resultStack;

            ItemStack slotStack = slot.getItem();
            resultStack = slotStack.copy();

            if (!this.moveItemStackTo(slotStack, 0, 1, false))
                return ItemStack.EMPTY;

            if (slotStack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (slotStack.getCount() == resultStack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, slotStack);

            return resultStack;
        }

        public @Nullable String getNameLore() {
            ItemStack itemStack = this.resultSlots.getItem(0);
            if (itemStack.isEmpty()) return null;

            ArrayList<String> nameLore = new ArrayList<>();

            Component customName = itemStack.getCustomName();
            Component itemName = itemStack.getItemName();
            Component name = customName == null ?
                    itemName.copy().withStyle(itemName.getStyle().withItalic(
                            itemName.getStyle().italic != null && itemName.getStyle().italic
                    )) :
                    customName;
            nameLore.add(FullTextParser.decompose(name));

            ItemLore lore = itemStack.get(DataComponents.LORE);
            if (lore != null) lore.lines().forEach(line -> nameLore.add(FullTextParser.decompose(line)));

            return String.join("\n", nameLore);
        }

        public void setNameLore(String nameLore) {
            ItemStack itemStack = this.resultSlots.getItem(0);
            if (itemStack.isEmpty()) return;

            if (StringUtil.isBlank(nameLore)) {
                itemStack.remove(DataComponents.CUSTOM_NAME);
                itemStack.set(DataComponents.LORE, ItemLore.EMPTY);
                return;
            }

            ArrayList<String> lines = new ArrayList<>(List.of(nameLore.split("\n")));
            if (lines.isEmpty()) return;

            String name = lines.removeFirst();
            ItemStack inputStack = this.inputSlots.getItem(0);
            itemStack.set(
                    DataComponents.CUSTOM_NAME,
                    inputStack.getHoverName().getString().equals(name) ?
                            inputStack.get(DataComponents.CUSTOM_NAME) :
                            FullTextParser.formatText(name)
            );

            if (lines.isEmpty()) itemStack.set(DataComponents.LORE, ItemLore.EMPTY);
            else {
                ItemLore oldLore = inputStack.get(DataComponents.LORE);
                List<Component> oldLines = oldLore != null ? oldLore.lines() : List.of();

                ArrayList<Component> lore = new ArrayList<>();
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (oldLines.size() > i) {
                        Component oldLine = oldLore.lines().get(i);
                        lore.add(oldLine.getString().equals(line) ? oldLine : FullTextParser.formatText(line));
                    } else lore.add(FullTextParser.formatText(line));
                }

                itemStack.set(DataComponents.LORE, new ItemLore(lore));
            }
        }

        @Override
        public void slotsChanged(@NotNull Container container) {
            if (container == this.inputSlots) {
                ItemStack result = this.inputSlots.getItem(0).copy();
                this.resultSlots.setItem(0, result);
                RenameMenuTab.this.update();
            } else if (container == this.resultSlots) {
                this.inputSlots.clearContent();
                RenameMenuTab.this.update();
            }
        }

        @Override
        public void removed(@NotNull Player player) {
            if (player.hasInfiniteMaterials()) for (int i = 0; i < this.inputSlots.getContainerSize(); i++)
                player.getInventory().placeItemBackInInventory(this.inputSlots.removeItemNoUpdate(i));
        }
    }
}
