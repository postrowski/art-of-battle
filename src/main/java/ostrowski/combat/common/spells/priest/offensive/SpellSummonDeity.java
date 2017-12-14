package ostrowski.combat.common.spells.priest.offensive;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellSummonDeity extends PriestSpell
{
   public static final String NAME = "Summon Deity";
   public SpellSummonDeity() {};
   public SpellSummonDeity(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      return "The '" + getName() + "' spell brings a Deity to aid the priest." +
             " The caster must cast this spell at his or her full power." +
             " If the caster does not have good reason for summoning the Deity, the caster will be severely punished by the Deity." +
             " This punishment may include loss of affinity, or even being attacked by the Deity.";
   }

}
