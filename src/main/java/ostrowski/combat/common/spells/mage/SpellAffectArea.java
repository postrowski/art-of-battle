/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;

public class SpellAffectArea extends MageSpell
{
   public static final String NAME = "Affect Area";
   public SpellAffectArea() {
      super(NAME, new Class[] {}, new MageCollege[] {MageCollege.EVOCATION});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows other spells to be applied to an area. The spell has no effect when cast by itself.";
   }

}
