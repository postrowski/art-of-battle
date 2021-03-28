/*
 * Created on May 13, 2007
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.IHolder;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestGrapplingHoldMaintain;
import ostrowski.combat.server.Arena;

public class SpellSpiderWeb extends MageSpell implements IHolder, IRangedSpell, ICastInBattle
{
   public byte                holdReductionAmount = 0;
   public static final String NAME                = "Spider Web";

   public SpellSpiderWeb() {
      super(NAME, new Class[] { SpellCreateEarth.class, SpellCreateRope.class, SpellCreateForce.class, SpellGlue.class, SpellThrowSpell.class},
            new MageCollege[] { MageCollege.CONJURATION, MageCollege.EARTH, MageCollege.ENERGY});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         StringBuilder sb = new StringBuilder();
         sb.append(getCasterName()).append(" creates a spider web with a hold level of ").append(getHoldingLevel());
         sb.append(" (power level ").append(getPower());
         sb.append(" times two, plus bonus damage of ").append(excessSuccess / 2);
         byte casterSize = (caster == null) ? 0 : caster.getRace().getBuildModifier();
         byte targetSize = (target == null) ? 0 : (byte) (-1 * target.getRace().getBuildModifier());

         if ((targetSize != 0) || (casterSize != 0)) {
            sb.append(", ");
            if (casterSize != 0) {
               if (casterSize > 0) {
                  sb.append("+");
               }
               sb.append(casterSize).append(" for caster's size");
            }
            if ((targetSize != 0) && (casterSize != 0)) {
               sb.append(" and ");
            }
            if (targetSize != 0) {
               if (targetSize > 0) {
                  sb.append("+");
               }
               sb.append(targetSize).append(" for target's size");
            }
         }
         sb.append(").");
         return sb.toString();
      }
      return null;
   }

   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell creates a sticky spider web that is shot out of the caster's hands."
             + " This web can hit any target like any missile spell, however it does no actual damage."
             + " Instead, if the target is hit, they become held as if grappled." + " The hold level of the grab is equal to two times the power of the spell,"
             + " plus one point for every two full point over the defense TN of the target (skill-bonus damage)."
             + " The hold level is further adjusted by the sizes of the caster and target:"
             + " The caster adds his/her racial size adjuster, and the target subtracts his/her racial size adjuster to the final hold level."
             + " The target may break free by rolling against their Strength, with as many actions as they choose to use."
             + " A 1-action break free attempt uses STR-5, a 2-action break free attempt uses STR, and a 3-action attempt uses STR+5."
             + " This break-free roll is against a TN equal to the hold level of the web."
             + " While held by the web, the target may attack and defend, but at a penalty equal to the hold level."
             + " The target of the spell has his or her movement reduced by the hold level."
             + " If the hold level exceeds the movement allowed for the target, the target is stuck at their current location until they break free.";
   }

   @Override
   public void applyEffects(Arena arena) {
      if (excessSuccess >= 0) {
         getTarget().setHoldLevel(this, getHoldingLevel());
      }
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE_FIGHTING;
   }

   @Override
   public void applyHoldMaintenance(RequestGrapplingHoldMaintain grappleMaintain, Arena arena) {
   }

   @Override
   public RequestGrapplingHoldMaintain getGrapplingHoldMaintain(Character actor, RequestAction action, Arena arena) {
      RequestGrapplingHoldMaintain holdMaintain = new RequestGrapplingHoldMaintain();
      holdMaintain.addMaintainHoldOptions(0, this, getTarget(), 0/*skill*/, (byte) 0/*holderPainPenalty*/);
      holdMaintain.setAnswerID(0);
      return holdMaintain;
   }

   @Override
   public Character getHoldTarget() {
      return getTarget();
   }

   @Override
   public Byte getHoldingLevel() {
      byte casterSize = caster == null ? 0 : caster.getRace().getBuildModifier();
      byte targetSize = target == null ? 0 : target.getRace().getBuildModifier();
      return (byte) (((((getPower() * 2) + (byte) (excessSuccess / 2)) - holdReductionAmount) + casterSize) - targetSize);
   }

   public boolean reduceHoldingLevel(byte reductionAmount) {
      holdReductionAmount += reductionAmount;
      return getHoldingLevel() <= 0;
   }

   @Override
   public void setHoldTarget(Character holdTarget) {
      // holdTarget == null when the target breaks free of the hold
      if ((holdTarget != null) && (holdTarget != getTarget())) {
         DebugBreak.debugBreak();
      }
   }

   @Override
   public byte getAdjustedStrength() {
      return getCaster().getRace().getBuildModifier();
   }

   @Override
   public boolean isDefendable() {
      return true;
   }

   @Override
   public short getRangeBase() {
      return 8;
   }

   @Override
   public short getMaxRange(Character caster) {
      return (short) (getAdjustedRangeBase() * 4);
   }

   @Override
   public short getMinRange(Character caster) {
      return 0;
   }

   @Override
   public byte getRangeDefenseAdjustmentToPD(short distanceInHexes) {
      return Rules.getRangeDefenseAdjustmentToPD(getRange(distanceInHexes));
   }

   @Override
   public byte getRangeDefenseAdjustmentPerAction(short distanceInHexes) {
      return Rules.getRangeDefenseAdjustmentPerAction(getRange(distanceInHexes));
   }

   @Override
   public byte getRangeTNAdjustment(short distanceInHexes) {
      // The Range TN is already accounted for in the defender's TN.
      // so this should return 0
      //return (byte) (0-Rules.getRangeToHitAdjustment(getRange(distanceInHexes)));
      return 0;
   }
}
