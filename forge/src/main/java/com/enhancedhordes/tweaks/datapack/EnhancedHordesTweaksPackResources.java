package com.enhancedhordes.tweaks.datapack;
import com.enhancedhordes.tweaks.util.VersionCompat;

import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
//? if >=1.20.1 {
import net.minecraft.server.packs.resources.IoSupplier;
//?}
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EnhancedHordesTweaksPackResources implements PackResources {

    private static final String PACK_ID = "builtin/enhanced_hordes_tweaks";

    private final Map<ResourceLocation, String> jsonCache = new HashMap<>();

    public EnhancedHordesTweaksPackResources() {
        tag("entity_types/hordes", EnhancedHordesTweaksConfig.enableHordeStacking || EnhancedHordesTweaksConfig.enableHordeMultiplying ? EnhancedHordesTweaksConfig.hordeMobs : List.of());
        tag("entity_types/intelligent_teams", !EnhancedHordesTweaksConfig.enableIntelligentTeams ? List.of()
                : EnhancedHordesTweaksConfig.enableWitherSkeletonBowTactics ? EnhancedHordesTweaksConfig.intelligentTeamMobs
                : without(EnhancedHordesTweaksConfig.intelligentTeamMobs, "minecraft:wither_skeleton"));
        tag("entity_types/leaping_mobs", EnhancedHordesTweaksConfig.enableLeapingMobs ? EnhancedHordesTweaksConfig.leapingMobs : List.of());
        tag("entity_types/horde_grave_robbers", EnhancedHordesTweaksConfig.enableHordeMultiplying ? EnhancedHordesTweaksConfig.graveRobbers : List.of());
        tag("entity_types/intelligent_piglin", !EnhancedHordesTweaksConfig.enableIntelligentPiglins ? List.of()
                : EnhancedHordesTweaksConfig.enableZombifiedPiglinCrossbow ? EnhancedHordesTweaksConfig.intelligentPiglins
                : without(EnhancedHordesTweaksConfig.intelligentPiglins, "minecraft:zombified_piglin"));
        tag("blocks/hidden_zombie_blocks", EnhancedHordesTweaksConfig.enableHiddenZombies ? EnhancedHordesTweaksConfig.hiddenZombieBlocks : List.of());
        tag("blocks/horde_breakable", EnhancedHordesTweaksConfig.enableHordeBlockBreaking ? EnhancedHordesTweaksConfig.hordeBreakableBlocks : List.of("minecraft:air"));
    }

    private void tag(String path, List<? extends String> values) {
        jsonCache.put(new ResourceLocation("forge", "tags/" + path + ".json"), values.stream()
                .map(v -> v.startsWith("#") ? "{\"id\":\"" + v + "\",\"required\":false}" : "\"" + v + "\"")
                .collect(Collectors.joining(",", "{\"replace\":true,\"values\":[", "]}")));
    }

    private static List<? extends String> without(List<? extends String> list, String id) {
        return list.stream().filter(s -> !s.equals(id)).toList();
    }

    //? if >=1.20.1 {
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
    public String packId() {
        return PACK_ID;
    }
    //?} else {
    /*@Nullable
    @Override
    public InputStream getRootResource(String fileName) {
        return null;
    }

    @Nullable
    @Override
    public InputStream getResource(PackType type, ResourceLocation location) {
        if (type != PackType.SERVER_DATA) return null;
        String json = jsonCache.get(location);
        if (json == null) return null;
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean hasResource(PackType type, ResourceLocation location) {
        return type == PackType.SERVER_DATA && jsonCache.containsKey(location);
    }

    @Override
    public String getName() {
        return PACK_ID;
    }*/
    //?}

    //? if >=1.20.1 {
    //?} else if >=1.19.2 {
    /*@Override
    public java.util.Collection<ResourceLocation> getResources(PackType type, String namespace, String path, java.util.function.Predicate<ResourceLocation> filter) {
        java.util.List<ResourceLocation> out = new java.util.ArrayList<>();
        if (type != PackType.SERVER_DATA || !namespace.equals("forge")) return out;
        for (ResourceLocation loc : jsonCache.keySet()) {
            if ((path.isEmpty() || loc.getPath().startsWith(path + "/")) && filter.test(loc)) out.add(loc);
        }
        return out;
    }*/
    //?} else {
    /*@Override
    public java.util.Collection<ResourceLocation> getResources(PackType type, String namespace, String path, int maxDepth, java.util.function.Predicate<String> filter) {
        java.util.List<ResourceLocation> out = new java.util.ArrayList<>();
        if (type != PackType.SERVER_DATA || !namespace.equals("forge")) return out;
        for (ResourceLocation loc : jsonCache.keySet()) {
            if ((path.isEmpty() || loc.getPath().startsWith(path + "/")) && filter.test(loc.getPath())) out.add(loc);
        }
        return out;
    }*/
    //?}

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.SERVER_DATA ? Set.of("forge") : Set.of();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) throws IOException {
        //? if >=1.20.1 {
        if (deserializer == PackMetadataSection.TYPE) {
        //?} else {
        /*if (deserializer == PackMetadataSection.SERIALIZER) {*/
        //?}
            return (T) new PackMetadataSection(
                    VersionCompat.literal("Enhanced Hordes Tweaks data"),
                    //? if >=1.20.1 {
                    15
                    //?} else {
                    /*PackType.SERVER_DATA.getVersion(net.minecraft.SharedConstants.getCurrentVersion())*/
                    //?}
            );
        }
        return null;
    }

    @Override
    public void close() {
    }
}
