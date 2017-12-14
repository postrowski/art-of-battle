package ostrowski.combat.common.spells;

import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Enums.TargetType;
import ostrowski.combat.common.things.MissileWeapon;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackRanged;

public interface IMissileSpell extends IRangedSpell
{
   public String getMissileWeaponName();
   public int    getMissileWeaponSize();
   public int    getMissileWeaponSkillPenalty();
   public int    getHandsRequired();
   public String explainDamage();
   public byte   getSpellDamageBase();
   public MissileWeapon getMissileWeapon();
   public WeaponStyleAttackRanged getWeaponStyleAttackRanged();
   public TargetType getTargetType();
   public DiceSet getDamageDice();
   public DamageType getDamageType();
}
