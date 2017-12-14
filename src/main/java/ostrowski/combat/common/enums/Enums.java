package ostrowski.combat.common.enums;



/*
 * Created on May 4, 2006
 *
 */

/**
 * @author Paul
 *
 */
public interface Enums {
   public static final byte     TEAM_ALPHA             = 0;
   public static final byte     TEAM_BETA              = 1;
   public static final byte     TEAM_INDEPENDENT       = 2;
   public static final String[] TEAM_NAMES             = new String[] { "A", "B", "I", "U"};


   public enum TargetType {
      TARGET_SELF,
      TARGET_ANYONE,
      TARGET_ANYONE_ALIVE,
      TARGET_ANYONE_FIGHTING,
      TARGET_OTHER_FIGHTING,
      TARGET_ANIMAL_FIGHTING,
      TARGET_OTHER_GOOD_FIGHTING,
      TARGET_OTHER_EVIL_FIGHTING,
      TARGET_AREA,
      TARGET_DEAD,
      TARGET_UNCONSIOUS,
      TARGET_UNDEAD,
      TARGET_OBJECT,
      TARGET_NONE
   }

   // actions
   public static final int      ACTION_NONE                = 0;
   public static final int      ACTION_MOVE                = 1 << 0;
   public static final int      ACTION_ATTACK              = 1 << 1;
   public static final int      ACTION_WAIT                = 1 << 2;
   public static final int      ACTION_DEFEND              = 1 << 3;
   public static final int      ACTION_POSITION            = 1 << 4;
   public static final int      ACTION_RETREAT             = 1 << 5;
   public static final int      ACTION_STAND               = 1 << 6;
   public static final int      ACTION_KNEEL               = 1 << 7;
   public static final int      ACTION_CROUCH              = 1 << 8;
   public static final int      ACTION_SIT                 = 1 << 9;
   public static final int      ACTION_LAYDOWN_BACK        = 1 << 10;
   public static final int      ACTION_LAYDOWN_FRONT       = 1 << 11;
   public static final int      ACTION_BREAK_OUT           = 1 << 12;

   public enum RANGE {
      OUT_OF_RANGE, POINT_BLANK, SHORT, MEDIUM, LONG;
      private RANGE() {
         _name = this.name().charAt(0) + // first character in upper case
                 this.name().replaceAll("_", " ")
                            .substring(1, this.name().length()).toLowerCase();
      }
      public String getName() {
         return this._name;
      }
      private final String _name;
   }

   // wound data:
   public static final long     EFFECT_NONE                = 0;
   public static final long     EFFECT_WEAPON_DROPPED      = 1 << 0;
   public static final long     EFFECT_WEAPON_UNREADY      = 1 << 1;
   public static final long     EFFECT_WEAPON_KNOCKED_AWAY = 1 << 2;
   public static final long     EFFECT_KNOCKDOWN           = 1 << 3;
   public static final long     EFFECT_KNOCKOUT            = 1 << 4;
   public static final long     EFFECT_COMA                = 1 << 5;
   public static final long     EFFECT_DEATH               = 1 << 6;
   public static final long     EFFECT_BLINDED_1           = 1 << 7;
   public static final long     EFFECT_ORGAN_DAM           = 1 << 8;
   public static final long     EFFECT_LUNG_DAM            = 1 << 9;
   public static final long     EFFECT_PARAPLEGIC          = 1 << 10;
   public static final long     EFFECT_QUADRIPLEGIC        = 1 << 11;
   public static final long     EFFECT_VENA_CAVA           = 1 << 12;
   public static final long     EFFECT_HEART               = 1 << 13;
   public static final long     EFFECT_BRAIN_DAMAGE        = 1 << 14;
   public static final long     EFFECT_BRAIN_DESTROY       = 1 << 15;
   public static final long     EFFECT_DECAPITATE          = 1 << 16;
   public static final long     EFFECT_OFF_BALANCE_1       = 1 << 17;
   public static final long     EFFECT_OFF_BALANCE_2       = 1 << 18;
   public static final long     EFFECT_OFF_BALANCE_3       = 1 << 19;
   public static final long     EFFECT_CONCUSSION          = 1 << 20;

   // Hit locations
   public enum Location {
      MISSED, WEAPON, ARM, LEG, WING, HEAD, EYE, NECK, BODY, LIMB, TAIL
   }

   public enum Side {
      ANY, LEFT, RIGHT, BOTH
   }

   public enum Pair {
      ANY, FIRST, SECOND, THIRD, ALL
   }

   public enum RollType {
      INITIATIVE,
      PAIN_RECOVERY,
      PAIN_CONCENTRATION, // to avoid losing a spell when taking pain, for instance
      ATTACK_TO_HIT,
      SPELL_CASTING,
      DAMAGE_ATTACK,
      DAMAGE_SPELL,
      BERSERK_RESISTANCE, // to avoid going into a berserking rage
      BERSERK_RECOVERY, // to allow the berserker to come out of their rage
      MAGIC_RESISTANCE,
      BREAK_FREE
   };

}
