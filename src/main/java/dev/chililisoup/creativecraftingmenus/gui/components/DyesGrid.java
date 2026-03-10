package dev.chililisoup.creativecraftingmenus.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.chililisoup.creativecraftingmenus.CreativeCraftingMenus;
import dev.chililisoup.creativecraftingmenus.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import org.jetbrains.annotations.Nullable;

/**
 * Reusable dyes grid component for menu tabs. Renders a 4x4 grid of dye colors with
 * hover, tooltip, and click handling.
 */
public class DyesGrid {

    public static final DyeColor[] COLORS = {
            DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK,
            DyeColor.BROWN, DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW,
            DyeColor.LIME, DyeColor.GREEN, DyeColor.CYAN, DyeColor.LIGHT_BLUE,
            DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK
    };

    private static final int CELL_SIZE = 14;
    private static final int COLUMNS = 4;

    private static final Identifier BUTTON = CreativeCraftingMenus.id("widget/button_unselected");
    private static final Identifier BUTTON_HIGHLIGHTED = CreativeCraftingMenus.id("widget/button_highlighted");
    private static final Identifier BUTTON_SELECTED = CreativeCraftingMenus.id("widget/button_selected");

    /**
     * Renders the dyes grid and handles hover (cursor) and tooltip display.
     *
     * @param guiGraphics the graphics context
     * @param left        left edge of the grid
     * @param top         top edge of the grid
     * @param mouseX      mouse X position
     * @param mouseY      mouse Y position
     * @param selected    the currently selected dye color
     */
    public static void renderDyes(
            GuiGraphics guiGraphics,
            int left,
            int top,
            int mouseX,
            int mouseY,
            DyeColor selected
    ) {
        for (int i = 0; i < COLORS.length; i++) {
            int x = left + (i % COLUMNS) * CELL_SIZE;
            int y = top + (i / COLUMNS) * CELL_SIZE;

            DyeColor color = COLORS[i];
            boolean selectedState = selected == color;
            boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + CELL_SIZE && mouseY < y + CELL_SIZE;

            if (hovered) {
                if (!selectedState) guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                guiGraphics.setTooltipForNextFrame(
                        Component.translatable("color.minecraft." + color.getSerializedName()),
                        mouseX,
                        mouseY);
            }

            guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    selectedState ? BUTTON_SELECTED : (hovered ? BUTTON_HIGHLIGHTED : BUTTON),
                    x,
                    y,
                    CELL_SIZE,
                    CELL_SIZE
            );

            renderDyeIcon(guiGraphics, color, x, y);
        }
    }

    /**
     * Checks if a dye was clicked at the given mouse position. Call this from
     * mouseClicked or mouseReleased to determine which dye (if any) was selected.
     *
     * @param left   left edge of the grid (same as passed to renderDyes)
     * @param top    top edge of the grid (same as passed to renderDyes)
     * @param mouseX mouse X position
     * @param mouseY mouse Y position
     * @param selected the currently selected dye color (clicks on this return null)
     * @return the clicked dye color, or null if none was clicked or the selected one was clicked
     */
    @Nullable
    public static DyeColor getClickedDye(
            int left,
            int top,
            double mouseX,
            double mouseY,
            DyeColor selected
    ) {
        for (int i = 0; i < COLORS.length; i++) {
            DyeColor color = COLORS[i];
            if (selected == color) continue;

            int x = left + (i % COLUMNS) * CELL_SIZE;
            int y = top + (i / COLUMNS) * CELL_SIZE;
            if (mouseX >= x && mouseY >= y && mouseX < x + CELL_SIZE && mouseY < y + CELL_SIZE)
                return color;
        }

        return null;
    }

    /**
     * Renders a single dye color icon (dye item or colored square based on config).
     * Useful for other UI elements that display dye colors.
     */
    public static void renderDyeIcon(GuiGraphics guiGraphics, DyeColor color, int x, int y) {
        if (ModConfig.HANDLER.instance().dyeItemColorIcons) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(x + 1, y);
            guiGraphics.pose().scale(3F / 4F);
            guiGraphics.renderItem(DyeItem.byColor(color).getDefaultInstance(), 0, 0);
            guiGraphics.pose().popMatrix();
        } else {
            guiGraphics.fill(x + 2, y + 2, x + 12, y + 12, color.getTextureDiffuseColor());
        }
    }
}
