/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.spells.ICastInBattle;

public class SpellImmobilize extends ResistedMageSpell implements ICastInBattle
{
   public static final String NAME = "Immobilize";
   public SpellImmobilize() {
      super(NAME, Attribute.Health, (byte) 3/*resistedActions*/, true/*expires*/,
            new Class[] {SpellSuggestion.class, SpellWeaken.class},
            new MageCollege[] {MageCollege.EVOCATION, MageCollege.ENERGY, MageCollege.ENCHANTMENT});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTarget().getName() + " is slowed down by an 'Immobilize' spell, reduing "+getTarget().getHisHer()+" movement by " + getPower() + " per round.";
      }
      return " movement reduce by " + getPower() + " per round.";
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell reduces the subject's movement rate by one point per point of spell power.";
   }

   @Override
   public byte getModifiedMovementPerRound(byte previousMovementRate) {
      if (!isExpired()) {
         return 0;
      }
      return previousMovementRate;
   }
}
