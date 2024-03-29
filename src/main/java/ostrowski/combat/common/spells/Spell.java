package ostrowski.combat.common.spells;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.*;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.mage.MageSpell;
import ostrowski.combat.common.spells.mage.MageSpells;
import ostrowski.combat.common.spells.priest.PriestMissileSpell;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestDefense;
import ostrowski.combat.protocol.request.RequestLocation;
import ostrowski.combat.server.*;
import ostrowski.protocol.SerializableObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author pnostrow
 *
 */
public abstract class Spell extends SerializableObject implements Enums, Cloneable, Comparable<Spell>
{
   protected String    name;
   // These attributes are serialized, because they may differ from one 'fire' spell to another 'fire' spell:
   protected byte      power                     = 0;
   protected byte      level                     = 0;
   protected Character caster                    = null;
   protected Character target                    = null;
   public    int       casterID                  = -1;
   public    int       targetID                  = -1;
   private   boolean   maintainedThisTurn        = true;
   private   boolean   isInateSpell              = false;
   private   byte      incantationRoundsRequired = 0;
   private   byte      defenseTN;
   private   int       castRoll;
   protected boolean   castRolledAllOnes         = false;
   private   int       defenseRoll;
   private   int       resistanceRoll;
   protected byte      castingEffectiveness      = EFFECTIVENESS_FAILURE;
   protected byte      excessSuccess;

   public Spell() {
   }

   public Spell(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public byte getPower() {
      return power;
   }

   public void setPower(byte power) {
      this.power = power;
   }

   public byte getLevel() {
      return level;
   }

   public void setLevel(byte level) {
      this.level = level;
   }

   public Wound channelEnergy(byte additionalPower) {
      maintainedThisTurn = true;
      power += additionalPower;
      return null;
   }

   /**
    * Each turn, this method should be called on all active spells.
    */
   public void newTurn() {
      maintainedThisTurn = false;
   }

   public void completeSpell() {
      maintainedThisTurn = true;
   }

   public void maintainSpell() {
      maintainedThisTurn = true;
   }

   public void discardSpell() {
      maintainedThisTurn = false;
   }

   public void setIsInate(boolean isInate) {
      isInateSpell = isInate;
   }

   public boolean isInate() {
      return isInateSpell;
   }

   public boolean isMaintainedThisTurn() {
      return maintainedThisTurn;
   }

   public void beginIncantation() {
      incantationRoundsRequired = getIncantationTime();
   }

   public boolean incant() {
      maintainSpell();
      incantationRoundsRequired--;
      return (incantationRoundsRequired <= 0);
   }

   public byte getIncantationRoundsRequired() {
      return incantationRoundsRequired;
   }

   public abstract String describeEffects(Character defender, boolean firstTime);

   public abstract String describeSpell();

   public abstract byte getTN(Character caster);

   public static String getSpellGrimioreForHTML(Collection<? extends Spell> spells) {
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
         previousSpell = spell;
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
            return (short) Math.round(rangeBase * Rules.getRangeAdjusterForAdjustedStr(sizeAdjustedAttr));
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
      this.caster = caster;
      casterID = caster.uniqueID;
   }

   public Character getCaster() {
      return caster;
   }

   public String getCasterName() {
      if (caster == null) {
         return "";
      }
      return caster.getName();
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
         this.target = caster;
      }
      else {
         this.target = target;
      }
   }

   public Character getTarget() {
      return target;
   }

   public String getTargetName() {
      if (target == null) {
         return "";
      }
      return target.getName();
   }

   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
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

   public Wound setResults(int excessSuccess, boolean success, boolean effective, int castRoll, byte skill, Arena arena) {
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
      return MageSpell.generateHtmlTableMageSpells() +
             PriestSpell.generateHtmlTablePriestSpells();
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
// No need to serialize the name, the serialization key uniquely defined this spell
//         writeToStream(name, out);
         writeToStream(power, out);
         writeToStream(level, out);
         // resistedAtt, prerequisiteSpellNames & attributeMod don't need to be serialized,
         // because they are constant for a given spell (defined by its name).
         writeToStream(maintainedThisTurn, out);
         writeToStream(isInateSpell, out);
         writeToStream(incantationRoundsRequired, out);
         writeToStream(defenseTN, out);
         writeToStream(castRoll, out);
         writeToStream(castRolledAllOnes, out);
         writeToStream(defenseRoll, out);
         writeToStream(resistanceRoll, out);
         writeToStream(castingEffectiveness, out);

      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
      // No need to serialize the name, the serialization key uniquely defined this spell
//         String name = readString(in);
//         Spell spell = MageSpells.getSpell(name).clone();
//         this.copyDataFrom(spell);
         power = readByte(in);
         level = readByte(in);
         // resistedAtt, prerequisiteSpellNames & attributeMod don't need to be serialized,
         // because they are constant for a given spell (defined by its name).
         maintainedThisTurn = readBoolean(in);
         isInateSpell = readBoolean(in);
         incantationRoundsRequired = readByte(in);
         defenseTN = readByte(in);
         castRoll = readInt(in);
         castRolledAllOnes = readBoolean(in);
         defenseRoll = readInt(in);
         resistanceRoll = readInt(in);
         castingEffectiveness = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   protected void copyDataFrom(Spell source) {
      caster = source.caster;
      name = source.name;
      power = source.power;
      level = source.level;
      target = source.target;
      maintainedThisTurn = source.maintainedThisTurn;
      isInateSpell = source.isInateSpell;
      incantationRoundsRequired = source.incantationRoundsRequired;
      defenseTN = source.defenseTN;
      castRoll = source.castRoll;
      castRolledAllOnes = source.castRolledAllOnes;
      defenseRoll = source.defenseRoll;
      resistanceRoll = source.resistanceRoll;
      castingEffectiveness = source.castingEffectiveness;
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
      if (target != null) {
         sb.append(", target=").append(getTargetName());
      }
      return sb.toString();
   }

   public Wound modifyDamageDealt(Wound wound, Character attacker, Character defender, String attackingWeaponName, StringBuilder modificationsExplanation) {
      return wound;
   }

   public Wound modifyDamageReceived(Wound wound) {
      return wound;
   }

   public DiceSet getCastDice(byte actionsUsed, RANGE range) {
      Attribute att = getCastingAttribute();
      return Rules.getDice(getCaster().getAttributeLevel(att), actionsUsed, att/*attribute*/, RollType.SPELL_CASTING);
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
      byte spellTN = getTN(caster);
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

      castingEffectiveness = EFFECTIVENESS_FAILURE;
      byte castingPower;
      if (this instanceof ResistedPriestSpell) {
         // don't fill in the description here, cause it will be filled in in the call to getDefenseTn/resolveEffectivePower
         castingPower = getCastingPower(attack, distanceInHexes, range, battle, null/*sbDescription*/);
      } else {
         castingPower = getCastingPower(attack, distanceInHexes, range, battle, sbDescription);
      }
      if (this instanceof IAreaSpell) {
         RequestLocation locationSelection = attack.locationSelection;
         ArenaCoordinates coord = locationSelection.getAnswerCoordinates();
         ArenaLocation loc = battle.arena.getCombatMap().getLocation(coord);
         ((IAreaSpell) this).setTargetLocation(loc, battle.arena);
      }
      boolean missingTarget = false;
      if (getTargetType() == TargetType.TARGET_AREA) {
         if (((IAreaSpell) this).getTargetLocation() == null) {
            missingTarget = true;
         }
      } else if ((getTargetType() != TargetType.TARGET_NONE) && (target == null)) {
         missingTarget = true;
      }
      if (missingTarget) {
         sbDescription.append(getCasterName()).append("'s ").append(getName())
                      .append(" spell is wasted because ").append(getCasterName()).append(" has no target!");
      }
      else {
         if (!(this instanceof PriestSpell) || (castingPower > 0)) {
            defenseTN = getDefenseTn(attack, defense, distanceInHexes, range, battle, sbDescription);
            castRoll = resolveCast(attack, rangeAdjustedCastingTN, castingPower, distanceInHexes, range, battle, sbDescription);
            defenseRoll = getDefenseResult(defense, castingPower, battle, sbDescription);
            resistanceRoll = getResistanceResult(defense, distanceInHexes, battle, sbDescription);
            castingEffectiveness = getEffectiveness(attack, defense, distanceInHexes, castingTN, rangeAdjustedCastingTN, battle, sbDescription);
         }
      }
      if (castingEffectiveness >= 0) {
         if (this instanceof IMissileSpell) {
            IMissileSpell missileSpell = ((IMissileSpell) this);

            byte bonusDamage = castingEffectiveness;
            DiceSet damageDie = missileSpell.getDamageDice();
            String damageExplanation = missileSpell.explainDamage();
            battle.resolveDamage(caster, target, getName() + " spell", damageExplanation, missileSpell.getSpellDamageBase(), bonusDamage, damageDie,
                                 missileSpell.getDamageType(), getSpecialDamageModifier(), getSpecialDamageModifierExplanation(), sbDescription, wounds,
                                 excessSuccess, false/*isCharge*/);
         }
         String effects = describeEffects(target, true/*firstTime*/);
         if (effects != null) {
            sbDescription.append("<br/>").append(effects);
         }

         Character target = getTarget();
         if (target == null) {
            target = caster;
         }
         List<Spell> spellList = spells.computeIfAbsent(target, k -> new ArrayList<>());
         spellList.add(this);
      }
      String message = sbDescription.toString();
      battle.arena.sendMessageTextToAllClients(message, false/*popUp*/);
      battle.arena.sendMessageTextToParticipants(message, getCaster(), getTarget());

      return (castingEffectiveness >= 0);
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
      String rollMessage = caster.getName() + ", roll to cast your " + getName() + " spell.";
      int castRoll = adjCastDice.roll(true/*allowExplodes*/, getCaster(), RollType.SPELL_CASTING, rollMessage);
      castRolledAllOnes = adjCastDice.lastRollRolledAllOnes();
      if ((adjCastDice.getDiceCount() > 0) || (this.castRoll != 0)) {
         sbDescription.append("<br/>");
         sbDescription.append(caster.getName()).append(" rolls ").append(adjCastDice);
         sbDescription.append(", rolling ").append(adjCastDice.getLastDieRoll());
         sbDescription.append(", for a total of ").append(castRoll);
         if (!Configuration.useExtendedDice() && (this instanceof MageSpell)) {
            sbDescription.append("<table border=1>");
            sbDescription.append("<tr><td>").append(castRoll).append("</td><td>Die Roll.</td></tr>");

            byte adjSkillLevel = getAdjustedCastingSkillLevel(caster);
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
            sbDescription.append("<tr><td>").append(-woundsAndPain).append("</td><td>").append("pain and wounds</td></tr>");

            sbDescription.append("<tr><td>").append(castRoll).append("</td><td>roll result.</td></tr>");
            sbDescription.append("</table>");
         }
         else if (this instanceof PriestMissileSpell) {
            sbDescription.append("<table border=1>");
            sbDescription.append("<tr><td>").append(castRoll)
                         .append("</td><td>Die Roll.</td></tr>");

            byte skill = getCastingLevel();
            sbDescription.append("<tr><td>").append(skill)
                         .append("</td><td>Divine Aff. ").append("</td></tr>");
            castRoll += skill;

            Attribute attribute = Attribute.Dexterity;
            byte attrLevel = caster.getAttributeLevel(attribute);
            sbDescription.append("<tr><td>").append(attrLevel)
                         .append("</td><td>").append(attribute.shortName).append("</td></tr>");
            castRoll += attrLevel;

            sbDescription.append("<tr><td>").append(-woundsAndPain)
                         .append("</td><td>").append("caster's pain and wounds.</td></tr>");
            castRoll -= woundsAndPain;

            sbDescription.append("<tr><td>").append(castRoll)
                         .append("</td><td>roll result.</td></tr>");
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
      if (target != null) {
         distanceInHexes = Arena.getMinDistance(caster, target);
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
      return caster.getSpellSkill(getName());
   }

   protected String describeCastingAction(boolean isAdvance, int spellActions) {
      StringBuilder sbDescription = new StringBuilder();
      sbDescription.append("<span style=\"color:red\">");
      sbDescription.append(caster.getName());
      if (isAdvance) {
         sbDescription.append(" advances to cast a ");
      }
      else {
         sbDescription.append(" casts a ");
      }
      sbDescription.append(getPower()).append("-power '");
      sbDescription.append(getName()).append("' spell");
      if (target != null) {
         sbDescription.append(" on ");
         if (target == caster) {
            sbDescription.append("himself");
         }
         else {
            sbDescription.append(target.getName());
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
         return battle.resolveDefenses(target, defense, (byte) 0/*attackParryPenalty*/, distanceInHexes,
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
      byte resistanceAttributeLevel = resistedThis.getResistanceAttribute(target);
      DiceSet resistanceDice = resistedThis.getResistanceDice(target);
      resistanceDice = getTarget().adjustDieRoll(resistanceDice, RollType.MAGIC_RESISTANCE, null/*target*/);
      String rollMessage = getTarget() + ", roll to resist the effects of the " + this.getName() +
                           " spell, cast by " + getCasterName();
      int resistanceRoll = resistanceDice.roll(true/*allowExplodes*/, getTarget(),
                                               RollType.MAGIC_RESISTANCE, rollMessage);
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
//               sbDescription.append("�");
//            }
         }
      }

      sbDescription.append(" = ");
      sbDescription.append(resistanceDice).append(", rolling");
      sbDescription.append(resistanceDice.getLastDieRoll());
      if (magicResistanceAdv != null) {
         sbDescription.append(", plus ")
                      .append(magicResistanceBonus)
                      .append(" for having the 'Magic Resistance' advantage at level ")
                      .append(magicResistanceAdv.getLevel())
                      .append(".");
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
      if (distanceModifier != 0) {
         sbDescription.append("<table border=1>");
         sbDescription.append("<tr><td>").append(resistanceRoll).append("</td>");
         sbDescription.append("<td>resistance TN</td></tr>");

         sbDescription.append("<tr>");
         sbDescription.append("<td>");
         if (distanceModifier > 0) {
            sbDescription.append("+");
         }
         sbDescription.append(distanceModifier).append("</td>");
         sbDescription.append("<td>").append("distance modifier for being ").append(distanceInHexes).append(" yards away.</td>");
         sbDescription.append("</tr>");
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
      byte result = (byte) castRoll;
      if (!Configuration.useSimpleDice() && !Configuration.useBellCurveDice()) {
         result += skill;
      }

      sbDescription.append("<table border=1>");
      if (castRoll > 0) {
         if (this instanceof PriestSpell) {
            // Priest missile spell have already had the D.A. added in
            if (!(this instanceof PriestMissileSpell)) {
               sbDescription.append("<tr><td>").append(castRoll).append("</td><td>Dice roll</td></tr>");
               sbDescription.append("<tr><td>+").append(skill).append("</td><td>Divine Affinity</td></tr>");
               if (Configuration.useSimpleDice() || Configuration.useBellCurveDice()) {
                  result += skill;
               }
            }
         }
         else {
            if (!Configuration.useSimpleDice() && !Configuration.useBellCurveDice()) {
               sbDescription.append("<tr><td>").append(castRoll).append("</td><td>Dice roll</td></tr>");
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
            sbDescription.append("<tr><td><b>").append(defenseTN).append("</b></td>");
            sbDescription.append("<td>Defenders TN</td></tr>");
         }
         if (this instanceof IResistedSpell) {
            sbDescription.append("<tr><td><b>").append(resistanceRoll).append("</b></td>");
            sbDescription.append("<td>Resistance TN</td></tr>");
         }
      }
      excessSuccess = (byte) (result - castingTN);
      boolean success = (isInate() || (this instanceof PriestSpell) || (excessSuccess >= 0)) && !castRolledAllOnes;
      boolean effective = false;
      if (success) {
         // This instanceof check is replaced by isDefendable, with a check of validity in the
         // static initializer of Spell.
         //if ((this instanceof IMissileSpell) || (this instanceof SpellSpiderWeb)) {
         if (isDefendable()) {
            effective = (result >= defenseTN);
            excessSuccess = (byte) (result - defenseTN);
         }
         else if (this instanceof IResistedSpell) {
            if (this instanceof ResistedPriestSpell) {
               excessSuccess = (byte) (defenseTN - resistanceRoll);
               effective = (excessSuccess > 0);
            }
            else {
               excessSuccess = (byte) (result - resistanceRoll);
               effective = (excessSuccess >= 0);
            }
         }
         else {
            effective = (result >= rangeAdjustedCastingTN);
            excessSuccess = (byte) (result - rangeAdjustedCastingTN);
         }
      }

      Wound burn = setResults(excessSuccess, success, effective, castRoll, skill, battle.arena);
      castingEffectiveness = EFFECTIVENESS_SUCCESS;
      if (success) {
         if (this instanceof IMissileSpell) {
            if (effective) {
               byte bonusDamage = Rules.getDamageBonusForSkillExcess((byte) (result - defenseTN));
               sbDescription.append("<tr><td><b>").append(bonusDamage).append("</b></td>");
               sbDescription.append("<td><b>spell succeeded</b>, bonus damage</td></tr>");
               castingEffectiveness = bonusDamage;
            }
            else {
               sbDescription.append("<tr><td colspan=2><b>spell succeeded</b>, but missed the target</td></tr>");
               castingEffectiveness = EFFECTIVENESS_MISSED;
            }
         }
         else if (effective) {
            sbDescription.append("<tr><td colspan=2><b>spell succeeded</b></td></tr>");
            //_castingEffectiveness = (byte) (result - defenseTN);
            castingEffectiveness = excessSuccess;
         }
         else {
            if (this instanceof IResistedSpell) {
               sbDescription.append("<tr><td colspan=2><b>spell cast succesfully, but was resisted.</b></td></tr>");
               castingEffectiveness = EFFECTIVENESS_RESISTED;
            }
            else {
               sbDescription.append("<tr><td colspan=2><b>spell cast succesfully, but was ineffective.</b></td></tr>");
               castingEffectiveness = EFFECTIVENESS_NO_EFFECT;
            }
         }
      }
      else {
         if (burn != null) {
            sbDescription.append("<tr><td>").append(burn.getWounds());
            sbDescription.append("</td><td><b>Casting failure!</b>");
            if (castRolledAllOnes) {
               sbDescription.append("(all 1s rolled)");
            }
            sbDescription.append("  Spell burn level (");
            sbDescription.append(getPower()).append(" - ").append(caster.getMagicalAptitude());
            sbDescription.append(")</td></tr>");
         }
         else {
            sbDescription.append("<tr><td colspan=2><b>Spell failed</b>");
            if (castRolledAllOnes) {
               sbDescription.append("(all 1s rolled)");
            }
            sbDescription.append("</td></tr>");
         }
         castingEffectiveness = EFFECTIVENESS_FAILURE;
      }
      sbDescription.append("</table>");

      return castingEffectiveness;
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
    * @return true if the spell in the parameter is incompatible with this spell, false otherwise
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
      mainElement.setAttribute("Name", name);
      mainElement.setAttribute("power", String.valueOf(power));
      if (caster != null) {
         mainElement.setAttribute("casterID", String.valueOf(caster.uniqueID));
      }
      if (target != null) {
         mainElement.setAttribute("targetID", String.valueOf(target.uniqueID));
      }
      mainElement.setAttribute("maintainedThisTurn", String.valueOf(maintainedThisTurn));
      mainElement.setAttribute("isInateSpell", String.valueOf(isInateSpell));
      mainElement.setAttribute("incantationRoundsRequired", String.valueOf(incantationRoundsRequired));
      mainElement.setAttribute("defenseTN", String.valueOf(defenseTN));
      mainElement.setAttribute("castRoll", String.valueOf(castRoll));
      mainElement.setAttribute("castRolledAllOnes", String.valueOf(castRolledAllOnes));
      mainElement.setAttribute("defenseRoll", String.valueOf(defenseRoll));
      mainElement.setAttribute("resistanceRoll", String.valueOf(resistanceRoll));
      mainElement.setAttribute("castingEffectiveness", String.valueOf(castingEffectiveness));
      return mainElement;
   }

   public void readFromXMLObject(NamedNodeMap namedNodeMap) {
      Node targetIDNode = namedNodeMap.getNamedItem("targetID");
      if (targetIDNode != null) {
         String targetID = targetIDNode.getNodeValue();
         this.targetID = Integer.parseInt(targetID);
      }
      Node casterIDNode = namedNodeMap.getNamedItem("casterID");
      if (casterIDNode != null) {
         String casterID = casterIDNode.getNodeValue();
         this.casterID = Integer.parseInt(casterID);
      }
      power = Byte.parseByte(namedNodeMap.getNamedItem("power").getNodeValue());
      maintainedThisTurn = Boolean.parseBoolean(namedNodeMap.getNamedItem("maintainedThisTurn").getNodeValue());
      isInateSpell = Boolean.parseBoolean(namedNodeMap.getNamedItem("isInateSpell").getNodeValue());
      incantationRoundsRequired = Byte.parseByte(namedNodeMap.getNamedItem("incantationRoundsRequired").getNodeValue());
      defenseTN = Byte.parseByte(namedNodeMap.getNamedItem("defenseTN").getNodeValue());
      castRoll = Byte.parseByte(namedNodeMap.getNamedItem("castRoll").getNodeValue());
      castRolledAllOnes = Boolean.parseBoolean(namedNodeMap.getNamedItem("castRolledAllOnes").getNodeValue());
      defenseRoll = Byte.parseByte(namedNodeMap.getNamedItem("defenseRoll").getNodeValue());
      resistanceRoll = Byte.parseByte(namedNodeMap.getNamedItem("resistanceRoll").getNodeValue());
      castingEffectiveness = Byte.parseByte(namedNodeMap.getNamedItem("castingEffectiveness").getNodeValue());
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
      Spell spell = MageSpells.getSpell(name);
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
         if (casterID == character.uniqueID) {
            caster = character;
         }
         if (targetID == character.uniqueID) {
            target = character;
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
