package ostrowski.combat.common.spells.priest.information;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellRemoteConversation extends PriestSpell
{
   public static final String NAME = "Remote Conversation";
   public SpellRemoteConversation() {};
   public SpellRemoteConversation(Class<? extends IPriestGroup> group, int affinity) {
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
      return "The '"+ getName() + "' spell opens an invisible window to any person or creature that is known by the Deity." +
      " The more power put into the spell, the further away the target may be, and the longer the window will remain open." +
      table.toString() +
      "<br/> The caster may choose if sounds from his location are sent to the destination location." +
      " If the caster allows it, then the window becomes like a speaker phone, passing only sounds in each direction."+
      " If the caster denies it, observers at the target location will not know of the windows existence, without some form of a detect magic spell." +
      " The caster may choose to close the portal earlier than the duration, " +
      "but if the caster dies, the portal will remain open for the duration specified, unless closed by other spells." +
      " Only sound may pass through the window, and spells may not be cast through this window.";
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
