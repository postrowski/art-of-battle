package ostrowski.combat.common.spells.priest.nature;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellSeekElement extends PriestSpell
{
   public static final String NAME = "Seek Element";
   public SpellSeekElement() {}

   public SpellSeekElement(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell() {
      return "The '"+getName()+"' spell allows the caster to determine the direction, distance and quantity to the nearest amount of any particular element (fire, water, air, or earth)." +
             " The more power put into the spell, the larger the searchable area becomes, and the smaller the amount of the element sought that can detected.";
   }

}
