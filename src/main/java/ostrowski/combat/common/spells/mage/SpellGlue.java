/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;


import ostrowski.combat.common.enums.SkillType;

public class SpellGlue extends MageSpell
{
   public static final String NAME = "Glue";
   public SpellGlue() {
      super(NAME, new Class[] {SpellCreateForce.class}, new SkillType[] {SkillType.Spellcasting_Energy, SkillType.Spellcasting_Earth});
   }

   @Override
   public String describeSpell() {
      return "The '" + getName() + "' creates a sticky film on the target."
             + " The more power put into the spell, the stickier the glue will be, and the longer it will last."
             + " Characters or objects that touch the glue may become stuck, based upon their size, strength, and"
             + " how much surface area they came into direct contact with.";
   }

}
