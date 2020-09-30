/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.healing;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.server.Arena;

public class SpellCureWound extends PriestSpell implements ICastInBattle
{
   public static final String NAME = "Cure Wound";
   public SpellCureWound() {}

   public SpellCureWound(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }
   public SpellCureWound(String name, Class<? extends IPriestGroup> group, int affinity) {
      super(name, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getCasterName() + "'s " + getName() + " spell heals " + getWoundReduction() + " wounds from " + getTargetName();
      }
      return null;
   }
   @Override
   public String describeSpell() {
      StringBuilder sb = new StringBuilder();
      sb.append("The '");
      sb.append(getName());
      sb.append("' spell reduces the wounds from a single blow on the subject, removing wound, bleeding and pain based on the effective power of the spell:");
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
      SortedSet<Wound> sortedWounds = new TreeSet<>((wound1, wound2) -> {
         if ((wound1.getPenaltyMove() == -1) == (wound2.getPenaltyMove() == -1)) {
            if ((wound1.getPenaltyLimb() == -1) == (wound2.getPenaltyLimb() == -1)) {
               if (wound1.getEffectiveWounds() == wound2.getEffectiveWounds()) {
                  if (wound1.getEffectiveBleedRate() == wound2.getEffectiveBleedRate()) {
                     if (wound1.getWounds() == wound2.getWounds()) {
                        if (wound1.getPenaltyMove() == wound2.getPenaltyMove()) {
                           if (wound1.getBleedRate() == wound2.getBleedRate()) {
                              return Byte.compare(wound1.getLevel(), wound2.getLevel());
                           }
                           if (wound1.getBleedRate() < wound2.getBleedRate()) {
                              return -1;
                           }
                           return 1;
                        }
                        if (wound1.getPenaltyMove() < wound2.getPenaltyMove()) {
                           return -1;
                        }
                        return 1;
                     }
                     if (wound1.getWounds() < wound2.getWounds()) {
                        return -1;
                     }
                     return 1;
                  }
                  if (wound1.getEffectiveBleedRate() < wound2.getEffectiveBleedRate()) {
                     return -1;
                  }
                  return 1;
               }
               if (wound1.getEffectiveWounds() < wound2.getEffectiveWounds()) {
                  return -1;
               }
               return 1;
            }
            if (wound1.getPenaltyLimb() == -1) {
               return 1;
            }
            return -1;
         }
         if (wound1.getPenaltyMove() == -1) {
            return 1;
         }
         return -1;
      });
      sortedWounds.addAll(getTarget().getWoundsList());
      Wound bestWoundToCure = null;
      for (Wound wound : sortedWounds) {
         // Find the highest wound that has a wound level equal-to or greater-than our woundReduction level.
         if (bestWoundToCure == null) {
            bestWoundToCure = wound;
         }
         else {
            if (wound.getWounds() <= getWoundReduction()) {
               bestWoundToCure = wound;
            }
         }
      }
      if (bestWoundToCure != null) {
         getTarget().cureWound(bestWoundToCure, getWoundReduction(), getBleedingReduction());
      }
      getTarget().reducePain(getPainReduction());
   }
   protected byte getPainReduction() {
      return getPainReduction(getPower());
   }
   protected byte getWoundReduction() {
      return getWoundReduction(getPower());
   }
   protected byte getBleedingReduction() {
      return getBleedingReduction(getPower());
   }
   public byte getWoundReduction(byte power) {
      return (byte)(power / 2);
   }
   public byte getPainReduction(byte power) {
      return power;
   }
   public byte getBleedingReduction(byte power) {
      return (byte)((power+1) / 2);
   }
   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE_ALIVE;
   }
}
