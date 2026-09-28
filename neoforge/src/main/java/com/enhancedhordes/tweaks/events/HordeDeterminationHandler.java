package com.enhancedhordes.tweaks.events;

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
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID)
public class HordeDeterminationHandler {

    private static final Map<UUID, DeterminationRecord> RECORDS = new ConcurrentHashMap<>();
    private static final Set<UUID> FORCED_PERSISTENCE = ConcurrentHashMap.newKeySet();
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
    public static void onLivingTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!EnhancedHordesTweaksConfig.enableHordeDetermination) {
            if (!FORCED_PERSISTENCE.isEmpty() && FORCED_PERSISTENCE.contains(mob.getUUID())) {
                clearForcedPersistence(mob);
            }
            return;
        }
        if (!(mob.level() instanceof ServerLevel level)) return;
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
                clearForcedPersistence(mob);
                return;
            }
            if (!GameStagesCompat.allows(player, EnhancedHordesTweaksConfig.hordeDeterminationStage)) {
                RECORDS.remove(mob.getUUID());
                clearForcedPersistence(mob);
                return;
            }
            RECORDS.compute(mob.getUUID(), (k, existing) -> {
                if (existing == null || !existing.playerUuid.equals(player.getUUID())) {
                    return new DeterminationRecord(player.getUUID(), gameTime);
                }
                return existing;
            });
            forcePersistence(mob);
            return;
        }

        DeterminationRecord record = RECORDS.get(mob.getUUID());
        if (record == null) {
            clearForcedPersistence(mob);
            return;
        }

        if (maxTimeMinutes > 0 && (gameTime - record.startTick) > maxTicks) {
            RECORDS.remove(mob.getUUID());
            clearForcedPersistence(mob);
            return;
        }

        Player player = level.getPlayerByUUID(record.playerUuid);
        if (player == null || player.isRemoved() || player.isSpectator()) {
            return;
        }
        if (player.isCreative()
                || !GameStagesCompat.allows(player, EnhancedHordesTweaksConfig.hordeDeterminationStage)) {
            RECORDS.remove(mob.getUUID());
            clearForcedPersistence(mob);
            return;
        }

        double distSqr = mob.distanceToSqr(player);
        if (distSqr > (double) maxDistance * maxDistance) {
            RECORDS.remove(mob.getUUID());
            clearForcedPersistence(mob);
            return;
        }

        mob.setTarget(player);
        forcePersistence(mob);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!mob.isPersistenceRequired()) return;
        if (!ConfigCache.isHordeMob(mob.getType())) return;
        if (mob.getPersistentData().getBoolean(FORCED_PERSISTENCE_TAG)) {
            FORCED_PERSISTENCE.add(mob.getUUID());
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity().isRemoved()) {
            RECORDS.remove(event.getEntity().getUUID());
            FORCED_PERSISTENCE.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % PRUNE_INTERVAL_TICKS != 0) return;
        if (RECORDS.isEmpty()) return;

        ServerLevel overworld = event.getServer().overworld();
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

    private static void forcePersistence(Mob mob) {
        if (!mob.isPersistenceRequired()) {
            mob.setPersistenceRequired();
            FORCED_PERSISTENCE.add(mob.getUUID());
            mob.getPersistentData().putBoolean(FORCED_PERSISTENCE_TAG, true);
        }
    }

    private static void clearForcedPersistence(Mob mob) {
        boolean tracked = FORCED_PERSISTENCE.remove(mob.getUUID());
        if (!tracked && !mob.isPersistenceRequired()) return;
        CompoundTag data = mob.getPersistentData();
        if (!tracked && !data.getBoolean(FORCED_PERSISTENCE_TAG)) return;
        data.remove(FORCED_PERSISTENCE_TAG);
        mob.persistenceRequired = false;
    }
}
