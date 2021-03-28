/*
 * Created on May 25, 2007
 *
 */
package ostrowski.combat.common.spells.priest.defensive;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.InstantaneousPriestSpell;

public class SpellBlockAttack extends InstantaneousPriestSpell
{
   public static final String NAME = "Block Attack";
   public SpellBlockAttack() {}
   public SpellBlockAttack(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell is used defensively when attacked by an enemy with a melee weapon, ranged weapon, or missile spell." +
             " This spell increases the caster's TN" +
             " by an amount equal to the caster's divine affinity times the actions spent on the spell." +
             " For each action spent casting this spell, the priest expends one power point against his daily limit." +
             " The number of actions spent may never exceed the priest's Divine Power level.";
   }

   @Override
   public byte getActiveDefensiveTN(byte actionsSpent, Character caster) {
      if ((this.caster != null) && (this.caster != caster)) {
         DebugBreak.debugBreak();
      }
      this.caster = caster;
      return (byte) (getCastingLevel() * actionsSpent);
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
