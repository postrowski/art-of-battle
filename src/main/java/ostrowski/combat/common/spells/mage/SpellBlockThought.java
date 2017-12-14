/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;

public class SpellBlockThought extends ResistedMageSpell
{
   public static final String NAME = "Block Thought";
   public SpellBlockThought() {
      super(NAME, Attribute.Intelligence, (byte)2, true, new Class[] {}, new MageCollege[] {MageCollege.DIVINATION, MageCollege.ENCHANTMENT});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell prevents the subject of the spell from thinking about a particular subject.";
   }

}
