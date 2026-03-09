package dev.chililisoup.creativecraftingmenus.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.chililisoup.creativecraftingmenus.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(BannerPatternLayers.class)
public abstract class BannerPatternLayersMixin {
    @Shadow public abstract List<BannerPatternLayers.Layer> layers();

    @WrapOperation(method = "addToTooltip", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private int showMoreLines(int layerCount, int maxLines, Operation<Integer> original) {
        return original.call(
                layerCount,
                ModConfig.HANDLER.instance().bannerTooltipChanges ?
                        (this.layers().size() > 9 ? 8 : 9) :
                        maxLines
        );
    }

    @WrapOperation(
            method = "addToTooltip", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;"
    ))
    private MutableComponent highlightUnfriendlyLines(MutableComponent instance, ChatFormatting chatFormatting, Operation<MutableComponent> original, @Local int i) {
        return original.call(instance, chatFormatting);
    }

    @Inject(method = "addToTooltip", at = @At("TAIL"))
    private void addHiddenLineInfo(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter, CallbackInfo ci) {
        if (ModConfig.HANDLER.instance().bannerTooltipChanges && this.layers().size() > 9)
            consumer.accept(Component
                    .translatable("item.container.more_items", this.layers().size() - 8)
                    .withStyle(ChatFormatting.RED)
                    .withStyle(ChatFormatting.ITALIC)
            );
    }
}
