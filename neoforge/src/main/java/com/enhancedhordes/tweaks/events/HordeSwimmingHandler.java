package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Drowned;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID)
public class HordeSwimmingHandler {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!EnhancedHordesTweaksConfig.enableHordeSwimming) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof PathfinderMob mob)) return;
        if (!ConfigCache.isHordeMob(mob.getType())) return;
        if (mob instanceof Drowned) return;

        for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof FloatGoal) return;
        }
        mob.goalSelector.addGoal(0, new FloatGoal(mob) {
            @Override
            public boolean canUse() {
                boolean active = EnhancedHordesTweaksConfig.daysElapsedReached(mob.level(), EnhancedHordesTweaksConfig.featuresDaysBeforeActivation);
                mob.getNavigation().setCanFloat(active);
                return active && super.canUse();
            }
        });
    }
}
