package ostrowski.combat.common.spells.priest.evil;

import ostrowski.combat.common.Advantage;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;
import ostrowski.combat.server.Arena;

public class SpellEnrage extends ResistedPriestSpell implements ICastInBattle
{
   public static final String NAME = "Enrage";
   public SpellEnrage() {
   }
   public SpellEnrage(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, Attribute.Intelligence, (byte)2/*resistedActions*/, true/*expires*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTargetName() + " is sent into a berserking rage from " + getCasterName() + "'s 'Enrage' spell.";
      }
      return getTargetName() + " is in a berserking rage from " + getCasterName() + "'s 'Enrage' spell.";
   }

   @Override
   public DiceSet getResistanceDice(Character target)
   {
      DiceSet dice = super.getResistanceDice(target);
      int penalty = 0;
      if (getTarget().hasAdvantage(Advantage.BAD_TEMPER)) {
         penalty += 2;
      }
      if (getTarget().hasAdvantage(Advantage.BERSERKER)) {
         penalty += 2;
      }
      if (penalty == 0) {
         return dice;
      }
      return dice.addBonus(-penalty);
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell causes the subject to enter a berserking state, unless they make their resistance roll." +
             " If the subject has either a 'Bad Temper' or a 'Berserker' disadvantage, they suffer a -2 penalty to their resistance roll (-4 if they have both disadvantages)." +
             " The berserking state will last for 3 turns for every point by which they fail their resistance roll.";
   }

   @Override
   public void applyEffects(Arena arena) {
      if (_castingEffectiveness >= 0) {
         _duration = (short) ((_castingEffectiveness+1) * 3);
      }
      getTarget().setIsBerserking(true);
   }
   @Override
   public void removeEffects(Arena arena) {
      getTarget().setIsBerserking(false);
   }

}
