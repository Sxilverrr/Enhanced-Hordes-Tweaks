package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID)
public class PassiveFearHandler {

    private static final float AVOID_DISTANCE = 8.0f;
    private static final double WALK_SPEED = 1.0;
    private static final double SPRINT_SPEED = 1.3;

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof PathfinderMob mob)) return;

        if (!ConfigCache.isHostilityTarget(mob.getType())) return;

        mob.goalSelector.addGoal(2, new AvoidEntityGoal<>(mob, LivingEntity.class,
                AVOID_DISTANCE, WALK_SPEED, SPRINT_SPEED, candidate -> isActiveThreat(candidate, mob)));
    }

    private static boolean isActiveThreat(LivingEntity candidate, PathfinderMob mob) {
        if (!EnhancedHordesTweaksConfig.enableUniversalHostility) return false;
        boolean fearEnabled = mob instanceof Enemy ? EnhancedHordesTweaksConfig.enableHostileFear
                : mob instanceof NeutralMob ? EnhancedHordesTweaksConfig.enableNeutralFear
                : EnhancedHordesTweaksConfig.enablePassiveFear;
        if (!fearEnabled) return false;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                candidate.level(), EnhancedHordesTweaksConfig.universalHostilityDaysBeforeActivation)) return false;
        return ConfigCache.isHostileMob(candidate.getType());
    }
}
