package com.chaosbuffalo.mkultra.api;

import net.minecraft.item.ItemArmor;

public interface IArmorClass {
    boolean canWear(ItemArmor.ArmorMaterial material);

    IArmorClass inherit(IArmorClass armorClass);

    IArmorClass register(ItemArmor.ArmorMaterial material);
}
