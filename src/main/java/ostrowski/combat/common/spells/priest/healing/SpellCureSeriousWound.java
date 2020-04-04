/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.healing;

import ostrowski.combat.common.spells.priest.IPriestGroup;


public class SpellCureSeriousWound extends SpellCureWound
{
   public static final String NAME = "Cure Serious Wound";
   public SpellCureSeriousWound() {}

   public SpellCureSeriousWound(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }
   public SpellCureSeriousWound(String name, Class<? extends IPriestGroup> group, int affinity) {
      super(name, group, affinity);
   }

   @Override
   public byte getWoundReduction(byte power) {
      return power;
   }
   @Override
   public byte getBleedingReduction(byte power) {
      return power;
   }
   @Override
   public byte getPainReduction(byte power) {
      return (byte) (power * 2);
   }
}
