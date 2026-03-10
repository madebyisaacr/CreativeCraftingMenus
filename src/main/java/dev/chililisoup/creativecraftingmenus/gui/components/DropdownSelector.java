package dev.chililisoup.creativecraftingmenus.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.chililisoup.creativecraftingmenus.util.VersionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class DropdownSelector<T> extends ObjectSelectionList<DropdownSelector.Entry<T>> {
    private final int closedHeight;
    private final int openHeight;
    private final int openX;
    private final int openY;
    private @Nullable Component placeholder;
    private @Nullable Consumer<T> selectionCallback;
    public boolean open = false;

    public DropdownSelector(int x, int openX, int openY, int width, int closedHeight, int openHeight) {
        super(Minecraft.getInstance(), width, openHeight, 0, 14);
        this.openX = openX;
        this.openY = openY;
        this.closedHeight = closedHeight;
        this.openHeight = openHeight;
        this.setX(x);
    }

    public void setPlaceholder(@Nullable Component placeholder) {
        this.placeholder = placeholder;
    }

    public void setSelectionCallback(@Nullable Consumer<T> selectionCallback) {
        this.selectionCallback = selectionCallback;
    }

    public void updateEntries(Collection<Entry<T>> entries) {
        this.replaceEntries(List.copyOf(entries));
        this.setHeight(this.openHeight);
        this.setScrollAmount(0.0);
    }

    public @Nullable T value() {
        Entry<T> selected = this.getSelected();
        return selected != null ? selected.value : null;
    }

    @Override
    public void setSelected(DropdownSelector.Entry<T> entry) {
        boolean wasOpen = this.open;
        this.open = false;
        this.playDownSound(this.minecraft.getSoundManager());
        super.setSelected(entry);
        if (wasOpen && this.selectionCallback != null && entry != null) {
            this.selectionCallback.accept(entry.value);
        }
    }

    @Override
    public int getX() {
        return this.open ? this.openX : super.getX();
    }

    @Override
    public int getY() {
        return this.open ? this.openY : super.getY();
    }

    @Override
    public int getWidth() {
        return this.open ? super.getX() - this.openX + super.getWidth() : super.getWidth();
    }

    @Override
    public int getHeight() {
        return this.open ? this.openHeight : this.closedHeight;
    }

    @Override
    public int getRowLeft() {
        return this.getX() + 3;
    }

    @Override
    public int getRowWidth() {
        return this.getWidth() - 6;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.open) {
            this.renderListBackground(guiGraphics);

            if (mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getRight() && mouseY < this.getBottom()) {
                guiGraphics.fill(
                        this.getX(),
                        this.getY(),
                        this.getRight(),
                        this.getBottom(),
                        0x40FFFFFF
                );
                guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
            }

            Entry<T> selected = this.getSelected();
            Component toRender = this.placeholder != null ? this.placeholder : (selected != null ? selected.name : null);
            if (toRender != null) VersionHelper.drawScrollingString(
                    guiGraphics,
                    toRender,
                    this.getX() + 3,
                    this.getX() + 3,
                    this.getRight() - 3,
                    this.getY(),
                    this.getBottom()
            );

            return;
        }

        if (this.isHovered()) guiGraphics.requestCursor(CursorType.DEFAULT);
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent mouseButtonEvent, boolean isDoubleClick) {
        if (!this.open) {
            int button = mouseButtonEvent.button();
            if (button == 0 && !this.children().isEmpty()) {
                this.open = true;
                this.setScrollAmount(0);
                Entry<T> selected = this.getSelected();
                if (selected != null) this.scrollToEntry(selected);
                this.playDownSound(this.minecraft.getSoundManager());
            }
            return true;
        }

        return super.mouseClicked(mouseButtonEvent, isDoubleClick);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent mouseButtonEvent) {
        return this.open && super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent mouseButtonEvent, double mouseX, double mouseY) {
        if (!this.open) return false;
        if (!this.scrolling) return super.mouseDragged(mouseButtonEvent, mouseX, mouseY);

        if (mouseButtonEvent.y() < this.getY()) this.setScrollAmount(0.0);
        else if (mouseButtonEvent.y() > this.getBottom()) this.setScrollAmount(this.maxScrollAmount());
        else this.setScrollAmount(this.scrollAmount() + mouseY * Math.max(
                1.0,
                (double) Math.max(1, this.maxScrollAmount()) / (this.getHeight() -  this.scrollerHeight())
        ));

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.open && super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }


    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getRight() + 15 && mouseY < this.getBottom();
    }

    @Override
    protected int scrollBarX() {
        return this.getRight() + 3;
    }

    @Override
    protected int scrollBarY() {
        return this.scrollbarVisible() ? super.scrollBarY() : this.getY();
    }

    @Override
    protected int scrollerHeight() {
        return 15;
    }

    @Override
    protected boolean isOverScrollbar(double mouseX, double mouseY) {
        return mouseX >= this.scrollBarX() && mouseX < this.scrollBarX() + 12 && mouseY >= this.getY() && mouseY < this.getBottom();
    }

    @Override
    protected void renderScrollbar(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                this.scrollbarVisible() ? StonecutterScreen.SCROLLER_SPRITE : StonecutterScreen.SCROLLER_DISABLED_SPRITE,
                this.scrollBarX(),
                this.scrollBarY(),
                12,
                this.scrollerHeight()
        );

        if (this.scrollbarVisible() && this.isOverScrollbar(mouseX, mouseY))
            guiGraphics.requestCursor(this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
    }

    public static class Entry<T> extends ObjectSelectionList.Entry<DropdownSelector.Entry<T>> {
        final Component name;
        final T value;

        public Entry(final Component name, final T value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.translatable("narrator.select", this.name);
        }

        @Override
        public void renderContent(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (hovered) {
                guiGraphics.fill(
                        this.getContentX(),
                        this.getContentY(),
                        this.getContentRight(),
                        this.getContentBottom(),
                        0x40FFFFFF
                );
                guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
            }

            VersionHelper.drawScrollingString(
                    guiGraphics,
                    this.name,
                    this.getContentX(),
                    this.getContentX(),
                    this.getContentRight(),
                    this.getContentY(),
                    this.getContentBottom()
            );
        }
    }
}
