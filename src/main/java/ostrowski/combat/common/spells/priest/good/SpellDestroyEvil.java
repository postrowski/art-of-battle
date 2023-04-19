package ostrowski.combat.common.spells.priest.good;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.SpellDestroy;

public class SpellDestroyEvil extends SpellDestroy
{
   public static final String NAME = "Destroy Evil";
   public SpellDestroyEvil() {
   }
   public SpellDestroyEvil(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OTHER_EVIL_FIGHTING;
   }
   @Override
   public String getTargetTypeName() {
      return "an evil or unholy alignment.";
   }
   
}
