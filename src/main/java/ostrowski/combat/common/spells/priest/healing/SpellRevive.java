/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.healing;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.server.Arena;

public class SpellRevive extends PriestSpell implements ICastInBattle
{
   public static final String NAME = "Revive";
   public SpellRevive() {}

   public SpellRevive(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }
   public SpellRevive(String name, Class<? extends IPriestGroup> group, int affinity) {
      super(name, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getCasterName() + "'s " + getName() + " spell revives " + getTargetName();
      }
      return null;
   }
   @Override
   public String describeSpell() {
      StringBuilder sb = new StringBuilder();
      sb.append("The '").append(getName()).append("' spell revives an unconscious or sleeping subject.");
      sb.append(" To be effective, the caster needs only one point of effective power.");
      sb.append(" The subject will remain awake until they are again knocked out, or fall asleep naturally.");
      sb.append(" If the subject has more than 10 wounds, they will fall back unconscious after 1 turn.");
      sb.append(" For each point over 1, then subject will remain awake for twice as long. So at 3 power, the subject will stay awake for 4 turns.");
      sb.append(" At 4 power, the subject will stay awake for 8 turns, and so on.");
      return sb.toString();
   }
   @Override
   public void applyEffects(Arena arena) {
      getTarget().awaken();
   }
   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_UNCONSIOUS;
   }
}
