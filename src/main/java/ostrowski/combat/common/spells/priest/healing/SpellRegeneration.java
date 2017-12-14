/*
 * Created on May 13, 2007
 */
package ostrowski.combat.common.spells.priest.healing;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.server.Arena;

public class SpellRegeneration extends PriestSpell
{
   public static final String NAME = "Regeneration";

   public SpellRegeneration() {
   };

   public SpellRegeneration(Class< ? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return getCasterName() + "'s " + getName() + " spell regrows " + getTargetName() + "'s lost limb.";
   }

   @Override
   public String describeSpell() {
      Table table = new Table();
      table.addRow(new TableRow(-1, "Effective Power", "Time required"));
      for (int p=1 ; p<=8 ; p++) {
         table.addRow(new TableRow(p-1).addHeader("" + p + ((p==8) ? "+" : ""))
                                       .addTD(new TableData(getDescriptionForpower(p)).setAlignLeft()));
      }
      StringBuilder sb = new StringBuilder();
      sb.append("The 'Regeneration' spell causes a single lost body part on the subject to regrow.");
      sb.append(" The more power put into the spell, the quicker the limb is regrown.");
      sb.append(table.toString());
      return sb.toString();
   }
   private static String getDescriptionForpower(int p) {
      switch (p) {
         case 1: return "1 week";
         case 2: return "1 day";
         case 3: return "5 hours";
         case 4: return "1 hour";
         case 5: return "10 minutes";
         case 6: return "1 minute";
         case 7: return "1 turn";
         case 8: return "1 round";
      }
      return "";
   }

   @Override
   public void applyEffects(Arena arena) {
      // only level 5 will have a real-time game effect.
      if (getPower() >= 5) {
         for (Wound wound : getTarget().getWoundsList()) {
            if (wound.isSeveredArm() || wound.isSeveredLeg() || wound.isSeveredWing()) {
               getTarget().regrowLimb(wound);
               break;
            }
         }
      }
   }

   @Override
   public Boolean isCastInBattle() {
      return true;
   }

   @Override
   public void removeEffects(Arena arena) {
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE_ALIVE;
   }
}
