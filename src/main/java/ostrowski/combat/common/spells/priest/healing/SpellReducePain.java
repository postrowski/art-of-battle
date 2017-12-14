/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.healing;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.server.Arena;

public class SpellReducePain extends PriestSpell
{
   public static final String NAME = "Reduce Pain";
   public SpellReducePain() {};
   public SpellReducePain(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getCasterName() + "'s reduce pain spell removes " + getPainReduction() + " points of pain from " + getTargetName();
      }
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell reduces the pain level of the subject by three points of pain for each point of effective power in the spell.";
   }
   @Override
   public Boolean isCastInBattle() {
      return true;
   }
   @Override
   public void applyEffects(Arena arena) {
      getTarget().applyWound(new Wound(getPower(),
                                  Location.BODY,
                                  getName() + " spell",
                                  0-getPainReduction(),//painLevel
                                  0,//wounds
                                  0,//bleedRate
                                  0,//armPenalty
                                  0,//movePenalty
                                  0,//knockedDownDist,
                                  DamageType.GENERAL,
                                  0,//effectMask,
                                  getTarget()), arena);
   }
   private byte getPainReduction() {
      return (byte)(getPower() *3);
   }
   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE_FIGHTING;
   }
}
