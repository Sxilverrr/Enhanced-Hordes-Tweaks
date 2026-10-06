package com.enhancedhordes.tweaks.events;
import com.enhancedhordes.tweaks.util.VersionCompat;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.compat.GameStagesCompat;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import com.enhancedhordes.tweaks.util.FeatureGate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
//? if >=1.19.2 {
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
//?} else {
/*import net.minecraftforge.event.entity.EntityJoinWorldEvent;*/
//?}
//? if >=1.19.2 {
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
//?} else {
/*import net.minecraftforge.event.entity.EntityLeaveWorldEvent;*/
//?}
import net.minecraftforge.event.entity.living.LivingEvent;
//? if >=1.20.1 {
import net.minecraftforge.event.entity.living.MobSpawnEvent;
//?} else {
/*import net.minecraftforge.event.entity.living.LivingSpawnEvent;*/
//?}
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HordeDeterminationHandler {

    private static final Map<UUID, DeterminationRecord> RECORDS = new ConcurrentHashMap<>();
    private static final int PRUNE_INTERVAL_TICKS = 20 * 30;
    private static final String FORCED_PERSISTENCE_TAG = "eht_forced_persistence";

    private record DeterminationRecord(UUID playerUuid, long startTick) {}

    public static UUID getFollowedPlayer(UUID mobUuid) {
        DeterminationRecord r = RECORDS.get(mobUuid);
        return r == null ? null : r.playerUuid;
    }

    public static void seedFrom(UUID observerUuid, UUID sourceMobUuid) {
        DeterminationRecord source = RECORDS.get(sourceMobUuid);
        if (source != null) {
            RECORDS.putIfAbsent(observerUuid, new DeterminationRecord(source.playerUuid, source.startTick));
        }
    }

    @SubscribeEvent
    //? if >=1.19.2 {
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
    //?} else {
    /*public static void onLivingTick(LivingEvent.LivingUpdateEvent event) {*/
    //?}
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!EnhancedHordesTweaksConfig.enableHordeDetermination) return;
        if (!(VersionCompat.level(mob) instanceof ServerLevel level)) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                level, EnhancedHordesTweaksConfig.hordeDeterminationDaysBeforeActivation)) return;
        if (!ConfigCache.isHordeMob(mob.getType())) return;
        if (FeatureGate.blocked(mob)) return;

        long gameTime = level.getGameTime();
        long day = EnhancedHordesTweaksConfig.day(level);
        int maxDistance = computeFollowDistance(day);
        int maxTimeMinutes = computeFollowTimeMinutes(day);
        long maxTicks = (long) maxTimeMinutes * 60L * 20L;

        LivingEntity target = mob.getTarget();

        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                mob.setTarget(null);
                RECORDS.remove(mob.getUUID());
                return;
            }
            if (!GameStagesCompat.allows(player, EnhancedHordesTweaksConfig.hordeDeterminationStage)) {
                RECORDS.remove(mob.getUUID());
                return;
            }
            RECORDS.compute(mob.getUUID(), (k, existing) -> {
                if (existing == null || !existing.playerUuid.equals(player.getUUID())) {
                    return new DeterminationRecord(player.getUUID(), gameTime);
                }
                return existing;
            });
            return;
        }

        DeterminationRecord record = RECORDS.get(mob.getUUID());
        if (record == null) return;

        if (maxTimeMinutes > 0 && (gameTime - record.startTick) > maxTicks) {
            RECORDS.remove(mob.getUUID());
            return;
        }

        Player player = level.getPlayerByUUID(record.playerUuid);
        if (player == null || player.isRemoved() || player.isSpectator()) {
            return;
        }
        if (player.isCreative()
                || !GameStagesCompat.allows(player, EnhancedHordesTweaksConfig.hordeDeterminationStage)) {
            RECORDS.remove(mob.getUUID());
            return;
        }

        double distSqr = mob.distanceToSqr(player);
        if (distSqr > (double) maxDistance * maxDistance) {
            RECORDS.remove(mob.getUUID());
            return;
        }

        mob.setTarget(player);
    }

    @SubscribeEvent
    //? if >=1.20.1 {
    public static void onDespawnCheck(MobSpawnEvent.AllowDespawn event) {
    //?} else {
    /*public static void onDespawnCheck(LivingSpawnEvent.AllowDespawn event) {*/
    //?}
        if (EnhancedHordesTweaksConfig.enableHordeDetermination && RECORDS.containsKey(event.getEntity().getUUID())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    //? if >=1.19.2 {
    public static void onEntityJoin(EntityJoinLevelEvent event) {
    //?} else {
    /*public static void onEntityJoin(EntityJoinWorldEvent event) {*/
    //?}
        //? if >=1.19.2 {
        if (event.getLevel().isClientSide()) return;
        //?} else {
        /*if (event.getWorld().isClientSide()) return;*/
        //?}
        if (!(event.getEntity() instanceof Mob mob)) return;
        CompoundTag data = mob.getPersistentData();
        if (!data.getBoolean(FORCED_PERSISTENCE_TAG)) return;
        data.remove(FORCED_PERSISTENCE_TAG);
        mob.persistenceRequired = false;
    }

    @SubscribeEvent
    //? if >=1.19.2 {
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
    //?} else {
    /*public static void onEntityLeave(EntityLeaveWorldEvent event) {*/
    //?}
        if (event.getEntity().isRemoved()) {
            RECORDS.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        //? if >=1.19.2 {
        if (event.getServer().getTickCount() % PRUNE_INTERVAL_TICKS != 0) return;
        //?} else {
        /*if (ServerLifecycleHooks.getCurrentServer().getTickCount() % PRUNE_INTERVAL_TICKS != 0) return;*/
        //?}
        if (RECORDS.isEmpty()) return;

        //? if >=1.19.2 {
        ServerLevel overworld = event.getServer().overworld();
        //?} else {
        /*ServerLevel overworld = ServerLifecycleHooks.getCurrentServer().overworld();*/
        //?}
        long gameTime = overworld.getGameTime();
        int maxTimeMinutes = computeFollowTimeMinutes(EnhancedHordesTweaksConfig.day(overworld));
        long maxTicks = (long) maxTimeMinutes * 60L * 20L;

        Iterator<Map.Entry<UUID, DeterminationRecord>> it = RECORDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DeterminationRecord> e = it.next();
            if (maxTimeMinutes > 0 && (gameTime - e.getValue().startTick) > maxTicks) {
                it.remove();
            }
        }
    }

    public static int computeFollowDistance(long day) {
        return (int) EnhancedHordesTweaksConfig.scaled(EnhancedHordesTweaksConfig.hordeDeterminationFollowDistance,
                EnhancedHordesTweaksConfig.hordeDeterminationDistanceIncreaseOverTime,
                EnhancedHordesTweaksConfig.hordeDeterminationDaysBeforeActivation,
                EnhancedHordesTweaksConfig.hordeDeterminationDistanceIncreaseIntervalDays,
                EnhancedHordesTweaksConfig.hordeDeterminationDistanceIncreaseAmount, 10000, day);
    }

    public static int computeFollowTimeMinutes(long day) {
        int base = EnhancedHordesTweaksConfig.hordeDeterminationFollowTimeMinutes;
        if (base <= 0) return base;
        return (int) EnhancedHordesTweaksConfig.scaled(base,
                EnhancedHordesTweaksConfig.hordeDeterminationTimeIncreaseOverTime,
                EnhancedHordesTweaksConfig.hordeDeterminationDaysBeforeActivation,
                EnhancedHordesTweaksConfig.hordeDeterminationTimeIncreaseIntervalDays,
                EnhancedHordesTweaksConfig.hordeDeterminationTimeIncreaseAmount, 1440, day);
    }
}
