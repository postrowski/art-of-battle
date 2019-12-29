package ostrowski.combat.common.spells.priest.good;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellForceTruth extends PriestSpell
{
   public static final String NAME = "Force Truth";
   public SpellForceTruth() {
   }
   public SpellForceTruth(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime)
   {
      return null;
   }

   @Override
   public String describeSpell()
   {
      Table table = new Table();
      table.addRow(new TableRow(-1, "Effective Power", "Effects"));
      for (int p=1 ; p<=5 ; p++) {
         table.addRow(new TableRow(p-1).addHeader("" + p + ((p==5) ? "+" : ""))
                                       .addTD(new TableData(getDescriptionForpower(p)).setAlignLeft()));
      }
      return "The '"+getName()+"' spell causes the subject to not be able to lie." +
             " The more effect power in the spell, the higher the effects are:"+
             table;
   }
   private static String getDescriptionForpower(int p) {
      switch (p) {
         case 1: return "Subject stutters and sweats when he lies, and may choose to not talk.";
         case 2: return "Subject may not lie at all, but may choose to not talk, or to leave out some information.</td></tr>";
         case 3: return "Subject may not lie at all, and is compelled to answer all questions truthfully and fully.";
         case 4: return "Subject is compelled to answer all questions truthfully, and will eagerly give up any information he think the asker might be interested in.";
         case 5: return "Subject will talk about anything and everything, and will not shut-up. Everything he/she says will be truthful.";
      }
      return "";
   }
}
