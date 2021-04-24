/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.server.Arena;

public class SpellTrip extends ResistedMageSpell
{
   public static final String NAME = "Trip";
   public SpellTrip() {
      super(NAME, Attribute.Intelligence, (byte) 1/*resistedActions*/, false/*expires*/,
            new Class[] {SpellCreateForce.class},
            new SkillType[] {SkillType.Spellcasting_Energy});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTargetName() + " is tripped by " + getCasterName() +"'s spell.";
      }
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell causes the subject to fall down.";
   }
   @Override
   public void applyEffects(Arena arena) {
      getTarget().applyWound(new Wound(getPower(),
                                       Location.BODY,
                                       getName() + " spell",
                                       0,//painLevel
                                       0,//wounds
                                       0,//bleedRate
                                       0,//armPenalty
                                       0,//movePenalty
                                       0,//knockedDownDist,
                                       DamageType.GENERAL,
                                       EFFECT_KNOCKDOWN,//effectMask,
                                       getTarget()), arena);
   }
}
