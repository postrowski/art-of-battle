package ostrowski.combat.common.spells.priest.nature.plant;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellGrowPlant extends PriestSpell
{
   public static final String NAME = "Grow Plant";
   public SpellGrowPlant() {};
   public SpellGrowPlant(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      return "The '"+getName()+"' spell causes the subject plant or plants to grow." +
      " Every point of effective power put into the spell causes the plant(s) to double in size." +
      " If the plant is already at its full size, then each point of effective power only causes the plant " +
      "to increase its size by an additional 25%." +
      " The effects of this spell are permanent.";
   }

}
