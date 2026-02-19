package dev.chililisoup.creativecraftingmenus.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.NotNull;

public class NameLoreEditBox extends MultiLineEditBox {
    public boolean isEditable = true;

    public NameLoreEditBox(
            Font font,
            int x,
            int y,
            int width,
            int height
    ) {
        super(
                font,
                x,
                y,
                width,
                height,
                CommonComponents.EMPTY,
                CommonComponents.EMPTY,
                0xFFE0E0E0,
                true,
                0xFFD0D0D0,
                true,
                true
        );
    }

    @Override
    public boolean isFocused() {
        return this.isEditable && super.isFocused();
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        return this.isEditable && super.keyPressed(event);
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
}
