package ostrowski.combat.common.spells.priest.elemental;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellControlElement extends PriestSpell
{
   public static final String NAME = "Control Element";

   public SpellControlElement() {
      this(null, 0);
   }

   public SpellControlElement(Class<PriestElementalSpell> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to move a targeted element (earth, air, fire or water)." +
      		" The amount of the element that can be moved, and how fast and how far the element can be moved depends upon the effective power of the spell.";
   }

}
