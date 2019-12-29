package ostrowski.combat.common.spells.priest.offensive;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.SpellSummonBeing;

public class SpellSummonChampion extends SpellSummonBeing
{
   public static final String NAME = "Summon Champion";
   public SpellSummonChampion() {}

   public SpellSummonChampion(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String summonedTypeName() {
      return "champion";
   }
   @Override
   public int getCharPointsPerPowerPoint() {
      return 100;
   }
   @Override
   public String summonedTypeRaceName() {
      return _caster.getRace().getName();
   }

}
