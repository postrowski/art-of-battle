/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.server.Arena;

/**
 * This spell causes the target to the suffer a wound with wound penalties equal to
 * the spells power level. This spell does not inflict pain.
 */
public class SpellCauseWound extends ResistedMageSpell
{
   public static final String NAME = "Cause Wound";
   public SpellCauseWound() {
      super(NAME, Attribute.Health, (byte) 2/*resistedActions*/, false/*expires*/,
            new Class[] {SpellCausePain.class, SpellCreateForce.class, SpellSuggestion.class},
            new MageCollege[] {MageCollege.ENERGY, MageCollege.ENCHANTMENT, MageCollege.NECROMANCY});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTarget().getName() + " suffers " + getPower() + " wounds from " + getCasterName() + "'s spell.";
      }
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell causes a single wound to appear on the subject."
           + " The wound’s penalty equals the power put into the spell.";
   }
   @Override
   public Boolean isCastInBattle() {
      return true;
   }

   @Override
   public void applyEffects(Arena arena) {
      getTarget().applyWound(new Wound(getPower(),
                                       Location.BODY,
                                       getName() + " spell",
                                       0,//painLevel
                                       getPower(),//wounds
                                       0,//bleedRate
                                       0,//armPenalty
                                       0,//movePenalty
                                       0,//knockedDownDist,
                                       DamageType.GENERAL,
                                       0,//effectMask,
                                       getTarget()), arena);
   }

}
