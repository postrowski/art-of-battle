package ostrowski.combat.common.wounds;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.DamageType;

/*
 * Created on May 4, 2006
 *
 */
public class WoundChartCut extends WoundChart {

   public WoundChartCut() {
      super(DamageType.CUT);
   }

   /* (non-Javadoc)
    * @see WoundChart#getWound(int)
    */
   @Override
   public Wound getWound(byte damageLevel, Character target) {
      while (true) {
         if (damageLevel<0) {
            return new Wound((byte)0, Wound.Location.MISSED, "missed",   0, 0, 0, 0, 0, 0, DamageType.CUT, EFFECT_NONE, target);
         }
         Wound wound;
         switch (damageLevel) {                                                                    //  pn wn bl ar mv dist  type     effects
            case  0:  wound = new Wound(damageLevel, Wound.Location.WEAPON, "hit",                      0, 0, 0, 0, 0, 0, DamageType.CUT, EFFECT_WEAPON_UNREADY, target); break;
            case  1:  wound = new Wound(damageLevel, Wound.Location.ARM,  "minor cut",                  2, 0, 0, 0, 0, 0, DamageType.CUT, EFFECT_NONE, target); break;
            case  2:  wound = new Wound(damageLevel, Wound.Location.LEG,  "minor cut",                  2, 0, 0, 0, 0, 0, DamageType.CUT, EFFECT_NONE, target); break;
            case  3:  wound = new Wound(damageLevel, Wound.Location.BODY, "minor cut",                  3, 0, 0, 0, 0, 0, DamageType.CUT, EFFECT_NONE, target); break;
            case  4:  wound = new Wound(damageLevel, Wound.Location.HEAD, "minor cut",                  3, 0, 0, 0, 0, 0, DamageType.CUT, EFFECT_NONE, target); break;
            case  5:  wound = new Wound(damageLevel, Wound.Location.ARM,  "moderate cut",               4, 0, 1, 1, 0, 0, DamageType.CUT, EFFECT_NONE, target); break;
            case  6:  wound = new Wound(damageLevel, Wound.Location.LEG,  "moderate cut",               5, 0, 1, 0, 1, 0, DamageType.CUT, EFFECT_NONE, target); break;
            case  7:  wound = new Wound(damageLevel, Wound.Location.BODY, "moderate cut",               4, 1, 1, 0, 0, 0, DamageType.CUT, EFFECT_NONE, target); break;
            case  8:  wound = new Wound(damageLevel, Wound.Location.NECK, "moderate cut",               4, 1, 1, 0, 0, 0, DamageType.CUT, EFFECT_NONE, target); break;
            case  9:  wound = new Wound(damageLevel, Wound.Location.ARM,  "major cut",                  6, 0, 2, 2, 0, 0, DamageType.CUT, EFFECT_NONE, target); break;
            case 10:  wound = new Wound(damageLevel, Wound.Location.LEG,  "major cut",                  6, 1, 2, 0, 2, 0, DamageType.CUT, EFFECT_NONE, target); break;
            case 11:  wound = new Wound(damageLevel, Wound.Location.BODY, "major cut",                  5, 2, 2, 0, 0, 0, DamageType.CUT, EFFECT_NONE, target); break;
            case 12:  wound = new Wound(damageLevel, Wound.Location.HEAD, "moderate cut",               6, 2, 2, 0, 0, 0, DamageType.CUT, EFFECT_NONE, target); break;
            case 13:  wound = new Wound(damageLevel, Wound.Location.NECK, "major cut",                  6, 2, 3, 0, 0, 0, DamageType.CUT, EFFECT_NONE, target); break;
            case 14:  wound = new Wound(damageLevel, Wound.Location.BODY, "organs cut",                 6, 2, 4, 0, 0, 1, DamageType.CUT, EFFECT_ORGAN_DAM, target); break;
            case 15:  wound = new Wound(damageLevel, Wound.Location.BODY, "lungs cut",                  7, 3, 4, 0, 0, 1, DamageType.CUT, EFFECT_LUNG_DAM, target); break;
            case 16:  wound = new Wound(damageLevel, Wound.Location.ARM,  "crippled, broken bone",      7, 3, 3,-1, 0, 0, DamageType.CUT, EFFECT_WEAPON_DROPPED, target); break;
            case 17:  wound = new Wound(damageLevel, Wound.Location.LEG,  "crippled, broken bone",      7, 3, 3, 0,-1, 0, DamageType.CUT, EFFECT_KNOCKDOWN, target); break;
            case 18:  wound = new Wound(damageLevel, Wound.Location.ARM,  "severed",                    8, 3, 4,-2, 0, 1, DamageType.CUT, EFFECT_NONE, target); break;
            case 19:  wound = new Wound(damageLevel, Wound.Location.LEG,  "severed",                    8, 3, 4, 0,-2, 0, DamageType.CUT, EFFECT_KNOCKDOWN, target); break;
            case 20:  wound = new Wound(damageLevel, Wound.Location.HEAD, "skull cracked",              0, 4, 3, 0, 0, 0, DamageType.CUT, EFFECT_KNOCKOUT, target); break;
            case 21:  wound = new Wound(damageLevel, Wound.Location.BODY, "lower spinal cord severed",  0, 5, 2, 0,-1, 0, DamageType.CUT, EFFECT_KNOCKOUT | EFFECT_PARAPLEGIC, target); break;
            case 22:  wound = new Wound(damageLevel, Wound.Location.BODY, "lower spinal cord severed",  0, 5, 2, 0,-1, 0, DamageType.CUT, EFFECT_KNOCKOUT | EFFECT_PARAPLEGIC, target); break;
            case 23:  wound = new Wound(damageLevel, Wound.Location.HEAD, "massive cut",                0, 5, 3, 0, 0, 0, DamageType.CUT, EFFECT_KNOCKOUT | EFFECT_BRAIN_DAMAGE, target); break;
            case 24:  wound = new Wound(damageLevel, Wound.Location.HEAD, "massive cut",                0, 5, 3, 0, 0, 0, DamageType.CUT, EFFECT_KNOCKOUT | EFFECT_BRAIN_DAMAGE, target); break;
            case 25:  wound = new Wound(damageLevel, Wound.Location.NECK, "upper spinal cord severed",  0, 5, 5,-1,-1, 0, DamageType.CUT, EFFECT_KNOCKOUT | EFFECT_QUADRIPLEGIC, target); break;
            case 26:  wound = new Wound(damageLevel, Wound.Location.NECK, "upper spinal cord severed",  0, 5, 5,-1,-1, 0, DamageType.CUT, EFFECT_KNOCKOUT | EFFECT_QUADRIPLEGIC, target); break;
            case 27:  wound = new Wound(damageLevel, Wound.Location.BODY, "massive cut",                0, 6, 6, 0, 0, 0, DamageType.CUT, EFFECT_COMA | EFFECT_VENA_CAVA, target); break;
            case 28:  wound = new Wound(damageLevel, Wound.Location.BODY, "massive cut",                0, 6, 6, 0, 0, 0, DamageType.CUT, EFFECT_COMA | EFFECT_VENA_CAVA, target); break;
            case 29:  wound = new Wound(damageLevel, Wound.Location.BODY, "massive cut",                0, 6, 7, 0, 0, 0, DamageType.CUT, EFFECT_COMA | EFFECT_HEART, target); break;
            case 30:  wound = new Wound(damageLevel, Wound.Location.BODY, "massive cut",                0, 6, 7, 0, 0, 0, DamageType.CUT, EFFECT_COMA | EFFECT_HEART, target); break;
            case 31:  wound = new Wound(damageLevel, Wound.Location.NECK, "partial decapitation",       0, 6, 4, 0, 0, 0, DamageType.CUT, EFFECT_COMA | EFFECT_BRAIN_DESTROY, target); break;
            case 32:  wound = new Wound(damageLevel, Wound.Location.NECK, "partial decapitation",       0, 6, 4, 0, 0, 0, DamageType.CUT, EFFECT_COMA | EFFECT_BRAIN_DESTROY, target); break;
            case 33:  wound = new Wound(damageLevel, Wound.Location.HEAD, "split skull",                0, 0, 0, 0, 0, 0, DamageType.CUT, EFFECT_DEATH, target); break;
            case 34:  wound = new Wound(damageLevel, Wound.Location.HEAD, "split skull",                0, 0, 0, 0, 0, 0, DamageType.CUT, EFFECT_DEATH, target); break;
            case 35:  wound = new Wound(damageLevel, Wound.Location.NECK, "decapitation",               0, 0, 0, 0, 0, 0, DamageType.CUT, EFFECT_DEATH, target); break;
            case 36:  wound = new Wound(damageLevel, Wound.Location.NECK, "decapitation",               0, 0, 0, 0, 0, 0, DamageType.CUT, EFFECT_DEATH, target); break;
            case 37:  wound = new Wound(damageLevel, Wound.Location.BODY, "cleaved in half at waist",   0, 0, 0, 0, 0, 0, DamageType.CUT, EFFECT_DEATH, target); break;
            case 38:  wound = new Wound(damageLevel, Wound.Location.BODY, "cleaved in half at waist",   0, 0, 0, 0, 0, 0, DamageType.CUT, EFFECT_DEATH, target); break;
            default:  wound = new Wound(damageLevel, Wound.Location.HEAD, "split from head to toe",     0, 0, 0, 0, 0, 0, DamageType.CUT, EFFECT_DEATH, target); break;
         }
         if (wound.isValid()) {
            return wound;
         }
         // Try a lower level wound, until either the attack misses, or the wound is valid.
         damageLevel--;
      }
   }
}
