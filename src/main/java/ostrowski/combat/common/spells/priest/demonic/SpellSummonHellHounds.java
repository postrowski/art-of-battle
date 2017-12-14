package ostrowski.combat.common.spells.priest.demonic;

import ostrowski.combat.common.Race;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.SpellSummonBeing;

public class SpellSummonHellHounds extends SpellSummonBeing
{
   public static final String NAME = "Summon Hell Hounds";
   public SpellSummonHellHounds() {};
   public SpellSummonHellHounds(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String summonedTypeName() {
      return Race.NAME_HellHound;
   }
   @Override
   public int getCharPointsPerPowerPoint() {
      return 50;
   }
   @Override
   public String summonedTypeRaceName() {
      return Race.NAME_HellHound;
   }

}
