package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID)
public class HordeFallDamageHandler {

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!EnhancedHordesTweaksConfig.hordeFallDamageImmunity) return;
        if (!ConfigCache.isHordeMob(event.getEntity().getType())) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(event.getEntity().level(), EnhancedHordesTweaksConfig.featuresDaysBeforeActivation)) return;
        event.setCanceled(true);
    }
}
