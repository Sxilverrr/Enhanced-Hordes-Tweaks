package com.enhancedhordes.tweaks.config;

import net.minecraft.core.Registry;
//? if >=1.20.1 {
import net.minecraft.core.registries.Registries;
//?}
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class ConfigCache {

    private ConfigCache() {}

    private static volatile boolean dirty = true;

    private static IdTagSet<EntityType<?>> hordeMobs = IdTagSet.empty();
    private static IdTagSet<EntityType<?>> intelligentTeamMobs = IdTagSet.empty();
    private static IdTagSet<EntityType<?>> hostileMobs = IdTagSet.empty();
    private static IdTagSet<EntityType<?>> hostilityTargetMobs = IdTagSet.empty();

    private static IdTagSet<Block> hordeBreakableBlocks = IdTagSet.empty();
    private static List<IdTagSet<Block>> tierBlocks = Collections.nCopies(4, IdTagSet.empty());
    private static IdTagSet<Block> blacklistBlocks = IdTagSet.empty();

    public static void markDirty() {
        dirty = true;
    }

    private static void rebuildIfNeeded() {
        if (!dirty) return;
        synchronized (ConfigCache.class) {
            if (!dirty) return;
            rebuild();
            dirty = false;
        }
    }

    private static void rebuild() {
        hordeMobs = entities(EnhancedHordesTweaksConfig.hordeMobs);
        intelligentTeamMobs = entities(EnhancedHordesTweaksConfig.intelligentTeamMobs);
        hostileMobs = entities(EnhancedHordesTweaksConfig.hostileMobs);
        hostilityTargetMobs = entities(EnhancedHordesTweaksConfig.hostilityTargetMobs);

        hordeBreakableBlocks = blocks(EnhancedHordesTweaksConfig.hordeBreakableBlocks);

        List<IdTagSet<Block>> tiers = new ArrayList<>();
        List<String> cumulative = new ArrayList<>();
        for (List<? extends String> tier : Arrays.asList(
                EnhancedHordesTweaksConfig.hordeMentalityTier1Blocks,
                EnhancedHordesTweaksConfig.hordeMentalityTier2Blocks,
                EnhancedHordesTweaksConfig.hordeMentalityTier3Blocks,
                EnhancedHordesTweaksConfig.hordeMentalityTier4Blocks)) {
            if (tier != null) cumulative.addAll(tier);
            tiers.add(blocks(cumulative));
        }
        tierBlocks = tiers;

        blacklistBlocks = blocks(EnhancedHordesTweaksConfig.hordeMentalityBlacklistBlocks);
    }

    private static IdTagSet<EntityType<?>> entities(List<? extends String> list) {
        //? if >=1.20.1 {
        return parse(list, Registries.ENTITY_TYPE, ForgeRegistries.ENTITY_TYPES);
        //?} else if >=1.19.2 {
        /*return parse(list, Registry.ENTITY_TYPE_REGISTRY, ForgeRegistries.ENTITY_TYPES);*/
        //?} else {
        /*return parse(list, Registry.ENTITY_TYPE_REGISTRY, ForgeRegistries.ENTITIES);*/
        //?}
    }

    private static IdTagSet<Block> blocks(List<? extends String> list) {
        //? if >=1.20.1 {
        return parse(list, Registries.BLOCK, ForgeRegistries.BLOCKS);
        //?} else {
        /*return parse(list, Registry.BLOCK_REGISTRY, ForgeRegistries.BLOCKS);*/
        //?}
    }

    //? if >=1.19.2 {
    private static <T> IdTagSet<T> parse(
    //?} else {
    /*private static <T extends net.minecraftforge.registries.IForgeRegistryEntry<T>> IdTagSet<T> parse(*/
    //?}
            List<? extends String> list, ResourceKey<? extends Registry<T>> key, IForgeRegistry<T> registry) {
        Set<T> ids = new HashSet<>();
        Set<TagKey<T>> tags = new HashSet<>();
        if (list == null) return new IdTagSet<>(ids, tags);
        for (String entry : list) {
            boolean isTag = entry.startsWith("#");
            ResourceLocation rl = ResourceLocation.tryParse(isTag ? entry.substring(1) : entry);
            if (rl == null) continue;
            if (isTag) tags.add(TagKey.create(key, rl));
            else if (registry.containsKey(rl)) ids.add(registry.getValue(rl));
        }
        return new IdTagSet<>(ids, tags);
    }

    public static boolean isHordeMob(EntityType<?> type) {
        rebuildIfNeeded();
        return hordeMobs.contains(type, type::is);
    }

    public static boolean isIntelligentTeamMob(EntityType<?> type) {
        rebuildIfNeeded();
        return intelligentTeamMobs.contains(type, type::is);
    }

    public static boolean isHostileMob(EntityType<?> type) {
        rebuildIfNeeded();
        return hostileMobs.contains(type, type::is);
    }

    public static boolean isHostilityTarget(EntityType<?> type) {
        rebuildIfNeeded();
        return hostilityTargetMobs.contains(type, type::is);
    }

    public static boolean hasHordeMobs() {
        rebuildIfNeeded();
        return !hordeMobs.isEmpty();
    }

    public static boolean isHordeBreakable(BlockState state) {
        rebuildIfNeeded();
        return hordeBreakableBlocks.contains(state.getBlock(), state::is);
    }

    public static boolean isMentalityBlacklisted(BlockState state) {
        rebuildIfNeeded();
        return blacklistBlocks.contains(state.getBlock(), state::is);
    }

    public static boolean isBreakableAtTier(BlockState state, int qualifiedTier) {
        rebuildIfNeeded();
        if (qualifiedTier < 1) return false;
        return tierBlocks.get(Math.min(qualifiedTier, 4) - 1).contains(state.getBlock(), state::is);
    }

    private record IdTagSet<T>(Set<T> ids, Set<TagKey<T>> tags) {

        static <T> IdTagSet<T> empty() {
            return new IdTagSet<>(Set.of(), Set.of());
        }

        boolean contains(T value, Predicate<TagKey<T>> inTag) {
            if (ids.contains(value)) return true;
            for (TagKey<T> tag : tags) {
                if (inTag.test(tag)) return true;
            }
            return false;
        }

        boolean isEmpty() {
            return ids.isEmpty() && tags.isEmpty();
        }
    }
}
