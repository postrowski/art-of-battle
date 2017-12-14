/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.healing;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.server.Arena;

public class SpellCurePoison extends PriestSpell
{
   public static final String NAME = "Cure Poison";
   public SpellCurePoison() {};
   public SpellCurePoison(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getCasterName() + "'s cure poison spell cures " + getTargetName() + "'s poison.";
      }
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell removes the effects of poison in the target system." +
             " Larger doses, or more deadly poisons require higher power levels to be effective.";
   }
   @Override
   public void applyEffects(Arena arena) {
   }
   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE_ALIVE;
   }
}
