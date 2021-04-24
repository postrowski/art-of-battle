/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.SkillType;

public class SpellIllusion extends ResistedMageSpell
{
   public static final String NAME = "Illusion";
   public SpellIllusion() {
      super(NAME, Attribute.Intelligence, (byte) 2/*resistedActions*/, true/*expires*/,
            new Class[] {SpellCreateSound.class, SpellCreateLight.class},
            new SkillType[] {SkillType.Spellcasting_Illusion});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell creates a visual scene dictated by the caster."
               + " The caster must be familiar with the images or creatures he is creating."
               + " A single point creates a static silent image, covering a circle of hexes"
               + " with a radius of 2 (7 hexes total) that would last for 10 turns."
               + " Each additional point put into the spell, allows the caster to add complexity as follows:<ul>"
               +    "<li> Add sounds.</li>"
               +    "<li> Create movement of a single distinct characters within the scene. Caster must concentrate to move characters.</li>"
               +    "<li> Allow creations to move without constant concentration, but still at direction of caster.</li>"
               +    "<li> Quadruple the number of moving elements.</li>"
               +    "<li> Quadruple the radius of the affected area.</li>"
               +    "<li> Double the duration of the spell.</li> </ul>"
               + " So, to cast an illusion of 3 Orcs walking down a hall under direct control,"
               + " the caster must spend 4 power points (1 for the base illusion, 1 to give it sound,"
               + " 1 to make it move, and 1 point to get up to 4 distinct orcs)."
               + " If the casting is out of place, or surprising, the viewer may be allowed resist"
               + " the effects by making an IQ check. During any turn in which they try to disbelieve"
               + " the illusion, they may not take any action that would require belief in the scene’s"
               + " existence (such as attacking or dodging the image.) The images and sounds are real,"
               + " and would show up on electronic surveillance devices, but the illusion is not capable"
               + " of any physical interaction.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_AREA;
   }

}
