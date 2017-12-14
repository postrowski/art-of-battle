package ostrowski.combat.common.spells.priest.good;

import ostrowski.combat.common.spells.priest.IPriestGroup;

public class SpellCharmMonster extends SpellCharmPerson
{
   @SuppressWarnings("hiding")
   public static final String NAME = "Charm Monster";
   public SpellCharmMonster() {
   }
   public SpellCharmMonster(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANIMAL_FIGHTING;
   }

   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell causes the subject of the spell to immediately regard the caster as a close friend." +
               " The subject does not forget who its previous allies are, but will do his or her best to prevent anyone from attacking the caster." +
               " The subject will fight anyone to defend the caster of the spell." +
               " The subject has no allegiance to the caster's allies." +
               " If the caster harms the subject, the spell is immediately broken.";
   }

}
