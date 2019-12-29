package ostrowski.combat.common.spells.priest.information;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellLocatePerson extends PriestSpell
{
   public static final String NAME = "Locate Person";
   public SpellLocatePerson() {}

   public SpellLocatePerson(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      return "The '"+getName()+"' spell allows the caster to determine the direction and distance to specified person." +
      " The more power put into the spell, the larger the searchable area becomes, and greater the odds of finding the person, even if magically or physically hidden." +
      " The person may be specified by any particular characteristics from name to description." +
      " Having physical artifact related to the target person (such as a lock of hair, or an old shoe) greatly increases the range and accuracy of this spell." +
      " If more than one person exists that matches the given criteria (i.e. 'John Smith'), then the closest one will be located.";
   }


}
