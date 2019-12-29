/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.SpecialDamage;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.common.wounds.WoundChart;

public class SpellShockingGrasp extends ExpiringMageSpell implements ICastInBattle
{
   public static final String NAME = "Shocking Grasp";
   public SpellShockingGrasp() {
      super(NAME, (short)10/*baseExpirationTimeInTurns*/, (short)5/*bonusTimeInTurnsPerPower*/,
            new Class[] {SpellControlCharge.class, SpellCreateElectricity.class},
            new MageCollege[] {MageCollege.ENERGY});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      StringBuilder sb = new StringBuilder();
      if (firstTime) {
         sb.append(getTargetName()).append("'s ");
      }
      sb.append("hands");
      if (firstTime) {
         sb.append(" start to spark with a ");
      }
      else {
         sb.append(" is sparking with a ");
      }
      sb.append(getPower()).append("-point electric field (");
      DiceSet damageDice = getDamage(getPower());
      sb.append(damageDice).append(" electric damage)");
      return sb.toString();
   }
   private static DiceSet getDamage(byte power) {
      return new DiceSet(0/*d1*/,0/*d4*/,power/*d6*/,0/*d8*/,0/*d10*/,0/*d12*/,0/*d20*/, 0/*dBell*/, 1.0);
   }

   @Override
   public String describeSpell() {
      StringBuilder sb = new StringBuilder();
      sb.append("The '").append(getName()).append("' spell causes the caster's hands to shimmer in electricity.");
      sb.append(" Any time the caster touches another person (friend or foe) with their hand, ");
      sb.append("the electricity sparks to that person, causing 1d6 of electric damage per point of power put into the spell. ");
      sb.append(" The touch of the caster may be dodged, or retreated from, but blocking or parrying the touch will still ");
      sb.append("trigger the electric damage, though the block or parry may prevent any damage from the punch itself.");
      sb.append(" If the caster's touch results in any damage (from a punch, for instance), the damage done from this spell causes its own separate wound. ");
      sb.append(" Once the spell sparks to another person, it fades away and is lost. ");
      sb.append(" If the caster does not touch anyone in a round, the caster must spend one action maintaining the spell, ");
      sb.append("or it will fade away harmlessly.");
      return sb.toString();
   }

   @Override
   public Wound modifyDamageDealt(Wound wound, Character attacker, Character defender, String attackingWeaponName, StringBuilder modificationsExplanation) {
      if (!attackingWeaponName.equals(Weapon.NAME_Claws) &&
          !attackingWeaponName.equals(Weapon.NAME_Punch) &&
          !attackingWeaponName.equals(Weapon.NAME_KarateKick) &&
          !attackingWeaponName.equals(Weapon.NAME_Tusks) &&
          !attackingWeaponName.equals(Weapon.NAME_HeadButt) &&
          !attackingWeaponName.equals(Weapon.NAME_HornGore) &&
          !attackingWeaponName.equals(Weapon.NAME_SturgeBeak) &&
          !attackingWeaponName.equals(Weapon.NAME_Fangs)) {
          return wound;
      }
      DiceSet damageDice = getDamage(getPower());
      damageDice = attacker.adjustDieRoll(damageDice, RollType.DAMAGE_SPELL, attacker/*target*/);
      modificationsExplanation.append("The 'Shocking Grasp' spell does ").append(damageDice).append(" electric damage.<br/>");
      byte damageLevel = (byte) damageDice.roll(true/*allowExplodes*/);
      byte bldElectric = defender.getBuild(DamageType.ELECTRIC);
      modificationsExplanation.append(attacker.getName()).append(" rolls ").append(damageDice.getLastDieRoll()).append("<br/>");
      modificationsExplanation.append(defender.getName()).append("'s build-electric is ").append(bldElectric);
      damageLevel -= bldElectric ;
      if (damageLevel < 0) {
         modificationsExplanation.append(" which negates all electric damage.");
      }
      else {
         StringBuilder alterationExplanationBuffer = new StringBuilder();
         Wound electricWound = WoundChart.getWound(damageLevel, DamageType.ELECTRIC, defender, alterationExplanationBuffer);
         if (alterationExplanationBuffer.length() > 0) {
            modificationsExplanation.append(" and is further reduced by ").append(alterationExplanationBuffer);
         }
         modificationsExplanation.append(" which reduces the damage to ").append(electricWound.describeWound());

         if (wound == null ) {
            wound = electricWound;
         }
         else {
            SpecialDamage specialDamageModifier = new SpecialDamage(SpecialDamage.MOD_NONE);
            specialDamageModifier.setPainModifier(electricWound.getPain());
            specialDamageModifier.setWoundModifier(electricWound.getWounds());
            wound.setSpecialDamageModifier(specialDamageModifier);
            wound.setEffectsMask(electricWound.getEffectsMask() | wound.getEffectsMask());
         }
      }
      // expire the spell once its cast:
      _duration = -1;

      return wound;
   }

   @Override
   public boolean isBeneficial() {
      return true;
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_SELF;
   }

}
