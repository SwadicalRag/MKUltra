package com.chaosbuffalo.mkultra.core;

import com.chaosbuffalo.mkultra.GameConstants;
import com.chaosbuffalo.mkultra.MKUltra;
import com.chaosbuffalo.mkultra.client.audio.MovingSoundCasting;
import com.chaosbuffalo.mkultra.core.abilities.cast_states.CastState;
import com.chaosbuffalo.mkultra.core.events.PlayerAbilityCastEvent;
import com.chaosbuffalo.mkultra.core.events.PlayerClassEvent;
import com.chaosbuffalo.mkultra.core.talents.PassiveAbilityTalent;
import com.chaosbuffalo.mkultra.core.talents.RangedAttributeTalent;
import com.chaosbuffalo.mkultra.core.talents.TalentTreeRecord;
import com.chaosbuffalo.mkultra.effects.SpellCast;
import com.chaosbuffalo.mkultra.effects.SpellPotionBase;
import com.chaosbuffalo.mkultra.effects.passives.PassiveAbilityPotionBase;
import com.chaosbuffalo.mkultra.effects.spells.ArmorTrainingPotion;
import com.chaosbuffalo.mkultra.event.ItemEventHandler;
import com.chaosbuffalo.mkultra.log.Log;
import com.chaosbuffalo.mkultra.network.packets.AbilityUpdatePacket;
import com.chaosbuffalo.mkultra.network.packets.ClassUpdatePacket;
import com.chaosbuffalo.mkultra.network.packets.PlayerSyncRequestPacket;
import com.chaosbuffalo.mkultra.utils.AbilityUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;

public class PlayerData implements IPlayerData {

    private final static DataParameter<Float> MANA = EntityDataManager.createKey(
            EntityPlayer.class, DataSerializers.FLOAT);
    private final static DataParameter<Integer> LEVEL = EntityDataManager.createKey(
            EntityPlayer.class, DataSerializers.VARINT);
    private final static DataParameter<Integer> UNSPENT_POINTS = EntityDataManager.createKey(
            EntityPlayer.class, DataSerializers.VARINT);
    private final static DataParameter<String> CLASS_ID = EntityDataManager.createKey(
            EntityPlayer.class, DataSerializers.STRING);
    private final static DataParameter<String>[] ACTION_BAR_ABILITY_ID;
    private final static DataParameter<Integer>[] ACTION_BAR_ABILITY_RANK;
    private final static DataParameter<Integer> CAST_TICKS = EntityDataManager.createKey(
            EntityPlayer.class, DataSerializers.VARINT);
    private final static DataParameter<String> CURRENT_CAST = EntityDataManager.createKey(
            EntityPlayer.class, DataSerializers.STRING);

    private final static String INVALID_ABILITY_STRING = MKURegistry.INVALID_ABILITY.toString();

    static {
        ACTION_BAR_ABILITY_ID = new DataParameter[GameConstants.ACTION_BAR_SIZE];
        for (int i = 0; i < GameConstants.ACTION_BAR_SIZE; i++) {
            ACTION_BAR_ABILITY_ID[i] = EntityDataManager.createKey(EntityPlayer.class, DataSerializers.STRING);
        }
        ACTION_BAR_ABILITY_RANK = new DataParameter[GameConstants.ACTION_BAR_SIZE];
        for (int i = 0; i < GameConstants.ACTION_BAR_SIZE; i++) {
            ACTION_BAR_ABILITY_RANK[i] = EntityDataManager.createKey(EntityPlayer.class, DataSerializers.VARINT);
        }
    }


    private final EntityPlayer player;
    private final EntityDataManager privateData;
    private float regenTime;
    private float healthRegenTime;
    private AbilityTracker abilityTracker;
    private Map<ResourceLocation, PlayerClassInfo> knownClasses = new HashMap<>();
    private Map<ResourceLocation, PlayerToggleAbility> activeToggleMap = new HashMap<>();
    private boolean needPassiveTalentRefresh;
    private boolean talentPassivesUnlocked;
    private EnumHandSide originalMainHand;
    private boolean isDualWielding;
    private int ticksSinceLastSwing;
    private final static int DUAL_WIELD_TIMEOUT = 25;
    private CastState currentCastState;
    private Set<String> activeSpellTriggers;
    private boolean lastUpdateIsCasting;
    private MovingSoundCasting castingSound;

    public PlayerData(EntityPlayer player) {
        this.player = player;
        regenTime = 0;
        healthRegenTime = 0;
        ticksSinceLastSwing = 0;
        isDualWielding = false;
        originalMainHand = player.getPrimaryHand();
        abilityTracker = AbilityTracker.getTracker(player);
        privateData = player.getDataManager();
        currentCastState = null;
        lastUpdateIsCasting = false;
        activeSpellTriggers = new HashSet<>();
        setupWatcher();
        player.getAttributeMap().registerAttribute(PlayerAttributes.MAX_MANA);
        player.getAttributeMap().registerAttribute(PlayerAttributes.MANA_REGEN);
        player.getAttributeMap().registerAttribute(PlayerAttributes.MAGIC_ATTACK_DAMAGE);
        player.getAttributeMap().registerAttribute(PlayerAttributes.MAGIC_ARMOR);
        player.getAttributeMap().registerAttribute(PlayerAttributes.COOLDOWN);
        player.getAttributeMap().registerAttribute(PlayerAttributes.MELEE_CRIT);
        player.getAttributeMap().registerAttribute(PlayerAttributes.SPELL_CRIT);
        player.getAttributeMap().registerAttribute(PlayerAttributes.SPELL_CRITICAL_DAMAGE);
        player.getAttributeMap().registerAttribute(PlayerAttributes.HEALTH_REGEN);
        player.getAttributeMap().registerAttribute(PlayerAttributes.HEAL_BONUS);
        player.getAttributeMap().registerAttribute(PlayerAttributes.MELEE_CRITICAL_DAMAGE);
        player.getAttributeMap().registerAttribute(PlayerAttributes.BUFF_DURATION);
    }

    private void setupWatcher() {

        privateData.register(MANA, 0f);
        privateData.register(UNSPENT_POINTS, 0);
        privateData.register(CLASS_ID, MKURegistry.INVALID_CLASS.toString());
        privateData.register(LEVEL, 0);
        privateData.register(CAST_TICKS, 0);
        privateData.register(CURRENT_CAST, INVALID_ABILITY_STRING);
        for (int i = 0; i < GameConstants.ACTION_BAR_SIZE; i++) {
            privateData.register(ACTION_BAR_ABILITY_ID[i], INVALID_ABILITY_STRING);
            privateData.register(ACTION_BAR_ABILITY_RANK[i], GameConstants.ABILITY_INVALID_RANK);
        }
    }

    private void markEntityDataDirty() {
        privateData.setDirty(MANA);
        privateData.setDirty(UNSPENT_POINTS);
        privateData.setDirty(CLASS_ID);
        privateData.setDirty(LEVEL);
        privateData.setDirty(CAST_TICKS);
        privateData.setDirty(CURRENT_CAST);
        for (int i = 0; i < GameConstants.ACTION_BAR_SIZE; i++) {
            privateData.setDirty(ACTION_BAR_ABILITY_ID[i]);
            privateData.setDirty(ACTION_BAR_ABILITY_RANK[i]);
        }
    }

    @Nullable
    private PlayerClassInfo getActiveClass() {
        return knownClasses.get(getClassId());
    }

    private ResourceLocation getLastUpgradedAbility() {
        PlayerClassInfo cinfo = getActiveClass();
        if (cinfo != null) {
            return cinfo.getAbilitySpendOrder(getAbilityLearnIndex());
        }
        return MKURegistry.INVALID_ABILITY;
    }

    @Override
    public boolean spendTalentPoint(ResourceLocation talentTree, String line, int index) {
        PlayerClassInfo classInfo = getActiveClass();
        if (classInfo == null) {
            return false;
        }
        boolean didSpend = false;
//        Log.info("In spend talent point %d", classInfo.unspentTalentPoints);
        if (classInfo.getUnspentTalentPoints() > 0) {
            didSpend = classInfo.spendTalentPoint(player, talentTree, line, index);
            if (didSpend) {
                updateTalents();
                sendCurrentClassUpdate();
            }
//            Log.info("Did spend talent %b", didSpend);
        }

        return didSpend;
    }

    @Override
    public boolean refundTalentPoint(ResourceLocation talentTree, String line, int index) {
        PlayerClassInfo classInfo = getActiveClass();
        if (classInfo == null) {
            return false;
        }
        boolean didSpend = classInfo.refundTalentPoint(player, talentTree, line, index);
        if (didSpend) {
            updateTalents();
            sendCurrentClassUpdate();
        }
        return didSpend;
    }

    public boolean canSpendTalentPoint(ResourceLocation talentTree, String line, int index) {
        PlayerClassInfo classInfo = getActiveClass();
        if (classInfo == null) {
            return false;
        }
        return classInfo.canIncrementPointInTree(talentTree, line, index);
    }

    public boolean canRefundTalentPoint(ResourceLocation talentTree, String line, int index) {
        PlayerClassInfo classInfo = getActiveClass();
        if (classInfo == null) {
            return false;
        }
        return classInfo.canDecrementPointInTree(talentTree, line, index);
    }

    @Override
    public void gainTalentPoint() {
        if (!hasChosenClass()) {
            return;
        }
        PlayerClassInfo classInfo = getActiveClass();
        if (classInfo == null) {
            return;
        }
        if (player.experienceLevel >= classInfo.getTotalTalentPoints()) {
            player.addExperienceLevel(-classInfo.getTotalTalentPoints());
            classInfo.addTalentPoints(1);
            sendCurrentClassUpdate();
        }
    }

    @Override
    public int getTotalTalentPoints() {
        if (!hasChosenClass()) {
            return 0;
        }
        PlayerClassInfo classInfo = getActiveClass();
        if (classInfo == null) {
            return 0;
        }
        return classInfo.getTotalTalentPoints();
    }

    private boolean checkTalentTotals() {
        if (!hasChosenClass()) {
            return false;
        }
        PlayerClassInfo classInfo = getActiveClass();
        if (classInfo == null) {
            return false;
        }
        return classInfo.checkTalentTotals();
    }

    @Override
    public int getUnspentTalentPoints() {
        if (!hasChosenClass()) {
            return 0;
        }
        PlayerClassInfo classInfo = getActiveClass();
        if (classInfo == null) {
            return 0;
        }
        return classInfo.getUnspentTalentPoints();
    }

    @Override
    public TalentTreeRecord getTalentTree(ResourceLocation loc) {
        if (!hasChosenClass()) {
            return null;
        }
        PlayerClassInfo classInfo = getActiveClass();
        if (classInfo == null) {
            return null;
        }
        return classInfo.getTalentTree(loc);
    }

    @Override
    @Nullable
    public List<ResourceLocation> getActivePassives() {
        PlayerClassInfo activeClass = getActiveClass();
        if (activeClass != null) {
            return Arrays.asList(activeClass.getActivePassives());
        }
        return null;
    }

    @Nullable
    @Override
    public List<ResourceLocation> getActiveUltimates() {
        PlayerClassInfo activeClass = getActiveClass();
        if (activeClass != null) {
            return Arrays.asList(activeClass.getActiveUltimates());
        }
        return null;
    }

    @Override
    @Nullable
    public Set<PlayerPassiveAbility> getLearnedPassives() {
        PlayerClassInfo activeClass = getActiveClass();
        if (activeClass != null) {
            return activeClass.getPassiveAbilitiesFromTalents();
        }
        return null;
    }

    @Nullable
    @Override
    public Set<PlayerAbility> getLearnedUltimates() {
        PlayerClassInfo activeClass = getActiveClass();
        if (activeClass != null) {
            return activeClass.getUltimateAbilitiesFromTalents();
        }
        return null;
    }

    public boolean canActivatePassiveForSlot(ResourceLocation loc, int slotIndex) {
        PlayerClassInfo activeClass = getActiveClass();
        if (activeClass != null) {
            return activeClass.canAddPassiveToSlot(loc, slotIndex);
        }
        return false;
    }

    @Override
    public boolean activatePassiveForSlot(ResourceLocation loc, int slotIndex) {
        PlayerClassInfo activeClass = getActiveClass();
        if (activeClass != null) {
            boolean didWork = activeClass.addPassiveToSlot(loc, slotIndex);
            setRefreshPassiveTalents();
            sendCurrentClassUpdate();
            return didWork;
        }
        return false;
    }

    public boolean canActivateUltimateForSlot(ResourceLocation loc, int slotIndex) {
        PlayerClassInfo activeClass = getActiveClass();
        if (activeClass != null) {
            return activeClass.canAddUltimateToSlot(loc, slotIndex);
        }
        return false;
    }

    @Override
    public boolean activateUltimateForSlot(ResourceLocation loc, int slotIndex) {
        PlayerClassInfo activeClass = getActiveClass();
        if (activeClass != null) {
            ResourceLocation currentAbility = activeClass.getUltimateForSlot(slotIndex);
            if (loc.equals(MKURegistry.INVALID_ABILITY) && !currentAbility.equals(MKURegistry.INVALID_ABILITY)){
                unlearnAbility(currentAbility, false, true);
                activeClass.clearUltimateSlot(slotIndex);
                sendCurrentClassUpdate();
                return true;
            } else {
                if (!currentAbility.equals(MKURegistry.INVALID_ABILITY)){
                    activeClass.clearUltimateSlot(slotIndex);
                    unlearnAbility(currentAbility, false, true);
                }
                boolean didWork = activeClass.addUltimateToSlot(loc, slotIndex);
                if (didWork){
                    learnAbility(loc, false);
                }
                sendCurrentClassUpdate();
                return didWork;
            }
        }
        return false;
    }


    @Override
    public boolean hasUltimates() {
        PlayerClassInfo activeClass = getActiveClass();
        if (activeClass != null) {
            return activeClass.hasUltimate();
        }
        return false;
    }

    @Override
    public int getActionBarSize() {
        ResourceLocation loc = getAbilityInSlot(GameConstants.ACTION_BAR_SIZE - 1);
        return hasUltimates() || !loc.equals(MKURegistry.INVALID_ABILITY) ?
                GameConstants.ACTION_BAR_SIZE : GameConstants.CLASS_ACTION_BAR_SIZE;
    }

    private void swapHands() {
        player.setPrimaryHand(player.getPrimaryHand().opposite());
        ItemStack mainHand = player.getHeldItemMainhand();
        player.setHeldItem(EnumHand.MAIN_HAND, player.getHeldItem(EnumHand.OFF_HAND));
        player.setHeldItem(EnumHand.OFF_HAND, mainHand);
    }

    public void performDualWieldSequence() {
        if (!isDualWielding) {
            isDualWielding = true;
            originalMainHand = player.getPrimaryHand();
        } else {
            swapHands();
        }
        ticksSinceLastSwing = 0;
    }

    public void endDualWieldSequence() {
        if (isDualWielding) {
            if (player.getPrimaryHand() != originalMainHand) {
                swapHands();
            }
            isDualWielding = false;
        }
    }

    @Override
    public boolean isDualWielding() {
        return isDualWielding;
    }

    @Override
    public void setArbitraryCooldown(ResourceLocation loc, int cooldown) {
        if (cooldown > 0) {
            abilityTracker.setCooldown(loc, cooldown);
        } else {
            abilityTracker.removeCooldown(loc);
        }
    }

    @Override
    public int getArbitraryCooldown(ResourceLocation loc) {
        return abilityTracker.getCooldownTicks(loc);
    }

    private void updateTalents() {
        removeTalents();
        if (!hasChosenClass()) {
            return;
        }
        applyTalents();
    }

    private void applyTalents() {
        PlayerClassInfo activeClass = getActiveClass();
        if (activeClass == null) {
            return;
        }
        activeClass.applyAttributesModifiersToPlayer(player);
        // Since this can be called early, don't try to apply potions before being added to the world
        if (player.isAddedToWorld()) {
            refreshPassiveTalents(activeClass);
        } else {
            setRefreshPassiveTalents();
        }
    }

    private void refreshPassiveTalents(PlayerClassInfo activeClass) {
        removeAllPassiveTalents(player);
        activeClass.applyPassives(player, this, player.getEntityWorld());
        ItemEventHandler.checkEquipment(player);
    }

    private void removeTalents() {
        removeAllAttributeTalents(player);
        removeAllPassiveTalents(player);
    }

    private void removeAllAttributeTalents(EntityPlayer player) {
        AbstractAttributeMap attributeMap = player.getAttributeMap();
        for (RangedAttributeTalent entry : MKURegistry.getAllAttributeTalents()) {
            IAttributeInstance instance = attributeMap.getAttributeInstance(entry.getAttribute());
            if (instance != null) {
                instance.removeModifier(entry.getUUID());
            }
        }
    }

    private void removeAllPassiveTalents(EntityPlayer player) {
        for (PassiveAbilityTalent talent : MKURegistry.getAllPassiveTalents()) {
            if (player.isPotionActive(talent.getAbility().getPassiveEffect())) {
                talent.getAbility().removeEffect(player, this, player.world);
            }
        }
    }

    public void setRefreshPassiveTalents() {
        needPassiveTalentRefresh = true;
    }

    public boolean getPassiveTalentsUnlocked() {
        return talentPassivesUnlocked;
    }

    void removePassiveEffect(PassiveAbilityPotionBase passiveEffect) {
        talentPassivesUnlocked = true;
        player.removePotionEffect(passiveEffect);
        talentPassivesUnlocked = false;
    }

    private void updatePlayerStats(boolean doTalents) {
        if (!hasChosenClass()) {
            setMana(0);
            setTotalMana(0);
            setManaRegen(0);
            setHealthRegen(0);
            setHealth(Math.min(20, this.player.getHealth()));
            setTotalHealth(20);
            if (doTalents) {
                removeTalents();
            }
        } else {
            PlayerClass playerClass = MKURegistry.getClass(getClassId());
            if (playerClass == null)
                return;

            int level = getLevel();
            int newTotalMana = playerClass.getBaseMana() + (level * playerClass.getManaPerLevel());
            int newTotalHealth = playerClass.getBaseHealth() + (level * playerClass.getHealthPerLevel());
            float newManaRegen = playerClass.getBaseManaRegen() + (level * playerClass.getManaRegenPerLevel());
            setTotalMana(newTotalMana);
            setMana(getMana()); // Refresh after changing total mana
            setTotalHealth(newTotalHealth);
            setHealth(Math.min(newTotalHealth, this.player.getHealth()));
            setManaRegen(newManaRegen);
            setHealthRegen(0);
            if (doTalents) {
                updateTalents();
                checkTalentTotals();
            }
        }
    }

    private void setTotalHealth(float maxHealth) {
        this.player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(maxHealth);
    }

    @Override
    public float getTotalHealth() {
        return player.getMaxHealth();
    }

    @Override
    public void setHealth(float health) {
        health = MathHelper.clamp(health, 0, getTotalHealth());
        this.player.setHealth(health);
    }

    @Override
    public float getHealth() {
        return player.getHealth();
    }

    @Override
    public float getMeleeCritChance() {
        return (float) player.getEntityAttribute(PlayerAttributes.MELEE_CRIT).getAttributeValue();
    }

    @Override
    public float getSpellCritChance() {
        return (float) player.getEntityAttribute(PlayerAttributes.SPELL_CRIT).getAttributeValue();
    }

    @Override
    public float getSpellCritDamage() {
        return (float) player.getEntityAttribute(PlayerAttributes.SPELL_CRITICAL_DAMAGE).getAttributeValue();
    }

    @Override
    public float getMeleeCritDamage() {
        return (float) player.getEntityAttribute(PlayerAttributes.MELEE_CRITICAL_DAMAGE).getAttributeValue();
    }

    @Override
    public float getCooldownProgressSpeed() {
        return (float) player.getEntityAttribute(PlayerAttributes.COOLDOWN).getAttributeValue();
    }

    @Override
    public float getMagicDamageBonus() {
        return (float) player.getEntityAttribute(PlayerAttributes.MAGIC_ATTACK_DAMAGE).getAttributeValue();
    }

    @Override
    public float getMagicArmor() {
        return (float) player.getEntityAttribute(PlayerAttributes.MAGIC_ARMOR).getAttributeValue();
    }

    @Override
    public float getHealBonus() {
        return (float) player.getEntityAttribute(PlayerAttributes.HEAL_BONUS).getAttributeValue();
    }

    @Override
    public float getBuffDurationBonus() {
        return (float) player.getEntityAttribute(PlayerAttributes.BUFF_DURATION).getAttributeValue();
    }

    @Override
    public boolean hasChosenClass() {
        return !getClassId().equals(MKURegistry.INVALID_CLASS);
    }

    @Override
    public int getUnspentPoints() {
        return privateData.get(UNSPENT_POINTS);
    }

    private void setUnspentPoints(int unspentPoints) {
        // You shouldn't have more unspent points than your levels
        if (unspentPoints > getLevel())
            return;
        privateData.set(UNSPENT_POINTS, unspentPoints);
    }

    private void setClassId(ResourceLocation classId) {
        privateData.set(CLASS_ID, classId.toString());
    }

    @Override
    public ResourceLocation getClassId() {
        return new ResourceLocation(privateData.get(CLASS_ID));
    }

    @Override
    public int getLevel() {
        return privateData.get(LEVEL);
    }

    @Override
    public boolean canLevelUp() {
        return (this.player.experienceLevel >= (this.getLevel() + 1)
                && this.hasChosenClass()
                && this.getLevel() < GameConstants.MAX_CLASS_LEVEL);
    }


    @Override
    public void levelUp() {
        if (canLevelUp()) {
            int newLevel = this.getLevel() + 1;
            this.setLevel(newLevel);
            this.setUnspentPoints(this.getUnspentPoints() + 1);
            this.player.addExperienceLevel(-newLevel);
        }
    }

    private void setLevel(int level) {
        int currentLevel = getLevel();
        privateData.set(LEVEL, level);
        updatePlayerStats(false);
        MinecraftForge.EVENT_BUS.post(new PlayerClassEvent.LevelChanged(player, this, getActiveClass(), currentLevel, level));
    }

    private void setActiveAbilities(ResourceLocation[] abilities) {
        int max = Math.min(abilities.length, GameConstants.ACTION_BAR_SIZE);
        for (int i = 0; i < max; i++) {
            setAbilityInSlot(i, abilities[i]);
        }
        updateActiveAbilities();
    }

    private ResourceLocation[] getActiveAbilities() {
        ResourceLocation[] actives = new ResourceLocation[GameConstants.ACTION_BAR_SIZE];
        for (int i = 0; i < GameConstants.ACTION_BAR_SIZE; i++) {
            actives[i] = getAbilityInSlot(i);
        }
        return actives;
    }

    private void setAbilityInSlot(int slotIndex, ResourceLocation abilityId) {
        privateData.set(ACTION_BAR_ABILITY_ID[slotIndex], abilityId.toString());
    }

    private int getCurrentSlotForAbility(ResourceLocation abilityId) {
        for (int i = 0; i < GameConstants.ACTION_BAR_SIZE; i++) {
            if (getAbilityInSlot(i).equals(abilityId)) {
                return i;
            }
        }
        return GameConstants.ACTION_BAR_INVALID_SLOT;
    }

    private int getFirstFreeAbilitySlot() {
        return getCurrentSlotForAbility(MKURegistry.INVALID_ABILITY);
    }

    @Override
    public ResourceLocation getAbilityInSlot(int index) {
        if (index < ACTION_BAR_ABILITY_ID.length) {
            return new ResourceLocation(privateData.get(ACTION_BAR_ABILITY_ID[index]));
        }
        return MKURegistry.INVALID_ABILITY;
    }

    @Override
    public int getCurrentAbilityCooldown(ResourceLocation abilityId) {
        PlayerAbilityInfo abilityInfo = getAbilityInfo(abilityId);
        return abilityInfo != null ? abilityTracker.getCooldownTicks(abilityId) : GameConstants.ACTION_BAR_INVALID_COOLDOWN;
    }

    @Override
    public int getAbilityCooldown(PlayerAbility ability) {
        return PlayerFormulas.applyCooldownReduction(this, ability.getCooldownTicks(getAbilityRank(ability.getAbilityId())));
    }

    @Override
    public int getCooldownForLevel(PlayerAbility ability, int level) {
        return ability.getCooldownTicks(level);
    }

    @Override
    public int getAbilityRank(ResourceLocation abilityId) {
        PlayerAbilityInfo abilityInfo = getAbilityInfo(abilityId);
        return abilityInfo != null ? abilityInfo.getRank() : GameConstants.ABILITY_INVALID_RANK;
    }

    @SideOnly(Side.CLIENT)
    private int getAbilityRankForClient(ResourceLocation abilityId) {
        int slot = getCurrentSlotForAbility(abilityId);
        if (slot != GameConstants.ACTION_BAR_INVALID_SLOT) {
            return privateData.get(ACTION_BAR_ABILITY_RANK[slot]);
        }
        return GameConstants.ABILITY_INVALID_RANK;
    }

    private int getAbilityLearnIndex() {
        return getLevel() - getUnspentPoints();
    }

    @Override
    public boolean learnAbility(ResourceLocation abilityId, boolean consumePoint) {
        PlayerClassInfo classInfo = getActiveClass();

        // Can't learn an ability without a class
        if (classInfo == null) {
            return false;
        }
        PlayerAbility ability = MKURegistry.getAbility(abilityId);
        if (ability == null) {
            return false;
        }
        PlayerAbilityInfo info = getAbilityInfo(abilityId);
        if (info == null) {
            info = ability.createAbilityInfo();
        }

        if (consumePoint && getUnspentPoints() == 0)
            return false;

        if (!info.upgrade())
            return false;

        if (consumePoint) {
            int curUnspent = getUnspentPoints();
            if (curUnspent > 0) {
                setUnspentPoints(curUnspent - 1);
            } else {
                return false;
            }
            classInfo.setAbilitySpendOrder(abilityId, getAbilityLearnIndex());
        }

        if (abilityTracker.hasCooldown(abilityId)) {
            int newMaxCooldown = getAbilityCooldown(ability);
            int current = abilityTracker.getCooldownTicks(abilityId);
            setCooldown(info.getId(), Math.min(current, newMaxCooldown));
        }

        classInfo.putInfo(abilityId, info);
        updateToggleAbility(info);
        sendSingleAbilityUpdate(info);

        int slot = getCurrentSlotForAbility(abilityId);
        if (slot == GameConstants.ACTION_BAR_INVALID_SLOT) {
            // Skill was just learned so let's try to put it on the bar
            slot = getFirstFreeAbilitySlot();
            if (slot != GameConstants.ACTION_BAR_INVALID_SLOT) {
                setAbilityInSlot(slot, abilityId);
            }
        }

        if (slot != GameConstants.ACTION_BAR_INVALID_SLOT) {
            updateActiveAbilitySlot(slot);
        }

        return true;
    }

    public boolean unlearnAbility(ResourceLocation abilityId, boolean refundPoint, boolean allRanks) {
        PlayerAbilityInfo info = getAbilityInfo(abilityId);
        if (info == null || !info.isCurrentlyKnown()) {
            // We never knew it or it exists but is currently unlearned
            return false;
        }

        int ranks = 0;
        if (allRanks) {
            while (info.isCurrentlyKnown())
                if (info.downgrade())
                    ranks += 1;
        } else {
            if (info.downgrade())
                ranks += 1;
        }

        if (refundPoint) {
            int curUnspent = getUnspentPoints();
            setUnspentPoints(curUnspent + ranks);
        }

        updateToggleAbility(info);
        sendSingleAbilityUpdate(info);

        int slot = getCurrentSlotForAbility(abilityId);
        if (slot != GameConstants.ACTION_BAR_INVALID_SLOT) {
            updateActiveAbilitySlot(slot);
        }

        return true;
    }

    private void updateToggleAbility(PlayerAbilityInfo info) {
        PlayerAbility ability = info.getAbility();
        if (ability instanceof PlayerToggleAbility && player != null) {
            PlayerToggleAbility toggle = (PlayerToggleAbility) ability;

            if (info.isCurrentlyKnown()) {
                // If this is a toggle ability we must re-apply the effect to make sure it's working at the proper rank
                if (player.isPotionActive(toggle.getToggleEffect())) {
                    toggle.removeEffect(player, this, player.getEntityWorld());
                    toggle.applyEffect(player, this, player.getEntityWorld());
                }
            } else {
                // Unlearning, remove the effect
                toggle.removeEffect(player, this, player.getEntityWorld());
            }
        }
    }

    @Override
    public PlayerToggleAbility getActiveToggleGroupAbility(ResourceLocation groupId) {
        return activeToggleMap.get(groupId);
    }

    @Override
    public void clearToggleGroupAbility(ResourceLocation groupId) {
        activeToggleMap.remove(groupId);
    }

    @Override
    public void setToggleGroupAbility(ResourceLocation groupId, PlayerToggleAbility ability) {
        PlayerToggleAbility current = getActiveToggleGroupAbility(ability.getToggleGroupId());
        // This can also be called when rebuilding the activeToggleMap after transferring dimensions and in that case
        // ability will be the same as current
        if (current != null && current != ability) {
            current.removeEffect(player, this, player.getEntityWorld());
            setCooldown(current.getAbilityId(), getAbilityCooldown(current));
        }
        activeToggleMap.put(groupId, ability);
    }

    @Override
    public boolean isCasting() {
        return !getCastingAbility().equals(MKURegistry.INVALID_ABILITY);
    }

    @Override
    public int getCastTicks() {
        return privateData.get(CAST_TICKS);
    }

    @Override
    public void setCastTicks(int value) {
        privateData.set(CAST_TICKS, value);
    }

    @Override
    public ResourceLocation getCastingAbility() {
        return new ResourceLocation(privateData.get(CURRENT_CAST));
    }

    private void setCastingAbility(ResourceLocation abilityId) {
        privateData.set(CURRENT_CAST, abilityId.toString());
    }

    private void clearCastingAbility() {
        setCastingAbility(MKURegistry.INVALID_ABILITY);
        setCastTicks(0);
        currentCastState = null;
    }

    private CastState startCast(PlayerAbility ability, int castTime) {
        setCastingAbility(ability.getAbilityId());
        setCastTicks(castTime);
        return ability.createCastState(castTime);
    }

    @SideOnly(Side.CLIENT)
    private void updateCastTimeClient() {
        if (isCasting()) {
            ResourceLocation loc = getCastingAbility();
            PlayerAbility ability = MKURegistry.getAbility(loc);
            if (ability != null) {
                int currentCastTime = getCastTicks();
                ability.continueCastClient(player, this, player.getEntityWorld(), currentCastTime);
                if (!lastUpdateIsCasting) {
                    SoundEvent event = ability.getCastingSoundEvent();
                    if (event != null) {
                        int castTime = ability.getCastTime(getAbilityRankForClient(loc));
                        castingSound = new MovingSoundCasting(player, event, SoundCategory.PLAYERS, castTime);
                        Minecraft.getMinecraft().getSoundHandler().playSound(castingSound);
                    }
                }
            }
        } else {
            if (lastUpdateIsCasting && castingSound != null) {
                Minecraft.getMinecraft().getSoundHandler().stopSound(castingSound);
                castingSound = null;
            }
        }

        lastUpdateIsCasting = isCasting();
    }

    private void updateCastTime() {
        if (!isCasting())
            return;

        PlayerAbilityInfo abilityInfo = getAbilityInfo(getCastingAbility());
        if (abilityInfo != null) {
            PlayerAbility ability = abilityInfo.getAbility();
            int currentCastTime = getCastTicks();
            ability.continueCast(player, this, player.getEntityWorld(), currentCastTime, currentCastState);
            if (currentCastTime > 0) {
                setCastTicks(currentCastTime - 1);
            } else {
                ability.endCast(player, this, player.getEntityWorld(), currentCastState);
                completeAbility(ability, abilityInfo);
            }
        } else {
            clearCastingAbility();
        }
    }

    @Override
    public boolean executeHotBarAbility(int slotIndex) {
        ResourceLocation abilityId = getAbilityInSlot(slotIndex);
        if (abilityId.equals(MKURegistry.INVALID_ABILITY))
            return false;

        PlayerAbilityInfo info = getAbilityInfo(abilityId);
        if (info == null || !info.isCurrentlyKnown())
            return false;

        if (getCurrentAbilityCooldown(abilityId) == 0) {

            PlayerAbility ability = info.getAbility();
            if (ability != null &&
                    ability.meetsRequirements(this) &&
                    !MinecraftForge.EVENT_BUS.post(new PlayerAbilityCastEvent.Starting(player, this, ability, info))) {
                ability.execute(player, this, player.getEntityWorld());
                return true;
            }
        }

        return false;
    }


    private void completeAbility(PlayerAbility ability, PlayerAbilityInfo info) {
        int cooldown = ability.getCooldownTicks(info.getRank());
        cooldown = PlayerFormulas.applyCooldownReduction(this, cooldown);
        setCooldown(info.getId(), cooldown);
        currentCastState = null;
        SoundEvent sound = ability.getSpellCompleteSoundEvent();
        if (sound != null){
            AbilityUtils.playSoundAtServerEntity(player, sound, SoundCategory.PLAYERS);
        }
        clearCastingAbility();
        MinecraftForge.EVENT_BUS.post(new PlayerAbilityCastEvent.Completed(player, this, ability, info));
    }

    @Nullable
    @Override
    public CastState startAbility(PlayerAbility ability) {
        PlayerAbilityInfo info = getAbilityInfo(ability.getAbilityId());
        if (info == null || !info.isCurrentlyKnown() || isCasting())
            return null;

        float manaCost = getAbilityManaCost(ability.getAbilityId());
        setMana(getMana() - manaCost);

        int castTime = ability.getCastTime(info.getRank());
        if (castTime > 0) {
            currentCastState = startCast(ability, castTime);
            return currentCastState;
        } else {
            completeAbility(ability, info);
        }
        return null;
    }


    @Override
    @Nullable
    public PlayerAbilityInfo getAbilityInfo(ResourceLocation abilityId) {
        PlayerClassInfo info = getActiveClass();
        if (info != null){
            return info.getAbilityInfo(abilityId);
        }
        return null;
    }

    private void updateActiveAbilitySlot(int index) {
        ResourceLocation abilityId = getAbilityInSlot(index);
        PlayerAbilityInfo abilityInfo = getAbilityInfo(abilityId);

        boolean valid = abilityInfo != null && abilityInfo.isCurrentlyKnown();
        ResourceLocation id = valid ? abilityInfo.getId() : MKURegistry.INVALID_ABILITY;
        int rank = valid ? abilityInfo.getRank() : GameConstants.ABILITY_INVALID_RANK;

        setAbilityInSlot(index, id);
        privateData.set(ACTION_BAR_ABILITY_RANK[index], rank);

        if (abilityTracker.hasCooldown(abilityId)) {
            int cd = abilityTracker.getCooldownTicks(abilityId);
            setCooldown(abilityId, cd);
        }
    }

    private void updateActiveAbilities() {
        for (int i = 0; i < GameConstants.ACTION_BAR_SIZE; i++) {
            updateActiveAbilitySlot(i);
        }
    }

    @Override
    public void setManaRegen(float manaRegenRate) {
        player.getEntityAttribute(PlayerAttributes.MANA_REGEN).setBaseValue(manaRegenRate);
    }

    @Override
    public void setHealthRegen(float healthRegenRate) {
        player.getEntityAttribute(PlayerAttributes.HEALTH_REGEN).setBaseValue(healthRegenRate);
    }

    @Override
    public float getHealthRegenRate() {
        return (float) player.getEntityAttribute(PlayerAttributes.HEALTH_REGEN).getAttributeValue();
    }

    @Override
    public float getManaRegenRate() {
        return (float) player.getEntityAttribute(PlayerAttributes.MANA_REGEN).getAttributeValue();
    }

    private void setTotalMana(float totalMana) {
        player.getEntityAttribute(PlayerAttributes.MAX_MANA).setBaseValue(totalMana);
    }

    @Override
    public float getTotalMana() {
        return (float) player.getEntityAttribute(PlayerAttributes.MAX_MANA).getAttributeValue();
    }

    @Override
    public void setMana(float mana) {
        mana = MathHelper.clamp(mana, 0, getTotalMana());
        privateData.set(MANA, mana);
    }

    @Override
    public float getMana() {
        return privateData.get(MANA);
    }

    private void updateMana() {
        if (getMana() > getTotalMana())
            setMana(getTotalMana());

        if (this.getManaRegenRate() == 0.0f) {
            return;
        }
        regenTime += 1. / 20.;
        float i_regen = 3.0f / this.getManaRegenRate();
        if (regenTime >= i_regen) {
            if (this.getMana() < this.getTotalMana()) {
                addMana(1);
            }
            regenTime -= i_regen;
        }
    }

    private void updateHealth() {
        if (this.getHealthRegenRate() == 0.0f) {
            return;
        }
        healthRegenTime += 1. / 20.;
        float i_regen = 3.0f / this.getHealthRegenRate();
        if (healthRegenTime >= i_regen) {
            if (this.getHealth() > 0 && this.getHealth() < this.getTotalHealth()) {
                this.setHealth(this.getHealth() + 1);
            }
            healthRegenTime -= i_regen;
        }
    }

    private boolean isServerSide() {
        return this.player instanceof EntityPlayerMP;
    }

    public void forceUpdate() {
        markEntityDataDirty();
        sendBulkAbilityUpdate();
        sendBulkClassUpdate();
        updateActiveAbilities();
    }

    public void onJoinWorld() {
        Log.trace("PlayerData@onJoinWorld\n");

        if (isServerSide()) {
            checkPassiveEffects();
            rebuildActiveToggleMap();
            updatePlayerStats(true);
        } else {
            Log.trace("PlayerData@onJoinWorld - Client sending sync req\n");
            MKUltra.packetHandler.sendToServer(new PlayerSyncRequestPacket());
        }

    }


    private void updateDualWielding() {
        if (isDualWielding) {
            if (ticksSinceLastSwing > DUAL_WIELD_TIMEOUT) {
                endDualWieldSequence();
            } else {
                ticksSinceLastSwing++;
            }
        }
    }

    public void onTick() {
        abilityTracker.tick();
        if (!isServerSide()){
            updateCastTimeClient();
            return;
        }
        if (needPassiveTalentRefresh) {
            PlayerClassInfo info = getActiveClass();
            if (info != null) {
                refreshPassiveTalents(info);
            }
            needPassiveTalentRefresh = false;
        }
        updateMana();
        updateHealth();
        updateDualWielding();
        updateCastTime();
    }

    private void sendSingleAbilityUpdate(PlayerAbilityInfo info) {
        if (isServerSide()) {
            MKUltra.packetHandler.sendTo(new AbilityUpdatePacket(info), (EntityPlayerMP) player);
        }
    }

    private void sendBulkAbilityUpdate() {
        if (isServerSide()) {
            PlayerClassInfo classInfo = getActiveClass();
            if (classInfo != null){
                MKUltra.packetHandler.sendTo(new AbilityUpdatePacket(classInfo.getAbilityInfos()),
                        (EntityPlayerMP) player);
            }

        }
    }

    private void sendBulkClassUpdate() {
        if (isServerSide()) {
            MKUltra.packetHandler.sendTo(new ClassUpdatePacket(knownClasses.values()), (EntityPlayerMP) player);
        }
    }

    private void sendCurrentClassUpdate() {
        if (isServerSide()) {
            PlayerClassInfo activeClass = getActiveClass();
            if (activeClass != null) {
                MinecraftForge.EVENT_BUS.post(new PlayerClassEvent.Updated(player, this, activeClass));
                MKUltra.packetHandler.sendTo(new ClassUpdatePacket(activeClass), (EntityPlayerMP) player);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void clientSkillListUpdate(PlayerAbilityInfo info) {
        PlayerClassInfo classInfo = getActiveClass();
        if (classInfo != null){
            classInfo.putInfo(info.getId(), info);
        }
    }

    @SideOnly(Side.CLIENT)
    public void clientBulkKnownClassUpdate(Collection<PlayerClassInfo> info, boolean isFullUpdate) {
        if (isFullUpdate) {
            knownClasses.clear();
            info.forEach(ci -> {
                knownClasses.put(ci.getClassId(), ci);
                MinecraftForge.EVENT_BUS.post(new PlayerClassEvent.Updated(player, this, ci));
            });
        } else {
            info.forEach(ci -> {
                knownClasses.remove(ci.getClassId());
                knownClasses.put(ci.getClassId(), ci);
                MinecraftForge.EVENT_BUS.post(new PlayerClassEvent.Updated(player, this, ci));
            });
        }
    }


    private void serializeClasses(NBTTagCompound tag) {
        saveCurrentClass();

        NBTTagList classes = new NBTTagList();
        for (PlayerClassInfo info : knownClasses.values()) {
            NBTTagCompound sk = new NBTTagCompound();
            info.serialize(sk);
            classes.appendTag(sk);
        }
        tag.setTag("classes", classes);

        tag.setString("activeClassId", getClassId().toString());
    }

    private void deserializeClasses(NBTTagCompound tag) {
        if (tag.hasKey("classes")) {
            NBTTagList classes = tag.getTagList("classes", Constants.NBT.TAG_COMPOUND);
            knownClasses = new HashMap<>(classes.tagCount());

            for (int i = 0; i < classes.tagCount(); i++) {
                NBTTagCompound cls = classes.getCompoundTagAt(i);
                PlayerClassInfo info = new PlayerClassInfo(new ResourceLocation(cls.getString("id")));
                info.deserialize(cls);
                knownClasses.put(info.getClassId(), info);
            }
        }

        if (tag.hasKey("activeClassId", Constants.NBT.TAG_STRING)) {
            ResourceLocation classId = new ResourceLocation(tag.getString("activeClassId"));
            // If the character was saved with a class that doesn't exist anymore (say from a plugin),
            // reset the character to have no class
            if (MKURegistry.getClass(classId) == null)
                classId = MKURegistry.INVALID_CLASS;

            activateClass(classId);
            sendBulkAbilityUpdate();
        } else {
            activateClass(MKURegistry.INVALID_CLASS);
        }
    }

    @Override
    public void serialize(NBTTagCompound nbt) {
        nbt.setFloat("mana", getMana());
        abilityTracker.serialize(nbt);
//        serializeAbilities(nbt);
        serializeClasses(nbt);
    }

    @Override
    public void deserialize(NBTTagCompound nbt) {
        abilityTracker.deserialize(nbt);
//        deserializeAbilities(nbt);
        deserializeClasses(nbt);
        if (nbt.hasKey("mana")) {
            setMana(nbt.getFloat("mana"));
        }
    }

    public void clone(EntityPlayer previous) {

        PlayerData prevData = (PlayerData) MKUPlayerData.get(previous);
        if (prevData == null)
            return;

        NBTTagCompound tag = new NBTTagCompound();
        prevData.serialize(tag);
        deserialize(tag);
        updateActiveAbilities();
    }

    private void validateAbilityPoints() {
        if (!hasChosenClass())
            return;

        PlayerClass playerClass = MKURegistry.getClass(getClassId());
        if (playerClass == null)
            return;

        int totalPoints = getUnspentPoints();
        for (int i = 0; i < GameConstants.CLASS_ACTION_BAR_SIZE; i++) {
            PlayerAbility ability = playerClass.getOfferedAbilityBySlot(i);
            if (ability == null)
                continue;

            PlayerAbilityInfo info = getAbilityInfo(ability.getAbilityId());
            if (info == null || !info.isCurrentlyKnown())
                continue;

            totalPoints += info.getRank();
        }

        Log.info("validateAbilityPoints: %s expected %d calculated %d", player.getName(), getLevel(), totalPoints);
        if (totalPoints != getLevel()) {
            resetAbilities(false);
            player.sendMessage(new TextComponentString("Your abilities have been reset and points refunded. Sorry for the inconvenience"));
        }
    }

    public void doDeath() {
        if (getLevel() > 1) {
            int curUnspent = getUnspentPoints();
            if (curUnspent > 0) {
                setUnspentPoints(curUnspent - 1);
            } else {
                ResourceLocation lastAbility = getLastUpgradedAbility();
                if (!lastAbility.equals(MKURegistry.INVALID_ABILITY)) {
                    unlearnAbility(lastAbility, false, false);
                }
            }

            // Check to see if de-leveling will make us lower than the required level for some spells.
            // If so, unlearn the spell and refund the point.
            int newLevel = getLevel() - 1;
            Arrays.stream(getActiveAbilities())
                    .map(MKURegistry::getAbility)
                    .filter(Objects::nonNull)
                    .filter(ability -> ability.getType() == PlayerAbility.AbilityType.Active)
                    .filter(ability -> {
                        int currentRank = getAbilityRank(ability.getAbilityId());
                        // Subtract 1 because getRequiredLevel is a little weird. It actually tells you the required
                        // level to go up a rank, not the required level for the current rank
                        int newRank = currentRank - 1;
                        int reqLevel = ability.getRequiredLevel(newRank);
                        reqLevel = Math.max(1, reqLevel);
                        return reqLevel > newLevel;
                    })
                    .forEach(a -> unlearnAbility(a.getAbilityId(), true, false));

            setLevel(newLevel);
            validateAbilityPoints();
        }
    }

    @Override
    public boolean learnClass(IClassProvider provider, ResourceLocation classId) {
        if (knowsClass(classId)) {
            // Class was already known
            return true;
        }

        PlayerClass playerClass = MKURegistry.getClass(classId);
        if (playerClass == null)
            return false;

        if (!provider.teachesClass(playerClass))
            return false;

        PlayerClassInfo info = new PlayerClassInfo(classId);
        knownClasses.put(classId, info);
        sendBulkClassUpdate();
        MinecraftForge.EVENT_BUS.post(new PlayerClassEvent.Learned(player, this, info));

        // Learned class
        return true;
    }

    public void unlearnClass(ResourceLocation classId) {
        if (!knowsClass(classId)) {
            return;
        }

        // If it's the active class, switch to no class first
        if (getClassId().equals(classId))
            activateClass(MKURegistry.INVALID_CLASS);

        PlayerClassInfo info = knownClasses.remove(classId);

        // Unlearn all abilities offered by this class
        PlayerClass bc = MKURegistry.getClass(classId);
        if (bc != null) {
            bc.getAbilities().forEach(a -> unlearnAbility(a.getAbilityId(), false, true));
        }

        sendBulkClassUpdate();
        MinecraftForge.EVENT_BUS.post(new PlayerClassEvent.Removed(player, this, info));
    }

    private void saveCurrentClass() {
        if (!hasChosenClass()) {
            return;
        }

        PlayerClassInfo cinfo = getActiveClass();
        if (cinfo == null) {
            return;
        }

        // save current class data
        cinfo.save(this);
    }

    private void deactivateCurrentToggleAbilities() {
        for (int i = 0; i < GameConstants.ACTION_BAR_SIZE; i++) {
            ResourceLocation abilityId = getAbilityInSlot(i);
            PlayerAbility ability = MKURegistry.getAbility(abilityId);
            if (ability instanceof PlayerToggleAbility && player != null) {
                PlayerToggleAbility toggle = (PlayerToggleAbility) ability;
                toggle.removeEffect(player, this, player.getEntityWorld());
            }
        }
    }

    private void rebuildActiveToggleMap() {
        for (int i = 0; i < GameConstants.ACTION_BAR_SIZE; i++) {
            ResourceLocation abilityId = getAbilityInSlot(i);
            PlayerAbility ability = MKURegistry.getAbility(abilityId);
            if (ability instanceof PlayerToggleAbility && player != null) {
                PlayerToggleAbility toggle = (PlayerToggleAbility) ability;
                if (player.isPotionActive(toggle.getToggleEffect()))
                    setToggleGroupAbility(toggle.getToggleGroupId(), toggle);
            }
        }
    }

    private void checkPassiveEffects() {
        player.getActivePotionMap().forEach((p, e) -> {
            if (p instanceof SpellPotionBase) {
                SpellPotionBase sp = (SpellPotionBase) p;
                if (!sp.canPersistAcrossSessions()) {
                    SpellCast cast = sp.createReapplicationCast(player);
                    if (cast != null) {
                        // Force PotionEffect combination so it will call add/remove of potion attributes
                        player.addPotionEffect(cast.toPotionEffect(e.getDuration(), e.getAmplifier()));
                    }
                }
            }
        });
    }

    @Override
    public void activateClass(ResourceLocation classId) {

        int level;
        int unspent;
        ResourceLocation[] hotbar;

        ResourceLocation oldClassId = getClassId();
        saveCurrentClass();
        deactivateCurrentToggleAbilities();

        if (classId.equals(MKURegistry.INVALID_CLASS) || !knowsClass(classId)) {
            // Switching to no class

            classId = MKURegistry.INVALID_CLASS;
            level = 1;
            unspent = 1;
            hotbar = new ResourceLocation[GameConstants.ACTION_BAR_SIZE];
            Arrays.fill(hotbar, MKURegistry.INVALID_ABILITY);
        } else {
            PlayerClassInfo info = knownClasses.get(classId);
            level = info.getLevel();
            unspent = info.getUnspentPoints();
            hotbar = info.getActiveAbilities();
        }

        setClassId(classId);
        setLevel(level);
        setUnspentPoints(unspent);
        setActiveAbilities(hotbar);
        ItemEventHandler.checkEquipment(player);
        updateTalents();
        checkTalentTotals();
        validateAbilityPoints();
        sendCurrentClassUpdate();

        if (!classId.equals(oldClassId)) {
            MinecraftForge.EVENT_BUS.post(new PlayerClassEvent.ClassChanged(player, this, getActiveClass(), oldClassId));
        }
    }

    @Override
    public Collection<ResourceLocation> getKnownClasses() {
        return Collections.unmodifiableSet(knownClasses.keySet());
    }

    @Override
    public ArmorClass getArmorClass() {
        PlayerClass currentClass = MKURegistry.getClass(getClassId());
        if (currentClass == null)
            return ArmorClass.ALL;
        ArmorClass ac = currentClass.getArmorClass();
        return player.isPotionActive(ArmorTrainingPotion.INSTANCE) ? ac.getSuccessor() : ac;
    }

    @Override
    public boolean canWearArmor(ItemArmor item) {
        ArmorClass effective = getArmorClass();
        // If no class, default to vanilla behaviour of wearing anything
        // Then check the current class if it's allowed
        // Then check for special exceptions granted by other means
        return effective == null || effective.canWear(item);
    }



    @Override
    public boolean setCooldown(ResourceLocation abilityId, int cooldownTicks) {
        PlayerAbilityInfo info = getAbilityInfo(abilityId);
        if (info == null)
            return false;

        if (cooldownTicks > 0) {
            abilityTracker.setCooldown(info.getId(), cooldownTicks);
        } else {
            abilityTracker.removeCooldown(info.getId());
        }
        return true;
    }

    @Override
    public void addToAllCooldowns(int cooldownTicks) {
        abilityTracker.iterateActive((loc, ticks) -> {
            abilityTracker.setCooldown(loc, ticks + cooldownTicks);
        });
    }

    @Override
    public float getCooldownPercent(PlayerAbilityInfo abilityInfo, float partialTicks) {
        return abilityInfo != null ? abilityTracker.getCooldown(abilityInfo.getId(), partialTicks) : 0.0f;
    }

    @Override
    public float getAbilityManaCost(ResourceLocation abilityId) {
        PlayerAbilityInfo abilityInfo = getAbilityInfo(abilityId);
        if (abilityInfo == null) {
            return 0.0f;
        }
        return PlayerFormulas.applyManaCostReduction(this, abilityInfo.getAbility().getManaCost(abilityInfo.getRank()));
    }

    public void debugResetAllCooldowns() {
        abilityTracker.removeAll();
        updateActiveAbilities();
    }

    public void debugDumpAllAbilities(ICommandSender sender) {
        String msg = "All active cooldowns:";
        sender.sendMessage(new TextComponentString(msg));
        abilityTracker.iterateActive((abilityId, current) -> {
            String name;
            int max;
            PlayerAbility ability = MKURegistry.getAbility(abilityId);
            if (ability != null) {
                name = ability.getTranslationKey();
                max = getAbilityCooldown(ability);
            } else {
                name = abilityId.toString();
                max = abilityTracker.getMaxCooldownTicks(abilityId);
            }
            ITextComponent line = new TextComponentTranslation(name).appendText(String.format(": %d / %d", current, max));
            sender.sendMessage(line);
        });
    }

    public void setInSpellTriggerCallback(String tag, boolean enable) {
        if (enable) {
            activeSpellTriggers.add(tag);
        }
        else {
            activeSpellTriggers.remove(tag);
        }
    }

    public boolean isInSpellTriggerCallback(String tag) {
        return activeSpellTriggers.contains(tag);
    }

    public boolean resetAbilities(boolean includeTalents) {
        if (!hasChosenClass())
            return false;

        PlayerClass playerClass = MKURegistry.getClass(getClassId());
        if (playerClass == null)
            return false;

        PlayerClassInfo classInfo = getActiveClass();
        if (classInfo == null)
            return false;

        for (int i = 0; i < GameConstants.CLASS_ACTION_BAR_SIZE; i++) {
            PlayerAbility ability = playerClass.getOfferedAbilityBySlot(i);
            if (ability == null)
                continue;
            unlearnAbility(ability.getAbilityId(), false, true);
        }

        classInfo.clearAbilitySpendOrder();
        setUnspentPoints(getLevel());

        return true;
    }
}
