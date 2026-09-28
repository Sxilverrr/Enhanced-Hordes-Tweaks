package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import com.enhancedhordes.tweaks.util.VersionCompat;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Drowned;
//? if >=1.19.2 {
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
//?} else {
/*import net.minecraftforge.event.entity.EntityJoinWorldEvent;*/
//?}
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HordeSwimmingHandler {

    @SubscribeEvent
    //? if >=1.19.2 {
    public static void onEntityJoin(EntityJoinLevelEvent event) {
    //?} else {
    /*public static void onEntityJoin(EntityJoinWorldEvent event) {*/
    //?}
        if (!EnhancedHordesTweaksConfig.enableHordeSwimming) return;
        //? if >=1.19.2 {
        if (event.getLevel().isClientSide()) return;
        //?} else {
        /*if (event.getWorld().isClientSide()) return;*/
        //?}
        if (!(event.getEntity() instanceof PathfinderMob mob)) return;
        if (!ConfigCache.isHordeMob(mob.getType())) return;
        if (mob instanceof Drowned) return;

        for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof FloatGoal) return;
        }
        mob.goalSelector.addGoal(0, new FloatGoal(mob) {
            @Override
            public boolean canUse() {
                boolean active = EnhancedHordesTweaksConfig.daysElapsedReached(VersionCompat.level(mob), EnhancedHordesTweaksConfig.featuresDaysBeforeActivation);
                mob.getNavigation().setCanFloat(active);
                return active && super.canUse();
            }
        });
    }
}
