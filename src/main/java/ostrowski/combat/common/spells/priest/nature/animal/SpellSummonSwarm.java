package ostrowski.combat.common.spells.priest.nature.animal;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellSummonSwarm extends PriestSpell
{
   public static final String NAME = "Summon Swarm";
   public SpellSummonSwarm() {}

   public SpellSummonSwarm(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      StringBuilder sb = new StringBuilder();
      sb.append("The '").append(getName()).append("' spell causes a swarm of creature to appear.");
      sb.append(" The power of the spell determines the type of creatures that can be brought, the size of the area affected, and the duration for which they remain.");
      sb.append(" Most of the creatures summoned will damage the area, and those creatures within the area of effect.");
      sb.append(" Also, any character within short distance of the caster will be protected from harm.");
      sb.append(" The range of this protection is chosen by the caster, and may be up to a range in hexes equal to the divine affinity of the caster.");
      sb.append(" The swarm moves as a blob, covering and engulfing everything and everyone it reaches.");
      sb.append(" By optionally spending 1 action on any turn the swarm is active, the casting priest may determine the direction the swarm moves for that single turn.");
      sb.append(" The speed at which the swarm moves is dependent upon the type of creature, and the GM determines its actual speed.");
      sb.append(" Each turn the swarm is active, each character in the area of the swarm will be damaged by the swarm.");
      sb.append(" Damage done by a swarm is different from regular damage:");
      sb.append(" At the start of each turn, each character affected by the swarm rolls one die, determined by the type of swarm.");
      sb.append(" If the die rolls a '1', the character suffers 1d6 generic damage.");
      sb.append(" Build does not protect against swarm damage. The 1d6 (which may explode) determines the actual damage on the generic damage table.");
      sb.append(" Armor does protect against swarms, but only for a short period of time, and in a different way.");
      sb.append(" Armor prevents a number of damages equal to its build adjuster for blunt damage.");
      sb.append(" For example, someone in Heavy Leather (blunt resistance = 3) can bypass the first three 1d6 damage rolls.");
      sb.append(" The fourth time the die rolls a '1', they will suffer a 1d6 swarm damage.");
      sb.append(" If a character falls to the ground for any reason, the swarm will automatically do its 1d6 damage until the character stands back up and moves.");
      Table table = new Table();
      table.addRow(new TableRow(-1, "Effective power", "Examples of creatures", "Swarm size<br/>(diameter in hexes)", "Swarm duration", "Damage chance<br/>per turn"));
      for (int power=1 ; power<=8 ; power++) {
         table.addRow(new TableRow(power-1, "" + power, getCreatureTypesForPower(power),
                                                        ""+getAreaDiameterForPower(power),
                                                        new TableData(""+getDurationForPower(power)).setAlignLeft(),
                                                        getDamageChanceForPower(power)));
      }
      sb.append(table.toString());
//      sb.append("If desired, the casting priest may trade 1 power point from duration, creature type or swarm size for 1 point of any other attribute (duration, type or size).");
//      sb.append(" So a power level 5 spell would normally create a ").append(getAreaDiameterForPower(5)).append(" hex diameter swarm of ").append(getCreatureTypesForPower(5).replaceAll(", ", " or "));
//      sb.append(" that remain for ").append(getDurationForPower(5));
//      sb.append(". However, the priest could move one point of power from duration to creature type, and also move 1 point of swarm size to creature type, resulting in ");
//      sb.append(getAreaDiameterForPower(4)).append(" hex diameter swarm of ").append(getCreatureTypesForPower(7).replaceAll(",", " or "));
//      sb.append(" that remain for ").append(getDurationForPower(4)).append(".");
      return sb.toString();
   }
   private static String getDamageChanceForPower(int power) {
      switch (power) {
         case 1: return "1/20";
         case 2: return "1/12";
         case 3: return "1/10";
         case 4: return "1/8";
         case 5: return "1/6";
         case 6: return "1/4";
         case 7: return "2/6";
         case 8: return "2/4";
      }
      return "---";
   }
   private static String getCreatureTypesForPower(int power) {
      switch (power) {
         case 1: return "locust";
         case 2: return "ants, mice";
         case 3: return "scorpions, birds";
         case 4: return "bees";
         case 5: return "rats, bats";
         case 6: return "snakes";
         case 7: return "poisonous snakes";
         case 8: return "GMs discretion";
      }
      return "";
   }
   private static String getDurationForPower(int power) {
      switch (power) {
         case  1: return "1 minutes (10 turns)";
         case  2: return "2 minutes (20 turns)";
         case  3: return "5 minutes (50 turns)";
         case  4: return "10 minutes (100 turns)";
         case  5: return "1 hour";
         case  6: return "2 hours";
         case  7: return "5 hours";
         case  8: return "1 day";
         case  9: return "1 week";
         case 10: return "1 month";
      }
      return "";
   }
   private static int getAreaDiameterForPower(int power) {
      return (2*power) - 1;
   }

}
