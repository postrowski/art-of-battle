package ostrowski.combat.common.spells.priest.demonic;

import ostrowski.combat.common.Race;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.SpellSummonBeing;

public class SpellSummonMajorDemon extends SpellSummonBeing
{
   public static final String NAME = "Summon Major Demon";
   public SpellSummonMajorDemon() {}

   public SpellSummonMajorDemon(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String summonedTypeName() {
      return Race.NAME_Major_Demon;
   }
   @Override
   public int getCharPointsPerPowerPoint() {
      return 200;
   }
   @Override
   public String summonedTypeRaceName() {
      return Race.NAME_Major_Demon;
   }

}
