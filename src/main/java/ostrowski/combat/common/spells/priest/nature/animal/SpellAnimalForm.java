/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.nature.animal;

import ostrowski.combat.common.Race;
import ostrowski.combat.common.spells.priest.IPriestGroup;

public class SpellAnimalForm extends SpellShapeChange
{
   public static final String NAME = "Animal Form";

   public SpellAnimalForm() {
      this(null, 0);
   }
   public SpellAnimalForm(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String getShapeName(byte effectivePower) {
      switch (effectivePower) {
         case 1: return Race.NAME_Fox;
         case 2: return Race.NAME_Eagle;
         case 3: return Race.NAME_Wolf;
         case 4: return Race.NAME_Puma;
         case 5: return Race.NAME_Tiger;
         case 6: return Race.NAME_Bear;
         case 7: return Race.NAME_Crocodile;
         case 8: return Race.NAME_CaveBear;
      }
      return null;
   }
}
