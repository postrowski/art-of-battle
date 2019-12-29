package ostrowski.combat.common.spells.priest.information;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellDetectDanger extends PriestSpell
{
   public static final String NAME = "Detect Danger";
   public SpellDetectDanger() {}

   public SpellDetectDanger(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      return "The '" + getName() + "' spell detects the closest present danger. This could be a nearby trap, monster, cliff edge, poisoned food, etc."+
             " The more power put into the spell, the greater the range at which it can detect danger, and the finer the level of danger that can be detected." +
             "<br>At 1 point, only nearby deadly threats can be detected." +
             "<br>At 3 points, deadly threats can be detected moderately far away, while moderate threats can be detected nearby." +
             "<br>At 5 points, deadly threats can be detected fairly far away, moderate threats can be detected moderately far away, and minor threats can be detected nearby.";
   }

}
