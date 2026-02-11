package dev.chililisoup.creativecraftingmenus.reg;

import dev.chililisoup.creativecraftingmenus.CreativeCraftingMenus;
import dev.chililisoup.creativecraftingmenus.gui.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
    }
}
