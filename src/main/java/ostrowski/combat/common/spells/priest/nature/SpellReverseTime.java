package ostrowski.combat.common.spells.priest.nature;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellReverseTime extends PriestSpell
{
   public static final String NAME = "Reverse Time";
   public SpellReverseTime() {};
   public SpellReverseTime(Class<? extends IPriestGroup> group, int affinity) {
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
      table.addRow(new TableRow(-1, "Effective Power", "Time span"));
      for (int p=1 ; p<=8 ; p++) {
         table.addRow(new TableRow(p-1).addHeader("" + p)
                                       .addTD(new TableData(getDescriptionForpower(p)).setAlignLeft()));
      }
      return "The '" + getName() + "' sends the caster (or a chosen subject) back in time." +
             " The power of the spell determines how far back in time the subject is sent:"+
             table.toString() +
             " If the caster wants to send more than one person back in time, the caster must spend more power." +
             " For each additional power point in the spell, the number of people sent back doubles." +
             " So if the caster spends an additional 2 power points, they could send 4 people back in time." +
             " To send 8 people back in time 1 day would require 7 power points (3 for the 8 people, 4 for the time span)." +
             " Those send back in time will appear at roughly the same location at the previous point in time." +
             " If that location is occupied at the past location, a nearby safe location will be chosen by the GM." +
             " Until the point in the future when the spell is cast, two instances of those sent will exist." +
             " The older of the two copies will know about the younger copy, but the younger will not know about the older copy (until they meet)." +
             " It is up to the GM to resolve what happens if (and when) those sent into the past alter "+
             "the time line such that they no longer need to be sent back in time from the future.";
   }
   private static String getDescriptionForpower(int p) {
      switch (p) {
         case 1: return "1 second";
         case 2: return "1 minute";
         case 3: return "1 hour";
         case 4: return "1 day";
         case 5: return "1 week";
         case 6: return "1 month";
         case 7: return "4 months";
         case 8: return "1 year";
      }
      return "";
   }

}
