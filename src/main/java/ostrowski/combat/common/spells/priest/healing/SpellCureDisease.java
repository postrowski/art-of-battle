/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.healing;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.server.Arena;

public class SpellCureDisease extends PriestSpell
{
   public static final String NAME = "Cure Disease";
   public SpellCureDisease() {}

   public SpellCureDisease(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getCasterName() + "'s cure disease spell cures " + getTargetName() + "'s disease.";
      }
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell removes the effects of disease in the subject's system." +
             " Advanced, or more deadly diseases, require higher power levels to be effective.";
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
