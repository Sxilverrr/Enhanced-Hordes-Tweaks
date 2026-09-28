package com.enhancedhordes.tweaks.events;
import com.enhancedhordes.tweaks.util.VersionCompat;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HordeSightHandler {

    private static final UUID SIGHT_MODIFIER_ID = UUID.fromString("a7d3c1f4-9b2e-4d56-8f10-2c4e9a6b3d71");
    private static final String SIGHT_MODIFIER_NAME = "EnhancedHordes Horde Sight Bonus";
    private static final int REFRESH_INTERVAL_TICKS = 200;
    private static final long MAX_SIGHT_BONUS = 256;

    @SubscribeEvent
    //? if >=1.19.2 {
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
    //?} else {
    /*public static void onLivingTick(LivingEvent.LivingUpdateEvent event) {*/
    //?}
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(VersionCompat.level(mob) instanceof ServerLevel level)) return;
        if (mob.tickCount % REFRESH_INTERVAL_TICKS != 0) return;
        if (!ConfigCache.isHordeMob(mob.getType())) return;

        AttributeInstance inst = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (inst == null) return;

        double targetBonus = computeBonus(EnhancedHordesTweaksConfig.day(level));
        AttributeModifier existing = inst.getModifier(SIGHT_MODIFIER_ID);
        double currentBonus = existing == null ? 0.0 : existing.getAmount();
        if (Math.abs(currentBonus - targetBonus) < 1.0e-6) return;

        if (existing != null) {
            inst.removeModifier(SIGHT_MODIFIER_ID);
        }
        if (targetBonus > 0.0) {
            inst.addTransientModifier(new AttributeModifier(
                    SIGHT_MODIFIER_ID,
                    SIGHT_MODIFIER_NAME,
                    targetBonus,
                    AttributeModifier.Operation.ADDITION));
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
