package com.enhancedhordes.tweaks.mixin;

import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import com.enhancedhordes.tweaks.util.BlockSupportUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.mcreator.horde_hoard.procedures.HordeTickProcedure", remap = false)
public class HordeTickMixin {

    private static final String EXECUTE =
            "execute(Lnet/neoforged/bus/api/Event;" +
            "Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V";

    private static final ThreadLocal<java.lang.ref.WeakReference<Entity>> CURRENT_ENTITY = new ThreadLocal<>();

    @Inject(method = EXECUTE, at = @At("HEAD"), remap = false)
    private static void captureCurrentEntity(Event ev, LevelAccessor level, double x, double y, double z,
                                             Entity entity, CallbackInfo ci) {
        CURRENT_ENTITY.set(new java.lang.ref.WeakReference<>(entity));
    }

    @Inject(method = EXECUTE, at = @At("RETURN"), remap = false)
    private static void clearCurrentEntity(Event ev, LevelAccessor level, double x, double y, double z,
                                           Entity entity, CallbackInfo ci) {
        CURRENT_ENTITY.remove();
    }

    @Redirect(method = EXECUTE,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V",
            remap = false),
        require = 1)
    private static void redirectFireSpread(Entity nearbyEntity, float seconds) {
        if (!EnhancedHordesTweaksConfig.hordeFireSpread) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                nearbyEntity.level(), EnhancedHordesTweaksConfig.featuresDaysBeforeActivation)) return;
        nearbyEntity.igniteForSeconds(seconds);
    }

    @Redirect(method = EXECUTE,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
            remap = false,
            ordinal = 0),
        require = 1)
    private static void redirectBabyThrow(Entity entity, Vec3 velocity) {
        if (!EnhancedHordesTweaksConfig.hordeBabyThrow) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                entity.level(), EnhancedHordesTweaksConfig.featuresDaysBeforeActivation)) return;
        entity.setDeltaMovement(velocity);
    }

    @Redirect(method = EXECUTE,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            remap = false),
        require = 1)
    private static boolean redirectAddEffect(LivingEntity entity, MobEffectInstance effectInstance) {
        if (effectInstance.getEffect().value() != MobEffects.MOVEMENT_SPEED.value()) {
            return entity.addEffect(effectInstance);
        }
        if (!EnhancedHordesTweaksConfig.hordeFireSpeedBoost) {
            return false;
        }
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                entity.level(), EnhancedHordesTweaksConfig.featuresDaysBeforeActivation)) {
            return false;
        }
        if (!EnhancedHordesTweaksConfig.hordeBabyFireSpeedBoost
                && entity instanceof Mob mob && mob.isBaby()) {
            return false;
        }
        int amp = EnhancedHordesTweaksConfig.hordeFireSpeedAmplifier;
        if (amp != effectInstance.getAmplifier()) {
            return entity.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED, effectInstance.getDuration(), amp));
        }
        return entity.addEffect(effectInstance);
    }

    @Redirect(method = EXECUTE,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            remap = false),
        require = 1)
    private static void redirectBlockDropResources(BlockState state, LevelAccessor level, BlockPos pos, BlockEntity blockEntity) {
        if (shouldAllowEHBreak(level, pos, state)) {
            Block.dropResources(state, level, pos, blockEntity);
        }
    }

    @Redirect(method = EXECUTE,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z",
            remap = false),
        require = 1)
    private static boolean redirectLevelDestroyBlock(LevelAccessor level, BlockPos pos, boolean drop) {
        BlockState state = level.getBlockState(pos);
        if (!shouldAllowEHBreak(level, pos, state)) return false;
        return level.destroyBlock(pos, drop);
    }

    @Redirect(method = EXECUTE,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z",
            remap = false),
        require = 1)
    private static boolean redirectGameRule(GameRules rules, GameRules.Key<GameRules.BooleanValue> key) {
        if (!rules.getBoolean(key)) return false;
        if (!(currentEntity() instanceof Mob mob)) return true;
        LivingEntity target = mob.getTarget();
        return switch (key.getId()) {
            case "hordeStacking" -> target != null || !EnhancedHordesTweaksConfig.hordeStackingRequiresTarget;
            case "hordeMultiplying" -> target != null && canMultiply(mob, target);
            default -> true;
        };
    }

    private static boolean canMultiply(Mob mob, LivingEntity target) {
        Level level = mob.level();
        int maxDistance = EnhancedHordesTweaksConfig.hordeMultiplyingMaxTargetDistance;
        int maxNearby = EnhancedHordesTweaksConfig.hordeMultiplyingMaxNearbyMobs;
        return (target instanceof Player || !EnhancedHordesTweaksConfig.hordeMultiplyingRequiresPlayerTarget)
                && (maxDistance == 0 || mob.distanceToSqr(target) <= (double) maxDistance * maxDistance)
                && (!EnhancedHordesTweaksConfig.hordeMultiplyingNightOnly || !level.isDay())
                && (!EnhancedHordesTweaksConfig.hordeMultiplyingRequiresLineOfSight || mob.hasLineOfSight(target))
                && (maxNearby == 0 || level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(16),
                        m -> ConfigCache.isHordeMob(m.getType())).size() <= maxNearby);
    }

    @ModifyConstant(method = EXECUTE, constant = @Constant(doubleValue = 8.0E-4), remap = false, require = 1)
    private static double modifyMultiplyChance(double chance) {
        return chance * EnhancedHordesTweaksConfig.hordeMultiplyingChance / 100.0;
    }

    private static Entity currentEntity() {
        java.lang.ref.WeakReference<Entity> ref = CURRENT_ENTITY.get();
        return ref == null ? null : ref.get();
    }

    private static boolean shouldAllowEHBreak(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!EnhancedHordesTweaksConfig.enableHordeBlockBreaking) return false;
        if (level instanceof net.minecraft.world.level.Level concrete
                && !EnhancedHordesTweaksConfig.daysElapsedReached(
                        concrete, EnhancedHordesTweaksConfig.featuresDaysBeforeActivation)) {
            return false;
        }
        if (!EnhancedHordesTweaksConfig.hordeBabyBlockBreaking
                && currentEntity() instanceof Mob mob && mob.isBaby()) {
            return false;
        }
        if (EnhancedHordesTweaksConfig.hordeMentalityProtectSupportingBlocks
                && level instanceof ServerLevel serverLevel
                && BlockSupportUtil.wouldOrphanNeighbor(serverLevel, pos, state)) {
            return false;
        }
        return true;
    }
}
