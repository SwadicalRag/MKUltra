package com.chaosbuffalo.mkultra.core.abilities.nether_mage;

import com.chaosbuffalo.mkultra.GameConstants;
import com.chaosbuffalo.mkultra.MKUltra;
import com.chaosbuffalo.mkultra.core.*;
import com.chaosbuffalo.mkultra.core.abilities.cast_states.CastState;
import com.chaosbuffalo.mkultra.core.abilities.cast_states.SingleTargetCastState;
import com.chaosbuffalo.mkultra.fx.ParticleEffects;
import com.chaosbuffalo.mkultra.init.ModSounds;
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
public class Ember extends PlayerAbility {
    public static final Ember INSTANCE = new Ember();

    @SubscribeEvent
    public static void register(RegistryEvent.Register<PlayerAbility> event) {
        event.getRegistry().register(INSTANCE.setRegistryName(INSTANCE.getAbilityId()));
    }

    public static float BASE_DAMAGE = 6.0f;
    public static float DAMAGE_SCALE = 2.0f;
    public static int BASE_DURATION = 4;
    public static int DURATION_SCALE = 1;

    private Ember() {
        super(MKUltra.MODID, "ability.ember");
    }

    @Override
    public int getCooldown(int currentRank) {
        return 6 - 2 * currentRank;
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
        return 25.0f;
    }

    @Override
    public int getCastTime(int currentRank) {
        return GameConstants.TICKS_PER_SECOND / 2;
    }

    @Override
    public CastState createCastState(int castTime) {
        return new SingleTargetCastState(castTime);
    }

    @Nullable
    @Override
    public SoundEvent getSpellCompleteSoundEvent() {
        return ModSounds.spell_cast_7;
    }

    @Override
    public SoundEvent getCastingSoundEvent() {
        return ModSounds.casting_fire;
    }

    @Override
    public int getRequiredLevel(int currentRank) {
        return currentRank * 2;
    }

    @Override
    public void endCast(EntityPlayer entity, IPlayerData data, World theWorld, CastState state) {
        super.endCast(entity, data, theWorld, state);
        SingleTargetCastState singleTargetState = AbilityUtils.getCastStateAsType(state,
                SingleTargetCastState.class);
        if (singleTargetState == null){
            return;
        }
        if (singleTargetState.hasTarget()){
            int level = data.getAbilityRank(getAbilityId());
            EntityLivingBase targetEntity = singleTargetState.getTarget();
            targetEntity.setFire(BASE_DURATION + level * DURATION_SCALE);
            targetEntity.attackEntityFrom(MKDamageSource.causeIndirectMagicDamage(getAbilityId(), entity, entity), BASE_DAMAGE + level * DAMAGE_SCALE);
            AbilityUtils.playSoundAtServerEntity(targetEntity, ModSounds.spell_fire_6, SoundCategory.PLAYERS);
            Vec3d lookVec = entity.getLookVec();
            MKUltra.packetHandler.sendToAllAround(
                    new ParticleEffectSpawnPacket(
                            EnumParticleTypes.FLAME.getParticleID(),
                            ParticleEffects.CIRCLE_PILLAR_MOTION, 60, 10,
                            targetEntity.posX, targetEntity.posY + 1.0,
                            targetEntity.posZ, 1.0, 1.0, 1.0, 1.0,
                            lookVec),
                    entity.dimension, targetEntity.posX,
                    targetEntity.posY, targetEntity.posZ, 50.0f);
        }
    }

    @Override
    public void execute(EntityPlayer entity, IPlayerData pData, World theWorld) {
        int level = pData.getAbilityRank(getAbilityId());
        EntityLivingBase targetEntity = getSingleLivingTarget(entity, getDistance(level));
        if (targetEntity != null) {
            CastState state = pData.startAbility(this);
            SingleTargetCastState singleTargetState = AbilityUtils.getCastStateAsType(state, SingleTargetCastState.class);
            if (singleTargetState != null){
                singleTargetState.setTarget(targetEntity);
            }
        }
    }
}