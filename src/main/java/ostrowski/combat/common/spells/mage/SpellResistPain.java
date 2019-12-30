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

public class SpellResistPain extends MageSpell implements ICastInBattle
{
   public static final String NAME = "Resist Pain";
   public SpellResistPain() {
      super(NAME, new Class[] {SpellBlockThought.class},
            new MageCollege[] {MageCollege.DIVINATION, MageCollege.ENCHANTMENT});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTargetName() + " has "+getTarget().getHisHer()+" TOU increased by " + getPower() + " points.";
      }
      return " (TOU increased by " + getPower() + " points)";
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell increases the subject's TOU by an amount equal to the power put into the spell.";
   }
   @Override
   public void applyEffects(Arena arena) {
      // Apply a wound with a negative pain level.
      getTarget().applyWound(new Wound(getPower(),
                                       Location.BODY,
                                       getName() + " spell",
                                       -getPower(),//painLevel
                                       0,//wounds
                                       0,//bleedRate
                                       0,//armPenalty
                                       0,//movePenalty
                                       0,//knockedDownDist,
                                       DamageType.GENERAL,
                                       0,//effectMask,
                                       getTarget()), arena);
      // increase toughness.
      getTarget().setAttribute(Attribute.Toughness, (byte) (getTarget().getAttributeLevel(Attribute.Toughness) + getPower()), false/*containInLimits*/);
   }
   @Override
   public void removeEffects(Arena arena) {
      getTarget().setAttribute(Attribute.Toughness, (byte) (getTarget().getAttributeLevel(Attribute.Toughness) - getPower()), false/*containInLimits*/);
   }

}
