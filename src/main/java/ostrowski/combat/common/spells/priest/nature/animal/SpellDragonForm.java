/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.nature.animal;

import ostrowski.combat.common.Race;
import ostrowski.combat.common.spells.priest.IPriestGroup;

public class SpellDragonForm extends SpellShapeChange
{
   public static final String NAME = "Dragon Form";
   
   public SpellDragonForm() {
      this(null, 0);
   }
   public SpellDragonForm(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }
   
   @Override
   public String getShapeName(byte effectivePower) {
      switch (effectivePower) {
         case 1: return Race.NAME_Baby_Dragon;
         case 2: return Race.NAME_Small_Dragon;
         case 3:
         case 4: return Race.NAME_Dragon;
         case 5: return Race.NAME_Large_Dragon;
         case 6:
         case 7: return Race.NAME_Huge_Dragon;
         case 8: return Race.NAME_Ancient_Dragon;
      }
      return null;
   }
}
