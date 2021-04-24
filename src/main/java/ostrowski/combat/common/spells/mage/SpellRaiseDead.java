/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;

public class SpellRaiseDead extends MageSpell
{
   public static final String NAME = "Raise Dead";
   public SpellRaiseDead() {
      super(NAME, new Class[] {SpellControlMind.class, SpellResistPain.class},
                  new SkillType[] {SkillType.Spellcasting_Necromancy});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      Table table = new Table();
      table.addRow(new TableRow(-1, "Power&nbsp;points<br/>spent&nbsp;on&nbsp;duration", "Duration"));
      for (int p=0 ; p<=7 ; p++) {
         table.addRow(new TableRow(p).addHeader("" + p)
                                     .addTD(new TableData(getDurationForpower(p)).setAlignLeft()));
      }

      return "The Raise Dead spell reanimates one corpse for one hour."
             + " The zombie will obey the commands of its creator,"
             + " even to the point of its own destruction."
             + " Zombies do not feel pain but are impeded by wounds the same as living creatures are."
             + " If the corpse used to create a zombie is missing limbs,"
             + " the zombie will also be missing the limbs, and will be hampered appropriately."
             + " A zombie is dispelled when its wounds level reaches 10, or the spell expires."
             + " The attributes of the zombie will be equal to that of the creature when it was alive,"
             + " except for IQ, which will be 5 points lower."
             + " However, no attribute level can be higher than the power points put into the"
             + " spell specifically for attributes and profession levels. "
             + " Similarly, the skill ranks and profession levels of the zombie will be equal to their living level, "
             + "but can not exceed the power points of the spell for attributes and skills times two."
             + " Zombies normally stay around for 1 hour; however, power points may be allocated"
             + " away from profession levels and attributes to prolong the zombie’s existence."
             + " For example, given the corpse of a fallen warrior who had a STR of 4 and a DEX of 2,"
             + " and a fighter profession level of 5: A necromancer casting this spell with 3 power points"
             + " (all allocated to attributes and profession levels) would raise this zombie with its STR at 3,"
             + " and DEX at 2, and fighter profession level of 5 for 1 hour."
             + " The same caster could instead spend two of his power points on duration,"
             + " raising the zombie with a STR and DEX of 1 and fighter profession level of 2 for 1 day."
             + table
             + " When animating large creatures, such as ogres, trolls, giants, etc.,"
             + " the caster must spend 1 extra power point for every full 4 points of size over"
             + " the caster's size. For example, to raise an ogre (racial size adjuster of +10),"
             + " a caster must spend 2 points to allow for the size."
             + " So, if a human necromancy spent 3 power points to raise an ogre,"
             + " the Ogre’s maximum attribute level would be a 1, and its maximum profession level 2,"
             + " and it could only stay around for 1 hour."
             + "Note: While the maximum STR of this ogre zombie is 1, its ASTR could still be as high as 11, "
             + "due to its large size."
             + " Similarly, the ogre zombie's BLD will still be 10 points higher than its HT attribute."
             + " A zombie can not have more skills within a profession level than the profession's level, so if its profession level is lowered,"
             + " the zombie may completely lose skills, starting with the non-proficient skills, and others determined by the GM."
             + " \r\n";
   }

   private static String getDurationForpower(int p) {
      switch (p) {
         case 0: return "1 hour";
         case 1: return "5 hours";
         case 2: return "1 day";
         case 3: return "1 week";
         case 4: return "1 month";
         case 5: return "6 months";
         case 6: return "3 years";
         case 7: return "15 years";
         case 8: return "permanent";
      }
      return "";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE;
   }

}
