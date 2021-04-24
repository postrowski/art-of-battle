/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.SpecialDamage;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.things.Hand;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Thing;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.common.weaponStyles.WeaponStyle;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackThrown;
import ostrowski.combat.server.Arena;

public class SpellFireball extends MageSpell implements ICastInBattle
{
   public static final String NAME = "Fireball";
   public SpellFireball() {
      super(NAME, new Class[] {SpellCreateFire.class, SpellControlFire.class,
                               SpellControlTemperature.class, SpellThrowSpell.class},
                               new SkillType[] {SkillType.Spellcasting_Conjuration, SkillType.Spellcasting_Fire});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         StringBuilder sb = new StringBuilder();
         sb.append(getCasterName()).append(" creates a fireball (level ").append(getPower());
         sb.append("), which will do 5 + 5 * ").append(getPower());
         int size = (caster == null) ? 0 : caster.getRace().getBuildModifier();
         if (size > 0) {
            sb.append(" +").append(size).append(" (racial size adjument)");
         }
         else if (size < 0) {
            sb.append(" ").append(size).append(" (racial size adjument)");
         }
         sb.append(" + 1d6 points of fire damage when thrown.");
         return sb.toString();
      }
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell creates an explosive ball of fire in the caster's hand, which should then be thrown (using the 'throwing' skill)." +
              "<br/>The damage the fireball does is equal to 5 + 5*(spell power) + 1d6. Damage type is fire." +
              " When cast by mage's that are larger or smaller than human, the damage done is modified by the caster's racial size adjuster." +
              "<br/>Nearby targets also take damage; for every hex away from the blast center, characters take 5" +
              " points less damage. Bonus damage does not apply to nearby targets." +
              "<br/>The range base for throwing the Fireball is 10.";
   }

   @Override
   public void applyEffects(Arena arena) {
      Hand hand = (Hand) getCaster().getLimb(LimbType.HAND_RIGHT);
      int damage = 5 + (getPower() * 5); // + getTarget().getRace().getBonusToBeHit();
      if (caster != null) {
         damage += caster.getRace().getBuildModifier();
      }
      String name;
      int power = getPower();
      switch (power) {
         case 1: name = "Small Fireball spell"; break;
         case 2: name = "Average Fireball spell"; break;
         case 3: name = "Large Fireball spell"; break;
         case 4: name = "Very large Fireball spell"; break;
         case 5: name = "Huge Fireball spell";  break;
         default:
            name = "Level-" + power + " Fireball spell";
      }
      WeaponStyleAttack style = new WeaponStyleAttackThrown(0/*minSkill*/, 0/*penalty*/, damage, DieType.D6, DamageType.FIRE, 12/*rangeBase*/,  1/*hands*/);
      // Give it a non-zero cost, so it will quality as being real
      Weapon fireball = new Weapon(-power/*size*/, getCaster().getRace(), 0/*lbs*/, 1/*$*/, name, new WeaponStyle[] {style});
      SpecialDamage specDam = new SpecialDamage(SpecialDamage.MOD_EXPLODING | SpecialDamage.MOD_FLAMING);
      fireball.setSpecialDamageModifier(specDam, "");
      if (!hand.setHeldThing(fireball, getCaster())) {
         String sb = getCasterName() +
                     " drops " +
                     getCaster().getHisHer() + " " +
                     getCaster().getLimb(LimbType.HAND_RIGHT).getHeldThingName() +
                     " in order to hold the created fireball.";
         arena.sendMessageTextToAllClients(sb, false/*popUp*/);
         Thing thingDropped = hand.dropThing();
         getCaster().getLimbLocation(hand.limbType, arena.getCombatMap()).addThing(thingDropped);
         hand.setHeldThing(fireball, getCaster());
      }
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

   @Override
   public boolean requiresTargetToCast() {
      return false;
   }
}
