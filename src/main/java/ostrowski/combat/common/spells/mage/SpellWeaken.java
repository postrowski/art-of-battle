/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.server.Arena;

public class SpellWeaken extends ResistedMageSpell
{
   public static final String NAME = "Weaken";
   public SpellWeaken() {
      super(NAME, Attribute.Intelligence, (byte) 1/*resistedActions*/, true/*expires*/,
            new Class[] {SpellSuggestion.class},
            new MageCollege[] {MageCollege.ENERGY, MageCollege.ENCHANTMENT});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTargetName() + "'s STR decreases by " + getPower() + " points.";
      }
      return " (STR decreased by " + getPower() + " points.)";
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell decreases the subject's STR by an amount equal to the power put into the spell." +
             " This will affect the damage done with weapons, and may put the subject into a higher encumbrance level." +
             " Subject that use bows made for their original strength, will find that they are no longer strong enough to draw their own bow!";
   }
   @Override
   public Boolean isCastInBattle() {
      return true;
   }

   @Override
   public void applyEffects(Arena arena) {
      getTarget().setAttribute(Attribute.Strength, (byte) (getTarget().getAttributeLevel(Attribute.Strength) - getPower()), false/*containInLimits*/);
   }

   @Override
   public void removeEffects(Arena arena) {
      getTarget().setAttribute(Attribute.Strength, (byte) (getTarget().getAttributeLevel(Attribute.Strength) + getPower()), false/*containInLimits*/);
   }
}
