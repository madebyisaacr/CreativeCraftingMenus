package dev.chililisoup.creativecraftingmenus.util;

import dev.chililisoup.creativecraftingmenus.CreativeCraftingMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.*;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.WorldDataConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//? if < 1.21.11 {
/*import net.minecraft.Util;
import net.fabricmc.fabric.impl.resource.loader.ModResourcePackUtil;
*///?} else {
import net.minecraft.util.Util;
import net.fabricmc.fabric.impl.resource.pack.ModPackResourcesUtil;
 //?}

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public final class ServerResourceProvider {
    private static final CompletableFuture<Unit> DATA_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
    private static @Nullable RecipeManager RECIPE_MANAGER;
    private static boolean RECIPE_LOAD_ATTEMPTED;

    public static @Nullable RecipeManager getRecipeManager() {
        @Nullable IntegratedServer singleplayerServer = Minecraft.getInstance().getSingleplayerServer();
        if (singleplayerServer != null) return singleplayerServer.getRecipeManager();
        return getVanillaRecipeManager();
    }

    public static<T> List<T> getFromRegistry(ResourceKey<@NotNull Registry<@NotNull T>> key) {
        @Nullable RegistryAccess registryAccess = registryAccess();
        if (registryAccess != null) {
            var reg = registryAccess.lookup(key);
            if (reg.isPresent()) return reg.get().stream().toList();
        }

        return List.of();
    }

    public static List<Item> getFromTag(TagKey<@NotNull Item> tag) {
        return BuiltInRegistries.ITEM.get(tag).map(
                holders -> holders.stream().map(Holder::value).toList()
        ).orElseGet(List::of);
//        return getFromPredicate(ref -> ref.tags().anyMatch(itemTag -> itemTag.equals(tag)));
    }

    public static List<Item> getFromComponent(DataComponentType<?> component) {
        return getFromPredicate(ref -> ref.value().components().has(component));
    }

    public static<T> List<Holder.Reference<@NotNull T>> getRegistryElements(
            ResourceKey<@NotNull Registry<@NotNull T>> key
    ) {
        @Nullable RegistryAccess registryAccess = registryAccess();
        return registryAccess == null ?
                List.of() :
                registryAccess
                        .lookupOrThrow(key)
                        .listElements()
                        .toList();
    }

    public static<T> Holder.@Nullable Reference<@NotNull T> getRegistryElement(
            ResourceKey<@NotNull Registry<@NotNull T>> key,
            Identifier id
    ) {
        @Nullable RegistryAccess registryAccess = registryAccess();
        return registryAccess == null ?
                null :
                registryAccess
                        .lookupOrThrow(key)
                        .get(id).orElse(null);
    }

    public static List<Item> getFromPredicate(Predicate<Holder.Reference<@NotNull Item>> predicate) {
        @Nullable RegistryAccess registryAccess = registryAccess();
        if (registryAccess != null) {
            var reg = registryAccess.lookup(Registries.ITEM);
            if (reg.isPresent()) return reg.get().listElements()
                    .filter(predicate)
                    .sorted(Comparator.comparing(reference -> reference.key().identifier()))
                    .map(Holder.Reference::value).toList();
        }

        return List.of();
    }

    public static @Nullable RegistryAccess registryAccess() {
        @Nullable ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.registryAccess();
    }

    private static @Nullable RecipeManager getVanillaRecipeManager() {
        if (RECIPE_MANAGER != null) return RECIPE_MANAGER;
        if (RECIPE_LOAD_ATTEMPTED) return null;

        RECIPE_LOAD_ATTEMPTED = true;
        try { RECIPE_MANAGER = createVanillaRecipeManager(); }
        catch (Exception e) { CreativeCraftingMenus.LOGGER.error("Unable to load recipes!", e); }

        return RECIPE_MANAGER;
    }

    private static RecipeManager createVanillaRecipeManager() {
        PackRepository packRepository =
                /*? if < 1.21.11 {*/ /*ModResourcePackUtil *//*?} else {*/ ModPackResourcesUtil /*?}*/
                .createClientManager();

        MinecraftServer.configurePackRepository(
                packRepository, WorldDataConfiguration.DEFAULT, false, false
        );
        CloseableResourceManager closeableResourceManager = new MultiPackResourceManager(
                PackType.SERVER_DATA,
                packRepository.openAllSelected()
        );

        RecipeManager recipeManager = new RecipeManager(Objects.requireNonNull(registryAccess()));

        Minecraft.getInstance().managedBlock(
                ProfiledReloadInstance.of(
                        closeableResourceManager,
                        List.of(recipeManager),
                        Util.backgroundExecutor(),
                        Minecraft.getInstance(),
                        DATA_RELOAD_INITIAL_TASK
                )
                .done()
                .whenComplete((object, throwable) -> {
                    if (throwable != null) closeableResourceManager.close();
                })::isDone
        );

        return recipeManager;
    }
}
