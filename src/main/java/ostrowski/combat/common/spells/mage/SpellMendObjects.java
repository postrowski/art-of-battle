/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

public class SpellMendObjects extends MageSpell
{
   public static final String NAME = "Mend Objects";
   public SpellMendObjects() {
      super(NAME, new Class[] {SpellCreateForce.class}, new MageCollege[] {MageCollege.ENERGY, MageCollege.EARTH});
   }

   @Override
   public String describeSpell() {
      return "The '" + getName() + "' allows the caster to attach two objects together." +
              " Objects that fit together perfectly, such as reconnecting a broken object, are easy to mend." +
              " Objects that normally don't fit together require more power to connect." +
              " Larger breaks require more power to connect together.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
