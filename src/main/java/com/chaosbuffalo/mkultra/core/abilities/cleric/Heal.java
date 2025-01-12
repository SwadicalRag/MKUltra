package com.chaosbuffalo.mkultra.core.abilities.cleric;

import com.chaosbuffalo.mkultra.GameConstants;
import com.chaosbuffalo.mkultra.MKUltra;
import com.chaosbuffalo.mkultra.core.*;
import com.chaosbuffalo.mkultra.core.abilities.cast_states.CastState;
import com.chaosbuffalo.mkultra.core.abilities.cast_states.SingleTargetCastState;
import com.chaosbuffalo.mkultra.effects.SpellCast;
import com.chaosbuffalo.mkultra.effects.spells.ClericHealPotion;
import com.chaosbuffalo.mkultra.fx.ParticleEffects;
import com.chaosbuffalo.mkultra.init.ModSounds;
import com.chaosbuffalo.mkultra.log.Log;
import com.chaosbuffalo.mkultra.network.packets.ParticleEffectSpawnPacket;
import com.chaosbuffalo.mkultra.utils.AbilityUtils;
import com.chaosbuffalo.targeting_api.Targeting;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = MKUltra.MODID)
public class Heal extends PlayerAbility {
    public static final Heal INSTANCE = new Heal();

    @SubscribeEvent
    public static void register(RegistryEvent.Register<PlayerAbility> event) {
        event.getRegistry().register(INSTANCE.setRegistryName(INSTANCE.getAbilityId()));
    }

    public static float BASE_VALUE = 5.0f;
    public static float VALUE_SCALE = 5.0f;

    private Heal() {
        super(MKUltra.MODID, "ability.heal");
    }

    @Override
    public int getCooldown(int currentRank) {
        return 6 - currentRank;
    }

    @Override
    public CastState createCastState(int castTime) {
        return new SingleTargetCastState(castTime);
    }

    @Override
    public Targeting.TargetType getTargetType() {
        return Targeting.TargetType.FRIENDLY;
    }

    @Override
    public SoundEvent getCastingSoundEvent() {
        return ModSounds.casting_holy;
    }

    @Nullable
    @Override
    public SoundEvent getSpellCompleteSoundEvent() {
        return ModSounds.spell_holy_5;
    }

    @Override
    public void endCast(EntityPlayer entity, IPlayerData data, World theWorld, CastState castState) {
        super.endCast(entity, data, theWorld, castState);

        SingleTargetCastState singleTargetState = AbilityUtils.getCastStateAsType(castState, SingleTargetCastState.class);
        if (singleTargetState == null){
            return;
        }
        if (singleTargetState.hasTarget()){
            int level = data.getAbilityRank(getAbilityId());
            EntityLivingBase target = singleTargetState.getTarget();
            SpellCast heal = ClericHealPotion.Create(entity, BASE_VALUE, VALUE_SCALE).setTarget(target);
            target.addPotionEffect(heal.toPotionEffect(level));
            Vec3d lookVec = entity.getLookVec();
            AbilityUtils.playSoundAtServerEntity(target, ModSounds.spell_heal_3, SoundCategory.PLAYERS);
            MKUltra.packetHandler.sendToAllAround(
                    new ParticleEffectSpawnPacket(
                            EnumParticleTypes.VILLAGER_HAPPY.getParticleID(),
                            ParticleEffects.SPHERE_MOTION, 50, 10,
                            target.posX, target.posY + 1.0f,
                            target.posZ, 1.0, 1.0, 1.0, 1.5,
                            lookVec),
                    entity.dimension, target.posX,
                    target.posY, target.posZ, 50.0f);
        }

    }

    @Override
    public boolean canSelfCast() {
        return true;
    }

    @Override
    public float getManaCost(int currentRank) {
        return 2 + currentRank * 2;
    }

    @Override
    public float getDistance(int currentRank) {
        return 10.0f + 5.0f * currentRank;
    }

    @Override
    public int getRequiredLevel(int currentRank) {
        return currentRank * 2;
    }

    @Override
    protected boolean isValidTarget(EntityLivingBase caster, EntityLivingBase target) {
        return ClericHealPotion.INSTANCE.isValidTarget(getTargetType(), caster, target, !canSelfCast());
    }

    @Override
    public int getCastTime(int currentRank) {
        return GameConstants.TICKS_PER_SECOND / 4;
    }

    @Override
    public void execute(EntityPlayer entity, IPlayerData pData, World theWorld) {
        int level = pData.getAbilityRank(getAbilityId());
        EntityLivingBase targetEntity = getSingleLivingTargetOrSelf(entity, getDistance(level), true);
        CastState state = pData.startAbility(this);
        SingleTargetCastState singleTargetState = AbilityUtils.getCastStateAsType(state, SingleTargetCastState.class);
        if (singleTargetState != null){
            singleTargetState.setTarget(targetEntity);
        }
    }
}

