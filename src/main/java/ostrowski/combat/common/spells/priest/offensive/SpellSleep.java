package ostrowski.combat.common.spells.priest.offensive;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.server.Arena;

public class SpellSleep extends ResistedPriestSpell implements ICastInBattle
{
   public static final String NAME = "Sleep";
   public SpellSleep() {}

   public SpellSleep(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, Attribute.Nimbleness, (byte)3/*resistedActions*/, true/*expires*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTargetName() + " falls asleep from "+getCasterName()+"'s sleep spell.";
      }
      return getTargetName() + " is still asleep from "+getCasterName()+"'s sleep spell.";
   }

   @Override
   public String describeSpell()  {
      return "The '" + getName() + "' spell makes the subject fall asleep.";
   }

   @Override
   public void applyEffects(Arena arena) {
      getTarget().applyWound(new Wound(getPower(),
                                       Location.BODY,
                                       getName() + " spell",
                                       0,//painLevel
                                       0,//wounds
                                       0,//bleedRate
                                       0,//armPenalty
                                       0,//movePenalty
                                       0,//knockedDownDist,
                                       DamageType.GENERAL,
                                       EFFECT_KNOCKOUT,//effectMask,
                                       getTarget()), arena);
   }

}
