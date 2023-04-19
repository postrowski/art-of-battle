package ostrowski.combat.common.spells.priest.evil;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.SpellBanish;

public class SpellBanishGood extends SpellBanish
{
   public static final String NAME = "Banish Good";
   public SpellBanishGood() {
   }
   public SpellBanishGood(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String getTargetTypeName() {
      return "a good or holy alignment.";
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OTHER_GOOD_FIGHTING;
   }
}
