/*
 * Created on May 25, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;

public class SpellBlockAttack extends InstantaneousMageSpell
{
   public static final String NAME = "Block Attack";
   public SpellBlockAttack() {
      super(NAME, new Class[] {SpellCreateForce.class}, new MageCollege[] {MageCollege.ENERGY, MageCollege.PROTECTION});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell is used defensively when attacked by an enemy with a melee weapon, ranged weapon or missile spell." +
              " This spell increases the caster's TN by an amount equal to the caster's base skill level with the spell" +
              " (not adjusted by IQ), times the number of actions spent on the spell. To be effective in combat, this" +
              " spell must be memorized.";
   }

   @Override
   public byte getActiveDefensiveTN(byte actionsSpent, Character caster) {
      return (byte) (actionsSpent * caster.getSpellSkill(getName()));
   }
   @Override
   public boolean canDefendAgainstMeleeAttacks() {
      return true;
   }
   @Override
   public boolean canDefendAgainstRangedAttacks() {
      return true;
   }
}
