package ostrowski.combat.common.spells.priest.nature.weather;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellHurricane extends PriestSpell
{
   public static final String NAME = "Hurricane";
   public SpellHurricane() {}

   public SpellHurricane(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      return "The '"+getName()+"' spell is an extension of the 'summon storm' spell, with increase magnitude." +
             " A low-power (1-2 point) '"+getName()+"' spell is similar in effect to a high power 'Summon Storm' spell."+
             " A medium-power (3-5 point) '"+getName()+"' spell will cause a funnel cloud to touch down, causing massive damage nearby structures, and throwing unsecured objects or people about wildly." +
             " A High-power (6+ point) '"+getName()+"' spell will cause a multiple large funnel clouds to touch down, causing total devastation to nearby structure, and killing almost all nearby creatures.";
   }

}
