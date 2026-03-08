package dev.chililisoup.creativecraftingmenus.reg;

import dev.chililisoup.creativecraftingmenus.CreativeCraftingMenus;
import dev.chililisoup.creativecraftingmenus.gui.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.*;
import java.util.function.Supplier;

public class CreativeMenuTabs {
    public static final List<CreativeMenuTab<?>> MENU_TABS;

    static {
        MENU_TABS = List.of(
                register(
                        AnvilMenuTab::new,
                        "anvil",
                        Items.ANVIL::getDefaultInstance
                ),
                register(
                        CraftingMenuTab::new,
                        "crafting",
                        Items.CRAFTING_TABLE::getDefaultInstance
                ),
                register(
                        LoomMenuTab::new,
                        "loom",
                        Items.LOOM::getDefaultInstance
                ),
                register(
                        SmithingMenuTab::new,
                        "smithing",
                        Items.SMITHING_TABLE::getDefaultInstance
                )
        );
    }

    private static<M extends CreativeMenuTab.CreativeTabMenu<M>, T extends CreativeMenuTab<M>> T register(
            CreativeMenuTab.MenuTabConstructor<M, T> constructor,
            String name,
            Supplier<ItemStack> iconGenerator
    ) {
        Identifier id = CreativeCraftingMenus.id(name);

        T menuTab = new CreativeMenuTab.Builder<>(constructor)
                .title(Component.translatable("container.creative_crafting_menus." + name))
                .backgroundTexture(CreativeCraftingMenus.id(
                        String.format("textures/gui/container/creative_%s_menu.png", name)
                ))
                .icon(iconGenerator)
                .id(id.toString())
                .build();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, menuTab);
        FabricCreativeGuiComponents.COMMON_GROUPS.add(menuTab);
        return menuTab;
    }

    public static void init() {
        ClientPlayConnectionEvents.DISCONNECT.register((a, b) -> MENU_TABS.forEach(CreativeMenuTab::dispose));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
            ItemStack unlitCampfire = new ItemStack(Items.CAMPFIRE);
            unlitCampfire.set(DataComponents.BLOCK_STATE,
                    BlockItemStateProperties.EMPTY.with(BlockStateProperties.LIT, false));
            unlitCampfire.set(DataComponents.CUSTOM_NAME, Component.literal("Unlit Campfire"));
            entries.accept(unlitCampfire);

            ItemStack unlitSoulCampfire = new ItemStack(Items.SOUL_CAMPFIRE);
            unlitSoulCampfire.set(DataComponents.BLOCK_STATE,
                    BlockItemStateProperties.EMPTY.with(BlockStateProperties.LIT, false));
            unlitSoulCampfire.set(DataComponents.CUSTOM_NAME, Component.literal("Unlit Soul Campfire"));
            entries.accept(unlitSoulCampfire);
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            ItemStack ominousVault = new ItemStack(Items.VAULT);
            ominousVault.set(DataComponents.BLOCK_STATE,
                    BlockItemStateProperties.EMPTY.with(BlockStateProperties.OMINOUS, true));
            ominousVault.set(DataComponents.CUSTOM_NAME, Component.literal("Ominous Vault"));
            entries.accept(ominousVault);

            ItemStack ominousTrialSpawner = new ItemStack(Items.TRIAL_SPAWNER);
            ominousTrialSpawner.set(DataComponents.BLOCK_STATE,
                    BlockItemStateProperties.EMPTY.with(BlockStateProperties.OMINOUS, true));
            ominousTrialSpawner.set(DataComponents.CUSTOM_NAME, Component.literal("Ominous Trial Spawner"));
            entries.accept(ominousTrialSpawner);
        });
    }
}
