/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.server.Arena;

public class SpellStrength extends MageSpell
{
   public static final String NAME = "Strength";
   public SpellStrength() {
      super(NAME, new Class[] {SpellHarden.class},
            new MageCollege[] {MageCollege.ENERGY, MageCollege.PROTECTION});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTargetName() + "'s STR increases by " + getPower() + " points.";
      }
      return " (STR increased by " + getPower() + " points.)";
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell increases the subject's STR by an amount equal to the power put into the spell.";
   }
   @Override
   public Boolean isCastInBattle() {
      return true;
   }

   @Override
   public void applyEffects(Arena arena) {
      getTarget().setAttribute(Attribute.Strength, (byte) (getTarget().getAttributeLevel(Attribute.Strength) + getPower()), false/*containInLimits*/);
   }
   @Override
   public void removeEffects(Arena arena) {
      getTarget().setAttribute(Attribute.Strength, (byte) (getTarget().getAttributeLevel(Attribute.Strength) - getPower()), false/*containInLimits*/);
   }

   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE_FIGHTING;
   }
}
