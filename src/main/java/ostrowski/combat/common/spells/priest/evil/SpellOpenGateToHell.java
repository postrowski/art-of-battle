package ostrowski.combat.common.spells.priest.evil;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellOpenGateToHell extends PriestSpell
{
   public static final String NAME = "Open Gate to Hell";
   public SpellOpenGateToHell() {
   }
   public SpellOpenGateToHell(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell() {
      Table table = new Table();
      table.addRow(new TableRow(-1, "Effective Power", "Duration"));
      for (int p=1 ; p<=8 ; p++) {
         table.addRow(new TableRow(p-1).addHeader("" + p + ((p==8 ? "+" : "")))
                                       .addTD(new TableData(getDescriptionForpower(p)).setAlignLeft()));
      }
      return "The '" + getName() + "' spell opens a portal directly into the plane of hell." +
             " The more power put into the spell, the longer the portal will remain open." +
             table +
             "<br/> As long as the portal is open, anyone or anything may enter through the portal from either side," +
             " and will be transported instantly to the other end of the portal." +
             " The caster may choose to close the portal earlier than the duration, " +
             "but if the caster dies, the portal will remain open for the duration specified, unless closed by other spells." +
             " A gate opened to hell will usually draw the attention of one or more devils or demons, which will use " +
             "the gate to enter our world. Generally, gates to hell should only be opened within a pentagram, or other protective " +
             " mechanism, so that the creatures that will come out can be controlled or at least contained." +
             " Only a few casters have ever survived opening a gate to hell without such protections.";
   }
   private static String getDescriptionForpower(int p) {
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

}
