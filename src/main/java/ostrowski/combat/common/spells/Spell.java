package ostrowski.combat.common.spells;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Advantage;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.SpecialDamage;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.mage.MageSpell;
import ostrowski.combat.common.spells.priest.PriestMissileSpell;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestDefense;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.Battle;
import ostrowski.combat.server.BattleTerminatedException;
import ostrowski.combat.server.Configuration;
import ostrowski.protocol.SerializableObject;

/**
 * @author pnostrow
 *
 */
public abstract class Spell extends SerializableObject implements Enums, Cloneable, Comparable<Spell>
{
   protected String    _name;
   // These attributes are serialized, because they may differ from one 'fire' spell to another 'fire' spell:
   protected byte      _power                     = 0;
   protected byte      _level                     = 0;
   protected Character _caster                    = null;
   protected Character _target                    = null;
   public int          _casterID                  = -1;
   public int          _targetID                  = -1;
   private boolean     _maintainedThisTurn        = true;
   private boolean     _isInateSpell              = false;
   private byte        _incantationRoundsRequired = 0;
   private byte        _defenseTN;
   private int         _castRoll;
   protected boolean   _castRolledAllOnes         = false;
   private int         _defenseRoll;
   private int         _resistanceRoll;
   protected byte      _castingEffectiveness      = EFFECTIVENESS_FAILURE;
   protected byte      _excessSuccess;

   public Spell() {
   }

   public Spell(String name) {
      _name = name;
   }

   public String getName() {
      return _name;
   }

   public byte getPower() {
      return _power;
   }

   public void setPower(byte power) {
      _power = power;
   }

   public byte getLevel() {
      return _level;
   }

   public void setLevel(byte level) {
      _level = level;
   }

   public Wound channelEnergy(byte additionalPower) {
      _maintainedThisTurn = true;
      _power += additionalPower;
      return null;
   }

   /**
    * Each turn, this method should be called on all active spells.
    */
   public void newTurn() {
      _maintainedThisTurn = false;
   }

   public void completeSpell() {
      _maintainedThisTurn = true;
   }

   public void maintainSpell() {
      _maintainedThisTurn = true;
   }

   public void discardSpell() {
      _maintainedThisTurn = false;
   }

   public void setIsInate(boolean isInate) {
      _isInateSpell = isInate;
   }

   public boolean isInate() {
      return _isInateSpell;
   }

   public boolean isMaintainedThisTurn() {
      return _maintainedThisTurn;
   }

   public void beginIncantation() {
      _incantationRoundsRequired = getIncantationTime();
   }

   public boolean incant() {
      maintainSpell();
      _incantationRoundsRequired--;
      return (_incantationRoundsRequired <= 0);
   }

   public byte getIncantationRoundsRequired() {
      return _incantationRoundsRequired;
   }

   public abstract String describeEffects(Character defender, boolean firstTime);

   public abstract String describeSpell();

   public abstract byte getTN(Character caster);

   protected static String getSpellGrimioreForHTML(Collection< ? extends Spell> spells) {
      boolean formatForRuleBook = false;
      Table table = new Table();
      table.addRow(new TableRow(-1, "Spell name", "Spell description"));
      int htmlRow = 0;
      Spell previousSpell = null;
      StringBuilder sb = new StringBuilder();
      sb.append("<dl>");
      for (Spell spell : spells) {
         if ((previousSpell != null) && (previousSpell.compareTo(spell) == 0)) {
            continue;
         }
         StringBuilder descriptionBuffer = new StringBuilder();
         descriptionBuffer.append("<A NAME='").append(spell.getName().replaceAll(" ", "")).append("'><!-- --></A>");
         descriptionBuffer.append(spell.describeSpell());
         spell.describeGrimioreDescription(descriptionBuffer);
         if (!formatForRuleBook) {
            if (!(spell instanceof ICastInBattle) && !(spell instanceof IInstantaneousSpell)) {
               descriptionBuffer.append("<br/><i>No effect in this simulator</i>");
            }
         }
         TableRow row = new TableRow(htmlRow++, spell.getName()).addTD(new TableData(descriptionBuffer.toString()).setAlignLeft());
         table.addRow(row);
         sb.append("<br/><dt><b>").append(spell.getName()).append("</b></dt><dd>").append(descriptionBuffer).append("</dd>\n");
      }
      sb.append("</dl>");
      if (formatForRuleBook) {
         return sb.toString();
      }
      return table.toString();
   }

   protected void describeGrimioreDescription(StringBuilder descriptionBuffer) {
      if (this instanceof IResistedSpell) {
         IResistedSpell resistedSpell = (IResistedSpell) this;
         descriptionBuffer.append("<br/><b>Resisted by:</b> ");
         if (this instanceof ResistedPriestSpell) {
            ResistedPriestSpell resistedPriestSpell = (ResistedPriestSpell) this;
            descriptionBuffer.append(resistedPriestSpell.getResistanceAttributeName());
            descriptionBuffer.append(resistedPriestSpell.describeResistanceTN());
         }
         else {
            descriptionBuffer.append(resistedSpell.getResistanceAttributeName());
            byte actions = resistedSpell.getResistanceActions();
            if (Configuration.useExtendedDice()) {
               descriptionBuffer.append(", at ").append(actions).append(" actions.");
            }
            else {
               if (actions == 1) {
                  descriptionBuffer.append(" - 5");
               }
               if (actions == 3) {
                  descriptionBuffer.append(" + 5");
               }
               descriptionBuffer.append(".");
            }
         }
      }
      if (this instanceof IRangedSpell) {
         descriptionBuffer.append("<br/><b>Base range:</b> ").append(getAdjustedRangeBase()).append(".");
      }
      if (this instanceof IExpiringSpell) {
         IExpiringSpell expSpell = (IExpiringSpell) this;
         short baseExp = expSpell.getBaseExpirationTimeInTurns();
         short bonusExp = expSpell.getBonusTimeInTurnsPerPower();
         if ((baseExp != 0) || (bonusExp != 0)) {
            descriptionBuffer.append("<br/><b>Duration:</b> ");
            descriptionBuffer.append(baseExp).append(" turns");
            if (bonusExp != 0) {
               if (bonusExp > 0) {
                  descriptionBuffer.append(", plus ");
               }
               else {
                  descriptionBuffer.append(", minus ");
               }
               descriptionBuffer.append(bonusExp);
               descriptionBuffer.append(" turns for each point of ");
               if (this instanceof PriestSpell) {
                  descriptionBuffer.append("effective ");
               }
               descriptionBuffer.append("power");
            }
            descriptionBuffer.append(".");
         }
      }
   }

   public boolean isDefendable() {
      return false;
   }

   public RANGE getRange(short distanceInHexes) {
      if (this instanceof IRangedSpell) {
         short rangeBaseAdjusted = getAdjustedRangeBase();
         return Rules.getRangeForSpell(distanceInHexes, rangeBaseAdjusted);
      }
      return RANGE.OUT_OF_RANGE;
   }

   public short getAdjustedRangeBase() {
      return getAdjustedRangeBase(getCaster());
   }
   public short getAdjustedRangeBase(Character caster) {
      if (this instanceof IRangedSpell) {
         short rangeBase = ((IRangedSpell) this).getRangeBase();
         if (caster != null) {
            byte rangeDeterminingAttribute = caster.getAttributeLevel(getCastingAttribute());
            byte buildModifier = caster.getRace().getBuildModifier();
            byte sizeAdjustedAttr = (byte) (rangeDeterminingAttribute + buildModifier);
            short rangeBaseAdjusted = (short) Math.round(rangeBase * Rules.getRangeAdjusterForAdjustedStr(sizeAdjustedAttr));
            return rangeBaseAdjusted;
         }
         return rangeBase;
      }
      return 0;
   }

   public byte getRangeTNAdjustment(short distanceInHexes) {
      if (distanceInHexes < 2) {
         return 0;
      }
      // a target that is 2 hexes away has a TN penalty of 1.
      if (distanceInHexes < 127) {
         return (byte) (distanceInHexes - 1);
      }
      return (byte) 127;
   }

   /**
    * Spells that have a time limit, return this limit (in Turns). Spells that have no time limit
    * (or are instantaneous) return -1.
    * @return number of Turns remaining for spell effect
    */
   public short getDuration() {
      return 0;
   }

   public abstract byte getIncantationTime();

   @SuppressWarnings("unused")
   public void applySpell(Character target, Arena arena) throws BattleTerminatedException {
      if (target != null) {
         target.addSpellToActiveSpellsList(this, arena);
      }
      applyEffects(arena);
   }

   public void applyEffects(Arena arena) {
   }

   public void removeEffects(Arena arena) {
   }

   /**
    * @return the actions used in the resisted attribute to resist this spell.
    * Difficult spells (Control Mind, for example) return higher values.
    */
   public byte getResistanceActions() {
      return 1;
   }

   public int getMaxAttackActions() {
      return 1;
   }

   /**
    * completeTurn returns 'true' if this spell expired this turn.
    * @param arena
    * @return
    */
   public boolean completeTurn(Arena arena) {
      return false;
   }

   // This method is used by spell that raise or lower a beings PD (armor or blur spells):
   public byte getPassiveDefenseModifier(boolean vsRangedWeapons, DamageType vsDamageType) {
      return 0;
   }

   // These methods are used by spell that raise or lower a being's actions per turn (speed & slow spells):
   public byte getModifiedActionsPerRound(byte previousActionsPerRound) {
      return previousActionsPerRound;
   }

   public byte getModifiedActionsPerTurn(byte previousActionsPerTurn) {
      return previousActionsPerTurn;
   }

   public byte getModifiedMovementPerRound(byte previousMovementRate) {
      return previousMovementRate;
   }

   public void setCaster(Character caster) {
      _caster = caster;
      _casterID = caster._uniqueID;
   }

   public Character getCaster() {
      return _caster;
   }

   public String getCasterName() {
      if (_caster == null) {
         return "";
      }
      return _caster.getName();
   }

   public void setTarget(Character target) {
      if (getTargetType() == TargetType.TARGET_NONE) {
         return;
      }
      if (getTargetType() == TargetType.TARGET_AREA) {
         return;
      }
      if (getTargetType() == TargetType.TARGET_OBJECT) {
         return;
      }
      if (getTargetType() == TargetType.TARGET_SELF) {
         _target = _caster;
      }
      else {
         _target = target;
      }
   }

   public Character getTarget() {
      return _target;
   }

   public String getTargetName() {
      if (_target == null) {
         return "";
      }
      return _target.getName();
   }

   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

   public boolean getCanTarget(Character target) {
      return (canTarget(getCaster(), target) == null);
   }

   public short getMaxRange(Character caster) {
      return 1;
   }

   public short getMinRange(Character caster) {
      return 0;
   }

   public Wound getNewTurnBurnWound() {
      return null;
   }

   public Wound getFailureBurnWound() {
      return null;
   }

   public Wound setResults(int excessSuccess, boolean success, boolean effective, int castRoll, byte skill, Arena _arena) {
      return null;
   }

   /**
    * Spells that are cast upon ones self or ones own team mates should return true.
    * @return
    */
   public boolean isBeneficial() {
      return (getTargetType() == TargetType.TARGET_SELF);
   }

   public byte getActiveDefensiveTN(byte actionsSpent, Character caster) {
      return 0;
   }

   public SpecialDamage getSpecialDamageModifier() {
      return new SpecialDamage(0);
   }

   public String getSpecialDamageModifierExplanation() {
      return "";
   }

   public static String generateHtmlTable() {
      StringBuilder sb = new StringBuilder();
      sb.append(MageSpell.generateHtmlTableMageSpells());
      sb.append(PriestSpell.generateHtmlTablePriestSpells());
      return sb.toString();
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
// No need to serialize the name, the serialization key uniquely defined this spell
//         writeToStream(_name, out);
         writeToStream(_power, out);
         writeToStream(_level, out);
         // _resistedAtt, _prerequisiteSpellNames & _attributeMod don't need to be serialized,
         // because they are constant for a given spell (defined by its name).
         writeToStream(_maintainedThisTurn, out);
         writeToStream(_isInateSpell, out);
         writeToStream(_incantationRoundsRequired, out);
         writeToStream(_defenseTN, out);
         writeToStream(_castRoll, out);
         writeToStream(_castRolledAllOnes, out);
         writeToStream(_defenseRoll, out);
         writeToStream(_resistanceRoll, out);
         writeToStream(_castingEffectiveness, out);

      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
      // No need to serialize the name, the serialization key uniquely defined this spell
//         String name = readString(in);
//         Spell spell = MageSpell.getSpell(name).clone();
//         this.copyDataFrom(spell);
         _power = readByte(in);
         _level = readByte(in);
         // _resistedAtt, _prerequisiteSpellNames & _attributeMod don't need to be serialized,
         // because they are constant for a given spell (defined by its name).
         _maintainedThisTurn = readBoolean(in);
         _isInateSpell = readBoolean(in);
         _incantationRoundsRequired = readByte(in);
         _defenseTN = readByte(in);
         _castRoll = readInt(in);
         _castRolledAllOnes = readBoolean(in);
         _defenseRoll = readInt(in);
         _resistanceRoll = readInt(in);
         _castingEffectiveness = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   protected void copyDataFrom(Spell source) {
      _caster = source._caster;
      _name = source._name;
      _power = source._power;
      _level = source._level;
      _target = source._target;
      _maintainedThisTurn = source._maintainedThisTurn;
      _isInateSpell = source._isInateSpell;
      _incantationRoundsRequired = source._incantationRoundsRequired;
      _defenseTN = source._defenseTN;
      _castRoll = source._castRoll;
      _castRolledAllOnes = source._castRolledAllOnes;
      _defenseRoll = source._defenseRoll;
      _resistanceRoll = source._resistanceRoll;
      _castingEffectiveness = source._castingEffectiveness;
   }

   @Override
   public Spell clone() {
      try {
         Spell spell = (Spell) super.clone();
         spell.copyDataFrom(this);
         return spell;
      } catch (CloneNotSupportedException e) {
         e.printStackTrace();
         DebugBreak.debugBreak("Clone failure");
         return null;
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(getName()).append(" spell");
      sb.append(", power=").append(getPower());
      sb.append(", level=").append(getLevel());
      sb.append(", caster=").append(getCasterName());
      if (_target != null) {
         sb.append(", target=").append(getTargetName());
      }
      return sb.toString();
   }

   public Wound modifyDamageDealt(Wound wound, Character attacker, Character defender, String attackingWeaponName, StringBuilder modificationsExplanation) {
      return wound;
   }

   public Wound modifyDamageRecieved(Wound wound) {
      return wound;
   }

   public DiceSet getCastDice(byte actionsUsed, RANGE range) {
      Attribute att = getCastingAttribute();
      return Rules.getDice(getCaster().getAttributeLevel(att), actionsUsed, att/*attribute*/);
   }

   public Attribute getCastingAttribute() {
      if (isInate()) {
         return Attribute.Dexterity;
      }
      return Attribute.Intelligence;
   }

   public boolean resolveSpell(RequestAction attack, RequestDefense defense, Map<Character, List<Spell>> spells,
                               Map<Character, List<Wound>> wounds, Battle battle) throws BattleTerminatedException {
      StringBuilder sbDescription = new StringBuilder();
      // explain casting
      // determine spell casting TN
      // determine defense against spell (defensive TN, resistance roll)
      // apply distance modifier (to TN or power)
      // roll attack
      // roll resistance
      // determine effectiveness

      sbDescription.append(describeCastingAction(attack.isAdvance(), 0/*spellActions*/));
      short distanceInHexes = describeDistance(battle, sbDescription);
      RANGE range = getRange(distanceInHexes);
      byte rangeTNAdjustment = getRangeTNAdjustment(distanceInHexes);
      byte spellTN = getTN(_caster);
      // defense information:
      // result:

      StringBuilder sbCastingDescription = new StringBuilder();
      sbCastingDescription.append("<table border=1>");
      byte castingTN = getCastingTN(attack, distanceInHexes, range, spellTN, battle, sbCastingDescription);
      byte rangeAdjustedCastingTN = getRangeAdjustedCastingTN(attack, distanceInHexes, range, castingTN, rangeTNAdjustment, battle,
                                                              sbCastingDescription);
      sbCastingDescription.append("</table>");
      if ((rangeAdjustedCastingTN != 0) || !isInate()) {
         sbDescription.append(sbCastingDescription);
      }

      _castingEffectiveness = EFFECTIVENESS_FAILURE;
      byte castingPower;
      if (this instanceof ResistedPriestSpell) {
         // don't fill in the description here, cause it will be filled in in the call to getDefenseTn/resolveEffectivePower
         castingPower = getCastingPower(attack, distanceInHexes, range, battle, null/*sbDescription*/);
      }
      else {
         castingPower = getCastingPower(attack, distanceInHexes, range, battle, sbDescription);
      }
      if ((_target == null) && (getTargetType() != TargetType.TARGET_NONE)
               && !(this instanceof IAreaSpell) && (((IAreaSpell) this).getTargetLocation() == null))  {
         sbDescription.append(getCasterName()).append("'s ").append(getName()).append(" spell is wasted because ").append(getCasterName()).append(" has no target!");
      }
      else {
         if (!(this instanceof PriestSpell) || (castingPower > 0)) {
            _defenseTN = getDefenseTn(attack, defense, distanceInHexes, range, battle, sbDescription);
            _castRoll = resolveCast(attack, rangeAdjustedCastingTN, castingPower, distanceInHexes, range, battle, sbDescription);
            _defenseRoll = getDefenseResult(defense, castingPower, battle, sbDescription);
            _resistanceRoll = getResistanceResult(defense, distanceInHexes, battle, sbDescription);
            _castingEffectiveness = getEffectiveness(attack, defense, distanceInHexes, castingTN, rangeAdjustedCastingTN, battle, sbDescription);
         }
      }
      if (_castingEffectiveness >= 0) {
         if (this instanceof IMissileSpell) {
            IMissileSpell missileSpell = ((IMissileSpell) this);

            byte bonusDamage = _castingEffectiveness;
            DiceSet damageDie = missileSpell.getDamageDice();
            String damageExplanation = missileSpell.explainDamage();
            battle.resolveDamage(_caster, _target, getName() + " spell", damageExplanation, missileSpell.getSpellDamageBase(), bonusDamage, damageDie,
                                 missileSpell.getDamageType(), getSpecialDamageModifier(), getSpecialDamageModifierExplanation(), sbDescription, wounds,
                                 _excessSuccess, false/*isCharge*/);
         }
         String effects = describeEffects(_target, true/*firstTime*/);
         if (effects != null) {
            sbDescription.append("<br/>").append(effects);
         }

         Character target = getTarget();
         if (target == null) {
            target = _caster;
         }
         List<Spell> spellList = spells.get(target);
         if (spellList == null) {
            spellList = new ArrayList<>();
            spells.put(target, spellList);
         }
         spellList.add(this);
      }
      String message = sbDescription.toString();
      battle._arena.sendMessageTextToAllClients(message, false/*popUp*/);
      battle._arena.sendMessageTextToParticipants(message, getCaster(), getTarget());

      return (_castingEffectiveness >= 0);
   }

   protected int resolveCast(RequestAction attack, byte rangeAdjustedCastingTN, byte castingPower, short distanceInHexes, RANGE range, Battle battle,
                           StringBuilder sbDescription) {
      DiceSet castDice = getCastDice(attack, rangeAdjustedCastingTN, castingPower, distanceInHexes, range, battle, sbDescription);
      if (!Configuration.useExtendedDice() && (this instanceof MageSpell)) {
         if (Configuration.useSimpleDice()) {
            castDice = new DiceSet(0/*d1*/, 0/*d4*/, 0/*d6*/, 0/*d8*/, 1/*d10*/, 0/*d12*/, 0/*d20*/, 0/*dBell*/, 1.0/*multiplier*/);
         }
         else {
            castDice = new DiceSet(0/*d1*/, 0/*d4*/, 0/*d6*/, 0/*d8*/, 0/*d10*/, 0/*d12*/, 0/*d20*/, 1/*dBell*/, 1.0/*multiplier*/);
         }
      }
      DiceSet adjCastDice = getCaster().adjustDieRoll(castDice, RollType.SPELL_CASTING, null/*target*/);
      byte woundsAndPain = getCaster().getWoundsAndPainPenalty();
      int castRoll = adjCastDice.roll(true/*allowExplodes*/);
      _castRolledAllOnes = adjCastDice.lastRollRolledAllOnes();
      if ((adjCastDice.getDiceCount() > 0) || (_castRoll != 0)) {
         sbDescription.append("<br/>");
         sbDescription.append(_caster.getName()).append(" rolls ").append(adjCastDice);
         sbDescription.append(", rolling ").append(adjCastDice.getLastDieRoll());
         sbDescription.append(", for a total of ").append(castRoll);
         if (!Configuration.useExtendedDice() && (this instanceof MageSpell)) {
            sbDescription.append("<table border=1>");
            sbDescription.append("<tr><td>").append(castRoll).append("</td><td>Die Roll.</td></tr>");

            byte adjSkillLevel = getAdjustedCastingSkillLevel(_caster);
            castRoll += adjSkillLevel;
            sbDescription.append("<tr><td>").append(adjSkillLevel).append("</td><td>Adj. Skill.</td></tr>");

            byte effectiveActions = ((MageSpell) this).getActionsUsed(attack);
            int actionsAdj = Rules.getTNBonusForActions(effectiveActions);
            castRoll += actionsAdj;
            sbDescription.append("<tr><td>");
            if (actionsAdj >= 0) {
               sbDescription.append("+");
            }
            sbDescription.append(actionsAdj).append("</td><td>").append(effectiveActions).append("-actions</td></tr>");

            castRoll -= woundsAndPain;
            sbDescription.append("<tr><td>").append(0-woundsAndPain).append("</td><td>").append("pain and wounds</td></tr>");

            sbDescription.append("<tr><td>").append(castRoll).append("</td><td>roll result.</td></tr>");
            sbDescription.append("</table>");
         }
         else if (this instanceof PriestMissileSpell) {
            sbDescription.append("<table border=1>");
            sbDescription.append("<tr><td>").append(castRoll).append("</td><td>Die Roll.</td></tr>");
            byte skill = getCastingLevel();
            Attribute attribute = Attribute.Dexterity;
            byte attrLevel = _caster.getAttributeLevel(attribute);
            castRoll += (skill + attrLevel);
            sbDescription.append("<tr><td>").append(skill).append("</td><td>Divine Aff. ").append("</td></tr>");
            sbDescription.append("<tr><td>").append(attrLevel).append("</td><td>").append(attribute.shortName).append("</td></tr>");

            castRoll -= woundsAndPain;
            sbDescription.append(0-woundsAndPain).append("</td><td>").append("caster's pain and wounds.</td></tr>");

            sbDescription.append("<tr><td>").append(castRoll).append("</td><td>roll result.</td></tr>");
            sbDescription.append("</table>");
         }
      }
      return castRoll;
   }

   public byte getAdjustedCastingSkillLevel(Character caster) {
      return Rules.getAdjustedSkillLevel(getCastingLevel(), caster.getAttributeLevel(getCastingAttribute()));
   }

   protected static final int EFFECTIVENESS_FAILURE      = -5;
   protected static final int EFFECTIVENESS_MISSED       = -4;
   protected static final int EFFECTIVENESS_NO_EFFECT    = -3;
   protected static final int EFFECTIVENESS_RESISTED     = -2;
   protected static final int EFFECTIVENESS_OUT_OF_RANGE = -1;
   protected static final int EFFECTIVENESS_SUCCESS      = 0;

   protected short describeDistance(Battle battle, StringBuilder sbDescription) {
      short distanceInHexes = 0;
      if (_target != null) {
         distanceInHexes = Arena.getMinDistance(_caster, _target);
      }
      if (this instanceof IRangedSpell) {
         sbDescription.append("The ").append(getName()).append(" spell");
         sbDescription.append(" has an adjusted base range of ").append(getAdjustedRangeBase());
         sbDescription.append(", and the target is ").append(distanceInHexes).append(" hexes away, making this ");
         sbDescription.append(getRange(distanceInHexes).getName()).append(" range.<br/>");
      }
      else {
         sbDescription.append("The target, ").append(getTargetName()).append(" is ");
         sbDescription.append(distanceInHexes).append(" hexes away.<br/>");
      }
      return distanceInHexes;
   }

   abstract protected byte getCastingTN(RequestAction attack, short distanceInHexes, RANGE range, byte spellTN, Battle battle,
                                                    StringBuilder sbDescription);

   abstract protected byte getRangeAdjustedCastingTN(RequestAction attack, short distanceInHexes, RANGE range, byte castingTN, byte rangeTNAdjustment,
                                                     Battle battle, StringBuilder sbDescription);

   abstract public byte getCastingPower(RequestAction attack, short distanceInHexes, RANGE range, Battle battle, StringBuilder sbDescription);

   abstract protected DiceSet getCastDice(RequestAction attack, byte distanceModifiedTN, byte distanceModifiedPower, short distanceInHexes, RANGE range,
                                          Battle battle, StringBuilder sbDescription);

   abstract protected int getDefenseResult(RequestDefense defense, byte distanceModifiedPower, Battle battle, StringBuilder sbDescription);

   public byte getCastingLevel() {
      return _caster.getSpellSkill(getName());
   }

   protected String describeCastingAction(boolean isAdvance, int spellActions) {
      StringBuilder sbDescription = new StringBuilder();
      sbDescription.append("<span style=\"color:red\">");
      sbDescription.append(_caster.getName());
      if (isAdvance) {
         sbDescription.append(" advances to cast a ");
      }
      else {
         sbDescription.append(" casts a ");
      }
      sbDescription.append(getPower()).append("-power '");
      sbDescription.append(getName()).append("' spell");
      if (_target != null) {
         sbDescription.append(" on ");
         if (_target == _caster) {
            sbDescription.append("himself");
         }
         else {
            sbDescription.append(_target.getName());
         }
      }
      if (spellActions > 0) {
         sbDescription.append(" with ").append(spellActions).append(" actions.");
      }
      sbDescription.append(":<br/></span>");
      return sbDescription.toString();
   }

   protected byte getDefenseTn(RequestAction attack, RequestDefense defense, short distanceInHexes,
                               RANGE range, Battle battle, StringBuilder sbDescription) {
      if (isDefendable()) {
         return battle.resolveDefenses(_target, defense, (byte) 0/*attackParryPenalty*/, distanceInHexes,
                                       range, attack, sbDescription);
      }
      return 0;
   }

   protected int getResistanceResult(RequestDefense defense, short distanceInHexes, Battle battle,
                                     StringBuilder sbDescription) {
      if (!(this instanceof IResistedSpell)) {
         return 0;
      }
      IResistedSpell resistedThis = (IResistedSpell) this;
      // Since getRangeTNAdjustment returns a positive number for increased ranges,
      // we want to subtract this from the resistanceTN
      int distanceModifier = getRangeTNAdjustment(distanceInHexes);
      int defenderWoundPenalty = 0;// - defender.getWounds();
      //defenderWoundPenalty = 0 - defender.getWoundsAndPainPenalty();
      byte resistanceAttributeLevel = resistedThis.getResistanceAttribute(_target);
      DiceSet resistanceDice = resistedThis.getResistanceDice(_target);
      resistanceDice = getTarget().adjustDieRoll(resistanceDice, RollType.MAGIC_RESISTANCE, null/*target*/);
      int resistanceRoll = resistanceDice.roll(true/*allowExplodes*/);
      byte magicResistanceBonus = 0;
      Advantage magicResistanceAdv = null;
      if (this instanceof MageSpell) {
         magicResistanceAdv = getTarget().getAdvantage(Advantage.MAGIC_RESISTANCE);
         if (magicResistanceAdv != null) {
            magicResistanceBonus = Rules.getMagicResistanceBonus(magicResistanceAdv, resistedThis.getResistanceActions());
            resistanceRoll += magicResistanceBonus;
         }
      }
      sbDescription.append("<br/>");
      sbDescription.append(getTargetName()).append(" resists with ");
      if (Configuration.useExtendedDice()) {
         sbDescription.append(resistedThis.getResistanceActions()).append(" actions vs. a ");
         sbDescription.append(resistedThis.getResistanceAttributeName()).append(" of ");
         sbDescription.append(resistanceAttributeLevel);
      }
      else {
         sbDescription.append(resistedThis.getResistanceAttributeName()).append(" of ");
         sbDescription.append(resistanceAttributeLevel);
         if (this instanceof PriestSpell) {
            byte actions = resistedThis.getResistanceActions();
            if (actions == 1) {
               sbDescription.append(" - 5");
            }
            if (actions == 3) {
               sbDescription.append(" + 5");
            }
//            sbDescription.append(" + d10");
//            if (Configuration.useBellCurveDice()) {
//               sbDescription.append("±");
//            }
         }
      }

      sbDescription.append(" = ");
      sbDescription.append(resistanceDice).append(", rolling");
      sbDescription.append(resistanceDice.getLastDieRoll());
      if (magicResistanceAdv != null) {
         sbDescription.append(", plus ").append(magicResistanceBonus).append(" for having the 'Magic Resistance' advantage at level ").append(magicResistanceAdv.getLevel()).append(".");
      }

      if (this instanceof PriestSpell) {
         sbDescription.append(", resulting in a resistance roll of ");
      }
      else {
         sbDescription.append(", resulting in a resistance TN of ");
      }
      sbDescription.append(resistanceRoll);
      int woundAdjustedResitanceRoll = resistanceRoll - defenderWoundPenalty;
      int adjustedResistanceRoll = woundAdjustedResitanceRoll + distanceModifier;
      if ((defenderWoundPenalty != 0) || (distanceModifier != 0)) {
         sbDescription.append("<table border=1>");
         sbDescription.append("<tr><td>").append(resistanceRoll).append("</td>");
         sbDescription.append("<td>resistance TN</td></tr>");

         if (distanceModifier != 0) {
            sbDescription.append("<tr>");
            sbDescription.append("<td>");
            if (distanceModifier > 0) {
               sbDescription.append("+");
            }
            sbDescription.append(distanceModifier).append("</td>");
            sbDescription.append("<td>").append("distance modifier for being ").append(distanceInHexes).append(" yards away.</td>");
            sbDescription.append("</tr>");
         }
         if (defenderWoundPenalty != 0) {
            sbDescription.append("<tr>");
            sbDescription.append("<td>-").append(defenderWoundPenalty).append("</td>");
            sbDescription.append("<td>").append("defenders wounds penalty").append("</td>");
            sbDescription.append("</tr>");

         }
         sbDescription.append("<tr>");
         sbDescription.append("<td><b>").append(adjustedResistanceRoll).append("</b></td>");
         sbDescription.append("<td><b>adjusted resistance roll</b></td>");
         sbDescription.append("</tr>");
         sbDescription.append("</table>");
      }
      return adjustedResistanceRoll;
   }

   protected byte getEffectiveness(RequestAction attack, RequestDefense defense, short distanceInHexes, byte castingTN,
                                   byte rangeAdjustedCastingTN, Battle battle, StringBuilder sbDescription) {
      byte skill = getCastingLevel();
      byte result = (byte) _castRoll;
      if (!Configuration.useSimpleDice() && !Configuration.useBellCurveDice()) {
         result += skill;
      }

      sbDescription.append("<table border=1>");
      if (_castRoll > 0) {
         if (this instanceof PriestSpell) {
            // Priest missile spell have already had the D.A. added in
            if (!(this instanceof PriestMissileSpell)) {
               sbDescription.append("<tr><td>").append(_castRoll).append("</td><td>Dice roll</td></tr>");
               sbDescription.append("<tr><td>+").append(skill).append("</td><td>Divine Affinity</td></tr>");
               if (Configuration.useSimpleDice() || Configuration.useBellCurveDice()) {
                  result += skill;
               }
            }
         }
         else {
            if (!Configuration.useSimpleDice() && !Configuration.useBellCurveDice()) {
               sbDescription.append("<tr><td>").append(_castRoll).append("</td><td>Dice roll</td></tr>");
               sbDescription.append("<tr><td>+").append(skill).append("</td><td>Spell skill</td></tr>");
            }
         }
         sbDescription.append("<tr><td>").append(result).append("</td>");
         sbDescription.append("<td><b>Final cast roll</b></td></tr>");
         if (!this.isInate()) {
            if (!(this instanceof PriestSpell)) {
               sbDescription.append("<tr><td><b>").append(castingTN).append("</b></td>");
               sbDescription.append("<td>Spell Casting TN</td></tr>");
            }
            if (rangeAdjustedCastingTN != castingTN) {
               sbDescription.append("<tr><td><b>").append(rangeAdjustedCastingTN).append("</b></td>");
               sbDescription.append("<td>Range Adjusted Spell TN</td></tr>");
            }
         }
         if (isDefendable()) {
            sbDescription.append("<tr><td><b>").append(_defenseTN).append("</b></td>");
            sbDescription.append("<td>Defenders TN</td></tr>");
         }
         if (this instanceof IResistedSpell) {
            sbDescription.append("<tr><td><b>").append(_resistanceRoll).append("</b></td>");
            sbDescription.append("<td>Resistance TN</td></tr>");
         }
      }
      _excessSuccess = (byte) (result - castingTN);
      boolean success = (isInate() || (this instanceof PriestSpell) || (_excessSuccess >= 0)) && !_castRolledAllOnes;
      boolean effective = false;
      if (success) {
         // This instanceof check is replaced by isDefendable, with a check of validity in the
         // static initializer of Spell.
         //if ((this instanceof IMissileSpell) || (this instanceof SpellSpiderWeb)) {
         if (isDefendable()) {
            effective = (result >= _defenseTN);
            _excessSuccess = (byte) (result - _defenseTN);
         }
         else if (this instanceof IResistedSpell) {
            if (this instanceof ResistedPriestSpell) {
               _excessSuccess = (byte) (_defenseTN - _resistanceRoll);
               effective = (_excessSuccess > 0);
            }
            else {
               _excessSuccess = (byte) (result - _resistanceRoll);
               effective = (_excessSuccess >= 0);
            }
         }
         else {
            effective = (result >= rangeAdjustedCastingTN);
            _excessSuccess = (byte) (result - rangeAdjustedCastingTN);
         }
      }

      Wound burn = setResults(_excessSuccess, success, effective, _castRoll, skill, battle._arena);
      _castingEffectiveness = EFFECTIVENESS_SUCCESS;
      if (success) {
         if (this instanceof IMissileSpell) {
            if (effective) {
               byte bonusDamage = Rules.getDamageBonusForSkillExcess((byte) (result - _defenseTN));
               sbDescription.append("<tr><td><b>").append(bonusDamage).append("</b></td>");
               sbDescription.append("<td><b>spell succeeded</b>, bonus damage</td></tr>");
               _castingEffectiveness = bonusDamage;
            }
            else {
               sbDescription.append("<tr><td colspan=2><b>spell succeeded</b>, but missed the target</td></tr>");
               _castingEffectiveness = EFFECTIVENESS_MISSED;
            }
         }
         else if (effective) {
            sbDescription.append("<tr><td colspan=2><b>spell succeeded</b></td></tr>");
            //_castingEffectiveness = (byte) (result - _defenseTN);
            _castingEffectiveness = _excessSuccess;
         }
         else {
            if (this instanceof IResistedSpell) {
               sbDescription.append("<tr><td colspan=2><b>spell cast succesfully, but was resisted.</b></td></tr>");
               _castingEffectiveness = EFFECTIVENESS_RESISTED;
            }
            else {
               sbDescription.append("<tr><td colspan=2><b>spell cast succesfully, but was ineffective.</b></td></tr>");
               _castingEffectiveness = EFFECTIVENESS_NO_EFFECT;
            }
         }
      }
      else {
         if (burn != null) {
            sbDescription.append("<tr><td>").append(burn.getWounds());
            sbDescription.append("</td><td><b>Casting failure!</b>");
            if (_castRolledAllOnes) {
               sbDescription.append("(all 1s rolled)");
            }
            sbDescription.append("  Spell burn level (");
            sbDescription.append(getPower()).append(" - ").append(_caster.getMagicalAptitude());
            sbDescription.append(")</td></tr>");
         }
         else {
            sbDescription.append("<tr><td colspan=2><b>Spell failed</b>");
            if (_castRolledAllOnes) {
               sbDescription.append("(all 1s rolled)");
            }
            sbDescription.append("</td></tr>");
         }
         _castingEffectiveness = EFFECTIVENESS_FAILURE;
      }
      sbDescription.append("</table>");

      return _castingEffectiveness;
   }

   public DamageType getDamageType() {
      return DamageType.NONE;
   }

   public byte getPowerPenaltyForWoundsAndPain() {
      return 0;
   }

   /**
    * This method returns true if the spell in the parameter is incompatible with this spell.
    * Generally, this is true is the spell in the parameter is the same type of spell as the 'this' spell.
    * For example, a 'charm person' spell will not work on an already charmed person, unless the new spell
    * has a greater power than the original spell.
    * @param spell
    * @return
    */
   public boolean isIncompatibleWith(Spell spell) {
      return getName().equals(spell.getName());
   }

   public Spell getActiveSpellIncompatibleWith(Character target) {
      for (Spell currentActiveSpell : target.getActiveSpells()) {
         if (isIncompatibleWith(currentActiveSpell)) {
            if (currentActiveSpell.takesPrecedenceOver(this)) {
               return currentActiveSpell;
            }
         }
      }
      return null;
   }

   public boolean takesPrecedenceOver(Spell spell) {
      return getPower() > spell.getPower();
   }

   @Override
   public int compareTo(Spell otherSpell) {
      return getName().compareTo(otherSpell.getName());
   }

   abstract public byte getSpellPoints();

   // If a spell can modify wounds as they occur, they need to override this method
   public Wound alterWound(Wound wound, StringBuilder alterationExplanationBuffer) {
      return wound;
   }

   // If a spell can modify the attacker dice, they need to override this method
   public DiceSet adjustDieRoll(DiceSet dice, RollType rollType, Object target) {
      return dice;
   }

   // If a spell can modify the resistance dice, they need to override this method
   public DiceSet adjustResistanceRoll(DiceSet dice) {
      return dice;
   }

   public Element getXMLObject(Document parentDoc, String newLine) {
      Element mainElement = parentDoc.createElement("Spell");
      mainElement.setAttribute("Name", _name);
      mainElement.setAttribute("power", String.valueOf(_power));
      if (_caster != null) {
         mainElement.setAttribute("casterID", String.valueOf(_caster._uniqueID));
      }
      if (_target != null) {
         mainElement.setAttribute("targetID", String.valueOf(_target._uniqueID));
      }
      mainElement.setAttribute("maintainedThisTurn", String.valueOf(_maintainedThisTurn));
      mainElement.setAttribute("isInateSpell", String.valueOf(_isInateSpell));
      mainElement.setAttribute("incantationRoundsRequired", String.valueOf(_incantationRoundsRequired));
      mainElement.setAttribute("defenseTN", String.valueOf(_defenseTN));
      mainElement.setAttribute("castRoll", String.valueOf(_castRoll));
      mainElement.setAttribute("castRolledAllOnes", String.valueOf(_castRolledAllOnes));
      mainElement.setAttribute("defenseRoll", String.valueOf(_defenseRoll));
      mainElement.setAttribute("resistanceRoll", String.valueOf(_resistanceRoll));
      mainElement.setAttribute("castingEffectiveness", String.valueOf(_castingEffectiveness));
      return mainElement;
   }

   public void readFromXMLObject(NamedNodeMap namedNodeMap) {
      Node targetIDNode = namedNodeMap.getNamedItem("targetID");
      if (targetIDNode != null) {
         String targetID = targetIDNode.getNodeValue();
         _targetID = Integer.parseInt(targetID);
      }
      Node casterIDNode = namedNodeMap.getNamedItem("casterID");
      if (casterIDNode != null) {
         String casterID = casterIDNode.getNodeValue();
         _casterID = Integer.parseInt(casterID);
      }
      _power                     = Byte.parseByte(namedNodeMap.getNamedItem("power").getNodeValue());
      _maintainedThisTurn        = Boolean.parseBoolean(namedNodeMap.getNamedItem("maintainedThisTurn").getNodeValue());
      _isInateSpell              = Boolean.parseBoolean(namedNodeMap.getNamedItem("isInateSpell").getNodeValue());
      _incantationRoundsRequired = Byte.parseByte(namedNodeMap.getNamedItem("incantationRoundsRequired").getNodeValue());
      _defenseTN                 = Byte.parseByte(namedNodeMap.getNamedItem("defenseTN").getNodeValue());
      _castRoll                  = Byte.parseByte(namedNodeMap.getNamedItem("castRoll").getNodeValue());
      _castRolledAllOnes         = Boolean.parseBoolean(namedNodeMap.getNamedItem("castRolledAllOnes").getNodeValue());
      _defenseRoll               = Byte.parseByte(namedNodeMap.getNamedItem("defenseRoll").getNodeValue());
      _resistanceRoll            = Byte.parseByte(namedNodeMap.getNamedItem("resistanceRoll").getNodeValue());
      _castingEffectiveness      = Byte.parseByte(namedNodeMap.getNamedItem("castingEffectiveness").getNodeValue());
   }

   public static Spell serializeFromXmlObject(Node mainElement) {

      if (!mainElement.getNodeName().equals("Spell")) {
         return null;
      }
      NamedNodeMap namedNodeMap = mainElement.getAttributes();
      if (namedNodeMap == null) {
         return null;
      }
      String name = namedNodeMap.getNamedItem("Name").getNodeValue();
      Spell spell = MageSpell.getSpell(name);
      if (spell == null) {
         // This might be a priest spell, since getSpell only returns Mage spells,
         // since they are the only kind of spell that can be completely defined just by name
         String deity = namedNodeMap.getNamedItem("deity").getNodeValue();
         if ((deity != null) && (deity.length() > 0)) {
            String affinityLevel = namedNodeMap.getNamedItem("affinityLevel").getNodeValue();
            if (affinityLevel != null) {
               byte aff = Byte.parseByte(affinityLevel);
               List<PriestSpell> priestSpells = PriestSpell.getSpellsForDeity(deity, aff, false);
               for (PriestSpell prSpell : priestSpells) {
                  if (prSpell.getName().equals(name) && (prSpell.getAffinity() == aff)) {
                     spell = prSpell;
                     break;
                  }
               }
            }
         }
      }
      if (spell != null) {
         spell.readFromXMLObject(namedNodeMap);
      }
      return spell;
   }

   public void setCasterAndTargetFromIDs(List<Character> combatants) {
      for (Character character : combatants) {
         if (_casterID == character._uniqueID) {
            _caster = character;
         }
         if (_targetID == character._uniqueID) {
            _target = character;
         }
      }
   }

   /**
    * This method returns 'null' if the target can be targeted.
    * If the target can not be targeted, this method returns the
    * reason that the target can not be targeted.
    * @param castor
    * @param target
    * @return
    */
   public String canTarget(Character castor, Character target) {
      return castor.canTarget(target, getTargetType());
   }

   public boolean affectsMultipleTargets() {
      return false;
   }

   public boolean requiresTargetToCast() {
      if (!isBeneficial()) {
         return true;
      }
      TargetType targetType = getTargetType();
      return (targetType != TargetType.TARGET_OTHER_EVIL_FIGHTING) && (targetType != TargetType.TARGET_OTHER_GOOD_FIGHTING)
             && (targetType != TargetType.TARGET_AREA) && (targetType != TargetType.TARGET_OBJECT) && (targetType != TargetType.TARGET_NONE);
   }

}
