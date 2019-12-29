package ostrowski.combat.common.spells.priest.nature.animal;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellDetectLife extends PriestSpell
{
   public static final String NAME = "Detect Life";
   public SpellDetectLife() {}

   public SpellDetectLife(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      return "The '"+getName()+"' spell allows the caster to determine the source and type of any life within the area of affect." +
             " The more power put into the spell, the larger the area becomes, and the smaller the amount of life that can detected.";
   }

}
