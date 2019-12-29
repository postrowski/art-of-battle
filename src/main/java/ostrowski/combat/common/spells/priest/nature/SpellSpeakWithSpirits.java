package ostrowski.combat.common.spells.priest.nature;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellSpeakWithSpirits extends PriestSpell
{
   public static final String NAME = "Speak with spirits";
   public SpellSpeakWithSpirits() {}

   public SpellSpeakWithSpirits(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      return "The '"+getName()+"' spell allows the caster to speak with nearby spirits." +
             " The more effect power in the spell, the greater the amount of information the spirits can divulge." +
             " Spirits that have been dead for a long time, or that were never alive require more power to be contacted." +
             " Just because the caster contacts the spirit does not mean that the spirit will co-operate or be truthful." +
             " Not all spirits want to be contacted, and speaking with some of these spirits can be dangerous.";
   }

}
