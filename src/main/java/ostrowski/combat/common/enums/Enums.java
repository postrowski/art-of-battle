package ostrowski.combat.common.enums;



/*
 * Created on May 4, 2006
 *
 */

import java.util.HashMap;
import java.util.Map;

/**
 * @author Paul
 *
 */
public interface Enums {
   byte     TEAM_ALPHA        = 0;
   byte     TEAM_BETA         = 1;
   byte     TEAM_INDEPENDENT  = 2;
   byte     TEAM_UNIVERSAL    = 3;
   String[] TEAM_NAMES        = new String[] { "A", "B", "I", "U"};


   enum TargetType {
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
   int      ACTION_NONE                = 0;
   int      ACTION_MOVE                = 1 << 0;
   int      ACTION_ATTACK              = 1 << 1;
   int      ACTION_WAIT                = 1 << 2;
   int      ACTION_DEFEND              = 1 << 3;
   int      ACTION_POSITION            = 1 << 4;
   int      ACTION_RETREAT             = 1 << 5;
   int      ACTION_STAND               = 1 << 6;
   int      ACTION_KNEEL               = 1 << 7;
   int      ACTION_CROUCH              = 1 << 8;
   int      ACTION_SIT                 = 1 << 9;
   int      ACTION_LAYDOWN_BACK        = 1 << 10;
   int      ACTION_LAYDOWN_FRONT       = 1 << 11;
   int      ACTION_BREAK_OUT           = 1 << 12;

   enum RANGE {
      OUT_OF_RANGE, POINT_BLANK, SHORT, MEDIUM, LONG;
      RANGE() {
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
   long     EFFECT_NONE                = 0;
   long     EFFECT_WEAPON_DROPPED      = 1 << 0;
   long     EFFECT_WEAPON_UNREADY      = 1 << 1;
   long     EFFECT_WEAPON_KNOCKED_AWAY = 1 << 2;
   long     EFFECT_KNOCKDOWN           = 1 << 3;
   long     EFFECT_KNOCKOUT            = 1 << 4;
   long     EFFECT_COMA                = 1 << 5;
   long     EFFECT_DEATH               = 1 << 6;
   long     EFFECT_BLINDED_1           = 1 << 7;
   long     EFFECT_ORGAN_DAM           = 1 << 8;
   long     EFFECT_LUNG_DAM            = 1 << 9;
   long     EFFECT_PARAPLEGIC          = 1 << 10;
   long     EFFECT_QUADRIPLEGIC        = 1 << 11;
   long     EFFECT_VENA_CAVA           = 1 << 12;
   long     EFFECT_HEART               = 1 << 13;
   long     EFFECT_BRAIN_DAMAGE        = 1 << 14;
   long     EFFECT_BRAIN_DESTROY       = 1 << 15;
   long     EFFECT_DECAPITATE          = 1 << 16;
   long     EFFECT_OFF_BALANCE_1       = 1 << 17;
   long     EFFECT_OFF_BALANCE_2       = 1 << 18;
   long     EFFECT_OFF_BALANCE_3       = 1 << 19;
   long     EFFECT_CONCUSSION          = 1 << 20;

   // Hit locations
   enum Location {
      MISSED, WEAPON, ARM, LEG, WING, HEAD, EYE, NECK, BODY, LIMB, TAIL
   }

   enum Side {
      ANY, LEFT, RIGHT, BOTH
   }

   enum Pair {
      ANY, FIRST, SECOND, THIRD, ALL
   }

   enum RollType {
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
      BREAK_FREE;

      public String toString() {
         if (!nameMap.containsKey(this)) {
            String name = name().replaceAll("_", " ").toLowerCase();
            String sb = String.valueOf(Character.toUpperCase(name.charAt(0))) +
                        name.subSequence(1, name.length());
            nameMap.put(this, sb);
         }
         return nameMap.get(this);
      }
      private static final Map<RollType, String> nameMap = new HashMap<>();
   }

}
