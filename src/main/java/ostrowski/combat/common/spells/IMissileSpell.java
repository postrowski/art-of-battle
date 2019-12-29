package ostrowski.combat.common.spells;

import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Enums.TargetType;
import ostrowski.combat.common.things.MissileWeapon;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackRanged;

public interface IMissileSpell extends IRangedSpell
{
   String getMissileWeaponName();
   int    getMissileWeaponSize();
   int    getMissileWeaponSkillPenalty();
   int    getHandsRequired();
   String explainDamage();
   byte   getSpellDamageBase();
   MissileWeapon getMissileWeapon();
   WeaponStyleAttackRanged getWeaponStyleAttackRanged();
   TargetType getTargetType();
   DiceSet getDamageDice();
   DamageType getDamageType();
}
