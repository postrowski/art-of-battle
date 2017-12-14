package ostrowski.combat.common.spells.priest.good;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.SpellBanish;

public class SpellBanishEvil extends SpellBanish
{
   public static final String NAME = "Banish Evil";
   public SpellBanishEvil() {
   }
   public SpellBanishEvil(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String getTargetTypeName() {
      return "an evil or unholy alignment.";
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OTHER_EVIL_FIGHTING;
   }
}
