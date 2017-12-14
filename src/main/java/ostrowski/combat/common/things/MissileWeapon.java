/*
 * Created on Jul 10, 2006
 *
 */
package ostrowski.combat.common.things;

import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.weaponStyles.WeaponStyle;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackMissile;


public class MissileWeapon extends Weapon
{
   public MissileWeapon() {}
   public MissileWeapon(int size, Race racialBase, double weight, int cost, String name,
                        SkillType skillType, int minSkill, int skillPenalty, int damageMod,
                        DieType varianceDie, DamageType damageType, int rangeBase,
                        int handsRequired, String[] preparationSteps)
   {
      this(size, racialBase, weight, cost, name, skillType, minSkill, skillPenalty, damageMod,
           DiceSet.getSingleDie(varianceDie), damageType, rangeBase, handsRequired, preparationSteps);
   }
   public MissileWeapon(int size, Race racialBase, double weight, int cost, String name,
                        SkillType skillType, int minSkill, int skillPenalty, int damageMod,
                        DiceSet varianceDice, DamageType damageType, int rangeBase,
                        int handsRequired, String[] preparationSteps)
   {
      super(size, racialBase, weight, cost, name,
            new WeaponStyle[] {new WeaponStyleAttackMissile(skillType, minSkill, skillPenalty,
                                                            damageMod, varianceDice, damageType,
                                                            rangeBase, handsRequired, preparationSteps)});
   }
   public MissileWeapon(int size, Race racialBase, double weight, int cost, String name, WeaponStyle style)
   {
      super(size, racialBase, weight, cost, name, new WeaponStyle[] {style});
   }

   @Override
   public Object clone()
   {
      return new MissileWeapon(_size, getRacialBase(), getWeight(), getCost(), getName(), _attackStyles[0]);
   }

   @Override
   public boolean isMissileWeapon()
   {
      return true;
   }
   public RANGE getRange(short distanceToTarget, byte adjustedStrength)
   {
      return ((WeaponStyleAttackMissile) _attackStyles[0]).getRangeForDistance(distanceToTarget, adjustedStrength);
   }

   @Override
   public void copyData(Thing source) {
      super.copyData(source);
   }
}
