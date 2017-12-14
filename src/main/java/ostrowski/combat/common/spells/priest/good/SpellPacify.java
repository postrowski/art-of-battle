package ostrowski.combat.common.spells.priest.good;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.Configuration;

public class SpellPacify extends ResistedPriestSpell
{
   public static final String NAME = "Pacify";
   public SpellPacify() {};
   public SpellPacify(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, Attribute.Intelligence, (byte)1/*resistedActions*/, true/*expires*/,group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         if (defender.isBerserking()) {
            return getTargetName() + " becomes somewhat paficied and is no longer berserking.";
         }
         return getTargetName() + " becomes paficied and will not longer attack anyone.";
      }
      return getTargetName() + " is still paficied and will not attack anyone.";
   }

   @Override
   public String describeSpell() {
      return "The '"+getName()+"' spell fills the subject with an overwhelming sense of peace." +
             " While under the effects of this spell, the subject is unable to take any action which he knows will cause harm to any individual." +
             " The subject will try and talk his or her way out any fight." +
             " If struck with a weapon, causing any pain at all, the subject may make another saving throw to break out of the control of this spell." +
             " If the subject is currently berserking, the spell is resisted at " +
             (Configuration._useExtendedDice ? " 2 actions (instead of the usual 1 action)" : " +5") +
             " and falling under the spell's effect does nothing more than bring the subject out of their berserking state.";
   }

   @Override
   public byte getResistanceActions() {
      if ((getTarget() != null) &&  (getTarget().isBerserking())) {
         return 2;
      }
      return super.getResistanceActions();
   }

   @Override
   public void applyEffects(Arena arena) {
      if (getTarget().isBerserking()) {
         Byte originalTeam = arena._battle._berserkingCharactersOriginalTeamID.get(getTarget()._uniqueID);
         if (originalTeam != null) {
            getTarget()._teamID = originalTeam.byteValue();
         }
         getTarget().setIsBerserking(false);
         getTarget().removeSpellFromActiveSpellsList(this);
      }
   }

   @Override
   public Boolean isCastInBattle() {
      return true;
   }

}
