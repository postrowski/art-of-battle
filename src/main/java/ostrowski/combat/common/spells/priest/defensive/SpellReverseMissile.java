/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.defensive;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.InstantaneousPriestSpell;
import ostrowski.combat.server.Arena;

public class SpellReverseMissile extends InstantaneousPriestSpell
{
   public static final String NAME = "Reverse Missile";
   public SpellReverseMissile() {
   }
   public SpellReverseMissile(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      Table table = new Table();
      table.addRow(new TableRow(-1, "Effective Power", "Max. missile size"));
      for (int p=1 ; p<=3 ; p++) {
         table.addRow(new TableRow(p-1).addHeader("" + p)
                                       .addTD(new TableData(getDescriptionForpower(p)).setAlignLeft()));
      }
      return "The '" + getName() + "' spell is an instantaneous spell that allows the caster to reverse " +
              "the flight of any missile, and cause it to return to the sender." +
              " The power required to reverse a missile is dependent upon the size of the missile fired or thrown at the caster." +
              " The priest's affinity, and the actions spent on casting determines how likely the missile is to hit the sender of the missile."+
              " The caster rolls an attack roll using his or her NIM attribute, minus 8, instead of DEX, and for each level of effective power, add an additional +3 to the to-hit roll." +
              " The caster adds in his/her affinity level to his to-hit roll." +
              " So a 3-point effective power spell would use NIM - 8 + 9 + Divine Affinity, resulting in a NIM + 1 + Divine Affinity + d10±." +
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
   public void applyEffects(Arena arena) {
      // TODO: missile attacks firer, priest's NIM to-hit roll (at actions of spell)
   }
   @Override
   public void removeEffects(Arena arena) {
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_SELF;
   }


   @Override
   public boolean canDefendAgainstRangedAttacks() {
      return true;
   }


}
