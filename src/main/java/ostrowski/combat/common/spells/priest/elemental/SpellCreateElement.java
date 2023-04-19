package ostrowski.combat.common.spells.priest.elemental;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellCreateElement extends PriestSpell
{
   public static final String NAME = "Create Element";

   public SpellCreateElement() {
      this(null, 0);
   }

   public SpellCreateElement(Class<PriestElementalSpell> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to create a base element." +
      		" The amount of the element created depends upon the power of the spell." +
      		" If fire is created, it will immediately be extinguished unless it has fuel to continue burning.";
   }

}
