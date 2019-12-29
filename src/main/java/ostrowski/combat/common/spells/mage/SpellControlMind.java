/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.server.Arena;

/**
 * This spell causes the subject do what the caster asks of him. The power level of
 * spells determines what the caster can force the subject to do:
 *    Level 1: Subject may be given simple suggestions that do not conflict with their own desires
 *          examples: "tie your shoe", "open the door", "drink the coffee"
 *    Level 2: Subject may be given complex suggestions that do not conflict with their own desires
 *          examples: "unlock the cell door", "These are not the droids you're looking for", "remove your armor"
 *    Level 3: Subject may be given commands that conflict somewhat with their own desires
 *          examples: "Don't attack me", "Tell your boss you quit", "Give me your wallet"
 *    Level 4: Subject may be given any command that does not risk their own person safety
 *          examples: "release the prisoner", "", ""
 *    Level 5: Subject may be forced to do anything that does not immediately end their own life.
 *          examples: "Attack your friend", "open the gates to let the enemy in"
 */
public class SpellControlMind extends ResistedMageSpell
{
   public static final String NAME = "Control Mind";
   public SpellControlMind() {
      super(NAME, Attribute.Intelligence, (byte) 3/*resistedActions*/, true/*expires*/,
            new Class[] {SpellBlockThought.class, SpellSuggestion.class},
            new MageCollege[] {MageCollege.DIVINATION, MageCollege.ENCHANTMENT});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTargetName() + " falls under the control of " + getCasterName() + " 'Mind Control' spell.";
      }
      return " is under the control of " + getCasterName() +" 'Mind Control' spell.";
   }
   @Override
   public String describeSpell() {
      Table table = new Table();
      table.addRow(new TableRow(-1, "Power", "Control level"));
      for (int p=1 ; p<=5 ; p++) {
         table.addRow(new TableRow(p-1).addHeader("" + p)
                                       .addTD(new TableData(getDescriptionForpower(p)).setAlignLeft()));
      }
      return "The '" + getName() + "' spell allows the caster to control the thoughts and or actions of the subject. "+
              " The power put into the spell determines the level of mind control:" +
             table;
   }

   private static String getDescriptionForpower(int p) {
      switch (p) {
         case 1: return "Subject may be given simple suggestions that do not conflict with their own desires<br/>" +
                        "Examples: \"Tie your shoe\", \"Open the door\", \"Drink the coffee\"";
         case 2: return "Subject may be given complex suggestions that do not conflict with their own desires<br/>" +
                        "Examples: \"Unlock the cell door\", \"These are not the droids you're looking for\", \"Remove your armor\"";
         case 3: return "Subject may be given commands that conflict somewhat with their own desires<br/>" +
                        "Examples: \"Don't attack me\", \"Tell your boss you quit\", \"Give me your wallet\"";
         case 4: return "Subject may be given any command that does not risk their own person safety<br/>" +
                        "Examples: \"Release the prisoner\", \"Set fire to your house\"";
         case 5: return "Subject may be forced to do anything that does not immediately end their own life.<br/>" +
                        "Examples: \"Attack your friend\", \"Open the gates to let the enemy in\"";
      }
      return "";
   }

   @Override
   public void applyEffects(Arena arena) {
   }
   @Override
   public void removeEffects(Arena arena) {
   }
}
