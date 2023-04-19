package ostrowski.combat.common.wounds;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.server.CombatServer;

/*
 * Created on May 4, 2006
 *
 */
public class WoundChartElectric extends WoundChart {

   public WoundChartElectric() {
      super(DamageType.ELECTRIC);
   }
   /* (non-Javadoc)
    * @see WoundChart#getWound(int)
    */
   @Override
   public Wound getWound(byte damageLevel, Character target) {
      if (damageLevel<0) {
         return new Wound((byte)0, Wound.Location.MISSED, "missed",   0, 0, 0, 0, 0, 0, DamageType.CUT, EFFECT_NONE, target);
      }
      int pain = 2 + (damageLevel / 3);
      int wounds = damageLevel / 5;
      int bleed = 0;
      if (damageLevel<20) {
         // 15 points or more is a crippling limb blow:
         if (damageLevel>=15) {
            if (target == null) {
               // If target is null, this call is only used to generate the HTML table for the rule's pages.
               return new Wound(damageLevel, Wound.Location.LIMB, "crippling limb shock", pain, wounds, bleed, -1, -1, 0, DamageType.ELECTRIC, EFFECT_KNOCKDOWN | EFFECT_WEAPON_DROPPED, target);
            }
            Wound wound = new Wound(damageLevel, Wound.Location.ARM, "crippling arm shock", pain, wounds, bleed, -1, 0, 0, DamageType.ELECTRIC, EFFECT_WEAPON_DROPPED, target);
            Wound altWound = new Wound(damageLevel, Wound.Location.LEG, "crippling leg shock", pain, wounds, bleed, 0, -1, 0, DamageType.ELECTRIC, EFFECT_KNOCKDOWN, target);
            if (CombatServer.random() > 0.5) {
               Wound temp = altWound;
               altWound = wound;
               wound = temp;
            }
            if (wound.isValid()) {
               return wound;
            }
            if (altWound.isValid()) {
               return altWound;
            }
         }
         return new Wound(damageLevel, Wound.Location.BODY, "general shock", pain, wounds, bleed, 0, 0, 0, DamageType.ELECTRIC, EFFECT_NONE, target);
      }
      if (damageLevel<30) {
         return new Wound(damageLevel, Wound.Location.BODY, "general KO shock", 0, wounds, 0, 0, 0, 0, DamageType.ELECTRIC, EFFECT_KNOCKOUT, target);
      }
      return new Wound(damageLevel, Wound.Location.BODY, "killing shock", 0, 0, 0, 0, 0, 0, DamageType.ELECTRIC, EFFECT_DEATH, target);
   }
}
