package ostrowski.combat.common;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.*;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;
import ostrowski.DebugBreak;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.enums.*;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.spells.IInstantaneousSpell;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.spells.mage.*;
import ostrowski.combat.common.spells.priest.Deity;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.SpellGroup;
import ostrowski.combat.common.spells.priest.SpellSummonBeing;
import ostrowski.combat.common.spells.priest.elemental.SpellSwim;
import ostrowski.combat.common.spells.priest.evil.SpellParalyze;
import ostrowski.combat.common.spells.priest.good.SpellPacify;
import ostrowski.combat.common.things.*;
import ostrowski.combat.common.weaponStyles.WeaponStyle;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackRanged;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.common.wounds.WoundCantBeAppliedToTargetException;
import ostrowski.combat.protocol.request.*;
import ostrowski.combat.server.*;
import ostrowski.protocol.IRequestOption;
import ostrowski.protocol.ObjectChanged;
import ostrowski.protocol.SerializableObject;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


/*
 * Created on May 3, 2006
 */
/*
 * @author Paul
 */
public class Character extends SerializableObject implements IHolder, Enums, IMonitorableObject, IMonitoringObject, Comparable<Character> {
   //   private final transient MonitoredObject   monitoredObj                               = new MonitoredObject("Character");
//   private final transient MonitoringObject  monitoringObj                              = new MonitoringObject("Character");
   public transient ArenaLocationBook                   mapWatcher                = null;
   // character traits:
   private          String                              name                      = null;
   private          Race                                race                      = null;
   private final    HashMap<Attribute, Byte>            attributes                = new HashMap<>();
   public final     HashMap<LimbType, Limb>             limbs                     = new HashMap<>();
   private final    List<Thing>                         equipment                 = new ArrayList<>();
   private          Armor                               armor                     = Armor.getArmor("", null);
   private final    HashMap<ProfessionType, Profession> professionsList           = new HashMap<>();
   private          List<MageSpell>                     knownMageSpellsList       = new ArrayList<>();
   private          List<Advantage>                     advList                   = new ArrayList<>();
   public           IInstantaneousSpell                 bestDefensiveSpell_melee  = null;
   public           IInstantaneousSpell                 bestDefensiveSpell_ranged = null;
   public           IInstantaneousSpell                 bestDefensiveSpell_spell  = null;
   private          AI_Type                             aiType                    = null;
   public           RequestAction                       lastAction;

   final Semaphore lock_equipment = new Semaphore("equipment", CombatSemaphore.CLASS_CHARACTER_EQUIPMENT);

   // computed values:
   private byte buildBase;
   private byte damageBase;

   // active values:
   private       Condition     condition;
   public        int           targetID         = -1;
   private       byte          aimDuration      = 0;
   private final List<Spell>   activeSpellsList = new ArrayList<>();
   private final List<Integer> orderedTargetIds = new ArrayList<>();

   // location values
   public               int     uniqueID                                   = -1;
   public               byte    teamID                                     = -1;
   private              int     headCount                                  = 1;
   private              int     eyeCount                                   = 2;
   private              int     legCount                                   = 2;
   private              int     wingCount                                  = 0;
   private              Spell   currentSpell                               = null;
   private              boolean isBerserking                               = false;
   private              boolean hasInitiativeAndActionsEverBeenInitialized = false;
   private static final String  SEPARATOR_MAIN                             = "|";
   private static final String  SEPARATOR_SECONDARY                        = ";";
   private static final String  SEPARATOR_TIRTIARY                         = ":";
   public static final  String  YOU_ARE_BEING_TARGETED_BY                  = " \nYou are being targeted by ";

   private final List<IHolder>          placedIntoHoldThisTurn = new ArrayList<>();
   private final HashMap<IHolder, Byte> heldPenalties          = new HashMap<>();
   private       Character              holdTarget             = null;

   private final transient HashMap<String, DrawnObject> drawnObjectsCache       = new HashMap<>();
   private final transient List<String>                 recentlyDrawnObjectKeys = new ArrayList<>();


   public Character(String name, String raceName, Gender gender, byte[] atts, String armorName, HashMap<LimbType, Limb> limbs,
                    List<Thing> equipment, Profession[] professions, MageSpell[] mageSpells, Advantage[] advantages) {
      this.name = name;
      IMonitorableObject.monitoredObj.objectIDString = this.getClass().getName();
      IMonitoringObject._monitoringObj.objectIDString = this.getClass().getName();

      for (Attribute att : Attribute.values()) {
         attributes.put(att, atts[att.value]);
      }
      setRace(raceName, gender);

      synchronized (this.equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
            this.equipment.clear();
            this.equipment.addAll(equipment);
         }
      }

      armor = Armor.getArmor(armorName, getRace());
      condition = new Condition(this);
      if (professions != null) {
         for (Profession element : professions) {
            professionsList.put(element.getType(), element);
         }
      }
      if (mageSpells != null) {
         knownMageSpellsList.addAll(Arrays.asList(mageSpells));
      }
      if (advantages != null) {
         advList.addAll(Arrays.asList(advantages));
      }
      if (limbs != null) {
         for (Hand curHand : getArms()) {
            Thing heldThing = curHand.getHeldThing();
            if (heldThing == null) {
               curHand.setHeldThing(null, this);
            } else {
               curHand.setHeldThing(heldThing.clone(), this);
            }
         }
      }
      resetSpellPoints();
      updateWeapons();
   }

   public Orientation getOrientation() {
      return condition.getOrientation();
   }

   public boolean isInCoordinates(ArenaCoordinates loc) {
      return condition.isInCoordinates(loc);
   }

   public List<ArenaCoordinates> getCoordinates() {
      return condition.getCoordinates();
   }

   public ArenaCoordinates getHeadCoordinates() {
      return condition.getHeadCoordinates();
   }

   public ArenaLocation getLimbLocation(LimbType limbType, CombatMap map) {
      return condition.getLimbLocation(limbType, map);
   }

   public ArenaLocation getAttackFromLocation(RequestAction action, CombatMap map) {
      return getLimbLocation(action.getLimb(), map);
   }

   public List<Limb> getLimbs() {
      List<Limb> limbs = new ArrayList<>();
      // Make sure we return a list that is in the same order as the LimbType array, so Head is return first.
      for (LimbType type : LimbType.values()) {
         Limb limb = this.limbs.get(type);
         if (limb != null) {
            limbs.add(limb);
         }
      }
      return limbs;
   }

   public Character() {
      for (Attribute att : Attribute.values()) {
         attributes.put(att, (byte) 0);
      }

      setRace(Race.NAME_Human, Gender.MALE);

      refreshDefenses();
      condition = new Condition(this);
   }

   private void initHands() {
      HashMap<LimbType, Limb> newLimbs = new HashMap<>();
      for (LimbType limbType : LimbType.values()) {
         Limb limb = race.createLimb(limbType);
         if (limb != null) {
            newLimbs.put(limbType, limb);
         }
      }
      // Transfer anything from our current hand either to the new hands, or to our equipment belt:
      for (Hand curHand : getArms()) {
         Hand newHand = (Hand) (newLimbs.get(curHand.limbType));
         Thing heldThing = curHand.getHeldThing();

         if (newHand != null) {
            newHand.setHeldThing(heldThing, this);
         } else {
            if ((heldThing != null) && (heldThing.isReal())) {
               addEquipment(heldThing);
            }
         }
      }

      limbs.clear();
      for (LimbType limbType : newLimbs.keySet()) {
         limbs.put(limbType, newLimbs.get(limbType));
      }
   }

   public byte getHandPenalties(LimbType limbType, SkillType skillType) {
      // Use Penalty is for non-ambidextrous characters. left hands normally have a -3 penalty.
      if (hasAdvantage(Advantage.AMBIDEXTROUS)) {
         return 0;
      }
      Thing heldThing = limbs.get(limbType).getWeapon(this);
      if (heldThing == null) {
         return 0;
      }
      return heldThing.getHandUseagePenalties(limbType, this, skillType);
   }

   public boolean isAmbidextrous() {
      return hasAdvantage(Advantage.AMBIDEXTROUS);
   }

   public boolean isBerserker() {
      return hasAdvantage(Advantage.BERSERKER);
   }

   public boolean isBerserking() {
      return isBerserking && isBerserker();
   }

   public void setIsBerserking(boolean isBerserking) {
      this.isBerserking = isBerserking;
   }

   public boolean isRegenerative() {
      return race.getAdvantage(Advantage.REGENERATION) != null;
   }

   public boolean hasWings() {
      return wingCount > 0;
   }

   public boolean isMounted() {
      return isFlying() || getRace().getName().equals(Race.NAME_Centaur);
   }

   public boolean isFlying() {
      // Assume that if it has all its wings, it's flying.
      if (hasWings() && (wingCount == race.getWingCount())) {
         return true;
      }

      // Without wings, it can't fly, unless it has a flying spell active:
      for (Spell spell : activeSpellsList) {
         if (spell instanceof SpellFlight) {
            return true;
         }
         if (spell instanceof SpellLevitate) {
            return true;
         }
      }
      return false;
   }

   public boolean hasPeripheralVision() {
      return hasAdvantage(Advantage.PERIPHERAL_VISION);
   }

   public boolean hasAdvantage(String advName) {
      if (getAdvantage(advName) != null) {
         return true;
      }
      return (getRace().hasProperty(advName));
   }

   public Advantage getAdvantage(String advName) {
      for (Advantage adv : advList) {
         if (adv.toString().equals(advName)) {
            return adv;
         }
         if (adv.name.equals(advName)) {
            return adv;
         }
      }
      return race.getAdvantage(advName);
   }

   public boolean addAdvantage(Advantage newAdvantage) {
      List<String> existingAdvNames = new ArrayList<>();
      for (Advantage advantage : advList) {
         existingAdvNames.add(advantage.getName());
         if (advantage.getName().equals(newAdvantage.getName())) {
            if (advantage.getLevel() == newAdvantage.getLevel()) {
               return false;
            }
            advantage.setLevel(newAdvantage.getLevel());
            return true;
         }
      }
      if (newAdvantage.isAllowed(existingAdvNames, getRace())) {
         if (!existingAdvNames.contains(newAdvantage.getName())) {
            advList.add(newAdvantage);
            return true;
         }
      }
      return false;
   }

   public boolean computeWealth() {
      return computeWealth(getTotalCost());
   }

   /**
    * This method computes the lowest wealth setting that allows the character
    * to cover all their costs. If the wealth level changes, this method returns 'true'.
    *
    * @param moneySpent
    * @return
    */
   private boolean computeWealth(int moneySpent) {
      Advantage wealth = getAdvantage(Advantage.WEALTH);
      if (wealth == null) {
         wealth = Advantage.getAdvantage(Advantage.WEALTH);
         addAdvantage(wealth);
      }

      float adjustedMoneySpent = moneySpent / race.getWealthMultiplier();

      List<String> levels = wealth.getLevelNames();
      for (byte i = 0; i < levels.size(); i++) {
         int level = Integer.parseInt(levels.get(i).substring(1).replaceAll(",", ""));// remove the '$' and all commas
         if (adjustedMoneySpent <= level) {
            if (wealth.level == i) {
               return false;
            }
            wealth.level = i;
            addAdvantage(wealth);
            return true;
         }
      }
      DebugBreak.debugBreak();
      return false;
   }

   public byte getAimDuration(int targetID) {
      if (this.targetID == targetID) {
         return aimDuration;
      }
      return 0;
   }

   public void clearAimDuration() {
      aimDuration = 0;
   }

   public List<Profession> getProfessionsList() {
      List<Profession> copy = new ArrayList<>();
      for (Profession prof : professionsList.values()) {
         copy.add(new Profession(prof));
      }
      return copy;
   }
   public Profession getProfession(ProfessionType professionType) {
      return professionsList.get(professionType);
   }

   public void setProfessionsList(List<Profession> newProfessions) {
      professionsList.clear();
      for (Profession profession : newProfessions) {
         professionsList.put(profession.getType(), new Profession(profession));
      }
   }

   public List<MageSpell> getMageSpellsList() {
      return knownMageSpellsList;
   }

   public void setSpellsList(List<MageSpell> newSpells) {
      knownMageSpellsList = newSpells;
      if (knownMageSpellsList.isEmpty()) {
         return;
      }
      Set<SkillType> expectedSkillTypes = knownMageSpellsList.stream()
                                                             .map(s -> Arrays.asList(s.prerequisiteSkillTypes))
                                                             .filter(Objects::nonNull)
                                                             .flatMap(List::stream)
                                                             .filter(Objects::nonNull)
                                                             .collect(Collectors.toSet());

      Profession spellCastingProf = professionsList.get(ProfessionType.Spellcasting);
      if (spellCastingProf == null) {
         // Set one of the skill as the primary skill. It doesn't matter which.
         SkillType primarySkill = expectedSkillTypes.iterator().next();
         spellCastingProf = new Profession(ProfessionType.Spellcasting, primarySkill, (byte) expectedSkillTypes.size());
         expectedSkillTypes.remove(primarySkill);
         professionsList.put(ProfessionType.Spellcasting, spellCastingProf);
      }
      if (spellCastingProf.getLevel() < expectedSkillTypes.size()) {
         spellCastingProf.setLevel((byte) Math.min(10, expectedSkillTypes.size()));
      }
      Profession finalSpellCastingProf = spellCastingProf;
      expectedSkillTypes.removeIf(o -> finalSpellCastingProf.getFamiliarSkills().contains(o) ||
                                       finalSpellCastingProf.getProficientSkills().contains(o));
      List<SkillType> familiarSkills = new ArrayList<>();
      familiarSkills.addAll(spellCastingProf.getFamiliarSkills());
      familiarSkills.addAll(expectedSkillTypes);
      spellCastingProf.setFamiliarSkills(familiarSkills);
   }

   public void setAdvantagesList(List<Advantage> newAdv) {
      advList = newAdv;
      resetSpellPoints();
   }

   public List<Advantage> getAdvantagesList() {
      return advList;
   }

   public byte getPhysicalDamageBase() {
      return damageBase;
   }

   /**
    * @param attribute
    * @return the characters level for the specified attribute
    */
   public byte getAttributeLevel(Attribute attribute) {
      return attributes.get(attribute);
   }

   @Override
   public byte getAdjustedStrength() {
      return (byte) (attributes.get(Attribute.Strength) + race.getBuildModifier());
   }

   @Override
   public String getName() {
      return (name == null) ? "" : name;
   }

   public Race getRace() {
      return race;
   }

   public Gender getGender() {
      return race.getGender();
   }

   public Armor getArmor() {
      if ((armor == null) || (!armor.isReal())) {
         Armor naturalArmor = race.getNaturalArmor();
         if (naturalArmor != null) {
            return naturalArmor;
         }
      }
      return armor;
   }

   public Condition getCondition() {
      return condition;
   }

   public int getPointTotal() {
      int totalPoints = race.getCost();
      int attrCost = 0;
      int professionsCost = 0;
      int spellCost = 0;
      int advCost = 0;
      for (Attribute att : Attribute.values()) {
         attrCost += race.getAttCost(attributes.get(att), att);
      }
      for (Profession profession : professionsList.values()) {
         professionsCost += Rules.getProfessionCost(profession);
      }
      for (MageSpell spell : knownMageSpellsList) {
         spellCost += spell.getFamiliarity().getCost();
      }
      for (Advantage adv : advList) {
         advCost += adv.getCost(getRace());
      }
      totalPoints += attrCost;
      totalPoints += professionsCost;
      totalPoints += spellCost;
      totalPoints += advCost;
      return totalPoints;
   }

   public int getAttCostAtCurLevel(Attribute attribute) {
      return race.getAttCost(attributes.get(attribute), attribute);
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setAttribute(Attribute attr, byte attLevel, boolean containInLimits) {
      if (containInLimits) {
         byte baseAtt = (byte) (attLevel - race.getAttributeMods(attr));
         baseAtt = (byte) Math.min(baseAtt, Rules.getMaxAttribute());
         baseAtt = (byte) Math.max(baseAtt, Rules.getMinAttribute());
         attLevel = (byte) (baseAtt + race.getAttributeMods(attr));
      }
      attributes.put(attr, attLevel);
      refreshDefenses();
   }

   public void setRace(String raceName, Gender gender) {
      if (race != null) {
         if (race.getName().equals(raceName) && (race.getGender() == gender)) {
            return;
         }

         for (Attribute att : Attribute.values()) {
            attributes.put(att, (byte) (attributes.get(att) - race.getAttributeMods(att)));
         }
      }
      race = Race.getRace(raceName, gender);
      if (condition == null) {
         condition = new Condition(this);
      }
      for (Thing equ : equipment) {
         equ.setRacialBase(race);
      }
      for (Limb limb : limbs.values()) {
         Thing heldThing = limb.getHeldThing();
         if (heldThing != null) {
            heldThing.setRacialBase(race);
         }
      }
      if (armor != null) {
         armor.setRacialBase(race);
      }
      setOrientation(race.getBaseOrientation(), null/*diag*/);
      for (Attribute att : Attribute.values()) {
         attributes.put(att, (byte) (attributes.get(att) + race.getAttributeMods(att)));
      }
      // Validate the advantages
      List<Advantage> prevAdvList = advList;
      advList = new ArrayList<>();
      while (prevAdvList.size() > 0) {
         Advantage adv = prevAdvList.remove(0);
         List<String> advNames = Advantage.getAdvantagesNames(getPropertyNames(), getRace());
         if (advNames.contains(adv.getName())) {
            advList.add(adv);
         } else {
            // sex changed. Look for advantages that are gender-based
            if (adv.conflicts.contains(getRace().getGender().name)) {
               // Find the matching advantage. It will be the conflicts list:
               for (String conflict : adv.conflicts) {
                  Advantage newAdv = Advantage.getAdvantage(conflict);
                  if (newAdv != null) {
                     newAdv.setLevel(adv.getLevel());
                     advList.add(newAdv);
                     break;
                  }
               }
            }
         }
      }
      headCount = race.getHeadCount();
      legCount = race.getLegCount();
      eyeCount = race.getEyeCount();
      wingCount = race.getWingCount();
      initHands();
      // Since the build modifier may have changed, update defenses
      refreshDefenses();
      resetSpellPoints();
   }

   public void setArmor(String armorName) {
      if (armorName.equals(armor.getName())) {
         return;
      }
      armor = Armor.getArmor(armorName, getRace());
      refreshDefenses();
   }

   public void setInitiativeActionsAndMovementForNewTurn(int initiativeDieRoll) {
      byte initiative = (byte) (attributes.get(Attribute.Nimbleness) + initiativeDieRoll);
      condition.setInitiative(initiative);
      byte maxActionsPerRound = 3;
      byte actionsPerTurn = Rules.getStartingActions(this);
      for (Spell spell : activeSpellsList) {
         actionsPerTurn = spell.getModifiedActionsPerTurn(actionsPerTurn);
         maxActionsPerRound = spell.getModifiedActionsPerRound(maxActionsPerRound);
         spell.newTurn();
      }
      if (currentSpell != null) {
         currentSpell.newTurn();
      }

      condition.initializeActionsAndMovementForNewTurn(actionsPerTurn, maxActionsPerRound, getMovementRate());

      placedIntoHoldThisTurn.clear();

      hasInitiativeAndActionsEverBeenInitialized = true;
   }

   public boolean hasInitiativeAndActionsEverBeenInitialized() {
      return hasInitiativeAndActionsEverBeenInitialized;
   }

   public void reducePain(byte painReduction) {
      condition.reducePain(painReduction, getAttributeLevel(Attribute.Toughness));
   }

   public byte getActionsPerTurn() {
      byte actionsPerTurn = Rules.getStartingActions(this);
      for (Spell spell : activeSpellsList) {
         actionsPerTurn = spell.getModifiedActionsPerTurn(actionsPerTurn);
      }
      return actionsPerTurn;
   }

   public List<Spell> getActiveSpells() {
      return activeSpellsList;
   }

   public Spell isUnderSpell(String spellName) {
      for (Spell spell : activeSpellsList) {
         if (spell instanceof SpellSummonBeing) {
            continue;
         }
         if (spell.getName().equals(spellName)) {
            return spell;
         }
      }
      return null;
   }

   public byte getMovementRate() {
      byte movementRate = race.getMovementRate(Rules.getEncumbranceLevel(this));
      for (Spell spell : activeSpellsList) {
         movementRate = spell.getModifiedMovementPerRound(movementRate);
      }
      return movementRate;
   }

   public Wound modifyWoundFromAttack(Wound originalWound, Character defender, String attackingWeaponName, StringBuilder modificationsExplanation) {
      Wound newWound = originalWound;
      for (Spell spell : activeSpellsList) {
         newWound = spell.modifyDamageDealt(newWound, this, defender, attackingWeaponName, modificationsExplanation);
      }
      return newWound;
   }

   public Wound modifyWoundFromDefense(Wound originalWound) {
      Wound newWound = originalWound;
      for (Spell spell : activeSpellsList) {
         newWound = spell.modifyDamageReceived(newWound);
      }
      return newWound;
   }

   private byte getMaxActionsPerRound() {
      byte maxActions = 3;
      for (Spell spell : activeSpellsList) {
         maxActions += spell.getModifiedActionsPerRound(maxActions);
      }
      return maxActions;
   }

   // return 'true' if any actions remains to be spent
   public boolean endRound() {
      for (Limb limb : limbs.values()) {
         limb.endRound();
      }
      return condition.endRound();
   }

   public byte getInitiative() {
      return condition.getInitiative();
   }

   public byte getActionsAvailable(boolean usedForDefenseOnly) {
      return condition.getActionsAvailable(usedForDefenseOnly);
   }

   public Position getPosition() {
      return condition.getPosition();
   }

   public boolean isStanding() {
      return condition.isStanding();
   }

   public Position getMovingToPosition() {
      return condition.getPosition();
   }

   public byte getActionsNeededToChangePosition() {
      return condition.getActionsNeededToChangePosition();
   }

   public byte getPainPenalty(boolean accountForBerserking) {
      if (accountForBerserking && isBerserking()) {
         return 0;
      }
      return condition.getPenaltyPain();
   }

   public byte getWoundsAndPainPenalty() {
      return condition.getWoundsAndPainPenalty();
   }

   public byte getWounds() {
      return condition.getWounds();
   }

   public void collapseFromPain(CombatMap map) {
      condition.collapseFromPain(map, this);
   }

   public void refreshDefenses() {
      buildBase = (byte) (attributes.get(Attribute.Health) + race.getBuildModifier());
      damageBase = Rules.getDamageBase(getAdjustedStrength());
   }

   public SkillType getWeaponSkillType(LimbType limb, int attackStyle, boolean isGrapple, boolean isCounterAttack) {
      WeaponStyleAttack style = getWeaponStyle(limb, attackStyle, isGrapple, isCounterAttack);
      if (style != null) {
         return style.getSkillType();
      }
      return null;
   }

   public byte getWeaponSkill(LimbType limb, int attackStyle, boolean isGrapple, boolean isCounterAttack, boolean accountForHandPenalty, boolean adjustForHolds) {
      WeaponStyleAttack style = getWeaponStyle(limb, attackStyle, isGrapple, isCounterAttack);
      if (style != null) {
         return getSkillLevel(style, false, accountForHandPenalty ? limb : null, true/*sizeAdjust*/, true/*adjustForEncumbrance*/, adjustForHolds);
      }
      return -1;
   }

   public byte getAdjustedWeaponSkill(LimbType limb, int attackStyle, boolean isGrapple, boolean isCounterAttack, boolean accountForHandPenalty, boolean adjustForHolds) {
      WeaponStyleAttack style = getWeaponStyle(limb, attackStyle, isGrapple, isCounterAttack);
      if (style != null) {
         return getAdjustedSkillLevel(style, false, accountForHandPenalty ? limb : null, true/*sizeAdjust*/, true/*adjustForEncumbrance*/, adjustForHolds);
      }
      return -1;
   }

   private WeaponStyleAttack getWeaponStyle(LimbType limb, int attackStyle, boolean isGrapple, boolean isCounterAttack) {
      if (limbs.containsKey(limb)) {
         Weapon weap = limbs.get(limb).getWeapon(this);
         if (weap != null) {
            if (isGrapple) {
               if (weap.grapplingStyles.length > attackStyle) {
                  return weap.grapplingStyles[attackStyle];
               }
            } else if (isCounterAttack) {
               if (weap.counterattackStyles.length > attackStyle) {
                  return weap.counterattackStyles[attackStyle];
               }
            } else if (weap.attackStyles.length > attackStyle) {
               return weap.attackStyles[attackStyle];
            }
         }
      }
      return null;
   }

   public SkillType getBestSkillType(Weapon weapon) {
      SkillType bestRangedSkill = null;
      SkillType bestMeleeSkill = null;
      byte bestRangedSkillLevel = -1;
      byte bestMeleeSkillLevel = -1;
      byte bestMeleeSkillAttackSpeed = -1;
      byte bestMeleeSkillAttackDamage = -1;
      for (int i = 0; i < weapon.attackStyles.length; i++) {
         WeaponStyleAttack attackMode = weapon.getAttackStyle(i);
         SkillType skillType = attackMode.getSkillType();
         Profession prof = getProfession(skillType);
         if (prof == null) {
            continue;
         }
         byte skillLevel = prof.getLevel(skillType);
         if (attackMode.isRanged()) {
            if ((bestRangedSkill == null) || (bestRangedSkillLevel < skillLevel)) {
               bestRangedSkill = skillType;
               bestRangedSkillLevel = skillLevel;
            }
         }
         else {
            byte speed = attackMode.getSpeed(getAttributeLevel(Attribute.Strength));
            byte damage = attackMode.getDamage((byte) 0);

            boolean useSkill = (bestMeleeSkill == null) || (bestMeleeSkillLevel < skillLevel);
            if (!useSkill && (bestMeleeSkillLevel == skillLevel)) {
               if ((bestMeleeSkillAttackSpeed >= speed) && (bestMeleeSkillAttackDamage < damage)) {
                  // new skill is same speed or faster, and does more damage
                  useSkill = true;
               }
               else if (bestMeleeSkillAttackSpeed > speed) {
                  // new skill is faster, but does less damage
                  if (bestMeleeSkillAttackDamage - damage < 5) {
                     // new skill is faster but doesn't do too much less damage
                     useSkill = true;
                  }
               }
            }
            if (useSkill) {
               bestMeleeSkill = skillType;
               bestMeleeSkillLevel = skillLevel;
               bestMeleeSkillAttackSpeed = speed;
               bestMeleeSkillAttackDamage = damage;
            }
         }
      }
      if (bestMeleeSkill != null) {
         return bestMeleeSkill;
      }
      return bestRangedSkill;
   }

   public byte getBestSkillLevel(Weapon weapon) {
      byte best = 0;
      for (int i = 0; i < weapon.attackStyles.length; i++) {
         WeaponStyleAttack attackMode = weapon.getAttackStyle(i);
         byte testSkill = getSkillLevel(attackMode, false/*adjustForPain*/, null /*ignore use penalty*/, true/*sizeAdjust*/,
                                        true/*adjustForEncumbrance*/, true/*adjustForHolds*/);
         if (best < testSkill) {
            best = testSkill;
         }
      }
      return best;
   }
   public SkillRank getBestSkillRank(Weapon weapon) {
      SkillRank best = SkillRank.UNKNOWN;
      for (int i = 0; i < weapon.attackStyles.length; i++) {
         SkillRank testSkill = getSkillRank(weapon.getAttackStyle(i).skillType);
         if (best.getCost() < testSkill.getCost()) {
            best = testSkill;
         }
      }
      return best;
   }

   public SkillRank getSkillRank(SkillType skillType) {
      Profession prof = getProfession(skillType);
      if (prof != null) {
         if (prof.getFamiliarSkills().contains(skillType)) {
            return SkillRank.FAMILIAR;
         }
         if (prof.getProficientSkills().contains(skillType)) {
            return SkillRank.PROFICIENT;
         }
      }
      return SkillRank.UNKNOWN;
   }
   public byte getSkillLevel(WeaponStyle attackMode, boolean adjustForPain, LimbType useHand,
                             boolean sizeAdjust, boolean adjustForEncumbrance, boolean adjustForHolds) {
      byte skillLevel = getSkillLevel(attackMode.getSkillType(), useHand, sizeAdjust, adjustForEncumbrance, adjustForHolds);
      if (adjustForPain) {
         if (useHand != null) {
            skillLevel -= limbs.get(useHand).getWoundPenalty();
         }
         skillLevel -= condition.getWoundsAndPainPenalty();
      }
      return skillLevel;
   }

   public byte getAdjustedSkillLevel(WeaponStyle attackMode, boolean adjustForPain, LimbType useHand,
                                     boolean sizeAdjust, boolean adjustForEncumbrance, boolean adjustForHolds) {
      byte skillLevel = getAdjustedSkillLevel(attackMode.getSkillType(), useHand, sizeAdjust, adjustForEncumbrance, adjustForHolds);
      if (adjustForPain) {
         if (useHand != null) {
            skillLevel -= limbs.get(useHand).getWoundPenalty();
         }
         skillLevel -= condition.getWoundsAndPainPenalty();
      }
      return skillLevel;
   }

   private Profession getProfession(SkillType skillType) {
      return professionsList.values()
                            .stream()
                            .filter(p -> p.getType().skillList.contains(skillType))
                            .max(Comparator.comparingInt(o -> o.getLevel(skillType)))
                            .orElse(null);
   }

   public byte getSkillLevel(SkillType skillType, LimbType useLimb, boolean sizeAdjust, boolean adjustForEncumbrance, boolean adjustForHolds) {
      Profession prof = getProfession(skillType);
      if (prof == null) {
         return 0;
      }
      byte skillLevel = prof.getLevel(skillType);
      if (useLimb != null) {
         skillLevel -= getHandPenalties(useLimb, skillType);
      }
      if (sizeAdjust && (skillType.isAdjustedForSize)) {
         skillLevel += race.getBonusToHit();
      }
      if (adjustForEncumbrance && skillType.isAdjustedForEncumbrance) {
         skillLevel -= Rules.getEncumbranceLevel(this);
      }
      if (adjustForHolds) {
         for (Byte hold : heldPenalties.values()) {
            skillLevel -= hold;
         }
      }
      if (skillLevel < 0) {
         return 0;
      }
      return skillLevel;
   }

   public byte getAdjustedSkillLevel(SkillType skillType, LimbType useLimb, boolean sizeAdjust, boolean adjustForEncumbrance, boolean adjustForHolds) {
      Profession prof = getProfession(skillType);
      if (prof == null) {
         return 0;
      }
      byte baseSkillLevel = prof.getLevel(skillType);
      byte skillLevel = Rules.getAdjustedSkillLevel(baseSkillLevel, getAttributeLevel(skillType.getAttributeBase()));
      if (useLimb != null) {
         skillLevel -= getHandPenalties(useLimb, skillType);
      }
      if (sizeAdjust && (skillType.isAdjustedForSize)) {
         skillLevel += race.getBonusToHit();
      }
      if (adjustForEncumbrance && skillType.isAdjustedForEncumbrance) {
         skillLevel -= Rules.getEncumbranceLevel(this);
      }
      if (adjustForHolds) {
         for (Byte hold : heldPenalties.values()) {
            skillLevel -= hold;
         }
      }
      if (skillLevel < 0) {
         return 0;
      }
      return skillLevel;
   }

   public void setSkillLevel(ProfessionType professionType, SkillType skillType, byte skillLevel) {
      Profession prof = professionsList.computeIfAbsent(professionType,
                                                        o -> new Profession(professionType, skillType, skillLevel));
      List<SkillType> proficientSkills = prof.getProficientSkills();
      List<SkillType> familiarSkills = prof.getFamiliarSkills();
      List<SkillType> newProficientSkills = new ArrayList<>(proficientSkills);
      List<SkillType> newFamiliarSkills = new ArrayList<>(familiarSkills);
      if ((prof.getLevel() > skillLevel + 2) && (prof.getProficientSkills().size() > 1)) {
         // This can be a familiar Skill within the existing profession
         if (!familiarSkills.contains(skillType)) {
            newFamiliarSkills.add(skillType);
         }
         newProficientSkills.remove(skillType);
      } else {
         prof.setLevel(skillLevel);
         if (!proficientSkills.contains(skillType)) {
            newProficientSkills.add(skillType);
         }
         newFamiliarSkills.remove(skillType);
      }
      prof.setFamiliarSkills(newFamiliarSkills);
      prof.setProficientSkills(newProficientSkills);
   }

   public void setProfessionLevel(ProfessionType professionType, SkillType defaultProfSkillType, byte profLevel) {
      professionsList.computeIfAbsent(professionType,
                                      o -> new Profession(professionType, defaultProfSkillType, profLevel))
                     .setLevel(profLevel);
   }

   public byte getProfessionLevel(ProfessionType professionType) {
      Profession profession = professionsList.get(professionType);
      return profession == null ? -1 : profession.getLevel();
   }

   public byte getSpellLevel(String spellName) {
      Profession spellCasting = professionsList.get(ProfessionType.Spellcasting);
      if (spellCasting != null) {
         for (MageSpell spell : knownMageSpellsList) {
            if (spell.getName().equals(spellName)) {
               Byte level = null;
               for (SkillType skillType : spell.prerequisiteSkillTypes) {
                  byte preReqLevel = spellCasting.getLevel(skillType);
                  if (level == null || level < preReqLevel) {
                     level = preReqLevel;
                  }
               }
               return (level == null) ? -1 : level.byteValue();
            }
         }
      }
      return -1;
   }

   public byte getSpellSkill(String spellName) {
      byte mageSkill = 0;
      byte inateSkill = 0;
      for (MageSpell spell : knownMageSpellsList) {
         if (spell.getName().equals(spellName)) {
            mageSkill = spell.getEffectiveSkill(this, true);
            break;
         }
      }
      List<Spell> inateSpells = race.getInateSpells();
      if ((inateSpells != null) && (inateSpells.size() > 0)) {
         Profession commonProfession = professionsList.get(ProfessionType.Common);
         if (commonProfession != null) {
            for (Spell spell : inateSpells) {
               if (spell.getName().equals(spellName)) {
                  inateSkill = commonProfession.getLevel();
                  break;
               }
            }
         }
      }
      return (byte) Math.max(mageSkill, inateSkill);
   }

   public void setSpellLevel(String spellName, byte spellLevel, MageSpell.Familiarity familiarity) {
      MageSpell spellFound = null;
      for (MageSpell spell : knownMageSpellsList) {
         if (spell.getName().equals(spellName)) {
            spellFound = spell;
            break;
         }
      }
      if (spellFound == null) {
         spellFound = MageSpells.getSpell(spellName);
         knownMageSpellsList.add(spellFound);
      }
      spellFound.setFamiliarity(familiarity);
      MageSpell finalSpellFound = spellFound;
      professionsList.computeIfAbsent(ProfessionType.Spellcasting,
                                      o-> new Profession(ProfessionType.Spellcasting,
                                                         Arrays.asList(finalSpellFound.prerequisiteSkillTypes),
                                                         spellLevel));
   }

   public byte getBuildBase() {
      return buildBase;
   }

   public byte getAlertness(boolean visionBased, boolean hearingBased) {
      byte iq = getAttributeLevel(Attribute.Intelligence);
      Advantage alertness = getAdvantage(Advantage.ALERTNESS);
      if (alertness != null) {
         switch (alertness.getLevelName()) {
            case "Oblivious":            iq -=4; break;
            case "Unaware":              iq -=2; break;
            //case "Normal":               iq +=0; break;
            case "Alert":                iq +=2; break;
            case "Very Alert":           iq +=4; break;
            case "Exceptionally Alert":  iq +=6; break;
         }
      }
      if (getRace().isAnimal()) {
         iq += 6;
      }
      if (visionBased) {
         Advantage vision = getAdvantage(Advantage.VISION);
         if (vision != null) {
            switch (vision.getLevelName()) {
               case "Blind":           iq -=50; break;
               case "Poor Sight":      iq -=5; break;
               case "Near Sighted":    iq -=5; break;
               case "Far Sighted":     iq -=5; break;
               //case "Normal":          iq +=0; break;
               case "Acute Vision":    iq +=5; break;
            }
         }
         if ((armor != null) && (armor.isReal())) {
            if (armor.getName().contains("Plate")) {
               iq -= 2;
            }
         }
      }
      if (hearingBased) {
         Advantage hearing = getAdvantage(Advantage.HEARING);
         if (hearing != null) {
            switch (hearing.getLevelName()) {
               case "Deaf":          iq -=50; break;
               case "Poor Hearing":  iq -=5; break;
               //case "Normal":        iq +=0; break;
               case "Acute Hearing": iq +=5; break;
            }
         }
      }
      return iq;
   }

   public byte getBuild(DamageType damType) {
      byte build = (byte) (armor.getBarrier(damType) + buildBase);
      if (!armor.equals(race.getNaturalArmor())) {
         build += race.getNaturalArmorBarrier(damType);
      }
      return build;
   }

   public boolean stillFighting() {
      return ((condition.isConscious()) && (condition.isAlive()));
   }

   public RequestAction getActionRequest(Arena arena, Character delayedTarget, List<Character> charactersTargetingActor) {
      boolean mustAdvance = false;
      Character target = arena.getCharacter(targetID);
      if (delayedTarget != null) {
         target = delayedTarget;
         aimDuration = 0;
      } else {
         boolean canAdvance = canAdvance();
         if ((target == null) || !target.stillFighting() || (!arena.canAttack(this, target, canAdvance, canAdvance/*withCharge*/))) {
            // Do we have a different target we can attack without advancing?
            Character oldTarget = target;
            target = arena.getBestTarget(this, false);
            // If not, do we get one when we advance?
            if ((target == null) && canAdvance) {
               target = arena.getBestTarget(this, true);
               mustAdvance = (target != null);
            }
            if (target != oldTarget) {
               if ((target == null) || (oldTarget == null) || (target.uniqueID != oldTarget.uniqueID)) {
                  aimDuration = 0;
               }
            }
         } else {
            if (canAdvance) {
               if (!arena.canAttack(this, target, false, false/*withCharge*/)) {
                  mustAdvance = true;
               }
            }
         }
      }

      boolean isPacified = false;
      boolean isParalyzed = false;
      boolean isSwimming = false;
      for (Spell spell : activeSpellsList) {
         if (spell instanceof SpellPacify) {
            isPacified = true;
         }
         if (spell instanceof SpellParalyze) {
            if (!((SpellParalyze) spell).allowsAttack()) {
               isParalyzed = true;
            }
         }
      }
      short minDistanceToTarget = -100;
      short maxDistanceToTarget = -100;
      if (target != null) {
         minDistanceToTarget = Arena.getMinDistance(this, target);
         maxDistanceToTarget = Arena.getMaxDistance(this, target);
      }
      RequestAction req = null;
      IRequestOption defaultOption = null;
      if (stillFighting()) {
         req = new RequestAction(uniqueID, (target == null) ? -1 : target.uniqueID);
         req.setSpell(currentSpell);

         StringBuilder sb = new StringBuilder();
         sb.append(getName());
         if ((delayedTarget != null) && (target != null)) {
            sb.append(", your target ").append(target.getName());
            sb.append(" has moved into range.\nYou have ");
         } else {
            sb.append(", you have ");
         }
         sb.append(condition.getActionsAvailable(false/*usedForDefenseOnly*/)).append(" actions remaining");
         if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) == condition.getActionsAvailable(false/*usedForDefenseOnly*/)) {
            sb.append(".");
         } else {
            sb.append(" for this turn, ").append(getActionsAvailableThisRound(false/*usedForDefenseOnly*/));
            sb.append(" can be spent this round.");
         }
         if (currentSpell != null) {
            sb.append("\nYou currently have a ").append(currentSpell.getName()).append(" spell");
            if (currentSpell instanceof MageSpell) {
               sb.append(", with ").append(currentSpell.getPower()).append(" power points");
            }
            sb.append(".");
         }
         if (condition.isCollapsed()) {
            sb.append("\nBecause you have collapsed in pain, you may not stand or attack.");
         } else if (isPacified) {
            sb.append("\nYou are under the effects of a pacify spell, so you may not attack anyone.");
         } else if (isParalyzed) {
            sb.append("\nYou are under the effects of a paralyze spell, so you may not attack anyone.");
         } else {
            byte totalHold = 0;
            Set<IHolder> holders = getHolders();
            if (!holders.isEmpty()) {
               sb.append("\nYou are being held by ");
               boolean first = true;
               for (IHolder holder : holders) {
                  if (!first) {
                     sb.append(", ");
                  }
                  first = false;
                  sb.append(holder.getName()).append(" at level ").append(getHoldLevel(holder));
                  totalHold += getHoldLevel(holder);
               }
               sb.append(" for a total penalty of ").append(totalHold).append(".");
               if (heldPenalties.size() == placedIntoHoldThisTurn.size()) {
                  sb.append("\nBecause you have been placed into ");
                  if (heldPenalties.size() > 1) {
                     sb.append("each of your holds within");
                  } else {
                     sb.append("this hold");
                  }
                  sb.append(" this turn, you may not try to break free until next turn.");
               }
            }
            boolean weaponReady = false;
            for (LimbType limbType : limbs.keySet()) {
               Limb limb = limbs.get(limbType);
               Weapon weap = limb.getWeapon(this);
               boolean showLimbNames = false;
               LimbType pairedLimb = limbType.getPairedType();
               if (limbs.containsKey(pairedLimb)) {
                  Limb otherLimb = limbs.get(pairedLimb);
                  Weapon otherHandWeapon = otherLimb.getWeapon(this);
                  if ((otherHandWeapon != null) && (otherHandWeapon.equals(weap))) {
                     showLimbNames = true;
                  }
               }
               if (limb.canAttack(this)) {
                  weaponReady = true;
               } else {
                  if (limb.getActionsNeededToReady() != 0) {
                     if (weap != null) {
                        sb.append(" Your ").append(weap.getName());
                        if (showLimbNames) {
                           sb.append(" (").append(limb.getName()).append(")");
                        }
                        sb.append(" requires ");
                        int actions = limb.getActionsNeededToReady();
                        if (actions == 1) {
                           sb.append("1 action");
                        } else {
                           sb.append(actions).append(" actions");
                        }
                        sb.append(" to re-ready before you may attack.");
                     }
                  }
                  // else arm crippled, or no weapon.
               }
            }
            boolean showPain = false;
            if (!weaponReady) {
               if ((currentSpell != null)
                   && (delayedTarget == null)
                   && ((currentSpell.getTargetType() == TargetType.TARGET_OTHER_FIGHTING)
                       || (currentSpell.getTargetType() == TargetType.TARGET_ANIMAL_FIGHTING)
                       || (currentSpell.getTargetType() == TargetType.TARGET_OTHER_EVIL_FIGHTING)
                       || (currentSpell.getTargetType() == TargetType.TARGET_OTHER_GOOD_FIGHTING)
                       || (currentSpell.getTargetType() == TargetType.TARGET_UNDEAD))) {
                  if (target == null) {
                     sb.append("\nAll of your selected targets are currently out of range, or not visible this round.");
                  } else {
                     showPain = true;
                  }
               } else {
                  sb.append("\nYour weapon is not ready to attack this round.");
               }
            } else if (target == null) {
               sb.append("\nAll of your selected targets are currently out of range, or not visible this round.");
            } else {
               showPain = true;
            }
            if (showPain) {
               if ((delayedTarget == null) && (target != null)) {
                  sb.append("\nYour currently selected target is ").append(target.getName()).append(".");
               }
               int limbsWithWeaponCount = 0;
               Limb singleWeaponHand = null;
               for (Limb limb : limbs.values()) {
                  if (limb.getWeapon(this) != null) {
                     limbsWithWeaponCount++;
                     singleWeaponHand = limb;
                  }
               }
               if ((limbsWithWeaponCount == 0) && (currentSpell != null)) {
                  sb.append(" \nYou have no weapon with which to attack.");
               } else {
                  int messageSizeBeforePenalties = sb.length();
                  byte painPenalty = getCondition().getPenaltyPain();
                  if ((limbsWithWeaponCount == 1) && (currentSpell == null)) {
                     byte attackPenalty = getPenaltyToUseArm(singleWeaponHand, true/*includeWounds*/, true/*includePain*/);
                     if (attackPenalty > 0) {
                        if (isBerserking() && (painPenalty > 0)) {
                           sb.append("\nBecause you are Berserking, you are unaffected by your ");
                           sb.append(painPenalty).append(" points of pain.");
                           attackPenalty -= painPenalty;
                        }
                        sb.append(" \nYou may attack at a -").append(attackPenalty).append(" due to ");
                        List<String> penalties = new ArrayList<>();
                        if ((condition.getPenaltyPain() > 0) && !isBerserking()) {
                           penalties.add(String.valueOf(condition.getPenaltyPain()) + " points of pain");
                        }
                        if (condition.getWounds() > 0) {
                           penalties.add(String.valueOf(condition.getWounds()) + " wounds");
                        }
                        if (limbsWithWeaponCount == 1) {
                           byte armPenalty = getPenaltyToUseArm(singleWeaponHand, false/*includeWounds*/, false/*includePain*/);
                           if (armPenalty > 0) {
                              penalties.add(String.valueOf(armPenalty) + " arm-use penalty");
                           }

                           while (penalties.size() > 0) {
                              sb.append(penalties.remove(0));
                              if (penalties.size() > 1) {
                                 sb.append(", ");
                              } else if (penalties.size() == 1) {
                                 sb.append(" and ");
                              }
                           }
                        }
                        sb.append(".");
                     }
                  }
                  if ((limbsWithWeaponCount > 1) || (currentSpell != null)) {
                     byte baseAttackPenalty = condition.getWoundsAndPainPenalty();
                     if (baseAttackPenalty > 0) {
                        if (isBerserking() && (painPenalty > 0)) {
                           sb.append("\nBecause you are Berserking, you are unaffected by your ");
                           sb.append(painPenalty).append(" points of pain.");
                           baseAttackPenalty -= painPenalty;
                        }
                        sb.append(" \nDue to ");
                        if ((condition.getPenaltyPain() > 0) && !isBerserking()) {
                           sb.append(condition.getPenaltyPain()).append(" points of pain");
                           if (condition.getWounds() > 0) {
                              sb.append(" and ");
                           }
                        }
                        if (condition.getWounds() > 0) {
                           sb.append(condition.getWounds()).append(" wounds");
                        }
                        if (limbsWithWeaponCount == 0) {
                           sb.append(", all spells ");
                        } else {
                           sb.append(", all attacks ");
                           if (currentSpell != null) {
                              sb.append("and spells ");
                           }
                        }
                        sb.append("will be at a -").append(baseAttackPenalty);
                        sb.append(".");
                     }
                     for (Limb limb : limbs.values()) {
                        Weapon weap = limb.getWeapon(this);
                        if (weap != null) {
                           if (limb.canAttack(this)) {
                              Limb otherLimb = limbs.get(limb.limbType.getPairedType());
                              if (otherLimb != null) {
                                 Weapon otherWeapon = otherLimb.getWeapon(this);
                                 if ((otherWeapon != null) && (otherWeapon.isOnlyTwoHanded())) {
                                    break;
                                 }
                              }
                              byte armPenalty = getPenaltyToUseArm(limb, false/*includeWounds*/, false/*includePain*/);
                              if (armPenalty > 0) {
                                 sb.append(" Attacks with your ").append(weap.getName());
                                 if (!weap.isOnlyTwoHanded()) {
                                    // When a combatant has a left-arm wound, using a two-handed weapon,
                                    // the armPenalty will include this penalty. In this case we don't
                                    // want to incorrectly report the wrong limb having a wound:
                                    sb.append(" (").append(limb.getName()).append(")");
                                 }
                                 sb.append(" are at a");
                                 if ((condition.getPenaltyPain() > 0) || (condition.getWounds() > 0)) {
                                    sb.append("n additional");
                                 }
                                 sb.append(" -").append(armPenalty);
                                 if (limb.getWoundPenalty() > 0) {
                                    sb.append(" due to arm-use penalties");
                                 }
                                 sb.append(".");
                              }
                           }
                        }
                     }
                     if (getHolders().size() > 0) {
                        sb.append("You are being held by ");
                        int totalPenalty = 0;
                        for (IHolder holder : getHolders()) {
                           Byte hold = getHoldLevel(holder);
                           totalPenalty += hold;
                           sb.append(holder.getName()).append(" (level-").append(hold).append(" hold), ");
                        }
                        sb.append(" so all your attacks are at -").append(totalPenalty);
                     }
                  }
                  byte positionAdjustment = getPositionAdjustmentForAttack();
                  if (positionAdjustment != 0) {
                     boolean otherPenaltiesReported = (messageSizeBeforePenalties != sb.length());
                     sb.append("\nBecause you are ").append(getPositionName());
                     if (positionAdjustment <= -25) {
                        sb.append(", you may not attack.");
                     } else {
                        sb.append(", your attacks are at ");
                        if (otherPenaltiesReported) {
                           sb.append("an additional ");
                        }
                        sb.append(positionAdjustment).append(".");
                     }
                  }
                  StringBuilder terrainExplanation = new StringBuilder();
                  byte terrainAdjustment = getTerrainAdjustmentForAttack(terrainExplanation, arena.getCombatMap());
                  if (terrainAdjustment != 0) {
                     boolean otherPenaltiesReported = (messageSizeBeforePenalties != sb.length());
                     sb.append("\nBecause you are on ").append(terrainExplanation);
                     if (terrainAdjustment <= -25) {
                        sb.append(", you may not attack.");
                        isSwimming = true;
                     } else {
                        sb.append(", your attacks are at ");
                        if (otherPenaltiesReported) {
                           sb.append("an additional ");
                        }
                        sb.append(terrainAdjustment).append(".");
                     }
                  }

                  // Adjustments for size are now handled in the skill and Passive Defense values directly.
                  //                  int sizeDiff = getRace().getBonusToHit() + target.getRace().getBonusToBeHit();
                  //                  if (sizeDiff != 0) {
                  //                     sb.append("\nAttacks against ").append(target.getName()).append(" are made at ");
                  //                     if (sizeDiff>0) sb.append("+");
                  //                     sb.append(sizeDiff).append(" because ");
                  //                     if (target.getRace().getBonusToHit() != 0) {
                  //                        sb.append("he is a ").append(target.getRace().getName());
                  //                        if (getRace().getBonusToHit() != 0)
                  //                           sb.append(", while ");
                  //                     }
                  //                     if (getRace().getBonusToHit() != 0)
                  //                        sb.append("you are a ").append(getRace().getName());
                  //                  }
               }
            }
         }
         boolean spellIsInRange = true;
         boolean hasLineOfSightToTarget = false;
         short spellPowerPenalty = 0;
         if (target != null) {
            hasLineOfSightToTarget = arena.hasLineOfSight(this, target);
            // is this a ranged attack?
            Weapon rangedWeapon = null;
            WeaponStyleAttackRanged rangedStyle = null;
            for (Limb limb : limbs.values()) {
               Weapon weap = limb.getWeapon(this);
               if (weap != null) {
                  if (weap.isMissileWeapon()) {
                     rangedWeapon = weap;
                     rangedStyle = (WeaponStyleAttackRanged) weap.getAttackStyle(0);
                     break;
                  }
                  if (weap.isThrowable()) {
                     rangedWeapon = weap;
                     rangedStyle = weap.getThrownAttackStyle();
                     break;
                  }
               }
            }
            boolean rangeShown = false;
            if ((rangedWeapon != null) || ((currentSpell != null) && (currentSpell instanceof IRangedSpell))) {
               RANGE range = RANGE.OUT_OF_RANGE;
               short rangeBase = -1;
               short adjustedRangeBase = -1;
               String rangeAdjustingAttributeName = "";
               if (rangedStyle != null) {
                  range = rangedStyle.getRangeForDistance(minDistanceToTarget, getAdjustedStrength());
                  rangeBase = rangedStyle.getRangeBase();
                  adjustedRangeBase = (short) Math.round(rangeBase * Rules.getRangeAdjusterForAdjustedStr(getAdjustedStrength()));
                  rangeAdjustingAttributeName = Attribute.Strength.shortName;
               } else if (currentSpell != null) {
                  range = currentSpell.getRange(minDistanceToTarget);
                  rangeBase = ((IRangedSpell) currentSpell).getRangeBase();
                  adjustedRangeBase = currentSpell.getAdjustedRangeBase();
                  Attribute attr = currentSpell.getCastingAttribute();
                  rangeAdjustingAttributeName = attr.shortName;
               }
               if (!hasLineOfSightToTarget) {
                  sb.append("\nYou do not have a clear line of site to ").append(target.getName()).append(".");
               } else {
                  if (aimDuration > 0) {
                     sb.append("\nYou have been aiming at ").append(target.getName());
                     sb.append(" for ").append(aimDuration).append(" rounds. ");
                     sb.append("\nYour target is ");
                  } else {
                     sb.append("\nYour target (").append(target.getName()).append(") is ");
                  }
                  sb.append(minDistanceToTarget);
                  if (minDistanceToTarget != maxDistanceToTarget) {
                     sb.append(" to ").append(maxDistanceToTarget);
                  }
                  sb.append(" hexes away.");
                  rangeShown = true;
                  if (rangedWeapon != null) {
                     sb.append("\nYour ").append(rangedWeapon.getName());
                  } else {
                     sb.append("\nYour ").append(currentSpell.getName()).append(" spell");
                  }
                  sb.append(" has a range base of ").append(rangeBase);
                  if (adjustedRangeBase != rangeBase) {
                     sb.append(" (").append(rangeAdjustingAttributeName).append(" and size adjusted to ").append(adjustedRangeBase).append(")");
                  }
                  sb.append(", making this ");
                  if ((range == RANGE.OUT_OF_RANGE) && (maxDistanceToTarget < 2)) {
                     sb.append(" too close");
                  } else {
                     sb.append(range.getName()).append(" range");
                  }
                  byte rangePenalty = (byte) (-Rules.getRangeDefenseAdjustmentToPD(range));
                  if (rangePenalty > 0) {
                     sb.append(", for a bonus of +").append(rangePenalty).append(" to hit.");
                  } else if (rangePenalty < 0) {
                     sb.append(", for a penalty of ").append(rangePenalty).append(" to hit.");
                  } else if (range != RANGE.OUT_OF_RANGE) {
                     sb.append(" (+0 to hit).");
                  } else {
                     sb.append(".");
                  }
                  if (range != RANGE.OUT_OF_RANGE) {
                     if (target.hasMovedLastAction()) {
                        byte movingTNBonus = Rules.getTNBonusForMovement(range, target.isMovingEvasively());
                        sb.append("\nYour target is moving");
                        if (target.isMovingEvasively()) {
                           sb.append(" evasively");
                        }
                        sb.append(", so it's TN will be at +").append(movingTNBonus);
                     }
                  } else {
                     if (currentSpell != null) {
                        spellIsInRange = false;
                     }
                  }
               }
            }
            if (currentSpell != null) {
               if (currentSpell.requiresTargetToCast() && !currentSpell.isBeneficial()) {
                  byte rangePenalty = currentSpell.getRangeTNAdjustment(minDistanceToTarget);
                  if ((rangePenalty != 0) || (currentSpell instanceof PriestSpell)) {
                     if (!rangeShown) {
                        sb.append("\nYour target is ");
                        sb.append(minDistanceToTarget);
                        sb.append(" hexes away.");
                     }
                     sb.append("\nYour ").append(currentSpell.getName()).append(" spell");
                     if (rangePenalty != 0) {
                        sb.append(" will be cast at a penalty of ");
                        sb.append(rangePenalty).append(".");
                     }
                     if (currentSpell instanceof PriestSpell) {
                        PriestSpell priestSpell = (PriestSpell) currentSpell;
                        RANGE range = currentSpell.getRange(minDistanceToTarget);
                        spellPowerPenalty = priestSpell.getPowerReductionForRange(minDistanceToTarget, range);
                        spellPowerPenalty += getPainPenalty(true/*accountForBerserking*/);
                        sb.append(" will have a power penalty of ");
                        sb.append(spellPowerPenalty).append(".");
                     }
                  }
               }
            }
         }
         if (currentSpell instanceof PriestSpell) {
            byte spellPowerPenaltyForPain = getPainPenalty(true/*accountForBerserking*/);
            if (spellPowerPenaltyForPain != 0) {
               sb.append("\nBecause of your pain, your ").append(currentSpell.getName()).append(" spell");
               sb.append(" will have a");
               if (spellPowerPenalty == 0) {
                  sb.append("n additional");
               }
               sb.append(" power penalty of ");
               sb.append(spellPowerPenaltyForPain);
               spellPowerPenalty += spellPowerPenaltyForPain;
               if (spellPowerPenalty == 0) {
                  sb.append(", for a total power penalty of ").append(spellPowerPenalty);
               }
               sb.append(".");
            }
         }

         if ((charactersTargetingActor != null) && (charactersTargetingActor.size() > 0)) {
            sb.append(YOU_ARE_BEING_TARGETED_BY);
            for (int i = 0; i < charactersTargetingActor.size(); i++) {
               Character targeter = charactersTargetingActor.get(i);
               sb.append(targeter.getName()).append(" (");
               int aimDuration = targeter.getAimDuration(uniqueID);
               if (aimDuration != 1) {
                  sb.append(aimDuration).append(" rounds)");
               } else {
                  sb.append(aimDuration).append(" round)");
               }
               if ((i + 1) < charactersTargetingActor.size()) {
                  if ((i + 2) == charactersTargetingActor.size()) {
                     sb.append(" and ");
                  } else {
                     sb.append(", ");
                  }
               }
            }
         }
         if (getPosition() != Position.STANDING) {
            sb.append(" \nYou are currently ").append(getPositionName());
            if (condition.getPenaltyMove() < 0) {
               sb.append(", due to a crippling leg wound, so you are unable to stand.");
            } else {
               //               if (getPosition() != condition.getMovingToPosition()) {
               //                  sb.append(", and are currently moving to ").append(POSITION_NAME[getMovingToPosition()]);
               //                  sb.append(". You still need to spend ").append(getActionsNeededToChangePosition());
               //                  sb.append(" action to complete the position change");
               //               }
               sb.append(".");
               if (getPosition() == Position.PRONE_BACK) {
                  sb.append(" Before you can stand-up, you must first sit up, then stand.");
               }
               if (getPosition() == Position.PRONE_FRONT) {
                  sb.append(" Before you can stand-up, you must first kneel, then stand.");
               }
            }
         }
         if (isBerserking()) {
            sb.append("\nBecause you are beserking, you are not able to choose the defend, prepare missile weapons, change targets, or move evasively.");
         }
         sb.append("\nWhat would you like to do this round?");
         req.setMessage(sb.toString());
         boolean weaponReady = false;
         boolean attackOptAvailable = false;
         StringBuilder sbReasons = new StringBuilder();
         boolean isBeingHeld = (heldPenalties.size() > 0);
         int availActions = condition.getAvailableActions(sbReasons, isBeingHeld);
         //         if (condition.isConscious() && !_condition.isCollapsed()) {
         //            if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) >0) {
         //               availActions |= orientation.getAvailablePositions();
         //            }
         //         }
         if (heldPenalties.size() > placedIntoHoldThisTurn.size()) {
            boolean enabled = !getCondition().isCollapsed();
            switch (getActionsAvailableThisRound(false/*usedForDefenseOnly*/)) {
               case 7:
               case 6:
               case 5:
               case 4:
               case 3:
                  req.addOption(new RequestActionOption("Break free (3-actions)", RequestActionType.OPT_BREAK_FREE_3, LimbType.BODY, enabled));
                  //req.addOption(RequestAction.OPT_BREAK_FREE_3, "Break free (3-actions)", enabled);
               case 2:
                  req.addOption(new RequestActionOption("Break free (2-actions)", RequestActionType.OPT_BREAK_FREE_2, LimbType.BODY, enabled));
                  //req.addOption(RequestAction.OPT_BREAK_FREE_2, "Break free (2-actions)", enabled);
               case 1:
                  req.addOption(new RequestActionOption("Break free (1-action)", RequestActionType.OPT_BREAK_FREE_1, LimbType.BODY, enabled));
                  //req.addOption(RequestAction.OPT_BREAK_FREE_1, "Break free (1-action)", enabled);
            }
         }
         boolean columnSpacerAdded = false;
         int maxEntriesPerColumn = 6;

         boolean isFacingTarget = (target != null) && (arena.getCombatMap().isFacing(this, target));

         boolean grappleAttackListed = false;
         boolean legAttackListed = false;
         boolean legAdvanceAttackListed = false;
         List<Orientation> advOrients = condition.getPossibleAdvanceOrientations(arena.getCombatMap());
         HashMap<Orientation, List<Orientation>> mapOrientationToNextOrientationsLeadingToChargeAttack = new HashMap<>();
         if (target != null) {
            if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) > 2) {
               getOrientation().getPossibleChargePathsToTarget(arena.getCombatMap(), this, target,
                                                               getAvailableMovement(false/*movingEvasively*/),
                                                               mapOrientationToNextOrientationsLeadingToChargeAttack);
            }
         }
         // Sort the list for the order to show in the options list
         List<Limb> sortedLimbs = limbs.values().stream()
                                       .sorted((limbA, limbB) -> {
                                          LimbType limbAtype = limbA.limbType;
                                          LimbType limbBtype = limbB.limbType;
                                          if (limbA == limbB) {
                                             return 0;
                                          }
                                          if ((limbAtype.isHead() && !limbBtype.isHead()) ||
                                              (limbAtype.isHand() && !limbBtype.isHand()) ||
                                              (limbAtype.isWing() && !limbBtype.isWing()) ||
                                              (limbAtype.isBody() && !limbBtype.isBody()) ||
                                              (limbAtype.isTail() && !limbBtype.isTail()) ||
                                              (limbAtype.isLeg() && !limbBtype.isLeg())) {
                                             return -1;
                                          }
                                          if (((limbAtype.side == Side.RIGHT) && (limbBtype.side != Side.RIGHT))) {
                                             return -1;
                                          }
                                          return Integer.compare(limbAtype.setId, limbBtype.setId);
                                       }).collect(Collectors.toList());
         for (Limb limb : sortedLimbs) {
            // adjust the listed distance of each of our limb locations
            if (target != null) {
               ArenaLocation limbLoc = getLimbLocation(limb.limbType, arena.getCombatMap());
               minDistanceToTarget = Arena.getShortestDistance(limbLoc, target.getOrientation());
               maxDistanceToTarget = Arena.getFarthestDistance(limbLoc, target.getOrientation());
            }
            Weapon weap = limb.getWeapon(this);
            if (weap != null) {
               String handName = "";
               if (limb instanceof Hand) {
                  boolean showHandNames = false;
                  LimbType otherLimbType = limb.limbType.getPairedType();
                  Limb pairedLimb = limbs.get(otherLimbType);
                  if ((limb != pairedLimb) && (pairedLimb != null)) {
                     Weapon otherHandWeap = pairedLimb.getWeapon(this);
                     if ((otherHandWeap != null) && (otherHandWeap.getName().equals(weap.getName()))) {
                        showHandNames = true;
                     }
                  }
                  if (showHandNames) {
                     handName = " (" + limb.getName() + ") ";
                  }
               }
               String weapName = weap.getName() + handName;
               // for unarmed combat, use the style name instead of the weapon name.
               if (!weap.isReal()) {
                  List<String> attackStyleNames = new ArrayList<>();
                  for (WeaponStyleAttack style : weap.getAttackStyles()) {
                     if (style.canAttack(this, weap, limb)) {
                        String styleName = style.getName();
                        if (!attackStyleNames.contains(styleName)) {
                           attackStyleNames.add(styleName);
                        }
                     }
                  }
                  if (attackStyleNames.size() > 0) {
                     StringBuilder styleNameBuffer = new StringBuilder();
                     styleNameBuffer.append(attackStyleNames.remove(0));
                     while (attackStyleNames.size() > 0) {
                        styleNameBuffer.append("/").append(attackStyleNames.remove(0));
                     }
                     styleNameBuffer.append(handName);
                     weapName = styleNameBuffer.toString();
                  }
               }

               //if (isFacingTarget) {
               //   minDistanceToTarget *= -1;
               //   maxDistanceToTarget *= -1;
               //}
               boolean inMeleeRange = (minDistanceToTarget <= weap.getWeaponMaxRange(false/*allowRanged*/, false/*onlyChargeTypes*/, this))
                                      && (maxDistanceToTarget >= weap.getWeaponMinRange(false/*allowRanged*/, false/*onlyChargeTypes*/, this));

               WeaponStyleAttackRanged rangedStyle = weap.getRangedStyle();
               boolean inRange;
               boolean canPrepare = (limb instanceof Hand) && ((Hand) limb).canPrepare();
               if (canPrepare) {
                  if (limb.isCrippled()) {
                     canPrepare = false;
                  } else {
                     if (getCondition().isCollapsed()) {
                        canPrepare = false;
                     } else if (rangedStyle.getHandsRequired() == 2) {
                        Limb otherHand = limbs.get(limb.limbType.getPairedType());
                        if (otherHand.isCrippled()) {
                           canPrepare = false;
                        }
                     }
                  }
               }
               boolean rangedAllowed = weap.isMissileWeapon();
               if (!rangedAllowed) {
                  // If we can prepare this attack then we can't throw it yet.
                  rangedAllowed = (rangedStyle != null) && !canPrepare;
               }
               inRange = weap.isWeaponInRange(minDistanceToTarget, maxDistanceToTarget, rangedAllowed, false/*onlyChargeTypes*/, this);
               if ((rangedStyle != null) && canPrepare) {
                  req.addOption(new RequestActionOption(rangedStyle.getPreparationStepName(weapName, ((Hand) limb).getPreparedState() - 1),
                                                        RequestActionType.OPT_PREPARE_RANGED, limb.limbType,
                                                        !isBerserking() && !isPacified && !isParalyzed && !isSwimming));
               }
               if (limb.canAttack(this)) {
                  weaponReady = true;
                  if (((availActions & ACTION_ATTACK) != 0) && (target != null)) {
                     if (rangedStyle != null) {
                        // ranged attack require the attacker to face the defender:
                        if (isFacingTarget) {
                           if (inRange && !canPrepare) {
                              RequestActionOption targetOpt = new RequestActionOption("aim " + weapName + " at " + target.getName(),
                                                                                      RequestActionType.OPT_TARGET_ENEMY, limb.limbType,
                                                                                      !isPacified && !isParalyzed && !isSwimming);
                              req.addOption(targetOpt);
                              if (aimDuration > 0) {
                                 if (weap.isMissileWeapon()) {
                                    RequestActionOption option = new RequestActionOption("fire " + weapName + " at " + target.getName(),
                                                                                         RequestActionType.OPT_ATTACK_MISSILE,
                                                                                         limb.limbType,
                                                                                         !isPacified && !isParalyzed && !isSwimming);

                                    req.addOption(option);
                                    if (aimDuration >= 3) {
                                       defaultOption = option;
                                    }
                                 } else {
                                    String attackName = "throw " + weapName + " at " + target.getName();
                                    byte actionsAvailableToAttack = (byte) Math.min(getActionsAvailableThisRound(false/*usedForDefenseOnly*/),
                                                                                    rangedStyle.getMaxAttackActions());
                                    RequestActionOption defOption = null;
                                    RequestActionOption option;
                                    switch (actionsAvailableToAttack) {
                                       case 5:
                                       case 4:
                                       case 3:
                                          option = new RequestActionOption(attackName + " (3-actions)",
                                                                           RequestActionType.OPT_ATTACK_THROW_3, limb.limbType,
                                                                           !isPacified && !isParalyzed && !isSwimming);
                                          req.addOption(option);
                                          defOption = option;
                                       case 2:
                                          option = new RequestActionOption(attackName + " (2-actions)",
                                                                           RequestActionType.OPT_ATTACK_THROW_2, limb.limbType,
                                                                           !isPacified && !isParalyzed && !isSwimming);
                                          req.addOption(option);
                                          if (defOption == null) {
                                             defOption = option;
                                          }
                                       case 1:
                                          option = new RequestActionOption(attackName + " (1-action)",
                                                                           RequestActionType.OPT_ATTACK_THROW_1, limb.limbType,
                                                                           !isPacified && !isParalyzed && !isSwimming);
                                          req.addOption(option);
                                          if (defOption == null) {
                                             defOption = option;
                                          }
                                          attackOptAvailable = true;
                                       case 0:
                                    }
                                    // if we can put all our effort into this attack, make it the default option
                                    if (actionsAvailableToAttack == rangedStyle.getMaxAttackActions()) {
                                       defaultOption = defOption;
                                    }
                                 }
                              }
                              if (defaultOption == null) {
                                 defaultOption = targetOpt;
                              }
                              attackOptAvailable = true;
                           }
                        } // if (getCombatMap().isFacing(this, target))
                     } // if (rangedStyle != null) {
                     if (!weap.isMissileWeapon()) {
                        if (inMeleeRange && !mustAdvance) {
                           if (getOrientation().canLimbAttack(this, target, limb, arena.getCombatMap(), false/*allowRanged*/, false/*onlyChargeTypes*/)) {
                              if (weap.canMeleeAttack()) {
                                 if (limb instanceof Leg) {
                                    if (legAttackListed) {
                                       // only list one knee strike, kick, or other (claw?) attack
                                       continue;
                                    }
                                    legAttackListed = true;
                                 }
                                 switch (getActionsAvailableThisRound(false/*usedForDefenseOnly*/)) {
                                    case 7:
                                    case 6:
                                    case 5:
                                    case 4:
                                    case 3:
                                       req.addOption(new RequestActionOption("attack target (3-actions, " + weapName + ")",
                                                                             RequestActionType.OPT_ATTACK_MELEE_3, limb.limbType,
                                                                             !isPacified && !isParalyzed && !isSwimming));
                                    case 2:
                                       req.addOption(new RequestActionOption("attack target (2-actions, " + weapName + ")",
                                                                             RequestActionType.OPT_ATTACK_MELEE_2, limb.limbType,
                                                                             !isPacified && !isParalyzed && !isSwimming));
                                    case 1:
                                       req.addOption(new RequestActionOption("attack target (1-action, " + weapName + ")",
                                                                             RequestActionType.OPT_ATTACK_MELEE_1, limb.limbType,
                                                                             !isPacified && !isParalyzed && !isSwimming));
                                       attackOptAvailable = true;
                                 }
                              }
                              if (weap.canGrappleAttack(this)) {
                                 if (!grappleAttackListed) {
                                    switch (getActionsAvailableThisRound(false/*usedForDefenseOnly*/)) {
                                       case 7:
                                       case 6:
                                       case 5:
                                       case 4:
                                       case 3:
                                          req.addOption(new RequestActionOption(
                                                  "grab target (3-actions)",
                                                  RequestActionType.OPT_ATTACK_GRAPPLE_3, limb.limbType,
                                                  !isPacified && !isParalyzed && !isSwimming));
                                       case 2:
                                          req.addOption(new RequestActionOption("grab target (2-actions)",
                                                                                RequestActionType.OPT_ATTACK_GRAPPLE_2, limb.limbType,
                                                                                !isPacified && !isParalyzed && !isSwimming));
                                       case 1:
                                          req.addOption(new RequestActionOption("grab target (1-action)",
                                                                                RequestActionType.OPT_ATTACK_GRAPPLE_1, limb.limbType,
                                                                                !isPacified && !isParalyzed && !isSwimming));
                                          attackOptAvailable = true;
                                    }
                                    grappleAttackListed = true;
                                 }
                              }
                           }
                        } else if (((availActions & ACTION_MOVE) != 0) && (delayedTarget == null)) {
                           boolean advanceAllowsAttack = false;
                           boolean advanceIsTurn = false;
                           for (Orientation advOrientation : advOrients) {
                              if (advOrientation.canLimbAttack(this, target, limb, arena.getCombatMap(), false/*allowRanged*/, false/*onlyChargeTypes*/)) {
                                 advanceAllowsAttack = true;
                                 if (advOrientation.getCoordinates().containsAll(condition.getCoordinates())) {
                                    advanceIsTurn = true;
                                    break;
                                 }
                              }
                           }
                           if (mapOrientationToNextOrientationsLeadingToChargeAttack.size() > 0) {
                              List<WeaponStyleAttack> styles = getPossibleChargeAttacksForLimb(limb);
                              boolean distanceValid = false;
                              for (WeaponStyleAttack style : styles) {
                                 //int desiredDistant = style.getMaxRange();

                                 for (List<Orientation> destOrientationList : mapOrientationToNextOrientationsLeadingToChargeAttack.values()) {
                                    if (destOrientationList != null) {
                                       for (Orientation orient : destOrientationList) {
                                          if (orient.canAttack(this, target, arena.getCombatMap(), false/*allowRanged*/, true/*onlyChargeTypes*/)) {
                                             //if (Arena.getDistance(orient.getLimbCoordinates(limb._id), target) == desiredDistant) {
                                             distanceValid = true;
                                             break;
                                          }
                                       }
                                    }
                                    if (distanceValid) {
                                       break;
                                    }
                                 }
                                 if (distanceValid) {
                                    byte actions = getActionsAvailableThisRound(false/*usedForDefenseOnly*/);
                                    if (actions > 2) {
                                       if (actions > 3) {
                                          req.addOption(new RequestActionOption("Charge " + target.getName() + " and attack (4-actions, " + style.getWeapon().getName() + ")",
                                                                                RequestActionType.OPT_CHARGE_ATTACK_3, limb.limbType,
                                                                                !isPacified && !isParalyzed && !isSwimming));
                                       }
                                       req.addOption(new RequestActionOption("Charge " + target.getName() + " and attack (3-actions, " + style.getWeapon().getName() + ")",
                                                                             RequestActionType.OPT_CHARGE_ATTACK_2, limb.limbType,
                                                                             !isPacified && !isParalyzed && !isSwimming));
                                    }
                                    break;
                                 }
                              }
                           }
                           if (advanceAllowsAttack) {
                              // If we are not standing, do not allow an advancing attack.
                              if (advanceIsTurn || (getPosition() == Position.STANDING)) {
                                 String verb = advanceIsTurn ? "turn" : "close";
                                 if (weap.canMeleeAttack()) {
                                    if (limb instanceof Leg) {
                                       if (legAdvanceAttackListed) {
                                          // only list one knee strike, kick, or other (claw?) attack
                                          continue;
                                       }
                                       legAdvanceAttackListed = true;
                                    }


                                    switch (getActionsAvailableThisRound(false/*usedForDefenseOnly*/)) {
                                       case 7:
                                       case 6:
                                       case 5:
                                       case 4:
                                          req.addOption(new RequestActionOption(verb + " on target and attack (3-actions, " + weapName + ")",
                                                                                RequestActionType.OPT_CLOSE_AND_ATTACK_3, limb.limbType,
                                                                                !isPacified && !isParalyzed && !isSwimming));
                                       case 3:
                                          req.addOption(new RequestActionOption(verb + " on target and attack (2-actions, " + weapName + ")",
                                                                                RequestActionType.OPT_CLOSE_AND_ATTACK_2, limb.limbType,
                                                                                !isPacified && !isParalyzed && !isSwimming));
                                       case 2:
                                          req.addOption(new RequestActionOption(verb + " on target and attack (1-action, " + weapName + ")",
                                                                                RequestActionType.OPT_CLOSE_AND_ATTACK_1, limb.limbType,
                                                                                !isPacified && !isParalyzed && !isSwimming));
                                    }
                                 }
                                 if (weap.canGrappleAttack(this)) {
                                    if (!grappleAttackListed) {
                                       switch (getActionsAvailableThisRound(false/*usedForDefenseOnly*/)) {
                                          case 7:
                                          case 6:
                                          case 5:
                                          case 4:
                                             req.addOption(new RequestActionOption(verb + " on target and grab (3-actions)",
                                                                                   RequestActionType.OPT_CLOSE_AND_GRAPPLE_3, limb.limbType,
                                                                                   !isPacified && !isParalyzed && !isSwimming));
                                          case 3:
                                             req.addOption(new RequestActionOption(verb + " on target and grab (2-actions)",
                                                                                   RequestActionType.OPT_CLOSE_AND_GRAPPLE_2, limb.limbType,
                                                                                   !isPacified && !isParalyzed && !isSwimming));
                                          case 2:
                                             req.addOption(new RequestActionOption(verb + " on target and grab (1-action)",
                                                                                   RequestActionType.OPT_CLOSE_AND_GRAPPLE_1, limb.limbType,
                                                                                   !isPacified && !isParalyzed && !isSwimming));
                                       }
                                       grappleAttackListed = true;
                                    }
                                 }
                              }
                           }
                        }
                     } // if (!weap.isMissileWeapon()) {
                  } // if (((availActions & ACTION_ATTACK) != 0) && (target != null)) {
               } // if (limb.canAttack(this)) {
               else {
                  if (!columnSpacerAdded && (req.getActionCount() >= maxEntriesPerColumn)) {
                     req.addSeparatorOption();
                     columnSpacerAdded = true;
                  }
                  int actionsAvailToReady = Math.min(getActionsAvailableThisRound(false/*usedForDefenseOnly*/), limb.getActionsNeededToReady());
                  boolean canBeReadied = limb.canBeReadied(this);
                  switch (actionsAvailToReady) {
                     case 5: req.addOption(new RequestActionOption("ready " + weapName + " (5-actions)",
                                                              RequestActionType.OPT_READY_5, limb.limbType,
                                                              canBeReadied));
                     case 4: req.addOption(new RequestActionOption("ready " + weapName + " (4-actions)",
                                                              RequestActionType.OPT_READY_4, limb.limbType,
                                                              canBeReadied));
                     case 3: req.addOption(new RequestActionOption("ready " + weapName + " (3-actions)",
                                                              RequestActionType.OPT_READY_3, limb.limbType,
                                                              canBeReadied));
                     case 2: req.addOption(new RequestActionOption("ready " + weapName + " (2-actions)",
                                                              RequestActionType.OPT_READY_2, limb.limbType,
                                                              canBeReadied));
                     case 1: req.addOption(new RequestActionOption("ready " + weapName + " (1-action)",
                                                              RequestActionType.OPT_READY_1, limb.limbType,
                                                              canBeReadied));
                  }
               }
            } // if (weap != null) {
            else {// (weap == null)
               Thing heldThing = limb.getHeldThing();
               if (heldThing != null) {
                  if (heldThing.canBeApplied()) {
                     req.addOption(new RequestActionOption(heldThing.getApplicationName() + " (1-action)",
                                                           RequestActionType.OPT_APPLY_ITEM, limb.limbType,
                                                           true));
                  }
               }
            }
         } // for (Limb limb : limbs.values()) {


         if (!columnSpacerAdded && (req.getActionCount() >= maxEntriesPerColumn)) {
            req.addSeparatorOption();
            columnSpacerAdded = true;
         }

         if (delayedTarget == null) {
            if ((availActions & ACTION_MOVE) != 0) {
               RequestActionOption moveOpt;
               if (condition.getPosition() == Position.STANDING) {
                  // Berserkers and animals are not allowed to move evasively
                  boolean evasiveMoveAllowed = !isBerserking() && !race.isAnimal();
                  req.addOption(new RequestActionOption("move evasively", RequestActionType.OPT_MOVE_EVASIVE, LimbType.BODY, evasiveMoveAllowed));
                  //req.addOption(RequestAction.OPT_MOVE_EVASIVE, "move evasively", evasiveMoveAllowed);
                  moveOpt = new RequestActionOption("move", RequestActionType.OPT_MOVE, LimbType.BODY, true);
                  req.addOption(moveOpt);
                  //req.addOption(RequestAction.OPT_MOVE, "move", true);
               } else {
                  moveOpt = new RequestActionOption("crawl", RequestActionType.OPT_MOVE, LimbType.BODY, true);
                  req.addOption(moveOpt);
                  //req.addOption(RequestAction.OPT_MOVE, "crawl", true);
               }
               if (defaultOption == null) {
                  defaultOption = moveOpt;
               }
            }
         }

         if (((availActions & ACTION_ATTACK) != 0) && weaponReady) {
            if (!attackOptAvailable) {
               if (target != null) {
                  req.addOption(new RequestActionOption("wait for opportunity to attack " + target.getName(), RequestActionType.OPT_WAIT_TO_ATTACK, LimbType.BODY, !isBerserking()));
                  //req.addOption(RequestAction.OPT_WAIT_TO_ATTACK, "wait for opportunity to attack " + target.getName(), !isBerserking());
               } else {
                  req.addOption(new RequestActionOption("wait for opportunity to attack any enemy", RequestActionType.OPT_WAIT_TO_ATTACK, LimbType.BODY, !isBerserking()));
                  //req.addOption(RequestAction.OPT_WAIT_TO_ATTACK, "wait for opportunity to attack any enemy", !isBerserking());
               }
            }
         }
         if (delayedTarget == null) {
            arena.addLocationActions(req, this);
         }
         boolean defAllowed = true;
         if (isBerserking()) {
            // If we can't move, always allow the defend option, even if berserking.
            defAllowed = ((availActions & ACTION_MOVE) == 0);
         }

         if (defAllowed) {
            int actionsThisRound = getActionsAvailableThisRound(true/*usedForDefenseOnly*/);
            int actionsThisTurn = getActionsAvailable(true/*usedForDefenseOnly*/);
            boolean finalDefAllowed = (actionsThisRound == actionsThisTurn);
            if (condition.getActionsSpentThisRound() > 0) {
               req.addOption(new RequestActionOption("on gaurd (0-actions, you have already acted this round)", RequestActionType.OPT_ON_GAURD, LimbType.BODY, true/*enabled*/));
               //req.addOption(RequestAction.OPT_ON_GAURD, "on gaurd (0-actions, you have already acted this round)", true/*enabled*/);
            } else {
               req.addOption(new RequestActionOption("on gaurd (1-action, unless attacked this round)", RequestActionType.OPT_ON_GAURD, LimbType.BODY, (actionsThisTurn > 1)/*enabled*/));
               //req.addOption(RequestAction.OPT_ON_GAURD, "on gaurd (1-action, unless attacked this round)", (actionsThisTurn > 1)/*enabled*/);
            }
            if (actionsThisRound > 0) {
               switch (actionsThisRound) {
                  case 1: req.addOption(new RequestActionOption("final defensive action (1-actions)", RequestActionType.OPT_FINAL_DEFENSE_1, LimbType.BODY, finalDefAllowed/*enabled*/)); break;
                  case 2: req.addOption(new RequestActionOption("final defensive action (2-actions)", RequestActionType.OPT_FINAL_DEFENSE_2, LimbType.BODY, finalDefAllowed/*enabled*/)); break;
                  case 3: req.addOption(new RequestActionOption("final defensive action (3-actions)", RequestActionType.OPT_FINAL_DEFENSE_3, LimbType.BODY, finalDefAllowed/*enabled*/)); break;
                  case 4: req.addOption(new RequestActionOption("final defensive action (4-actions)", RequestActionType.OPT_FINAL_DEFENSE_4, LimbType.BODY, finalDefAllowed/*enabled*/)); break;
                  case 5: req.addOption(new RequestActionOption("final defensive action (5-actions)", RequestActionType.OPT_FINAL_DEFENSE_5, LimbType.BODY, finalDefAllowed/*enabled*/)); break;
               }
               //req.addOption(((RequestAction.OPT_FINAL_DEFENSE_1 + actionsThisRound) - 1), "final defensive action (" + actionsThisRound + "-actions)", finalDefAllowed/*enabled*/);
            }
         } else {
            req.addOption(new RequestActionOption("do nothing", RequestActionType.OPT_ON_GAURD, LimbType.BODY, true/*enabled*/));
            //req.addOption(RequestAction.OPT_ON_GAURD, "do nothing", true);
         }

         // If we have no equipment ready, and none in our equipment list, don't offer equip option.
         boolean objectHeld = false;
         for (Hand hand : getArms()) {
            if (!hand.isEmpty()) {
               objectHeld = true;
               break;
            }
         }
         if (objectHeld || (equipment.size() != 0)) {
            RequestEquipment reqEquip = getEquipmentRequest();
            int enabledCount = reqEquip.getEnabledCount(false/*includeCancelAction*/);
            if (enabledCount > 0) {
               String singleItemAction;
               if (enabledCount == 1) {
                  singleItemAction = reqEquip.getSingleEnabledAction();
                  req.addOption(new RequestActionOption("[un]equip gear or weapons: " + singleItemAction, RequestActionType.OPT_EQUIP_UNEQUIP, LimbType.BODY, true/*enabled*/));
                  //req.addOption(RequestAction.OPT_EQUIP_UNEQUIP, "[un]equip gear or weapons: " + singleItemAction, true/*enabled*/);
               } else {
                  req.addOption(new RequestActionOption("[un]equip gear or weapons", RequestActionType.OPT_EQUIP_UNEQUIP, LimbType.BODY, true/*enabled*/));
                  //req.addOption(RequestAction.OPT_EQUIP_UNEQUIP, "[un]equip gear or weapons", true/*enabled*/);
               }
            }
         }
         if (delayedTarget == null) {
            if ((availActions & ACTION_POSITION) != 0) {
               int actionsNeededToChangePos = RequestActionType.OPT_CHANGE_POS.getActionsUsed((byte) 0);
               if (condition.getActionsAvailableThisRound(false/*useForDefenseOnly*/) >= actionsNeededToChangePos) {
                  req.addOption(new RequestActionOption("change position (" + actionsNeededToChangePos + " actions)", RequestActionType.OPT_CHANGE_POS, LimbType.BODY, true/*enabled*/));
                  //req.addOption(RequestAction.OPT_CHANGE_POS, "change position (" + actionsNeededToChangePos + " actions)", true);
               }
            }
         }
         // Berserkers can't change their targets
         req.addOption(new RequestActionOption("change target", RequestActionType.OPT_CHANGE_TARGET_PRIORITIES, LimbType.BODY, (!isBerserking() && (delayedTarget == null))/*enabled*/));
         //req.addOption(RequestAction.OPT_CHANGE_TARGET_PRIORITIES, "change target", (!isBerserking() && (delayedTarget == null)));
         if (!condition.isCollapsed()) {
            if (currentSpell == null) {
               List<Spell> inateSpells = race.getInateSpells();
               if (inateSpells != null) {
                  //int optionId = RequestAction.OPT_PREPARE_INITATE_SPELL_1;
                  RequestActionType option = RequestActionType.OPT_PREPARE_INITATE_SPELL_1;
                  for (Spell inateSpell : inateSpells) {
                     req.addOption(new RequestActionOption("prepare inate spell " + inateSpell.getName() + " (power " + inateSpell.getPower() + ")",
                                                           option, LimbType.BODY, (!isBerserking() && (delayedTarget == null))/*enabled*/));
                     //req.addOption(optionId++, "prepare inate spell " + inateSpell.getName() + " (power " + inateSpell.getPower() + ")", true);
                     option = RequestActionType.lookupByOrdinal(option.ordinal() + 1);
                  }
               }
            }

            if ((knownMageSpellsList.size() > 0) || (getPriestDeities().size() > 0) || (currentSpell != null)) {
               byte offensiveActionsAvailable = getActionsAvailableThisRound(false/*usedForDefenseOnly*/);
               if (currentSpell == null) {
                  short mageSpellPointsLeft = condition.getMageSpellPointsAvailable();
                  short priestSpellPointsLeft = condition.getPriestSpellPointsAvailable();
                  boolean enabled = true;
                  StringBuilder optName = new StringBuilder();
                  if ((priestSpellPointsLeft == 0) && (mageSpellPointsLeft == 0)) {
                     enabled = false;
                     optName.append("begin spell (no spell points left)");
                  } else if ((mageSpellPointsLeft > 0) && (priestSpellPointsLeft == 0)) {
                     optName.append("begin mage spell (").append(mageSpellPointsLeft).append(" points left)");
                  } else if ((mageSpellPointsLeft == 0) && (priestSpellPointsLeft > 0)) {
                     optName.append("begin priest spell (").append(priestSpellPointsLeft).append(" points left)");
                  } else if ((mageSpellPointsLeft > 0) && (priestSpellPointsLeft > 0)) {
                     optName.append("begin spell (")
                            .append(mageSpellPointsLeft).append(" mage points and ")
                            .append(priestSpellPointsLeft).append(" priest points left)");
                  }
                  req.addOption(new RequestActionOption(optName.toString(), RequestActionType.OPT_BEGIN_SPELL, LimbType.BODY, enabled));
                  //req.addOption(RequestAction.OPT_BEGIN_SPELL, optName.toString(), enabled);
               } else {
                  // Inate spells don't need to be incanted, channeled or maintained
                  if (!currentSpell.isInnate()) {
                     if (currentSpell.getIncantationRoundsRequired() > 0) {
                        String name = "continue incantation of spell (" + currentSpell.getIncantationRoundsRequired() + " rounds remaining)";
                        defaultOption = new RequestActionOption(name, RequestActionType.OPT_CONTINUE_INCANTATION, LimbType.BODY, true);
                        req.addOption(defaultOption);
                        //req.addOption(RequestAction.OPT_CONTINUE_INCANTATION, name, true);
                     } else {
                        if (currentSpell instanceof MageSpell) {
                           for (byte energy = 1; energy <= 5; energy++) {
                              if (offensiveActionsAvailable >= energy) {
//                                 int actionID = 0;
//                                 switch (energy) {
//                                    case 1:  actionID = RequestAction.OPT_CHANNEL_ENERGY_1;  break;
//                                    case 2:  actionID = RequestAction.OPT_CHANNEL_ENERGY_2;  break;
//                                    case 3:  actionID = RequestAction.OPT_CHANNEL_ENERGY_3;  break;
//                                    case 4:  actionID = RequestAction.OPT_CHANNEL_ENERGY_4;  break;
//                                    case 5:  actionID = RequestAction.OPT_CHANNEL_ENERGY_5;  break;
//                                 }
                                 RequestActionType action = null;
                                 switch (energy) {
                                    case 1:  action = RequestActionType.OPT_CHANNEL_ENERGY_1;  break;
                                    case 2:  action = RequestActionType.OPT_CHANNEL_ENERGY_2;  break;
                                    case 3:  action = RequestActionType.OPT_CHANNEL_ENERGY_3;  break;
                                    case 4:  action = RequestActionType.OPT_CHANNEL_ENERGY_4;  break;
                                    case 5:  action = RequestActionType.OPT_CHANNEL_ENERGY_5;  break;
                                 }
                                 StringBuilder actionStr = new StringBuilder();
                                 actionStr.append("channel energy (").append(energy).append("-actions");
                                 actionStr.append(", using ").append(energy);
                                 if (currentSpell.getSpellPoints() > 0) {
                                    actionStr.append(" more");
                                 }
                                 actionStr.append(" spell point");
                                 if (energy > 1) {
                                    actionStr.append("s");
                                 }
                                 actionStr.append(", out of ").append(condition.getMageSpellPointsAvailable()).append(")");
                                 boolean enabled = energy <= condition.getMageSpellPointsAvailable();
                                 req.addOption(new RequestActionOption(actionStr.toString(), action, LimbType.BODY, enabled));
                                 //req.addOption(actionID, actionStr.toString(), enabled);
                              }
                           }
                        }
                     }
                  }
                  req.addOption(new RequestActionOption("discard spell", RequestActionType.OPT_DISCARD_SPELL, LimbType.BODY, true/*enabled*/));
                  //req.addOption(RequestAction.OPT_DISCARD_SPELL, "discard spell", true);
                  if (currentSpell.getIncantationRoundsRequired() == 0) {
                     if (offensiveActionsAvailable > 0) {
                        if (currentSpell instanceof MageSpell) {
                           if (currentSpell.getPower() > 0) {
                              // Inate spells don't need to be incanted, channeled or maintained
                              if (!currentSpell.isInnate()) {
                                 if (!currentSpell.isMaintainedThisTurn()) {
                                    RequestActionOption opt = new RequestActionOption("maintain spell", RequestActionType.OPT_MAINTAIN_SPELL, LimbType.BODY, true/*enabled*/);
                                    req.addOption(opt);
                                    //req.addOption(RequestAction.OPT_MAINTAIN_SPELL, "maintain spell", true);
                                    if (offensiveActionsAvailable == 1) {
                                       defaultOption = opt;
                                    }
                                 }
                              }
                              if (currentSpell instanceof MageSpell) {
                                 for (int action = 1; action <= 3; action++) {
                                    if (offensiveActionsAvailable >= action) {
//                                       int actionID = 0;
//                                       switch (action) {
//                                          case 1: actionID = RequestAction.OPT_COMPLETE_SPELL_1; break;
//                                          case 2: actionID = RequestAction.OPT_COMPLETE_SPELL_2; break;
//                                          case 3: actionID = RequestAction.OPT_COMPLETE_SPELL_3; break;
//                                       }
                                       RequestActionType actionType = null;
                                       switch (action) {
                                          case 1: actionType = RequestActionType.OPT_COMPLETE_SPELL_1; break;
                                          case 2: actionType = RequestActionType.OPT_COMPLETE_SPELL_2; break;
                                          case 3: actionType = RequestActionType.OPT_COMPLETE_SPELL_3; break;
                                       }
                                       boolean enabled = (spellIsInRange && hasLineOfSightToTarget) ||
                                                         currentSpell.isBeneficial() || !currentSpell.requiresTargetToCast();
                                       req.addOption(new RequestActionOption("complete spell (" + action + "-actions)", actionType, LimbType.BODY, enabled));
                                       //req.addOption(actionID, actionStr.toString(), enabled);
                                    }
                                 }
                              }
                           }
                        } else if (currentSpell instanceof PriestSpell) {
                           // Inate spells don't need to be incanted or channeled or maintained
                           if (!currentSpell.isInnate()) {
                              if (!currentSpell.isMaintainedThisTurn()) {
                                 RequestActionOption opt = new RequestActionOption("maintain spell", RequestActionType.OPT_MAINTAIN_SPELL, LimbType.BODY, true);
                                 req.addOption(opt);
                                 //req.addOption(RequestAction.OPT_MAINTAIN_SPELL, "maintain spell", true);
                                 if (offensiveActionsAvailable == 1) {
                                    defaultOption = opt;
                                 }
                              }
                           }

                           int divinePower;
                           // inate spells don't need divine power. They have their maximum power
                           // set as their 'power' attribute.
                           if (!currentSpell.isInnate()) {
                              Advantage adv = getAdvantage(Advantage.DIVINE_POWER);
                              divinePower = adv.getLevel() + 1;
                           } else {
                              divinePower = currentSpell.getPower();
                           }
                           for (int p = 1; p <= divinePower; p++) {
//                              int actionID = 0;
//                              switch (p) {
//                                 case 1: actionID = RequestAction.OPT_COMPLETE_PRIEST_SPELL_1; break;
//                                 case 2: actionID = RequestAction.OPT_COMPLETE_PRIEST_SPELL_2; break;
//                                 case 3: actionID = RequestAction.OPT_COMPLETE_PRIEST_SPELL_3; break;
//                                 case 4: actionID = RequestAction.OPT_COMPLETE_PRIEST_SPELL_4; break;
//                                 case 5: actionID = RequestAction.OPT_COMPLETE_PRIEST_SPELL_5; break;
//                              }
                              RequestActionType actionType = null;
                              switch (p) {
                                 case 1: actionType = RequestActionType.OPT_COMPLETE_PRIEST_SPELL_1; break;
                                 case 2: actionType = RequestActionType.OPT_COMPLETE_PRIEST_SPELL_2; break;
                                 case 3: actionType = RequestActionType.OPT_COMPLETE_PRIEST_SPELL_3; break;
                                 case 4: actionType = RequestActionType.OPT_COMPLETE_PRIEST_SPELL_4; break;
                                 case 5: actionType = RequestActionType.OPT_COMPLETE_PRIEST_SPELL_5; break;
                              }
                              int spellPoints = ((PriestSpell) currentSpell).getAffinity() * p;
                              StringBuilder actionStr = new StringBuilder();
                              actionStr.append("complete spell (").append(p).append("-power");
                              boolean enabled = spellIsInRange && (p > spellPowerPenalty) &&
                                                (hasLineOfSightToTarget || !currentSpell.requiresTargetToCast() || currentSpell.isBeneficial());
                              if (!currentSpell.isInnate()) {
                                 actionStr.append(", using ").append(spellPoints).append(" spell points");
                                 actionStr.append(", out of ").append(condition.getPriestSpellPointsAvailable());
                                 enabled = enabled && (spellPoints <= condition.getPriestSpellPointsAvailable());
                              }
                              actionStr.append(")");
                              req.addOption(new RequestActionOption(actionStr.toString(), actionType, LimbType.BODY, enabled));
                              //req.addOption(actionID, actionStr.toString(), enabled);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
      if (defaultOption != null) {
         req.setDefaultOption(defaultOption);
      }
      return req;
   }

   public byte getTerrainAdjustmentForAttack(StringBuilder terrainExplanation, CombatMap map) {
      List<String> terrainNames = new ArrayList<>();
      byte terrainAdj = (byte) (-getOrientation().getAttackPenaltyForTerrain(this, map, terrainNames));
      if (terrainExplanation != null) {
         while (terrainNames.size() > 0) {
            String terrainName = terrainNames.remove(0);
            if (terrainExplanation.length() > 0) {
               if (terrainNames.size() > 0) {
                  terrainExplanation.append(", ");
               } else {
                  terrainExplanation.append(" and ");
               }
            }
            terrainExplanation.append(terrainName);
         }
      }
      return terrainAdj;
   }


   public List<WeaponStyleAttack> getPossibleChargeAttacksForLimb(Limb limb) {
      List<WeaponStyleAttack> styles = new ArrayList<>();
      // Weapons can be used if we are flying, and they are two-handed weapons.
      // otherwise, head attacks can be used if we have 4 legs, or horns (minotaur)
      boolean isMounted = isMounted();
      boolean hasFourLegs = getLegCount() > 3;
      // flying creature can use any 2-handed thrusting attack in a charge
      LimbType pairedLimbType = limb.limbType.getPairedType();
      Weapon thing = limb.getWeapon(this);
      if (thing != null) {
         boolean canUseTwoHands = false;
         Limb pairedLimb = limbs.get(pairedLimbType);
         if ((pairedLimb != null) && (!pairedLimb.isCrippled())) {
            if (pairedLimb.getHeldThing() == null) {
               canUseTwoHands = true;
            }
         }
         for (WeaponStyleAttack style : thing.getAttackStyles()) {
            SkillRank minRank = style.getMinRank();
            if (minRank != SkillRank.UNKNOWN) {
               SkillRank skillRank = getSkillRank(style.getSkillType());
               if (skillRank.getCost() < minRank.getCost()) {
                  continue;
               }
            }
            if ((style.handsRequired == 2) && !canUseTwoHands) {
               continue;
            }
            if (style.canCharge(isMounted, hasFourLegs)) {
               styles.add(style);
            }
         }
      }
      return styles;
   }

   public Weapon getWeapon() {
      for (Hand hand : getArms()) {
         Weapon thing = hand.getWeapon(this);
         if (thing != null) {
            return thing;
         }
      }
      return null;
   }

   public Weapon getAltWeapon() {
      for (Thing equipment : getEquipment()) {
         if ((equipment instanceof Weapon)) {
            if (((Weapon) equipment).isUnarmedStyle()) {
               return (Weapon) equipment;
            }
         }
      }
      return null;
   }

   public boolean isMovingEvasively() {
      return condition.isMovingEvasively();
   }

   public boolean hasMovedLastAction() {
      return condition.hasMovedLastAction();
   }

   public RequestDefense getDefenseRequest(int attackingCombatantIndex, Character attacker,
                                           RequestAction attack, Arena arena, boolean forCounterAttack) {
      updateWeapons();
      short minDistance = Arena.getMinDistance(attacker, this);
      RANGE range = attack.getRange(attacker, minDistance);
      RequestDefense req = new RequestDefense(attacker, attack, range);

      addDefenseOption(req, new DefenseOptions(DefenseOption.DEF_PD), new DefenseOptions(DefenseOption.DEF_PD),
                       (byte) 0/*attackingWeaponsParryPenalty*/, range, minDistance, attack);
      if (!stillFighting()) {
         return req;
      }
      boolean rangedAttack = attack.isRanged();
      boolean grappleAttack = attack.isGrappleAttack();
      boolean chargeAttack = attack.isCharge();
      DefenseOptions availActions = condition.getAvailableDefenseOptions();
      SkillType martialArtsSkill = null;
      boolean tooFarForMartialArtsDefence = false;
      if (!forCounterAttack) {
         // a counter-attack may not be counter-attacked
         boolean holdingWeapon = false;
         for (Limb limb : limbs.values()) {
            if (limb instanceof Hand) {
               Thing weap = limb.getHeldThing();
               if ((weap != null) && (weap.isReal())) {
                  holdingWeapon = true;
                  break;
               }
            }
         }
         if (!holdingWeapon) {
            if (minDistance > 2) {
               tooFarForMartialArtsDefence = true;
               Limb hand = limbs.get(LimbType.HAND_RIGHT);
               if (hand == null) { hand = limbs.get(LimbType.HAND_LEFT); }
               if (hand == null) { hand = limbs.get(LimbType.HAND_RIGHT_2); }
               if (hand == null) { hand = limbs.get(LimbType.HAND_LEFT_2); }
               if (hand == null) { hand = limbs.get(LimbType.HAND_RIGHT_3); }
               if (hand == null) { hand = limbs.get(LimbType.HAND_LEFT_3); }
               if (hand != null) {
                  Weapon unarmedWeapon = hand.getWeapon(this);
                  if (unarmedWeapon != null) {
                     Profession martialArts = professionsList.get(ProfessionType.MartialArtist);
                     if (martialArts != null) {
                        if (martialArts.getProficientSkills().contains(SkillType.Aikido)) {
                           martialArtsSkill = SkillType.Aikido;
                        }
                        else if (martialArts.getProficientSkills().contains(SkillType.Karate)) {
                           martialArtsSkill = SkillType.Karate;
                        }
                        else if (martialArts.getFamiliarSkills().contains(SkillType.Aikido)) {
                           martialArtsSkill = SkillType.Aikido;
                        }
                        else if (martialArts.getFamiliarSkills().contains(SkillType.Karate)) {
                           martialArtsSkill = SkillType.Karate;
                        }
                        else if (martialArts.getProficientSkills().contains(SkillType.Brawling)) {
                           martialArtsSkill = SkillType.Brawling;
                        }
                        else if (martialArts.getFamiliarSkills().contains(SkillType.Brawling)) {
                           martialArtsSkill = SkillType.Brawling;
                        }
                     }
                     if (martialArtsSkill == null) {
                        Profession fighter = professionsList.get(ProfessionType.Fighter);
                        if (fighter != null) {
                           if (fighter.getProficientSkills().contains(SkillType.Brawling)) {
                              martialArtsSkill = SkillType.Brawling;
                           } else if (fighter.getFamiliarSkills().contains(SkillType.Brawling)) {
                              martialArtsSkill = SkillType.Brawling;
                           }
                        }
                     }
                     if (martialArtsSkill == null) {
                        Profession common = professionsList.get(ProfessionType.Common);
                        if (common != null) {
                           if (common.getProficientSkills().contains(SkillType.Brawling)) {
                              martialArtsSkill = SkillType.Brawling;
                           } else if (common.getFamiliarSkills().contains(SkillType.Brawling)) {
                              martialArtsSkill = SkillType.Brawling;
                           }
                        }
                     }
                  }
               }
            } else {
               // allow counter attack options, if the limb allows it (based on Aikido skill):
               availActions.add(DefenseOption.DEF_COUNTER_GRAB_1,
                                DefenseOption.DEF_COUNTER_GRAB_2,
                                DefenseOption.DEF_COUNTER_GRAB_3,
                                DefenseOption.DEF_COUNTER_THROW_1,
                                DefenseOption.DEF_COUNTER_THROW_2,
                                DefenseOption.DEF_COUNTER_THROW_3);
            }
         }
      }

      SpellParalyze paralyzeSpell = null;
      for (Spell spell : activeSpellsList) {
         if (spell instanceof SpellParalyze) {
            paralyzeSpell = (SpellParalyze) spell;
            break;
         }
      }
      if (paralyzeSpell != null) {
         if (!paralyzeSpell.allowsRetreat()) {
            availActions.remove(DefenseOption.DEF_RETREAT);
         }
         if (!paralyzeSpell.allowsDodge()) {
            availActions.remove(DefenseOption.DEF_DODGE);
         }
      }
      if ((paralyzeSpell == null) || paralyzeSpell.allowsBlockParry()) {
         for (Limb limb : limbs.values()) {
            if (limb.canDefend(this, rangedAttack, minDistance, attack.isCharge(), grappleAttack, attack.getDamageType(), true)) {
               availActions.add(limb.getDefOption());
            }
         }
      }


      boolean spellDef = false;
      if (attack.isRanged()) {
         if (bestDefensiveSpell_ranged != null) {
            spellDef = true;
         }
      } else if (attack.isCompleteSpell()) {
         if (bestDefensiveSpell_spell != null) {
            spellDef = true;
         }
      } else if (bestDefensiveSpell_melee != null) {
         // counter-attacks may not be defended against with spells
         if (!forCounterAttack) {
            spellDef = true;
         }
      }

      if (spellDef) {
         availActions.add(DefenseOption.DEF_MAGIC_1,
                          DefenseOption.DEF_MAGIC_2,
                          DefenseOption.DEF_MAGIC_3,
                          DefenseOption.DEF_MAGIC_4,
                          DefenseOption.DEF_MAGIC_5);
      }
      boolean retreatBlocked = false;
      boolean retreatHeld = false;
      String cantRetreatOrParryFromAttackType = null;
      WeaponStyleAttack attackMode = attack.getAttackStyle(attacker);
      byte attackingWeaponsParryPenalty = (attackMode == null) ? 0 : attackMode.getParryPenalty();

      if (rangedAttack || attack.isCharge()) {
         cantRetreatOrParryFromAttackType = attack.getCantRetreatOrParryFromAttackTypeString();
         availActions.remove(DefenseOption.DEF_RETREAT);
         for (Limb limb : limbs.values()) {
            if (!limb.canDefend(this, rangedAttack, minDistance, attack.isCharge(), grappleAttack, attack.getDamageType(), true)) {
               availActions.remove(limb.getDefOption());
            }
         }
      }
      byte defenseAdjForRangePerAction = Rules.getRangeDefenseAdjustmentPerAction(range);
      if (availActions.contains(DefenseOption.DEF_RETREAT)) {
         if (heldPenalties.size() > 0) {
            retreatHeld = true;
            availActions.remove(DefenseOption.DEF_RETREAT);
         } else if (!arena.canRetreat(this, attacker.getLimbLocation(attack.getLimb(), arena.getCombatMap()))) {
            retreatBlocked = true;
            availActions.remove(DefenseOption.DEF_RETREAT);
         }
      }
      if (isBerserking()) {
         // Berserking characters can only dodge as a defense.
         availActions = availActions.logicAndWithSet(new DefenseOptions(DefenseOption.DEF_DODGE));
      }
      StringBuilder sb = new StringBuilder();
      sb.append(getName()).append(", you have ").append(condition.getActionsAvailable(true/*usedForDefenseOnly*/)).append(" actions remaining");
      byte actionsAvailableThisRound = getActionsAvailableThisRound(true/*usedForDefenseOnly*/);
      if (actionsAvailableThisRound == condition.getActionsAvailable(true/*usedForDefenseOnly*/)) {
         sb.append(".");
      } else {
         sb.append(" for this turn, ").append(actionsAvailableThisRound).append(" can be spent this round.");
      }
      if (forCounterAttack) {
         sb.append(" \nYou are being counter-attacked by ").append(attacker.getName());
      } else if (attack.isGrappleAttack()) {
         sb.append(" \nYou are being grabbed by ").append(attacker.getName());
      } else {
         sb.append(" \nYou are being attacked by ").append(attacker.getName());
         Weapon attackingWeapon = attack.getAttackingWeapon(attacker);
         if (attackingWeapon != null) {
            sb.append(", using a ");
            if (attackingWeapon.isReal()) {
               sb.append(attackingWeapon.getName());
            } else if (attackingWeapon.isMissileWeapon()) {
               // missile spells return 'false' to the isReal() call
               sb.append(attackingWeapon.getName());
            } else if (attackMode != null) {
               sb.append(attackMode.getName());
            }
         } else {
            Spell spell = attack.getSpell();
            if (spell != null) {
               sb.append(", casting a '").append(spell.getName()).append("' spell");
            }
         }
      }

      if (attack.isRangedAttack()) {
         // If we are being attack by a missile weapon, display how long the
         // attacker has been aiming at us for.
         int aimDuration = attacker.getAimDuration(uniqueID);
         sb.append(" (").append(attacker.getName()).append(" has been aiming for ").append(aimDuration).append(" rounds, ");
         // compute and display the effective actions of the attack.
         int effectiveActions = attack.getAttackActions(false/*considerSpellAsAttack*/);
         IRequestOption answer = attack.answer();
         boolean isMissileAttack = false;
         if (answer instanceof RequestActionOption) {
            RequestActionOption reqAnswer = (RequestActionOption) answer;
            isMissileAttack = reqAnswer.getValue() == RequestActionType.OPT_ATTACK_MISSILE;
         }
         if (isMissileAttack) {
            effectiveActions += Math.min(3, aimDuration) - 1;
         } else {
            effectiveActions += Math.min(2, aimDuration) - 1;
         }

         sb.append(" effectively using ").append(effectiveActions).append(" actions");
      } else {
         if (forCounterAttack) {
            sb.append(" (").append(attack.getActionsUsed()).append(" actions");
         } else {
            sb.append(" (").append(attack.getAttackActions(true/*considerSpellAsAttack*/)).append(" actions");
         }
         if (attack.isCharge()) {
            sb.append(" charging attack");
         }
         sb.append(")");
      }
      byte woundPenalty = condition.getWounds();
      boolean woundReported = false;
      if (woundPenalty > 0) {
         sb.append(" \nWounds reduce all your defenses by ").append(woundPenalty);
         woundReported = true;
      }
      if (getPosition() != Position.STANDING) {
         sb.append("\nBecause you are ").append(getPositionName());
         DamageType damType = attack.getDamageType();
         byte minDam = req.getMinimumDamage();
         int dodge1 = getDefenseOptionTN(new DefenseOptions(DefenseOption.DEF_DODGE), minDam, false/*includeWoundPenalty*/,
                                         true/*includeHolds*/, false/*includePos*/, false/*includeMassiveDam*/,
                                         attackingWeaponsParryPenalty, rangedAttack, minDistance, chargeAttack, grappleAttack,
                                         damType, false/*defenseAppliedAlready*/, range);
         int dodge2 = getDefenseOptionTN(new DefenseOptions(DefenseOption.DEF_DODGE), minDam, false/*includeWoundPenalty*/,
                                         true/*includeHolds*/, true/*includePos*/, false/*includeMassiveDam*/,
                                         attackingWeaponsParryPenalty, rangedAttack, minDistance, chargeAttack, grappleAttack,
                                         damType, false/*defenseAppliedAlready*/, range);
         int retreat1 = getDefenseOptionTN(new DefenseOptions(DefenseOption.DEF_RETREAT), minDam, false/*includeWoundPenalty*/,
                                           true/*includeHolds*/, false/*includePos*/, false/*includeMassiveDam*/,
                                           attackingWeaponsParryPenalty, rangedAttack, minDistance, chargeAttack, grappleAttack,
                                           damType, false/*defenseAppliedAlready*/, range);
         int retreat2 = getDefenseOptionTN(new DefenseOptions(DefenseOption.DEF_RETREAT), minDam, false/*includeWoundPenalty*/,
                                           true/*includeHolds*/, true/*includePos*/, false/*includeMassiveDam*/,
                                           attackingWeaponsParryPenalty, rangedAttack, minDistance, chargeAttack, grappleAttack,
                                           damType, false/*defenseAppliedAlready*/, range);
         int parry1 = getDefenseOptionTN(new DefenseOptions(DefenseOption.DEF_RIGHT), minDam, false/*includeWoundPenalty*/,
                                         true/*includeHolds*/, false/*includePos*/, false/*includeMassiveDam*/,
                                         attackingWeaponsParryPenalty, rangedAttack, minDistance, chargeAttack, grappleAttack,
                                         damType, false/*defenseAppliedAlready*/, range);
         int parry2 = getDefenseOptionTN(new DefenseOptions(DefenseOption.DEF_RIGHT), minDam, false/*includeWoundPenalty*/,
                                         true/*includeHolds*/, true/*includePos*/, false/*includeMassiveDam*/,
                                         attackingWeaponsParryPenalty, rangedAttack, minDistance, chargeAttack, grappleAttack,
                                         damType, false/*defenseAppliedAlready*/, range);
         if (dodge1 != dodge2) {
            sb.append(" your dodge is reduced by ").append(dodge1 - dodge2).append(" points,");
         }
         if (retreat1 != retreat2) {
            sb.append(" your retreat is reduced by ").append(retreat1 - retreat2).append(" points,");
         }
         if (parry1 != parry2) {
            sb.append(" your parry and blocks are reduced by ").append(parry1 - parry2).append(" points,");
         }
         sb.setLength(sb.length()); // remove the trailing ','
         sb.append("."); // and replace it with a '.'
      }
      int maxActionsPerRound = getMaxActionsPerRound();
      // If we are berserking, don't bother mentioning that an arm is p
      if (forCounterAttack) {
         sb.append("\nBecause you are being counter-attacked, your armor's PD does not benefit your TN.");
      }
      if (isBerserking()) {
         sb.append("\nBecause you are berserking, your only allowed defense is dodge.");
      } else {
         for (Hand hand : getArms()) {
            byte handPenalty = hand.getWoundPenalty();
            String handDefName = hand.getDefenseName(false, this);
            if (handDefName != null) {
               if (handPenalty > 0) {
                  if (availActions.contains(hand.getDefOption())) {
                     if (woundReported) {
                        sb.append(" and reduces");
                     } else {
                        sb.append("\nWounds reduce");
                        woundReported = true;
                     }
                     sb.append(" your ").append(handDefName).append(" level by an additional ").append(handPenalty);
                  }
               } else if (handPenalty < 0) {
                  sb.append("\nYour ").append(hand.getName());
                  sb.append(" is crippled, so you may not ").append(handDefName).append(".");
               }
            }
         }
         byte retreatPenalty = condition.getPenaltyRetreat(false/*includeWounds*/);
         if (retreatPenalty > 0) {
            if (availActions.contains(DefenseOption.DEF_RETREAT)) {
               if (woundPenalty == 0) {
                  sb.append("\nWounds reduce");
               } else {
                  sb.append(" and reduces");
               }
               sb.append(" your retreat level by an additional ").append(retreatPenalty);
            }
         }
         sb.append(".");
      }
      if (tooFarForMartialArtsDefence && (martialArtsSkill != null)) {
         sb.append("\nYou are too far away from the attacker defend with your ")
           .append(martialArtsSkill.name()).append(" skill.");
      }

      if (cantRetreatOrParryFromAttackType != null) {
         if (!isBerserking()) {
            sb.append("\nYou may not retreat or parry an attack from a ");
            sb.append(cantRetreatOrParryFromAttackType).append(".");
         }
         if (rangedAttack) {
            sb.append("\nYou are in the attacker's ").append(range.getName()).append(" range");
            if (defenseAdjForRangePerAction != 0) {
               sb.append(", so every active defense will be at ");
               if (defenseAdjForRangePerAction > 0) {
                  sb.append("+");
               }
               sb.append(defenseAdjForRangePerAction);
               sb.append(" per action");
            } else {
               sb.append(" so your active defenses are unaffected by range");
            }
            byte tnRangeAdj = Rules.getRangeDefenseAdjustmentToPD(range);
            if (tnRangeAdj != 0) {
               sb.append(" and your PD is increased by ").append(tnRangeAdj);
            }
            sb.append(".");
         }
      } else {
         if (!isBerserking()) {
            // paralyze at 2 or higher prevents dodge defenses.
            // paralyze at 3 or higher prevents block & parry defenses.
            if ((paralyzeSpell != null) && !paralyzeSpell.allowsBlockParry()) {
               sb.append(" Because you are fully paralyzed, you may not actively defend any attack.");
            } else {
               if (attackingWeaponsParryPenalty > 0) {
                  sb.append(" The attacking weapon is difficult to parry, so you parry at a -");
                  sb.append(attackingWeaponsParryPenalty);
               }
               // paralyze at 1 or higher prevents retreat.
               if ((paralyzeSpell != null) && !paralyzeSpell.allowsRetreat()) {
                  sb.append(" Because you are paralyzed, you may not retreat from");
                  if (!paralyzeSpell.allowsDodge()) {
                     sb.append(", or dodge");
                  }
                  sb.append(" the attack.");
               } else {
                  if (retreatBlocked) {
                     sb.append(" Your back is against a wall (or obstacle), blocking your retreat.");
                  } else if (retreatHeld) {
                     sb.append(" Because your being held, you may not retreat.");
                  }
               }
            }
         }
      }
      byte heldPenalty = 0;
      for (IHolder holder : getHolders()) {
         heldPenalty += holder.getHoldingLevel();
      }
      if (heldPenalty > 0) {
         sb.append("\nBecause you are being held, all your active defenses are at -").append(heldPenalty).append(".");
      }
      boolean massiveAttack = false;
      List<DefenseOptions> listOfListOfDefOptions = new ArrayList<>();
      listOfListOfDefOptions.add(new DefenseOptions(DefenseOption.DEF_DODGE, DefenseOption.DEF_RETREAT));
      if (!isBerserking() && !forCounterAttack) {
         for (Hand hand : getArms()) {
            listOfListOfDefOptions.add(new DefenseOptions(hand.getDefOption()));
            if (hand.canDefend(this, req.isRangedAttack(), minDistance, req.isChargeAttack(),
                               req.isGrapple(), req.getDamageType(), true/*checkState*/)) {
               byte handPenalty = hand.getPenaltyForMassiveDamage(this, req.getMinimumDamage(), minDistance,
                                                                  req.isRangedAttack(), req.isChargeAttack(),
                                                                  req.isGrapple(), req.getDamageType(),
                                                                  true/*checkState*/);
               if (handPenalty > 0) {
                  if (!massiveAttack) {
                     massiveAttack = true;
                     sb.append("\nThe attack will do at least ").append(req.getMinimumDamage());
                     sb.append(" ").append(req.getDamageType().shortname);
                     sb.append(" damage, so your defenses are reduced.");
                  }
                  sb.append("\n").append(hand.getDefenseName(false, this));
                  sb.append("(").append(hand.getName()).append(")");
                  sb.append(" is at -").append(handPenalty).append(".");
               }
            }
         }
         for (Hand hand : getArms()) {
            if (hand.canDefend(this, req.isRangedAttack(), minDistance, req.isChargeAttack(), req.isGrapple(), req.getDamageType(), true/*checkState*/)) {
               DefenseOptions listOfCounterAttackOptions = new DefenseOptions();
               if (hand.canCounterAttack(this, true/*grab*/)) {
                  switch (actionsAvailableThisRound) {
                     case 7:
                     case 6:
                     case 5:
                     case 4: listOfCounterAttackOptions.add(DefenseOption.DEF_COUNTER_GRAB_3);
                     case 3: listOfCounterAttackOptions.add(DefenseOption.DEF_COUNTER_GRAB_2);
                     case 2: listOfCounterAttackOptions.add(DefenseOption.DEF_COUNTER_GRAB_1);
                     case 1:
                  }
               }
               if (hand.canCounterAttack(this, false/*grab*/)) {
                  switch (actionsAvailableThisRound) {
                     case 7:
                     case 6:
                     case 5:
                     case 4: listOfCounterAttackOptions.add(DefenseOption.DEF_COUNTER_THROW_3);
                     case 3: listOfCounterAttackOptions.add(DefenseOption.DEF_COUNTER_THROW_2);
                     case 2: listOfCounterAttackOptions.add(DefenseOption.DEF_COUNTER_THROW_1);
                     case 1:
                  }
               }
               if (listOfCounterAttackOptions.getIntValue() != 0) {
                  listOfListOfDefOptions.add(listOfCounterAttackOptions);
               }
            }
         }
      }
      if (req.isRangedAttack()) {
         if (hasMovedLastAction()) {
            byte movingTNBonus = Rules.getTNBonusForMovement(range, isMovingEvasively());
            sb.append("\nBecause you are moving");
            if (isMovingEvasively()) {
               sb.append(" evasively");
            }
            sb.append(", your defenses are increased by ").append(movingTNBonus).append(".");
         }
      }
      if (paralyzeSpell != null) {
         if (!paralyzeSpell.allowsRetreat()) {
            sb.append("\nBecause you are paralyzed, you may not retreat");
            if (!paralyzeSpell.allowsDodge()) {
               if (!paralyzeSpell.allowsBlockParry()) {
                  sb.append(" dodge, parry or block");
               } else {
                  sb.append(" or dodge");
               }
            }
            sb.append(".");
         }
      }
      if (spellDef) {
         short priestPoints = getCondition().getPriestSpellPointsAvailable();
         short magePoints = getCondition().getMageSpellPointsAvailable();
         if ((priestPoints == 0) && (magePoints == 0)) {
            sb.append(" You have no spell points remaining to use spell defenses.");
         } else if ((priestPoints < actionsAvailableThisRound) && (magePoints < actionsAvailableThisRound)) {
            sb.append(" You only have ").append(Math.max(priestPoints, magePoints)).append(" spell points remaining to use spell defenses.");
         }
         DefenseOptions magicDefOptions = new DefenseOptions();
         switch (maxActionsPerRound) {
            case 5: magicDefOptions.add(DefenseOption.DEF_MAGIC_5);
            case 4: magicDefOptions.add(DefenseOption.DEF_MAGIC_4);
            case 3: magicDefOptions.add(DefenseOption.DEF_MAGIC_3);
            case 2: magicDefOptions.add(DefenseOption.DEF_MAGIC_2);
            case 1: magicDefOptions.add(DefenseOption.DEF_MAGIC_1);
                    listOfListOfDefOptions.add(magicDefOptions);
         }
      }
      sb.append("\nHow do you want to defend yourself?");
      req.setMessage(sb.toString());
      System.out.println("listOfListOfDefOptions = " + listOfListOfDefOptions);

      HashMap<Byte, TreeSet<DefenseOptions>> mapActionsToDefActions = new HashMap<>();
      addOptionForDefenseOptions(mapActionsToDefActions, listOfListOfDefOptions, actionsAvailableThisRound, new DefenseOptions());
      // passive defense option has already been added
      for (byte actions = 1; actions <= maxActionsPerRound; actions++) {
         // separator for this new columns:
         addDefenseOption(req, null, null, attackingWeaponsParryPenalty, range, minDistance, attack);

         TreeSet<DefenseOptions> defActions = mapActionsToDefActions.get(actions);
         if (defActions != null) {
            for (DefenseOptions defAction : defActions) {
               addDefenseOption(req, defAction, availActions, attackingWeaponsParryPenalty, range, minDistance, attack);
            }
         }
      }
      return req;
   }

   private void addOptionForDefenseOptions(HashMap<Byte, TreeSet<DefenseOptions>> mapActionsToDefActions,
                                           List<DefenseOptions> listOfListOfDefOptions, int maxActionsUseAllowed, DefenseOptions defs) {
      if (listOfListOfDefOptions.size() == 0) {
         return;
      }
      // Make a copy of listOfListOfDefs, because if we mess with it, it will affect the caller
      List<DefenseOptions> listOfListOfDefOptionsCopy = new ArrayList<>();
      for (DefenseOptions defOptions : listOfListOfDefOptions) {
         listOfListOfDefOptionsCopy.add(defOptions.clone());
      }

      DefenseOptions defOptions = listOfListOfDefOptionsCopy.remove(0);
      // recurse the options where the first action is not taken at all:
      addOptionForDefenseOptions(mapActionsToDefActions, listOfListOfDefOptionsCopy, maxActionsUseAllowed, defs);
      // now recurse for each possible action taken:
      for (int i = 0; i < defOptions.size(); i++) {
         DefenseOption defOpt = defOptions.get(i);
         DefenseOptions defOpts = defs.clone();
         defOpts.add(defOpt);
         // check for illegal combinations such as Dodge/Retreat, or counter-attack/Retreat
         if (!defOpts.isDefensesValid()) {
            continue;
         }
         byte actionsUsed = defOpts.getDefenseActionsUsed();
         if (actionsUsed <= maxActionsUseAllowed) {
            TreeSet<DefenseOptions> defActionsPerAction = mapActionsToDefActions.computeIfAbsent(actionsUsed, k -> new TreeSet<>());
            defActionsPerAction.add(defOpts);
            addOptionForDefenseOptions(mapActionsToDefActions, listOfListOfDefOptionsCopy, maxActionsUseAllowed, defOpts);
         }
      }
   }

   @Override
   public RequestGrapplingHoldMaintain getGrapplingHoldMaintain(Character escapingCharacter, RequestAction escape, Arena arena) {
      if (escapingCharacter != holdTarget) {
         DebugBreak.debugBreak();
      }
      if (escapingCharacter.getHoldLevel(this) == null) {
         DebugBreak.debugBreak();
      }
      if (getHoldingLevel() == null) {
         DebugBreak.debugBreak();
      }
      RequestGrapplingHoldMaintain maintReq = new RequestGrapplingHoldMaintain();
      int maxActions = getActionsAvailableThisRound(true/*usedForDefenseOnly*/);

      int brawlingSkill  = getSkillLevel(SkillType.Brawling,  null, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, false/*adjustForHolds*/);
      int wrestlingSkill = getSkillLevel(SkillType.Wrestling, null, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, false/*adjustForHolds*/);
      int aikidoSkill    = getSkillLevel(SkillType.Aikido,    null, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, false/*adjustForHolds*/);
      int skill = Math.max(brawlingSkill, Math.max(wrestlingSkill, aikidoSkill));
      if (skill <= 0) {
         if (skill <= Rules.getEncumbranceLevel(this)) {
            DebugBreak.debugBreak();
         }
         //return null;
      }
      String sb = getName() + ", " + escapingCharacter.getName() +
                  " is trying to break free of your level-" + escapingCharacter.getHoldLevel(this) +
                  " hold, using " + escape.getActionsUsed() + " actions.\n" +
                  " How many actions do you want to use to maintain your hold on " + escapingCharacter.getHimHer() + "?";
      maintReq.setMessage(sb);

      maintReq.addMaintainHoldOptions(maxActions, this, escapingCharacter, skill, getWoundsAndPainPenalty());
      return maintReq;
   }

   public RequestPosition getRequestPosition(RequestAction parentReq) {
      RequestPosition req = null;
      if (stillFighting()) {
         req = new RequestPosition(parentReq);
         StringBuilder sb = new StringBuilder();
         sb.append(getName()).append(", you have ").append(condition.getActionsAvailable(false/*usedForDefenseOnly*/)).append(" actions remaining");
         if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) == condition.getActionsAvailable(false/*usedForDefenseOnly*/)) {
            sb.append(".");
         } else {
            sb.append(" for this turn, ").append(getActionsAvailableThisRound(false/*usedForDefenseOnly*/)).append(" can be spent this round.");
         }
         sb.append(" \nYou are currently ").append(getPositionName()).append(".");
         sb.append(" \nWhat position would you like to move to this round?");
         req.setMessage(sb.toString());
         req.addPositions(condition.getAvailablePositions(), condition.getPosition());
      }
      return req;
   }

   static class AttackOption {
      Integer styleIndex;
      String  desc;
      Boolean    styleAllowed;
      AttackType attackType;
      DiceSet    attackDice;
      byte       styleDamage;
      DamageType damageType;
      byte       adjustedSkillLevel;
      byte       speed;
      SkillRank  rank;
      SkillType  skillType;

      public AttackOption(Integer styleIndex, String desc, Boolean styleAllowed, AttackType attackType,
                          DiceSet attackDice, byte styleDamage, DamageType damageType, byte adjustedSkillLevel,
                          byte speed, SkillRank rank, SkillType skillType) {
         this.styleIndex = styleIndex;
         this.desc = desc;
         this.styleAllowed = styleAllowed;
         this.attackType = attackType;
         this.attackDice = attackDice;
         this.styleDamage = styleDamage;
         this.damageType = damageType;
         this.adjustedSkillLevel = adjustedSkillLevel;
         this.speed = speed;
         this.rank = rank;
         this.skillType = skillType;
      }
   }

   public RequestAttackStyle getRequestAttackStyle(RequestAction parentAction, Arena arena) {
      RequestAttackStyle req = null;
      boolean has4legs = getLegCount() > 3;
      Character target = arena.getCharacter(parentAction.targetID);
      Weapon weap = limbs.get(parentAction.getLimb()).getWeapon(this);
      ArenaCoordinates weapCoord = getOrientation().getLimbCoordinates(parentAction.getLimb());
      if (weap != null) {
         if (stillFighting() && (target != null)) {
            boolean isGrapple = parentAction.isGrappleAttack();
            boolean isCharge = parentAction.isCharge();
            boolean isCounterAttack = parentAction.isCounterAttack();

            req = new RequestAttackStyle(uniqueID, target.uniqueID, parentAction.getLimb());
            StringBuilder sb = new StringBuilder();
            if (isGrapple) {
               sb.append(getName()).append(", how do you want to grab ").append(target.getName()).append("?");
            } else if (isCounterAttack) {
               sb.append(getName()).append(", how do you want to counter attack ").append(target.getName()).append("?");
            } else {
               sb.append(getName()).append(", your opponent, ").append(target.getName());
               sb.append(" is wearing ").append(target.getArmor().getName());
               sb.append(" (");
               for (DamageType damType : new DamageType[]{DamageType.BLUNT, DamageType.CUT, DamageType.IMP}) {
                  sb.append(" build-").append(damType.shortname);
                  sb.append(" = ").append(target.getBuild(damType));
               }
               sb.append(" )");
               sb.append(" \nHow do you want to attack him with your ").append(weap.getName()).append("?");
            }
            req.setMessage(sb.toString());
            short minDistanceToTarget = Arena.getShortestDistance(weapCoord, target.getOrientation());
            short maxDistanceToTarget = Arena.getFarthestDistance(weapCoord, target.getOrientation());
            int advanceRange = 0;
            if (parentAction.isAdvance()) {
               advanceRange = 1;
            }
            WeaponStyleAttack[] styles = isCounterAttack ? weap.counterattackStyles :
                                         isGrapple ? weap.grapplingStyles : weap.attackStyles;

            boolean canUseTwohanded = false;
            // if no shield, always try to use weapon in a 2-handed style
            Limb otherLimb = limbs.get(parentAction.getLimb().getPairedType());
            if ((otherLimb instanceof Hand) &&
                (otherLimb.limbType != parentAction.getLimb()) &&
                otherLimb.isEmpty() &&
                (!otherLimb.isCrippled())) {
               for (int i = 0; i < styles.length; i++) {
                  // If the target is out of range, don't bother checking for handedness
                  short minRange = styles[i].getMinRange();
                  short maxRange = styles[i].getMaxRange();
                  boolean inRange = (maxDistanceToTarget >= minRange) && ((minDistanceToTarget - advanceRange) <= maxRange);
                  if (inRange) {
                     if (isGrapple) {
                        canUseTwohanded = true;
                     } else if (weap.isTwoHanded(i)) {
                        canUseTwohanded = true;
                     }
                  }
               }
            }
            boolean singleSpeed = true;
            int curSpeed = -1;
            byte lowestSpeed = 127;
            byte strength = getAttributeLevel(Attribute.Strength);
            for (WeaponStyleAttack style : styles) {
               byte styleSpeed = style.getSpeed(strength);
               if (styleSpeed < lowestSpeed) {
                  lowestSpeed = styleSpeed;
               }

               if (curSpeed != styleSpeed) {
                  if (curSpeed != -1) {
                     singleSpeed = false;
                     break;
                  }
                  curSpeed = styleSpeed;
               }
            }
            int defaultIndex = -1;
            byte bestExpectedDamage = -127;
            List<AttackOption> options = new ArrayList<>();
            for (int styleIndex = 0; styleIndex < styles.length; styleIndex++) {
               short minRange = styles[styleIndex].getMinRange();
               short maxRange = styles[styleIndex].getMaxRange();

               if (isCharge) {
                  // If this is a charge, or an advance-and-attack,
                  // then the attacker has already moved up next to the target
               } else {
                  // This should not be needed:
                  maxRange += advanceRange;
               }
               // Large target example (a two-hex figure):
               //     twohanded sword: minRange=2, maxRange=3.
               //     target is 1-2, 2-3 or 3-4 hexes away, the target is in range:
               boolean inRange = (minRange <= maxDistanceToTarget) && (minDistanceToTarget <= maxRange);
               boolean handsOK = true;
               // If the target is out of range, don't bother checking for handedness
               if (inRange) {
                  // For bastard swords, allow single-handed attacks, even if we could use both hands.
                  //                  if (canUseTwohanded && (styles[styleIndex].getHandsRequired() == 1)) {
                  //                     handsOK = false;
                  //                  }
                  if (!canUseTwohanded && (styles[styleIndex].getHandsRequired() == 2)) {
                     handsOK = false;
                  }
               }
               SkillType skillType = styles[styleIndex].getSkillType();
               byte skillLevel = getSkillLevel(skillType, null/*handUse*/, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, true/*adjustForHolds*/);
               SkillRank skillRank = getSkillRank(skillType);
               if ((styles[styleIndex].getMinRank().getCost() <= skillRank.getCost()) &&
                   (!isCharge || styles[styleIndex].canCharge(isMounted(), has4legs))) {
                  sb.setLength(0);
                  sb.append(" (");
                  sb.append(styles[styleIndex].getName());
                  sb.append(": ");
                  if (!isGrapple && !isCounterAttack) {
                     sb.append(styles[styleIndex].getDamageString(getPhysicalDamageBase())).append(", ");
                  }
                  sb.append("skill '").append(skillType);
                  sb.append("' level = ").append(skillLevel - styles[styleIndex].getSkillPenalty());
                  byte styleSpeed = -1;
                  if (!singleSpeed) {
                     styleSpeed = styles[styleIndex].getSpeed(strength);
                     sb.append(", speed = ").append(styleSpeed);
                  }
                  sb.append(")");

                  byte attackActions = (byte) Math.min(parentAction.getAttackActions(false/*considerSpellAsAttack*/), styles[styleIndex].getMaxAttackActions());
                  int aimActions = getAimDuration(target.uniqueID);
                  if (aimActions > 0) {
                     attackActions += (byte) (Math.min(aimActions - 1, styles[styleIndex].getMaxAimBonus()));
                  }
                  boolean styleAllowed = true;
                  if (styles[styleIndex].isRanged()) {
                     if ((getAimDuration(target.uniqueID) <= 0) || parentAction.isAdvance()) {
                        styleAllowed = false;
                     }
                  }
                  if (!inRange || !handsOK) {
                     styleAllowed = false;
                  }
                  byte styleDamage = styles[styleIndex].getDamage(getAdjustedStrength());
                  if (styleAllowed) {
                     if (singleSpeed || (lowestSpeed == styleSpeed)) {
                        byte expectedDamageForStyle = styleDamage;
                        expectedDamageForStyle -= target.getBuild(styles[styleIndex].getDamageType());
                        expectedDamageForStyle += (skillLevel / 2);
                        if ((defaultIndex == -1) || (expectedDamageForStyle > bestExpectedDamage)) {
                           defaultIndex = styleIndex;
                           bestExpectedDamage = expectedDamageForStyle;
                        }
                     }
                  }
                  DiceSet attackDice = Rules.getDice(getAttributeLevel(Attribute.Dexterity), attackActions, Attribute.Dexterity/*attribute*/, RollType.ATTACK_TO_HIT);
                  options.add(new AttackOption(styleIndex, sb.toString(), styleAllowed, styles[styleIndex].getAttackType(),
                                               attackDice, styleDamage, styles[styleIndex].getDamageType(),
                                               (byte) (skillLevel - styles[styleIndex].getSkillPenalty()), styleSpeed,
                                               skillRank, skillType));
               }
            }

            // Remove less effective attacks
            for (int i=0 ; i<options.size() ; i++) {
               AttackOption aoi = options.get(i);
               for (int j=i+1 ; j<options.size() ; j++) {
                  AttackOption aoj = options.get(j);
                  // never exclude different types of damage (cut vs. imp)
                  if (aoi.damageType != aoj.damageType) {
                     continue;
                  }
                  // If the skillTypes are the same, and the levels are the same, never exclude them (aikido throw vs aikido grab)
                  if (aoi.adjustedSkillLevel == aoj.adjustedSkillLevel &&
                      aoi.skillType == aoj.skillType) {
                     continue;
                  }

                  if (aoi.attackDice.getExpectedRoll() >= aoj.attackDice.getExpectedRoll() &&
                      aoi.styleDamage >= aoj.styleDamage &&
                      aoi.adjustedSkillLevel >= aoj.adjustedSkillLevel &&
                      aoi.rank.getCost() >= aoj.rank.getCost() &&
                      aoi.speed <= aoj.speed
                      ) {
                     // aoi is superior or equal in every way to aoj, so we can remove aoj
                     options.remove(j--);
                     continue;
                  }
                  if (aoi.attackDice.getExpectedRoll() <= aoj.attackDice.getExpectedRoll() &&
                      aoi.styleDamage <= aoj.styleDamage &&
                      aoi.adjustedSkillLevel <= aoj.adjustedSkillLevel &&
                      aoi.rank.getCost() <= aoj.rank.getCost() &&
                      aoi.speed >= aoj.speed
                     ) {
                     // aoj is superior or equal in every way to aoi, so we can remove aoi
                     options.remove(i);
                     i--;
                     break;
                  }
               }
            }
            int availableCount = 0;
            int availableIndex = -1;
            for (AttackOption opt : options) {
               if (opt.styleAllowed) {
                  availableCount++;
                  availableIndex = opt.styleIndex;
               }
               req.addAttackOption(opt.styleIndex, opt.desc, opt.styleAllowed, opt.attackType, opt.attackDice, opt.damageType);
            }
            if (defaultIndex != -1) {
               req.setDefaultOption(defaultIndex);
            }
            if (availableCount == 1) {
               // if there is only one way to attack, don't ask
               req.setAnswerID(availableIndex);
            } else if (availableCount == 0) {
               // if there is no way to attack, cancel the attack.
               // This can happen when the attacker and target have the
               // same initiative, and the target moves away while the
               // attack is deciding what to do:
               req.addOption(SyncRequest.OPT_CANCEL_ACTION, "Cancel attack", true/*enabled*/);
               req.setAnswerID(SyncRequest.OPT_CANCEL_ACTION);
            }
         }
      }
      return req;
   }

   private void addDefenseOption(RequestDefense req, DefenseOptions defOpts, DefenseOptions availableOptions,
                                 byte attackingWeaponsParryPenalty, RANGE range, short distance, RequestAction attack) {
      if (defOpts == null) {
         req.addSeparatorOption();
         return;
      }
      // check for illegal combinations
      if (!defOpts.isDefensesValid()) {
         DebugBreak.debugBreak();
         return;
      }
      IInstantaneousSpell spellDefenseUsed;
      //      if ( attack.isCompleteSpell()) {
      //         spellDefenseUsed = bestDefensiveSpell_spell;
      //      }
      if (attack.isRanged()) {
         spellDefenseUsed = bestDefensiveSpell_ranged;
      } else {
         spellDefenseUsed = bestDefensiveSpell_melee;
      }
      if (spellDefenseUsed == null) {
         if (defOpts.contains(DefenseOption.DEF_MAGIC_1) ||
             defOpts.contains(DefenseOption.DEF_MAGIC_2) ||
             defOpts.contains(DefenseOption.DEF_MAGIC_3) ||
             defOpts.contains(DefenseOption.DEF_MAGIC_4) ||
             defOpts.contains(DefenseOption.DEF_MAGIC_5)) {
            return;
         }
      }
      int actionsUsed = defOpts.getDefenseActionsUsed();
      boolean enabled = defOpts.logicAndWithSet(availableOptions).equals(defOpts);
      if (enabled) {
         if ((actionsUsed > getActionsAvailableThisRound(true/*usedForDefenseOnly*/)) && (actionsUsed > 0)) {
            enabled = false;
         } else {
            int spellPointsUsed = defOpts.getDefenseMagicPointsUsed();
            if (spellPointsUsed > 0) {
               int pointAvailable = 0;
               if (spellDefenseUsed instanceof PriestSpell) {
                  pointAvailable = getCondition().getPriestSpellPointsAvailable();
               }
               if (spellDefenseUsed instanceof MageSpell) {
                  pointAvailable = getCondition().getMageSpellPointsAvailable();
               }
               if (pointAvailable < spellPointsUsed) {
                  enabled = false;
               }
            }
         }
      }
      byte TN = 0;
      if (enabled) {
         TN = getDefenseOptionTN(defOpts, req.getMinimumDamage(), true/*includeWoundPenalty*/, true/*includeHolds*/,
                                 true/*includePosition*/, true, attackingWeaponsParryPenalty, req.isRangedAttack(),
                                 distance, req.isChargeAttack(), req.isGrapple(), req.getDamageType(), false/*defenseAppliedAlready*/,
                                 range);
         if (req.isRangedAttack()) {
            if (hasMovedLastAction()) {
               byte movingTNBonus = Rules.getTNBonusForMovement(range, isMovingEvasively());
               TN += movingTNBonus;
            }
         }
      }
      req.addOption(defOpts, TN, enabled, this, attack);
   }

   public String getDefenseName(DefenseOption defOpt, boolean pastTense, RequestAction attack) {
      return defOpt.getName(pastTense, this, attack);
   }

   public byte getDefenseTN(RequestDefense defense, boolean includeWoundPenalty, boolean includeHolds,
                            boolean includePosition, boolean includeMassiveDamagePenalty,
                            byte attackingWeaponsParryPenalty, boolean defenseAppliedAlready,
                            short distance, RANGE range) {
      return getDefenseOptionTN(new DefenseOptions(defense.getDefenseIndex()), defense.getMinimumDamage(), includeWoundPenalty, includeHolds, includePosition,
                                includeMassiveDamagePenalty, attackingWeaponsParryPenalty, defense.isRangedAttack(), distance, defense.isChargeAttack(),
                                defense.isGrapple(), defense.getDamageType(), defenseAppliedAlready, range);
   }

   public byte getPassiveDefense(RANGE range, boolean isGrappleAttack, short distance) {
      return getDefenseOptionsBase(DamageType.GENERAL, isGrappleAttack, false/*includeWoundPenalty*/,
                                   false/*includePosition*/, true/*computePdOnly*/, distance).get(range).get(DefenseOption.DEF_PD);
   }

   public byte getDefenseOptionTN(DefenseOptions defenseOptions, byte minimumDamage, boolean includeWoundPenalty,
                                  boolean includeHolds, boolean includePosition, boolean includeMassiveDamagePenalty,
                                  byte attackingWeaponsParryPenalty, boolean isRangedAttack, short distance,
                                  boolean isChargeAttack, boolean isGrappleAttack, DamageType damageType,
                                  boolean defenseAppliedAlready, RANGE range) {
      HashMap<RANGE, HashMap<DefenseOption, Byte>> baseDefs = getDefenseOptionsBase(DamageType.GENERAL, isGrappleAttack,
                                                                                    false/*includeWoundPenalty*/,
                                                                                    includePosition,
                                                                                    false/*computePdOnly*/, distance);
      byte basePD = baseDefs.get(range).get(DefenseOption.DEF_PD);
      byte defenseTN = getBaseDefenseOptionTN(baseDefs, defenseOptions, range, isGrappleAttack, damageType, includeWoundPenalty, includePosition, includeHolds);
      for (Limb limb : limbs.values()) {
         // Is this hand used in the defense?
         DefenseOption defIndex = limb.getDefOption();
         if (defenseOptions.contains(defIndex)) {
            byte maxTnThisHand = limb.getDefenseTNWithoutWounds(this, isRangedAttack, distance, isChargeAttack, isGrappleAttack, damageType, !defenseAppliedAlready);
            byte penalty = 0;
            if ((limb instanceof Hand) && ((Hand) limb).isDefenseParry(this)) {
               penalty += attackingWeaponsParryPenalty;
            }
            // This now occurs inside getBaseDefenseTN(...)
            //               if (includePain) {
            //                  if (limbs.get(limbType).isCrippled()) penalty = maxTnThisHand;
            //                  else penalty += limbs.get(limbType).getWoundPenalty();
            //               }
            if (includeMassiveDamagePenalty) {
               byte massiveDamagePenalty = limb.getPenaltyForMassiveDamage(this, minimumDamage, distance,
                                                                           isRangedAttack, isChargeAttack,
                                                                           isGrappleAttack, damageType,
                                                                           !defenseAppliedAlready);
               penalty += massiveDamagePenalty;
            }
            defenseTN -= Math.min(maxTnThisHand, penalty);
         }
      }
      // This now occurs inside getBaseDefenseTN(...)
      //      if (includePain) {
      //         if ((defenseIndex & DEF_RETREAT) != 0) {
      //            defenseTN -= condition.getPenaltyRetreat(false);
      //         }
      //         defenseTN -= condition.getWounds();
      //      }

      // you can never defend worse than your Passive Defense.
      if (defenseTN < basePD) {
         return basePD;
      }
      return defenseTN;
   }

   public HashMap<RANGE, HashMap<DefenseOption, Byte>> getDefenseOptionsBase(DamageType damType, boolean isGrappleAttack,
                                                                             boolean includeWoundPenalty,
                                                                             boolean includePosition,
                                                                             boolean computePdOnly,
                                                                             short distance) {
      HashMap<RANGE, HashMap<DefenseOption, Byte>> defBase = new HashMap<>();
      for (RANGE range : RANGE.values()) {
         defBase.put(range, new HashMap<>());
      }

      byte pd = 0;
      Armor armor = getArmor(); // may be natural armor, if no outer armor is worn
      if (!isGrappleAttack) {
         if (armor != null) {
            pd += armor.getPassiveDefense();
         }
      }
      pd -= getRace().getBonusToBeHit();

      for (LimbType armType : LimbType.ARM_TYPES) {
         Hand hand = (Hand) getLimb(armType);
         if (hand != null) {
            Thing heldThing = hand.getHeldThing();
            Limb pairedHand = getLimb(hand.limbType.getPairedType());
            Thing pairedHeldThing = (pairedHand == null) ? null : pairedHand.getHeldThing();
            boolean canUse2Hands = ((pairedHeldThing == null) || (pairedHeldThing == heldThing));
            if (heldThing == null) {
               heldThing = hand.getWeapon(this);
            } else {
               if (!isGrappleAttack) {
                  pd += heldThing.getPassiveDefense();
               }
            }

            if (!computePdOnly) {
               DefenseOption defOption = hand.getDefOption();
               for (RANGE range : RANGE.values()) {
                  byte def = 0;
                  if (heldThing != null) {
                     if ((range == RANGE.OUT_OF_RANGE) || heldThing.canDefendAgainstRangedWeapons()) {
                        def = heldThing.getBestDefenseOption(this, armType, canUse2Hands, damType, isGrappleAttack,
                                                             distance);
                        byte rangeAdjustment = Rules.getRangeDefenseAdjustmentPerAction(range);
                        def += rangeAdjustment;
                        if (includeWoundPenalty) {
                           byte limbUsePenalty = hand.getWoundPenalty();
                           if (limbUsePenalty < 0) {
                              def = 0;
                           } else {
                              def -= limbUsePenalty;
                           }
                        }
                        if (includePosition) {
                           def = getPositionAdjustedDefenseOption(defOption, def);
                        }
                     }
                  }
                  if (def < 0) {
                     def = 0;
                  }
                  defBase.get(range).put(defOption, def);
               }
            }
         }
      }
      byte rangedPD = pd;
      byte meleePD = pd;
      for (Spell spell : activeSpellsList) {
         rangedPD += spell.getPassiveDefenseModifier(true, DamageType.GENERAL);
         meleePD += spell.getPassiveDefenseModifier(false, DamageType.GENERAL);
      }

      if (!computePdOnly) {
         List<Spell> spells = getSpells();

         for (Spell spell : spells) {
            if (spell instanceof IInstantaneousSpell) {
               IInstantaneousSpell instantSpell = (IInstantaneousSpell) spell;
               if (instantSpell.canDefendAgainstMeleeAttacks()) {
                  if ((bestDefensiveSpell_melee == null) ||
                      (getSpellLevel(bestDefensiveSpell_melee.getName()) < getSpellLevel(instantSpell.getName()))) {
                     bestDefensiveSpell_melee = instantSpell;
                  }
               }
               if (instantSpell.canDefendAgainstRangedAttacks()) {
                  if ((bestDefensiveSpell_ranged == null) ||
                      (getSpellLevel(bestDefensiveSpell_ranged.getName()) < getSpellLevel(instantSpell.getName()))) {
                     bestDefensiveSpell_ranged = instantSpell;
                  }
               }
               if (instantSpell.canDefendAgainstSpells()) {
                  if ((bestDefensiveSpell_spell == null) ||
                      (getSpellLevel(bestDefensiveSpell_spell.getName()) < getSpellLevel(instantSpell.getName()))) {
                     bestDefensiveSpell_spell = instantSpell;
                  }
               }
            }
         }
         if (bestDefensiveSpell_melee != null) {
            defBase.get(RANGE.OUT_OF_RANGE).put(DefenseOption.DEF_MAGIC_1, bestDefensiveSpell_melee.getActiveDefensiveTN((byte) 1, this));
            defBase.get(RANGE.OUT_OF_RANGE).put(DefenseOption.DEF_MAGIC_2, bestDefensiveSpell_melee.getActiveDefensiveTN((byte) 2, this));
            defBase.get(RANGE.OUT_OF_RANGE).put(DefenseOption.DEF_MAGIC_3, bestDefensiveSpell_melee.getActiveDefensiveTN((byte) 3, this));
            defBase.get(RANGE.OUT_OF_RANGE).put(DefenseOption.DEF_MAGIC_4, bestDefensiveSpell_melee.getActiveDefensiveTN((byte) 4, this));
            defBase.get(RANGE.OUT_OF_RANGE).put(DefenseOption.DEF_MAGIC_5, bestDefensiveSpell_melee.getActiveDefensiveTN((byte) 5, this));
         }
         if (bestDefensiveSpell_ranged != null) {
            for (RANGE range : RANGE.values()) {
               if (range != RANGE.OUT_OF_RANGE) {
                  defBase.get(range).put(DefenseOption.DEF_MAGIC_1, bestDefensiveSpell_ranged.getActiveDefensiveTN((byte) 1, this));
                  defBase.get(range).put(DefenseOption.DEF_MAGIC_2, bestDefensiveSpell_ranged.getActiveDefensiveTN((byte) 2, this));
                  defBase.get(range).put(DefenseOption.DEF_MAGIC_3, bestDefensiveSpell_ranged.getActiveDefensiveTN((byte) 3, this));
                  defBase.get(range).put(DefenseOption.DEF_MAGIC_4, bestDefensiveSpell_ranged.getActiveDefensiveTN((byte) 4, this));
                  defBase.get(range).put(DefenseOption.DEF_MAGIC_5, bestDefensiveSpell_ranged.getActiveDefensiveTN((byte) 5, this));
               }
            }
         }
      }
      byte attributeNim = getAttributeLevel(Attribute.Nimbleness);
      for (RANGE range : RANGE.values()) {
         byte dodge = Rules.getDodgeLevel(attributeNim);
         byte retreat = Rules.getRetreatLevel(attributeNim);
         byte rangeAdjustmentToPD = Rules.getRangeDefenseAdjustmentToPD(range);
         byte rangeAdjustmentPerAction = Rules.getRangeDefenseAdjustmentPerAction(range);
         boolean canDodge = true;
         boolean canRetreat = true;
         if (!computePdOnly) {
            if (includeWoundPenalty) {
               if (condition.getPenaltyMove() < 0) {
                  canDodge = false;
                  canRetreat = false;
               } else {
                  dodge = (byte) Math.max(0, dodge - condition.getPenaltyMove());
                  byte retreatPenalty = condition.getPenaltyRetreat(includeWoundPenalty);
                  if (retreatPenalty < 0) {
                     canRetreat = false;
                  } else {
                     retreat = (byte) Math.max(0, retreat - retreatPenalty);
                  }
               }
            }
            if (canDodge) {
               dodge += rangeAdjustmentPerAction;
               if (dodge < 0) {
                  dodge = 0;
               }
            } else {
               dodge = 0;
            }
            if (canRetreat) {
               retreat += rangeAdjustmentPerAction * 2;
               if (retreat < 0) {
                  retreat = 0;
               }
            } else {
               retreat = 0;
            }
            if (includePosition) {
               dodge = getPositionAdjustedDefenseOption(DefenseOption.DEF_DODGE, dodge);
               retreat = getPositionAdjustedDefenseOption(DefenseOption.DEF_RETREAT, retreat);
            }
            defBase.get(range).put(DefenseOption.DEF_DODGE, dodge);
            defBase.get(range).put(DefenseOption.DEF_RETREAT, retreat);
         }
         defBase.get(range).put(DefenseOption.DEF_PD, (byte) (((range == RANGE.OUT_OF_RANGE) ? meleePD : rangedPD) + rangeAdjustmentToPD));
      }
      return defBase;
   }

   public List<Spell> getSpells() {
      List<Spell> spells = new ArrayList<>();
      spells.addAll(knownMageSpellsList);
      spells.addAll(getPriestSpells());
      return spells;
   }

   public List<PriestSpell> getPriestSpells() {
      List<PriestSpell> spells = new ArrayList<>();
      List<Deity> deities = getPriestDeities();
      for (Deity deity : deities) {
         int deityAffinity = getAffinity(deity);
         if (deityAffinity > 0) {
            List<SpellGroup> spellGroups = PriestSpell.getSpellGroups(deity);
            for (SpellGroup group : spellGroups) {
               List<PriestSpell> sroupSpells = PriestSpell.getSpellsInGroup(group);
               for (PriestSpell spell : sroupSpells) {
                  if (deityAffinity >= spell.getAffinity()) {
                     spell = (PriestSpell) spell.clone();
                     spell.setCaster(this);
                     spell.setDeity(deity);
                     boolean lowerPowerSpellAlreadyExists = false;
                     for (Spell alreadyAddedSpell : spells) {
                        if (alreadyAddedSpell.getClass().equals((spell.getClass()))) {
                           // If we already have this spell in our list, at a lower affinity level,
                           // always use that one, and that one only.
                           // For example, the SummonHellHounds is a level 6 affinity evil spell, but only a level 3 Demonic spell.
                           if (spell.getAffinity() > ((PriestSpell) alreadyAddedSpell).getAffinity()) {
                              // don't add the higher-affinity version
                              lowerPowerSpellAlreadyExists = true;
                           } else {
                              // remove the higher-affinity version:
                              spells.remove(alreadyAddedSpell);
                           }
                           break;
                        }
                     }
                     if (!lowerPowerSpellAlreadyExists) {
                        spells.add(spell);
                     }
                  }
               }
            }
         }
      }
      return spells;
   }

   public List<Deity> getPriestDeities() {
      List<Deity> deities = new ArrayList<>();
      for (Deity deity : Deity.values()) {
         if (hasAdvantage(Advantage.DIVINE_AFFINITY_ + deity.getName())) {
            deities.add(deity);
         }
      }
      return deities;
   }

   public byte getBaseDefenseOptionTN(HashMap<RANGE, HashMap<DefenseOption, Byte>> defenseBase, DefenseOptions defenseActions,
                                      RANGE range, boolean isGrappleAttack, DamageType damType,
                                      boolean includeWoundPenalty, boolean includePosition, boolean includeHolds) {
      // Start with passive defense:
      byte pd = defenseBase.get(range).get(DefenseOption.DEF_PD);
      byte defense = 0;
      // now check for each possible defense base:
      for (DefenseOption defOpt : defenseBase.get(range).keySet()) {
         // If this base is used in defenseActions, count it:
         if (defenseActions.contains(defOpt) || (defOpt == DefenseOption.DEF_PD)) {
            defense += defenseBase.get(range).get(defOpt);
         }
      }

      if (includeWoundPenalty) {
         defense -= getWounds();
      }
      if (includeHolds) {
         for (IHolder holder : getHolders()) {
            defense -= getHoldLevel(holder);
         }
      }
      // NEVER go below the PD of the target:
      if (defense < pd) {
         defense = pd;
      }
      return defense;
   }

   //   public byte[][] getDefenses(byte damType)
   //   {
   //      byte[][] defenseBase = getDefenseBase(damType);
   //      byte[][] defense = new byte[(int) Math.pow((1*2), defenseBase.length)][RANGE_NAMES.length];
   //      for (int i = 0; i < defense.length; i++) {
   //         for (RANGE range : RANGE.values()) {
   //            // passive defense:
   //            defense[i][range.ordinal()] = defenseBase[DEF_BASE_NONE][range.ordinal()];
   //            for (int b = DEF_BASE_NONE; b < DEF_BASE_ENTRY_COUNT; b++) {
   //               if ((i & (1 << b)) != 0) {
   //                  defense[i][range.ordinal()] += defenseBase[b][range.ordinal()];
   //               }
   //            }
   //         }
   //      }
   //      return defense;
   //   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         writeToStream(uniqueID, out);
         writeToStream(teamID, out);
         writeToStream(name, out);
         writeToStream(((race != null) ? race.getName() : ""), out);
         writeToStream(((race != null) ? race.getGender().name : ""), out);
         for (Attribute att : Attribute.values()) {
            writeToStream(attributes.get(att), out);
         }
         List<Limb> limbs = new ArrayList<>(this.limbs.values());
         writeToStream(limbs, out);

         synchronized (equipment) {
            try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
               writeToStream(equipment, out);
            }
         }
         writeToStream(((armor != null) ? armor.getName() : ""), out);
         writeToStream(getProfessionsList(), out);
         writeToStream(knownMageSpellsList, out);
         writeToStream(advList, out);
         writeToStream(aimDuration, out);
         writeToStream(targetID, out);
         condition.serializeToStream(out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   static public int readIntoListThing(List<Thing> data, DataInputStream in) throws IOException {
      data.clear();
      List<SerializableObject> things = readIntoListSerializableObject(in);
      for (SerializableObject thing : things) {
         if (thing instanceof Thing) {
            data.add((Thing) thing);
         }
      }
      return data.size();
   }

   static public int readIntoListProfession(List<Profession> data, DataInputStream in) throws IOException {
      data.clear();
      List<SerializableObject> skills = readIntoListSerializableObject(in);
      for (SerializableObject profession : skills) {
         if (profession instanceof Profession) {
            data.add((Profession) profession);
         }
      }
      return data.size();
   }

   static public int readIntoListSpell(List<MageSpell> data, DataInputStream in) throws IOException {
      data.clear();
      List<SerializableObject> spells = readIntoListSerializableObject(in);
      for (SerializableObject spell : spells) {
         if (spell instanceof MageSpell) {
            data.add((MageSpell) spell);
         }
      }
      return data.size();
   }

   static public int readIntoListAdvantage(List<Advantage> data, DataInputStream in) throws IOException {
      data.clear();
      List<SerializableObject> advs = readIntoListSerializableObject(in);
      for (SerializableObject adv : advs) {
         if (adv instanceof Advantage) {
            data.add((Advantage) adv);
         }
      }
      return data.size();
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         uniqueID = readInt(in);
         teamID = readByte(in);
         name = readString(in);
         String race = readString(in);
         String genderStr = readString(in);
         Gender gender = Gender.getByName(genderStr);
         setRace(race, gender);
         for (Attribute att : Attribute.values()) {
            attributes.put(att, readByte(in));
         }

         limbs.clear();
         List<SerializableObject> things = readIntoListSerializableObject(in);
         for (SerializableObject thing : things) {
            if (thing instanceof Limb) {
               Limb newLimb = (Limb) thing;
               if (newLimb.getRacialBase() == null) {
                  DebugBreak.debugBreak();
               }
               limbs.put(newLimb.limbType, newLimb);
            }
         }
         synchronized (equipment) {
            try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
               readIntoListThing(equipment, in);
            }
         }
         armor = Armor.getArmor(readString(in), getRace());
         List<Profession> professionList = new ArrayList<>();
         readIntoListProfession(professionList, in);
         setProfessionsList(professionList);
         readIntoListSpell(knownMageSpellsList, in);
         readIntoListAdvantage(advList, in);
         aimDuration = readByte(in);
         targetID = readInt(in);
         condition.serializeFromStream(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
      resetSpellPoints();
      updateWeapons();
   }

   @Override
   public Character clone() {
      byte[] attArray = new byte[Attribute.COUNT];
      for (Attribute att : Attribute.values()) {
         attArray[att.value] = attributes.get(att);
      }
      Profession[] professionsArray = professionsList.values().toArray(new Profession[professionsList.size()]);
      MageSpell[] spellsArray = knownMageSpellsList.toArray(new MageSpell[knownMageSpellsList.size()]);
      Advantage[] advArray = advList.toArray(new Advantage[advList.size()]);

      Character newChar = new Character(name, (race == null) ? null : race.getName(),
                                        (race == null) ? Gender.MALE : race.getGender(), attArray,
                                        (armor == null) ? null : armor.getName(), limbs, getEquipment(),
                                        professionsArray, spellsArray, advArray);
      newChar.copyData(this);
      return newChar;
   }

   public void copyData(Character source) {
      if (this == source) {
         return;
      }
      name = source.getName();
      setRace(source.getRace().getName(), source.getGender());
      for (Attribute att : Attribute.values()) {
         attributes.put(att, source.attributes.get(att));
      }
      limbs.clear();
      for (LimbType limbType : source.limbs.keySet()) {
         limbs.put(limbType, source.limbs.get(limbType).clone());
      }

      synchronized (equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
            equipment.clear();
            equipment.addAll(source.equipment);
         }
      }
      armor = Armor.getArmor(source.getArmor().getName(), getRace());
      setProfessionsList(source.getProfessionsList());
      knownMageSpellsList = new ArrayList<>();
      for (MageSpell sourceSpell : source.knownMageSpellsList) {
         MageSpell spell = MageSpells.getSpell(sourceSpell.getName());
         spell.setFamiliarity(sourceSpell.getFamiliarity());
         knownMageSpellsList.add(spell);
      }
      advList = new ArrayList<>();
      for (Advantage sourceAdvantage : source.advList) {
         Advantage adv = Advantage.getAdvantage(sourceAdvantage.toString());
         advList.add(adv);
      }

      // active values:
      condition = source.condition.clone();
      uniqueID = source.uniqueID;
      teamID = source.teamID;
      targetID = source.targetID;
      aimDuration = source.aimDuration;
      headCount = source.headCount;
      eyeCount = source.eyeCount;
      legCount = source.legCount;
      wingCount = source.wingCount;
      if (source.currentSpell != null) {
         if (source.currentSpell instanceof MageSpell) {
            for (Spell spell : knownMageSpellsList) {
               if (spell.getClass() == source.currentSpell.getClass()) {
                  currentSpell = spell.clone();
                  currentSpell.setCaster(this);
                  break;
               }
            }
         } else {
            currentSpell = source.currentSpell.clone();
            currentSpell.setCaster(this);
         }
      }
      isBerserking = source.isBerserking;
      hasInitiativeAndActionsEverBeenInitialized = source.hasInitiativeAndActionsEverBeenInitialized;
      // computed values:
      //resetSpellPoints();
      updateWeapons();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Character: ");
      sb.append("uniqueID: ").append(uniqueID);
      sb.append(", name: ").append(name);
      sb.append(", points: ").append(getPointTotal());
      sb.append(", race: ").append((race == null) ? null : race.getName());
      sb.append(", gender: ").append((race == null) ? null : race.getGender());
      for (Attribute att : Attribute.values()) {
         sb.append(", ").append(att.shortName).append(":").append(attributes.get(att));
         if (race.getBuildModifier() != 0) {
            if (att == Attribute.Strength) {
               sb.append(" (").append(getAdjustedStrength()).append(")");
            } else if (att == Attribute.Health) {
               sb.append(" (").append(getBuildBase()).append(")");
            }
         }
      }
      sb.append(", armor: ").append((armor == null) ? null : armor.getName());
      for (Limb limb : limbs.values()) {
         sb.append(", ").append(limb.limbType.name).append(":").append(limb.getHeldThingName());
      }
      synchronized (equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
            sb.append(", equipment: ").append(equipment);
         }
      }
      sb.append(", $").append(getTotalCost()).append(" spent");
      sb.append(", professions: ").append(professionsList.values());
      sb.append(", spells: ").append(knownMageSpellsList);
      sb.append(", advantages: ").append(advList);
      sb.append(", teamID: ").append(teamID);
      sb.append(", aimDuration: ").append(aimDuration);
      sb.append(", condition: ").append(condition);
      return sb.toString();
   }

   public String print() {
      StringBuilder sb = new StringBuilder();
      sb.append(name).append(": (").append(getPointTotal()).append(" points)");
      if (race != null) {
         sb.append(race.getGender()).append(" ").append(race.getName());
      }
      for (Attribute att : Attribute.values()) {
         sb.append(", ").append(att.shortName).append(":").append(attributes.get(att));
         if (race.getBuildModifier() != 0) {
            if (att == Attribute.Strength) {
               sb.append(" (").append(getAdjustedStrength()).append(")");
            } else if (att == Attribute.Health) {
               sb.append(" (").append(getBuildBase()).append(")");
            }
         }
      }
      sb.append("<br>armor: ").append((armor == null) ? null : armor.getName());
      for (Limb limb : limbs.values()) {
         String heldThingName = limb.getHeldThingName();
         if ((heldThingName != null) && (heldThingName.length() > 0)) {
            sb.append("<br>held in ").append(limb.getName()).append(":").append(heldThingName);
         }
      }
      synchronized (equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
            if (!equipment.isEmpty()) {
               sb.append("<br>equipment on belt: ");
               for (Thing thing : equipment) {
                  sb.append(thing.getName()).append(", ");
               }
            }
         }
      }
      sb.append("<br>encumbrance level: ").append(Rules.getEncumbranceLevel(this));
      if (!professionsList.isEmpty()) {
         sb.append("<br>professions: ").append(professionsList.values());
      }
      if (!knownMageSpellsList.isEmpty()) {
         sb.append("<br>spells: ").append(knownMageSpellsList);
      }
      if (!advList.isEmpty()) {
         sb.append("<br>advantages: ").append(advList);
      }
      return sb.toString();
   }

   public List<Integer> getOrderedTargetPriorites() {
      return orderedTargetIds;
   }

   public void setTargetPriorities(List<Integer> orderedTargetIds) {
      this.orderedTargetIds.clear();
      this.orderedTargetIds.addAll(orderedTargetIds);
      if (this.orderedTargetIds.size() > 0) {
         targetID = this.orderedTargetIds.get(0);
      }
   }

   public void setTarget(int targetsUniqueID) {
      targetID = targetsUniqueID;
      // re-order the list, moving the new target to the top of the list.
      if (orderedTargetIds.remove(Integer.valueOf(targetsUniqueID))) {
         orderedTargetIds.add(0, targetsUniqueID);
      }
   }

   public String serializeToString() {
      StringBuilder sb = new StringBuilder();
      sb.append(name).append(SEPARATOR_MAIN);
      sb.append(race.getName()).append(SEPARATOR_MAIN);
      sb.append(race.getGender().name).append(SEPARATOR_MAIN);
      for (Attribute att : Attribute.values()) {
         sb.append(attributes.get(att)).append(SEPARATOR_SECONDARY);
      }
      sb.append(SEPARATOR_MAIN);
      sb.append((armor == null) ? " " : armor.getName()).append(SEPARATOR_MAIN);
      for (Hand hand : getArms()) {
         sb.append(hand.getHeldThingName()).append(SEPARATOR_SECONDARY);
      }
      sb.append(SEPARATOR_MAIN);
      synchronized (equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
            if (equipment.size() == 0) {
               sb.append(SEPARATOR_SECONDARY);
            } else {
               for (Thing thing : equipment) {
                  if (thing != null) {
                     sb.append(thing.getName()).append(SEPARATOR_SECONDARY);
                  }
               }
            }
         }
      }
      sb.append(SEPARATOR_MAIN);
      if ((professionsList.size() + knownMageSpellsList.size()) == 0) {
         sb.append(SEPARATOR_SECONDARY);
      } else {
         for (Profession profession : professionsList.values()) {
            sb.append(profession.getType().getName())
              .append('=').append(profession.getLevel())
              .append(SEPARATOR_TIRTIARY)
              .append(profession.getProficientSkills()
                                .stream()
                                .map(SkillType::getName)
                                .collect(Collectors.joining(",")))
              .append(SEPARATOR_TIRTIARY)
              .append(profession.getFamiliarSkills()
                                .stream()
                                .map(SkillType::getName)
                                .collect(Collectors.joining(",")))
              .append(SEPARATOR_SECONDARY);
         }
         for (MageSpell spell : knownMageSpellsList) {
            sb.append(spell.getName())
              .append('=')
              .append(spell.getFamiliarity().getName())
              .append(SEPARATOR_SECONDARY);
         }
      }
      sb.append(SEPARATOR_MAIN);
      if (advList.size() == 0) {
         sb.append(SEPARATOR_SECONDARY);
      } else {
         for (Advantage adv : advList) {
            sb.append(adv.getName()).append(SEPARATOR_SECONDARY);
            sb.append(adv.getLevel()).append(SEPARATOR_SECONDARY);
         }
      }
      sb.append(SEPARATOR_MAIN);

      return sb.toString();
   }

   public boolean serializeFromString(String source) {
      String attributes;
      String handsString;
      StringTokenizer st = new StringTokenizer(source, SEPARATOR_MAIN);
      if (!st.hasMoreElements()) {
         return false;
      }
      name = st.nextToken();
      if (!st.hasMoreElements()) {
         return false;
      }
      String race = st.nextToken();
      if (!st.hasMoreElements()) {
         return false;
      }
      String genderStr = st.nextToken();
      Gender gender = Gender.getByName(genderStr);
      setRace(race, gender);
      if (!st.hasMoreElements()) {
         return false;
      }
      attributes = st.nextToken();
      if (!st.hasMoreElements()) {
         return false;
      }
      armor = Armor.getArmor(st.nextToken(), getRace());
      if (!st.hasMoreElements()) {
         return false;
      }
      handsString = st.nextToken();
      if (!st.hasMoreElements()) {
         return false;
      }
      String equipment = st.nextToken();
      // If no skills or advantages are listed, then there will be no more elements, but that's OK.
      String skills = (st.hasMoreElements() ? st.nextToken() : "");
      String advantages = (st.hasMoreElements() ? st.nextToken() : "");

      // we should be done reading
      if (st.hasMoreElements()) {
         return false;
      }
      condition = new Condition(this);

      // parse the attributes
      st = new StringTokenizer(attributes, SEPARATOR_SECONDARY);
      for (Attribute att : Attribute.values()) {
         if (st.hasMoreElements()) {
            this.attributes.put(att, Byte.parseByte(st.nextToken()));
         }
      }
      // parse the held equipment
      st = new StringTokenizer(handsString, SEPARATOR_SECONDARY);
      for (Hand hand : getArms()) {
         if (st.hasMoreElements()) {
            hand.setHeldThing(Thing.getThing(st.nextToken(), getRace()), this);
         }
      }
      // parse the equipment
      synchronized (this.equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
            this.equipment.clear();
            st = new StringTokenizer(equipment, SEPARATOR_SECONDARY);
            while (st.hasMoreElements()) {
               String equipName = (String) st.nextElement();
               Thing thing = Thing.getThing(equipName, true/*allowTool*/, getRace());
               if (thing != null) {
                  this.equipment.add(thing);
               }
            }
         }
      }

      // parse the skills & spells
      professionsList.clear();
      st = new StringTokenizer(skills, SEPARATOR_SECONDARY);
      while (st.hasMoreElements()) {
         String profAsString = (String) st.nextElement();
         int loc = profAsString.indexOf('=');
         if (loc != -1) {
            String name = profAsString.substring(0, loc); // everything before the '='
            String level = profAsString.substring(loc + 1); // everything after the '='
            ProfessionType professionType = ProfessionType.getByName(name);
            if (professionType != null) {
               StringTokenizer st2 = new StringTokenizer(level, SEPARATOR_TIRTIARY);
               level = (String) st2.nextElement();
               String profSkillsStr = (String) st2.nextElement();
               String famSkillsStr = (String) st2.nextElement();
               List<SkillType> proficientSkills = new ArrayList<>();
               List<SkillType> familiarSkills = new ArrayList<>();
               String[] profSkillList = profSkillsStr.split(",");
               String[] famSkillList = famSkillsStr.split(",");
               for (String profSkill : profSkillList) {
                  SkillType skillType = SkillType.getSkillTypeByName(profSkill);
                  if (skillType != null) {
                     proficientSkills.add(skillType);
                  }
               }
               for (String famSkill : famSkillList) {
                  SkillType skillType = SkillType.getSkillTypeByName(famSkill);
                  if (skillType != null) {
                     familiarSkills.add(skillType);
                  }
               }
               Profession profession = new Profession(professionType, proficientSkills, Byte.parseByte(level));
               profession.setFamiliarSkills(familiarSkills);
               professionsList.put(professionType, profession);
            } else {
               MageSpell spell = MageSpells.getSpell(name);
               if (spell != null) {
                  byte castingLevel = 4;
                  Profession spellcasting = professionsList.computeIfAbsent(ProfessionType.Spellcasting,
                                                                            o -> new Profession(ProfessionType.Spellcasting,
                                                                                                spell.prerequisiteSkillTypes[0],
                                                                                                castingLevel));
                  setSpellLevel(name, spellcasting.getLevel(), MageSpell.Familiarity.getFamiliarityByName(level));
               }
            }
         }
      }
      // parse the advantages
      advList = new ArrayList<>();
      st = new StringTokenizer(advantages, SEPARATOR_SECONDARY);
      while (st.hasMoreElements()) {
         String advName = (String) st.nextElement();
         if (st.hasMoreElements()) {
            String levelStr = (String) st.nextElement();
            Advantage adv = Advantage.getAdvantage(advName);
            if (adv != null) {
               adv.setLevel(Byte.parseByte(levelStr));
               advList.add(adv);
            }
         }
      }

      resetSpellPoints();
      updateWeapons();
      return true;
   }

   public Document getXmlObject(boolean includeConditionData) {
      // Create a builder factory
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(true/*validating*/);

      // Create the builder and parse the file
      Document charDoc = null;
      try {
         DocumentBuilder builder = factory.newDocumentBuilder();
         charDoc = builder.newDocument();
         Element element = getXmlObject(charDoc, includeConditionData, "\n");
         charDoc.appendChild(element);
      } catch (ParserConfigurationException e) {
         e.printStackTrace();
      }
      return charDoc;
   }

   public Element getXmlObject(Document parentDoc, boolean includeConditionData, String newLine) {
      Element mainElement = parentDoc.createElement("Character");
      mainElement.setAttribute("Name", getName());
      mainElement.setAttribute("Race", getRace().getName());
      mainElement.setAttribute("Gender", getGender().name);
      if (includeConditionData) {
         mainElement.setAttribute("UniqueID", String.valueOf(uniqueID));
      }

      Element attrElement = parentDoc.createElement("Attributes");
      for (Attribute att : Attribute.values()) {
         attrElement.setAttribute(att.shortName, String.valueOf(attributes.get(att)));
      }
      mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      mainElement.appendChild(attrElement);

      Element equipmentElement = parentDoc.createElement("Equipment");
      equipmentElement.setAttribute("Armor", (armor == null) ? "" : armor.getName());
      mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      mainElement.appendChild(equipmentElement);

      for (Hand hand : getArms()) {
         Element handElement = parentDoc.createElement("Hand");
         handElement.setAttribute("Name", hand.getName());
         handElement.setTextContent(hand.getHeldThingName());
         equipmentElement.appendChild(parentDoc.createTextNode(newLine + "    "));
         equipmentElement.appendChild(handElement);
      }
      synchronized (equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
            for (Thing beltThing : equipment) {
               if (beltThing != null) {
                  Element beltElement = parentDoc.createElement("Belt");
                  beltElement.setTextContent(beltThing.getName());
                  equipmentElement.appendChild(parentDoc.createTextNode(newLine + "    "));
                  equipmentElement.appendChild(beltElement);
               }
            }
         }
      }
      equipmentElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      if (professionsList.size() > 0) {
         Element professionsElement = parentDoc.createElement("Professions");
         for (Profession profession : professionsList.values()) {
            Element professionElement = parentDoc.createElement("Profession");
            professionElement.setAttribute("Name", profession.getType().getName());
            professionElement.setAttribute("Level", String.valueOf(profession.getLevel()));
            professionElement.setAttribute("proficient", profession.getProficientSkillsAsString());
            professionElement.setAttribute("familiar", profession.getFamiliarSkillsAsString());
            professionsElement.appendChild(parentDoc.createTextNode(newLine + "    "));
            professionsElement.appendChild(professionElement);
         }
         professionsElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(professionsElement);
      }
      if (knownMageSpellsList.size() > 0) {
         Element spellsElement = parentDoc.createElement("Spells");
         for (MageSpell spell : knownMageSpellsList) {
            Element spellElement = parentDoc.createElement("Spell");
            spellElement.setAttribute("Name", spell.getName());
            spellElement.setAttribute("Familiarity", spell.getFamiliarity().getName());
            spellsElement.appendChild(parentDoc.createTextNode(newLine + "    "));
            spellsElement.appendChild(spellElement);
         }
         spellsElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(spellsElement);
      }
      if (advList.size() > 0) {
         Element advantagesElement = parentDoc.createElement("Advantages");
         for (Advantage adv : advList) {
            Element advantageElement = parentDoc.createElement("Advantage");
            advantageElement.setAttribute("Name", adv.getName());
            advantageElement.setAttribute("LevelName", adv.getLevelName());
            advantagesElement.appendChild(parentDoc.createTextNode(newLine + "    "));
            advantagesElement.appendChild(advantageElement);
         }
         advantagesElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(advantagesElement);
         mainElement.appendChild(parentDoc.createTextNode(newLine));
      }
      if (includeConditionData) {
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(condition.getXMLObject(parentDoc, newLine + "  "));
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));

         Element limbElement = parentDoc.createElement("limbs");
         for (Limb limb : limbs.values()) {
            // We only care about limbs that our race says we should have
            if (race.createLimb(limb.limbType) != null) {
               limbElement.appendChild(parentDoc.createTextNode(newLine + "    "));
               limbElement.appendChild(limb.getXMLObject(parentDoc, newLine + "  "));
            }
         }
         for (LimbType type : race.getLimbSet()) {
            if (!limbs.containsKey(type)) {
               // it must have been severed, so list it as such.
               Element severedLimbElement = parentDoc.createElement("Limb");
               severedLimbElement.setAttribute("id", String.valueOf(type.value));
               severedLimbElement.setAttribute("severed", "true");
               limbElement.appendChild(severedLimbElement);
            }
         }
         limbElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(limbElement);
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));

         Element activeElement = parentDoc.createElement("activeData");
         activeElement.setAttribute("teamID", String.valueOf(teamID));
         activeElement.setAttribute("targetID", String.valueOf(targetID));
         activeElement.setAttribute("aimDuration", String.valueOf(aimDuration));
         activeElement.setAttribute("headCount", String.valueOf(headCount));
         activeElement.setAttribute("eyeCount", String.valueOf(eyeCount));
         activeElement.setAttribute("legCount", String.valueOf(legCount));
         activeElement.setAttribute("wingCount", String.valueOf(wingCount));
         activeElement.setAttribute("isBerserking", String.valueOf(isBerserking));
         activeElement.setAttribute("hasInitiativeAndActionsEverBeenInitialized", String.valueOf(hasInitiativeAndActionsEverBeenInitialized));
         activeElement.setAttribute("aiType", ((aiType == null) ? "" : aiType.name));
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(activeElement);

         Element currentSpell = parentDoc.createElement("currentSpell");
         if (this.currentSpell != null) {
            Node spellXml = this.currentSpell.getXMLObject(parentDoc, newLine);
            currentSpell.appendChild(parentDoc.createTextNode(newLine + "    "));
            currentSpell.appendChild(spellXml);
            currentSpell.appendChild(parentDoc.createTextNode(newLine + "  "));
         }
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(currentSpell);

         Element activeSpells = parentDoc.createElement("activeSpells");
         for (Spell activeSpell : activeSpellsList) {
            Node spellXml = activeSpell.getXMLObject(parentDoc, newLine);
            activeSpells.appendChild(parentDoc.createTextNode(newLine + "    "));
            activeSpells.appendChild(spellXml);
         }
         if (activeSpellsList.size() > 0) {
            activeSpells.appendChild(parentDoc.createTextNode(newLine + "  "));
         }
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(activeSpells);
         mainElement.appendChild(parentDoc.createTextNode(newLine));
      }
      return mainElement;
   }

   public boolean serializeFromXmlObject(Node mainElement) {
      clearEquipmentList();
      condition = new Condition(this);
      professionsList.clear();
      knownMageSpellsList = new ArrayList<>();
      advList = new ArrayList<>();

      NamedNodeMap namedNodeMap = mainElement.getAttributes();
      if (namedNodeMap == null) {
         return false;
      }
      name = namedNodeMap.getNamedItem("Name").getNodeValue();
      Gender gender = Gender.MALE;
      Node genderNode = namedNodeMap.getNamedItem("Gender");
      if (genderNode != null) {
         gender = Gender.getByName(genderNode.getNodeValue());
      }
      setRace(namedNodeMap.getNamedItem("Race").getNodeValue(), gender);
      Node uniqueID = namedNodeMap.getNamedItem("UniqueID");
      if (uniqueID != null) {
         this.uniqueID = Integer.parseInt(uniqueID.getNodeValue());
      }

      NodeList children = mainElement.getChildNodes();
      for (int index = 0; index < children.getLength(); index++) {
         Node child = children.item(index);
         NamedNodeMap attributes = child.getAttributes();
         switch (child.getNodeName()) {
            case "Attributes":
               for (Attribute att : Attribute.values()) {
                  Node value = attributes.getNamedItem(att.shortName);
                  if (value == null) {
                     this.attributes.put(att, race.getAttributeMods(att));
                  } else {
                     this.attributes.put(att, Byte.valueOf(value.getNodeValue()));
                  }
               }
               break;
            case "Equipment": {
               armor = Armor.getArmor(attributes.getNamedItem("Armor").getNodeValue(), getRace());
               NodeList grandChildren = child.getChildNodes();
               for (int i = 0; i < grandChildren.getLength(); i++) {
                  Node grandChild = grandChildren.item(i);
                  if (grandChild.getNodeName().equals("Hand")) {
                     NamedNodeMap attr = grandChild.getAttributes();
                     String name = attr.getNamedItem("Name").getNodeValue();
                     for (Hand hand : getArms()) {
                        if (hand.getName().equals(name)) {
                           hand.setHeldThing(Thing.getThing(grandChild.getTextContent(), getRace()), this);
                        }
                     }
                  }
                  if (grandChild.getNodeName().equals("Belt")) {
                     addEquipment(Thing.getThing(grandChild.getTextContent(), true/*allowTool*/, getRace()));
                  }
               }
               break;
            }
            case "Professions":
            case "Spells":
            case "Advantages": {
               // These elements all have children below, that have a similar structure: <??? level="?" Name="?"/>
               NodeList grandChildren = child.getChildNodes();
               for (int i = 0; i < grandChildren.getLength(); i++) {
                  Node grandChild = grandChildren.item(i);
                  NamedNodeMap attr = grandChild.getAttributes();
                  if (attr == null) {
                     continue;
                  }
                  Node nameNode = attr.getNamedItem("Name");
                  if (nameNode == null) {
                     nameNode = attr.getNamedItem("name");
                  }

                  String name = nameNode.getNodeValue();
                  Node levelNode = attr.getNamedItem("Level");
                  if (levelNode == null) {
                     levelNode = attr.getNamedItem("level");
                  }
                  byte level = 0;
                  if ((levelNode != null) && (levelNode.getNodeValue() != null) && (!levelNode.getNodeValue().isEmpty())) {
                     level = Byte.parseByte(levelNode.getNodeValue());
                  }

                  Node levelNameNode = attr.getNamedItem("LevelName");
                  String levelName = ((levelNameNode != null) ? levelNameNode.getNodeValue() : "");

                  switch (grandChild.getNodeName()) {
                     case "Profession":
                        ProfessionType professionType = ProfessionType.getByName(name);
                        if (professionType != null) {
                           String proficientSkillsStr = attr.getNamedItem("proficient").getNodeValue();
                           String familiarSkillsStr = attr.getNamedItem("familiar").getNodeValue();
                           List<SkillType> proficientSkills = Arrays.asList(proficientSkillsStr.split(","))
                                                                    .stream()
                                                                    .map(SkillType::getSkillTypeByName)
                                                                    .filter(Objects::nonNull)
                                                                    .collect(Collectors.toList());
                           Profession profession = new Profession(professionType, proficientSkills, level);
                           profession.setFamiliarSkills(familiarSkillsStr);
                           professionsList.put(professionType, profession);
                        }
                        break;
                     case "Spell":
                        MageSpell spell = MageSpells.getSpell(name);
                        String familiarityStr = attr.getNamedItem("Familiarity").getNodeValue();
                        MageSpell.Familiarity familiarity = null;
                        if (familiarityStr != null) {
                           familiarity = MageSpell.Familiarity.getFamiliarityByName(familiarityStr);
                        }
                        if (spell != null) {
                           if (familiarity != null) {
                              spell.setFamiliarity(familiarity);
                           }
                           knownMageSpellsList.add(spell);
                        }
                        break;
                     case "Advantage":
                        Advantage adv = Advantage.getAdvantage(name);
                        if (adv != null) {
                           if (levelName != null && !levelName.isEmpty()) {
                              adv.setLevelByName(levelName);
                           } else {
                              if (adv.getName().equals(Advantage.CODE_OF_CONDUCT)) {
                                 // code of conduct had 4 elements added to the front of the list of levels
                                 // at the same time as the 'levelName' attribute was added.
                                 // so if we don't have a 'levelName', then the level's have changed:
                                 adv.setLevel((byte) (level + 4));
                              } else {
                                 adv.setLevel(level);
                              }
                           }
                           advList.add(adv);
                        }
                        break;
                  }
               }
               break;
            }
            case "Condition":
               condition.serializeFromXmlObject(child);
               // If we have a severed limb, set it's limb to null.
               for (Wound wound : getWoundsList()) {
                  if (wound.isSeveredArm() || wound.isSeveredLeg() || wound.isSeveredWing()) {
                     limbs.remove(wound.getLimb());
                  }
               }
               break;
            case "activeData":
               NamedNodeMap attr = child.getAttributes();
               if (attr != null) {
                  teamID = Byte.parseByte(attr.getNamedItem("teamID").getNodeValue());
                  targetID = Byte.parseByte(attr.getNamedItem("targetID").getNodeValue());
                  aimDuration = Byte.parseByte(attr.getNamedItem("aimDuration").getNodeValue());
                  headCount = Integer.parseInt(attr.getNamedItem("headCount").getNodeValue());
                  eyeCount = Integer.parseInt(attr.getNamedItem("eyeCount").getNodeValue());
                  legCount = Integer.parseInt(attr.getNamedItem("legCount").getNodeValue());
                  wingCount = Integer.parseInt(attr.getNamedItem("wingCount").getNodeValue());
                  isBerserking = Boolean.parseBoolean(attr.getNamedItem("isBerserking").getNodeValue());
                  Node node = attr.getNamedItem("hasInitiativeAndActionsEverBeenInitialized");
                  if (node != null) {
                     hasInitiativeAndActionsEverBeenInitialized = Boolean.parseBoolean(node.getNodeValue());
                  }
                  Node isAiNode = attr.getNamedItem("isAIPlayer");
                  if (isAiNode != null) {
                     if (Boolean.parseBoolean(isAiNode.getNodeValue())) {
                        aiType = AI_Type.NORM;
                     }
                  }
                  Node AiTypeNode = attr.getNamedItem("aiType");
                  if (AiTypeNode != null) {
                     aiType = AI_Type.getByString(AiTypeNode.getNodeValue());
                  }
               }
               break;
            case "limbs": {
               NodeList grandChildren = child.getChildNodes();
               for (int i = 0; i < grandChildren.getLength(); i++) {
                  Node grandChild = grandChildren.item(i);
                  NamedNodeMap grandChildAttr = grandChild.getAttributes();
                  if (grandChildAttr != null) {
                     if (grandChild.getNodeName().equals("Limb")) {
                        LimbType limbType = LimbType.getByValue(Byte.parseByte(grandChildAttr.getNamedItem("id").getNodeValue()));
                        if (grandChildAttr.getNamedItem("severed") != null) {
                           limbs.remove(limbType);
                        } else {
                           Limb limb = getLimb(limbType);
                           if (limb == null) {
                              limb = race.createLimb(limbType);
                              limbs.put(limbType, limb);
                           }
                           limb.serializeFromXmlObject(grandChild);
                        }
                     }
                  }
               }
               break;
            }
            case "activeSpells": {
               activeSpellsList.clear();
               NodeList grandChildren = child.getChildNodes();
               for (int i = 0; i < grandChildren.getLength(); i++) {
                  Node grandChild = grandChildren.item(i);
                  Spell spell = Spell.serializeFromXmlObject(grandChild);
                  if (spell != null) {
                     activeSpellsList.add(spell);
                  }
               }
               break;
            }
            case "currentSpell": {
               NodeList grandChildren = child.getChildNodes();
               for (int i = 0; i < grandChildren.getLength(); i++) {
                  Node grandChild = grandChildren.item(i);
                  Spell spell = Spell.serializeFromXmlObject(grandChild);
                  if (spell != null) {
                     currentSpell = spell;
                     currentSpell.setCaster(this);
                  }
               }
               break;
            }
         }
      }
      updateWeapons();
      // make sure all the skills required for our mage spells are accounted for in the professions:
      setSpellsList(knownMageSpellsList);
      return true;
   }

   public void setCasterAndTargetFromIDs(List<Character> combatants) {
      for (Spell activeSpell : activeSpellsList) {
         activeSpell.setCasterAndTargetFromIDs(combatants);
      }
      if (currentSpell != null) {
         currentSpell.setCasterAndTargetFromIDs(combatants);
      }
   }

   public boolean serializeToFile(File destFile) {
      Document charDoc = getXmlObject(false/*includeConditionData*/);

      try (FileOutputStream fos = new FileOutputStream(destFile)) {
         DOMImplementationRegistry reg = DOMImplementationRegistry.newInstance();
         DOMImplementationLS impl = (DOMImplementationLS) reg.getDOMImplementation("LS");
         LSSerializer serializer = impl.createLSSerializer();
         LSOutput lso = impl.createLSOutput();
         lso.setByteStream(fos);
         serializer.write(charDoc, lso);
      } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e) {
         e.printStackTrace();
      }
      return false;
   }

   public boolean serializeFromFile(File sourceFile) {
      if (sourceFile.getAbsolutePath().endsWith(".xml")) {
         Document charDoc = parseXmlFile(sourceFile, false/*validating*/);
         if (charDoc != null) {
            return serializeFromXmlObject(charDoc.getDocumentElement());
         }
      }
      return false;
   }

   // Parses an XML file and returns a DOM document.
   // If validating is true, the contents is validated against the DTD
   // specified in the file.
   public static Document parseXmlFile(File sourceFile, boolean validating) {
      try {
         // Create a builder factory
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         factory.setValidating(validating);

         // Create the builder and parse the file
         return factory.newDocumentBuilder().parse(sourceFile);
      } catch (SAXException e) {
         // A parsing error occurred; the xml input is not valid
      } catch (ParserConfigurationException | IOException e) {
      }
      return null;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof Character)) {
         return false;
      }
      return serializeToString().equals(((Character) obj).serializeToString());
   }

   //   // Since we override equals, we need to override hashCode():
   //   @Override
   //   public int hashCode() {
   //      return serializeToString().hashCode();
   //   }

   public List<Wound> getWoundsList() {
      return condition.getWoundsList();
   }

   public void applyWound(Wound wound, Arena arena) {
      Character origChar = clone(); // used to notify watchers if something changes, like a severed limb, etc

      wound.setInvalidReason(placeWound(wound));
      Wound modifiedWound = modifyWoundFromDefense(wound);
      if (modifiedWound.getLevel() >= 0) {
         // getting wounded always makes you lose aim
         aimDuration = 0;
      }
      ArenaLocation loc = null;
      if (arena != null) {
         if (modifiedWound.getLocation() == Location.WEAPON) {
            for (Hand hand : getArms()) {
               Weapon thing = hand.getWeapon(this);
               if (thing != null) {
                  loc = condition.getLimbLocation(hand.limbType, arena.getCombatMap());
               }
            }
         } else {
            LimbType limbType = modifiedWound.getLimb();
            if (limbType == null) {
               DebugBreak.debugBreak("Null location");
               limbType = modifiedWound.getLimb();
            }
            loc = condition.getLimbLocation(limbType, arena.getCombatMap());
         }
      }
      if (modifiedWound.isSeveredArm() || modifiedWound.isSeveredLeg() || modifiedWound.isSeveredWing()) {
         if (loc != null) {
            loc.addThing(race.createSeveredLimb(modifiedWound.getLimb()));
         }
      }

      if (modifiedWound.isSeveredLeg()) {
         legCount--;
      }
      if (modifiedWound.isBlinding()) {
         eyeCount--;
      }
      if (modifiedWound.isDecapitating()) {
         headCount--;
      }
      if (modifiedWound.isSeveredWing()) {
         wingCount--;
      }

      // If the wound severed an arm, drop anything carried by that arm, even if its not a Weapon (maybe a shield)
      if (modifiedWound.getLocation() == Wound.Location.ARM) {
         if (modifiedWound.isDropWeapon() || modifiedWound.isSeveredArm()) {
            Wound.Pair armPair = modifiedWound.getLocationPair();

            boolean twoHandedWeapon = false;
            Limb rightArm = limbs.get(LimbType.get(Location.ARM, Side.RIGHT, armPair));
            Limb leftArm = limbs.get(LimbType.get(Location.ARM, Side.LEFT, armPair));
            Weapon rightWeapon = (rightArm == null) ? null : rightArm.getWeapon(this);
            if (rightWeapon != null) {
               twoHandedWeapon = rightWeapon.isOnlyTwoHanded();
            }
            Weapon leftWeapon = (leftArm == null) ? null : leftArm.getWeapon(this);
            if (leftWeapon != null) {
               if (leftWeapon.isOnlyTwoHanded()) {
                  twoHandedWeapon = true;
               }
            }
            Thing thingDropped = null;
            if ((modifiedWound.getLocationSide() == Wound.Side.RIGHT) || twoHandedWeapon) {
               thingDropped = (rightArm == null) ? null : rightArm.dropThing();
            } else if ((modifiedWound.getLocationSide() == Wound.Side.LEFT)) {
               thingDropped = (leftArm == null) ? null : leftArm.dropThing();
            } else if ((modifiedWound.getLocationSide() != Wound.Side.RIGHT) && (modifiedWound.getLocationSide() != Wound.Side.LEFT)) {
               // A wound to the scapula can also cause us to drop our weapon
               // but the location is not a HAND, so check for this case.
               for (Limb limb : limbs.values()) {
                  if (limb.getHeldThing() instanceof Weapon) {
                     thingDropped = limb.dropThing();
                     break;
                  }
               }
            }
            if ((thingDropped != null) && thingDropped.isReal() && (loc != null)) {
               loc.addThing(thingDropped);
            }
         }
      } else if (modifiedWound.isDropWeapon()) {
         // This wound causes the target to drop its weapon, but is not targeted on an arm.
         // Drop the first item we find on any Hand.
         for (Limb limb : getLimbs()) {
            if (limb instanceof Hand) {
               Thing weap = limb.getHeldThing();
               if ((weap != null) && weap.isReal() && (loc != null)) {
                  Thing thingDropped = limb.dropThing();
                  loc.addThing(thingDropped);
                  break;
               }
            }
         }
      }

      if (modifiedWound.isUnreadyWeapon()) {
         for (Limb limb : limbs.values()) {
            Weapon weap = limb.getWeapon(this);
            if ((weap != null) && (weap.isReal())) {
               limb.setActionsNeededToReady((byte) (limb.getActionsNeededToReady() + 1));
            }
         }
      }
      for (Limb limb : limbs.values()) {
         limb.applyWound(modifiedWound);
      }
      condition.applyWound(modifiedWound, arena, this);

      byte newPain = condition.getPenaltyPain();
      StringBuilder sb = new StringBuilder();
      if ((newPain > 0) && (modifiedWound.getPain() > 0)) {
         if (isBerserker() && !isBerserking()) {
            byte iq = getAttributeLevel(Attribute.Intelligence);
            DiceSet berserkSaveDice = Rules.getDice(iq, (byte) 2/*action*/, Attribute.Intelligence, RollType.BERSERK_RESISTANCE);
            berserkSaveDice = adjustDieRoll(berserkSaveDice, RollType.BERSERK_RESISTANCE, null/*target*/);
            String rollMessage = getName() + ", because of your new pain level (" + newPain +
                                 "), you must roll your IQ (" + iq + ") + d10± against a TN of " +
                                 newPain + " to avoid going berserk!";
            int diceRoll = berserkSaveDice.roll(true/*allowExplodes*/, this,
                                                RollType.BERSERK_RESISTANCE, rollMessage);
            sb.append(getName()).append("'s pain causes a chance that he goes berserk.");
            sb.append(" He rolls 2-actions IQ (").append(berserkSaveDice);
            sb.append("), rolling ").append(berserkSaveDice.getLastDieRoll());
            sb.append(" = ").append(diceRoll);
            if (berserkSaveDice.lastRollRolledAllOnes()) {
               sb.append(", which is all ones, so ").append(getName()).append(" automatically goes berserk!");
               isBerserking = true;
            } else if (diceRoll >= newPain) {
               sb.append(", which is equal to, or above, the pain level of ").append(newPain);
               sb.append(", so ").append(getName()).append(" does not go berserk.");
            } else {
               sb.append(", which is below the pain level of ").append(newPain);
               sb.append(", so ").append(getName()).append(" goes berserk!");
               isBerserking = true;
            }

         }
         // TODO: implement that damage caused to a subject of a pacify spell will allow them to re-resist the spell.
         //         for (Spell spell : activeSpellsList) {
         //            if (spell instanceof SpellPacify) {
         //               IResistedSpell pacifySpell = (SpellPacify) spell;
         //               if (pacifySpell.resistAgain()) {
         //                  activeSpellsList.remove(spell);
         //               }
         //            }
         //         }
      }
      if (currentSpell != null) {
         byte currentPain = condition.getPenaltyPain();
         boolean spellLost = false;
         if (!condition.isConscious() || condition.isCollapsed()) {
            sb = new StringBuilder();
            sb.append(getName()).append("'s '").append(currentSpell.getName());
            sb.append("' spell, is lost!");
            spellLost = true;
         }
         if ((currentPain > 0) && (modifiedWound.getPain() > 0)) {
            byte toughness = getAttributeLevel(Attribute.Toughness);
            DiceSet magicSaveDice = Rules.getDice(toughness, (byte) 2/*action*/, Attribute.Toughness, RollType.PAIN_CONCENTRATION);
            magicSaveDice = adjustDieRoll(magicSaveDice, RollType.PAIN_CONCENTRATION, null/*target*/);
            String rollMessage = getName() + ", because of your new pain level (" + currentPain +
                                 "), you must roll your TOU (" + toughness + ") + d10± against a TN of " +
                                 currentPain + " to avoid losing your " +
                                 currentSpell.getName() + " spell.";
            int diceRoll = magicSaveDice.roll(true/*allowExplodes*/, this,
                                              RollType.PAIN_CONCENTRATION, rollMessage);

            sb.append("<br/>");
            sb.append(getName()).append("'s wound raises ").append(getHisHer()).append(" pain level to ").append(currentPain);
            sb.append("<br/>Since he has a '").append(currentSpell.getName());
            sb.append("' spell, he must roll 2-actions against ").append(getHisHer()).append(" TOU of ").append(toughness);
            sb.append(" (").append(magicSaveDice).append("), to avoid losing the spell. He rolls ");
            sb.append(magicSaveDice.getLastDieRoll()).append(" for a total of ").append(diceRoll).append(".<br/>");
            if (magicSaveDice.lastRollRolledAllOnes()) {
               sb.append(" The roll is all 1s, which always fails, so the spell is lost!");
               spellLost = true;
            } else if (currentPain > diceRoll) {
               sb.append(" The roll fails, so the spell is lost!");
               spellLost = true;
            } else {
               sb.append(" The roll succeeds, so the spell is not lost.");
            }
         }
         if (spellLost) {
            Wound newWound = currentSpell.getFailureBurnWound();
            currentSpell = null;
            if (newWound != null) {
               sb.append("<br/>Since the spell was over-powered, ").append(getName());
               sb.append(" takes an additional ").append(newWound.getWounds()).append(" wounds and ");
               sb.append(newWound.getPain()).append(" points of pain.");
               condition.applyWound(newWound, arena, this);
            }
         }
      }
      arena.sendMessageTextToAllClients(sb.toString(), false/*popUp*/);

      // notify any watchers of any changes to this character
      ObjectChanged changeNotif = new ObjectChanged(origChar, this);
      notifyWatchers(origChar, this, changeNotif, null/*skipList*/, null/*diag*/);
   }

   public void setCondition(Condition newCondition) {
      condition = newCondition;
   }

   public void applyAction(RequestAction action, Arena arena) throws BattleTerminatedException {
      Character originalCopy = clone();
      if (action.isEquipUnequip()) {
         RequestEquipment reqEqu = action.equipmentRequest;
         if (reqEqu == null) {
            DebugBreak.debugBreak();
            reqEqu = (RequestEquipment) action.getNextQuestion(this, arena.getCombatants(), arena);
         }
         if (reqEqu.isDrop()) {
            Limb limb = limbs.get(action.getLimb());
            if (limb != null) {
               Thing thingDropped = limb.dropThing();
               if ((thingDropped != null) && (thingDropped.isReal())) {
                  ArenaLocation loc = condition.getLimbLocation(limb.limbType, arena.getCombatMap());
                  loc.addThing(thingDropped);
               }
            }
         } else if (reqEqu.isSheath()) {
            Limb limb = limbs.get(action.getLimb());
            if (limb != null) {
               Thing thingSheathed = limb.dropThing();
               addEquipment(thingSheathed);
            }
         } else if (reqEqu.isReady() || reqEqu.isApply()) {
            synchronized (equipment) {
               try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
                  if (reqEqu.isReady()) {
                     String equName = reqEqu.getEquToReady();
                     for (Thing equ : equipment) {
                        if (equ.getName().equals(equName)) {
                           Limb hand = getLimb(reqEqu.getLimb());
                           if (hand.setHeldThing(equ, this)) {
                              equipment.remove(equ);
                           }
                           break;
                        }
                     }
                  } else if (reqEqu.isApply()) {
                     String equName = reqEqu.getEquToApply();
                     for (Thing equ : equipment) {
                        if (equ.getName().equals(equName)) {
                           if (equ.apply(this, arena)) {
                              equipment.remove(equ);
                           }
                           break;
                        }
                     }
                  }
               }
            }
         }
      } else if (action.isChangeTargets()) {
         if ((action.targetPriorities == null) && (action.targetSelection == null)) {
            DebugBreak.debugBreak();
         } else {
            if (action.targetSelection != null) {
               setTarget(action.targetSelection.getAnswerID());
            } else if (action.targetPriorities != null) {
               List<Integer> orderedTargetIds = action.targetPriorities.getOrderedTargetIds();
               setTargetPriorities(orderedTargetIds);
            }
         }
      }

      // spells
      if (action.isBeginSpell()) {
         int spellIndex = action.spellTypeSelectionRequest.spellSelectionRequest.getAnswerID();
         if (RequestSpellTypeSelection.SPELL_TYPE_MAGE.equals(action.spellTypeSelectionRequest.getAnswer())) {
            currentSpell = knownMageSpellsList.get(spellIndex).clone();
         } else {
            String deityName = action.spellTypeSelectionRequest.getAnswer();
            Deity deity = Deity.getByName(deityName);
            List<PriestSpell> spells = PriestSpell.getSpellsForDeity(deity, getAffinity(deity), true/*addNullBetweenGroups*/);
            currentSpell = spells.get(spellIndex).clone();
         }
         currentSpell.setCaster(this);
         currentSpell.beginIncantation();
         if (currentSpell.getIncantationRoundsRequired() > 0) {
            currentSpell.incant();
         } else {
            if (!(currentSpell instanceof InstantaneousMageSpell)) {
               // If the spell level is so high that it costs no actions
               // to make the incantation, then since we are spending an action anyway,
               // add a point of energy to the spell.
               currentSpell.channelEnergy((byte) 1);
            }
            currentSpell.maintainSpell();
         }
      } else if (action.isPrepareInateSpell()) {
         IRequestOption[] actionOptions = action.getReqOptions();
         IRequestOption actionOption = actionOptions[action.getAnswerIndex()];
         if (actionOption instanceof RequestActionOption) {
            RequestActionOption reqActOpt = (RequestActionOption) actionOption;
            int spellIndex = reqActOpt.getValue().getIndexOfPrepareInateSpell();
            currentSpell = race.getInateSpells().get(spellIndex).clone();
            currentSpell.maintainSpell();
            currentSpell.setCaster(this);
         }
      } else if (action.isContinueSpell()) {
         currentSpell.incant();
      } else if (action.isChannelEnergy()) {
         Wound burn = currentSpell.channelEnergy(action.getActionsUsed());
         if (burn != null) {
            condition.applyWound(burn, arena, this);
            String message = getName() + "'s over-powers a spell causing " + burn.getPain() + " points of pain.";
            arena.sendMessageTextToAllClients(message, false/*popUp*/);
         }
      } else if (action.isMaintainSpell()) {
         currentSpell.maintainSpell();
      } else if (action.isCompleteSpell()) {
         if (currentSpell instanceof PriestSpell) {
            PriestSpell priestSpell = (PriestSpell) currentSpell;
            IRequestOption answer = action.answer();
            if (answer instanceof RequestActionOption) {
               RequestActionOption reqActOpt = (RequestActionOption) answer;
               priestSpell.setPower((byte) (reqActOpt.getValue().getIndexOfCompletePriestSpell() + 1));
            }
         }
         currentSpell.completeSpell();
      } else if (action.isDiscardSpell()) {
         currentSpell.discardSpell();
         currentSpell = null;
      }

      if (action.isTargetEnemy()) {
         if (targetID != action.targetID) {
            targetID = action.targetID;
            aimDuration = 0;
         }
         aimDuration++;
      } else {
         // If we aren't actively targeting the enemy
         // this round, then we lose our aim duration,
         // however, don't clear this on the round that
         // we attack the enemy.
         if (!action.isAttack()) {
            aimDuration = 0;
         }
      }
      if (action.isAttack()) {
         if (targetID != action.targetID) {
            targetID = action.targetID;
            aimDuration = 0;
         }
      }
      for (Limb limb : limbs.values()) {
         limb.applyAction(action, getAttributeLevel(Attribute.Strength), this, arena);
      }
      condition.applyAction(action, this, currentSpell, arena);
      lastAction = action;
      //if (!originalCopy.equals(this)) {
      ObjectChanged changeNotif = new ObjectChanged(originalCopy, this);
      notifyWatchers(originalCopy, this, changeNotif, null/*skipList*/, null/*diag*/);
      //}
   }

   public byte getAffinity(Deity deity) {
      Advantage adv = getAdvantage(Advantage.DIVINE_AFFINITY_ + deity.getName());
      if (adv != null) {
         return (byte) (adv.getLevel() + 1);
      }
      return 0;
   }

   @Override
   public void applyHoldMaintenance(RequestGrapplingHoldMaintain holdMaintainenance, Arena arena) {
      condition.applyHoldMaintenance(holdMaintainenance);
   }

   public String applyDefense(RequestDefense defense, Arena arena) {
      if (defense.getAnswerID() == -1) {
         DebugBreak.debugBreak("Applying a defense with actionID == -1");
         // Since we don't know what to do, do nothing!
         return null;
      }
      if (defense.getActionsUsed() > 0) {
         // taking an active defense makes us lose aim
         aimDuration = 0;
      }
      for (Limb limb : limbs.values()) {
         limb.applyDefense(defense, getAttributeLevel(Attribute.Strength), this);
      }
      if (defense.isRetreat()) {
         if (holdTarget != null) {
            arena.sendMessageTextToAllClients("Because " + getName() + " retreated, " + getHeShe() + " releases " + getHisHer() + "hold of "
                                              + holdTarget.getName(), false/*popup*/);
            releaseHold();
         }
      }
      condition.applyDefense(defense);
      return null;
   }

   public void resetSpellPoints() {
      if (condition == null) {
         return;
      }
      List<Deity> deities = getPriestDeities();
      Advantage ma = getAdvantage(Advantage.MAGICAL_APTITUDE);
      byte affinity = 0;
      byte divinePower = 0;
      if (!deities.isEmpty()) {
         affinity = getAffinity(deities.get(0));
         Advantage powerAdv = getAdvantage(Advantage.DIVINE_POWER);
         if (powerAdv != null) {
            divinePower = (byte) (powerAdv.getLevel() + 1);
         }
      }
      condition.resetSpellPoints(ma, affinity, divinePower);
   }

   public byte getAvailableMovement(boolean movingEvasively) {
      return condition.getMovementAvailableThisRound(movingEvasively);
   }

   public boolean hasMovedThisRound() {
      return condition.hasMovedThisRound();
   }

   public void setMovingEvasively(boolean movingEvasively) {
      condition.setMovingEvasively(movingEvasively);
   }

   public void applyMovementCost(byte movementCost) {
      condition.applyMovementCost(movementCost);
   }

   public void setClientProxy(ClientProxy proxy, CombatMap map, Diagnostics diag) {
      // If we have a client proxy, then it is a watcher of the mapWatcher
      mapWatcher = new ArenaLocationBook(this, map, diag);
      mapWatcher.registerAsWatcher(proxy, diag);
      // and also as a watcher of the Character itself.
      registerAsWatcher(proxy, diag);
   }

   public AI_Type getAIType() {
      return aiType;
   }

   public boolean isAIPlayer() {
      return (aiType != null);
   }

   public void setAIType(AI_Type AItype) {
      this.aiType = AItype;
   }

   public boolean setHeadLocation(ArenaLocation headLocation, Facing facing, CombatMap map, Diagnostics diag) {
      if (condition.setHeadLocation(this, headLocation, facing, map, diag)) {
         map.updateCombatant(this, true/*checkTriggers*/);
         return true;
      }
      return false;
   }

   //   public void setLocations(List<ArenaLocation> newLocations, byte newFacing, CombatMap map, Diagnostics diag)
   //   {
   //      Orientation originalOrientation = (Orientation) condition.getOrientation().clone();
   //      if (condition.setLocations(newLocations, newFacing, map, diag)) {
   //         Character origChar = (Character) clone();
   //         origChar.condition.setOrientation(originalOrientation);
   //         ObjectChanged changeNotif = new ObjectChanged(origChar, this);
   //         notifyWatchers(origChar, this, changeNotif, null/*skipList*/, diag);
   //      }
   //   }

   public void setOrientation(Orientation destination, Diagnostics diag) {
      if (destination.equals(condition.getOrientation())) {
         return;
      }
      Character origChar = clone();
      condition.setOrientation(destination);
      ObjectChanged changeNotif = new ObjectChanged(origChar, this);
      notifyWatchers(origChar, this, changeNotif, null/*skipList*/, diag);
   }

   public boolean isMoveComplete() {
      return condition.isMoveComplete();
   }

   public void setMoveComplete() {
      condition.setMoveComplete();
   }

   public boolean canAdvance() {
      return condition.canAdvance();
   }

   public double getWeightArmor() {
      if (armor != null) {
         return armor.getAdjustedWeight();
      }
      return 0;
   }

   public double getWeightEquipment() {
      double weight = 0;
      for (Limb limb : limbs.values()) {
         Thing thing = limb.getHeldThing();
         if (thing != null) {
            weight += thing.getAdjustedWeight();
         }
      }
      synchronized (equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
            for (Thing thing : equipment) {
               if (thing != null) {
                  weight += thing.getAdjustedWeight();
               }
            }
         }
      }
      return weight;
   }

   public double getWeightCarried() {
      double armorLbs = getWeightArmor();
      double equipLbs = getWeightEquipment();
      return Math.round((armorLbs + equipLbs) * 10000) / 10000.0;
   }

   public int getTotalCost() {
      int cost = 0;
      if (armor != null) {
         cost += armor.getCost();
      }

      for (Limb limb : limbs.values()) {
         Thing thing = limb.getHeldThing();
         if (thing != null) {
            cost += thing.getCost();
         }
      }
      synchronized (equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
            for (Thing thing : equipment) {
               if (thing != null) {
                  cost += thing.getCost();
               }
            }
         }
      }
      // make sure we haven't exceeded our wealth level
      computeWealth(cost);
      return cost;
   }

   public RequestEquipment getEquipmentRequest() {
      RequestEquipment req = new RequestEquipment();
      boolean uncrippledArm = false;
      for (Hand hand : getArms()) {
         if (!hand.isEmpty()) {
            req.addDropSheathOptions(hand, getActionsAvailableThisRound(false/*usedForDefenseOnly*/));
         }
         if (!uncrippledArm) {
            if (!hand.isCrippled()) {
               uncrippledArm = true;
            }
         }
      }
      if (uncrippledArm) {
         List<Thing> equipment = getEquipment();
         if (equipment.size() > 0) {
            List<Thing> doneEqu = new ArrayList<>();
            for (Thing thing : equipment) {
               // If the character has multiple items of the same type, don't
               // re-ask the same questions for each one.
               if (doneEqu.contains(thing)) {
                  continue;
               }
               doneEqu.add(thing);

               if (thing != null) {
                  Object crippled = new Object();
                  for (Pair pair : new Pair[]{Pair.FIRST, Pair.SECOND, Pair.THIRD}) {
                     if ((race.getArmCount() == 2) && (pair == Pair.SECOND)) {
                        break;
                     }
                     if ((race.getArmCount() == 4) && (pair == Pair.THIRD)) {
                        break;
                     }
                     Limb leftHandOfSet = limbs.get(LimbType.get(Location.ARM, Side.LEFT, pair));
                     Limb rightHandOfSet = limbs.get(LimbType.get(Location.ARM, Side.RIGHT, pair));
                     Object leftHandObj = (leftHandOfSet == null) ? crippled : leftHandOfSet.getHeldThing();
                     Object rightHandObj = (rightHandOfSet == null) ? crippled : rightHandOfSet.getHeldThing();
                     if (rightHandObj != null) {
                        if (rightHandObj instanceof Weapon) {
                           Weapon weap = (Weapon) rightHandObj;
                           if (weap.isOnlyTwoHanded()) {
                              leftHandObj = rightHandObj;
                           }
                        }
                     }
                     if ((leftHandOfSet != null) && leftHandOfSet.isCrippled()) {
                        leftHandObj = crippled;
                     }
                     if ((rightHandOfSet != null) && rightHandOfSet.isCrippled()) {
                        rightHandObj = crippled;
                     }
                     // If either arm is crippled, consider if this can be equipped:
                     Limb limb = rightHandOfSet;
                     boolean enabled = true;
                     if (thing instanceof Shield) {
                        if (leftHandObj != null) {
                           if (rightHandObj != null) {
                              enabled = false;
                           }
                        } else {
                           limb = leftHandOfSet;
                        }
                     } else {
                        if (rightHandObj != null) {
                           if (leftHandObj != null) {
                              enabled = false;
                           } else {
                              limb = leftHandOfSet;
                           }
                        }
                        if (thing instanceof Weapon) {
                           // exclude weapons we can't ready.
                           Weapon weap = (Weapon) thing;
                           if (weap.isOnlyTwoHanded()) {
                              // two-handed weapons need both hands free
                              if ((rightHandObj != null) || (leftHandObj != null)) {
                                 enabled = false;
                              }
                           }
                        }
                     }
                     if (limb != null) {
                        req.addReadyOption(thing.getName(), equipment.indexOf(thing), limb.limbType, enabled);
                     }
                  }
                  if (thing.canBeApplied()) {
                     req.addApplyOption(thing.getName(), equipment.indexOf(thing), LimbType.HAND_RIGHT, true/*enabled*/);
                  }
               }
            }
         }
      }
      req.addOption(SyncRequest.OPT_CANCEL_ACTION, "Cancel", true/*enabled*/);
      req.setMessage(getName() + ", what would you like to equip?");
      req.setDefaultOption(0);
      return req;
   }

   public byte getPenaltyToUseArm(Limb limb, boolean includeWounds, boolean includePain) {
      if (limb.isCrippled()) {
         return -1;
      }
      byte penalty = getHandPenalties(limb.limbType, null/*SkillType*/); // TODO: get SkillType

      if (includeWounds) {
         penalty += limb.getWoundPenalty();
      }
      Weapon weap = limb.getWeapon(this);
      if (weap != null) {
         // TODO: what about bastard swords and katanas that are wielded with two hands?
         if (weap.isOnlyTwoHanded()) {
            Limb otherHand = limbs.get(limb.limbType.getPairedType());
            if (otherHand != null) {
               if (otherHand.isCrippled()) {
                  return -1;
               }
               penalty += otherHand.getWoundPenalty();
            }
         }
      }
      if (includeWounds) {
         penalty += getCondition().getWounds();
      }
      if (includePain) {
         penalty += getCondition().getPenaltyPain();
      }
      return penalty;
   }

   public Limb getLimb(LimbType limbType) {
      if (limbType == null) {
         DebugBreak.debugBreak();
         return null;
      }
      return limbs.get(limbType);
   }

   public List<Hand> getArms() {
      return limbs.values()
                  .stream()
                  .filter(l -> l.limbType.isHand())
                  .map(o -> (Hand) o)
                  .collect(Collectors.toList());
   }

   public void updateWeapons() {
      for (Hand hand : getArms()) {
         Weapon weap = hand.getWeapon(this);
         if (weap != null) {
            // Is our left hand free to wield the primary weapon with two hands?
            Hand otherLimb = (Hand) (limbs.get(hand.limbType.getPairedType()));
            boolean twoHandedAvailable = ((otherLimb != null) && otherLimb.isEmpty() && !otherLimb.isCrippled());

            int bestSkillLevel = -1;
            // select highest skill level for attack
            for (byte i = 0; i < weap.attackStyles.length; i++) {
               if ((weap.attackStyles[i].getHandsRequired() == 1) || twoHandedAvailable) {
                  int styleSkillLevel = getSkillLevel(weap.attackStyles[i], false/*adjustForPain*/, null/*useHand*/, false/*sizeAdjust*/,
                                                      true/*adjustForEncumbrance*/, true/*adjustForHolds*/);
                  if (bestSkillLevel < styleSkillLevel) {
                     hand.setAttackStyle(i);
                     bestSkillLevel = styleSkillLevel;
                  }
               }
            }
            bestSkillLevel = -1;
            // select highest skill level for defense
            for (byte i = 0; i < weap.parryStyles.length; i++) {
               if ((weap.parryStyles[i].getHandsRequired() == 1) || twoHandedAvailable) {
                  int styleSkillLevel = getSkillLevel(weap.parryStyles[i], false/*adjustForPain*/, null/*useHand*/, false/*sizeAdjust*/,
                                                      true/*adjustForEncumbrance*/, true/*adjustForHolds*/);
                  if (bestSkillLevel < styleSkillLevel) {
                     hand.setParryStyle(i);
                     bestSkillLevel = styleSkillLevel;
                  }
               }
            }
         }
      }
      refreshDefenses();
   }

   public boolean pickupObject(Object thing) {
      // If all hands have something
      if (!canPickup(thing)) {
         return false;
      }

      if (thing instanceof Weapon) {
         for (Hand hand : getArms()) {
            if (hand.getLocationSide() == Side.RIGHT) {
               if (hand.setHeldThing((Thing) thing, this)) {
                  return true;
               }
            }
         }
      }
      if (thing instanceof Thing) {
         // put non-weapons (like shields or potions) in the off hand:
         for (Hand hand : getArms()) {
            if (hand.getLocationSide() == Side.LEFT) {
               if (hand.setHeldThing((Thing) thing, this)) {
                  return true;
               }
            }
         }
         // If we couldn't put it in an off-hand, try a primary hand:
         for (Hand hand : getArms()) {
            if (hand.getLocationSide() == Side.RIGHT) {
               if (hand.setHeldThing((Thing) thing, this)) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   public short getWeaponMaxRange(boolean allowRanged, boolean onlyChargeTypes) {
      Optional<Short> mxRange = limbs.values()
                                    .stream()
                                    .filter(l -> l.getActionsNeededToReady() == 0)
                                    .map(l -> l.getWeapon(this))
                                    .filter(Objects::nonNull)
                                    .map(w -> w.getWeaponMaxRange(allowRanged, onlyChargeTypes, this))
                                    .max(Short::compareTo);
      short maxRange = (mxRange.isPresent() ? mxRange.get() : 0);

      if (allowRanged) {
         if (currentSpell != null) {
            short spellRange = currentSpell.getMaxRange(this);
            if (spellRange > maxRange) {
               maxRange = spellRange;
            }
         } else {
            mxRange = knownMageSpellsList.stream()
                                         .map(s -> s.getMaxRange(this))
                                         .max(Short::compareTo);
            if (mxRange.isPresent() && mxRange.get() > maxRange) {
               maxRange = mxRange.get();
            }
         }
      }
      return maxRange;
   }

   public int getUnseveredArmCount() {
      return (int) limbs.values()
                        .stream()
                        .filter(l -> l instanceof Hand && !l.isSevered())
                        .count();
   }

   public int getUncrippledArmCount(boolean reduceCountForTwoHandedWeaponsHeld) {
      int availableHands = 0;
      for (Hand hand : getArms()) {
         if (!hand.isCrippled()) {
            Thing heldThing = hand.getHeldThing();
            if ((heldThing == null) || (!heldThing.isReal())) {
               availableHands++;
            } else {
               if (reduceCountForTwoHandedWeaponsHeld) {
                  if (heldThing instanceof Weapon) {
                     Weapon weap = (Weapon) heldThing;
                     if (weap.isOnlyTwoHanded()) {
                        availableHands--;
                     }
                  }
               }
            }
         }
      }
      return availableHands;
   }

   public int getLegCount() {
      return legCount;
   }

   public int getEyeCount() {
      return eyeCount;
   }

   public int getHeadCount() {
      return headCount;
   }

   public String placeWound(Wound wound) {
      if (wound.getLocation() == Wound.Location.LIMB) {
         boolean targetHasArms = getUnseveredArmCount() > 0;
         boolean targetHasLegs = getLegCount() > 0;
         boolean targetHasWings = hasWings();
         if (targetHasArms) {
            // has arms
            if (targetHasLegs) {
               // has arms, and legs. May have wings
               if (targetHasWings) {
                  // has all three
                  int loc = (int) (CombatServer.random() * 3);
                  switch (loc) {
                     case 0:
                        wound.setLocation(Location.ARM);
                        break;
                     case 1:
                        wound.setLocation(Location.LEG);
                        break;
                     case 2:
                        wound.setLocation(Location.WING);
                        break;
                  }
               } else {
                  // has arms and wings (no legs)
                  wound.setLocation((CombatServer.random() > .5) ? Location.LEG : Location.ARM);
               }
            } else {
               if (targetHasWings) {
                  // has arms and wings (no legs)
                  wound.setLocation((CombatServer.random() > .5) ? Location.WING : Location.ARM);
               } else {
                  // has only arms
                  wound.setLocation(Location.ARM);
               }
            }
         } else {
            if (targetHasLegs) {
               // has no arms, does have legs, and maybe wings
               if (targetHasWings) {
                  // legs and wings (no arms)
                  wound.setLocation((CombatServer.random() > .5) ? Location.WING : Location.LEG);
               } else {
                  // only legs
                  wound.setLocation(Location.LEG);
               }
            } else {
               if (!targetHasWings) {
                  // has no limbs!
                  throw new WoundCantBeAppliedToTargetException("target has no arms, legs or wings");
               }
               // has no arms or legs (only wings)
               wound.setLocation(Location.WING);
            }
         }
      } else if ((wound.getLocation() == Location.ARM) && hasWings()) {
         // a general arm hit, could hit a wing instead
         wound.setLocation((CombatServer.random() > .5) ? Location.WING : Location.ARM);
      }

      // make sure we don't sever a limb that is already gone.
      if (wound.getLocation() == Location.ARM) {
         if (wound.getLocationSide() == Side.ANY) {
            int count = getUnseveredArmCount();
            // pick a random arm to sever
            int id = (int) (CombatServer.random() * count);
            for (Hand hand : getArms()) {
               if (!hand.isSevered()) {
                  if (id-- == 0) {
                     wound.setLocationSide(hand.getLocationSide());
                     wound.setLocationPair(hand.getLocationPair());
                     break;
                  }
               }
            }
         }
         if (wound.getLocationSide() == Side.ANY) {
            return "target has no arms";
         }
      } else if (wound.getLocation() == Location.LEG) {
         if (!setWoundLocationSidePair(legCount, race.getLegCount(), wound)) {
            return "target has no legs";
         }
      } else if (wound.getLocation() == Location.EYE) {
         if (!setWoundLocationSidePair(eyeCount, race.getEyeCount(), wound)) {
            return "target has no eyes";
         }
      }
      // return null on success
      return null;
   }

   private static boolean setWoundLocationSidePair(int curCount, int maxCount, Wound wound) {
      if (curCount == 0) {
         return false;
      }
      // Right or left?
      if ((curCount % 2) == 1) {
         wound.setLocationSide(Wound.Side.LEFT);
      } else {
         wound.setLocationSide(Wound.Side.RIGHT);
      }

      // Which pair?
      int itemsLost = maxCount - curCount;
      if ((itemsLost / 2) == 0) {
         wound.setLocationPair(Wound.Pair.FIRST);
      }
      if ((itemsLost / 2) == 1) {
         wound.setLocationPair(Wound.Pair.SECOND);
      }
      if ((itemsLost / 2) == 2) {
         wound.setLocationPair(Wound.Pair.THIRD);
      }
      return true;
   }

   public List<String> getPropertyNames() {
      List<String> props = race.getRacialPropertiesList();
      List<Advantage> advs = race.getAdvantagesList();
      for (Advantage adv : advs) {
         props.add(adv.getName());
      }
      for (Advantage adv : advList) {
         if (!props.contains(adv.getName())) {
            props.add(adv.getName());
         }
      }
      return props;
   }

   public Wound alterWound(Wound wound, StringBuilder alterationExplanationBuffer) {
      // This method allow us to alter a wound based on the specifics of the
      // target, based on protection spells, race details or whatever.
      // return 'null' if no altering occurs.
      wound = race.alterWound(wound, alterationExplanationBuffer);

      boolean noPain = hasAdvantage(Advantage.NO_PAIN) && (wound.getPain() > 0);
      boolean noBlood = hasAdvantage(Advantage.UNDEAD) && (wound.getBleedRate() > 0);
      if (noPain || noBlood) {
         Wound altWound = new Wound(wound.getLevel(),
                                    wound.getLocation(),
                                    wound.getDescription(),
                                    noPain ? 0 : wound.getPain(),
                                    wound.getWounds(),
                                    noBlood ? 0 : wound.getBleedRate(),
                                    wound.getPenaltyArm(),
                                    wound.getPenaltyMove(),
                                    wound.getKnockedBackDist(),
                                    wound.getDamageType(),
                                    wound.getEffectsMask(),
                                    null /*target, used for wound placement*/);
         alterationExplanationBuffer.append(getName());
         if (noPain && noBlood) {
            alterationExplanationBuffer.append(" feels no pain, and does not bleed.");
         } else if (noPain) {
            alterationExplanationBuffer.append(" feels no pain.");
         } else {// if (noBlood) {
            alterationExplanationBuffer.append(" does not bleed.");
         }
         wound = altWound;
      }

      // Check for protection spells, or other non-race issues.
      for (Spell spell : activeSpellsList) {
         wound = spell.alterWound(wound, alterationExplanationBuffer);
      }
      return wound;
   }

   public String getHisHer() {
      if (getGender() == Gender.FEMALE) {
         return "her";
      }
      return "his";
   }

   public String getHimHer() {
      if (getGender() == Gender.FEMALE) {
         return "her";
      }
      return "him";
   }

   public String getHeShe() {
      if (getGender() == Gender.FEMALE) {
         return "she";
      }
      return "he";
   }

   @Override
   public String getObjectIDString() {
      return IMonitorableObject.super.getObjectIDString();
   }

   @Override
   public void monitoredObjectChanged(IMonitorableObject originalWatchedObject, IMonitorableObject modifiedWatchedObject, Object changeNotification,
                                      Vector<IMonitoringObject> skipList, Diagnostics diag) {
      // do nothing. why?
   }

   public RequestTarget getTargetPrioritiesRequest(RequestAction action, Arena arena) {
      RequestTarget req = new RequestTarget();
      req.setOrderedTargetIds(arena.getEnemies(this, false));
      return req;
   }

   public String getCurrentSpellName() {
      if (currentSpell == null) {
         return null;
      }
      return currentSpell.getName();
   }

   public RequestSpellTypeSelection getSpellTypeSelectionRequest() {
      boolean mage = ((knownMageSpellsList != null) && (knownMageSpellsList.size() > 0));
      List<Deity> priestAffinities = getPriestDeities();
      return new RequestSpellTypeSelection(mage, priestAffinities, this);
   }

   public RequestSpellSelection getSpellSelectionRequest(String spellType) {
      if (RequestSpellTypeSelection.SPELL_TYPE_MAGE.equals(spellType)) {
         return new RequestSpellSelection(knownMageSpellsList, this);
      }
      Deity deity = Deity.getByName(spellType);
      return new RequestSpellSelection(PriestSpell.getSpellsForDeity(deity, getAffinity(deity),
                                                                     true/*addNullBetweenGroups*/), this);
   }

   public void completeTurn(Arena arena) {
      StringBuilder sb = new StringBuilder();
      if ((currentSpell != null) && !currentSpell.isMaintainedThisTurn() && !currentSpell.isInnate()) {
         sb.append(getName()).append(" has not maintained a '").append(currentSpell.getName());
         sb.append("' spell, and the spell has been lost.");
         currentSpell = null;
      }
      List<Spell> expiredSpells = new ArrayList<>();
      for (Spell spell : activeSpellsList) {
         if (spell.completeTurn(arena)) {
            expiredSpells.add(spell);
         }
      }
      for (Spell spell : expiredSpells) {
         activeSpellsList.remove(spell);
         sb.append("<br/>");
         sb.append(getName()).append(" is no longer under the effects of the '");
         sb.append(spell.getName()).append("' spell.");
      }
      arena.sendMessageTextToAllClients(sb.toString(), false/*popUp*/);
   }

   public Spell getCurrentSpell(boolean eraseCurrentSpell) {
      Spell currentSpell = this.currentSpell;
      if (eraseCurrentSpell) {
         this.currentSpell = null;
      }
      return currentSpell;
   }

   public byte getMagicalAptitude() {
      Advantage adv = getAdvantage(Advantage.MAGICAL_APTITUDE);
      if (adv == null) {
         return 0;
      }
      return (byte) (adv.getLevel() + 1); // add one, because advantages are 0-based.
   }

   public boolean addSpellToActiveSpellsList(Spell spell, Arena arena) {
      if (spell.getDuration() > 0) {
         for (Spell activeSpell : activeSpellsList) {
            if (activeSpell.isIncompatibleWith(spell)) {
               if (activeSpell.takesPrecedenceOver(spell)) {
                  arena.sendMessageTextToAllClients("The '" + spell.getName() + "' has no effect while " + getName() + " is under the effects of the '"
                                                    + activeSpell.getName() + "' spell.", false/*popUp*/);
                  return false;
               }
               arena.sendMessageTextToAllClients("The '" + spell.getName() + "' cancels the effects of the '" + activeSpell.getName() + "' spell.", false/*popUp*/);
               activeSpell.removeEffects(arena);
               activeSpellsList.remove(activeSpell);
               break;
            }
         }
         activeSpellsList.add(spell);
         return true;
      }
      return false;
   }

   public boolean removeSpellFromActiveSpellsList(Spell spell) {
      for (Spell activeSpell : activeSpellsList) {
         if (activeSpell.equals(spell)) {
            activeSpellsList.remove(activeSpell);
            return true;
         }
      }
      return false;
   }

   public byte getActionsAvailableThisRound(boolean usedForDefenseOnly) {
      return condition.getActionsAvailableThisRound(usedForDefenseOnly);
   }

   public boolean canPickup(Object thing) {
      int availableHands = getUncrippledArmCount(true);
      if (thing instanceof Weapon) {
         Weapon weap = (Weapon) thing;
         if (weap.isOnlyTwoHanded()) {
            return (availableHands > 1);
         }
      }
      return (availableHands > 0);
   }

   public Wound getNewTurnBurnWound() {
      if (currentSpell != null) {
         return currentSpell.getNewTurnBurnWound();
      }
      return null;
   }

   public boolean isEnemy(Character other) {
      return (other.teamID != teamID) || (teamID == TEAM_INDEPENDENT);
   }

   public String describeActiveSpells() {
      StringBuilder sb = new StringBuilder();
      for (Spell spell : activeSpellsList) {
         String effects = spell.describeEffects(this, false/*firstTime*/);
         if (effects != null) {
            if (sb.length() == 0) {
               sb.append("<br/>");
               sb.append(getName());
               sb.append(" is under the effects of the following spells:");
            }
            sb.append("<br>    '").append(spell.getName()).append("' in effect for ");
            sb.append(spell.getDuration()).append(" more turns: ").append(effects);
         }
      }
      return sb.toString();
   }

   /**
    * We implement this compareTo method so we can order a list of characters in a deterministic manner
    */
   @Override
   public int compareTo(Character otherChar) {
      int nameCompare = getName().compareTo(otherChar.getName());
      if (nameCompare != 0) {
         return nameCompare;
      }
      ArenaCoordinates thisHeadCoord = getHeadCoordinates();
      ArenaCoordinates otherHeadCoord = otherChar.getHeadCoordinates();
      if (thisHeadCoord == null) {
         if (otherHeadCoord != null) {
            return -1;
         }
         return 0;
      } else if (otherHeadCoord == null) {
         return 1;
      }
      return thisHeadCoord.compareTo(otherHeadCoord);
   }

   public boolean cureWound(Wound woundToCure, byte woundReduction, byte bleedingReduction) {
      boolean woundCured = condition.healWound(woundToCure, woundReduction, bleedingReduction);
      if (woundCured) {
         Limb limb = getLimb(woundToCure.getLimb());
         if (limb != null) {
            limb.removeWound(woundToCure);
         }
      }
      return woundCured;
   }

   public String regrowLimb(Wound woundToCure) {
      //      if (woundToCure.isSeveredArm()) {
      //      }
      if (woundToCure.isSeveredLeg()) {
         legCount++;
      }
      if (woundToCure.isBlinding()) {
         eyeCount++;
      }
      if (woundToCure.isDecapitating()) {
         headCount++;
      }
      if (woundToCure.isSeveredWing()) {
         wingCount++;
      }
      return condition.regrowLimb(woundToCure);
   }

   public Facing getFacing() {
      return condition.getFacing();
   }

   /**
    * This method returns the size of the character, based on the racial size adjustment,
    * plus the average of STR and HT.
    *
    * @return
    */
   public byte getSize() {
      return (byte) (race.getBuildModifier() + ((getAttributeLevel(Attribute.Strength) + getAttributeLevel(Attribute.Health)) / 2));
   }

   public DrawnObject getDrawnObject(int size, RGB background, RGB foreground, ArenaLocation loc, int[] bounds, Orientation orientation) {
      // If nothing has changed since we last drew ourselves, just return the last thing we drew
      String newDrawnObjectKey = getDrawnObjectKey(bounds, background, foreground, orientation);
      // If this character is drawn over multiple hexes, avoid drawing the same one portion of the character across
      // multiple locations by adding the location to the key
      if (orientation.getCoordinates().size() > 0) {
         newDrawnObjectKey += loc.x + "," + loc.y;
      }

      DrawnObject cachedObj = drawnObjectsCache.get(newDrawnObjectKey);
      if (CombatServer.usesCachedDrawing()) {
         if (cachedObj != null) {
            return cachedObj;
         }
      }
      if (recentlyDrawnObjectKeys.size() > 10) {
         String oldestObject = recentlyDrawnObjectKeys.remove(0);
         drawnObjectsCache.remove(oldestObject);
      }

      size = (int) (Math.pow(1.03, getSize()) * size);
      Point hexCenter = new Point((bounds[0] + bounds[6]) / 2, (bounds[1] + bounds[7]) / 2);

      int[] preOffsetBounds = new int[bounds.length];
      for (int i = 0; i < preOffsetBounds.length; i += 2) {
         preOffsetBounds[i] = bounds[i] - hexCenter.x;
         preOffsetBounds[i + 1] = bounds[i + 1] - hexCenter.y;
      }

      RGB backgroundChar = MapWidget2D.darkenColor(background, 60);
      RGB foregroundChar = MapWidget2D.darkenColor(foreground, 60);
      RGB backgroundHead = MapWidget2D.darkenColor(background, 75);
      RGB foregroundHead = MapWidget2D.darkenColor(foreground, 25);
      RGB backgroundWeap = new RGB(128, 128, 128);
      RGB foregroundWeap = new RGB(0, 0, 0);

      DrawnObject drawnObject = new DrawnObject(foregroundChar, backgroundChar);
      DrawnObject legs = orientation.getLegsOutlines(this, size, loc, foregroundChar, backgroundChar);
      if (legs != null) {
         drawnObject.addChild(legs);
      }
      DrawnObject body = orientation.getBodyOutlines(this, size, loc, bounds, foregroundChar, backgroundChar);
      if (body != null) {
         drawnObject.addChild(body);
      }
      DrawnObject arms = orientation.getArmsOutlines(this, size, loc, foregroundHead, backgroundHead);
      if (arms != null) {
         drawnObject.addChild(arms);
      }
      DrawnObject weaps = orientation.getWeaponOutlines(this, size, loc, foregroundWeap, backgroundWeap);
      if (weaps != null) {
         drawnObject.addChild(weaps);
      }
      DrawnObject heads = orientation.getHeadOutlines(this, size, loc, foregroundHead, backgroundHead);
      if (heads != null) {
         drawnObject.addChild(heads);
      }

      drawnObject.trimToHexBounds(preOffsetBounds);
      Facing facing = orientation.getFacing(loc);
      if (facing != null) {
         drawnObject.rotatePoints((facing.value * Math.PI) / 3);
         drawnObject.offsetPoints(hexCenter.x, hexCenter.y);

         recentlyDrawnObjectKeys.add(newDrawnObjectKey);
         drawnObjectsCache.put(newDrawnObjectKey, drawnObject);
         return drawnObject;
      }
      return null;
   }

   private String getDrawnObjectKey(int[] bounds, RGB background, RGB foreground, Orientation orientation) {
      StringBuilder sb = new StringBuilder();
      List<Limb> limbs = getLimbs();
      sb.append(limbs.size());
      for (Limb limb : limbs) {
         sb.append(limb.getHeldThingName());
      }
      sb.append(orientation.getFacing());
      sb.append(orientation.getPosition());
      ArenaCoordinates headCoord = orientation.getHeadCoordinates();
      sb.append(headCoord.x);
      sb.append(headCoord.y);
      sb.append(bounds[0]);
      sb.append(bounds[1]);
      sb.append(bounds[2]);
      sb.append(background);
      sb.append(foreground);
      return sb.toString();
   }

   public void dropAllEquipment(Arena arena) {
      ArenaCoordinates headCoord = condition.getOrientation().getHeadCoordinates();
      ArenaLocation headLoc = arena.getLocation(headCoord);
      List<Thing> things = new ArrayList<>();
      synchronized (equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
            while (equipment.size() > 0) {
               Thing thing = equipment.remove(0);
               things.add(thing);
            }
         }
      }
      for (Thing thing : things) {
         if ((thing != null) && (thing.isReal())) {
            headLoc.addThing(thing);
         }
      }
      for (Limb limb : getLimbs()) {
         Thing thingDropped = limb.dropThing();
         if ((thingDropped != null) && (thingDropped.isReal())) {
            ArenaLocation loc = getLimbLocation(limb.limbType, arena.getCombatMap());
            loc.addThing(thingDropped);
         }
      }
   }

   public DiceSet adjustDieRoll(DiceSet dice, RollType rollType, Object target) {
      for (Spell spell : activeSpellsList) {
         dice = spell.adjustDieRoll(dice, rollType, target);
      }
      return dice;
   }

   public boolean canStand() {
      return condition.canStand();
   }

   public boolean isPenalizedInWater() {
      if (race.isAquatic()) {
         return false;
      }
      return isUnderSpell(SpellSwim.NAME) == null;
   }

   public boolean isPenalizedOutOfWater() {
      return race.isAquatic();
   }

   public boolean isAnimal() {
      return race.isAnimal();
   }

   public boolean hasKey(String code) {
      String fullName = "key:" + code;
      for (Thing equipment : equipment) {
         if (equipment.getName().equalsIgnoreCase(fullName)) {
            return true;
         }
      }
      for (Limb limb : getLimbs()) {
         if (limb instanceof Hand) {
            if (limb.getHeldThingName().equalsIgnoreCase(fullName)) {
               return true;
            }
         }
      }
      return false;
   }

   public String getPositionName() {
      return condition.getPositionName();
   }

   private byte getPositionAdjustedDefenseOption(DefenseOption defOption, byte def) {
      return condition.getPositionAdjustedDefenseOption(defOption, def);
   }

   public byte getPositionAdjustmentForAttack() {
      return condition.getPositionAdjustmentForAttack();
   }

   public short getMaxWeaponRange(boolean allowRanged, boolean onlyChargeTypes) {
      short maxRange = 0;
      for (Limb limb : getLimbs()) {
         short limbRange = limb.getWeaponMaxRange(this, allowRanged, onlyChargeTypes);
         if (limbRange > maxRange) {
            maxRange = limbRange;
         }
      }
      return maxRange;
   }

   public short getMinWeaponRange(boolean allowRanged, boolean onlyChargeTypes) {
      short minRange = 10000;
      for (Limb limb : getLimbs()) {
         short limbRange = limb.getWeaponMinRange(this, allowRanged, onlyChargeTypes);
         if (limbRange < minRange) {
            minRange = limbRange;
         }
      }
      return minRange;
   }

   public void awaken() {
      condition.awaken();
   }

   public List<Thing> getEquipment() {
      synchronized (equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
            return new ArrayList<>(equipment);
         }
      }
   }

   public void clearEquipmentList() {
      synchronized (equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
            equipment.clear();
         }
      }
   }

   public void addEquipment(Thing newThing) {
      synchronized (equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_equipment)) {
            if ((newThing == null) || !newThing.isReal()) {
               DebugBreak.debugBreak();
            }
            equipment.add(newThing);
         }
      }
   }

   public byte getPenaltyForBeingHeld() {
      byte total = 0;
      for (Byte penalty : heldPenalties.values()) {
         total += penalty;
      }
      return total;
   }

   public void setHoldLevel(IHolder holder, Byte holdLevel) {
      if ((holdLevel == null) || (holdLevel < 0)) {
         heldPenalties.remove(holder);
         if (holder.getHoldTarget() == this) {
            holder.setHoldTarget(null);
         }
      } else {
         setPlacedIntoHoldThisTurn(holder);
         heldPenalties.put(holder, holdLevel);
         // a character can only hold one character at a time.
         // If the holding character is holding someone else, let go of him:
         Character holdingCharacter = holder.getHoldTarget();
         if ((holdingCharacter != null) && (holdingCharacter != this)) {
            holdingCharacter.setHoldLevel(holder, null);
         }
         holder.setHoldTarget(this);
      }
   }

   @Override
   public Character getHoldTarget() {
      return holdTarget;
   }

   @Override
   public void setHoldTarget(Character holdTarget) {
      this.holdTarget = holdTarget;
   }

   public Byte getHoldLevel(IHolder holder) {
      return heldPenalties.get(holder);
   }

   public void releaseHold() {
      if (holdTarget != null) {
         holdTarget.setHoldLevel(this, null);
      }
   }

   public Set<IHolder> getHolders() {
      return heldPenalties.keySet();
   }

   @Override
   public Byte getHoldingLevel() {
      if (holdTarget != null) {
         return holdTarget.getHoldLevel(this);
      }
      return null;
   }

   public void setPlacedIntoHoldThisTurn(IHolder holder) {
      if (!placedIntoHoldThisTurn.contains(holder)) {
         placedIntoHoldThisTurn.add(holder);
      }
   }

   public boolean getPlacedIntoHoldThisTurn(IHolder holder) {
      return placedIntoHoldThisTurn.contains(holder);
   }

   public static final Comparator<? super Character> NAME_COMPARATOR = (Comparator<Character>) (char1, char2) -> char1.getName().compareTo(char2.getName());

   public void regenerateWound(byte woundReduction) {
      getCondition().regenerateWound(woundReduction);
   }

   public String canTarget(Character target, TargetType targetType) {
      if (target != null) {
         switch (targetType) {
            case TARGET_SELF:
               if (this.uniqueID != target.uniqueID) {
                  return "not self";
               }
               return null;
            case TARGET_ANYONE:
               return null;
            case TARGET_ANYONE_ALIVE:
               if (!target.getCondition().isAlive()) {
                  return "dead";
               }
               return null;
            case TARGET_ANIMAL_FIGHTING:
               if (!target.isAnimal()) {
                  return "humanoid";
               }
               if (this.uniqueID == target.uniqueID) {
                  return "self";
               }
               if (!target.getCondition().isAlive()) {
                  return "dead";
               }
               if (!target.getCondition().isConscious()) {
                  return "unconscious";
               }
               return null;
            case TARGET_OTHER_GOOD_FIGHTING: // TODO: determine evil/goodness
               // fall though to other fighting:
            case TARGET_OTHER_EVIL_FIGHTING: // TODO: determine evil/goodness
               // fall though to other fighting:
            case TARGET_OTHER_FIGHTING:
               if (this.uniqueID == target.uniqueID) {
                  return "self";
               }
               // fall through into the anyone fighting case:
            case TARGET_ANYONE_FIGHTING:
               if (!target.getCondition().isAlive()) {
                  return "dead";
               }
               if (!target.getCondition().isConscious()) {
                  return "unconscious";
               }
               return null;
            case TARGET_AREA:
               return "not an area";
            case TARGET_UNCONSIOUS:
               if (!target.getCondition().isAlive()) {
                  return "dead";
               }
               if (target.getCondition().isConscious()) {
                  return "conscious";
               }
               return null;
            case TARGET_DEAD:
               if (target.getCondition().isAlive()) {
                  return "alive";
               }
               return null;
            case TARGET_UNDEAD:
               if (!target.hasAdvantage(Advantage.UNDEAD)) {
                  return "not undead";
               }
            case TARGET_OBJECT:
               return "not an object";
            case TARGET_NONE:
               return "";
         }
      }
      DebugBreak.debugBreak();
      return "";
   }
}
