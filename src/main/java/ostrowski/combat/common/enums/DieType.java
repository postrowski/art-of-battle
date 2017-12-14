package ostrowski.combat.common.enums;

import java.util.HashMap;

public enum DieType {
   D1, D4, D6, D8, D10, D12, D20, Dbell;

   public int getSides() {
      return SIDES.get(this);
   }
   private static final HashMap<DieType, Integer> SIDES = new HashMap<DieType, Integer> () {
      private static final long serialVersionUID = 1L;
      {
         put(DieType.D1, 1);
         put(DieType.D4, 4);
         put(DieType.D6, 6);
         put(DieType.D8, 8);
         put(DieType.D10, 10);
         put(DieType.D12, 12);
         put(DieType.D20, 20);
         put(DieType.Dbell, 12);
      }
   };
}