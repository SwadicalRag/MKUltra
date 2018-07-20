package com.chaosbuffalo.mkultra.core;

public interface IAbilitySource {
    BaseAbility getAbility();
    void setAbility(BaseAbility ability);

    default IAbilitySource link(IAbilitySource other) {
        setAbility(other.getAbility());
        return this;
    }
}
