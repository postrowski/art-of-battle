package ostrowski.combat.common.spells.priest.information;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellDeterminePath extends PriestSpell
{
   public static final String NAME = "Determine Path";
   public SpellDeterminePath() {}

   public SpellDeterminePath(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      return "The '"+getName()+"' spell allows the caster to ask for divine assistance as to which of a set of choices the caster should take." +
      " The result will be what the caster's Deity would choose if the Deity were making that decision themself."+
      " The spell can be applied to many many things, from 'which door should I open', to 'should we attack from the north, south, or east'." +
      " The Deity's response will be dependent upon what knowledge the Deity has of the particular situation." +
      " The more power put into the spell, the more information the Deity will have to make a good decision." +
      " But even the highest-power spell may return uncertain information if the Deity has no way of knowing the outcomes such as 'should I pick heads or tails on the coin flip?'" +
      " The Deity's response will always be aligned with what the Deity would want the players to do, over what the players would choose to do on their own.";
   }

}
