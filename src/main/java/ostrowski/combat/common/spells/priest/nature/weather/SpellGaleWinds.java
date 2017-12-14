package ostrowski.combat.common.spells.priest.nature.weather;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellGaleWinds extends PriestSpell
{
   public static final String NAME = "Gale Winds";
   public SpellGaleWinds() {};
   public SpellGaleWinds(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      return "The '"+getName()+"' spell causes strong winds to blow around the caster." +
             " The caster chooses the strength of the winds, however, stronger winds will not last as long." +
             " The more power put into the spell, the stronger the winds may be, or the longer they may last." +
             " The GM should determine the extent of the winds, based on the effective power used to cast, and the desired strength.";
   }

}
