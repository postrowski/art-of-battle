package ostrowski.combat.common.spells.priest.nature.weather;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellSummonStorm extends PriestSpell
{
   public static final String NAME = "Summon Storm";
   public SpellSummonStorm() {}

   public SpellSummonStorm(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      return "The '"+getName()+"' spell causes strong winds to blow around the caster, and rain or hail will fall." +
             " The effective power of the spell determines the severity of the storm, from a light rain, to a torrential downpour, possible with hail and or lightning.";
   }

}
