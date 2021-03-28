package ostrowski.combat.common.spells.priest.evil;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;
import ostrowski.combat.server.Arena;

public class SpellPossession extends ResistedPriestSpell implements ICastInBattle
{
   private byte previousTeamID;
   public static final String NAME = "Possession";
   public SpellPossession() {
   }
   public SpellPossession(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, Attribute.Intelligence, (byte)3/*resistedActions*/, (short)20/*baseExpirationTimeInTurns*/, (short) 10/*bonusTimeInTurnsPerPower*/, group, affinity);
   }


   @Override
   public String describeEffects(Character defender, boolean firstTime) {
//      if (firstTime)
//         return getTargetName() + " falls under the control of " + getCasterName() + " 'Mind Control' spell.";
//      return " is under the control of " + getCasterName() +" 'Mind Control' spell.";
      if (firstTime) {
         return getTargetName() + " becomes an ally to " + getCasterName() + ", and is now on team " + TEAM_NAMES[caster.teamID] + ".";
      }
      return "(currently on team " + TEAM_NAMES[caster.teamID] + ")";
   }

   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to enter the mind of the subject and control him completely. "+
             " While in control of the subject, the caster can see and feel anything that the subject would. " +
             " The caster will have attributes of the subject, except for IQ and SOC (with respect to casting, not appearance). " +
             " The caster also maintains his own skill set and knowledge base, and does not gain any knowledge held by the subject. " +
             " The caster will experience and be affected by any pain endured by the subject's body. "+
             " The subject will be conscious of their possession, and everything done by the caster, but unable to do anything. " +
             " If the subject's body suffers pain, the caster's concentration may be broken, causing the subject to regain control of his own body. " +
             " Once the subject's pain level exceeds the amount by which the casting succeeded, the spell is broken. " +
             " The caster will be unaware of his own body while in possession of the subject's body. " +
             " The caster's body will appear to be in a deep coma during the possession, and will collapse to the ground if standing at spell time. " +
             " If the caster is killed while the caster is in possession, the caster remains in the subject's body, until the spell expires, " +
             "or until the caster loses concentration for pain. If the caster's body is dead when he leaves the subject, he dies too. ";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OTHER_FIGHTING;
   }

   @Override
   public void applyEffects(Arena arena) {
      previousTeamID = target.teamID;
      target.teamID = caster.teamID;
      target.targetID = -1;
      arena.recomputeAllTargets(target);
   }

   @Override
   public void removeEffects(Arena arena) {
      target.teamID = previousTeamID;
      arena.recomputeAllTargets(target);
   }

}
