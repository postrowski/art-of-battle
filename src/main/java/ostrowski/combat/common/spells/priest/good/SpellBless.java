package ostrowski.combat.common.spells.priest.good;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;
import ostrowski.combat.common.spells.priest.IPriestGroup;

public class SpellBless extends ExpiringPriestSpell implements ICastInBattle
{
   public static final String NAME = "Bless";
   public SpellBless() {
      this(null, 0);
   }
   public SpellBless(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, (short)25, (short)5, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTargetName() + " is now blessed, and all rolls will be at a +" + getEffectivePower();
      }
      return getTargetName() + " is blessed, and all rolls are at a +" + getEffectivePower();
   }

   @Override
   public DiceSet adjustDieRoll(DiceSet dice, RollType rollType, Object target) {
      if ((rollType == RollType.ATTACK_TO_HIT)     ||
          (rollType == RollType.BERSERK_RECOVERY)  ||
          (rollType == RollType.BERSERK_RESISTANCE)||
          (rollType == RollType.MAGIC_RESISTANCE)  ||
          (rollType == RollType.SPELL_CASTING))
      {
         return dice.addBonus(getEffectivePower());
      }
      return dice;
   }

   @Override
   public DiceSet adjustResistanceRoll(DiceSet dice) {
      return dice.addBonus(getEffectivePower());
   }

   @Override
   public String describeSpell() {
      return "The '" + getName() +
             "' spell increases the subject's die rolls by 1 point for every effective power point of the spell."+
             " The casting priest may not cast on himself, but may cast it upon other priests.<br/>" +
             " This applies to the following die rolls:"+
             "<ul>"+
               "<li>Attack to-hit and spell casting rolls." +
               "<li>Resistance rolls." +
               "<li>IQ rolls to avoid becoming, or recover from being berserk.</ul>" +
             "It does <b>not</b> apply to the following die rolls:"+
             "<ul>"+
               "<li>Damage rolls." +
               "<li>Toughness rolls to reduce pain." +
               "<li>Rolls for initiative."+
               "<li>Rolls used to determine random location (such as which arm is hit)." +
               "<li>Any roll in which a lower number is a better thing (such as the 'call lightning' spell)." +
             "</ul>";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OTHER_FIGHTING;
   }
   @Override
   public boolean isBeneficial() {
      return true;
   }

}