package ostrowski.combat.common.spells.priest.evil;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.SpellDestroy;

public class SpellDestroyGood extends SpellDestroy
{
   public static final String NAME = "Destroy Good";
   public SpellDestroyGood() {
   }
   public SpellDestroyGood(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OTHER_GOOD_FIGHTING;
   }
   @Override
   public String getTargetTypeName() {
      return "a good or holy alignment.";
   }

}
