/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.healing;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.server.Arena;

import java.util.ArrayList;
import java.util.List;

public class SpellHeal extends SpellCureSeriousWound
{
   public static final String NAME = "Heal";
   public SpellHeal() {}

   public SpellHeal(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getCasterName() + "'s " + getName() + " spell heals " + getWoundReduction() + " wounds from each  wound on " + getTargetName();
      }
      return null;
   }
   @Override
   public String describeSpell() {
      StringBuilder sb = new StringBuilder();
      sb.append("The '");
      sb.append(getName());
      sb.append("' spell reduces the wounds from all blows on the subject, removing wounds and bleeding based on the effective power of the spell:");
      Table table = new Table();
      table.addRow(new TableRow(-1, "Effective power", "Bleed rate<br/>reduction", "Wound<br/>reduction", "Pain<br/>reduction"));
      for (byte power=1 ; power<8 ; power++) {
        table.addRow(new TableRow(power-1, ""+power,
                                           ""+getBleedingReduction(power),
                                           ""+getWoundReduction(power),
                                           ""+getPainReduction(power)));
      }
      sb.append(table);
      return sb.toString();
   }
   @Override
   public void applyEffects(Arena arena) {
      List<Wound> wounds = new ArrayList<>(getTarget().getWoundsList());
      for (Wound wound : wounds) {
         // Find the highest wound that has a wound level equal-to or greater-than our woundReduction level.
         getTarget().cureWound(wound, getWoundReduction(), getBleedingReduction());
      }
      getTarget().reducePain(getPainReduction());
   }
   @Override
   public byte getPainReduction(byte power) {
      return (byte) (power * 2);
   }
}
