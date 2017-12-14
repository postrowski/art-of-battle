package ostrowski.combat.common.spells.priest.elemental;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;

public class SpellBreatheWater extends ExpiringPriestSpell
{
   public static final String NAME = "Breathe Water";

   public SpellBreatheWater() {
      this(null, 0);
   }

   public SpellBreatheWater(Class<PriestElementalSpell> group, int affinity) {
      super(NAME, (short)10/*baseExpirationTimeInTurns*/, (short)5/*bonusTimeInTurnsPerPower*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTarget().getName() + " is able to breathe underwater.";
      }
      return " can breathe underwater.";
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell makes the subject able to directly breathe water."
               + " The water must contain oxygen, which almost all water does, but exceptions do exist.";
   }

   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE;
   }


}
