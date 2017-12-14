package ostrowski.combat.common.spells.priest.demonic;

import ostrowski.combat.common.Race;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.SpellSummonBeing;

public class SpellSummonMinorDemon extends SpellSummonBeing
{
   public static final String NAME = "Summon Minor Demon";
   public SpellSummonMinorDemon() {};
   public SpellSummonMinorDemon(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String summonedTypeName() {
      return Race.NAME_Minor_Demon;
   }
   @Override
   public int getCharPointsPerPowerPoint() {
      return 50;
   }
   @Override
   public String summonedTypeRaceName() {
      return Race.NAME_Minor_Demon;
   }


}
