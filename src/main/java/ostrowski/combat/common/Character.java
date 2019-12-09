package ostrowski.combat.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.enums.AI_Type;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DefenseOption;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Facing;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.spells.IInstantaneousSpell;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.spells.mage.InstantaneousMageSpell;
import ostrowski.combat.common.spells.mage.MageCollege;
import ostrowski.combat.common.spells.mage.MageSpell;
import ostrowski.combat.common.spells.mage.SpellFlight;
import ostrowski.combat.common.spells.mage.SpellLevitate;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.SpellSummonBeing;
import ostrowski.combat.common.spells.priest.elemental.SpellSwim;
import ostrowski.combat.common.spells.priest.evil.SpellParalyze;
import ostrowski.combat.common.spells.priest.good.SpellPacify;
import ostrowski.combat.common.things.Armor;
import ostrowski.combat.common.things.Hand;
import ostrowski.combat.common.things.Leg;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Shield;
import ostrowski.combat.common.things.Thing;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.common.weaponStyles.WeaponStyle;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackRanged;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.common.wounds.WoundCantBeAppliedToTargetException;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestActionOption;
import ostrowski.combat.protocol.request.RequestActionType;
import ostrowski.combat.protocol.request.RequestAttackStyle;
import ostrowski.combat.protocol.request.RequestDefense;
import ostrowski.combat.protocol.request.RequestEquipment;
import ostrowski.combat.protocol.request.RequestGrapplingHoldMaintain;
import ostrowski.combat.protocol.request.RequestPosition;
import ostrowski.combat.protocol.request.RequestSpellSelection;
import ostrowski.combat.protocol.request.RequestSpellTypeSelection;
import ostrowski.combat.protocol.request.RequestTarget;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.BattleTerminatedException;
import ostrowski.combat.server.ClientProxy;
import ostrowski.combat.server.CombatServer;
import ostrowski.protocol.IRequestOption;
import ostrowski.protocol.ObjectChanged;
import ostrowski.protocol.SerializableObject;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.CombatSemaphore;
import ostrowski.util.Diagnostics;
import ostrowski.util.IMonitorableObject;
import ostrowski.util.IMonitoringObject;
import ostrowski.util.Semaphore;
import ostrowski.util.SemaphoreAutoLocker;


/*
 * Created on May 3, 2006
 */
/*
 * @author Paul
 */
public class Character extends SerializableObject implements IHolder, Enums, IMonitorableObject, IMonitoringObject, Comparable<Character>
{
//   private final transient MonitoredObject   _monitoredObj                               = new MonitoredObject("Character");
//   private final transient MonitoringObject  _monitoringObj                              = new MonitoringObject("Character");
   public transient ArenaLocationBook        _mapWatcher                                 = null;
   // character traits:
   private String                            _name                                       = null;
   private Race                              _race                                       = null;
   private final HashMap<Attribute, Byte>    _attributes                                 = new HashMap<>();
   public final HashMap<LimbType, Limb>      _limbs                                      = new HashMap<>();
   private final ArrayList<Thing>            _equipment                                  = new ArrayList<>();
   private Armor                             _armor                                      = Armor.getArmor("", null);
   private final HashMap<SkillType, Skill>   _skillsList                                 = new HashMap<>();
   private List<MageSpell>                   _knownMageSpellsList                        = new ArrayList<>();
   private List<MageCollege>                 _knownCollegesList                          = new ArrayList<>();
   private List<Advantage>                   _advList                                    = new ArrayList<>();

   Semaphore                                 _lock_equipment                             = new Semaphore("equipment", CombatSemaphore.CLASS_CHARACTER_EQUIPMENT);

   // computed values:
   private byte                              _buildBase;
   private byte                              _damageBase;

   // active values:
   private Condition                         _condition                                  = null;
   public int                                _targetID                                   = -1;
   private byte                              _aimDuration                                = 0;
   private final List<Spell>                 _activeSpellsList                           = new ArrayList<>();

   private final ArrayList<Integer>          _orderedTargetIds                           = new ArrayList<>();

   // location values
   public int                                _uniqueID                                   = -1;
   public byte                               _teamID                                     = -1;
   private int                               _headCount                                  = 1;
   private int                               _eyeCount                                   = 2;
   private int                               _legCount                                   = 2;
   private int                               _wingCount                                  = 0;
   transient private ClientProxy             _clientProxy                                = null;
   private Spell                             _currentSpell                               = null;
   private boolean                           _isBerserking                               = false;

   private boolean                           _hasInitiativeAndActionsEverBeenInitialized = false;
   private static final String               SEPARATOR_MAIN                              = "|";
   private static final String               SEPARATOR_SECONDARY                         = ";";
   public static final String                YOU_ARE_BEING_TARGETED_BY                   = " \nYou are being targeted by ";

   public Character(String name, String raceName, Gender gender, byte[] atts, String armorName, HashMap<LimbType, Limb> limbs,
                    ArrayList<Thing> equipment, Skill[] skills, MageSpell[] mageSpells, MageCollege[] colleges, Advantage[] advantages) {
      _name = name;
      IMonitorableObject._monitoredObj._objectIDString = this.getClass().getName();
      IMonitoringObject._monitoringObj._objectIDString = this.getClass().getName();

      for (Attribute att : Attribute.values()) {
         _attributes.put(att, atts[att.value]);
      }
      setRace(raceName, gender);

      synchronized (_equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
            _equipment.clear();
            _equipment.addAll(equipment);
         }
      }

      _armor = Armor.getArmor(armorName, getRace());
      _condition = new Condition(this);
      if (skills != null) {
         for (Skill element : skills) {
            _skillsList.put(element.getType(), element);
         }
      }
      if (mageSpells != null) {
         for (MageSpell element : mageSpells) {
            _knownMageSpellsList.add(element);
         }
      }
      if (colleges != null) {
         for (MageCollege element : colleges) {
            _knownCollegesList.add(element);
         }
      }
      if (advantages != null) {
         for (Advantage element : advantages) {
            _advList.add(element);
         }
      }
      if (limbs != null) {
         for (Hand curHand : getArms()) {
            Thing heldThing = curHand.getHeldThing();
            if (heldThing == null) {
               curHand.setHeldThing(null, this);
            }
            else {
               curHand.setHeldThing(heldThing.clone(), this);
            }
         }
      }
      resetSpellPoints();
      updateWeapons();
   }

   public Orientation getOrientation() {
      return _condition.getOrientation();
   }

   public boolean isInCoordinates(ArenaCoordinates loc) {
      return _condition.isInCoordinates(loc);
   }

   public ArrayList<ArenaCoordinates> getCoordinates() {
      return _condition.getCoordinates();
   }

   public ArenaCoordinates getHeadCoordinates() {
      return _condition.getHeadCoordinates();
   }

   public ArenaLocation getLimbLocation(LimbType limbType, CombatMap map) {
      return _condition.getLimbLocation(limbType, map);
   }

   public ArenaLocation getAttackFromLocation(RequestAction action, CombatMap map) {
      return getLimbLocation(action.getLimb(), map);
   }

   public ArrayList<Limb> getLimbs() {
      ArrayList<Limb> limbs = new ArrayList<>();
      // Make sure we return a list that is in the same order as the LimbType array, so Head is return first.
      for (LimbType type : LimbType.values()) {
         Limb limb = _limbs.get(type);
         if (limb != null) {
            limbs.add(limb);
         }
      }
      return limbs;
   }

   public Character() {
      for (Attribute att : Attribute.values()) {
         _attributes.put(att, (byte)0);
      }

      setRace(Race.NAME_Human, Gender.MALE);

      refreshDefenses();
      _condition = new Condition(this);
   }

   private void initHands() {
      HashMap<LimbType, Limb> newLimbs = new HashMap<>();
      for (LimbType limbType : LimbType.values()) {
         Limb limb = _race.createLimb(limbType);
         if (limb != null) {
            newLimbs.put(limbType, limb);
         }
      }
      // Transfer anything from our current hand either to the new hands, or to our equipment belt:
      for (Hand curHand : getArms()) {
         Hand newHand = (Hand) (newLimbs.get(curHand._limbType));
         Thing heldThing = curHand.getHeldThing();

         if (newHand != null) {
            newHand.setHeldThing(heldThing, this);
         }
         else {
            if ((heldThing != null) && (heldThing.isReal())) {
               addEquipment(heldThing);
            }
         }
      }

      _limbs.clear();
      for (LimbType limbType : newLimbs.keySet()) {
         _limbs.put(limbType, newLimbs.get(limbType));
      }
   }

   public byte getHandPenalties(LimbType limbType, SkillType skillType) {
      // Use Penalty is for non-ambidextrous characters. left hands normally have a -3 penalty.
      if (hasAdvantage(Advantage.AMBIDEXTROUS)) {
         return 0;
      }
      Thing heldThing = _limbs.get(limbType).getWeapon(this);
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
      return _isBerserking && isBerserker();
   }

   public void setIsBerserking(boolean isBerserking) {
      _isBerserking = isBerserking;
   }

   public boolean isRegenerative() {
      return _race.getAdvantage(Advantage.REGENERATION) != null;
   }

   public boolean hasWings() {
      return _wingCount > 0;
   }

   public boolean isMounted() {
      return isFlying() || getRace().getName().equals(Race.NAME_Centaur);
   }
   public boolean isFlying() {
      // Assume that if it has all its wings, it's flying.
      if (hasWings() && (_wingCount == _race.getWingCount())) {
         return true;
      }

      // Without wings, it can't fly, unless it has a flying spell active:
      for (Spell spell : _activeSpellsList) {
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
      for (Advantage adv : _advList) {
         if (adv.toString().equals(advName)) {
            return adv;
         }
         if (adv._name.equals(advName)) {
            return adv;
         }
      }
      return _race.getAdvantage(advName);
   }

   public boolean addAdvantage(Advantage newAdvantage) {
      ArrayList<String> existingAdvNames = new ArrayList<>();
      for (Advantage advantage : _advList) {
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
            _advList.add(newAdvantage);
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
    * @param moneySpent
    * @return
    */
   private boolean computeWealth(int moneySpent) {
      Advantage wealth = getAdvantage(Advantage.WEALTH);
      if (wealth == null) {
         wealth = Advantage.getAdvantage(Advantage.WEALTH);
         addAdvantage(wealth);
      }

      float adjustedMoneySpent = moneySpent / _race.getWealthMultiplier();

      ArrayList<String> levels = wealth.getLevelNames();
      for (byte i = 0; i < levels.size(); i++) {
         int level = Integer.parseInt(levels.get(i).substring(1).replaceAll(",", ""));// remove the '$' and all commas
         if (adjustedMoneySpent <= level) {
            if (wealth._level == i) {
               return false;
            }
            wealth._level = i;
            addAdvantage(wealth);
            return true;
         }
      }
      DebugBreak.debugBreak();
      return false;
   }

   public byte getAimDuration(int targetID) {
      if (_targetID == targetID) {
         return _aimDuration;
      }
      return 0;
   }

   public void clearAimDuration() {
      _aimDuration = 0;
   }

   public List<Skill> getSkillsList() {
      ArrayList<Skill> skills = new ArrayList<>();
      skills.addAll(_skillsList.values());
      return skills;
   }

   public void setSkillsList(List<Skill> newSkills) {
      _skillsList.clear();
      for(Skill skill : newSkills) {
         _skillsList.put(skill.getType(), skill);
      }
   }

   public List<MageSpell> getMageSpellsList() {
      return _knownMageSpellsList;
   }

   public void setSpellsList(List<MageSpell> newSpells) {
      _knownMageSpellsList = newSpells;
   }

   public List<MageCollege> getCollegesList() {
      return _knownCollegesList;
   }

   public void setCollegesList(List<MageCollege> newCollege) {
      _knownCollegesList = newCollege;
   }

   public void setAdvantagesList(List<Advantage> newAdv) {
      _advList = newAdv;
      resetSpellPoints();
   }

   public List<Advantage> getAdvantagesList() {
      return _advList;
   }

   public byte getPhysicalDamageBase() {
      return _damageBase;
   }

   /**
    * @param attributeIndex
    * @return the characters level for the specified attribute
    */
   public byte getAttributeLevel(Attribute attribute) {
      return _attributes.get(attribute);
   }

   @Override
   public byte getAdjustedStrength() {
      return (byte) (_attributes.get(Attribute.Strength) + _race.getBuildModifier());
   }

   @Override
   public String getName() {
      return (_name == null) ? "" : _name;
   }

   public Race getRace() {
      return _race;
   }

   public Gender getGender() {
      return _race.getGender();
   }

   public Armor getArmor() {
      if ((_armor == null) || (!_armor.isReal())) {
         Armor naturalArmor = _race.getNaturalArmor();
         if (naturalArmor != null) {
            return naturalArmor;
         }
      }
      return _armor;
   }

   public Condition getCondition() {
      return _condition;
   }

   public int getPointTotal() {
      int totalPoints = _race.getCost();
      int attrCost = 0;
      int skillCost = 0;
      int spellCost = 0;
      int collegeCost = 0;
      int advCost = 0;
      for (Attribute att : Attribute.values()) {
         attrCost += _race.getAttCost(_attributes.get(att), att);
      }
      for (Skill skill : _skillsList.values()) {
         skillCost += Rules.getSkillCost(skill.getLevel());
      }
      for (MageSpell spell : _knownMageSpellsList) {
         spellCost += Rules.getSpellCost(spell.getLevel());
      }
      for (MageCollege college : _knownCollegesList) {
         collegeCost += Rules.getCollegeCost(college.getLevel());
      }
      for (Advantage adv : _advList) {
         advCost += adv.getCost(getRace());
      }
      totalPoints += attrCost;
      totalPoints += skillCost;
      totalPoints += spellCost;
      totalPoints += collegeCost;
      totalPoints += advCost;
      return totalPoints;
   }

   public int getAttCostAtCurLevel(Attribute attribute) {
      return _race.getAttCost(_attributes.get(attribute), attribute);
   }

   public void setName(String name) {
      _name = name;
   }

   public void setAttribute(Attribute attr, byte attLevel, boolean containInLimits) {
      if (containInLimits) {
         byte baseAtt = (byte) (attLevel - _race.getAttributeMods(attr));
         baseAtt = (byte) Math.min(baseAtt, Rules.getMaxAttribute());
         baseAtt = (byte) Math.max(baseAtt, Rules.getMinAttribute());
         attLevel = (byte) (baseAtt + _race.getAttributeMods(attr));
      }
      _attributes.put(attr, attLevel);
      refreshDefenses();
   }

   public void setRace(String raceName, Gender gender) {
      if (_race != null) {
         if (_race.getName().equals(raceName) && (_race.getGender() == gender)) {
            return;
         }

         for (Attribute att : Attribute.values()) {
            _attributes.put(att, (byte) (_attributes.get(att) - _race.getAttributeMods(att)));
         }
      }
      _race = Race.getRace(raceName, gender);
      if (_condition == null) {
         _condition = new Condition(this);
      }
      for (Thing equ : _equipment) {
         equ.setRacialBase(_race);
      }
      for (Limb limb : _limbs.values()) {
         Thing heldThing = limb.getHeldThing();
         if (heldThing != null) {
            heldThing.setRacialBase(_race);
         }
      }
      if (_armor != null) {
         _armor.setRacialBase(_race);
      }
      setOrientation(_race.getBaseOrientation(), null/*diag*/);
      for (Attribute att : Attribute.values()) {
         _attributes.put(att, (byte) (_attributes.get(att) + _race.getAttributeMods(att)));
      }
      // Validate the advantages
      List<Advantage> prevAdvList = _advList;
      _advList = new ArrayList<>();
      while (prevAdvList.size() > 0) {
         Advantage adv = prevAdvList.remove(0);
         ArrayList<String> advNames = Advantage.getAdvantagesNames(getPropertyNames(), getRace());
         if (advNames.contains(adv.getName())) {
            _advList.add(adv);
         }
         else {
            // sex changed. Look for advantages that are gender-based
            if (adv._conflicts.contains(getRace().getGender()._name)) {
               // Find the matching advantage. It will be the conflicts list:
               for (String conflict : adv._conflicts) {
                  Advantage newAdv = Advantage.getAdvantage(conflict);
                  if (newAdv != null) {
                     newAdv.setLevel(adv.getLevel());
                     _advList.add(newAdv);
                     break;
                  }
               }
            }
         }
      }
      _headCount = _race.getHeadCount();
      _legCount = _race.getLegCount();
      _eyeCount = _race.getEyeCount();
      _wingCount = _race.getWingCount();
      initHands();
      // Since the build modifier may have changed, update defenses
      refreshDefenses();
      resetSpellPoints();
   }

   public void setArmor(String armorName) {
      if (armorName.equals(_armor.getName())) {
         return;
      }
      _armor = Armor.getArmor(armorName, getRace());
      refreshDefenses();
   }

   public void setInitiativeActionsAndMovementForNewTurn(int initiativeDieRoll) {
      byte initiative = (byte) (_attributes.get(Attribute.Nimbleness) + initiativeDieRoll);
      _condition.setInitiative(initiative);
      byte maxActionsPerRound = 3;
      byte actionsPerTurn = Rules.getStartingActions(this);
      for (Spell spell : _activeSpellsList) {
         actionsPerTurn = spell.getModifiedActionsPerTurn(actionsPerTurn);
         maxActionsPerRound = spell.getModifiedActionsPerRound(maxActionsPerRound);
         spell.newTurn();
      }
      if (_currentSpell != null) {
         _currentSpell.newTurn();
      }

      _condition.initializeActionsAndMovementForNewTurn(actionsPerTurn, maxActionsPerRound, getMovementRate());

      _placedIntoHoldThisTurn.clear();

      _hasInitiativeAndActionsEverBeenInitialized = true;
   }

   public boolean hasInitiativeAndActionsEverBeenInitialized() {
      return _hasInitiativeAndActionsEverBeenInitialized;
   }

   public void reducePain(byte painReduction) {
      _condition.reducePain(painReduction);
   }

   public byte getActionsPerTurn() {
      byte actionsPerTurn = Rules.getStartingActions(this);
      for (Spell spell : _activeSpellsList) {
         actionsPerTurn = spell.getModifiedActionsPerTurn(actionsPerTurn);
      }
      return actionsPerTurn;
   }

   public List<Spell> getActiveSpells() {
      return _activeSpellsList;
   }

   public Spell isUnderSpell(String spellName) {
      for (Spell spell : _activeSpellsList) {
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
      byte movementRate = _race.getMovementRate(Rules.getEncumbranceLevel(this));
      for (Spell spell : _activeSpellsList) {
         movementRate = spell.getModifiedMovementPerRound(movementRate);
      }
      return movementRate;
   }

   public Wound modifyWoundFromAttack(Wound originalWound, Character defender, String attackingWeaponName, StringBuilder modificationsExplanation) {
      Wound newWound = originalWound;
      for (Spell spell : _activeSpellsList) {
         newWound = spell.modifyDamageDealt(newWound, this, defender, attackingWeaponName, modificationsExplanation);
      }
      return newWound;
   }

   public Wound modifyWoundFromDefense(Wound originalWound) {
      Wound newWound = originalWound;
      for (Spell spell : _activeSpellsList) {
         newWound = spell.modifyDamageRecieved(newWound);
      }
      return newWound;
   }

   private byte getMaxActionsPerRound() {
      byte maxActions = 3;
      for (Spell spell : _activeSpellsList) {
         maxActions += spell.getModifiedActionsPerRound(maxActions);
      }
      return maxActions;
   }

   // return 'true' if any actions remains to be spent
   public boolean endRound() {
      for (Limb limb : _limbs.values()) {
         limb.endRound();
      }
      return _condition.endRound();
   }

   public byte getInitiative() {
      return _condition.getInitiative();
   }

   public byte getActionsAvailable(boolean usedForDefenseOnly) {
      return _condition.getActionsAvailable(usedForDefenseOnly);
   }

   public Position getPosition() {
      return _condition.getPosition();
   }

   public boolean isStanding() {
      return _condition.isStanding();
   }

   public Position getMovingToPosition() {
      return _condition.getPosition();
   }

   public byte getActionsNeededToChangePosition() {
      return _condition.getActionsNeededToChangePosition();
   }

   public byte getPainPenalty(boolean accountForBerserking) {
      if (accountForBerserking && isBerserking()) {
         return 0;
      }
      return _condition.getPenaltyPain();
   }

   public byte getWoundsAndPainPenalty() {
      return _condition.getWoundsAndPainPenalty();
   }

   public byte getWounds() {
      return _condition.getWounds();
   }

   public void collapseFromPain(CombatMap map) {
      _condition.collapseFromPain(map, this);
   }

   public void refreshDefenses() {
      _buildBase = (byte) (_attributes.get(Attribute.Health) + _race.getBuildModifier());
      _damageBase = Rules.getDamageBase(getAdjustedStrength());
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
      if (_limbs.containsKey(limb)) {
         Weapon weap = _limbs.get(limb).getWeapon(this);
         if (weap != null) {
            if (isGrapple) {
               if (weap._grapplingStyles.length > attackStyle) {
                  return weap._grapplingStyles[attackStyle];
               }
            }
            else if (isCounterAttack) {
               if (weap._counterattackStyles.length > attackStyle) {
                  return weap._counterattackStyles[attackStyle];
               }
            }
            else if (weap._attackStyles.length > attackStyle) {
               return weap._attackStyles[attackStyle];
            }
         }
      }
      return null;
   }

   public Skill getBestSkill(Weapon weapon) {
      Skill bestSkill = null;
      for (int i = 0; i < weapon._attackStyles.length; i++) {
         WeaponStyleAttack attackMode = weapon.getAttackStyle(i);
         Skill testSkill = getSkill(attackMode.getSkillType());
         if (testSkill != null) {
            if ((bestSkill == null) || (bestSkill.getLevel() < testSkill.getLevel())) {
               bestSkill = testSkill;
            }
         }
      }
      return bestSkill;
   }

   public byte getBestSkillLevel(Weapon weapon) {
      byte best = 0;
      for (int i = 0; i < weapon._attackStyles.length; i++) {
         WeaponStyleAttack attackMode = weapon.getAttackStyle(i);
         byte testSkill = getSkillLevel(attackMode, false/*adjustForPain*/, null /*ignore use penalty*/, true/*sizeAdjust*/,
                                        true/*adjustForEncumbrance*/, true/*adjustForHolds*/);
         if (best < testSkill) {
            best = testSkill;
         }
      }
      return best;
   }

   public byte getSkillLevel(WeaponStyle attackMode, boolean adjustForPain, LimbType useHand,
                             boolean sizeAdjust, boolean adjustForEncumbrance, boolean adjustForHolds) {
      byte skillLevel = getSkillLevel(attackMode.getSkillType(), useHand, sizeAdjust, adjustForEncumbrance, adjustForHolds);
      if (adjustForPain) {
         if (useHand != null) {
            skillLevel -= _limbs.get(useHand).getWoundPenalty();
         }
         skillLevel -= _condition.getWoundsAndPainPenalty();
      }
      return skillLevel;
   }

   public byte getAdjustedSkillLevel(WeaponStyle attackMode, boolean adjustForPain, LimbType useHand,
                             boolean sizeAdjust, boolean adjustForEncumbrance, boolean adjustForHolds) {
      byte skillLevel = getAdjustedSkillLevel(attackMode.getSkillType(), useHand, sizeAdjust, adjustForEncumbrance, adjustForHolds);
      if (adjustForPain) {
         if (useHand != null) {
            skillLevel -= _limbs.get(useHand).getWoundPenalty();
         }
         skillLevel -= _condition.getWoundsAndPainPenalty();
      }
      return skillLevel;
   }

   public Skill getSkill(SkillType skillType) {
      return _skillsList.get(skillType);
   }

   public byte getSkillLevel(SkillType skillType, LimbType useLimb, boolean sizeAdjust, boolean adjustForEncumbrance, boolean adjustForHolds) {
      Skill skill = _skillsList.get(skillType);
      if (skill != null) {
         byte skillLevel = skill.getLevel();
         if (useLimb != null) {
            skillLevel -= getHandPenalties(useLimb, skillType);
         }
         if (sizeAdjust && (skill.isAdjustedForSize())) {
            skillLevel += _race.getBonusToHit();
         }
         if (adjustForEncumbrance) {
            skillLevel -= skill.getPenaltyForEncumbranceLevel(Rules.getEncumbranceLevel(this));
         }
         if (adjustForHolds) {
            for (Byte hold : _heldPenalties.values()) {
               skillLevel -= hold.byteValue();
            }
         }
         if (skillLevel < 0) {
            return 0;
         }
         return skillLevel;
      }
      return 0;
   }
   public byte getAdjustedSkillLevel(SkillType skillType, LimbType useLimb, boolean sizeAdjust, boolean adjustForEncumbrance, boolean adjustForHolds) {
      Skill skill = _skillsList.get(skillType);
      if (skill != null) {
         byte skillLevel = Rules.getAdjustedSkillLevel(skill.getLevel(), getAttributeLevel(skill.getAttributeBase()));
         if (useLimb != null) {
            skillLevel -= getHandPenalties(useLimb, skillType);
         }
         if (sizeAdjust && (skill.isAdjustedForSize())) {
            skillLevel += _race.getBonusToHit();
         }
         if (adjustForEncumbrance) {
            skillLevel -= skill.getPenaltyForEncumbranceLevel(Rules.getEncumbranceLevel(this));
         }
         if (adjustForHolds) {
            for (Byte hold : _heldPenalties.values()) {
               skillLevel -= hold.byteValue();
            }
         }
         if (skillLevel < 0) {
            return 0;
         }
         return skillLevel;
      }
      return 0;
   }

   public void setSkillLevel(SkillType skillType, byte skillLevel) {
      Skill skill = getSkill(skillType);
      if ((skill == null) && (skillLevel > 0)) {
         _skillsList.put(skillType, new Skill(skillType, skillLevel));
      }
      else {
         if (skillLevel < 1) {
            _skillsList.remove(skillType);
         }
         else {
            skill.setLevel(skillLevel);
         }
      }
   }

   public byte getSpellLevel(String spellName) {
      for (MageSpell spell : _knownMageSpellsList) {
         if (spell.getName().equals(spellName)) {
            return spell.getLevel();
         }
      }
      return 0;
   }

   public byte getSpellSkill(String spellName) {
      byte mageSkill = 0;
      byte inateSkill = 0;
      for (MageSpell spell : _knownMageSpellsList) {
         if (spell.getName().equals(spellName)) {
            mageSkill = spell.getEffectiveSkill(this, true);
            break;
         }
      }
      ArrayList<Spell> inateSpells = _race.getInateSpells();
      if ((inateSpells != null) && ( inateSpells.size() > 0)) {
         Skill baseSkill = getSkill(SkillType.Brawling);
         if (baseSkill != null) {
            for (Spell spell : inateSpells) {
               if (spell.getName().equals(spellName)) {
                  inateSkill = baseSkill.getLevel();
                  break;
               }
            }
         }
      }
      return (byte) Math.max(mageSkill, inateSkill);
   }

   public void setSpellLevel(String spellName, byte spellLevel) {
      for (MageSpell spell : _knownMageSpellsList) {
         if (spell.getName().equals(spellName)) {
            spell.setLevel(spellLevel);
            return;
         }
      }
      MageSpell spell = MageSpell.getSpell(spellName);
      spell.setLevel(spellLevel);
      _knownMageSpellsList.add(spell);
   }

   public byte getCollegeLevel(String collegeName) {
      for (MageCollege college : _knownCollegesList) {
         if (college.getName().equals(collegeName)) {
            return college.getLevel();
         }
      }
      return 0;
   }

   public void setCollegeLevel(String collegeName, byte collegeLevel) {
      for (MageCollege college : _knownCollegesList) {
         if (college.getName().equals(collegeName)) {
            college.setLevel(collegeLevel);
            return;
         }
      }
      MageCollege college = MageCollege.getCollege(collegeName);
      college.setLevel(collegeLevel);
      _knownCollegesList.add(college);
   }

   public byte getBuildBase() {
      return _buildBase;
   }

   public byte getBuild(DamageType damType) {
      byte build = (byte) (_armor.getBarrier(damType) + _buildBase);
      if (!_armor.equals(_race.getNaturalArmor())) {
         build += _race.getNaturalArmorBarrier(damType);
      }
      return build;
   }

   public boolean stillFighting() {
      return ((_condition.isConscious()) && (_condition.isAlive()));
   }

   public RequestAction getActionRequest(Arena arena, Character delayedTarget, ArrayList<Character> charactersTargetingActor) {
      boolean mustAdvance = false;
      Character target = arena.getCharacter(_targetID);
      if (delayedTarget != null) {
         target = delayedTarget;
         _aimDuration = 0;
      }
      else {
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
               if ((target == null) || (oldTarget == null) || (target._uniqueID != oldTarget._uniqueID)) {
                  _aimDuration = 0;
               }
            }
         }
         else {
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
      for (Spell spell : _activeSpellsList) {
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
         req = new RequestAction(_uniqueID, (target == null) ? -1 : target._uniqueID);
         req.setSpell(_currentSpell);

         StringBuilder sb = new StringBuilder();
         sb.append(getName());
         if ((delayedTarget != null) && (target != null)) {
            sb.append(", your target ").append(target.getName());
            sb.append(" has moved into range.\nYou have ");
         }
         else {
            sb.append(", you have ");
         }
         sb.append(_condition.getActionsAvailable(false/*usedForDefenseOnly*/)).append(" actions remaining");
         if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) == _condition.getActionsAvailable(false/*usedForDefenseOnly*/)) {
            sb.append(".");
         }
         else {
            sb.append(" for this turn, ").append(getActionsAvailableThisRound(false/*usedForDefenseOnly*/));
            sb.append(" can be spent this round.");
         }
         if (_currentSpell != null) {
            sb.append("\nYou currently have a ").append(_currentSpell.getName()).append(" spell");
            if (_currentSpell instanceof MageSpell) {
               sb.append(", with ").append(_currentSpell.getPower()).append(" power points");
            }
            sb.append(".");
         }
         if (_condition.isCollapsed()) {
            sb.append("\nBecause you have collapsed in pain, you may not stand or attack.");
         }
         else if (isPacified) {
            sb.append("\nYou are under the effects of a pacify spell, so you may not attack anyone.");
         }
         else if (isParalyzed) {
            sb.append("\nYou are under the effects of a paralyze spell, so you may not attack anyone.");
         }
         else {
            byte totalHold = 0;
            Set<IHolder> holders = getHolders();
            if (!holders.isEmpty()) {
               sb.append("\nYou are being held by ");
               boolean first = true;
               for (IHolder holder : holders) {
                  if (!first ) {
                     sb.append(", ");
                  }
                  first = false;
                  sb.append(holder.getName()).append(" at level ").append(getHoldLevel(holder));
                  totalHold += getHoldLevel(holder);
               }
               sb.append(" for a total penalty of ").append(totalHold).append(".");
               if (_heldPenalties.size() == _placedIntoHoldThisTurn.size()) {
                  sb.append("\nBecause you have been placed into ");
                  if (_heldPenalties.size() >1) {
                     sb.append("each of your holds within");
                  }
                  else {
                     sb.append("this hold");
                  }
                  sb.append(" this turn, you may not try to break free until next turn.");
               }
            }
            boolean weaponReady = false;
            for (LimbType limbType : _limbs.keySet()) {
               Limb limb = _limbs.get(limbType);
               Weapon weap = limb.getWeapon(this);
               boolean showLimbNames = false;
               LimbType pairedLimb = limbType.getPairedType();
               if (_limbs.containsKey(pairedLimb)) {
                  Limb otherLimb = _limbs.get(pairedLimb);
                  Weapon otherHandWeapon = otherLimb.getWeapon(this);
                  if ((otherHandWeapon != null) && (otherHandWeapon.equals(weap))) {
                     showLimbNames = true;
                  }
               }
               if (limb.canAttack(this)) {
                  weaponReady = true;
               }
               else {
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
                        }
                        else {
                           sb.append(actions).append(" actions");
                        }
                        sb.append(" to re-ready before you may attack.");
                     }
                  }
                  else {
                     // arm crippled, or no weapon.
                  }
               }
            }
            boolean showPain = false;
            if (!weaponReady) {
               if ((_currentSpell != null)
                   && (delayedTarget == null)
                   && ((_currentSpell.getTargetType() == TargetType.TARGET_OTHER_FIGHTING)
                       || (_currentSpell.getTargetType() == TargetType.TARGET_ANIMAL_FIGHTING)
                       || (_currentSpell.getTargetType() == TargetType.TARGET_OTHER_EVIL_FIGHTING)
                       || (_currentSpell.getTargetType() == TargetType.TARGET_OTHER_GOOD_FIGHTING)
                       || (_currentSpell.getTargetType() == TargetType.TARGET_UNDEAD))) {
                  if (target == null) {
                     sb.append("\nAll of your selected targets are currently out of range, or not visible this round.");
                  }
                  else {
                     showPain = true;
                  }
               }
               else {
                  sb.append("\nYour weapon is not ready to attack this round.");
               }
            }
            else if (target == null) {
               sb.append("\nAll of your selected targets are currently out of range, or not visible this round.");
            }
            else {
               showPain = true;
            }
            if (showPain) {
               if ((delayedTarget == null) && (target != null)) {
                  sb.append("\nYour currently selected target is ").append(target.getName()).append(".");
               }
               int limbsWithWeaponCount = 0;
               Limb singleWeaponHand = null;
               for (Limb limb : _limbs.values()) {
                  if (limb.getWeapon(this) != null) {
                     limbsWithWeaponCount++;
                     singleWeaponHand = limb;
                  }
               }
               if ((limbsWithWeaponCount == 0) && (_currentSpell != null)) {
                  sb.append(" \nYou have no weapon with which to attack.");
               }
               else {
                  int messageSizeBeforePenalties = sb.length();
                  byte painPenalty = getCondition().getPenaltyPain();
                  if ((limbsWithWeaponCount == 1) && (_currentSpell == null)) {
                     byte attackPenalty = getPenaltyToUseArm(singleWeaponHand, true/*includeWounds*/, true/*includePain*/);
                     if (attackPenalty > 0) {
                        if (isBerserking() && (painPenalty > 0)) {
                           sb.append("\nBecause you are Berserking, you are unaffected by your ");
                           sb.append(painPenalty).append(" points of pain.");
                           attackPenalty -= painPenalty;
                        }
                        sb.append(" \nYou may attack at a -").append(attackPenalty).append(" due to ");
                        ArrayList<String> penalties = new ArrayList<>();
                        if ((_condition.getPenaltyPain() > 0) && !isBerserking()) {
                           penalties.add(String.valueOf(_condition.getPenaltyPain()) + " points of pain");
                        }
                        if (_condition.getWounds() > 0) {
                           penalties.add(String.valueOf(_condition.getWounds()) + " wounds");
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
                              }
                              else if (penalties.size() == 1) {
                                 sb.append(" and ");
                              }
                           }
                        }
                        sb.append(".");
                     }
                  }
                  if ((limbsWithWeaponCount > 1) || (_currentSpell != null)) {
                     byte baseAttackPenalty = _condition.getWoundsAndPainPenalty();
                     if (baseAttackPenalty > 0) {
                        if (isBerserking() && (painPenalty > 0)) {
                           sb.append("\nBecause you are Berserking, you are unaffected by your ");
                           sb.append(painPenalty).append(" points of pain.");
                           baseAttackPenalty -= painPenalty;
                        }
                        sb.append(" \nDue to ");
                        if ((_condition.getPenaltyPain() > 0) && !isBerserking()) {
                           sb.append(_condition.getPenaltyPain()).append(" points of pain");
                           if (_condition.getWounds() > 0) {
                              sb.append(" and ");
                           }
                        }
                        if (_condition.getWounds() > 0) {
                           sb.append(_condition.getWounds()).append(" wounds");
                        }
                        if (limbsWithWeaponCount == 0) {
                           sb.append(", all spells ");
                        }
                        else {
                           sb.append(", all attacks ");
                           if (_currentSpell != null) {
                              sb.append("and spells ");
                           }
                        }
                        sb.append("will be at a -").append(baseAttackPenalty);
                        sb.append(".");
                     }
                     for (Limb limb : _limbs.values()) {
                        Weapon weap = limb.getWeapon(this);
                        if (weap != null) {
                           if (limb.canAttack(this)) {
                              Limb otherLimb = _limbs.get(limb._limbType.getPairedType());
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
                                 if ((_condition.getPenaltyPain() > 0) || (_condition.getWounds() > 0)) {
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
                     }
                     else {
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
                     }
                     else {
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
            for (Limb limb : _limbs.values()) {
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
            if ((rangedWeapon != null) || ((_currentSpell != null) && (_currentSpell instanceof IRangedSpell))) {
               RANGE range = RANGE.OUT_OF_RANGE;
               short rangeBase = -1;
               short adjustedRangeBase = -1;
               String rangeAdjustingAttributeName = "";
               if (rangedStyle != null) {
                  range = rangedStyle.getRangeForDistance(minDistanceToTarget, getAdjustedStrength());
                  rangeBase = rangedStyle.getRangeBase();
                  adjustedRangeBase = (short) Math.round(rangeBase * Rules.getRangeAdjusterForAdjustedStr(getAdjustedStrength()));
                  rangeAdjustingAttributeName = Attribute.Strength.shortName;
               }
               else if (_currentSpell != null) {
                  range = _currentSpell.getRange(minDistanceToTarget);
                  rangeBase = ((IRangedSpell)_currentSpell).getRangeBase();
                  adjustedRangeBase = _currentSpell.getAdjustedRangeBase();
                  Attribute attr = _currentSpell.getCastingAttribute();
                  rangeAdjustingAttributeName = attr.shortName;
               }
               if (!hasLineOfSightToTarget) {
                  sb.append("\nYou do not have a clear line of site to ").append(target.getName()).append(".");
               }
               else {
                  if (_aimDuration > 0) {
                     sb.append("\nYou have been aiming at ").append(target.getName());
                     sb.append(" for ").append(_aimDuration).append(" rounds. ");
                     sb.append("\nYour target is ");
                  }
                  else {
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
                  }
                  else {
                     sb.append("\nYour ").append(_currentSpell.getName()).append(" spell");
                  }
                  sb.append(" has a base range of ").append(rangeBase);
                  if (adjustedRangeBase != rangeBase) {
                     sb.append(" (").append(rangeAdjustingAttributeName).append(" and size adjusted to ").append(adjustedRangeBase).append(")");
                  }
                  sb.append(", making this ");
                  if ((range == RANGE.OUT_OF_RANGE) && (maxDistanceToTarget < 2)) {
                     sb.append(" too close");
                  }
                  else {
                     sb.append(range.getName()).append(" range");
                  }
                  byte rangePenalty = (byte) (0 - Rules.getRangeDefenseAdjustmentToPD(range));
                  if (rangePenalty > 0) {
                     sb.append(", for a bonus of +").append(rangePenalty).append(" to hit.");
                  }
                  else if (rangePenalty < 0) {
                     sb.append(", for a penalty of ").append(rangePenalty).append(" to hit.");
                  }
                  else if (range != RANGE.OUT_OF_RANGE) {
                     sb.append(" (+0 to hit).");
                  }
                  else {
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
                  }
                  else {
                     if (_currentSpell != null) {
                        spellIsInRange = false;
                     }
                  }
               }
            }
            if (_currentSpell != null) {
               if (_currentSpell.requiresTargetToCast() && !_currentSpell.isBeneficial()) {
                  byte rangePenalty = _currentSpell.getRangeTNAdjustment(minDistanceToTarget);
                  if ((rangePenalty != 0) || (_currentSpell instanceof PriestSpell)) {
                     if (!rangeShown) {
                        sb.append("\nYour target is ");
                        sb.append(minDistanceToTarget);
                        sb.append(" hexes away.");
                     }
                     sb.append("\nYour ").append(_currentSpell.getName()).append(" spell");
                     if (rangePenalty != 0) {
                        sb.append(" will be cast at a penalty of ");
                        sb.append(rangePenalty).append(".");
                     }
                     if (_currentSpell instanceof PriestSpell) {
                        PriestSpell priestSpell = (PriestSpell) _currentSpell;
                        RANGE range = _currentSpell.getRange(minDistanceToTarget);
                        spellPowerPenalty = priestSpell.getPowerReductionForRange(minDistanceToTarget, range);
                        spellPowerPenalty += getPainPenalty(true/*accountForBerserking*/);
                        sb.append(" will have a power penalty of ");
                        sb.append(spellPowerPenalty).append(".");
                     }
                  }
               }
            }
         }
         if (_currentSpell instanceof PriestSpell) {
            byte spellPowerPenaltyForPain = getPainPenalty(true/*accountForBerserking*/);
            if (spellPowerPenaltyForPain != 0) {
               sb.append("\nBecause of your pain, your ").append(_currentSpell.getName()).append(" spell");
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
               int aimDuration = targeter.getAimDuration(_uniqueID);
               if (aimDuration != 1) {
                  sb.append(aimDuration).append(" rounds)");
               }
               else {
                  sb.append(aimDuration).append(" round)");
               }
               if ((i + 1) < charactersTargetingActor.size()) {
                  if ((i + 2) == charactersTargetingActor.size()) {
                     sb.append(" and ");
                  }
                  else {
                     sb.append(", ");
                  }
               }
            }
         }
         if (getPosition() != Position.STANDING) {
            sb.append(" \nYou are currently ").append(getPositionName());
            if (_condition.getPenaltyMove() < 0) {
               sb.append(", due to a crippling leg wound, so you are unable to stand.");
            }
            else {
               //               if (getPosition() != _condition.getMovingToPosition()) {
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
         boolean isBeingHeld = (_heldPenalties.size() > 0);
         int availActions = _condition.getAvailableActions(sbReasons, isBeingHeld);
         //         if (_condition.isConscious() && !_condition.isCollapsed()) {
         //            if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) >0) {
         //               availActions |= _orientation.getAvailablePositions();
         //            }
         //         }
         if (_heldPenalties.size() > _placedIntoHoldThisTurn.size()) {
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
         ArrayList<Orientation> advOrients = _condition.getPossibleAdvanceOrientations(arena.getCombatMap());
         HashMap<Orientation, List<Orientation>> mapOrientationToNextOrientationsLeadingToChargeAttack = new HashMap<>();
         if (target != null) {
            if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) > 2) {
               getOrientation().getPossibleChargePathsToTarget(arena.getCombatMap(), this, target,
                                                               getAvailableMovement(false/*movingEvasively*/),
                                                               mapOrientationToNextOrientationsLeadingToChargeAttack);
            }
         }
         for (Limb limb : _limbs.values()) {
            // adjust the listed distance of each of our limb locations
            if (target != null) {
               ArenaLocation limbLoc = getLimbLocation(limb._limbType, arena.getCombatMap());
               minDistanceToTarget = Arena.getShortestDistance(limbLoc, target.getOrientation());
               maxDistanceToTarget = Arena.getFarthestDistance(limbLoc, target.getOrientation());
            }
            Weapon weap = limb.getWeapon(this);
            if (weap != null) {
               String handName = "";
               if (limb instanceof Hand) {
                  boolean showHandNames = false;
                  LimbType otherLimbType = limb._limbType.getPairedType();
                  Limb pairedLimb = _limbs.get(otherLimbType);
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
                  ArrayList<String> attackStyleNames = new ArrayList<>();
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
               boolean inRange = true;
               boolean canPrepare = (limb instanceof Hand) && ((Hand) limb).canPrepare();
               if (canPrepare) {
                  if (limb.isCrippled()) {
                     canPrepare = false;
                  }
                  else {
                     if (getCondition().isCollapsed()) {
                        canPrepare = false;
                     }
                     else if (rangedStyle.getHandsRequired() == 2) {
                        Limb otherHand = _limbs.get(limb._limbType.getPairedType());
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
                                                        RequestActionType.OPT_PREPARE_RANGED, limb._limbType,
                                                        !isBerserking() && !isPacified && !isParalyzed && !isSwimming));
               }
               if (limb.canAttack(this)) {
                  weaponReady = true;
                  if (((availActions & ACTION_ATTACK) != 0) && (target != null)) {
                     if (rangedStyle != null) {
                        // ranged attack require the attacker to face the defender:
                        if (isFacingTarget) {
                           if (inRange && !canPrepare) {
                              RequestActionOption targetOpt = new RequestActionOption ("aim " + weapName + " at " + target.getName(),
                                                                   RequestActionType.OPT_TARGET_ENEMY, limb._limbType,
                                                                   !isPacified && !isParalyzed && !isSwimming);
                              req.addOption(targetOpt);
                              if (_aimDuration > 0) {
                                 if (weap.isMissileWeapon()) {
                                    RequestActionOption option = new RequestActionOption("fire " + weapName + " at " + target.getName(),
                                                                          RequestActionType.OPT_ATTACK_MISSILE,
                                                                          limb._limbType,
                                                                          !isPacified && !isParalyzed && !isSwimming);

                                    req.addOption(option);
                                    if (_aimDuration >= 3) {
                                       defaultOption = option;
                                    }
                                 }
                                 else {
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
                                                                           RequestActionType.OPT_ATTACK_THROW_3, limb._limbType,
                                                                           !isPacified && !isParalyzed && !isSwimming);
                                          req.addOption(option);
                                          defOption = option;
                                       case 2:
                                          option = new RequestActionOption(attackName + " (2-actions)",
                                                                           RequestActionType.OPT_ATTACK_THROW_2, limb._limbType,
                                                                           !isPacified && !isParalyzed && !isSwimming);
                                          req.addOption(option);
                                          if (defOption == null) {
                                             defOption = option;
                                          }
                                       case 1:
                                          option = new RequestActionOption(attackName + " (1-action)",
                                                                           RequestActionType.OPT_ATTACK_THROW_1, limb._limbType,
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
                                                                             RequestActionType.OPT_ATTACK_MELEE_3, limb._limbType,
                                                                             !isPacified && !isParalyzed && !isSwimming));
                                    case 2:
                                       req.addOption(new RequestActionOption("attack target (2-actions, " + weapName + ")",
                                                                             RequestActionType.OPT_ATTACK_MELEE_2, limb._limbType,
                                                                             !isPacified && !isParalyzed && !isSwimming));
                                    case 1:
                                       req.addOption(new RequestActionOption("attack target (1-action, " + weapName + ")",
                                                                             RequestActionType.OPT_ATTACK_MELEE_1, limb._limbType,
                                                                             !isPacified && !isParalyzed && !isSwimming));
                                       attackOptAvailable = true;
                                 }
                              }
                              if (weap.canGrappleAttack(this)) {
                                 if (!grappleAttackListed ) {
                                    switch (getActionsAvailableThisRound(false/*usedForDefenseOnly*/)) {
                                       case 7:
                                       case 6:
                                       case 5:
                                       case 4:
                                       case 3:
                                          req.addOption(new RequestActionOption(
                                                                                "grab target (3-actions)",
                                                                                RequestActionType.OPT_ATTACK_GRAPPLE_3, limb._limbType,
                                                                                !isPacified && !isParalyzed && !isSwimming));
                                       case 2:
                                          req.addOption(new RequestActionOption("grab target (2-actions)",
                                                                                RequestActionType.OPT_ATTACK_GRAPPLE_2, limb._limbType,
                                                                                !isPacified && !isParalyzed && !isSwimming));
                                       case 1:
                                          req.addOption(new RequestActionOption("grab target (1-action)",
                                                                                RequestActionType.OPT_ATTACK_GRAPPLE_1, limb._limbType,
                                                                                !isPacified && !isParalyzed && !isSwimming));
                                          attackOptAvailable = true;
                                    }
                                    grappleAttackListed = true;
                                 }
                              }
                           }
                        }
                        else if (((availActions & ACTION_MOVE) != 0) && (delayedTarget == null)) {
                           boolean advanceAllowsAttack = false;
                           boolean advanceIsTurn = false;
                           for (Orientation advOrientation : advOrients) {
                              if (advOrientation.canLimbAttack(this, target, limb, arena.getCombatMap(), false/*allowRanged*/, false/*onlyChargeTypes*/)) {
                                 advanceAllowsAttack = true;
                                 if (advOrientation.getCoordinates().containsAll(_condition.getCoordinates())) {
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
                                                                                RequestActionType.OPT_CHARGE_ATTACK_3, limb._limbType,
                                                                                !isPacified && !isParalyzed && !isSwimming));
                                       }
                                       req.addOption(new RequestActionOption("Charge " + target.getName() + " and attack (3-actions, " + style.getWeapon().getName() + ")",
                                                                             RequestActionType.OPT_CHARGE_ATTACK_2, limb._limbType,
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
                                                                                RequestActionType.OPT_CLOSE_AND_ATTACK_3, limb._limbType,
                                                                                !isPacified && !isParalyzed && !isSwimming));
                                       case 3:
                                          req.addOption(new RequestActionOption(verb + " on target and attack (2-actions, " + weapName + ")",
                                                                                RequestActionType.OPT_CLOSE_AND_ATTACK_2, limb._limbType,
                                                                                !isPacified && !isParalyzed && !isSwimming));
                                       case 2:
                                          req.addOption(new RequestActionOption(verb + " on target and attack (1-action, " + weapName + ")",
                                                                                RequestActionType.OPT_CLOSE_AND_ATTACK_1, limb._limbType,
                                                                                !isPacified && !isParalyzed && !isSwimming));
                                    }
                                 }
                                 if (weap.canGrappleAttack(this)) {
                                    if (!grappleAttackListed ) {
                                       switch (getActionsAvailableThisRound(false/*usedForDefenseOnly*/)) {
                                          case 7:
                                          case 6:
                                          case 5:
                                          case 4:
                                             req.addOption(new RequestActionOption(verb + " on target and grab (3-actions)",
                                                                                   RequestActionType.OPT_CLOSE_AND_GRAPPLE_3, limb._limbType,
                                                                                   !isPacified && !isParalyzed && !isSwimming));
                                          case 3:
                                             req.addOption(new RequestActionOption(verb + " on target and grab (2-actions)",
                                                                                   RequestActionType.OPT_CLOSE_AND_GRAPPLE_2, limb._limbType,
                                                                                   !isPacified && !isParalyzed && !isSwimming));
                                          case 2:
                                             req.addOption(new RequestActionOption(verb + " on target and grab (1-action)",
                                                                                   RequestActionType.OPT_CLOSE_AND_GRAPPLE_1, limb._limbType,
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
                                                                   RequestActionType.OPT_READY_5, limb._limbType,
                                                                   canBeReadied));
                     case 4: req.addOption(new RequestActionOption("ready " + weapName + " (4-actions)",
                                                                   RequestActionType.OPT_READY_4, limb._limbType,
                                                                   canBeReadied));
                     case 3: req.addOption(new RequestActionOption("ready " + weapName + " (3-actions)",
                                                                   RequestActionType.OPT_READY_3, limb._limbType,
                                                                   canBeReadied));
                     case 2: req.addOption(new RequestActionOption("ready " + weapName + " (2-actions)",
                                                                   RequestActionType.OPT_READY_2, limb._limbType,
                                                                   canBeReadied));
                     case 1: req.addOption(new RequestActionOption("ready " + weapName + " (1-action)",
                                                                   RequestActionType.OPT_READY_1, limb._limbType,
                                                                   canBeReadied));
                  }
               }
            } // if (weap != null) {
            else {// (weap == null)
               Thing heldThing = limb.getHeldThing();
               if (heldThing != null) {
                  if (heldThing.canBeApplied()) {
                     req.addOption(new RequestActionOption(heldThing.getApplicationName() + " (1-action)",
                                                           RequestActionType.OPT_APPLY_ITEM, limb._limbType,
                                                           true));
                  }
               }
            }
         } // for (Limb limb : _limbs.values()) {


         if (!columnSpacerAdded && (req.getActionCount() >= maxEntriesPerColumn)) {
            req.addSeparatorOption();
            columnSpacerAdded = true;
         }

         if (delayedTarget == null) {
            if ((availActions & ACTION_MOVE) != 0) {
               RequestActionOption moveOpt;
               if (_condition.getPosition() == Position.STANDING) {
                  // Berserkers and animals are not allowed to move evasively
                  boolean evasiveMoveAllowed = !isBerserking() && !_race.isAnimal();
                  req.addOption(new RequestActionOption("move evasively", RequestActionType.OPT_MOVE_EVASIVE, LimbType.BODY, evasiveMoveAllowed));
                  //req.addOption(RequestAction.OPT_MOVE_EVASIVE, "move evasively", evasiveMoveAllowed);
                  moveOpt = new RequestActionOption("move", RequestActionType.OPT_MOVE, LimbType.BODY, true);
                  req.addOption(moveOpt);
                  //req.addOption(RequestAction.OPT_MOVE, "move", true);
               }
               else {
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
               }
               else {
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
            if (_condition.getActionsSpentThisRound() > 0) {
               req.addOption(new RequestActionOption("on gaurd (0-actions, you have already acted this round)", RequestActionType.OPT_ON_GAURD, LimbType.BODY, true/*enabled*/));
               //req.addOption(RequestAction.OPT_ON_GAURD, "on gaurd (0-actions, you have already acted this round)", true/*enabled*/);
            }
            else {
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
         }
         else {
            req.addOption(new RequestActionOption("do nothing", RequestActionType.OPT_ON_GAURD, LimbType.BODY, true/*enabled*/));
            //req.addOption(RequestAction.OPT_ON_GAURD, "do nothing", true);
         }

         // If we have no equipment ready, and none in our _equipment list, don't offer equip option.
         boolean objectHeld = false;
         for (Hand hand : getArms()) {
            if (!hand.isEmpty()) {
               objectHeld = true;
               break;
            }
         }
         if (objectHeld || (_equipment.size() != 0)) {
            RequestEquipment reqEquip = getEquipmentRequest();
            int enabledCount = reqEquip.getEnabledCount(false/*includeCancelAction*/);
            if (enabledCount > 0) {
               String singleItemAction = null;
               if (enabledCount == 1) {
                  singleItemAction = reqEquip.getSingleEnabledAction();
                  req.addOption(new RequestActionOption("[un]equip gear or weapons: " + singleItemAction, RequestActionType.OPT_EQUIP_UNEQUIP, LimbType.BODY, true/*enabled*/));
                  //req.addOption(RequestAction.OPT_EQUIP_UNEQUIP, "[un]equip gear or weapons: " + singleItemAction, true/*enabled*/);
               }
               else {
                  req.addOption(new RequestActionOption("[un]equip gear or weapons", RequestActionType.OPT_EQUIP_UNEQUIP, LimbType.BODY, true/*enabled*/));
                  //req.addOption(RequestAction.OPT_EQUIP_UNEQUIP, "[un]equip gear or weapons", true/*enabled*/);
               }
            }
         }
         if (delayedTarget == null) {
            if ((availActions & ACTION_POSITION) != 0) {
               int actionsNeededToChangePos = RequestActionType.OPT_CHANGE_POS.getActionsUsed((byte) 0);
               if (_condition.getActionsAvailableThisRound(false/*useForDefenseOnly*/) >= actionsNeededToChangePos) {
                  req.addOption(new RequestActionOption("change position (" + actionsNeededToChangePos + " actions)", RequestActionType.OPT_CHANGE_POS, LimbType.BODY, true/*enabled*/));
                  //req.addOption(RequestAction.OPT_CHANGE_POS, "change position (" + actionsNeededToChangePos + " actions)", true);
               }
            }
         }
         // Berserkers can't change their targets
         req.addOption(new RequestActionOption("change target", RequestActionType.OPT_CHANGE_TARGET_PRIORITIES, LimbType.BODY, (!isBerserking() && (delayedTarget == null))/*enabled*/));
         //req.addOption(RequestAction.OPT_CHANGE_TARGET_PRIORITIES, "change target", (!isBerserking() && (delayedTarget == null)));
         if (!_condition.isCollapsed()) {
            if (_currentSpell == null) {
               ArrayList<Spell> inateSpells = _race.getInateSpells();
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

            if ((_knownMageSpellsList.size() > 0) || (getPriestDeities().size() > 0) || (_currentSpell != null)) {
               byte offensiveActionsAvailable = getActionsAvailableThisRound(false/*usedForDefenseOnly*/);
               if (_currentSpell == null) {
                  short mageSpellPointsLeft = _condition.getMageSpellPointsAvailable();
                  short priestSpellPointsLeft = _condition.getPriestSpellPointsAvailable();
                  boolean enabled = true;
                  StringBuilder optName = new StringBuilder();
                  if ((priestSpellPointsLeft == 0) && (mageSpellPointsLeft == 0)) {
                     enabled = false;
                     optName.append("begin spell (no spell points left)");
                  }
                  else if ((mageSpellPointsLeft > 0) && (priestSpellPointsLeft == 0)) {
                     optName.append("begin mage spell (").append(mageSpellPointsLeft).append(" points left)");
                  }
                  else if ((mageSpellPointsLeft == 0) && (priestSpellPointsLeft > 0)) {
                     optName.append("begin priest spell (").append(priestSpellPointsLeft).append(" points left)");
                  }
                  else if ((mageSpellPointsLeft > 0) && (priestSpellPointsLeft > 0)) {
                     optName.append("begin spell (")
                            .append(mageSpellPointsLeft).append(" mage points and ")
                            .append(priestSpellPointsLeft).append(" priest points left)");
                  }
                  req.addOption(new RequestActionOption(optName.toString(), RequestActionType.OPT_BEGIN_SPELL, LimbType.BODY, enabled));
                  //req.addOption(RequestAction.OPT_BEGIN_SPELL, optName.toString(), enabled);
               }
               else {
                  // Inate spells don't need to be incanted, channeled or maintained
                  if (!_currentSpell.isInate()) {
                     if (_currentSpell.getIncantationRoundsRequired() > 0) {
                        String name = "continue incantation of spell (" + _currentSpell.getIncantationRoundsRequired() + " rounds remaining)";
                        defaultOption = new RequestActionOption(name, RequestActionType.OPT_CONTINUE_INCANTATION, LimbType.BODY, true);
                        req.addOption(defaultOption);
                        //req.addOption(RequestAction.OPT_CONTINUE_INCANTATION, name, true);
                     }
                     else {
                        if (_currentSpell instanceof MageSpell) {
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
                                 if (((MageSpell) _currentSpell).getSpellPoints() > 0) {
                                    actionStr.append(" more");
                                 }
                                 actionStr.append(" spell point");
                                 if (energy > 1) {
                                    actionStr.append("s");
                                 }
                                 actionStr.append(", out of ").append(_condition.getMageSpellPointsAvailable()).append(")");
                                 boolean enabled = energy <= _condition.getMageSpellPointsAvailable();
                                 req.addOption(new RequestActionOption(actionStr.toString(), action, LimbType.BODY, enabled));
                                 //req.addOption(actionID, actionStr.toString(), enabled);
                              }
                           }
                        }
                     }
                  }
                  req.addOption(new RequestActionOption("discard spell", RequestActionType.OPT_DISCARD_SPELL, LimbType.BODY, true/*enabled*/));
                  //req.addOption(RequestAction.OPT_DISCARD_SPELL, "discard spell", true);
                  if (_currentSpell.getIncantationRoundsRequired() == 0) {
                     if (offensiveActionsAvailable > 0) {
                        if (_currentSpell instanceof MageSpell) {
                           if (_currentSpell.getPower() > 0) {
                              // Inate spells don't need to be incanted, channeled or maintained
                              if (!_currentSpell.isInate()) {
                                 if (!_currentSpell.isMaintainedThisTurn()) {
                                    RequestActionOption opt = new RequestActionOption("maintain spell", RequestActionType.OPT_MAINTAIN_SPELL, LimbType.BODY, true/*enabled*/);
                                    req.addOption(opt);
                                    //req.addOption(RequestAction.OPT_MAINTAIN_SPELL, "maintain spell", true);
                                    if (offensiveActionsAvailable == 1) {
                                       defaultOption = opt;
                                    }
                                 }
                              }
                              if (_currentSpell instanceof MageSpell) {
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
                                       StringBuilder actionStr = new StringBuilder();
                                       actionStr.append("complete spell (").append(action).append("-actions)");
                                       boolean enabled = (spellIsInRange && hasLineOfSightToTarget) ||
                                                         _currentSpell.isBeneficial() || !_currentSpell.requiresTargetToCast();
                                       req.addOption(new RequestActionOption(actionStr.toString(), actionType, LimbType.BODY, enabled));
                                       //req.addOption(actionID, actionStr.toString(), enabled);
                                    }
                                 }
                              }
                           }
                        }
                        else if (_currentSpell instanceof PriestSpell) {
                           // Inate spells don't need to be incanted or channeled or maintained
                           if (!_currentSpell.isInate()) {
                              if (!_currentSpell.isMaintainedThisTurn()) {
                                 RequestActionOption opt = new RequestActionOption("maintain spell", RequestActionType.OPT_MAINTAIN_SPELL, LimbType.BODY, true);
                                 req.addOption(opt);
                                 //req.addOption(RequestAction.OPT_MAINTAIN_SPELL, "maintain spell", true);
                                 if (offensiveActionsAvailable == 1) {
                                    defaultOption = opt;
                                 }
                              }
                           }

                           int divinePower = 0;
                           // inate spells don't need divine power. They have their maximum power
                           // set as their 'power' attribute.
                           if (!_currentSpell.isInate()) {
                              Advantage adv = getAdvantage(Advantage.DIVINE_POWER);
                              divinePower = adv.getLevel() + 1;
                           }
                           else {
                              divinePower = _currentSpell.getPower();
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
                              int spellPoints = ((PriestSpell) _currentSpell).getAffinity() * p;
                              StringBuilder actionStr = new StringBuilder();
                              actionStr.append("complete spell (").append(p).append("-power");
                              boolean enabled = spellIsInRange  && (p > spellPowerPenalty) &&
                                       (hasLineOfSightToTarget || !_currentSpell.requiresTargetToCast() || _currentSpell.isBeneficial());
                              if (!_currentSpell.isInate()) {
                                 actionStr.append(", using ").append(spellPoints).append(" spell points");
                                 actionStr.append(", out of ").append(_condition.getPriestSpellPointsAvailable());
                                 enabled = enabled && (spellPoints <= _condition.getPriestSpellPointsAvailable());
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
      byte terrainAdj = (byte) (0 - getOrientation().getAttackPenaltyForTerrain(this, map, terrainNames));
      if (terrainExplanation != null) {
         while (terrainNames.size() > 0) {
            String terrainName = terrainNames.remove(0);
            if (terrainExplanation.length() > 0) {
               if (terrainNames.size() > 0) {
                  terrainExplanation.append(", ");
               }
               else {
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
      LimbType pairedLimbType = limb._limbType.getPairedType();
      Weapon thing = limb.getWeapon(this);
      if (thing != null) {
         boolean canUseTwoHands = false;
         Limb pairedLimb = _limbs.get(pairedLimbType);
         if ((pairedLimb != null) && (!pairedLimb.isCrippled())) {
            if (pairedLimb.getHeldThing() == null) {
               canUseTwoHands = true;
            }
         }
         for (WeaponStyleAttack style : thing.getAttackStyles()) {
            int minSkill = style.getMinSkill();
            if (minSkill > 0) {
               Skill skill = getSkill(style.getSkillType());
               if ((skill == null) || (skill.getLevel() < minSkill)) {
                  continue;
               }
            }
            if ((style._handsRequired == 2) && !canUseTwoHands) {
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
         if ((equipment != null) && (equipment instanceof Weapon)) {
            if (((Weapon) equipment).isUnarmedStyle()) {
               return (Weapon) equipment;
            }
         }
      }
      return null;
   }

   public boolean isMovingEvasively() {
      return _condition.isMovingEvasively();
   }

   public boolean hasMovedLastAction() {
      return _condition.hasMovedLastAction();
   }

   public RequestDefense getDefenseRequest(int attackingCombatantIndex, Character attacker,
                                           RequestAction attack, Arena arena, boolean forCounterAttack) {
      updateWeapons();
      short minDistance = Arena.getMinDistance(attacker, this);
      RANGE range = attack.getRange(attacker, minDistance);
      RequestDefense req = new RequestDefense(attacker, attack, range);

      addDefenseOption(req, new DefenseOptions(DefenseOption.DEF_PD), new DefenseOptions(DefenseOption.DEF_PD), (byte) 0/*attackingWeaponsParryPenalty*/, range, attack);
      if (stillFighting()) {
         boolean rangedAttack = attack.isRanged();
         boolean grappleAttack = attack.isGrappleAttack();
         boolean chargeAttack = attack.isCharge();
         DefenseOptions availActions = _condition.getAvailableDefenseOptions();

         if (!forCounterAttack) {
            // a counter-attack may not be counter-attacked
            boolean holdingWeapon = false;
            for (Limb limb : _limbs.values()) {
               if (limb instanceof Hand) {
                  Thing weap = limb.getHeldThing();
                  if ((weap != null) && (weap.isReal())) {
                     holdingWeapon = true;
                     break;
                  }
               }
            }
            if (!holdingWeapon) {
               // allow counter attack options, if the limb allows it (based on aikido skill):
               availActions.add(DefenseOption.DEF_COUNTER_GRAB_1,
                                DefenseOption.DEF_COUNTER_GRAB_2,
                                DefenseOption.DEF_COUNTER_GRAB_3,
                                DefenseOption.DEF_COUNTER_THROW_1,
                                DefenseOption.DEF_COUNTER_THROW_2,
                                DefenseOption.DEF_COUNTER_THROW_3);
            }
         }

         SpellParalyze paralyzeSpell = null;
         for (Spell spell : _activeSpellsList) {
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
            for (Limb limb : _limbs.values()) {
               if (limb.canDefend(this, rangedAttack, attack.isCharge(), grappleAttack, attack.getDamageType(), true)) {
                  availActions.add(limb.getDefOption());
               }
            }
         }


         boolean spellDef = false;
         if (attack.isRanged()) {
            if (_bestDefensiveSpell_ranged != null) {
               spellDef = true;
            }
         }
         else if (attack.isCompleteSpell()) {
            if (_bestDefensiveSpell_spell != null) {
               spellDef = true;
            }
         }
         else if (_bestDefensiveSpell_melee != null) {
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
            for (Limb limb : _limbs.values()) {
               if (!limb.canDefend(this, rangedAttack, attack.isCharge(),grappleAttack, attack.getDamageType(), true)) {
                  availActions.remove(limb.getDefOption());
               }
            }
         }
         byte defenseAdjForRangePerAction = Rules.getRangeDefenseAdjustmentPerAction(range);
         if (availActions.contains(DefenseOption.DEF_RETREAT)) {
            if (_heldPenalties.size() > 0) {
               retreatHeld = true;
               availActions.remove(DefenseOption.DEF_RETREAT);
            }
            else if (!arena.canRetreat(this, attacker.getLimbLocation(attack.getLimb(), arena.getCombatMap()))) {
               retreatBlocked = true;
               availActions.remove(DefenseOption.DEF_RETREAT);
            }
         }
         if (isBerserking()) {
            // Berserking characters can only dodge as a defense.
            availActions.remove(DefenseOption.DEF_DODGE);
         }
         StringBuilder sb = new StringBuilder();
         sb.append(getName()).append(", you have ").append(_condition.getActionsAvailable(true/*usedForDefenseOnly*/)).append(" actions remaining");
         byte actionsAvailableThisRound = getActionsAvailableThisRound(true/*usedForDefenseOnly*/);
         if (actionsAvailableThisRound == _condition.getActionsAvailable(true/*usedForDefenseOnly*/)) {
            sb.append(".");
         }
         else {
            sb.append(" for this turn, ").append(actionsAvailableThisRound).append(" can be spent this round.");
         }
         if (forCounterAttack) {
            sb.append(" \nYou are being counter-attacked by ").append(attacker.getName());
         }
         else if (attack.isGrappleAttack()) {
            sb.append(" \nYou are being grabbed by ").append(attacker.getName());
         }
         else {
            sb.append(" \nYou are being attacked by ").append(attacker.getName());
            Weapon attackingWeapon = attack.getAttackingWeapon(attacker);
            if (attackingWeapon != null) {
               sb.append(", using a ");
               if (attackingWeapon.isReal()) {
                  sb.append(attackingWeapon.getName());
               }
               else if (attackingWeapon.isMissileWeapon()) {
                  // missile spells return 'false' to the isReal() call
                  sb.append(attackingWeapon.getName());
               }
               else {
                  sb.append(attackMode.getName());
               }
            }
            else {
               Spell spell = attack.getSpell();
               if (spell != null) {
                  sb.append(", casting a '").append(spell.getName()).append("' spell");
               }
            }
         }

         if (attack.isRangedAttack()) {
            // If we are being attack by a missile weapon, display how long the
            // attacker has been aiming at us for.
            int aimDuration = attacker.getAimDuration(_uniqueID);
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
            }
            else {
               effectiveActions += Math.min(2, aimDuration) - 1;
            }

            sb.append(" effectively using ").append(effectiveActions).append(" actions");
         }
         else {
            if (forCounterAttack) {
               sb.append(" (").append(attack.getActionsUsed()).append(" actions");
            }
            else {
               sb.append(" (").append(attack.getAttackActions(true/*considerSpellAsAttack*/)).append(" actions");
            }
            if (attack.isCharge()) {
               sb.append(" charging attack");
            }
            sb.append(")");
         }
         byte woundPenalty = _condition.getWounds();
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
                                            attackingWeaponsParryPenalty, rangedAttack, chargeAttack, grappleAttack,
                                            damType, false/*defenseAppliedAlready*/, range);
            int dodge2 = getDefenseOptionTN(new DefenseOptions(DefenseOption.DEF_DODGE), minDam, false/*includeWoundPenalty*/,
                                            true/*includeHolds*/, true/*includePos*/, false/*includeMassiveDam*/,
                                            attackingWeaponsParryPenalty, rangedAttack, chargeAttack, grappleAttack,
                                            damType, false/*defenseAppliedAlready*/, range);
            int retreat1 = getDefenseOptionTN(new DefenseOptions(DefenseOption.DEF_RETREAT), minDam, false/*includeWoundPenalty*/,
                                              true/*includeHolds*/, false/*includePos*/, false/*includeMassiveDam*/,
                                              attackingWeaponsParryPenalty, rangedAttack, chargeAttack, grappleAttack,
                                              damType, false/*defenseAppliedAlready*/, range);
            int retreat2 = getDefenseOptionTN(new DefenseOptions(DefenseOption.DEF_RETREAT), minDam, false/*includeWoundPenalty*/,
                                              true/*includeHolds*/, true/*includePos*/, false/*includeMassiveDam*/,
                                              attackingWeaponsParryPenalty, rangedAttack, chargeAttack, grappleAttack,
                                              damType, false/*defenseAppliedAlready*/, range);
            int parry1 = getDefenseOptionTN(new DefenseOptions(DefenseOption.DEF_RIGHT), minDam, false/*includeWoundPenalty*/,
                                            true/*includeHolds*/, false/*includePos*/, false/*includeMassiveDam*/,
                                            attackingWeaponsParryPenalty, rangedAttack, chargeAttack, grappleAttack,
                                            damType, false/*defenseAppliedAlready*/, range);
            int parry2 = getDefenseOptionTN(new DefenseOptions(DefenseOption.DEF_RIGHT), minDam, false/*includeWoundPenalty*/,
                                            true/*includeHolds*/, true/*includePos*/, false/*includeMassiveDam*/,
                                            attackingWeaponsParryPenalty, rangedAttack, chargeAttack, grappleAttack,
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
         }
         else {
            for (Hand hand : getArms()) {
               byte handPenalty = hand.getWoundPenalty();
               String handDefName = hand.getDefenseName(false, this);
               if (handDefName != null) {
                  if (handPenalty > 0) {
                     if (availActions.contains(hand.getDefOption())) {
                        if (woundReported) {
                           sb.append(" and reduces");
                        }
                        else {
                           sb.append("\nWounds reduce");
                           woundReported = true;
                        }
                        sb.append(" your ").append(handDefName).append(" level by an additional ").append(handPenalty);
                     }
                  }
                  else if (handPenalty < 0) {
                     sb.append("\nYour ").append(hand.getName());
                     sb.append(" is crippled, so you may not ").append(handDefName).append(".");
                  }
               }
            }
            byte retreatPenalty = _condition.getPenaltyRetreat(false/*includeWounds*/);
            if (retreatPenalty > 0) {
               if (availActions.contains(DefenseOption.DEF_RETREAT)) {
                  if (woundPenalty == 0) {
                     sb.append("\nWounds reduce");
                  }
                  else {
                     sb.append(" and reduces");
                  }
                  sb.append(" your retreat level by an additional ").append(retreatPenalty);
               }
            }
            sb.append(".");
         }

         if (cantRetreatOrParryFromAttackType != null) {
            if (!isBerserking()) {
               sb.append("\nYou may not retreat or parry an attack from a ");
               sb.append(cantRetreatOrParryFromAttackType);
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
               }
               else {
                  sb.append(" so your active defenses are unaffected by range");
               }
               byte tnRangeAdj = Rules.getRangeDefenseAdjustmentToPD(range);
               if (tnRangeAdj != 0) {
                  sb.append(" and your PD is increased by ").append(tnRangeAdj);
               }
               else {
                  sb.append(".");
               }
            }
         }
         else {
            if (!isBerserking()) {
               // paralyze at 2 or higher prevents dodge defenses.
               // paralyze at 3 or higher prevents block & parry defenses.
               if ((paralyzeSpell != null) && !paralyzeSpell.allowsBlockParry()) {
                  sb.append(" Because you are fully paralyzed, you may not actively defend any attack.");
               }
               else {
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
                  }
                  else {
                     if (retreatBlocked) {
                        sb.append(" Your back is against a wall (or obstacle), blocking your retreat.");
                     }
                     else if (retreatHeld) {
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
               if (hand.canDefend(this, req.isRangedAttack(), req.isChargeAttack(), req.isGrapple(), req.getDamageType(), true/*checkState*/)) {
                  byte handPenalty = hand.getPenaltyForMassiveDamage(this, req.getMinimumDamage(), req.isRangedAttack(), req.isChargeAttack(), req.isGrapple(),
                                                                     req.getDamageType(), true/*checkState*/);
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
               if (hand.canDefend(this, req.isRangedAttack(), req.isChargeAttack(), req.isGrapple(), req.getDamageType(), true/*checkState*/)) {
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
         if (paralyzeSpell!= null) {
            if (!paralyzeSpell.allowsRetreat()) {
               sb.append("\nBecause you are paralyzed, you may not retreat");
               if (!paralyzeSpell.allowsDodge()) {
                  if (!paralyzeSpell.allowsBlockParry()) {
                     sb.append(" dodge, parry or block");
                  }
                  else {
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
            }
            else if ((priestPoints < actionsAvailableThisRound) && (magePoints < actionsAvailableThisRound)) {
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
         System.out.println("listOfListOfDefOptions = " + listOfListOfDefOptions.toString());

         HashMap<Byte, TreeSet<DefenseOptions>> mapActionsToDefActions = new HashMap<>();
         addOptionForDefenseOptions(mapActionsToDefActions, listOfListOfDefOptions, actionsAvailableThisRound, new DefenseOptions());
         // passive defense option has already been added
         for (byte actions = 1 ; actions <= maxActionsPerRound ; actions++) {
            // separator for this new columns:
            addDefenseOption(req, null, null, attackingWeaponsParryPenalty, range, attack);

            TreeSet<DefenseOptions> defActions = mapActionsToDefActions.get(actions);
            if (defActions != null) {
               Iterator<DefenseOptions> iter = defActions.iterator();
               while (iter.hasNext()) {
                  DefenseOptions defAction = iter.next();
                  addDefenseOption(req, defAction, availActions, attackingWeaponsParryPenalty, range, attack);
               }
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
      for (int i=0 ; i<defOptions.size() ; i++) {
         DefenseOption defOpt = defOptions.get(i);
         DefenseOptions defOpts = defs.clone();
         defOpts.add(defOpt);
         // check for illegal combinations such as Dodge/Retreat, or counter-attack/Retreat
         if (!defOpts.isDefensesValid()) {
            continue;
         }
         byte actionsUsed = defOpts.getDefenseActionsUsed();
         if (actionsUsed <= maxActionsUseAllowed) {
            TreeSet<DefenseOptions> defActionsPerAction = mapActionsToDefActions.get(actionsUsed);
            if (defActionsPerAction == null) {
               defActionsPerAction = new TreeSet<>();
               mapActionsToDefActions.put(actionsUsed, defActionsPerAction);
            }
            defActionsPerAction.add(defOpts);
            addOptionForDefenseOptions(mapActionsToDefActions, listOfListOfDefOptionsCopy, maxActionsUseAllowed, defOpts);
         }
      }
   }

   @Override
   public RequestGrapplingHoldMaintain getGrapplingHoldMaintain(Character escapingCharacter, RequestAction escape, Arena arena) {
      if (escapingCharacter != _holdTarget) {
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
      StringBuilder sb = new StringBuilder();
      sb.append(getName()).append(", ").append(escapingCharacter.getName());
      sb.append(" is trying to break free of your level-").append(escapingCharacter.getHoldLevel(this));
      sb.append(" hold, using ").append(escape.getActionsUsed()).append(" actions.\n");
      sb.append(" How many actions do you want to use to maintain your hold on ").append(escapingCharacter.getHimHer()).append("?");
      maintReq.setMessage(sb.toString());

      maintReq.addMaintainHoldOptions(maxActions, this, escapingCharacter, skill, getWoundsAndPainPenalty());
      return maintReq;
   }

   public RequestPosition getRequestPosition(RequestAction parentReq) {
      RequestPosition req = null;
      if (stillFighting()) {
         req = new RequestPosition(parentReq);
         StringBuilder sb = new StringBuilder();
         sb.append(getName()).append(", you have ").append(_condition.getActionsAvailable(false/*usedForDefenseOnly*/)).append(" actions remaining");
         if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) == _condition.getActionsAvailable(false/*usedForDefenseOnly*/)) {
            sb.append(".");
         }
         else {
            sb.append(" for this turn, ").append(getActionsAvailableThisRound(false/*usedForDefenseOnly*/)).append(" can be spent this round.");
         }
         sb.append(" \nYou are currently ").append(getPositionName()).append(".");
         sb.append(" \nWhat position would you like to move to this round?");
         req.setMessage(sb.toString());
         req.addPositions(_condition.getAvailablePositions(), _condition.getPosition());
      }
      return req;
   }

   public RequestAttackStyle getRequestAttackStyle(RequestAction parentAction, Arena arena) {
      RequestAttackStyle req = null;
      boolean has4legs = getLegCount() > 3;
      Character target = arena.getCharacter(parentAction._targetID);
      Weapon weap = _limbs.get(parentAction.getLimb()).getWeapon(this);
      ArenaCoordinates weapCoord = getOrientation().getLimbCoordinates(parentAction.getLimb());
      if (weap != null) {
         if (stillFighting() && (target != null)) {
            boolean isGrapple = parentAction.isGrappleAttack();
            boolean isCharge = parentAction.isCharge();
            boolean isCounterAttack = parentAction.isCounterAttack();

            req = new RequestAttackStyle(_uniqueID, target._uniqueID, parentAction.getLimb());
            StringBuilder sb = new StringBuilder();
            if (isGrapple) {
               sb.append(getName()).append(", how do you want to grab ").append(target.getName()).append("?");
            }
            else if (isCounterAttack) {
               sb.append(getName()).append(", how do you want to counter attack ").append(target.getName()).append("?");
            }
            else {
               sb.append(getName()).append(", your opponent, ").append(target.getName());
               sb.append(" is wearing ").append(target.getArmor().getName());
               sb.append(" (");
               for (DamageType damType : new DamageType[] {DamageType.BLUNT, DamageType.CUT, DamageType.IMP}) {
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
            WeaponStyleAttack[] styles = isCounterAttack ? weap._counterattackStyles :
                                                           isGrapple ? weap._grapplingStyles : weap._attackStyles;

            boolean canUseTwohanded = false;
            // if no shield, always try to use weapon in a 2-handed style
            Limb otherLimb = _limbs.get(parentAction.getLimb().getPairedType());
            if ((otherLimb != null) && (otherLimb._limbType != parentAction.getLimb())) {
               if ((otherLimb instanceof Hand) && ((Hand) otherLimb).isEmpty() && (!((Hand) otherLimb).isCrippled())) {
                  for (int i = 0; i < styles.length; i++) {
                     // If the target is out of range, don't bother checking for handedness
                     short minRange = styles[i].getMinRange();
                     short maxRange = styles[i].getMaxRange();
                     boolean inRange = (maxDistanceToTarget >= minRange) && ((minDistanceToTarget - advanceRange) <= maxRange);
                     if (inRange) {
                        if (isGrapple) {
                           canUseTwohanded = true;
                        }
                        else if (weap.isTwoHanded(i)) {
                           canUseTwohanded = true;
                        }
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
            int availableCount = 0;
            int availableIndex = -1;
            int defaultIndex = -1;
            byte bestExpectedDamage = -127;
            for (int i = 0; i < styles.length; i++) {
               short minRange = styles[i].getMinRange();
               short maxRange = styles[i].getMaxRange();

               if (isCharge) {
                   // If this is a charge, or an advance-and-attack,
                   // then the attacker has already moved up next to the target
               }
               else {
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
                  //                  if (canUseTwohanded && (styles[i].getHandsRequired() == 1)) {
                  //                     handsOK = false;
                  //                  }
                  if (!canUseTwohanded && (styles[i].getHandsRequired() == 2)) {
                     handsOK = false;
                  }
               }
               SkillType skillType = styles[i].getSkillType();
               byte skillLevel = getSkillLevel(skillType, null/*handUse*/, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, true/*adjustForHolds*/);
               if (styles[i].getMinSkill() <= skillLevel) {
                  if (!isCharge || styles[i].canCharge(isMounted(), has4legs)) {
                     sb.setLength(0);
                     sb.append(" (");
                     sb.append(styles[i].getName());
                     sb.append(": ");
                     if (!isGrapple && !isCounterAttack) {
                        sb.append(styles[i].getDamageString(getPhysicalDamageBase())).append(", ");
                     }
                     sb.append("skill '").append(skillType);
                     sb.append("' level = ").append(skillLevel - styles[i].getSkillPenalty());
                     byte styleSpeed = -1;
                     if (!singleSpeed) {
                        styleSpeed = styles[i].getSpeed(strength);
                        sb.append(", speed = ").append(styleSpeed);
                     }
                     sb.append(")");

                     byte attackActions = (byte) Math.min(parentAction.getAttackActions(false/*considerSpellAsAttack*/), styles[i].getMaxAttackActions());
                     int aimActions = getAimDuration(target._uniqueID);
                     if (aimActions > 0) {
                        attackActions += (byte) (Math.min(aimActions - 1, styles[i].getMaxAimBonus()));
                     }
                     boolean styleAllowed = true;
                     if (styles[i].isRanged()) {
                        if ((getAimDuration(target._uniqueID) <= 0) || parentAction.isAdvance()) {
                           styleAllowed = false;
                        }
                     }
                     if (!inRange || !handsOK) {
                        styleAllowed = false;
                     }
                     if (styleAllowed) {
                        availableCount++;
                        availableIndex = i;
                        if (singleSpeed || (lowestSpeed == styleSpeed)) {
                           byte expectedDamageForStyle = styles[i].getDamage(getAdjustedStrength());
                           expectedDamageForStyle -= target.getBuild(styles[i].getDamageType());
                           expectedDamageForStyle += (skillLevel / 2);
                           if ((defaultIndex == -1) || (expectedDamageForStyle > bestExpectedDamage)) {
                              defaultIndex = i;
                              bestExpectedDamage = expectedDamageForStyle;
                           }
                        }
                     }
                     DiceSet attackDice = Rules.getDice(getAttributeLevel(Attribute.Dexterity), attackActions, Attribute.Dexterity/*attribute*/);
                     req.addAttackOption(i, sb.toString(), styleAllowed, styles[i].getAttackType(), attackDice, styles[i].getDamageType());
                  }
               }
            }
            if (defaultIndex != -1) {
               req.setDefaultOption(defaultIndex);
            }
            if (availableCount == 1) {
               // if there is only one way to attack, don't ask
               req.setAnswerID(availableIndex);
            }
            else if (availableCount == 0) {
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
                                 byte attackingWeaponsParryPenalty, RANGE range, RequestAction attack) {
      if (defOpts == null) {
         req.addSeparatorOption();
         return;
      }
      // check for illegal combinations
      if (!defOpts.isDefensesValid()) {
         DebugBreak.debugBreak();
         return;
      }
      IInstantaneousSpell spellDefenseUsed = null;
      //      if ( attack.isCompleteSpell()) {
      //         spellDefenseUsed = _bestDefensiveSpell_spell;
      //      }
      if (attack.isRanged()) {
         spellDefenseUsed = _bestDefensiveSpell_ranged;
      }
      else {
         spellDefenseUsed = _bestDefensiveSpell_melee;
      }
      if (spellDefenseUsed == null) {
         if (defOpts.contains(DefenseOption.DEF_MAGIC_1) ||
             defOpts.contains(DefenseOption.DEF_MAGIC_2) ||
             defOpts.contains(DefenseOption.DEF_MAGIC_3) ||
             defOpts.contains(DefenseOption.DEF_MAGIC_4) ||
             defOpts.contains(DefenseOption.DEF_MAGIC_5)   ) {
            return;
         }
      }
      int actionsUsed = defOpts.getDefenseActionsUsed();
      boolean enabled = defOpts.logicAndWithSet(availableOptions).equals(defOpts);
      if (enabled) {
         if ((actionsUsed > getActionsAvailableThisRound(true/*usedForDefenseOnly*/)) && (actionsUsed > 0)) {
            enabled = false;
         }
         else {
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
                                 req.isChargeAttack(), req.isGrapple(), req.getDamageType(), false/*defenseAppliedAlready*/,
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

   public IInstantaneousSpell _bestDefensiveSpell_melee  = null;
   public IInstantaneousSpell _bestDefensiveSpell_ranged = null;
   public IInstantaneousSpell _bestDefensiveSpell_spell  = null;
   private AI_Type            _AItype                    = null;
   public RequestAction _lastAction;

   public String getDefenseName(DefenseOption defOpt, boolean pastTense, RequestAction attack) {
      return defOpt.getName(pastTense, this, attack);
   }

   public byte getDefenseTN(RequestDefense defense, boolean includeWoundPenalty, boolean includeHolds, boolean includePosition,
                            boolean includeMassiveDamagePenalty, byte attackingWeaponsParryPenalty, boolean defenseAppliedAlready, RANGE range) {
      return getDefenseOptionTN(new DefenseOptions(defense.getDefenseIndex()), defense.getMinimumDamage(), includeWoundPenalty, includeHolds, includePosition,
                          includeMassiveDamagePenalty, attackingWeaponsParryPenalty, defense.isRangedAttack(), defense.isChargeAttack(),
                          defense.isGrapple(), defense.getDamageType(), defenseAppliedAlready, range);
   }

   public byte getPassiveDefense(RANGE range, boolean isGrappleAttack) {
      return getDefenseOptionsBase(DamageType.GENERAL, isGrappleAttack, false/*includeWoundPenalty*/,
                                   false/*includePosition*/, true/*computePdOnly*/).get(range).get(DefenseOption.DEF_PD);
   }

   public byte getDefenseOptionTN(DefenseOptions defenseOptions, byte minimumDamage, boolean includeWoundPenalty, boolean includeHolds, boolean includePosition,
                            boolean includeMassiveDamagePenalty, byte attackingWeaponsParryPenalty, boolean isRangedAttack, boolean isChargeAttack,
                            boolean isGrappleAttack, DamageType damageType, boolean defenseAppliedAlready, RANGE range) {
      HashMap<RANGE, HashMap<DefenseOption, Byte>> baseDefs = getDefenseOptionsBase(DamageType.GENERAL, isGrappleAttack, false/*includeWoundPenalty*/, includePosition, false/*computePdOnly*/);
      byte basePD = baseDefs.get(range).get(DefenseOption.DEF_PD);
      byte baseTN = getBaseDefenseOptionTN(baseDefs, defenseOptions, range, isGrappleAttack, damageType, includeWoundPenalty, includePosition, includeHolds);
      byte defenseTN = baseTN;
      for (Limb limb : _limbs.values()) {
         // Is this hand used in the defense?
         DefenseOption defIndex = limb.getDefOption();
         if (defenseOptions.contains(defIndex)) {
            byte maxTnThisHand = limb.getDefenseTNWithoutWounds(this, isRangedAttack, isChargeAttack, isGrappleAttack, damageType, !defenseAppliedAlready);
            byte penalty = 0;
            if ((limb instanceof Hand) && ((Hand) limb).isDefenseParry(this)) {
               penalty += attackingWeaponsParryPenalty;
            }
            // This now occurs inside getBaseDefenseTN(...)
            //               if (includePain) {
            //                  if (_limbs.get(limbType).isCrippled()) penalty = maxTnThisHand;
            //                  else penalty += _limbs.get(limbType).getWoundPenalty();
            //               }
            if (includeMassiveDamagePenalty) {
               byte massiveDamagePenalty = limb.getPenaltyForMassiveDamage(this, minimumDamage, isRangedAttack, isChargeAttack, isGrappleAttack, damageType,
                                                                           !defenseAppliedAlready);
               penalty += massiveDamagePenalty;
            }
            defenseTN -= Math.min(maxTnThisHand, penalty);
         }
      }
      // This now occurs inside getBaseDefenseTN(...)
      //      if (includePain) {
      //         if ((defenseIndex & DEF_RETREAT) != 0) {
      //            defenseTN -= _condition.getPenaltyRetreat(false);
      //         }
      //         defenseTN -= _condition.getWounds();
      //      }

      // you can never defend worse than your Passive Defense.
      if (defenseTN < basePD) {
         return basePD;
      }
      return defenseTN;
   }

   public HashMap<RANGE, HashMap<DefenseOption, Byte>> getDefenseOptionsBase(DamageType damType, boolean isGrappleAttack, boolean includeWoundPenalty, boolean includePosition, boolean computePdOnly) {
      byte attributeNim = getAttributeLevel(Attribute.Nimbleness);
      byte dodge = Rules.getDodgeLevel(attributeNim);
      byte retreat = Rules.getRetreatLevel(attributeNim);
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

      for (LimbType armType : LimbType._armTypes) {
         Hand hand = (Hand) getLimb(armType);
         if (hand != null) {
            Thing heldThing = hand.getHeldThing();
            Limb pairedHand = getLimb(hand._limbType.getPairedType());
            Thing pairedHeldThing = (pairedHand == null) ? null : ((Hand) pairedHand).getHeldThing();
            boolean canUse2Hands = ((pairedHeldThing == null) || (pairedHeldThing == heldThing));
            if (heldThing == null) {
               heldThing = hand.getWeapon(this);
            }
            else {
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
                        def = heldThing.getBestDefenseOption(this, armType, canUse2Hands, damType, isGrappleAttack);
                        byte rangeAdjustment = Rules.getRangeDefenseAdjustmentPerAction(range);
                        def += rangeAdjustment;
                        if (includeWoundPenalty) {
                           byte limbUsePenalty = hand.getWoundPenalty();
                           if (limbUsePenalty < 0) {
                              def = 0;
                           }
                           else {
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
      for (Spell spell : _activeSpellsList) {
         rangedPD += spell.getPassiveDefenseModifier(true, DamageType.GENERAL);
         meleePD += spell.getPassiveDefenseModifier(false, DamageType.GENERAL);
      }

      if (!computePdOnly) {
         ArrayList<Spell> spells = getSpells();

         for (Spell spell : spells) {
            if (spell instanceof IInstantaneousSpell) {
               IInstantaneousSpell instantSpell = (IInstantaneousSpell) spell;
               if (instantSpell.canDefendAgainstMeleeAttacks()) {
                  if ((_bestDefensiveSpell_melee == null) || (_bestDefensiveSpell_melee.getLevel() < instantSpell.getLevel())) {
                     _bestDefensiveSpell_melee = instantSpell;
                  }
               }
               if (instantSpell.canDefendAgainstRangedAttacks()) {
                  if ((_bestDefensiveSpell_ranged == null) || (_bestDefensiveSpell_ranged.getLevel() < instantSpell.getLevel())) {
                     _bestDefensiveSpell_ranged = instantSpell;
                  }
               }
               if (instantSpell.canDefendAgainstSpells()) {
                  if ((_bestDefensiveSpell_spell == null) || (_bestDefensiveSpell_spell.getLevel() < instantSpell.getLevel())) {
                     _bestDefensiveSpell_spell = instantSpell;
                  }
               }
            }
         }
         if (_bestDefensiveSpell_melee != null) {
            defBase.get(RANGE.OUT_OF_RANGE).put(DefenseOption.DEF_MAGIC_1, _bestDefensiveSpell_melee.getActiveDefensiveTN((byte)1, this));
            defBase.get(RANGE.OUT_OF_RANGE).put(DefenseOption.DEF_MAGIC_2, _bestDefensiveSpell_melee.getActiveDefensiveTN((byte)2, this));
            defBase.get(RANGE.OUT_OF_RANGE).put(DefenseOption.DEF_MAGIC_3, _bestDefensiveSpell_melee.getActiveDefensiveTN((byte)3, this));
            defBase.get(RANGE.OUT_OF_RANGE).put(DefenseOption.DEF_MAGIC_4, _bestDefensiveSpell_melee.getActiveDefensiveTN((byte)4, this));
            defBase.get(RANGE.OUT_OF_RANGE).put(DefenseOption.DEF_MAGIC_5, _bestDefensiveSpell_melee.getActiveDefensiveTN((byte)5, this));
         }
         if (_bestDefensiveSpell_ranged != null) {
            for (RANGE range : RANGE.values()) {
               if (range != RANGE.OUT_OF_RANGE) {
                  defBase.get(range).put(DefenseOption.DEF_MAGIC_1, _bestDefensiveSpell_ranged.getActiveDefensiveTN((byte)1, this));
                  defBase.get(range).put(DefenseOption.DEF_MAGIC_2, _bestDefensiveSpell_ranged.getActiveDefensiveTN((byte)2, this));
                  defBase.get(range).put(DefenseOption.DEF_MAGIC_3, _bestDefensiveSpell_ranged.getActiveDefensiveTN((byte)3, this));
                  defBase.get(range).put(DefenseOption.DEF_MAGIC_4, _bestDefensiveSpell_ranged.getActiveDefensiveTN((byte)4, this));
                  defBase.get(range).put(DefenseOption.DEF_MAGIC_5, _bestDefensiveSpell_ranged.getActiveDefensiveTN((byte)5, this));
               }
            }
         }
      }
      for (RANGE range : RANGE.values()) {
         byte rangeAdjustmentToPD = Rules.getRangeDefenseAdjustmentToPD(range);
         byte rangeAdjustmentPerAction = Rules.getRangeDefenseAdjustmentPerAction(range);
         if (!computePdOnly) {
            if (includeWoundPenalty) {
               if (_condition.getPenaltyMove() < 0) {
                  dodge = 0;
                  retreat = 0;
               }
               else {
                  dodge = (byte) Math.max(0, dodge - _condition.getPenaltyMove());
                  byte retreatPenalty = _condition.getPenaltyRetreat(includeWoundPenalty);
                  if (retreatPenalty < 0) {
                     retreat = 0;
                  }
                  else {
                     retreat = (byte) Math.max(0, retreat - retreatPenalty);
                  }
               }
            }
            if (dodge != 0) {
               dodge += rangeAdjustmentPerAction;
            }
            if (retreat != 0) {
               retreat += rangeAdjustmentPerAction * 2;
            }
            if (dodge < 0) {
               dodge = 0;
            }
            if (retreat < 0) {
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

   public ArrayList<Spell> getSpells() {
      ArrayList<Spell> spells = new ArrayList<>();
      spells.addAll(_knownMageSpellsList);
      spells.addAll(getPriestSpells());
      return spells;
   }

   public ArrayList<PriestSpell> getPriestSpells() {
      ArrayList<PriestSpell> spells = new ArrayList<>();
      ArrayList<String> deities = getPriestDeities();
      for (String deity : deities) {
         int deityAffinity = getAffinity(deity);
         if (deityAffinity > 0) {
            List<String> spellGroups = PriestSpell.getSpellGroups(deity);
            for (String group : spellGroups) {
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
                           if (spell.getAffinity() > ((PriestSpell)alreadyAddedSpell).getAffinity()) {
                              // don't add the higher-affinity version
                              lowerPowerSpellAlreadyExists = true;
                           }
                           else {
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

   public ArrayList<String> getPriestDeities() {
      ArrayList<String> deities = new ArrayList<>();
      for (String deity : PriestSpell._deities) {
         if (hasAdvantage(Advantage.DIVINE_AFFINITY_ + deity)) {
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
         writeToStream(_uniqueID, out);
         writeToStream(_teamID, out);
         writeToStream(_name, out);
         writeToStream(((_race != null) ? _race.getName() : ""), out);
         writeToStream(((_race != null) ? _race.getGender()._name : ""), out);
         for (Attribute att : Attribute.values()) {
            writeToStream(_attributes.get(att), out);
         }
         ArrayList<Limb> limbs = new ArrayList<>();
         limbs.addAll(_limbs.values());
         writeToStream(limbs, out);

         synchronized (_equipment) {
            try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
               writeToStream(_equipment, out);
            }
         }
         writeToStream(((_armor != null) ? _armor.getName() : ""), out);
         writeToStream(getSkillsList(), out);
         writeToStream(_knownMageSpellsList, out);
         writeToStream(_knownCollegesList, out);
         writeToStream(_advList, out);
         writeToStream(_aimDuration, out);
         writeToStream(_targetID, out);
         _condition.serializeToStream(out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   static public int readIntoListThing(List<Thing> data, DataInputStream in) throws IOException {
      data.clear();
      ArrayList<SerializableObject> things = readIntoListSerializableObject(in);
      for (SerializableObject thing : things) {
         if (thing instanceof Thing) {
            data.add((Thing)thing);
         }
      }
      return data.size();
   }
   static public int readIntoListSkill(List<Skill> data, DataInputStream in) throws IOException {
      data.clear();
      ArrayList<SerializableObject> skills = readIntoListSerializableObject(in);
      for (SerializableObject skill : skills) {
         if (skill instanceof Skill) {
            data.add((Skill)skill);
         }
      }
      return data.size();
   }
   static public int readIntoListSpell(List<MageSpell> data, DataInputStream in) throws IOException {
      data.clear();
      ArrayList<SerializableObject> spells = readIntoListSerializableObject(in);
      for (SerializableObject spell : spells) {
         if (spell instanceof MageSpell) {
            data.add((MageSpell)spell);
         }
      }
      return data.size();
   }
  static public int readIntoListColleges(List<MageCollege> data, DataInputStream in) throws IOException {
     data.clear();
     ArrayList<SerializableObject> colleges = readIntoListSerializableObject(in);
     for (SerializableObject college : colleges) {
        if (college instanceof MageCollege) {
           data.add((MageCollege)college);
        }
     }
     return data.size();
  }
  static public int readIntoListAdvantage(List<Advantage> data, DataInputStream in) throws IOException {
      data.clear();
      ArrayList<SerializableObject> advs = readIntoListSerializableObject(in);
      for (SerializableObject adv : advs) {
         if (adv instanceof Advantage) {
            data.add((Advantage)adv);
         }
      }
      return data.size();
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         _uniqueID = readInt(in);
         _teamID = readByte(in);
         _name = readString(in);
         String race = readString(in);
         String genderStr = readString(in);
         Gender gender = Gender.getByName(genderStr);
         setRace(race, gender);
         for (Attribute att : Attribute.values()) {
            _attributes.put(att, readByte(in));
         }

         _limbs.clear();
         ArrayList<SerializableObject> things = readIntoListSerializableObject(in);
         for (SerializableObject thing : things) {
            if (thing instanceof Limb) {
               Limb newLimb = (Limb) thing;
               if (newLimb.getRacialBase() == null) {
                  DebugBreak.debugBreak();
               }
               _limbs.put(newLimb._limbType, newLimb);
            }
         }
         synchronized (_equipment) {
            try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
               readIntoListThing(_equipment, in);
            }
         }
         _armor = Armor.getArmor(readString(in), getRace());
         List<Skill> skillsList = new ArrayList<>();
         readIntoListSkill(skillsList, in);
         setSkillsList(skillsList);
         readIntoListSpell(_knownMageSpellsList, in);
         readIntoListColleges(_knownCollegesList, in);
         readIntoListAdvantage(_advList, in);
         _aimDuration = readByte(in);
         _targetID = readInt(in);
         _condition.serializeFromStream(in);
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
         attArray[att.value] = _attributes.get(att);
      }
      Skill[] skillsArray = new Skill[_skillsList.size()];
      skillsArray = _skillsList.values().toArray(skillsArray);

      MageSpell[] spellsArray = new MageSpell[_knownMageSpellsList.size()];
      spellsArray = _knownMageSpellsList.toArray(spellsArray);

      MageCollege[] collegesArray = new MageCollege[_knownCollegesList.size()];
      collegesArray = _knownCollegesList.toArray(collegesArray);

      Advantage[] advArray = new Advantage[_advList.size()];
      advArray = _advList.toArray(advArray);

      Character newChar = new Character(_name, (_race == null) ? null : _race.getName(), (_race == null) ? Gender.MALE : _race.getGender(), attArray,
                                        (_armor == null) ? null : _armor.getName(), _limbs, getEquipment(), skillsArray, spellsArray, collegesArray, advArray);
      newChar.copyData(this);
      return newChar;
   }

   public void copyData(Character source) {
      if (this == source) {
         return;
      }
      _name = source.getName();
      setRace(source.getRace().getName(), source.getGender());
      for (Attribute att : Attribute.values()) {
         _attributes.put(att, source._attributes.get(att));
      }
      _limbs.clear();
      for (LimbType limbType : source._limbs.keySet()) {
         _limbs.put(limbType, source._limbs.get(limbType).clone());
      }

      synchronized (_equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
            _equipment.clear();
            _equipment.addAll(source._equipment);
         }
      }
      _armor = Armor.getArmor(source.getArmor().getName(), getRace());
      _skillsList.clear();
      for (Skill sourceSkill : source._skillsList.values()) {
         Skill skill = new Skill(sourceSkill.getType(), sourceSkill.getLevel());
         _skillsList.put(skill.getType(), skill);
      }
      _knownMageSpellsList = new ArrayList<>();
      for (MageSpell sourceSpell : source._knownMageSpellsList) {
         MageSpell spell = MageSpell.getSpell(sourceSpell.getName());
         spell.setLevel(sourceSpell.getLevel());
         _knownMageSpellsList.add(spell);
      }
      _knownCollegesList = new ArrayList<>();
      for (MageCollege sourceCollege : source._knownCollegesList) {
         MageCollege college = MageCollege.getCollege(sourceCollege.getName());
         college.setLevel(sourceCollege.getLevel());
         _knownCollegesList.add(college);
      }
      _advList = new ArrayList<>();
      for (Advantage sourceAdvantage : source._advList) {
         Advantage adv = Advantage.getAdvantage(sourceAdvantage.toString());
         _advList.add(adv);
      }

      // active values:
      _condition = source._condition.clone();
      _uniqueID = source._uniqueID;
      _teamID = source._teamID;
      _targetID = source._targetID;
      _aimDuration = source._aimDuration;
      _headCount = source._headCount;
      _eyeCount = source._eyeCount;
      _legCount = source._legCount;
      _wingCount = source._wingCount;
      if (source._currentSpell != null) {
         if (source._currentSpell instanceof MageSpell) {
            for (Spell spell : _knownMageSpellsList) {
               if (spell.getClass() == source._currentSpell.getClass()) {
                  _currentSpell = spell.clone();
                  _currentSpell.setCaster(this);
                  break;
               }
            }
         }
         else {
            _currentSpell = source._currentSpell.clone();
            _currentSpell.setCaster(this);
         }
      }
      _isBerserking = source._isBerserking;
      _hasInitiativeAndActionsEverBeenInitialized = source._hasInitiativeAndActionsEverBeenInitialized;
      // computed values:
      resetSpellPoints();
      updateWeapons();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Character: ");
      sb.append(", uniqueID: ").append(_uniqueID);
      sb.append(", name: ").append(_name);
      sb.append(", points: ").append(getPointTotal());
      sb.append(", race: ").append((_race == null) ? null : _race.getName());
      sb.append(", gender: ").append((_race == null) ? null : _race.getGender());
      for (Attribute att : Attribute.values()) {
         sb.append(", ").append(att.shortName).append(":").append(_attributes.get(att));
         if (_race.getBuildModifier() != 0) {
            if (att == Attribute.Strength) {
               sb.append(" (").append(getAdjustedStrength()).append(")");
            }
            else if (att == Attribute.Health) {
               sb.append(" (").append(getBuildBase()).append(")");
            }
         }
      }
      sb.append(", armor: ").append((_armor == null) ? null : _armor.getName());
      for (Limb limb : _limbs.values()) {
         sb.append(", ").append(limb._limbType.name).append(":").append(limb.getHeldThingName());
      }
      synchronized (_equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
            sb.append(", equipment: ").append(_equipment);
         }
      }
      sb.append(", $").append(getTotalCost()).append(" spent");
      sb.append(", skills: ").append(_skillsList.values().toString());
      sb.append(", spells: ").append(_knownMageSpellsList.toString());
      sb.append(", colleges: ").append(_knownCollegesList.toString());
      sb.append(", advantages: ").append(_advList.toString());
      sb.append(", teamID: ").append(_teamID);
      sb.append(", aimDuration: ").append(_aimDuration);
      sb.append(", condition: ").append(_condition.toString());
      return sb.toString();
   }

   public String print() {
      StringBuilder sb = new StringBuilder();
      sb.append(_name).append(": (").append(getPointTotal()).append(" points)");
      if (_race != null) {
         sb.append(_race.getGender()).append(" ").append(_race.getName());
      }
      for (Attribute att : Attribute.values()) {
         sb.append(", ").append(att.shortName).append(":").append(_attributes.get(att));
         if (_race.getBuildModifier() != 0) {
            if (att == Attribute.Strength) {
               sb.append(" (").append(getAdjustedStrength()).append(")");
            }
            else if (att == Attribute.Health) {
               sb.append(" (").append(getBuildBase()).append(")");
            }
         }
      }
      sb.append("<br>armor: ").append((_armor == null) ? null : _armor.getName());
      for (Limb limb : _limbs.values()) {
         String heldThingName = limb.getHeldThingName();
         if ((heldThingName != null) && (heldThingName.length() > 0)) {
            sb.append("<br>held in ").append(limb.getName()).append(":").append(heldThingName);
         }
      }
      synchronized (_equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
            if (!_equipment.isEmpty()) {
               sb.append("<br>equipment on belt: ");
               for (Thing thing : _equipment) {
                  sb.append(thing.getName()).append(", ");
               }
            }
         }
      }
      sb.append("<br>encumbrance level: ").append(Rules.getEncumbranceLevel(this));
      if (!_skillsList.isEmpty()) {
         sb.append("<br>skills: ").append(_skillsList.values().toString());
      }
      if (!_knownMageSpellsList.isEmpty()) {
         sb.append("<br>spells: ").append(_knownMageSpellsList.toString());
      }
      if (!_knownCollegesList.isEmpty()) {
         sb.append("<br>colleges: ").append(_knownCollegesList.toString());
      }
      if (!_advList.isEmpty()) {
         sb.append("<br>advantages: ").append(_advList.toString());
      }
      return sb.toString();
   }

   public ArrayList<Integer> getOrderedTargetPriorites() {
      return _orderedTargetIds;
   }

   public void setTargetPriorities(ArrayList<Integer> orderedTargetIds) {
      _orderedTargetIds.clear();
      _orderedTargetIds.addAll(orderedTargetIds);
      if ((_orderedTargetIds != null) && (_orderedTargetIds.size() > 0)) {
         _targetID = _orderedTargetIds.get(0).intValue();
      }
   }
   public void setTarget(int targetsUniqueID) {
      _targetID = targetsUniqueID;
      // re-order the list, moving the new target to the top of the list.
      if (_orderedTargetIds.remove(Integer.valueOf(targetsUniqueID))) {
         _orderedTargetIds.add(0, targetsUniqueID);
      }
   }

   public String serializeToString() {
      StringBuilder sb = new StringBuilder();
      sb.append(_name).append(SEPARATOR_MAIN);
      sb.append(_race.getName()).append(SEPARATOR_MAIN);
      sb.append(_race.getGender()._name).append(SEPARATOR_MAIN);
      for (Attribute att : Attribute.values()) {
         sb.append(String.valueOf(_attributes.get(att))).append(SEPARATOR_SECONDARY);
      }
      sb.append(SEPARATOR_MAIN);
      sb.append((_armor == null) ? " " : _armor.getName()).append(SEPARATOR_MAIN);
      for (Hand hand : getArms()) {
         sb.append(hand.getHeldThingName()).append(SEPARATOR_SECONDARY);
      }
      sb.append(SEPARATOR_MAIN);
      synchronized (_equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
            if (_equipment.size() == 0) {
               sb.append(SEPARATOR_SECONDARY);
            }
            else {
               for (Thing thing : _equipment) {
                  if (thing != null) {
                     sb.append(thing.getName()).append(SEPARATOR_SECONDARY);
                  }
               }
            }
         }
      }
      sb.append(SEPARATOR_MAIN);
      if ((_skillsList.size() + _knownMageSpellsList.size() + _knownCollegesList.size()) == 0) {
         sb.append(SEPARATOR_SECONDARY);
      }
      else {
         for (Skill skill : _skillsList.values()) {
            sb.append(skill.getName()).append('=').append(skill.getLevel()).append(SEPARATOR_SECONDARY);
         }
         for (MageSpell spell : _knownMageSpellsList) {
            sb.append(spell.getName()).append('=').append(spell.getLevel()).append(SEPARATOR_SECONDARY);
         }
         for (MageCollege college : _knownCollegesList) {
            sb.append(college.getName()).append('=').append(college.getLevel()).append(SEPARATOR_SECONDARY);
         }
      }
      sb.append(SEPARATOR_MAIN);
      if (_advList.size() == 0) {
         sb.append(SEPARATOR_SECONDARY);
      }
      else {
         for (int i = 0; i < _advList.size(); i++) {
            Advantage adv = _advList.get(i);
            sb.append(adv.getName()).append(SEPARATOR_SECONDARY);
            sb.append(adv.getLevel()).append(SEPARATOR_SECONDARY);
         }
      }
      sb.append(SEPARATOR_MAIN);

      return sb.toString();
   }

   public boolean serializeFromString(String source) {
      String attributes = "";
      String handsString = "";
      StringTokenizer st = new StringTokenizer(source, SEPARATOR_MAIN);
      if (!st.hasMoreElements()) {
         return false;
      }
      _name = st.nextToken();
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
      _armor = Armor.getArmor(st.nextToken(), getRace());
      if (!st.hasMoreElements()) {
         return false;
      }
      handsString = st.nextToken();
      if (!st.hasMoreElements()) {
         return false;
      }
      String equipment = st.nextToken();
      // If no skills or advantages are listed, then there will be no more elements, but thats OK.
      String skills = (st.hasMoreElements() ? st.nextToken() : "");
      String advantages = (st.hasMoreElements() ? st.nextToken() : "");

      // we should be done reading
      if (st.hasMoreElements()) {
         return false;
      }
      _condition = new Condition(this);

      // parse the attributes
      st = new StringTokenizer(attributes, SEPARATOR_SECONDARY);
      for (Attribute att : Attribute.values()) {
         if (st.hasMoreElements()) {
            _attributes.put(att, Byte.parseByte(st.nextToken()));
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
      synchronized (_equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
            _equipment.clear();
            st = new StringTokenizer(equipment, SEPARATOR_SECONDARY);
            while (st.hasMoreElements()) {
               String equipName = (String) st.nextElement();
               _equipment.add(Thing.getThing(equipName, true/*allowTool*/, getRace()));
            }
         }
      }

      // parse the skills, spells & colleges
      _skillsList.clear();
      st = new StringTokenizer(skills, SEPARATOR_SECONDARY);
      while (st.hasMoreElements()) {
         String skillAsStr = (String) st.nextElement();
         int loc = skillAsStr.indexOf('=');
         if (loc != -1) {
            String name = skillAsStr.substring(0, loc);
            String level = skillAsStr.substring(loc + 1);
            SkillType skillType = SkillType.getSkillTypeByName(name);
            if (skillType != null) {
               _skillsList.put(skillType, new Skill(skillType, Byte.parseByte(level)));
            }
            else {
               MageSpell spell = MageSpell.getSpell(name);
               if (spell != null) {
                  spell.setLevel(Byte.parseByte(level));
                  _knownMageSpellsList.add(spell);
               }
               else {
                  MageCollege college = MageCollege.getCollege(name);
                  if (college != null) {
                     college.setLevel(Byte.parseByte(level));
                     _knownCollegesList.add(college);
                  }
               }
            }
         }
      }
      // parse the advantages
      _advList = new ArrayList<>();
      st = new StringTokenizer(advantages, SEPARATOR_SECONDARY);
      while (st.hasMoreElements()) {
         String advName = (String) st.nextElement();
         if (st.hasMoreElements()) {
            String levelStr = (String) st.nextElement();
            Advantage adv = Advantage.getAdvantage(advName);
            if (adv != null) {
               adv.setLevel(Byte.parseByte(levelStr));
               _advList.add(adv);
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
      mainElement.setAttribute("Gender", getGender()._name);
      if (includeConditionData) {
         mainElement.setAttribute("UniqueID", String.valueOf(_uniqueID));
      }

      Element attrElement = parentDoc.createElement("Attributes");
      for (Attribute att : Attribute.values()) {
         attrElement.setAttribute(att.shortName, String.valueOf(_attributes.get(att)));
      }
      mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      mainElement.appendChild(attrElement);

      Element equipmentElement = parentDoc.createElement("Equipment");
      equipmentElement.setAttribute("Armor", (_armor == null) ? "" : _armor.getName());
      mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      mainElement.appendChild(equipmentElement);

      for (Hand hand : getArms()) {
         Element handElement = parentDoc.createElement("Hand");
         handElement.setAttribute("Name", hand.getName());
         handElement.setTextContent(hand.getHeldThingName());
         equipmentElement.appendChild(parentDoc.createTextNode(newLine + "    "));
         equipmentElement.appendChild(handElement);
      }
      synchronized (_equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
            for (Thing beltThing : _equipment) {
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

      if (_skillsList.size() > 0) {
         Element skillsElement = parentDoc.createElement("Skills");
         for (Skill skill : _skillsList.values()) {
            Element skillElement = parentDoc.createElement("Skill");
            skillElement.setAttribute("Name", skill.getName());
            skillElement.setAttribute("Level", String.valueOf(skill.getLevel()));
            skillsElement.appendChild(parentDoc.createTextNode(newLine + "    "));
            skillsElement.appendChild(skillElement);
         }
         skillsElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(skillsElement);
      }
      if (_knownMageSpellsList.size() > 0) {
         Element spellsElement = parentDoc.createElement("Spells");
         for (MageSpell spell : _knownMageSpellsList) {
            Element spellElement = parentDoc.createElement("Spell");
            spellElement.setAttribute("Name", spell.getName());
            spellElement.setAttribute("Level", String.valueOf(spell.getLevel()));
            spellsElement.appendChild(parentDoc.createTextNode(newLine + "    "));
            spellsElement.appendChild(spellElement);
         }
         spellsElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(spellsElement);
      }
      if (_knownCollegesList.size() > 0) {
         Element collegesElement = parentDoc.createElement("Colleges");
         for (MageCollege college : _knownCollegesList) {
            Element collegeElement = parentDoc.createElement("College");
            collegeElement.setAttribute("Name", college.getName());
            collegeElement.setAttribute("Level", String.valueOf(college.getLevel()));
            collegesElement.appendChild(parentDoc.createTextNode(newLine + "    "));
            collegesElement.appendChild(collegeElement);
         }
         collegesElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(collegesElement);
      }
      if (_advList.size() > 0) {
         Element advantagesElement = parentDoc.createElement("Advantages");
         for (int i = 0; i < _advList.size(); i++) {
            Advantage adv = _advList.get(i);
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
         mainElement.appendChild(_condition.getXMLObject(parentDoc, newLine + "  "));
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));

         Element limbElement = parentDoc.createElement("limbs");
         for (Limb limb : _limbs.values()) {
            // We only care about limbs that our race says we should have
            if (_race.createLimb(limb._limbType) != null) {
               limbElement.appendChild(parentDoc.createTextNode(newLine + "    "));
               limbElement.appendChild(limb.getXMLObject(parentDoc, newLine + "  "));
            }
         }
         for (LimbType type : _race.getLimbSet()) {
            if (!_limbs.containsKey(type)) {
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
         activeElement.setAttribute("teamID", String.valueOf(_teamID));
         activeElement.setAttribute("targetID", String.valueOf(_targetID));
         activeElement.setAttribute("aimDuration", String.valueOf(_aimDuration));
         activeElement.setAttribute("headCount", String.valueOf(_headCount));
         activeElement.setAttribute("eyeCount", String.valueOf(_eyeCount));
         activeElement.setAttribute("legCount", String.valueOf(_legCount));
         activeElement.setAttribute("wingCount", String.valueOf(_wingCount));
         activeElement.setAttribute("isBerserking", String.valueOf(_isBerserking));
         activeElement.setAttribute("hasInitiativeAndActionsEverBeenInitialized", String.valueOf(_hasInitiativeAndActionsEverBeenInitialized));
         activeElement.setAttribute("aiType", ((_AItype == null) ? "" : _AItype.name));
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(activeElement);

         Element currentSpell = parentDoc.createElement("currentSpell");
         if (_currentSpell != null) {
            Node spellXml = _currentSpell.getXMLObject(parentDoc, newLine);
            currentSpell.appendChild(parentDoc.createTextNode(newLine + "    "));
            currentSpell.appendChild(spellXml);
            currentSpell.appendChild(parentDoc.createTextNode(newLine + "  "));
         }
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(currentSpell);

         Element activeSpells = parentDoc.createElement("activeSpells");
         for (Spell activeSpell : _activeSpellsList) {
            Node spellXml = activeSpell.getXMLObject(parentDoc, newLine);
            activeSpells.appendChild(parentDoc.createTextNode(newLine + "    "));
            activeSpells.appendChild(spellXml);
         }
         if (_activeSpellsList.size() > 0) {
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
      _condition = new Condition(this);
      _skillsList.clear();
      _knownCollegesList = new ArrayList<>();
      _knownMageSpellsList = new ArrayList<>();
      _advList = new ArrayList<>();

      NamedNodeMap namedNodeMap = mainElement.getAttributes();
      if (namedNodeMap == null) {
         return false;
      }
      _name = namedNodeMap.getNamedItem("Name").getNodeValue();
      Gender gender = Gender.MALE;
      Node genderNode = namedNodeMap.getNamedItem("Gender");
      if (genderNode != null) {
         gender = Gender.getByName(genderNode.getNodeValue());
      }
      setRace(namedNodeMap.getNamedItem("Race").getNodeValue(), gender);
      Node uniqueID = namedNodeMap.getNamedItem("UniqueID");
      if (uniqueID != null) {
         _uniqueID = Integer.parseInt(uniqueID.getNodeValue());
      }

      NodeList children = mainElement.getChildNodes();
      for (int index = 0; index < children.getLength(); index++) {
         Node child = children.item(index);
         NamedNodeMap attributes = child.getAttributes();
         if (child.getNodeName().equals("Attributes")) {
            for (Attribute att : Attribute.values()) {
               Node value = attributes.getNamedItem(att.shortName);
               if (value == null) {
                  _attributes.put(att, _race.getAttributeMods(att));
               }
               else {
                  _attributes.put(att, Byte.valueOf(value.getNodeValue()));
               }
            }
         }
         else if (child.getNodeName().equals("Equipment")) {
            _armor = Armor.getArmor(attributes.getNamedItem("Armor").getNodeValue(), getRace());
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
         }
         else if ((child.getNodeName().equals("Skills")) || (child.getNodeName().equals("Spells")) || (child.getNodeName().equals("Colleges"))
                  || (child.getNodeName().equals("Advantages"))) {
            // These elements all have children below, that have a similar structure: <??? level="?" Name="?"/>
            NodeList grandChildren = child.getChildNodes();
            for (int i = 0; i < grandChildren.getLength(); i++) {
               Node grandChild = grandChildren.item(i);
               NamedNodeMap attr = grandChild.getAttributes();
               if (attr == null) {
                  continue;
               }
               String name = attr.getNamedItem("Name").getNodeValue();
               Node node = attr.getNamedItem("Level");
               String level = ((node != null) ? node.getNodeValue() : null);
               node = attr.getNamedItem("LevelName");
               String levelName = ((node != null) ? node.getNodeValue() : null);

               if (child.getNodeName().equals("Skills")) {
                  SkillType skillType = SkillType.getSkillTypeByName(name);
                  if (skillType != null) {
                     _skillsList.put(skillType, new Skill(skillType, Byte.parseByte(level)));
                  }
               }
               else if (child.getNodeName().equals("Spells")) {
                  MageSpell spell = MageSpell.getSpell(name);
                  if (spell != null) {
                     spell.setLevel(Byte.parseByte(level));
                     _knownMageSpellsList.add(spell);
                  }
               }
               else if (child.getNodeName().equals("Colleges")) {
                  MageCollege college = MageCollege.getCollege(name);
                  if (college != null) {
                     college.setLevel(Byte.parseByte(level));
                     _knownCollegesList.add(college);
                  }
               }
               else if (child.getNodeName().equals("Advantages")) {
                  Advantage adv = Advantage.getAdvantage(name);
                  if (adv != null) {
                     if (levelName != null) {
                        adv.setLevelByName(levelName);
                     }
                     else {
                        if (adv.getName().equals(Advantage.CODE_OF_CONDUCT)) {
                           // code of conduct had 4 elements added to the front of the list of levels
                           // at the same time as the 'levelName' attribute was added.
                           // so if we don't have a 'levelName', then the level's have changed:
                           adv.setLevel((byte) (Byte.parseByte(level) + 4));
                        }
                        else {
                           adv.setLevel(Byte.parseByte(level));
                        }
                     }
                     _advList.add(adv);
                  }
               }
            }
         }
         else if (child.getNodeName().equals("Condition")) {
            _condition.serializeFromXmlObject(child);
            // If we have a severed limb, set it's limb to null.
            for (Wound wound : getWoundsList()) {
               if (wound.isSeveredArm() || wound.isSeveredLeg() || wound.isSeveredWing()) {
                  _limbs.remove(wound.getLimb());
               }
            }
         }
         else if (child.getNodeName().equals("activeData")) {
            NamedNodeMap attr = child.getAttributes();
            if (attr != null) {
               _teamID = Byte.parseByte(attr.getNamedItem("teamID").getNodeValue());
               _targetID = Byte.parseByte(attr.getNamedItem("targetID").getNodeValue());
               _aimDuration = Byte.parseByte(attr.getNamedItem("aimDuration").getNodeValue());
               _headCount = Integer.parseInt(attr.getNamedItem("headCount").getNodeValue());
               _eyeCount = Integer.parseInt(attr.getNamedItem("eyeCount").getNodeValue());
               _legCount = Integer.parseInt(attr.getNamedItem("legCount").getNodeValue());
               _wingCount = Integer.parseInt(attr.getNamedItem("wingCount").getNodeValue());
               _isBerserking = Boolean.parseBoolean(attr.getNamedItem("isBerserking").getNodeValue());
               Node node = attr.getNamedItem("hasInitiativeAndActionsEverBeenInitialized");
               if (node != null) {
                  _hasInitiativeAndActionsEverBeenInitialized = Boolean.parseBoolean(node.getNodeValue());
               }
               Node isAiNode = attr.getNamedItem("isAIPlayer");
               if (isAiNode != null) {
                  if (Boolean.parseBoolean(isAiNode.getNodeValue())) {
                     _AItype = AI_Type.NORM;
                  }
               }
               Node AiTypeNode = attr.getNamedItem("aiType");
               if (AiTypeNode != null) {
                  _AItype = AI_Type.getByString(AiTypeNode.getNodeValue());
               }
            }
         }
         else if (child.getNodeName().equals("limbs")) {
            NodeList grandChildren = child.getChildNodes();
            for (int i = 0; i < grandChildren.getLength(); i++) {
               Node grandChild = grandChildren.item(i);
               NamedNodeMap attr = grandChild.getAttributes();
               if (attr != null) {
                  if (grandChild.getNodeName().equals("Limb")) {
                     LimbType limbType = LimbType.getByValue(Byte.parseByte(attr.getNamedItem("id").getNodeValue()));
                     if (attr.getNamedItem("severed") != null) {
                        _limbs.remove(limbType);
                     }
                     else {
                        Limb limb = getLimb(limbType);
                        if (limb == null) {
                           limb = _race.createLimb(limbType);
                           _limbs.put(limbType, limb);
                        }
                        limb.serializeFromXmlObject(grandChild);
                     }
                  }
               }
            }
         }
         else if (child.getNodeName().equals("activeSpells")) {
            _activeSpellsList.clear();
            NodeList grandChildren = child.getChildNodes();
            for (int i = 0; i < grandChildren.getLength(); i++) {
               Node grandChild = grandChildren.item(i);
               Spell spell = Spell.serializeFromXmlObject(grandChild);
               if (spell != null) {
                  _activeSpellsList.add(spell);
               }
            }
         }
         else if (child.getNodeName().equals("currentSpell")) {
            NodeList grandChildren = child.getChildNodes();
            for (int i = 0; i < grandChildren.getLength(); i++) {
               Node grandChild = grandChildren.item(i);
               Spell spell = Spell.serializeFromXmlObject(grandChild);
               if (spell != null) {
                  _currentSpell = spell;
                  _currentSpell.setCaster(this);
               }
            }
         }
      }
      updateWeapons();
      return true;
   }

   public void setCasterAndTargetFromIDs(List<Character> combatants) {
      for (Spell activeSpell : _activeSpellsList) {
         activeSpell.setCasterAndTargetFromIDs(combatants);
      }
      if (_currentSpell != null) {
         _currentSpell.setCasterAndTargetFromIDs(combatants);
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
      } catch (IOException |ClassNotFoundException |InstantiationException |IllegalAccessException |ClassCastException e) {
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
         Document doc = factory.newDocumentBuilder().parse(sourceFile);
         return doc;
      } catch (SAXException e) {
         // A parsing error occurred; the xml input is not valid
      } catch (ParserConfigurationException e) {
      } catch (IOException e) {
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

   public ArrayList<Wound> getWoundsList() {
      return _condition.getWoundsList();
   }

   public void applyWound(Wound wound, Arena arena) {
      Character origChar = clone(); // used to notify watchers if something changes, like a severed limb, etc

      wound.setInvalidReason(placeWound(wound));
      Wound modifiedWound = modifyWoundFromDefense(wound);
      if (modifiedWound.getLevel() >= 0) {
         // getting wounded always makes you lose aim
         _aimDuration = 0;
      }
      ArenaLocation loc = null;
      if (arena != null) {
         if (modifiedWound.getLocation() == Location.WEAPON) {
            for (Hand hand : getArms()) {
               Weapon thing = hand.getWeapon(this);
               if (thing != null) {
                  loc = _condition.getLimbLocation(hand._limbType, arena.getCombatMap());
               }
            }
         }
         else {
            LimbType limbType = modifiedWound.getLimb();
            if (limbType == null) {
               DebugBreak.debugBreak("Null location");
               limbType = modifiedWound.getLimb();
            }
            loc = _condition.getLimbLocation(limbType, arena.getCombatMap());
         }
      }
      if (modifiedWound.isSeveredArm() || modifiedWound.isSeveredLeg() || modifiedWound.isSeveredWing()) {
         loc.addThing(_race.createSeveredLimb(modifiedWound.getLimb()));
      }

      if (modifiedWound.isSeveredLeg()) {
         _legCount--;
      }
      if (modifiedWound.isBlinding()) {
         _eyeCount--;
      }
      if (modifiedWound.isDecapitating()) {
         _headCount--;
      }
      if (modifiedWound.isSeveredWing()) {
         _wingCount--;
      }

      // If the wound severed an arm, drop anything carried by that arm, even if its not a Weapon (maybe a shield)
      if (modifiedWound.getLocation() == Wound.Location.ARM) {
         if (modifiedWound.isDropWeapon() || modifiedWound.isSeveredArm()) {
            Wound.Pair armPair = modifiedWound.getLocationPair();

            boolean twoHandedWeapon = false;
            Limb rightArm = _limbs.get(LimbType.get(Location.ARM, Side.RIGHT, armPair));
            Limb leftArm = _limbs.get(LimbType.get(Location.ARM, Side.LEFT, armPair));
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
            }
            else if ((modifiedWound.getLocationSide() == Wound.Side.LEFT) || twoHandedWeapon) {
               thingDropped = (leftArm == null) ? null : leftArm.dropThing();
            }
            else if ((modifiedWound.getLocationSide() != Wound.Side.RIGHT) && (modifiedWound.getLocationSide() != Wound.Side.LEFT)) {
               // A wound to the scapula can also cause us to drop our weapon
               // but the location is not a HAND, so check for this case.
               for (Limb limb : _limbs.values()) {
                  if (limb.getHeldThing() instanceof Weapon) {
                     thingDropped = limb.dropThing();
                     break;
                  }
               }
            }
            if (thingDropped != null) {
               if (thingDropped.isReal()) {
                  loc.addThing(thingDropped);
               }
            }
         }
      }
      else if (modifiedWound.isDropWeapon()) {
         // This wound causes the target to drop its weapon, but is not targeted on an arm.
         // Drop the first item we find on any Hand.
         for (Limb limb : getLimbs()) {
            if (limb instanceof Hand) {
               Thing weap = limb.getHeldThing();
               if ((weap != null) && (weap.isReal())) {
                  Thing thingDropped = limb.dropThing();
                  loc.addThing(thingDropped);
                  break;
               }
            }
         }
      }

      if (modifiedWound.isUnreadyWeapon()) {
         for (Limb limb : _limbs.values()) {
            Weapon weap = limb.getWeapon(this);
            if ((weap != null) && (weap.isReal())) {
               limb.setActionsNeededToReady((byte) (limb.getActionsNeededToReady() + 1));
            }
         }
      }
      for (Limb limb : _limbs.values()) {
         limb.applyWound(modifiedWound);
      }
      _condition.applyWound(modifiedWound, arena, this);

      byte newPain = _condition.getPenaltyPain();
      StringBuilder sb = new StringBuilder();
      if ((newPain > 0) && (modifiedWound.getPain() > 0))  {
         if (isBerserker() && !isBerserking()) {
            byte iq = getAttributeLevel(Attribute.Intelligence);
            DiceSet berserkSaveDice = Rules.getDice(iq, (byte) 2/*action*/, Attribute.Intelligence);
            berserkSaveDice = adjustDieRoll(berserkSaveDice, RollType.BERSERK_RESISTANCE, null/*target*/);
            int diceRoll = berserkSaveDice.roll(true/*allowExplodes*/);
            sb.append(getName()).append("'s pain causes a chance that he goes berserk.");
            sb.append(" He rolls 2-actions IQ (").append(berserkSaveDice.toString());
            sb.append("), rolling ").append(berserkSaveDice.getLastDieRoll());
            sb.append(" = ").append(diceRoll);
            if (berserkSaveDice.lastRollRolledAllOnes()) {
               sb.append(", which is all ones, so ").append(getName()).append(" automattically goes berserk!");
               _isBerserking = true;
            }
            else if (diceRoll >= newPain) {
               sb.append(", which is equal to, or above, the pain level of ").append(newPain);
               sb.append(", so ").append(getName()).append(" does not go berserk.");
            }
            else {
               sb.append(", which is below the pain level of ").append(newPain);
               sb.append(", so ").append(getName()).append(" goes berserk!");
               _isBerserking = true;
            }

         }
         // TODO: implement that damage caused to a subject of a pacify spell will allow them to re-resist the spell.
         //         for (Spell spell : _activeSpellsList) {
         //            if (spell instanceof SpellPacify) {
         //               IResistedSpell pacifySpell = (SpellPacify) spell;
         //               if (pacifySpell.resistAgain()) {
         //                  _activeSpellsList.remove(spell);
         //               }
         //            }
         //         }
      }
      if (_currentSpell != null) {
         byte currentPain = _condition.getPenaltyPain();
         boolean spellLost = false;
         if (!_condition.isConscious() || _condition.isCollapsed()) {
            sb = new StringBuilder();
            sb.append(getName()).append("'s '").append(_currentSpell.getName());
            sb.append("' spell, is lost!");
            spellLost = true;
         }
         if ((currentPain > 0) && (modifiedWound.getPain() > 0)) {
            byte toughness = getAttributeLevel(Attribute.Toughness);
            DiceSet magicSaveDice = Rules.getDice(toughness, (byte) 2/*action*/, Attribute.Toughness);
            magicSaveDice = adjustDieRoll(magicSaveDice, RollType.PAIN_CONCENTRATION, null/*target*/);
            int diceRoll = magicSaveDice.roll(true/*allowExplodes*/);

            sb.append("<br/>");
            sb.append(getName()).append("'s wound raises ").append(getHisHer()).append(" pain level to ").append(currentPain);
            sb.append("<br/>Since he has a '").append(_currentSpell.getName());
            sb.append("' spell, he must roll 2-actions against ").append(getHisHer()).append(" TOU of ").append(toughness);
            sb.append(" (").append(magicSaveDice.toString()).append("), to avoid losing the spell. He rolls ");
            sb.append(magicSaveDice.getLastDieRoll()).append(" for a total of ").append(diceRoll).append(".<br/>");
            if (magicSaveDice.lastRollRolledAllOnes()) {
               sb.append(" The roll is all 1s, which always fails, so the spell is lost!");
               spellLost = true;
            }
            else if (currentPain > diceRoll) {
               sb.append(" The roll fails, so the spell is lost!");
               spellLost = true;
            }
            else {
               sb.append(" The roll succeeds, so the spell is not lost.");
            }
         }
         if (spellLost && (sb != null)) {
            Wound newWound = _currentSpell.getFailureBurnWound();
            _currentSpell = null;
            if (newWound != null) {
               sb.append("<br/>Since the spell was over-powered, ").append(getName());
               sb.append(" takes an additional ").append(newWound.getWounds()).append(" wounds and ");
               sb.append(newWound.getPain()).append(" points of pain.");
               _condition.applyWound(newWound, arena, this);
            }
         }
      }
      if (arena != null) {
         arena.sendMessageTextToAllClients(sb.toString(), false/*popUp*/);
      }

      // notify any watchers of any changes to this character
      ObjectChanged changeNotif = new ObjectChanged(origChar, this);
      notifyWatchers(origChar, this, changeNotif, null/*skipList*/, null/*diag*/);
   }

   public void setCondition(Condition newCondition) {
      _condition = newCondition;
   }

   public void applyAction(RequestAction action, Arena arena) throws BattleTerminatedException {
      Character originalCopy = clone();
      if (action.isEquipUnequip()) {
         RequestEquipment reqEqu = action._equipmentRequest;
         if (reqEqu == null) {
            DebugBreak.debugBreak();
            reqEqu = (RequestEquipment) action.getNextQuestion(this, arena.getCombatants(), arena);
         }
         if (reqEqu.isDrop()) {
            Limb limb = _limbs.get(action.getLimb());
            if (limb != null) {
               Thing thingDropped = limb.dropThing();
               if ((thingDropped != null) && (thingDropped.isReal())) {
                  ArenaLocation loc = _condition.getLimbLocation(limb._limbType, arena.getCombatMap());
                  loc.addThing(thingDropped);
               }
            }
         }
         else if (reqEqu.isSheath()) {
            Limb limb = _limbs.get(action.getLimb());
            if (limb != null) {
               Thing thingSheathed = limb.dropThing();
               addEquipment(thingSheathed);
            }
         }
         else if (reqEqu.isReady() || reqEqu.isApply()) {
            synchronized (_equipment) {
               try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
                  if (reqEqu.isReady()) {
                     String equName = reqEqu.getEquToReady();
                     for (Thing equ : _equipment) {
                        if (equ.getName().equals(equName)) {
                           Limb hand = getLimb(reqEqu.getLimb());
                           if (hand.setHeldThing(equ, this)) {
                              _equipment.remove(equ);
                           }
                           break;
                        }
                     }
                  }
                  else if (reqEqu.isApply()) {
                     String equName = reqEqu.getEquToApply();
                     for (Thing equ : _equipment) {
                        if (equ.getName().equals(equName)) {
                           if (equ.apply(this, arena)) {
                              _equipment.remove(equ);
                           }
                           break;
                        }
                     }
                  }
               }
            }
         }
      }
      else if (action.isChangeTargets()) {
         if ((action == null) || ((action._targetPriorities == null) && (action._targetSelection == null))) {
            DebugBreak.debugBreak();
         }
         else {
            if (action._targetSelection != null) {
               setTarget(action._targetSelection.getAnswerID());
            }
            else if (action._targetPriorities== null) {
               ArrayList<Integer> orderedTargetIds = action._targetPriorities.getOrderedTargetIds();
               setTargetPriorities(orderedTargetIds);
            }
         }
      }

      // spells
      if (action.isBeginSpell()) {
         int spellIndex = action._spellTypeSelectionRequest._spellSelectionRequest.getAnswerID();
         if (RequestSpellTypeSelection.SPELL_TYPE_MAGE.equals(action._spellTypeSelectionRequest.getAnswer())) {
            _currentSpell = _knownMageSpellsList.get(spellIndex).clone();
         }
         else {
            String deity = action._spellTypeSelectionRequest.getAnswer();
            List<PriestSpell> spells = PriestSpell.getSpellsForDeity(deity, getAffinity(deity), true/*addNullBetweenGroups*/);
            _currentSpell = spells.get(spellIndex).clone();
         }
         _currentSpell.setCaster(this);
         _currentSpell.beginIncantation();
         if (_currentSpell.getIncantationRoundsRequired() > 0) {
            _currentSpell.incant();
         }
         else {
            if (!(_currentSpell instanceof InstantaneousMageSpell)) {
               // If the spell level is so high that it costs no actions
               // to make the incantation, then since we are spending an action anyway,
               // add a point of energy to the spell.
               _currentSpell.channelEnergy((byte) 1);
            }
            _currentSpell.maintainSpell();
         }
      }
      else if (action.isPrepareInateSpell()) {
         IRequestOption[] actionOptions = action.getReqOptions();
         IRequestOption actionOption = actionOptions[action.getAnswerIndex()];
         if (actionOption instanceof RequestActionOption)
         {
            RequestActionOption reqActOpt = (RequestActionOption) actionOption;
            int spellIndex = reqActOpt.getValue().getIndexOfPrepareInateSpell();
            _currentSpell = _race.getInateSpells().get(spellIndex).clone();
            _currentSpell.maintainSpell();
            _currentSpell.setCaster(this);
         }
      }
      else if (action.isContinueSpell()) {
         _currentSpell.incant();
      }
      else if (action.isChannelEnergy()) {
         Wound burn = _currentSpell.channelEnergy(action.getActionsUsed());
         if (burn != null) {
            _condition.applyWound(burn, arena, this);
            String message = getName() + "'s over-powers a spell causing " + burn.getPain() + " points of pain.";
            arena.sendMessageTextToAllClients(message, false/*popUp*/);
         }
      }
      else if (action.isMaintainSpell()) {
         _currentSpell.maintainSpell();
      }
      else if (action.isCompleteSpell()) {
         if (_currentSpell instanceof PriestSpell) {
            PriestSpell priestSpell = (PriestSpell) _currentSpell;
            IRequestOption answer = action.answer();
            if (answer instanceof RequestActionOption) {
               RequestActionOption reqActOpt = (RequestActionOption) answer;
               priestSpell.setPower((byte) (reqActOpt.getValue().getIndexOfCompletePriestSpell() + 1));
            }
         }
         _currentSpell.completeSpell();
      }
      else if (action.isDiscardSpell()) {
         _currentSpell.discardSpell();
         _currentSpell = null;
      }

      if (action.isTargetEnemy()) {
         if (_targetID != action._targetID) {
            _targetID = action._targetID;
            _aimDuration = 0;
         }
         _aimDuration++;
      }
      else {
         // If we aren't actively targeting the enemy
         // this round, then we lose our aim duration,
         // however, don't clear this on the round that
         // we attack the enemy.
         if (!action.isAttack()) {
            _aimDuration = 0;
         }
      }
      if (action.isAttack()) {
         if (_targetID != action._targetID) {
            _targetID = action._targetID;
            _aimDuration = 0;
         }
      }
      for (Limb limb : _limbs.values()) {
         limb.applyAction(action, getAttributeLevel(Attribute.Strength), this, arena);
      }
      _condition.applyAction(action, this, _currentSpell, arena);
      _lastAction = action;
      //if (!originalCopy.equals(this)) {
         ObjectChanged changeNotif = new ObjectChanged(originalCopy, this);
         notifyWatchers(originalCopy, this, changeNotif, null/*skipList*/, null/*diag*/);
      //}
   }

   public byte getAffinity(String deity) {
      Advantage adv = getAdvantage(Advantage.DIVINE_AFFINITY_ + deity);
      if (adv != null) {
         return (byte) (adv.getLevel() + 1);
      }
      return 0;
   }

   @Override
   public void applyHoldMaintenance(RequestGrapplingHoldMaintain holdMaintainenance, Arena arena) {
      _condition.applyHoldMaintenance(holdMaintainenance);
   }

   public String applyDefense(RequestDefense defense, Arena arena) {
      if (defense.getAnswerID() == -1) {
         DebugBreak.debugBreak("Applying a defense with actionID == -1");
         // Since we don't know what to do, do nothing!
         return null;
      }
      if (defense.getActionsUsed() > 0) {
         // taking an active defense makes us lose aim
         _aimDuration = 0;
      }
      for (Limb limb : _limbs.values()) {
         limb.applyDefense(defense, getAttributeLevel(Attribute.Strength), this);
      }
      if (defense.isRetreat()) {
         if (_holdTarget != null) {
            arena.sendMessageTextToAllClients("Because " + getName() + " retreated, " + getHeShe() + " releases " + getHisHer() + "hold of "
                                              + _holdTarget.getName(), false/*popup*/);
            releaseHold();
         }
      }
      _condition.applyDefense(defense);
      return null;
   }

   public void resetSpellPoints() {
      if (_condition == null) {
         return;
      }
      ArrayList<String> deities = getPriestDeities();
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
      _condition.resetSpellPoints(ma, affinity, divinePower);
   }

   public byte getAvailableMovement(boolean movingEvasively) {
      return _condition.getMovementAvailableThisRound(movingEvasively);
   }

   public boolean hasMovedThisRound() {
      return _condition.hasMovedThisRound();
   }

   public void setMovingEvasively(boolean movingEvasively) {
      _condition.setMovingEvasively(movingEvasively);
   }

   public void applyMovementCost(byte movementCost) {
      _condition.applyMovementCost(movementCost);
   }

   public void setClientProxy(ClientProxy proxy, CombatMap map, Diagnostics diag) {
      _clientProxy = proxy;
      // If we have a client proxy, then it is a watcher of the mapWatcher
      _mapWatcher = new ArenaLocationBook(this, map, diag);
      _mapWatcher.registerAsWatcher(_clientProxy, diag);
      // and also as a watcher of the Character itself.
      registerAsWatcher(_clientProxy, diag);
   }

   public AI_Type getAIType() {
      return _AItype;
   }
   public boolean isAIPlayer() {
      return (_AItype != null);
   }

   public void setAIType(AI_Type AItype) {
      _AItype = AItype;
   }

   public boolean setHeadLocation(ArenaLocation headLocation, Facing facing, CombatMap map, Diagnostics diag) {
      if (_condition.setHeadLocation(this, headLocation, facing, map, diag)) {
         map.updateCombatant(this, true/*checkTriggers*/);
         return true;
      }
      return false;
   }

   //   public void setLocations(ArrayList<ArenaLocation> newLocations, byte newFacing, CombatMap map, Diagnostics diag)
   //   {
   //      Orientation originalOrientation = (Orientation) _condition.getOrientation().clone();
   //      if (_condition.setLocations(newLocations, newFacing, map, diag)) {
   //         Character origChar = (Character) clone();
   //         origChar._condition.setOrientation(originalOrientation);
   //         ObjectChanged changeNotif = new ObjectChanged(origChar, this);
   //         notifyWatchers(origChar, this, changeNotif, null/*skipList*/, diag);
   //      }
   //   }

   public void setOrientation(Orientation destination, Diagnostics diag) {
      if (destination.equals(_condition.getOrientation())) {
         return;
      }
      Character origChar = clone();
      _condition.setOrientation(destination);
      ObjectChanged changeNotif = new ObjectChanged(origChar, this);
      notifyWatchers(origChar, this, changeNotif, null/*skipList*/, diag);
   }

   public boolean isMoveComplete() {
      return _condition.isMoveComplete();
   }

   public void setMoveComplete() {
      _condition.setMoveComplete();
   }

   public boolean canAdvance() {
      return _condition.canAdvance();
   }

   public double getWeightArmor() {
      if (_armor != null) {
         return _armor.getAdjustedWeight();
      }
      return 0;
   }

   public double getWeightEquipment() {
      double weight = 0;
      for (Limb limb : _limbs.values()) {
         Thing thing = limb.getHeldThing();
         if (thing != null) {
            weight += thing.getAdjustedWeight();
         }
      }
      synchronized (_equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
            for (Thing thing : _equipment) {
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
      if (_armor != null) {
         cost += _armor.getCost();
      }

      for (Limb limb : _limbs.values()) {
         Thing thing = limb.getHeldThing();
         if (thing != null) {
            cost += thing.getCost();
         }
      }
      synchronized (_equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
            for (Thing thing : _equipment) {
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
         ArrayList<Thing> equipment = getEquipment();
         if (equipment.size() > 0) {
            ArrayList<Thing> doneEqu = new ArrayList<>();
            for (Thing thing : equipment) {
               // If the character has multiple items of the same type, don't
               // re-ask the same questions for each one.
               if (doneEqu.contains(thing)) {
                  continue;
               }
               doneEqu.add(thing);

               if (thing != null) {
                  Object crippled = new Object();
                  for (Pair pair : new Pair[] {Pair.FIRST, Pair.SECOND, Pair.THIRD}) {
                     if ((_race.getArmCount() == 2) && (pair == Pair.SECOND)) {
                        break;
                     }
                     if ((_race.getArmCount() == 4) && (pair == Pair.THIRD)) {
                        break;
                     }
                     Limb leftHandOfSet  = _limbs.get(LimbType.get(Location.ARM, Side. LEFT, pair));
                     Limb rightHandOfSet = _limbs.get(LimbType.get(Location.ARM, Side. RIGHT, pair));
                     Object leftHandObj  = (leftHandOfSet == null) ? crippled : leftHandOfSet.getHeldThing();
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
                        }
                        else {
                           limb = leftHandOfSet;
                        }
                     }
                     else {
                        if (rightHandObj != null) {
                           if (leftHandObj != null) {
                              enabled = false;
                           }
                           else {
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
                     req.addReadyOption(thing.getName(), equipment.indexOf(thing), limb._limbType, enabled);
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
      byte penalty = getHandPenalties(limb._limbType, null/*SkillType*/); // TODO: get SkillType

      if (includeWounds) {
         penalty += limb.getWoundPenalty();
      }
      Weapon weap = limb.getWeapon(this);
      if (weap != null) {
         // TODO: what about bastard swords and katanas that are wielded with two hands?
         if (weap.isOnlyTwoHanded()) {
            Limb otherHand = _limbs.get(limb._limbType.getPairedType());
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
      return _limbs.get(limbType);
   }

   public ArrayList<Hand> getArms() {
      ArrayList<Hand> arms = new ArrayList<>();
      for (LimbType limbType : LimbType.values()) {
         if (limbType.isHand()) {
            Hand limb = (Hand) _limbs.get(limbType);
            if (limb != null) {
               arms.add(limb);
            }
         }
      }
      return arms;
   }

   public void updateWeapons() {
      for (Hand hand : getArms()) {
         Weapon weap = hand.getWeapon(this);
         if (weap != null) {
            // Is our left hand free to wield the primary weapon with two hands?
            Hand otherLimb = (Hand) (_limbs.get(hand._limbType.getPairedType()));
            boolean twoHandedAvailable = ((otherLimb != null) && otherLimb.isEmpty() && !otherLimb.isCrippled());

            int bestSkillLevel = -1;
            // select highest skill level for attack
            for (byte i = 0; i < weap._attackStyles.length; i++) {
               if ((weap._attackStyles[i].getHandsRequired() == 1) || twoHandedAvailable) {
                  int styleSkillLevel = getSkillLevel(weap._attackStyles[i], false/*adjustForPain*/, null/*useHand*/, false/*sizeAdjust*/,
                                                      true/*adjustForEncumbrance*/, true/*adjustForHolds*/);
                  if (bestSkillLevel < styleSkillLevel) {
                     hand.setAttackStyle(i);
                     bestSkillLevel = styleSkillLevel;
                  }
               }
            }
            bestSkillLevel = -1;
            // select highest skill level for defense
            for (byte i = 0; i < weap._parryStyles.length; i++) {
               if ((weap._parryStyles[i].getHandsRequired() == 1) || twoHandedAvailable) {
                  int styleSkillLevel = getSkillLevel(weap._parryStyles[i], false/*adjustForPain*/, null/*useHand*/, false/*sizeAdjust*/,
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
      short maxRange = 0;
      for (Limb limb : _limbs.values()) {
         Weapon weap = limb.getWeapon(this);
         if (weap != null) {
            // Is this weapon ready to attack?
            if (limb.getActionsNeededToReady() == 0) {
               short handRange = weap.getWeaponMaxRange(allowRanged, onlyChargeTypes, this);
               if (handRange > maxRange) {
                  maxRange = handRange;
               }
            }
         }
      }

      if (allowRanged) {
         if (_currentSpell != null) {
            short spellRange = _currentSpell.getMaxRange(this);
            if (spellRange > maxRange) {
               maxRange = spellRange;
            }
         }
         else {
            for (MageSpell spell : _knownMageSpellsList) {
               short spellRange = spell.getMaxRange(this);
               if (spellRange > maxRange) {
                  maxRange = spellRange;
               }
            }
         }
      }
      return maxRange;
   }

   public int getUnseveredArmCount() {
      int count = 0;
      for (Limb limb : _limbs.values()) {
         if (limb instanceof Hand) {
            if (!limb.isSevered()) {
               count++;
            }
         }
      }
      return count;
   }

   public int getUncrippledArmCount(boolean reduceCountForTwoHandedWeaponsHeld) {
      int availableHands = 0;
      for (Hand hand : getArms()) {
         if (!hand.isCrippled()) {
            Thing heldThing = hand.getHeldThing();
            if ((heldThing == null) || (!heldThing.isReal())) {
               availableHands++;
            }
            else {
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
      return _legCount;
   }

   public int getEyeCount() {
      return _eyeCount;
   }

   public int getHeadCount() {
      return _headCount;
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
               }
               else {
                  // has arms and wings (no legs)
                  wound.setLocation((CombatServer.random() > .5) ? Location.LEG : Location.ARM);
               }
            }
            else {
               if (targetHasWings) {
                  // has arms and wings (no legs)
                  wound.setLocation((CombatServer.random() > .5) ? Location.WING : Location.ARM);
               }
               else {
                  // has only arms
                  wound.setLocation(Location.ARM);
               }
            }
         }
         else {
            if (targetHasLegs) {
               // has no arms, does have legs, and maybe wings
               if (targetHasWings) {
                  // legs and wings (no arms)
                  wound.setLocation((CombatServer.random() > .5) ? Location.WING : Location.LEG);
               }
               else {
                  // only legs
                  wound.setLocation(Location.LEG);
               }
            }
            else {
               if (!targetHasWings) {
                  // has no limbs!
                  throw new WoundCantBeAppliedToTargetException("target has no arms, legs or wings");
               }
               // has no arms or legs (only wings)
               wound.setLocation(Location.WING);
            }
         }
      }
      else if ((wound.getLocation() == Location.ARM) && hasWings()) {
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
      }
      else if (wound.getLocation() == Location.LEG) {
         if (!setWoundLocationSidePair(_legCount, _race.getLegCount(), wound)) {
            return "target has no legs";
         }
      }
      else if (wound.getLocation() == Location.EYE) {
         if (!setWoundLocationSidePair(_eyeCount, _race.getEyeCount(), wound)) {
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
      }
      else {
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

   public ArrayList<String> getPropertyNames() {
      ArrayList<String> props = _race.getRacialPropertiesList();
      ArrayList<Advantage> advs = _race.getAdvantagesList();
      for (Advantage adv : advs) {
         props.add(adv.getName());
      }
      for (Advantage adv : _advList) {
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
      wound = _race.alterWound(wound, alterationExplanationBuffer);

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
         }
         else if (noPain) {
            alterationExplanationBuffer.append(" feels no pain.");
         }
         else if (noBlood) {
            alterationExplanationBuffer.append(" does not bleed.");
         }
         wound = altWound;
      }

      // Check for protection spells, or other non-race issues.
      for (Spell spell : _activeSpellsList) {
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
      if (_currentSpell == null) {
         return null;
      }
      return _currentSpell.getName();
   }

   public RequestSpellTypeSelection getSpellTypeSelectionRequest() {
      boolean mage = ((_knownMageSpellsList != null) && (_knownMageSpellsList.size() > 0));
      ArrayList<String> priestAffinities = getPriestDeities();
      return new RequestSpellTypeSelection(mage, priestAffinities, this);
   }

   public RequestSpellSelection getSpellSelectionRequest(String spellType) {
      if (RequestSpellTypeSelection.SPELL_TYPE_MAGE.equals(spellType)) {
         return new RequestSpellSelection(_knownMageSpellsList, this);
      }
      String deity = spellType;
      return new RequestSpellSelection(PriestSpell.getSpellsForDeity(deity, getAffinity(deity), true/*addNullBetweenGroups*/), this);
   }

   public void completeTurn(Arena arena) {
      StringBuilder sb = new StringBuilder();
      if ((_currentSpell != null) && !_currentSpell.isMaintainedThisTurn() && !_currentSpell.isInate()) {
         sb.append(getName()).append(" has not maintained a '").append(_currentSpell.getName());
         sb.append("' spell, and the spell has been lost.");
         _currentSpell = null;
      }
      List<Spell> expiredSpells = new ArrayList<>();
      for (Spell spell : _activeSpellsList) {
         if (spell.completeTurn(arena)) {
            expiredSpells.add(spell);
         }
      }
      for (Spell spell : expiredSpells) {
         _activeSpellsList.remove(spell);
         sb.append("<br/>");
         sb.append(getName()).append(" is no longer under the effects of the '");
         sb.append(spell.getName()).append("' spell.");
      }
      arena.sendMessageTextToAllClients(sb.toString(), false/*popUp*/);
   }

   public Spell getCurrentSpell(boolean eraseCurrentSpell) {
      Spell currentSpell = _currentSpell;
      if (eraseCurrentSpell) {
         _currentSpell = null;
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
         for (Spell activeSpell : _activeSpellsList) {
            if (activeSpell.isIncompatibleWith(spell)) {
               if (activeSpell.takesPrecedenceOver(spell)) {
                  arena.sendMessageTextToAllClients("The '" + spell.getName() + "' has no effect while " + getName() + " is under the effects of the '"
                                                    + activeSpell.getName() + "' spell.", false/*popUp*/);
                  return false;
               }
               arena.sendMessageTextToAllClients("The '" + spell.getName() + "' cancels the effects of the '" + activeSpell.getName() + "' spell.", false/*popUp*/);
               activeSpell.removeEffects(arena);
               _activeSpellsList.remove(activeSpell);
               break;
            }
         }
         _activeSpellsList.add(spell);
         return true;
      }
      return false;
   }

   public boolean removeSpellFromActiveSpellsList(Spell spell) {
      for (Spell activeSpell : _activeSpellsList) {
         if (activeSpell.equals(spell)) {
            _activeSpellsList.remove(activeSpell);
            return true;
         }
      }
      return false;
   }

   public byte getActionsAvailableThisRound(boolean usedForDefenseOnly) {
      return _condition.getActionsAvailableThisRound(usedForDefenseOnly);
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
      if (_currentSpell != null) {
         return _currentSpell.getNewTurnBurnWound();
      }
      return null;
   }

   public boolean isEnemy(Character other) {
      return (other._teamID != _teamID) || (other._teamID == TEAM_INDEPENDENT) || (_teamID == TEAM_INDEPENDENT);
   }

   public String describeActiveSpells() {
      StringBuilder sb = new StringBuilder();
      if (_activeSpellsList != null) {
         for (Spell spell : _activeSpellsList) {
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
      if ((thisHeadCoord == null) && (otherHeadCoord != null)) {
         return -1;
      }
      if ((thisHeadCoord != null) && (otherHeadCoord == null)) {
         return 1;
      }
      return thisHeadCoord.compareTo(otherHeadCoord);
   }

   public String cureWound(Wound woundToCure, byte woundReduction, byte bleedingReduction) {
      return _condition.healWound(woundToCure, woundReduction, bleedingReduction);
   }

   public String regrowLimb(Wound woundToCure) {
      //      if (woundToCure.isSeveredArm()) {
      //      }
      if (woundToCure.isSeveredLeg()) {
         _legCount++;
      }
      if (woundToCure.isBlinding()) {
         _eyeCount++;
      }
      if (woundToCure.isDecapitating()) {
         _headCount++;
      }
      if (woundToCure.isSeveredWing()) {
         _wingCount++;
      }
      return _condition.regrowLimb(woundToCure);
   }

   public Facing getFacing() {
      return _condition.getFacing();
   }

   /**
    * This method returns the size of the character, based on the racial size adjustment,
    * plus the average of STR and HT.
    * @return
    */
   public byte getSize() {
      return (byte) (_race.getBuildModifier() + ((getAttributeLevel(Attribute.Strength) + getAttributeLevel(Attribute.Health)) / 2));
   }

   private transient HashMap<String, DrawnObject> _drawnObjectsCache       = new HashMap<>();
   private transient ArrayList<String>            _recentlyDrawnObjectKeys = new ArrayList<>();

   public DrawnObject getDrawnObject(int size, RGB background, RGB foreground, ArenaLocation loc, int[] bounds, Orientation orientation) {
      // If nothing has changed since we last drew ourselves, just return the last thing we drew
      String newDrawnObjectKey = getDrawnObjectKey(bounds, background, foreground, orientation);
      // If this character is drawn over multiple hexes, avoid drawing the same one portion of the character across
      // multiple locations by adding the location to the key
      if (orientation.getCoordinates().size() > 0) {
         newDrawnObjectKey += loc.getMapKey();
      }

      DrawnObject cachedObj = _drawnObjectsCache.get(newDrawnObjectKey);
      if (CombatServer.usesCachedDrawing()) {
         if (cachedObj != null) {
            return cachedObj;
         }
      }
      if (_recentlyDrawnObjectKeys.size() > 10) {
         String oldestObject = _recentlyDrawnObjectKeys.remove(0);
         _drawnObjectsCache.remove(oldestObject);
      }

      size = (int) (Math.pow(1.03, getSize()) * size);
      Point hexCenter = new Point((bounds[0] + bounds[6]) / 2, (bounds[1] + bounds[7]) / 2);

      int[] preOffsetBounds = new int[bounds.length];
      for (int i = 0; i < preOffsetBounds.length; i += 2) {
         preOffsetBounds[i + 0] = bounds[i + 0] - hexCenter.x;
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

         _recentlyDrawnObjectKeys.add(newDrawnObjectKey);
         _drawnObjectsCache.put(newDrawnObjectKey, drawnObject);
         return drawnObject;
      }
      return null;
   }

   private String getDrawnObjectKey(int[] bounds, RGB background, RGB foreground, Orientation orientation) {
      StringBuilder sb = new StringBuilder();
      ArrayList<Limb> limbs = getLimbs();
      sb.append(limbs.size());
      for (Limb limb : limbs) {
         sb.append(limb.getHeldThingName());
      }
      sb.append(orientation.getFacing());
      sb.append(orientation.getPosition());
      ArenaCoordinates headCoord = orientation.getHeadCoordinates();
      sb.append(headCoord._x);
      sb.append(headCoord._y);
      sb.append(bounds[0]);
      sb.append(bounds[1]);
      sb.append(bounds[2]);
      sb.append(background);
      sb.append(foreground);
      return sb.toString();
   }

   public void dropAllEquipment(Arena arena) {
      ArenaCoordinates headCoord = _condition.getOrientation().getHeadCoordinates();
      ArenaLocation headLoc = arena.getLocation(headCoord);
      ArrayList<Thing> things = new ArrayList<>();
      synchronized (_equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
            while (_equipment.size() > 0) {
               Thing thing = _equipment.remove(0);
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
            ArenaLocation loc = getLimbLocation(limb._limbType, arena.getCombatMap());
            loc.addThing(thingDropped);
         }
      }
   }

   public DiceSet adjustDieRoll(DiceSet dice, RollType rollType, Object target) {
      for (Spell spell : _activeSpellsList) {
         dice = spell.adjustDieRoll(dice, rollType, target);
      }
      return dice;
   }

   public boolean canStand() {
      return _condition.canStand();
   }

   public boolean isPenalizedInWater() {
      if (_race.isAquatic()) {
         return false;
      }
      if (isUnderSpell(SpellSwim.NAME) != null) {
         return false;
      }
      return true;
   }
   public boolean isPenalizedOutOfWater() {
      return _race.isAquatic();
   }

   public boolean isAnimal() {
      return _race.isAnimal();
   }

   public boolean hasKey(String code) {
      String fullName = "key:" + code;
      for (Thing equipment : _equipment) {
         if (equipment.getName().equalsIgnoreCase(fullName)) {
            return true;
         }
      }
      for (Limb limb : getLimbs()) {
         if (limb instanceof Hand) {
            if (((Hand) limb).getHeldThingName().equalsIgnoreCase(fullName)) {
               return true;
            }
         }
      }
      return false;
   }

   public String getPositionName() {
      return _condition.getPositionName();
   }

   private byte getPositionAdjustedDefenseOption(DefenseOption defOption, byte def) {
      return _condition.getPositionAdjustedDefenseOption(defOption, def);
   }

   public byte getPositionAdjustmentForAttack() {
      return _condition.getPositionAdjustmentForAttack();
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
      _condition.awaken();
   }

   public ArrayList<Thing> getEquipment() {
      ArrayList<Thing> dupList = new ArrayList<>();
      synchronized (_equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
            for (Thing thing : _equipment) {
               dupList.add(thing);
            }
         }
      }
      return dupList;
   }

   public void clearEquipmentList() {
      synchronized (_equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
            _equipment.clear();
         }
      }
   }

   public void addEquipment(Thing newThing) {
      synchronized (_equipment) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lock_equipment)) {
            if ((newThing == null) || !newThing.isReal()) {
               DebugBreak.debugBreak();
            }
            _equipment.add(newThing);
         }
      }
   }

   public byte getPenaltyForBeingHeld() {
      byte total = 0;
      for (Byte penalty : _heldPenalties.values()) {
         total += penalty.byteValue();
      }
      return total;
   }

   public void setHoldLevel(IHolder holder, Byte holdLevel) {
      if ((holdLevel == null) || (holdLevel.byteValue() < 0)) {
         _heldPenalties.remove(holder);
         if (holder.getHoldTarget() == this) {
            holder.setHoldTarget(null);
         }
      }
      else {
         setPlacedIntoHoldThisTurn(holder);
         _heldPenalties.put(holder, holdLevel);
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
      return _holdTarget;
   }
   @Override
   public void setHoldTarget(Character holdTarget) {
      _holdTarget = holdTarget;
   }

   public Byte getHoldLevel(IHolder holder) {
      return _heldPenalties.get(holder);
   }

   public void releaseHold() {
      if (_holdTarget != null) {
         _holdTarget.setHoldLevel(this, null);
      }
   }

   public Set<IHolder> getHolders() {
      return _heldPenalties.keySet();
   }

   @Override
   public Byte getHoldingLevel() {
      if (_holdTarget != null) {
         return _holdTarget.getHoldLevel(this);
      }
      return null;
   }

   public void setPlacedIntoHoldThisTurn(IHolder holder) {
      if (!_placedIntoHoldThisTurn.contains(holder)) {
         _placedIntoHoldThisTurn.add(holder);
      }
   }
   public boolean getPlacedIntoHoldThisTurn(IHolder holder) {
      return _placedIntoHoldThisTurn.contains(holder);
   }

   private final ArrayList<IHolder> _placedIntoHoldThisTurn = new ArrayList<>();
   private final HashMap<IHolder, Byte> _heldPenalties    = new HashMap<>();
   private Character                _holdTarget = null;

   public static Comparator<? super Character> nameComparator = new Comparator<> () {
      @Override
      public int compare(Character char1, Character char2) {
         return char1.getName().compareTo(char2.getName());
      }
   };

   public void regenerateWound(byte woundReduction) {
      getCondition().regenerateWound(woundReduction);
   }

   public String canTarget(Character target, TargetType targetType) {
      if (target != null) {
         switch (targetType) {
            case TARGET_SELF:
               if (this._uniqueID != target._uniqueID) {
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
               if (this._uniqueID == target._uniqueID) {
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
               if (this._uniqueID == target._uniqueID) {
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
