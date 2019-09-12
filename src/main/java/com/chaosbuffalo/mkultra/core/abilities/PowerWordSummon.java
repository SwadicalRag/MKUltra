package com.chaosbuffalo.mkultra.core.abilities;

import com.chaosbuffalo.mkultra.GameConstants;
import com.chaosbuffalo.mkultra.MKUltra;
import com.chaosbuffalo.mkultra.core.*;
import com.chaosbuffalo.mkultra.effects.spells.WarpTargetPotion;
import com.chaosbuffalo.mkultra.fx.ParticleEffects;
import com.chaosbuffalo.mkultra.network.packets.ParticleEffectSpawnPacket;
import com.chaosbuffalo.mkultra.utils.AbilityUtils;
import com.chaosbuffalo.targeting_api.Targeting;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PowerWordSummon extends PlayerAbility {

    public PowerWordSummon() {
        super(MKUltra.MODID, "ability.power_word_summon");
    }

    @Override
    public int getCooldown(int currentRank) {
        return 16 - 4 * currentRank;
    }

    @Override
    public Targeting.TargetType getTargetType() {
        return Targeting.TargetType.ENEMY;
    }

    @Override
    public float getManaCost(int currentRank) {
        return 4 + currentRank * 2;
    }

    @Override
    public float getDistance(int currentRank) {
        return 45.0f;
    }

    @Override
    public int getRequiredLevel(int currentRank) {
        return 4 + currentRank * 2;
    }

    @Override
    public int getCastTime(int currentRank) {
        return GameConstants.TICKS_PER_SECOND * (4 - currentRank);
    }

    @Override
    public CastState createCastState(int castTime) {
        return new SingleTargetCastState(castTime);
    }

    @Override
    public void endCast(EntityPlayer entity, IPlayerData data, World theWorld, CastState state) {
        super.endCast(entity, data, theWorld, state);
        SingleTargetCastState singleTargetState = AbilityUtils.getCastStateAsType(state,
                SingleTargetCastState.class);
        if (singleTargetState == null || !singleTargetState.hasTarget()){
            return;
        }
        int level = data.getAbilityRank(getAbilityId());
        EntityLivingBase targetEntity = singleTargetState.getTarget();
        targetEntity.addPotionEffect(WarpTargetPotion.Create(entity).setTarget(targetEntity).toPotionEffect(level));
        targetEntity.addPotionEffect(
                new PotionEffect(MobEffects.SLOWNESS,
                        (4 + level) * GameConstants.TICKS_PER_SECOND, 100, false, true));

        Vec3d lookVec = entity.getLookVec();
        MKUltra.packetHandler.sendToAllAround(
                new ParticleEffectSpawnPacket(
                        EnumParticleTypes.SPELL_MOB.getParticleID(),
                        ParticleEffects.CIRCLE_PILLAR_MOTION, 60, 10,
                        targetEntity.posX, targetEntity.posY + 0.5,
                        targetEntity.posZ, 1.0, 1.0, 1.0, 1.0,
                        lookVec),
                entity.dimension, targetEntity.posX,
                targetEntity.posY, targetEntity.posZ, 50.0f);
    }

    @Override
    public void execute(EntityPlayer entity, IPlayerData pData, World theWorld) {
        int level = pData.getAbilityRank(getAbilityId());

        EntityLivingBase targetEntity = getSingleLivingTarget(entity, getDistance(level));

        if (targetEntity != null) {
            CastState state = pData.startAbility(this);
            SingleTargetCastState singleTargetState = AbilityUtils.getCastStateAsType(state,
                    SingleTargetCastState.class);
            if (singleTargetState != null){
                singleTargetState.setTarget(targetEntity);
            }
        }
    }
}

