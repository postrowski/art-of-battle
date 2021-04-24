/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.spells.IInstantaneousSpell;

public abstract class InstantaneousMageSpell extends MageSpell implements IInstantaneousSpell
{
   @SuppressWarnings("rawtypes")
   protected InstantaneousMageSpell(String name, Class[] prerequisiteSpells, SkillType[] skillTypes) {
      super(name, prerequisiteSpells, skillTypes);
   }
   @Override
   public byte getIncantationTime() {
      return 0;
   }
   @Override
   public boolean canDefendAgainstMeleeAttacks() {
      return false;
   }
   @Override
   public boolean canDefendAgainstRangedAttacks() {
      return false;
   }
   @Override
   public boolean canDefendAgainstSpells() {
      return false;
   }
}
