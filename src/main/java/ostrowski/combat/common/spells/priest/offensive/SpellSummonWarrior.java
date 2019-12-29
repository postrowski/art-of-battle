package ostrowski.combat.common.spells.priest.offensive;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.SpellSummonBeing;

public class SpellSummonWarrior extends SpellSummonBeing
{
   public static final String NAME = "Summon Warrior";
   public SpellSummonWarrior() {}

   public SpellSummonWarrior(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String summonedTypeName() {
      return "warrior";
   }
   @Override
   public int getCharPointsPerPowerPoint() {
      return 50;
   }
   @Override
   public String summonedTypeRaceName() {
      return _caster.getRace().getName();
   }


   
}
