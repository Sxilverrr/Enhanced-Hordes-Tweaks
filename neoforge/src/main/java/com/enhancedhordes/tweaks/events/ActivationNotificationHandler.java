package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID)
public class ActivationNotificationHandler {

    private static long lastDay = -1;

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        lastDay = -1;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;

        ServerLevel overworld = server.overworld();
        long day = overworld.getGameTime() / 24000L;

        if (lastDay < 0 || day <= lastDay) {
            lastDay = day;
            return;
        }

        if (!EnhancedHordesTweaksConfig.enableActivationNotifications) {
            lastDay = day;
            return;
        }
        if (server.getPlayerList().getPlayers().isEmpty()) return;

        long from = lastDay;
        lastDay = day;

        check(server, from, day, true,
                EnhancedHordesTweaksConfig.featuresDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyFeaturesMessage);
        check(server, from, day, EnhancedHordesTweaksConfig.enableHordeMentality,
                EnhancedHordesTweaksConfig.hordeMentalityDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyHordeMentalityMessage);
        check(server, from, day, EnhancedHordesTweaksConfig.enableUniversalHostility,
                EnhancedHordesTweaksConfig.universalHostilityDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyUniversalHostilityMessage);
        check(server, from, day, EnhancedHordesTweaksConfig.enableHordeDetermination,
                EnhancedHordesTweaksConfig.hordeDeterminationDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyHordeDeterminationMessage);
        check(server, from, day, EnhancedHordesTweaksConfig.enableHordeWandering,
                EnhancedHordesTweaksConfig.hordeWanderingDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyHordeWanderingMessage);
        check(server, from, day, EnhancedHordesTweaksConfig.enableCollectiveUnderstanding,
                EnhancedHordesTweaksConfig.collectiveUnderstandingDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyCollectiveUnderstandingMessage);
        check(server, from, day, EnhancedHordesTweaksConfig.enableHeightenedSense,
                EnhancedHordesTweaksConfig.heightenedSenseDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyHeightenedSenseMessage);
        check(server, from, day,
                EnhancedHordesTweaksConfig.hordeSightRangeBonus > 0
                        || EnhancedHordesTweaksConfig.hordeSightIncreaseOverTime,
                EnhancedHordesTweaksConfig.hordeSightDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyHordeSightMessage);
        check(server, from, day, EnhancedHordesTweaksConfig.enableCreeperWallExplosion,
                EnhancedHordesTweaksConfig.creeperWallExplosionDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyCreeperWallExplosionMessage);
    }

    private static void check(MinecraftServer server, long from, long to,
                              boolean enabled, int threshold, String message) {
        if (!enabled) return;
        if (message == null || message.isEmpty()) return;
        if (threshold <= 0) return;
        if (threshold <= from || threshold > to) return;
        announce(server, message);
    }

    private static void announce(MinecraftServer server, String message) {
        Component text = format(message);
        SoundEvent sound = resolveSound();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(text);
            if (sound != null) player.playNotifySound(sound, SoundSource.MASTER, 1.0f, 1.0f);
        }
    }

    private static SoundEvent resolveSound() {
        if (!EnhancedHordesTweaksConfig.notificationPlaySound) return null;
        ResourceLocation rl = ResourceLocation.tryParse(EnhancedHordesTweaksConfig.notificationSound);
        if (rl == null) return null;
        return BuiltInRegistries.SOUND_EVENT.get(rl);
    }

    private static Component format(String message) {
        return Component.literal(message.replaceAll("(?i)&([0-9a-fk-or])", "§$1"));
    }
}
