/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.good;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;

public class SpellDetectLies extends ResistedPriestSpell
{
   public static final String NAME = "Detect Lies";
   public SpellDetectLies() {
   }
   public SpellDetectLies(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, Attribute.Social, (byte)1/*resistedActions*/, true/*expires*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to determine if the subject of spell is telling a lie." +
             " The spell will only determine if the subject knows they are telling a lie." +
             " It will not detect if someone is incorrect about their statements, only if they believe it to be the truth."+
             " The more power put into the spell, the harder the spell is to resist.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
