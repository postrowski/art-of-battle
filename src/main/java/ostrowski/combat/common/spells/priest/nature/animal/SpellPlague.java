package ostrowski.combat.common.spells.priest.nature.animal;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellPlague extends PriestSpell
{
   public static final String NAME = "Plague";
   public SpellPlague() {};
   public SpellPlague(Class<? extends IPriestGroup> group, int affinity) {
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
      sb.append("The '").append(getName()).append("' spell causes a huge swarm of creature to appear, covering the ground, and possibly filling the skies.");
      sb.append(" The power of the spell determines the type of creatures that can be brought, the size of the area affected, and the duration for which they remain.");
      sb.append(" Most of the creatures summoned will damage the area, and any creatures within the area of effect.");
      sb.append(" Also, any character within short distance of the caster will be protected from harm.");
      sb.append(" The range of this protection is chosen by the caster, and may be up to a range in hexes equal to the divine affinity of the caster.");
      sb.append(" The swarm moves as a large blob, covering and engulfing everything and everyone it reaches.");
      sb.append(" The speed at which the swarm moves is dependent upon the type of creature, and the GM determines its actual movement.");
      sb.append(" Each turn the swarm is active, each character in the area of the swarm will be damaged by the swarm.");
      sb.append(" A character's only defense against the swarm is to surround themselves with a large fire, which will prevent the swarm from attacking them.");
      sb.append(" Characters may also try to run away, hoping to get inside a protective shelter before the swarm reaches them.");
      sb.append(" Damage done by a swarm is different from regular damage:");
      sb.append(" At the start of each turn, each character affected by the swarm rolls one die, determined by the type of swarm.");
      sb.append(" If the die rolls a '1', the character suffers 1d6 generic damage.");
      sb.append(" Build does not protect against swarm damage. The 1d6 (which may explode) determines the actual damage on the generic damage table.");
      sb.append(" Armor does protect against swarms, but only for a short period of time.");
      sb.append(" Armor prevents a number of damages equal to its build adjuster for blunt damage.");
      sb.append(" For example, someone in Heavy Leather (blunt resistance = 3) can bypass the first three 1d6 damage rolls.");
      sb.append(" The fourth time the die rolls a '1', they will suffer a 1d6 swarm damage.");
      sb.append(" If a character falls to the ground for any reason, the swarm will automatically do its 1d6 damage until the character stands back up and moves.");
      Table table = new Table();
      table.addRow(new TableRow(-1, "Effective power", "Examples of creatures", "Plague duration", "Damage chance<br/>per turn"));
      for (int power=1 ; power<=8 ; power++) {
         table.addRow(new TableRow(power-1, ""+power,
                                            ""+getCreatureTypesForPower(power),
                                            ""+getDurationForPower(power),
                                            ""+getDamageChanceForPower(power).toString()));
      }
      sb.append(table.toString());
//    sb.append("If desired, the casting priest may trade 1 power point from duration, creature type or swarm size for 1 point of any other attribute (duration, type or size).");
//    sb.append(" So a power level 5 spell would normally create a ").append(getAreaDiameterForPower(5)).append(" hex diameter swarm of ").append(getCreatureTypesForPower(5).replaceAll(", ", " or "));
//    sb.append(" that remain for ").append(getDurationForPower(5));
//    sb.append(". However, the priest could move one point of power from duration to creature type, and also move 1 point of swarm size to creature type, resulting in ");
//    sb.append(getAreaDiameterForPower(4)).append(" hex diameter swarm of ").append(getCreatureTypesForPower(7).replaceAll(",", " or "));
//    sb.append(" that remain for ").append(getDurationForPower(4)).append(".");
      return sb.toString();
   }
   private static String getDamageChanceForPower(int power) {
      switch (power) {
         case  1: return "---";
         case  2: return "---";
         case  3: return "1/20";
         case  4: return "1/12";
         case  5: return "1/10";
         case  6: return "1/8";
         case  7: return "1/6";
         case  8: return "1/4";
         case  9: return "2/6";
         case 10: return "2/4";
      }
      return "---";
   }
   private static String getCreatureTypesForPower(int power) {
      switch (power) {
         case  1: return "Beetles, Butterflies";
         case  2: return "Flies, Frogs";
         case  3: return "Locust";
         case  4: return "Ants, Mice";
         case  5: return "Scorpions, Birds";
         case  6: return "Bees";
         case  7: return "Rats, Bats";
         case  8: return "Snakes";
         case  9: return "Poisonous snakes";
         case 10: return "GMs discretion";
      }
      return "";
   }
   private static String getDurationForPower(int power) {
      switch (power) {
         case 1: return "5 minutes (10 turns)";
         case 2: return "10 minutes (20 turns)";
         case 3: return "30 minutes (60 turns)";
         case 4: return "1 hour";
         case 5: return "5 hours";
         case 6: return "1 day";
         case 7: return "1 week";
         case 8: return "1 month";
      }
      return "";
   }

}
