/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest;

import ostrowski.combat.common.spells.IInstantaneousSpell;

public abstract class InstantaneousPriestSpell extends PriestSpell implements IInstantaneousSpell
{
   protected InstantaneousPriestSpell() {
   }
   protected InstantaneousPriestSpell(String name, Class< ? extends IPriestGroup> group, int affinity) {
      super(name, group, affinity);
   }
   @Override
   public byte getIncantationTime() {
      return 0;
   }
   public boolean canDefendAgainstMeleeAttacks() {
      return false;
   }
   public boolean canDefendAgainstRangedAttacks() {
      return false;
   }
   public boolean canDefendAgainstSpells() {
      return false;
   }
}
