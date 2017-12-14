package ostrowski.combat.common.spells.priest.elemental;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;

public class SpellSwim extends ExpiringPriestSpell
{
   public static final String NAME = "Swim";

   public SpellSwim() {
      this(null, 0);
   }

   public SpellSwim(Class<PriestElementalSpell> group, int affinity) {
      super(NAME, (short)10/*baseExpirationTimeInTurns*/, (short)5/*bonusTimeInTurnsPerPower*/, group, affinity);
   }
   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTargetName() + " can now swim like a fish.";
      }
      return getTargetName() + " can swim well.";
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the spell's subject to move through water as quickly as they would if they were on land." +
      		" The spell does not allow the subject to breath underwater.";
   }

   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public Boolean isCastInBattle() {
      return true;
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE;
   }


}
