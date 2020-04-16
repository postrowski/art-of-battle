package ostrowski.combat.common.spells.priest.elemental;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;

public class SpellEarthquake extends ExpiringPriestSpell
{
   public static final String NAME = "Earthquake";

   public SpellEarthquake() {
      this(null, 0);
   }

   public SpellEarthquake(Class<PriestElementalSpell> group, int affinity) {
      super(NAME, (short)10/*baseExpirationTimeInTurns*/, (short)10/*bonusTimeInTurnsPerPower*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      Table table = new Table();
      table.addRow(new TableRow(-1, "Effective power", "Effects"));
      for (byte power=1 ; power<8 ; power++) {
         table.addRow(new TableRow(power-1, ""+power).addTD(new TableData(getEffects(power)).setAlignLeft()));
      }
      return "The '" + getName() + "' spell shakes the ground violently for a period of time." +
             " The effective power of the spell determines the duration and power of the shake." +
             " The GM determines what actual damage occurs, and may require characters to take action to avoid being hit by falling objects, or falling themselves." +
             table;
   }

   private static String getEffects(int power) {
      switch (power) {
         case 1: return "Minor vibrations";
         case 2: return "Major vibrations";
         case 3: return "Small items fall over";
         case 4: return "Walls crack, medium sized items fall";
         case 5: return "Thin walls collapse";
         case 6: return "Strong walls collapse";
         case 7: return "All buildings topple, fissures open";
         case 8: return "Huge fissures open, buildings may be swallowed";
      }
      return "";
   }

}
