package ostrowski.combat.common.spells.priest.information;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellOpenPortal extends PriestSpell
{
   public static final String NAME = "Open Portal";
   public SpellOpenPortal() {};
   public SpellOpenPortal(Class<? extends IPriestGroup> group, int affinity) {
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
      table.addRow(new TableRow(-1, "Power", "Duration", "Distance"));
      for (int p=1 ; p<=8 ; p++) {
         table.addRow(new TableRow(p-1).addHeader("" + p + ((p==8) ? "+" : ""))
                                       .addTD(new TableData(getDurationDescriptionForpower(p)))
                                       .addTD(new TableData(getDistanceDescriptionForpower(p)).setAlignLeft()));
      }
      return "The '"+ getName() + "' spell opens a portal to any destination that is known by the Deity." +
             " The more power put into the spell, the further away the destination may be, and the longer the portal will remain open." +
             " The caster must allocate points for each duration and distance individually." +
             " The total cost of the spell is the sum of the duration power and the distance power." +
             table.toString() +
             "<br/> So a priest with 4 power points of effective power could open a portal" +
             " to a location " + getDurationDescriptionForpower(1) + " away for "+getDistanceDescriptionForpower(3)+
             ", or " + getDurationDescriptionForpower(2) + " away for "+getDistanceDescriptionForpower(2)+
             ", or " + getDurationDescriptionForpower(3) + " away for "+getDistanceDescriptionForpower(1)+"." +
             " As long as the portal is open, anyone may enter through the portal from either side,"+
             " and will be transported at the speed of light to the other end of the portal."+
             " The caster may choose to close the portal earlier than the duration, " +
             "but if the caster dies, the portal will remain open for the duration specified, unless closed by other spells." +
             " The portal acts just like a normal door, and spells and missile weapons may pass freely through this door unobstructed.";
    }
   private static String getDurationDescriptionForpower(int p) {
      switch (p) {
         //case 0: return "1 round";
         case 1: return "1 turn";
         case 2: return "1 minute";
         case 3: return "10 minutes";
         case 4: return "1 hour";
         case 5: return "1 day";
         case 6: return "1 week";
         case 7: return "1 month";
         case 8: return "1 year";
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
