package ostrowski.combat.common.spells.priest.evil;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Advantage;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;

public class SpellMassFear extends ResistedPriestSpell implements IRangedSpell, ICastInBattle
{
   public static final String NAME = "Mass Fear";

   public SpellMassFear() {
   }
   public SpellMassFear(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, Attribute.Intelligence/*resistedAtt*/, (byte)2 /*resistedActions*/, (short)10/*baseExpirationTimeInTurns*/, (short)10/*bonusTimeInTurnsPerAction*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTargetName() + " is now in fear of " + getCasterName() + ", and all rolls to attack " + caster.getHimHer() + " will be at a " + getPenalty();
      }
      return getTargetName() + " is fearful of " + getCasterName() + ", and all rolls against " + caster.getHimHer() + " are at a " + getPenalty();
   }

   public byte getPenalty() {
      Advantage divineAff = caster.getAdvantage(Advantage.DIVINE_AFFINITY_ + getDeity());
      if (divineAff == null && isInate()) {
         return (byte) (-this.getAffinity());
      }
      if (divineAff == null) {
         DebugBreak.debugBreak();
         return 0;
      }
      // advantages have a 0-base index, so a level of 0 means 1 point of affinity
      return (byte) (-divineAff.getLevel() - 1);
   }
   @Override
   public DiceSet adjustDieRoll(DiceSet dice, RollType rollType, Object target) {
      if ((rollType == RollType.ATTACK_TO_HIT)     ||
          (rollType == RollType.SPELL_CASTING))
      {
         if (target == getCaster()) {
            return dice.addBonus(getPenalty());
         }
      }
      return dice;
   }

   @Override
   public DiceSet adjustResistanceRoll(DiceSet dice) {
      return dice.addBonus(getPenalty());
   }

   @Override
   public String describeSpell() {
      return "The '" + getName() +
             "' spell puts the subject into a state of fear regarding the caster."+
             " This reduces the subject's die rolls by 1 point for every level of divine affinity of the caster, for any action taken against the caster."+
             " This applies to the following die rolls:"+
             "<ul>"+
               "<li>Attack to-hit rolls." +
               "<li>Spell casting rolls." +
            "</ul>" +
             "It does <b>not</b> apply to the following die rolls:"+
             "<ul>"+
               "<li>Damage rolls." +
               "<li>Resistance rolls." +
             "</ul>";
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE_FIGHTING;
   }
   @Override
   public boolean requiresTargetToCast() {
      return false;
   }

   @Override
   public boolean affectsMultipleTargets() {
      return true;
   }

   @Override
   public boolean isBeneficial() {
      return false;
   }

   @Override
   public short getRangeBase() {
      return 5;
   }
   @Override
   public byte getRangeDefenseAdjustmentToPD(short distanceInHexes) {
      return 0;
   }
   @Override
   public byte getRangeDefenseAdjustmentPerAction(short distanceInHexes) {
      return 0;
   }

}
