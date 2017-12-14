package ostrowski.combat.common.spells.priest.information;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellRemoteVision extends PriestSpell
{
   public static final String NAME = "Remote Vision";
   public SpellRemoteVision() {};
   public SpellRemoteVision(Class<? extends IPriestGroup> group, int affinity) {
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
      table.addRow(new TableRow(-1, "Effective Power", "Duration", "Distance"));
      for (int p=1 ; p<=8 ; p++) {
         table.addRow(new TableRow(p-1).addHeader("" + p + ((p==8) ? "+" : ""))
                                       .addTD(new TableData(getDurationDescriptionForpower(p)))
                                       .addTD(new TableData(getDistanceDescriptionForpower(p)).setAlignLeft()));
      }
      return "The '"+ getName() + "' spell opens a floating window that is connected to any destination that is known by the Deity." +
      " The more power put into the spell, the further away the destination may be, and the longer the window will remain open." +
      table.toString() +
      "<br/> The caster may choose if the target location can observe the window." +
      " If the caster allows it, then the window becomes like a video phone, passing only light and sounds in each direction."+
      " If the caster denies it, observers at the target location will not see the window, nor know of its existence, without some form of a detect magic spell." +
      " The caster may choose to close the portal earlier than the duration, " +
      "but if the caster dies, the portal will remain open for the duration specified, unless closed by other spells." +
      " Spells may not be cast through the window in either direction, nor can missile weapons pass through it.";
   }

   private static String getDurationDescriptionForpower(int p) {
      switch (p) {
         case 1: return "1 round";
         case 2: return "1 turn";
         case 3: return "1 minute";
         case 4: return "10 minutes";
         case 5: return "1 hour";
         case 6: return "1 day";
         case 7: return "1 week";
         case 8: return "1 month";
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
