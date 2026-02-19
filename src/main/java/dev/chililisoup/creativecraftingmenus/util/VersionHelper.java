package dev.chililisoup.creativecraftingmenus.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

//? if < 1.21.11 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
*///?}

public final class VersionHelper {
    public static void drawScrollingString(GuiGraphics guiGraphics, Component text, int center, int minX, int maxX, int minY, int maxY) {
        //? if < 1.21.11 {
        /*AbstractWidget.renderScrollingString(
                guiGraphics,
                Minecraft.getInstance().font,
                text,
                center,
                minX,
                minY,
                maxX,
                maxY,
                -1
        );
        *///?} else {
        guiGraphics.textRenderer().acceptScrolling(
                text,
                center,
                minX,
                maxX,
                minY,
                maxY
        );
        //?}
    }
}
