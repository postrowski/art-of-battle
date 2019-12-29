/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.healing;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellDetectPoison extends PriestSpell
{
   public static final String NAME = "Detect Poison";
   public SpellDetectPoison() {}

   public SpellDetectPoison(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to determine the source and type of any poisons within the area of affect." +
             " The more power put into the spell, the larger the area becomes, and the smaller the amount of poison become.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
