package ostrowski.combat.protocol;

import org.junit.Test;

import ostrowski.combat.common.CombatMap;

public class MapVisibilityTest
{

   @Test
   public void test() {
      CombatMap map = new CombatMap((short)10, (short)10, null);
      MapVisibility obj = new MapVisibility(map);
      MapVisibility dup = obj.clone();
      dup.equals(obj);
   }

}
