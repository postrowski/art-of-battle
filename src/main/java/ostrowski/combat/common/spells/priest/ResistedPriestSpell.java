/*
 * Created on May 18, 2007
 *
 */
package ostrowski.combat.common.spells.priest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.spells.IResistedSpell;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestDefense;
import ostrowski.combat.server.Battle;
import ostrowski.combat.server.Configuration;

public abstract class ResistedPriestSpell extends ExpiringPriestSpell implements IResistedSpell
{
   protected Attribute _resistedAtt  = null;
   protected byte _resistedActions = 1;

   protected ResistedPriestSpell() {}
   protected ResistedPriestSpell(String name, Attribute resistedAtt, byte resistedActions, boolean expires,
                                 Class< ? extends IPriestGroup> group, int affinity) {
      this(name, resistedAtt, resistedActions,
           (short)(expires ? 10 : 0)/*baseExpirationTimeInTurns*/,
           (short)(expires ? 2 : 0)/*bonusTimeInTurnsPerPower*/,
           group, affinity);
   }
   protected ResistedPriestSpell(String name, Attribute resistedAtt, byte resistedActions, short baseExpirationTimeInTurns, short bonusTimeInTurnsPerPower,
                                 Class< ? extends IPriestGroup> group, int affinity) {
      super(name, baseExpirationTimeInTurns,bonusTimeInTurnsPerPower, group, affinity);
      _resistedAtt = resistedAtt;
      _resistedActions = resistedActions;
   }

   @Override
   public short getMaxRange(Character caster) {
      if (this instanceof IRangedSpell) {
         return super.getMaxRange(caster);
      }
      return 100;
   }
   /**
    * This method is called to determine if the defender can dodge, block, or parry this spell.
    * Resisted spells are Resistable, but not defendable.
    */
   @Override
   public boolean isDefendable() {
      return false;
   }

   /**
    * @param target
    * @return the targets level for the attribute resisted by this spell
    */
   @Override
   public byte getResistanceAttribute(Character target) {
      return target.getAttributeLevel(_resistedAtt);
   }
   @Override
   public byte getResistanceActions() {
      return _resistedActions;
   }
   @Override
   public DiceSet getResistanceDice(Character target) {
      byte resistanceAttrLevel = getResistanceAttribute(target);
      byte resistanceActions = getResistanceActions();
      if (Configuration._useExtendedDice) {
         return Rules.getDice(resistanceAttrLevel, resistanceActions, _resistedAtt/*attribute*/);
      }
      byte bonus = (byte) ((resistanceActions -2) * 5);
      resistanceAttrLevel += bonus;
      if (Configuration.useSimpleDice()) {
         return new DiceSet(resistanceAttrLevel/*d1*/, 0/*d4*/, 0/*d6*/, 0/*d8*/, 1/*d10*/, 0/*d12*/, 0/*d20*/, 0/*dBell*/, 1.0/*multiplier*/);
      }
      if (Configuration.useBellCurveDice()) {
         return new DiceSet(resistanceAttrLevel/*d1*/, 0/*d4*/, 0/*d6*/, 0/*d8*/, 0/*d10*/, 0/*d12*/, 0/*d20*/, 1/*dBell*/, 1.0/*multiplier*/);
      }
      DebugBreak.debugBreak("invalid Configuration");
      return new DiceSet(resistanceAttrLevel/*d1*/, 0/*d4*/, 0/*d6*/, 0/*d8*/, 0/*d10*/, 0/*d12*/, 0/*d20*/, 1/*dBell*/, 1.0/*multiplier*/);
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OTHER_FIGHTING;
   }

   @Override
   public String getResistanceAttributeName() {
      return _resistedAtt.shortName;
   }

   @Override
   public byte getCastingPower(RequestAction attack, short distanceInHexes, RANGE range,
                               Battle battle, StringBuilder sbDescription) {
      byte effectivePower = resolveEffectivePower(sbDescription, distanceInHexes, range);
      if (sbDescription != null) {
         if (effectivePower > 0) {
            if (Configuration.useExtendedDice()) {
               sbDescription.append(getCasterName()).append("'s effective power of ").append(effectivePower);
               sbDescription.append(" uses ").append(describeDiceForPower(effectivePower));
            }
            else {
               sbDescription.append(getCasterName()).append("'s ");
               sbDescription.append(_name);
               sbDescription.append(" spell has an effective power of ").append(effectivePower);
            }
         }
         else {
            sbDescription.append("<br/>Since the casting has no effective power, the spell fails.");
         }
      }
      return effectivePower;
   }

   @Override
   protected DiceSet getCastDice(RequestAction attack, byte distanceModifiedTN, byte distanceModifiedPower, short distanceInHexes, RANGE range, Battle battle, StringBuilder sbDescription) {
      // attack (cast) information:
      return getCastDice(distanceModifiedPower);
   }

   @Override
   protected void copyDataFrom(Spell source) {
      super.copyDataFrom(source);
      if (source instanceof ResistedPriestSpell) {
         ResistedPriestSpell resistedSource = (ResistedPriestSpell) source;
         _resistedAtt  = resistedSource._resistedAtt;
         _resistedActions = resistedSource._resistedActions;
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      super.serializeToStream(out);
      try {
         writeToStream(_resistedAtt.value, out);
         writeToStream(_resistedActions, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      super.serializeFromStream(in);
      try {
         _resistedAtt  = Attribute.getByValue(readByte(in));
         _resistedActions = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public Element getXMLObject(Document parentDoc, String newLine) {
      Element node = super.getXMLObject(parentDoc, newLine);
      node.setAttribute("resistedAtt", String.valueOf(_resistedAtt.value));
      node.setAttribute("resistedActions", String.valueOf(_resistedActions));
      return node;
   }
   @Override
   public void readFromXMLObject(NamedNodeMap namedNodeMap) {
      super.readFromXMLObject(namedNodeMap);
      Node node = namedNodeMap.getNamedItem("resistedAtt");
      if (node != null) {
         _resistedAtt  = Attribute.getByValue(Byte.parseByte(node.getNodeValue()));
      }
      node = namedNodeMap.getNamedItem("resistedActions");
      if (node != null) {
         _resistedActions  = Byte.parseByte(node.getNodeValue());
      }
   }
   @Override
   protected int resolveCast(RequestAction attack, byte rangeAdjustedCastingTN, byte castingPower, short distanceInHexes, RANGE range, Battle battle,
                           StringBuilder sbDescription) {
      _castRolledAllOnes = false;
      return 0;
   }

   @Override
   protected byte getDefenseTn(RequestAction attack, RequestDefense defense, short distanceInHexes, RANGE range,
                               Battle battle, StringBuilder sbDescription) {
      byte resistanceTN = getResistanceTN(getCaster());
      byte effectivePower = resolveEffectivePower(sbDescription, distanceInHexes, range);
      byte powerBonusToTN = (byte) (effectivePower * getResistanceTNPenaltyPerEffectivePower());
      sbDescription.append(" Has a base TN of ").append(resistanceTN);
      sbDescription.append(" plus ").append(powerBonusToTN);
      sbDescription.append(" for the effective power of ").append(effectivePower);
      resistanceTN += powerBonusToTN;
      sbDescription.append(", resulting in net TN of ").append(resistanceTN);
      return resistanceTN;
   }

   public byte getResistanceTN() {
      // as the resistance actions count goes up, its easier to resist.
      // This has the same effect as the resistance TN going down as the actions goes up.
      return (byte) (5 - (this.getResistanceActions() * 5));
   }
   public byte getResistanceTN(Character caster) {
      byte affinity = caster.getAffinity(this.getDeity());
      byte att = caster.getAttributeLevel(this.getCastingAttribute());
      return (byte) ( ((affinity + att)/2)  + getResistanceTN());
   }
   public String describeResistance(Character caster) {
      StringBuilder description = new StringBuilder();
      description.append(getResistanceAttributeName());
      if (this.getResistanceActions() == 1) {
         description.append("-5");
      }
      else if (this.getResistanceActions() == 3) {
         description.append("+5");
      }

      return description.toString();
   }
   public String describeResistanceTN() {
      StringBuilder description = new StringBuilder();
      byte adj = getResistanceTN();
      adj = (byte) (-5 - adj);
      if (adj > 0) {
         description.append(" +").append(adj);
      }
      if (adj < 0) {
         description.append(" ").append(adj);
      }
      return description.toString();
   }
   protected byte getResistanceTNPenaltyPerEffectivePower() {
      return 3;
   }
}
