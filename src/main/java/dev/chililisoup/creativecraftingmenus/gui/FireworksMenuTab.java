package dev.chililisoup.creativecraftingmenus.gui;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.chililisoup.creativecraftingmenus.gui.components.DyesGrid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FireworksMenuTab extends CreativeMenuTab<FireworksMenuTab.FireworksTabMenu> {
    private static final int DYES_GRID_LEFT = 99;
    private static final int DYES_GRID_TOP = 16;
    private static final int DURATION_BUTTONS_LEFT = 8;
    private static final int SHAPE_GRID_LEFT = 32;
    private static final int EFFECTS_BUTTONS_LEFT = 74;
    private static final int CONTROLS_TOP = 15;
    private static final int BUTTON_SIZE = 18;

    private static final FireworkExplosion.Shape[] SHAPES = FireworkExplosion.Shape.values();
    private static final int MAX_COLORS = 8;
    private static final Item[] SHAPE_ICONS = {
            Items.FIREWORK_STAR, // SMALL_BALL
            Items.FIRE_CHARGE,  // LARGE_BALL
            Items.GOLD_NUGGET,  // STAR
            Items.CREEPER_HEAD, // CREEPER
            Items.FEATHER,      // BURST
    };

    private final List<DyeColor> selectedColors = new ArrayList<>();
    private int duration = 0;
    private FireworkExplosion.Shape selectedShape = FireworkExplosion.Shape.SMALL_BALL;
    private boolean twinkle = false;
    private boolean trail = false;

    public FireworksMenuTab(Component displayName, Supplier<ItemStack> iconGenerator, String id) {
        super(displayName, iconGenerator, id);
        this.selectedColors.add(DyeColor.WHITE);
    }

    @Override
    FireworksTabMenu createMenu(Player player) {
        return new FireworksTabMenu(player, this);
    }

    @Override
    public void render(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (this.screen == null) return;
        int left = this.screen.leftPos + DYES_GRID_LEFT;
        int top = this.screen.topPos + DYES_GRID_TOP;
        DyesGrid.renderDyes(guiGraphics, left, top, mouseX, mouseY, this.selectedColors);

        int durationLeft = this.screen.leftPos + DURATION_BUTTONS_LEFT;
        int controlsTop = this.screen.topPos + CONTROLS_TOP;
        var font = Minecraft.getInstance().font;
        for (int i = 0; i < 3; i++) {
            int x = durationLeft;
            int y = controlsTop + i * BUTTON_SIZE;
            boolean selected = this.duration == i;
            boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + BUTTON_SIZE && mouseY < y + BUTTON_SIZE;

            if (hovered) {
                if (!selected) guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                guiGraphics.setTooltipForNextFrame(
                        Component.translatable("item.minecraft.firework_rocket.flight").append(" " + (i + 1)),
                        mouseX,
                        mouseY
                );
            }

            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    selected ? BUTTON_SELECTED : (hovered ? BUTTON_HIGHLIGHTED : BUTTON),
                    x,
                    y,
                    BUTTON_SIZE,
                    BUTTON_SIZE
            );

            guiGraphics.renderItem(Items.FIREWORK_ROCKET.getDefaultInstance(), x + 1, y + 1);

            String numStr = String.valueOf(i + 1);
            guiGraphics.drawString(font, numStr, x + BUTTON_SIZE - font.width(numStr) - 1, y + BUTTON_SIZE - font.lineHeight, 0xFFFFFFFF);
        }

        int shapeGridLeft = this.screen.leftPos + SHAPE_GRID_LEFT;
        for (int i = 0; i < SHAPES.length; i++) {
            FireworkExplosion.Shape shape = SHAPES[i];
            int col = i % 2;
            int row = i / 2;
            int x = shapeGridLeft + col * BUTTON_SIZE;
            int y = controlsTop + row * BUTTON_SIZE;
            boolean selected = this.selectedShape == shape;
            boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + BUTTON_SIZE && mouseY < y + BUTTON_SIZE;

            if (hovered) {
                if (!selected) guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                guiGraphics.setTooltipForNextFrame(shape.getName(), mouseX, mouseY);
            }

            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    selected ? BUTTON_SELECTED : (hovered ? BUTTON_HIGHLIGHTED : BUTTON),
                    x,
                    y,
                    BUTTON_SIZE,
                    BUTTON_SIZE
            );

            guiGraphics.renderItem(SHAPE_ICONS[i].getDefaultInstance(), x + 1, y + 1);
        }

        int effectsLeft = this.screen.leftPos + EFFECTS_BUTTONS_LEFT;
        Item[] effectsIcons = {Items.BARRIER, Items.GLOWSTONE_DUST, Items.DIAMOND};
        Component[] effectsTooltips = {Component.translatable("container.creative_crafting_menus.fireworks.no_effect"), Component.translatable("item.minecraft.firework_star.flicker"), Component.translatable("item.minecraft.firework_star.trail")};
        boolean[] effectsSelected = {!this.twinkle && !this.trail, this.twinkle, this.trail};
        for (int i = 0; i < 3; i++) {
            int x = effectsLeft;
            int y = controlsTop + i * BUTTON_SIZE;
            boolean selected = effectsSelected[i];
            boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + BUTTON_SIZE && mouseY < y + BUTTON_SIZE;

            if (hovered) {
                if (!selected) guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                if (effectsTooltips[i] != null) guiGraphics.setTooltipForNextFrame(effectsTooltips[i], mouseX, mouseY);
            }

            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    selected ? BUTTON_SELECTED : (hovered ? BUTTON_HIGHLIGHTED : BUTTON),
                    x,
                    y,
                    BUTTON_SIZE,
                    BUTTON_SIZE
            );

            guiGraphics.renderItem(effectsIcons[i].getDefaultInstance(), x + 1, y + 1);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent) {
        if (this.screen == null) return false;
        return DyesGrid.getClickedDye(
                this.screen.leftPos + DYES_GRID_LEFT,
                this.screen.topPos + DYES_GRID_TOP,
                mouseButtonEvent.x(),
                mouseButtonEvent.y()
        ) != null || getClickedDurationButton(mouseButtonEvent.x(), mouseButtonEvent.y()) >= 0
                || getClickedShape(mouseButtonEvent.x(), mouseButtonEvent.y()) != null
                || getClickedEffectsButton(mouseButtonEvent.x(), mouseButtonEvent.y()) >= 0;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (this.screen == null) return false;
        DyeColor clicked = DyesGrid.getClickedDye(
                this.screen.leftPos + DYES_GRID_LEFT,
                this.screen.topPos + DYES_GRID_TOP,
                mouseButtonEvent.x(),
                mouseButtonEvent.y()
        );
        if (clicked != null) {
            if (this.selectedColors.contains(clicked)) {
                this.selectedColors.remove(clicked);
                if (this.selectedColors.isEmpty()) {
                    this.selectedColors.add(DyeColor.WHITE);
                }
            } else {
                if (this.selectedColors.size() < MAX_COLORS) {
                    this.selectedColors.add(clicked);
                }
            }
            this.updateFireworkSlot();
            return true;
        }
        int durationButton = getClickedDurationButton(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (durationButton >= 0) {
            this.duration = durationButton;
            this.updateFireworkSlot();
            return true;
        }
        FireworkExplosion.Shape clickedShape = getClickedShape(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (clickedShape != null) {
            this.selectedShape = clickedShape;
            this.updateFireworkSlot();
            return true;
        }
        int effectsButton = getClickedEffectsButton(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (effectsButton >= 0) {
            if (effectsButton == 0) {
                this.twinkle = false;
                this.trail = false;
            } else if (effectsButton == 1) {
                this.twinkle = !this.twinkle;
            } else {
                this.trail = !this.trail;
            }
            this.updateFireworkSlot();
            return true;
        }
        return false;
    }

    private int getClickedDurationButton(double mouseX, double mouseY) {
        if (this.screen == null) return -1;
        int left = this.screen.leftPos + DURATION_BUTTONS_LEFT;
        int top = this.screen.topPos + CONTROLS_TOP;
        for (int i = 0; i < 3; i++) {
            int x = left;
            int y = top + i * BUTTON_SIZE;
            if (mouseX >= x && mouseY >= y && mouseX < x + BUTTON_SIZE && mouseY < y + BUTTON_SIZE) {
                return i;
            }
        }
        return -1;
    }

    private @Nullable FireworkExplosion.Shape getClickedShape(double mouseX, double mouseY) {
        if (this.screen == null) return null;
        int left = this.screen.leftPos + SHAPE_GRID_LEFT;
        int top = this.screen.topPos + CONTROLS_TOP;
        for (int i = 0; i < SHAPES.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int x = left + col * BUTTON_SIZE;
            int y = top + row * BUTTON_SIZE;
            if (mouseX >= x && mouseY >= y && mouseX < x + BUTTON_SIZE && mouseY < y + BUTTON_SIZE) {
                return SHAPES[i];
            }
        }
        return null;
    }

    private int getClickedEffectsButton(double mouseX, double mouseY) {
        if (this.screen == null) return -1;
        int left = this.screen.leftPos + EFFECTS_BUTTONS_LEFT;
        int top = this.screen.topPos + CONTROLS_TOP;
        for (int i = 0; i < 3; i++) {
            int x = left;
            int y = top + i * BUTTON_SIZE;
            if (mouseX >= x && mouseY >= y && mouseX < x + BUTTON_SIZE && mouseY < y + BUTTON_SIZE) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack buildFireworkStack() {
        IntList colors = new IntArrayList();
        if (this.selectedColors.isEmpty()) {
            colors.add(DyeColor.WHITE.getFireworkColor());
        } else {
            for (int i = 0; i < this.selectedColors.size() && i < MAX_COLORS; i++) {
                colors.add(this.selectedColors.get(i).getFireworkColor());
            }
        }
        FireworkExplosion explosion = new FireworkExplosion(
                this.selectedShape,
                colors,
                new IntArrayList(),
                this.trail,
                this.twinkle
        );
        Fireworks fireworks = new Fireworks(this.duration + 1, List.of(explosion));
        ItemStack stack = Items.FIREWORK_ROCKET.getDefaultInstance();
        stack.set(DataComponents.FIREWORKS, fireworks);
        return stack;
    }

    private void updateFireworkSlot() {
        if (this.menu != null) {
            this.menu.setFireworkResult(this.buildFireworkStack());
        }
    }

    @Override
    public void subInit() {
        this.updateFireworkSlot();
    }

    public static class FireworksTabMenu extends CreativeMenuTab.CreativeTabMenu<FireworksTabMenu> {
        private final ResultContainer fireworkSlot = new ResultContainer();
        private final FireworksMenuTab tab;

        FireworksTabMenu(Player player, FireworksMenuTab tab) {
            super(player);
            this.tab = tab;
            this.addSlot(new Slot(this.fireworkSlot, 0, 167, 34) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }

                @Override
                public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                    super.onTake(player, stack);
                    this.container.setItem(0, FireworksTabMenu.this.tab.buildFireworkStack());
                }
            });
            this.setFireworkResult(this.tab.buildFireworkStack());
        }

        void setFireworkResult(ItemStack stack) {
            this.fireworkSlot.setItem(0, stack);
        }

        @Override
        FireworksTabMenu copyWithPlayer(@NotNull Player player) {
            return this.copyContentsTo(new FireworksTabMenu(player, this.tab));
        }

        @Override
        public @NotNull ItemStack quickMoveFromInventory(@NotNull Player player, int slotIndex) {
            return ItemStack.EMPTY;
        }
    }
}
