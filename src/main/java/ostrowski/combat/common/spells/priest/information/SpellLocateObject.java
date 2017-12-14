package ostrowski.combat.common.spells.priest.information;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellLocateObject extends PriestSpell
{
   public static final String NAME = "Locate Object";
   public SpellLocateObject() {};
   public SpellLocateObject(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      return "The '"+getName()+"' spell allows the caster to determine the direction and distance to closest object of the sought type." +
             " The more power put into the spell, the larger the searchable area becomes, and greater the odds of finding it, even if magically or physically hidden.";
   }

}
