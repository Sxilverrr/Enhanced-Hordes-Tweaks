package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID)
public class HordeSightHandler {

    private static final ResourceLocation SIGHT_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("enhanced_hordes_tweaks", "horde_sight_bonus");
    private static final int REFRESH_INTERVAL_TICKS = 200;
    private static final long MAX_SIGHT_BONUS = 256;

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        if (mob.tickCount % REFRESH_INTERVAL_TICKS != 0) return;
        if (!ConfigCache.isHordeMob(mob.getType())) return;

        AttributeInstance inst = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (inst == null) return;

        double targetBonus = computeBonus(EnhancedHordesTweaksConfig.day(level));
        AttributeModifier existing = inst.getModifier(SIGHT_MODIFIER_ID);
        double currentBonus = existing == null ? 0.0 : existing.amount();
        if (Math.abs(currentBonus - targetBonus) < 1.0e-6) return;

        if (existing != null) {
            inst.removeModifier(SIGHT_MODIFIER_ID);
        }
        if (targetBonus > 0.0) {
            inst.addTransientModifier(new AttributeModifier(
                    SIGHT_MODIFIER_ID,
                    targetBonus,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public static long computeBonus(long day) {
        if (day < EnhancedHordesTweaksConfig.hordeSightDaysBeforeActivation) return 0;
        return EnhancedHordesTweaksConfig.scaled(EnhancedHordesTweaksConfig.hordeSightRangeBonus,
                EnhancedHordesTweaksConfig.hordeSightIncreaseOverTime,
                EnhancedHordesTweaksConfig.hordeSightDaysBeforeActivation,
                EnhancedHordesTweaksConfig.hordeSightIncreaseIntervalDays,
                EnhancedHordesTweaksConfig.hordeSightIncreaseAmount, MAX_SIGHT_BONUS, day);
    }
}
