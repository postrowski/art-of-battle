package ostrowski.combat.common.spells.priest.elemental;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;

public class SpellWalkThroughWalls extends ExpiringPriestSpell
{
   public static final String NAME = "Walk Through Walls";

   public SpellWalkThroughWalls() {
      this(null, 0);
   }

   public SpellWalkThroughWalls(Class<PriestElementalSpell> group, int affinity) {
      super(NAME, (short)5/*baseExpirationTimeInTurns*/, (short)5/*bonusTimeInTurnsPerPower*/, group, affinity);
   }
   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the subject of the spell to walk through natural stone or earthen walls at a rate of 1 hex (3 feet) per round." +
      		" It does not allow the subject to pass through wood, steel, or other manufactured walls or bars." +
      		" If the caster is still inside earth or stone when the spell expires, they die immediately.";
   }

}
