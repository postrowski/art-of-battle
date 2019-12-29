/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.good;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.server.Arena;

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
             " The more power put into the spell, the brighter the light, and the longer the spell lasts.";
   }

   @Override
   public void applyEffects(Arena arena) {
   }
   @Override
   public void removeEffects(Arena arena) {
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OBJECT;
   }

}
