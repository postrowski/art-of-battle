package ostrowski.combat.common.spells.priest;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.server.Arena;

public abstract class SpellBanish extends ResistedPriestSpell implements IRangedSpell, ICastInBattle
{
   private int painAmount;

   public SpellBanish() {
   }

   public SpellBanish(String name, Class< ? extends IPriestGroup> group, int affinity) {
      super(name, Attribute.Health, (byte) 2/*resistedActions*/, true/*expires*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      byte damage = getPainAmount();
      return getTargetName() + " is affected by the '" + getName() + "' spell, resulting in a penalty of " + damage + ".";
   }

   @Override
   public DiceSet adjustDieRoll(DiceSet dice, RollType rollType, Object target) {
      if ((rollType == RollType.ATTACK_TO_HIT) || (rollType == RollType.BERSERK_RECOVERY) || (rollType == RollType.BERSERK_RESISTANCE)
          || (rollType == RollType.MAGIC_RESISTANCE) || (rollType == RollType.SPELL_CASTING)) {
         return dice.addBonus(-getPainAmount());
      }
      return dice;
   }

   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell affects any creature that is of " + getTargetTypeName()
             + " If the subject of the spell fails to resist the spell, they suffer one point of pain"
             + " for every point that they missed their resistance by, multiplied by the effective power of the spell."
             + " The pain incurred from this spell can never put the subject's total pain over 9 points,"
             + " so the subject will not collapse in pain, and can always run away."
             + " As the subject gets further away from the caster, the pain is reduced as the effective power of the spell goes down, due to range."
             + " The pain caused by this spell does not recover from Toughness roll like normal pain does."
             + " Only moving away from the casting will reduce this penalty.";
   }

   abstract public String getTargetTypeName();

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OTHER_EVIL_FIGHTING;
   }

   @Override
   public boolean affectsMultipleTargets() {
      return true;
   }

   @Override
   public Wound setResults(int excessSuccess, boolean success, boolean effective, int castRoll, byte skill, Arena arena) {
      // The pain from this spell is based upon
      painAmount = excessSuccess * getPower();
      return super.setResults(excessSuccess, success, effective, castRoll, skill, arena);
   }

   public byte getPainAmount() {
      byte curPain = getTarget().getPainPenalty(false/*accountForBerserking*/);
      return (byte) (Math.min((9 - curPain), painAmount));
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

   @Override
   public boolean requiresTargetToCast() {
      return false;
   }

}
