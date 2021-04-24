/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;

public class SpellInvisibility extends ExpiringMageSpell
{
   public static final String NAME = "Invisibility";
   public SpellInvisibility() {
      super(NAME, (short)10, (short)5,
            new Class[] {SpellControlLight.class, SpellBlur.class},
            new SkillType[] {SkillType.Spellcasting_Illusion, SkillType.Spellcasting_Protection});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "This spell makes the subject almost impossible to see."
               + " Casting the spell on oneself costs 3 power points."
               + " Casting it on someone else costs 4 power points,"
               + " plus 1 point for every racial size above the caster."
               + " Invisible characters have a slight blurring of their"
               + " outline that may be noticed by onlookers."
               + " Anyone looking in the characters direction can make"
               + " an IQ-5 roll to notice the blurring. If the character"
               + " is moving, the observer get a bonus equal to twice the"
               + " characters pace. Even if successful, the observer knows"
               + " only in general terms where the character is."
               + " The character’s PD is increased by 10."
               + " If the invisible subject makes a melee attack against someone,"
               + " their location will be partially known by the target,"
               + " so attacks against the subject are at +5."
               + " Invisible characters detectable by thermal vision and can be heard normally."
               + " If explicitly stated at casting time, an addition point may be spent"
               + " to prevent thermal visibility.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE;
   }

}
