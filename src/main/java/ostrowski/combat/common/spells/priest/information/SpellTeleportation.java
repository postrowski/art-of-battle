package ostrowski.combat.common.spells.priest.information;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellTeleportation extends PriestSpell
{
   public static final String NAME = "Teleportation";
   public SpellTeleportation() {};
   public SpellTeleportation(Class<? extends IPriestGroup> group, int affinity) {
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
      table.addRow(new TableRow(-1, "Effective Power", "Number of people", "Distance"));
      for (int p=1 ; p<=8 ; p++) {
         table.addRow(new TableRow(p-1).addHeader("" + p + ((p==8) ? "+" : ""))
                                       .addTD(new TableData(getNumberOfPeopleForpower(p, false)))
                                       .addTD(new TableData(getDistanceDescriptionForpower(p)).setAlignLeft()));
      }
      return "The '"+ getName() + "' spell transports the subject(s) to any destination that is known by the deity."+
             " The more power put into the spell, the further away the location may be, and/or the more people (or things) that can be transported." +
             " Spell power must be spend on both number of people and distance separately." +
             table.toString() +
             "<br/> So a priest that spends 4 power points on a Teleportation spell may send"+
             " "+getNumberOfPeopleForpower(1, true)+" "+getDistanceDescriptionForpower(3)+" away,"+
             " or "+getNumberOfPeopleForpower(2, true)+" "+getDistanceDescriptionForpower(2)+" away,"+
             " or "+getNumberOfPeopleForpower(3, true)+" "+getDistanceDescriptionForpower(1)+" away."+
             " Items may be sent instead of people, so long as the weight of the item(s) does not exceed the weight of the allowed number of people, assuming 200 lbs. per person." +
             " So a level 4 spell could send 4 people, or 800 lbs. of equipment, or 2 people and 400 lbs. of equipment a distance of 1 mile.";
   }

   private static String getNumberOfPeopleForpower(int p, boolean includePerson) {
      switch (p) {
         case 1: return "1" + (includePerson ? " person" : "");
         case 2: return "2" + (includePerson ? " people" : "");
         case 3: return "4" + (includePerson ? " people" : "");
         case 4: return "10" + (includePerson ? " people" : "");
         case 5: return "20" + (includePerson ? " people" : "");
         case 6: return "50" + (includePerson ? " people" : "");
         case 7: return "100" + (includePerson ? " people" : "");
         case 8: return "200" + (includePerson ? " people" : "");
      }
      return "";
   }
   private static String getDistanceDescriptionForpower(int p) {
      switch (p) {
         case 1: return "1 mile";
         case 2: return "10 miles";
         case 3: return "100 miles";
         case 4: return "1,000 miles";
         case 5: return "Anywhere on the same planet";
         case 6: return "Anywhere in the same dimensional plane";
         case 7: return "Anywhere";
         case 8: return "Anywhere";
      }
      return "";
   }
}
