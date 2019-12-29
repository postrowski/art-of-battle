package ostrowski.combat.common.spells.priest.healing;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellStasis extends PriestSpell
{
   public static final String NAME = "Statis";
   public SpellStasis() {}

   public SpellStasis(Class<? extends IPriestGroup> group, int affinity) {
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
      table.addRow(new TableRow(-1, "Effective power", "Effects"));
      for (int p=1 ; p<=5 ; p++) {
         table.addRow(new TableRow(p-1).addHeader("" + p)
                                       .addTD(new TableData(getDescriptionForpower(p)).setAlignLeft()));
      }
      return "The '" + getName() + "' spell puts the subject into a state of suspended animation."+
             " The more power put into the spell, the deeper the statis is.<br/>" +
             table;
   }
   private static String getDescriptionForpower(int p) {
      switch (p) {
         case 1:  return "Sleeping state.";
         case 2:  return "Deep sleeping state, bleeding reduced by 50%.";
         case 3:  return "Light coma, subject is dreaming, and bleeding reduced by 75%.";
         case 4:  return "Deep coma, subject has very little brain activity, and bleeding is reduced by 95%.";
         case 5:  return "Suspended animation, subject has no brain activity or bleeding.";
      }
      return null;
   }

}
