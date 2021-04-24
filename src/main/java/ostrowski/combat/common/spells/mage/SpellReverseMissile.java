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

public class SpellReverseMissile extends InstantaneousMageSpell
{
   public static final String NAME = "Reverse Missile";
   public SpellReverseMissile() {
      super(NAME, new Class[] {SpellCreateForce.class, SpellDetectObject.class, SpellPush.class},
            new SkillType[] {SkillType.Spellcasting_Evocation, SkillType.Spellcasting_Energy, SkillType.Spellcasting_Divination, SkillType.Spellcasting_Protection});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      Table table = new Table();
      table.addRow(new TableRow(-1, "Power", "Max. missile size"));
      for (int p=1 ; p<=3 ; p++) {
         table.addRow(new TableRow(p-1).addHeader("" + p)
                                       .addTD(new TableData(getDescriptionForpower(p)).setAlignLeft()));
      }
      return "The '" + getName() + "' spell is an instantaneous spell that allows the caster to reverse " +
             "the flight of any missile, and cause it to return to the sender." +
             " The power required to reverse a missile is dependent upon the size of the missile fired or thrown at the caster." +
             " The skill of the 'Reverse Missile' spell, and the actions spent on casting determines how likely the missile is to hit the sender of the missile."+
             " The caster rolls an attack roll using his or her NIM attribute instead of DEX, for the actions spent on casting the spell." +
             " If one action is spent on the casting, the roll is NIM-5 + d10±. A 2 actions spell uses NIM + d10±, and a 3-action spell uses NIM + 5 + d10±. " +
             " The caster adds in his/her skill with the Reverse Missile spell to his to-hit roll." +
             " The firer of the missile may dodge or block the missile as if the caster had fired the missile himself." +
             " Range effects (modifications to TN and to defense effectiveness) in place for the returning missile are the same as they were for the initial firing of the missile." +
             table;
   }
   private static String getDescriptionForpower(int p) {
      switch (p) {
         case 1: return "Blow Darts<br/>Sling Rocks";
         case 2: return "Arrows<br/>Crossbow Bolts<br/>Thrown Knives";
         case 3: return "Thrown axes<br/>Spears<br/>Javelins";
      }
      return "";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_SELF;
   }

}
