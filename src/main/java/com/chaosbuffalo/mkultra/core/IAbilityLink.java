package com.chaosbuffalo.mkultra.core;

public interface IAbilityLink extends IAbilitySource {
    void setAbility(BaseAbility ability);

    default IAbilitySource link(IAbilitySource other) {
        setAbility(other.getAbility());
        return this;
    }
}
