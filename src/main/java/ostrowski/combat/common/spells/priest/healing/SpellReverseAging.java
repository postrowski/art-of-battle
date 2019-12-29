package ostrowski.combat.common.spells.priest.healing;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellReverseAging extends PriestSpell
{
   public static final String NAME = "Reverse Aging";
   public SpellReverseAging() {}

   public SpellReverseAging(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      Table table = new Table();
      table.addRow(new TableRow(-1, "Effective Power", "Age reduction"));
      for (int p=1 ; p<=8 ; p++) {
         table.addRow(new TableRow(p-1).addHeader("" + p)
                                       .addTD(new TableData(getDescriptionForpower(p)).setAlignLeft()));
      }
      return "The '" + getName() + "' spell reverses the effects of time, making the subject of the spell younger."+
             " The power of the spell determines how much age is removed from the subject:"+
             table;
   }
   private static String getDescriptionForpower(int p) {
      switch (p) {
         case 1: return "1 day";
         case 2: return "1 week";
         case 3: return "1 month";
         case 4: return "4 months";
         case 5: return "1 year";
         case 6: return "3 years";
         case 7: return "10 years";
         case 8: return "25 years";
      }
      return "";
   }

}
