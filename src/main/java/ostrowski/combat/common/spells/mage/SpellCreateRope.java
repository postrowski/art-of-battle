/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableHeader;
import ostrowski.combat.common.html.TableRow;


public class SpellCreateRope extends MageSpell
{
   public static final String NAME = "Create Rope";
   public SpellCreateRope() {
      super(NAME, new Class[] {SpellCreateEarth.class}, new SkillType[] {SkillType.Spellcasting_Conjuration, SkillType.Spellcasting_Earth});
   }

   @Override
   public String describeSpell() {
      StringBuilder sb = new StringBuilder();
      sb.append("The '").append(getName()).append("' spell allows the caster to create a rope for any desired purpose.");
      sb.append(" The length and strength of the rope are determined by the power put into the spell.");
      sb.append(" A 1-point rope will be a string about 10 feet in length that will hold 25 lbs. of weight.");
      sb.append(" Each additional point put into the spell can either:");
      sb.append(" <UL>");
      sb.append("  <LI>Quadruple the length of the rope");
      sb.append("  <LI>Quadruple the strength of the rope");
      sb.append("  <LI>Double the length and the strength of the rope");
      sb.append(" </UL>");
      sb.append(" The following table shows some common rope lengths, given the desired weight capacity, and the power of the spell:");
      Table table = new Table();
      table.addRow(new TableRow(-1, new TableHeader("Weight<br/>capacity").setRowSpan(2),
                                    new TableHeader("Spell power").setColSpan(6)));
      TableRow header2 = new TableRow(-1);
      table.addRow(header2);
      for (int power=1 ; power<=6 ; power++) {
         header2.addHeader(""+power);
      }

      int htmlRow=0;
      for (int weight=25 ; weight <= 1600 ; weight *= 2) {
         TableRow row = new TableRow(htmlRow++, ""+weight + " lbs.");
         for (int power=1 ; power<=6 ; power++) {
            double length = (10 * Math.pow(4.0, power-1) * 25.0)/weight;
            if (length < 5) {
               row.addTD("-");
            }
            else {
               if (length >= 1000) {
                  row.addTD("" + ((int) (length / 1000)) + "," +  + ((int) (length % 1000)) + "'");
               }
               else {
                  row.addTD("" + ((int) length) + "'");
               }
            }
         }
         table.addRow(row);
      }
      sb.append(table);
      return sb.toString();
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_AREA;
   }

}
