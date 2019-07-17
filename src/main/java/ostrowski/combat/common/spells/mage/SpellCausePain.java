/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.server.Arena;

/**
 * This spell causes the target to the suffer pain equal to twice
 * the spells power level. This spell has no lasting effects (no wounds).
 */
public class SpellCausePain extends ResistedMageSpell implements ICastInBattle
{
   public static final String NAME = "Cause Pain";
   public SpellCausePain() {
      super(NAME, Attribute.Intelligence, (byte) 1/*resistedActions*/, false/*expires*/,
            new Class[] {SpellSuggestion.class}, new MageCollege[] {MageCollege.ENCHANTMENT, MageCollege.NECROMANCY});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTarget().getName() + " feels " + (getPower() * 2) + " points of pain from " + getCasterName() + "'s spell.";
      }
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell causes pain on the subject equal to two times the power put into the spell.";
   }

   @Override
   public void applyEffects(Arena arena) {
      getTarget().applyWound(new Wound(getPower(),
                                       Location.BODY,
                                       getName() + " spell",
                                       getPower() * 2,//painLevel
                                       0,//wounds
                                       0,//bleedRate
                                       0,//armPenalty
                                       0,//movePenalty
                                       0,//knockedDownDist,
                                       DamageType.GENERAL,
                                       0,//effectMask,
                                       getTarget()), arena);
   }
}
