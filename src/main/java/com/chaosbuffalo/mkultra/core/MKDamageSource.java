package com.chaosbuffalo.mkultra.core;

import com.chaosbuffalo.mkultra.MKUltra;
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

    private BaseAbility ability;

    public ResourceLocation getAbilityId() {
        if (isAbility()) {
            return ability.getAbilityId();
        }
        return ClassData.INVALID_ABILITY;
    }

    public BaseAbility getAbility() {
        return ability;
    }

    public boolean isAbility() {
        return ability != null;
    }

    public MKDamageSource(BaseAbility abilityId, String damageTypeIn,
                          Entity source, @Nullable Entity indirectEntityIn) {
        super(ABILITY_DMG_TYPE, source, indirectEntityIn);
        this.ability = abilityId;
    }

    public boolean isMagicAbility() {
        return isAbility() && isMagicDamage();
    }

    public boolean isMeleeAbility() {
        return isAbility() && !isMagicDamage();
    }

    public static DamageSource causeIndirectMagicDamage(BaseAbility ability, Entity source,
                                                        @Nullable Entity indirectEntityIn) {
        return new MKDamageSource(ability, ABILITY_DMG_TYPE, source, indirectEntityIn)
                .setDamageBypassesArmor()
                .setMagicDamage();
    }

    public static DamageSource causeIndirectMagicDamage(IAbilitySource ability, Entity source,
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
