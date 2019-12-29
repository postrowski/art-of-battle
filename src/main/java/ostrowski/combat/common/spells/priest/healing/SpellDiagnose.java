/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.healing;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellDiagnose extends PriestSpell
{
   public static final String NAME = "Diagnose";
   public SpellDiagnose() {}

   public SpellDiagnose(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to determine how severely anything is damaged." +
             " The more power put into the spell, the more accurate and extensive the details become.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
