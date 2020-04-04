package ostrowski.combat.common.wounds;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.DamageType;

/*
 * Created on May 4, 2006
 *
 */
public class WoundChartBlunt extends WoundChart {

   public WoundChartBlunt() {
      super(DamageType.BLUNT);
   }
   /* (non-Javadoc)
    * @see WoundChart#getWound(int)
    */
   @Override
   public Wound getWound(byte damageLevel, Character target) {
      while (true) {
         if (damageLevel<0) {
            return new Wound((byte)0, Wound.Location.MISSED, "missed",   0, 0, 0, 0, 0, 0, DamageType.BLUNT, EFFECT_NONE, target);
         }
         Wound wound;
         switch (damageLevel) {                                                                    //  pn wn bl ar mv dist  type     effects
            case  0:  wound = new Wound(damageLevel, Wound.Location.WEAPON, "hit",                      0, 0, 0, 0, 0, 0, DamageType.BLUNT, EFFECT_WEAPON_UNREADY, target); break;
            case  1:  wound = new Wound(damageLevel, Wound.Location.ARM,  "minor blow",                 2, 0, 0, 0, 0, 0, DamageType.BLUNT, EFFECT_NONE, target); break;
            case  2:  wound = new Wound(damageLevel, Wound.Location.LEG,  "minor blow",                 2, 0, 0, 0, 0, 0, DamageType.BLUNT, EFFECT_NONE, target); break;
            case  3:  wound = new Wound(damageLevel, Wound.Location.BODY, "minor blow",                 3, 0, 0, 0, 0, 0, DamageType.BLUNT, EFFECT_NONE, target); break;
            case  4:  wound = new Wound(damageLevel, Wound.Location.HEAD, "minor blow",                 3, 0, 0, 0, 0, 0, DamageType.BLUNT, EFFECT_NONE, target); break;
            case  5:  wound = new Wound(damageLevel, Wound.Location.ARM,  "moderate blow",              4, 0, 0, 1, 0, 0, DamageType.BLUNT, EFFECT_NONE, target); break;
            case  6:  wound = new Wound(damageLevel, Wound.Location.LEG,  "moderate blow",              5, 0, 0, 0, 1, 0, DamageType.BLUNT, EFFECT_NONE, target); break;
            case  7:  wound = new Wound(damageLevel, Wound.Location.BODY, "moderate blow",              4, 1, 0, 0, 0, 0, DamageType.BLUNT, EFFECT_NONE, target); break;
            case  8:  wound = new Wound(damageLevel, Wound.Location.NECK, "moderate blow",              4, 1, 0, 0, 0, 0, DamageType.BLUNT, EFFECT_NONE, target); break;
            case  9:  wound = new Wound(damageLevel, Wound.Location.ARM,  "fractured bone",             6, 0, 1, 2, 0, 1, DamageType.BLUNT, EFFECT_NONE, target); break;
            case 10:  wound = new Wound(damageLevel, Wound.Location.LEG,  "fractured bone",             4, 1, 1, 0, 1, 1, DamageType.BLUNT, EFFECT_KNOCKDOWN, target); break;
            case 11:  wound = new Wound(damageLevel, Wound.Location.BODY, "cracked ribs",               5, 2, 1, 0, 0, 1, DamageType.BLUNT, EFFECT_NONE, target); break;
            case 12:  wound = new Wound(damageLevel, Wound.Location.HEAD, "moderate blow",              6, 2, 1, 0, 0, 1, DamageType.BLUNT, EFFECT_NONE, target); break;
            case 13:  wound = new Wound(damageLevel, Wound.Location.NECK, "major blow",                 6, 2, 1, 0, 0, 1, DamageType.BLUNT, EFFECT_NONE, target); break;
            case 14:  wound = new Wound(damageLevel, Wound.Location.BODY, "organs ruptured",            4, 2, 2, 0, 0, 2, DamageType.BLUNT, EFFECT_ORGAN_DAM | EFFECT_KNOCKDOWN, target); break;
            case 15:  wound = new Wound(damageLevel, Wound.Location.BODY, "broken ribs",                7, 3, 2, 0, 0, 2, DamageType.BLUNT, EFFECT_LUNG_DAM, target); break;
            case 16:  wound = new Wound(damageLevel, Wound.Location.ARM,  "crippled, broken bone",      7, 3, 2,-1, 0, 1, DamageType.BLUNT, EFFECT_WEAPON_DROPPED, target); break;
            case 17:  wound = new Wound(damageLevel, Wound.Location.LEG,  "crippled, broken bone",      7, 3, 2, 0,-1, 0, DamageType.BLUNT, EFFECT_KNOCKDOWN, target); break;
            case 18:  wound = new Wound(damageLevel, Wound.Location.ARM,  "shoulder crushed",           8, 3, 2,-1, 0, 1, DamageType.BLUNT, EFFECT_WEAPON_DROPPED, target); break;
            case 19:  wound = new Wound(damageLevel, Wound.Location.LEG,  "pelvis crushed",             8, 3, 2, 0,-1, 2, DamageType.BLUNT, EFFECT_KNOCKDOWN, target); break;
            case 20:  wound = new Wound(damageLevel, Wound.Location.HEAD, "skull cracked",              0, 3, 3, 0, 0, 0, DamageType.BLUNT, EFFECT_KNOCKOUT | EFFECT_CONCUSSION, target); break;
            case 21:  wound = new Wound(damageLevel, Wound.Location.BODY, "lower spinal cord crushed",  0, 5, 2, 0,-1, 0, DamageType.BLUNT, EFFECT_KNOCKOUT | EFFECT_PARAPLEGIC, target); break;
            case 22:  wound = new Wound(damageLevel, Wound.Location.BODY, "lower spinal cord crushed",  0, 5, 2, 0,-1, 0, DamageType.BLUNT, EFFECT_KNOCKOUT | EFFECT_PARAPLEGIC, target); break;
            case 23:  wound = new Wound(damageLevel, Wound.Location.HEAD, "massive blow",               0, 5, 3, 0, 0, 0, DamageType.BLUNT, EFFECT_KNOCKOUT | EFFECT_BRAIN_DAMAGE, target); break;
            case 24:  wound = new Wound(damageLevel, Wound.Location.HEAD, "massive blow",               0, 5, 3, 0, 0, 0, DamageType.BLUNT, EFFECT_KNOCKOUT | EFFECT_BRAIN_DAMAGE, target); break;
            case 25:  wound = new Wound(damageLevel, Wound.Location.NECK, "upper spinal cord crushed",  0, 5, 5,-1,-1, 0, DamageType.BLUNT, EFFECT_KNOCKOUT | EFFECT_QUADRIPLEGIC, target); break;
            case 26:  wound = new Wound(damageLevel, Wound.Location.NECK, "upper spinal cord crushed",  0, 5, 5,-1,-1, 0, DamageType.BLUNT, EFFECT_KNOCKOUT | EFFECT_QUADRIPLEGIC, target); break;
            case 27:  wound = new Wound(damageLevel, Wound.Location.BODY, "massive blow",               0, 6, 6, 0, 0, 2, DamageType.BLUNT, EFFECT_COMA | EFFECT_VENA_CAVA, target); break;
            case 28:  wound = new Wound(damageLevel, Wound.Location.BODY, "massive blow",               0, 6, 6, 0, 0, 2, DamageType.BLUNT, EFFECT_COMA | EFFECT_VENA_CAVA, target); break;
            case 29:  wound = new Wound(damageLevel, Wound.Location.BODY, "massive blow",               0, 6, 7, 0, 0, 2, DamageType.BLUNT, EFFECT_COMA | EFFECT_HEART, target); break;
            case 30:  wound = new Wound(damageLevel, Wound.Location.BODY, "massive blow",               0, 6, 7, 0, 0, 2, DamageType.BLUNT, EFFECT_COMA | EFFECT_HEART, target); break;
            case 31:  wound = new Wound(damageLevel, Wound.Location.HEAD, "skull crushed",              0, 6, 4, 0, 0, 0, DamageType.BLUNT, EFFECT_COMA | EFFECT_BRAIN_DESTROY, target); break;
            case 32:  wound = new Wound(damageLevel, Wound.Location.HEAD, "skull crushed",              0, 6, 4, 0, 0, 0, DamageType.BLUNT, EFFECT_COMA | EFFECT_BRAIN_DESTROY, target); break;
            case 33:  wound = new Wound(damageLevel, Wound.Location.HEAD, "skull & neck crushed",       0, 0, 0, 0, 0, 1, DamageType.BLUNT, EFFECT_DEATH, target); break;
            case 34:  wound = new Wound(damageLevel, Wound.Location.HEAD, "skull & neck crushed",       0, 0, 0, 0, 0, 1, DamageType.BLUNT, EFFECT_DEATH, target); break;
            case 35:  wound = new Wound(damageLevel, Wound.Location.NECK, "skull & spine crushed",      0, 0, 0, 0, 0, 1, DamageType.BLUNT, EFFECT_DEATH, target); break;
            case 36:  wound = new Wound(damageLevel, Wound.Location.NECK, "skull & spine crushed",      0, 0, 0, 0, 0, 1, DamageType.BLUNT, EFFECT_DEATH, target); break;
            case 37:  wound = new Wound(damageLevel, Wound.Location.BODY, "decapitation",               0, 0, 0, 0, 0, 1, DamageType.BLUNT, EFFECT_DEATH | EFFECT_DECAPITATE, target); break;
            case 38:  wound = new Wound(damageLevel, Wound.Location.BODY, "decapitation",               0, 0, 0, 0, 0, 1, DamageType.BLUNT, EFFECT_DEATH | EFFECT_DECAPITATE, target); break;
            case 39:  wound = new Wound(damageLevel, Wound.Location.HEAD, "skull smashed through body", 0, 0, 0, 0, 0, 0, DamageType.BLUNT, EFFECT_DEATH, target); break;
            default:  wound = new Wound(damageLevel, Wound.Location.HEAD, "skull smashed through body", 0, 0, 0, 0, 0, 0, DamageType.BLUNT, EFFECT_DEATH, target); break;
         }
         if (wound.isValid()) {
            return wound;
         }
         // Try a lower level wound, until either the attack misses, or the wound is valid.
         damageLevel--;
      }
   }
}
