package ostrowski.combat.common.things;

import java.util.HashMap;

public enum DoorState {
   Broken(0),
   Open(1),
   Closed(2),
   Locked(3),
   Blocked(4);
   DoorState(int val) {
      value = val;
   }
   public final int value;
   private static HashMap<Integer, DoorState> MAP_TO_DOORSTATE = new HashMap<>();
   static {
      for (DoorState state : values()) {
         MAP_TO_DOORSTATE.put(state.value, state);
      }
   }

   public static DoorState getByValue(int val) {
      return MAP_TO_DOORSTATE.get(val);
   }
}