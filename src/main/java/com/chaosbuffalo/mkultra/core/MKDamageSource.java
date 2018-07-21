package com.chaosbuffalo.mkultra.core;

import com.chaosbuffalo.mkultra.effects.spells.MeleeDamagePotion;
import com.chaosbuffalo.mkultra.effects.spells.InstantIndirectMagicDamagePotion;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Created by Jacob on 7/14/2018.
 */
public class MKDamageSource extends EntityDamageSourceIndirect {

    private static String ABILITY_DMG_TYPE = "mkUltraAbility";

    private ResourceLocation abilityId;
    private BaseAbility ability;

    public ResourceLocation getAbilityId() {
        if (isAbility()) {
            return ability.getAbilityId();
        }
        return abilityId;
    }

    public boolean isAbility() {
        return ability != null;
    }

    public MKDamageSource(ResourceLocation abilityId, String damageTypeIn,
                          Entity source, @Nullable Entity indirectEntityIn) {
        super(ABILITY_DMG_TYPE, source, indirectEntityIn);
        this.abilityId = abilityId;
    }

    public MKDamageSource(BaseAbility abilityId, String damageTypeIn,
                          Entity source, @Nullable Entity indirectEntityIn) {
        super(ABILITY_DMG_TYPE, source, indirectEntityIn);
        this.ability = abilityId;
    }

    public boolean isMagicAbility() {
        return isAbility() && ability.getAbilityId().equals(InstantIndirectMagicDamagePotion.INDIRECT_MAGIC_DMG_ABILITY_ID);
    }

    public boolean isMeleeAbility() {
        return isAbility() && ability.getAbilityId().equals(MeleeDamagePotion.INDIRECT_DMG_ABILITY_ID);
    }

    public static DamageSource causeIndirectMagicDamage(ResourceLocation abilityId, Entity source,
                                                        @Nullable Entity indirectEntityIn) {
        return new MKDamageSource(abilityId, ABILITY_DMG_TYPE, source, indirectEntityIn)
                .setDamageBypassesArmor()
                .setMagicDamage();
    }

    public static DamageSource fromMagicAbility(IAbilitySource ability, Entity source,
                                                @Nullable Entity indirectEntityIn) {
        return new MKDamageSource(ability.getAbility(), ABILITY_DMG_TYPE, source, indirectEntityIn)
                .setDamageBypassesArmor()
                .setMagicDamage();
    }

    public static DamageSource fromMeleeSkill(IAbilitySource ability, Entity source,
                                              @Nullable Entity indirectEntityIn) {
        return new MKDamageSource(ability.getAbility(), ABILITY_DMG_TYPE, source, indirectEntityIn);
    }
}
