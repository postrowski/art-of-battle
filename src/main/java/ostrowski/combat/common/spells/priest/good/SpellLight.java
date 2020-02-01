/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.good;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellLight extends PriestSpell
{
   public static final String NAME = "Light";
   public SpellLight() {}

   public SpellLight(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell causes the object upon which it is cast to glow." +
             " A 1 power point light spell will last for 12 minutes, with brightness equal to 1 candle." +
             " Each additional point of power can either increase the duration by a factor of 5," +
             " or it can increase the brightness by a factor 10.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OBJECT;
   }

}
