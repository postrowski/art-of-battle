package ostrowski.combat.common.enums;

import java.util.HashMap;

// positions
public enum Position {
                  //attack, dodge, retreat, parry, move
   PRONE_BACK (0,      -4,    -4,     -4,     -4,    3, "Laying on back"),
   PRONE_FRONT(1,     -99,    -6,     -6,    -99,    2, "Laying on stomach"),
   KNEELING   (2,       0,    -4,     -4,      0,    2, "Kneeling"),
   SITTING    (3,      -4,    -4,     -4,      0,    3, "Sitting"),
   CROUCHING  (4,       0,     0,     -4,      0,    1, "Crouching"),
   STANDING   (5,       0,     0,      0,      0,    0, "Standing");
   public final byte value;
   public final byte adjustmentToAttack;
   public final byte adjustmentToDefenseDodge;
   public final byte adjustmentToDefenseRetreat;
   public final byte adjustmentToDefenseParry;
   public final byte extraMovementPenalty;
   public final String name;
   Position(int val, int adjustmentToAttack,
            int adjustmentToDefendDodge, int adjustmentToDefendRetreat, int adjustmentToDefendParry,
            int extraMovementPenalty, String nam) {
      this.value = (byte)val;
      this.adjustmentToAttack = (byte) adjustmentToAttack;
      this.adjustmentToDefenseDodge = (byte) adjustmentToDefendDodge;
      this.adjustmentToDefenseRetreat = (byte) adjustmentToDefendRetreat;
      this.adjustmentToDefenseParry = (byte) adjustmentToDefendParry;
      this.extraMovementPenalty = (byte) extraMovementPenalty;
      this.name = nam;
   }

   private static HashMap<Byte, Position> MAP_TO_POSITIONS = new HashMap<>();
   static {
      for (Position pos : Position.values()) {
         MAP_TO_POSITIONS.put(pos.value, pos);
      }
   }
   public static Position getByValue(byte val) {
      return MAP_TO_POSITIONS.get(val);
   }
   public String getName() {
      return this.name;
   }
}