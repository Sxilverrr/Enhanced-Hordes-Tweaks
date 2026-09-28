package com.enhancedhordes.tweaks.datapack;

import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EnhancedHordesTweaksPackResources implements PackResources {

    private final PackLocationInfo location;
    private final Map<ResourceLocation, String> jsonCache = new HashMap<>();

    public EnhancedHordesTweaksPackResources(PackLocationInfo location) {
        this.location = location;
        tag("entity_type/hordes", EnhancedHordesTweaksConfig.enableHordeStacking || EnhancedHordesTweaksConfig.enableHordeMultiplying ? EnhancedHordesTweaksConfig.hordeMobs : List.of());
        tag("entity_type/intelligent_teams", !EnhancedHordesTweaksConfig.enableIntelligentTeams ? List.of()
                : EnhancedHordesTweaksConfig.enableWitherSkeletonBowTactics ? EnhancedHordesTweaksConfig.intelligentTeamMobs
                : without(EnhancedHordesTweaksConfig.intelligentTeamMobs, "minecraft:wither_skeleton"));
        tag("entity_type/leaping_mobs", EnhancedHordesTweaksConfig.enableLeapingMobs ? EnhancedHordesTweaksConfig.leapingMobs : List.of());
        tag("entity_type/horde_grave_robbers", EnhancedHordesTweaksConfig.enableHordeMultiplying ? EnhancedHordesTweaksConfig.graveRobbers : List.of());
        tag("entity_type/intelligent_piglin", !EnhancedHordesTweaksConfig.enableIntelligentPiglins ? List.of()
                : EnhancedHordesTweaksConfig.enableZombifiedPiglinCrossbow ? EnhancedHordesTweaksConfig.intelligentPiglins
                : without(EnhancedHordesTweaksConfig.intelligentPiglins, "minecraft:zombified_piglin"));
        tag("block/hidden_zombie_blocks", EnhancedHordesTweaksConfig.enableHiddenZombies ? EnhancedHordesTweaksConfig.hiddenZombieBlocks : List.of());
        tag("block/horde_breakable", EnhancedHordesTweaksConfig.enableHordeBlockBreaking ? EnhancedHordesTweaksConfig.hordeBreakableBlocks : List.of("minecraft:air"));
    }

    private void tag(String path, List<? extends String> values) {
        jsonCache.put(ResourceLocation.fromNamespaceAndPath("forge", "tags/" + path + ".json"), values.stream()
                .map(v -> v.startsWith("#") ? "{\"id\":\"" + v + "\",\"required\":false}" : "\"" + v + "\"")
                .collect(Collectors.joining(",", "{\"replace\":true,\"values\":[", "]}")));
    }

    private static List<? extends String> without(List<? extends String> list, String id) {
        return list.stream().filter(s -> !s.equals(id)).toList();
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.SERVER_DATA) return null;
        String json = jsonCache.get(location);
        if (json == null) return null;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, PackResources.ResourceOutput output) {
        if (type != PackType.SERVER_DATA || !namespace.equals("forge")) return;
        jsonCache.forEach((loc, json) -> {
            if (path.isEmpty() || loc.getPath().startsWith(path + "/")) {
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                output.accept(loc, () -> new ByteArrayInputStream(bytes));
            }
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.SERVER_DATA ? Set.of("forge") : Set.of();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) throws IOException {
        if (deserializer == PackMetadataSection.TYPE) {
            return (T) new PackMetadataSection(
                    Component.literal("Enhanced Hordes Tweaks data"),
                    48,
                    Optional.empty()
            );
        }
        return null;
    }

    @Override
    public PackLocationInfo location() {
        return location;
    }

    @Override
    public void close() {
    }
}
