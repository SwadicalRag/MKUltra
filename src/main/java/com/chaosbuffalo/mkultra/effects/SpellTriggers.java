package com.chaosbuffalo.mkultra.effects;

import com.chaosbuffalo.mkultra.MKUltra;
import com.chaosbuffalo.mkultra.core.*;
import com.chaosbuffalo.mkultra.fx.ParticleEffects;
import com.chaosbuffalo.mkultra.log.Log;
import com.chaosbuffalo.mkultra.network.packets.server.CritMessagePacket;
import com.chaosbuffalo.mkultra.network.packets.server.ParticleEffectSpawnPacket;
import com.chaosbuffalo.mkultra.utils.ItemUtils;
import com.google.common.collect.Lists;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;

public class SpellTriggers {


    private static boolean isMKUltraAbilityDamage(DamageSource source){
        return source instanceof MKDamageSource;
    }

    private static boolean isPlayerPhysicalDamage(DamageSource source) {
        return (!source.isFireDamage() && !source.isExplosion() && !source.isMagicDamage() &&
                source.getDamageType().equals("player"));
    }

    private static float getCombinedCritChance(IPlayerData data, EntityPlayerMP player) {
        return data.getMeleeCritChance() + ItemUtils.getCritChanceForItem(player.getHeldItemMainhand());
    }

    private static List<FallTrigger> fallTriggers = Lists.newArrayList();


    @FunctionalInterface
    public interface FallTrigger {
        void apply(LivingHurtEvent event, DamageSource source, EntityLivingBase entity);
    }

    public static void registerFallHandler(FallTrigger trigger) {
        fallTriggers.add(trigger);
    }

    public static void onLivingFall(LivingHurtEvent event, DamageSource source, EntityLivingBase entity) {
        fallTriggers.forEach(f -> f.apply(event, source, entity));
    }

    @FunctionalInterface
    public interface EntityHurtPlayerTrigger {
        void apply(LivingHurtEvent event, DamageSource source, EntityPlayer livingTarget, IPlayerData targetData);
    }

    private static List<EntityHurtPlayerTrigger> entityHurtPlayerPreTriggers = Lists.newArrayList();
    private static List<EntityHurtPlayerTrigger> entityHurtPlayerPostTriggers = Lists.newArrayList();

    public static void registerEntityHurtPlayerPreScaleHandler(EntityHurtPlayerTrigger trigger) {
        entityHurtPlayerPreTriggers.add(trigger);
    }

    public static void registerEntityHurtPlayerPostScaleHandler(EntityHurtPlayerTrigger trigger) {
        entityHurtPlayerPostTriggers.add(trigger);
    }

    public static void onEntityHurtPlayer(LivingHurtEvent event, DamageSource source, EntityPlayer livingTarget, IPlayerData targetData) {
        entityHurtPlayerPreTriggers.forEach(f -> f.apply(event, source, livingTarget, targetData));

        if (source.isMagicDamage()) {
            float newDamage = PlayerFormulas.applyMagicArmor(targetData, event.getAmount());
            Log.debug("Magic armor reducing damage from %f to %f", event.getAmount(), newDamage);
            event.setAmount(newDamage);
        }

        entityHurtPlayerPostTriggers.forEach(f -> f.apply(event, source, livingTarget, targetData));
    }

    @FunctionalInterface
    public interface PlayerHurtEntityTrigger {
        void apply(LivingHurtEvent event, DamageSource source, EntityLivingBase livingTarget, EntityPlayerMP playerSource, IPlayerData sourceData);
    }

    private static List<PlayerHurtEntityTrigger> playerHurtEntityMeleeTriggers = Lists.newArrayList();
    private static List<PlayerHurtEntityTrigger> playerHurtEntityMagicTriggers = Lists.newArrayList();
    private static List<PlayerHurtEntityTrigger> playerHurtEntityPostTriggers = Lists.newArrayList();

    public static void registerPlayerHurtEntityMeleeHandler(PlayerHurtEntityTrigger trigger) {
        playerHurtEntityMeleeTriggers.add(trigger);
    }

    public static void registerPlayerHurtEntityMagicHandler(PlayerHurtEntityTrigger trigger) {
        playerHurtEntityMagicTriggers.add(trigger);
    }

    public static void registerPlayerHurtEntityPostHandler(PlayerHurtEntityTrigger trigger) {
        playerHurtEntityPostTriggers.add(trigger);
    }

    public static void onPlayerHurtEntity(LivingHurtEvent event, DamageSource source, EntityLivingBase livingTarget, EntityPlayerMP playerSource, IPlayerData sourceData) {
        if (source.isMagicDamage()) {
            float newDamage = PlayerFormulas.scaleMagicDamage(sourceData, event.getAmount());
            event.setAmount(newDamage);
        }

        if (isMKUltraAbilityDamage(source)) {
            MKDamageSource mkSource = (MKDamageSource) source;
            // Handle 'melee damage' abilities
            if (mkSource.isMeleeAbility()) {
                handleMelee(event, source, livingTarget, playerSource, sourceData, false);
            } else {
                // Handle the generic magic damage potions
                handleMagic(event, livingTarget, playerSource, sourceData, mkSource);
            }
        }

        // If this is a weapon swing
        if (isPlayerPhysicalDamage(source)) {
            handleMelee(event, source, livingTarget, playerSource, sourceData, true);
        }

        playerHurtEntityPostTriggers.forEach(f -> f.apply(event, source, livingTarget, playerSource, sourceData));
    }

    private static void handleMagic(LivingHurtEvent event, EntityLivingBase livingTarget, EntityPlayerMP playerSource,
                                    IPlayerData sourceData, MKDamageSource mkSource) {

        if (playerSource.getRNG().nextFloat() >= 1.0f - sourceData.getSpellCritChance()) {
            float newDamage = event.getAmount() * sourceData.getSpellCritDamage();
            event.setAmount(newDamage);

            CritMessagePacket packet;
            if (mkSource.isIndirectMagic()) {
                packet = new CritMessagePacket(livingTarget.getEntityId(), playerSource.getUniqueID(),
                                newDamage, CritMessagePacket.CritType.INDIRECT_MAGIC_CRIT);
            }
            else {
                BaseAbility ability = ClassData.getAbility(mkSource.getAbilityId());
                packet = new CritMessagePacket(livingTarget.getEntityId(), playerSource.getUniqueID(),
                                newDamage, ability.getAbilityId());
            }

            sendCritPacket(livingTarget, playerSource, packet);
        }

        playerHurtEntityMagicTriggers.forEach(f -> f.apply(event, mkSource, livingTarget, playerSource, sourceData));
    }

    private static void sendCritPacket(EntityLivingBase livingTarget, EntityPlayerMP playerSource,
                                       CritMessagePacket packet) {
        MKUltra.packetHandler.sendToAllAround(packet, playerSource, 50.0f);

        Vec3d lookVec = livingTarget.getLookVec();
        MKUltra.packetHandler.sendToAllAround(
                new ParticleEffectSpawnPacket(
                        EnumParticleTypes.CRIT_MAGIC.getParticleID(),
                        ParticleEffects.SPHERE_MOTION, 30, 6,
                        livingTarget.posX, livingTarget.posY + 1.0f,
                        livingTarget.posZ, .5f, .5f, .5f, 1.5,
                        lookVec),
                livingTarget, 50.0f);
    }

    private static void handleMelee(LivingHurtEvent event, DamageSource source, EntityLivingBase livingTarget,
                                    EntityPlayerMP playerSource, IPlayerData sourceData, boolean isDirect) {
        if (playerSource.getRNG().nextFloat() >= 1.0f - getCombinedCritChance(sourceData, playerSource)) {
            ItemStack mainHand = playerSource.getHeldItemMainhand();
            float newDamage = event.getAmount() * ItemUtils.getCritDamageForItem(mainHand);
            event.setAmount(newDamage);
            CritMessagePacket.CritType type = isDirect ?
                    CritMessagePacket.CritType.MELEE_CRIT :
                    CritMessagePacket.CritType.INDIRECT_CRIT;

            sendCritPacket(livingTarget, playerSource,
                    new CritMessagePacket(livingTarget.getEntityId(), playerSource.getUniqueID(), newDamage, type));
        }

        playerHurtEntityMeleeTriggers.forEach(f -> f.apply(event, source, livingTarget, playerSource, sourceData));
    }


}
