package ostrowski.combat.common.things;

import ostrowski.combat.common.Race;
import ostrowski.combat.common.enums.AttackType;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.html.HtmlBuilder;
import ostrowski.combat.common.weaponStyles.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Weapons {
   static final SizelessWeapon[] specialWeaponsList;
   static final SizelessWeapon[] weaponsList;
   static {
      specialWeaponsList = new SizelessWeapon[] {
              //           size,lbs,  $, name                                              2                       (SkillType,       min,pen.,   StyleName,spd,Str-,Str+, dam,       Die,        damageType,       attackType,         charge type,  pry-,ranges hands)
              new WeaponBase(-1,0,   0, Weapon.NAME_HornGore,       new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling, 0, 0, "Horn Gore", 0, -99, 99, 3, DieType.D6, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Anytime, 0, 1, 1, 0)}),
              new WeaponBase(-1,0,   0, Weapon.NAME_Fangs,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling, 0, 0, "Bite", 0, -99, 99, 3, DieType.D6, DamageType.CUT, AttackType.THRUST, WeaponStyleAttack.Charge.With4Legs, 0, 0, 1, 0)}),
              new WeaponBase(-1,0,   0, Weapon.NAME_SturgeBeak,     new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling, 0, 0, "Beak", 0, -99, 99, 6, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.WhenMounted, 0, 0, 1, 0)}),
              new WeaponBase(-1,0,   0, Weapon.NAME_Tusks,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling, 0, 0, "Tusk slash", 0, -99, 99, 4, DieType.D4, DamageType.CUT, AttackType.THRUST, WeaponStyleAttack.Charge.With4Legs, 0, 1, 1, 0)}),
              new WeaponBase(-1,0,   0, Weapon.NAME_Claws,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling, 0, 0, "Claw", 0, -99, 99, 4, DieType.D6, DamageType.CUT, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 0, 1, 1)}),
              new WeaponBase(-1,0,   0, Weapon.NAME_TailStrike,     new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling, 0, 0, "Tail strike", 1, -99, 99, 2, DieType.D6, DamageType.BLUNT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, -2, 1, 0)}),
              };

      weaponsList = new SizelessWeapon[] {
              //           size,lbs,  $, name                                             2               (SkillType,                 min,pen.,   StyleName,spd,Str-,Str+, dam,  Die,            damageType,     attackType,      charge type, pry-,ranges hands)
              new WeaponBase(-1,0,   0, Weapon.NAME_KarateKick,     new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Karate, 3, 2, "Kick", 1, -99, 4, 2, DieType.D6, DamageType.BLUNT, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 1, 2, 0),
                                                                                new WeaponStyleAttackMelee(SkillType.Karate, 5, 4, "Spin kick", 1, -99, 4, 4, DieType.D6, DamageType.BLUNT, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 2, 0),
                                                                                new WeaponStyleAttackMelee(SkillType.Brawling, 3, 2, "Knee strike", 0, -2, 99, -2, DieType.D4, DamageType.BLUNT, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 0, 1, 0),
                                                                                new WeaponStyleAttackMelee(SkillType.Wrestling, 3, 2, "Knee strike", 0, -2, 99, -2, DieType.D4, DamageType.BLUNT, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 0, 1, 0)}),
              new WeaponBase(-1,0,   0, Weapon.NAME_HeadButt,       new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling, 1, 0, "Head Butt", 0, -99, 99, -3, DieType.D6, DamageType.BLUNT, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, -1, 1, 0)}),
              new WeaponBase(-1,0,   0, Weapon.NAME_Punch,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling, 1, 0, "Punch", 0, -2, 99, 0, DieType.D4, DamageType.BLUNT, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 0, 1, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.Boxing, 1, 0, "Punch", 0, -2, 99, 1, DieType.D4, DamageType.BLUNT, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 0, 1, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.Karate, 1, 0, "Punch", 0, -2, 99, 0, DieType.D4, DamageType.BLUNT, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 0, 1, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.Brawling, 3, 2, "Elbow strike", 0, -2, 99, -1, DieType.D4, DamageType.BLUNT, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, -1, 1, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.Wrestling, 3, 2, "Elbow strike", 0, -2, 99, -1, DieType.D4, DamageType.BLUNT, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, -1, 1, 1),
                                                                                new WeaponStyleCounterAttack(SkillType.Aikido, 3, 0, "Defensive Throw", 0, -2, 99, 0, DieType.D4, DamageType.BLUNT, AttackType.COUNTER_ATTACK, 0, 0, 2, 2),
                                                                                new WeaponStyleCounterAttack(SkillType.Aikido,         1, 0,"Defensive Grab", 0, -2,  99,   0,   DieType.D4, DamageType.BLUNT,  AttackType.COUNTER_ATTACK,         0,  0,  2, 2),
                                                                                new WeaponStyleAttackGrapple(SkillType.Aikido, 5, 4, "Offensive Grab", 0, 0, 1, 2),
                                                                                new WeaponStyleAttackGrapple(SkillType.Brawling,       3, 2,        "Grab",                                                                                        0,  0,  1, 2),
                                                                                new WeaponStyleAttackGrapple(SkillType.Wrestling,      1, 0,        "Grab",                                                                                        0,  0,  1, 2),
                                                                                new WeaponStyleParry(      SkillType.Boxing,           1, 0,       "Parry",  0,  -2,  99,  1.5, 1),
                                                                                new WeaponStyleParry(      SkillType.Wrestling,        1, 0,       "Parry",  0,  -2,  99,   1,  1),
                                                                                new WeaponStyleParry(      SkillType.Brawling,         1, 0,       "Parry",  0,  -2,  99,   1,  1),
                                                                                new WeaponStyleParry(      SkillType.Aikido,           1, 0,       "Parry",  0,  -2,  99,  1.5, 1),
                                                                                new WeaponStyleParry(      SkillType.Karate,           1, 0,       "Parry",  0,  -2,  99,  1.5, 1),
                                                                                new WeaponStyleGrapplingParry(SkillType.Brawling,      1, 0,"Grappling parry",0, -2,  99,   1,  1),
                                                                                new WeaponStyleGrapplingParry(SkillType.Wrestling,     1, 0,"Grappling parry",0, -2,  99,  1.5, 1),
                                                                                new WeaponStyleGrapplingParry(SkillType.Aikido,        1, 0,"Grappling parry",0, -2,  99,  1.5, 1)}),
              new WeaponBase(3, 4,  50, Weapon.NAME_Axe,            new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.AxeMace, 0, 0, "Swing", 2, 0, 6, 10, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleParry(      SkillType.AxeMace,          0, 0,       "Parry",  1,   0,  99,  1.0, 1)}),
              new WeaponBase(3, 3,  60, Weapon.NAME_ThrowingAxe,    new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.AxeMace, 0, 0, "Swing", 1, -4, 9, 7, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleAttackThrown(0,         0, 5,      DieType.D6,  DamageType.CUT, 10, 1),
                                                                                new WeaponStyleParry(      SkillType.AxeMace,          0, 0,       "Parry",  1, -99,   9,  1.0, 1)}),
              //           size,lbs,  $, name                                             2               (SkillType,                 min,pen.,   StyleName,spd,Str-,Str+, dam,  Die,             damageType,     attackType,       charge type, pry-,ranges hands)
              new WeaponBase(3, 6, 700, Weapon.NAME_BastardSword,   new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Swing (1h)", 2, -3, 8, 10, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 3, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Thrust (1h)", 1, -4, 99, 7, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 3, 1),
                                                                                new WeaponStyleParry(      SkillType.Sword,            0, 0,  "Parry (1h)",  1,  -3,  99,  1.0, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword, 0, 0, "Swing (2h)", 2, -5, 4, 12, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 3, 2),
                                                                                new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword, 0, 0, "Thrust (2h)", 2, -99, 1, 8, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 3, 2),
                                                                                new WeaponStyleParry(      SkillType.TwoHanded_Sword,  0, 0,  "Parry (2h)",  1,  -5,  99,  1.0, 2)}),
              new WeaponBase(3, 6,7000,Weapon.NAME_BastardSword_Fine,new WeaponStyle[]{new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Swing (1h)", 2, -4, 7, 11, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 3, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Thrust (1h)", 1, -5, 99, 7, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 3, 1),
                                                                                new WeaponStyleParry(      SkillType.Sword,            0, 0,  "Parry (1h)",  1,  -4,  99,  1.0, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword, 0, 0, "Swing (2h)", 2, -6, 3, 13, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 3, 2),
                                                                                new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword, 0, 0, "Thrust (2h)", 2, -99, 1, 8, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 3, 2),
                                                                                new WeaponStyleParry(      SkillType.TwoHanded_Sword,  0, 0,  "Parry (2h)",  1,  -6,  99,  1.0, 2)}),
              new WeaponBase(2, 3,  20, Weapon.NAME_Club,           new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.AxeMace, 0, 0, "Swing", 1, -2, 7, 7, DieType.D4, DamageType.BLUNT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleParry(      SkillType.AxeMace,          0, 0,       "Parry",  1, -99,   7,  1.0, 1)}),
              new WeaponBase(1, 1,  25, Weapon.NAME_Dagger,         new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Knife, 0, 0, "Thrust", 0, -1, 99, 7, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 0, 1, 1),
                                                                                new WeaponStyleParry(      SkillType.Knife,            0, 0,       "Parry",  0,  -1,  99,  0.5, 1)}),
              new WeaponBase(3,10, 125, Weapon.NAME_Flail,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Flail, 1, 0, "Swing", 2, -3, 7, 8, DieType.D10, DamageType.BLUNT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 4, 2, 3, 2),
                                                                                new WeaponStyleParry(      SkillType.Flail,            1, 0,       "Parry",  1,  -3,  99,  0.5, 2)}),
              new WeaponBase(5, 8, 200, Weapon.NAME_GreatAxe,       new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.TwoHanded_AxeMace, 0, 0, "Swing", 3, -2, 5, 14, DieType.D8, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 3, 2),
                                                                                new WeaponStyleParry(      SkillType.TwoHanded_AxeMace,0, 0,       "Parry",  2, -99,   5,  1.0, 2)}),
              new WeaponBase(3,13, 300, Weapon.NAME_Halberd,        new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Polearm, 0, 0, "Swing (cut)", 3, 0, 7, 14, DieType.D8, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 4, 2),
                                                                                new WeaponStyleAttackMelee(SkillType.Polearm, 0, 0, "Swing (imp)", 3, 0, 7, 14, DieType.D12, DamageType.IMP, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 4, 2),
                                                                                new WeaponStyleAttackMelee(SkillType.Polearm, 0, 0, "Thrust", 2, -1, 7, 8, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 3, 4, 2),
                                                                                new WeaponStyleParry(      SkillType.Polearm,          0, 0,       "Parry",  2, -99,   7,  1.0, 2)}),
              new WeaponBase(3,15, 750, Weapon.NAME_Lance,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Jousting, 0, 0, "Joust", 0, -99, 99, 8, DieType.D12, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.WhenMounted, 10, 3, 3, 1)}),
              new WeaponBase(3, 3,  40, Weapon.NAME_Javelin,        new WeaponStyle[] {new WeaponStyleAttackThrown(0,         0, 4,      DieType.D12,  DamageType.IMP, 20,  1)}),
              new WeaponBase(0, 1,  40, Weapon.NAME_Knife,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Knife, 0, 0, "Swing", 0, -1, 99, 5, DieType.D4, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 0, 1, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.Knife, 0, 0, "Thrust", 0, -1, 99, 7, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 0, 1, 1),
                                                                                new WeaponStyleAttackThrown(0,         0, 5,      DieType.D6,  DamageType.IMP, 10,  1),
                                                                                new WeaponStyleParry(      SkillType.Knife,            0, 0,       "Parry",  0,  -1,  99,  0.5, 1)}),
              //           size,lbs,  $, name                                             2               (SkillType,                 min,pen.,   StyleName,spd,Str-,Str+, dam,  Die,             damageType,     attackType,       charge type, pry-,ranges hands)
              new WeaponBase(3, 3,1200, Weapon.NAME_Katana,         new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Swing (1h)", 2, -5, 1, 9, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Thrust (1h)", 1, -3, 7, 7, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleParry(      SkillType.Sword,            0, 0,  "Parry (1h)",  1,  -5,  99,  1.0, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword, 0, 0, "Swing (2h)", 1, -6, 3, 11, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 2),
                                                                                new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword, 0, 0, "Thrust (2h)", 1, -99, 1, 8, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 2, 2),
                                                                                new WeaponStyleParry(      SkillType.TwoHanded_Sword,  0, 0,  "Parry (2h)",  1, -99,   3,  1.0, 2)}),
              new WeaponBase(3, 3,12000, Weapon.NAME_Katana_Fine,   new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Swing (1h)", 2, -6, 0, 10, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Thrust (1h)", 1, -3, 7, 7, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleParry(      SkillType.Sword,            0, 0,  "Parry (1h)",  1,  -6,  99,  1.0, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword, 0, 0, "Swing (2h)", 1, -6, 2, 12, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 2),
                                                                                new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword, 0, 0, "Thrust (2h)", 1, -99, 1, 8, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 2, 2),
                                                                                new WeaponStyleParry(      SkillType.TwoHanded_Sword,  0, 0,  "Parry (2h)",  1, -99,   2,  1.0, 2)}),
              new WeaponBase(3, 4, 550, Weapon.NAME_Longsword,      new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Swing", 2, -5, 2, 10, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Thrust", 1, -2, 8, 7, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleParry(      SkillType.Sword,            0, 0,       "Parry",  1,  -5,  99,  1.0, 1)}),
              new WeaponBase(3, 4,5000, Weapon.NAME_Longsword_Fine, new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Swing", 2, -6, 1, 11, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Thrust", 1, -2, 8, 7, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleParry(      SkillType.Sword,            0, 0,       "Parry",  1,  -6,  99,  1.0, 1)}),
              new WeaponBase(3, 5, 650, Weapon.NAME_Broadsword,     new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Swing", 2, -2, 5, 11, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Thrust", 1, -2, 9, 7, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleParry(      SkillType.Sword,            0, 0,       "Parry",  1,  -2,  99,  1.0, 1)}),
              new WeaponBase(3, 7,  50, Weapon.NAME_Mace,           new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.AxeMace, 0, 0, "Swing", 2, -5, 2, 8, DieType.D4, DamageType.BLUNT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleParry(      SkillType.AxeMace,          0, 0,       "Parry",  1,  -5,  99,  1.0, 1)}),
              new WeaponBase(2, 2,  25, Weapon.NAME_Nunchucks,      new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.NunChucks, 1, 0, "Swing (1h)", 1, -99, 99, 4, DieType.D8, DamageType.BLUNT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.NunChucks, 1, 0, "Swing (2h)", 0, -8, 99, 4, DieType.D8, DamageType.BLUNT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 2),
                                                                                new WeaponStyleParry(      SkillType.NunChucks,        1, 0,       "Parry",  0,  -8,  99,  1.0, 2)}),
              new WeaponBase(5,12, 100, Weapon.NAME_Maul,           new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.TwoHanded_AxeMace, 0, 0, "Swing", 3, -2, 6, 12, DieType.D4, DamageType.BLUNT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 3, 2),
                                                                                new WeaponStyleParry(      SkillType.TwoHanded_AxeMace,0, 0,       "Parry",  2, -99,   6,  1.0, 2)}),
              new WeaponBase(3, 8, 100, Weapon.NAME_MorningStar,    new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Flail, 1, 0, "Swing", 2, -1, 9, 5, DieType.D10, DamageType.BLUNT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 4, 2, 2, 1),
                                                                                new WeaponStyleParry(      SkillType.Flail,            1, 0,       "Parry",  1,  -1,  99,  0.5, 1)}),
              new WeaponBase(2, 4,  75, Weapon.NAME_PickAxe,        new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.AxeMace, 0, 0, "Swing", 2, -2, 8, 11, DieType.D10, DamageType.IMP, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleParry(      SkillType.AxeMace,          0, 0,       "Parry",  1,  -2,  99,  1.0, 1)}),
              new WeaponBase(3, 4,  20, Weapon.NAME_Quarterstaff,   new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Quarterstaff, 0, 0, "Swing", 1, -3, 99, 8, DieType.D4, DamageType.BLUNT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 2),
                                                                                new WeaponStyleAttackMelee(SkillType.Quarterstaff, 0, 0, "Thrust", 0, -3, 99, 1, DieType.D8, DamageType.BLUNT, AttackType.THRUST, WeaponStyleAttack.Charge.WhenMounted, 0, 2, 2, 2),
                                                                                new WeaponStyleParry(      SkillType.Quarterstaff,     0, 0,       "Parry",  0,  -3,  99,  1.5, 2)}),
              new WeaponBase(2, 2, 600, Weapon.NAME_Rapier,         new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Fencing, 0, 0, "Thrust", 1, -99, 1, 7, DieType.D6, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleParry(      SkillType.Fencing,          0, 0,       "Parry",  1, -99,   1,  1.5, 1)}),
              new WeaponBase(2, 3, 900, Weapon.NAME_Sabre,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Fencing, 0, 0, "Swing", 1, -99, 4, 6, DieType.D4, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.Fencing, 0, 0, "Thrust", 1, -99, 2, 7, DieType.D6, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleParry(      SkillType.Fencing,          0, 0,       "Parry",  1, -99,   2,  1.5, 1)}),
              new WeaponBase(2, 3, 400, Weapon.NAME_Shortsword,     new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Swing", 1, -2, 7, 8, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.Sword, 0, 0, "Thrust", 1, -99, 3, 7, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 2, 1),
                                                                                new WeaponStyleParry(      SkillType.Sword,            0, 0,       "Parry",  1, -99,   3,  1.0, 1)}),
              //           size,lbs,  $, name                                             2               (SkillType,                 min,pen.,   StyleName,spd,Str-,Str+, dam,  Die,             damageType,     attackType,       charge type, pry-,ranges hands)
              new WeaponBase(3, 5,  60, Weapon.NAME_Spear,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Spear, 0, 0, "Thrust (1h)", 2, -99, 6, 7, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 3, 1),
                                                                                new WeaponStyleAttackMelee(SkillType.Spear, 0, 0, "Thrust (2h)", 1, -2, 99, 8, DieType.D8, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.WhenMounted, 0, 2, 3, 2),
                                                                                new WeaponStyleAttackThrown(0,         0, 5,      DieType.D12,  DamageType.IMP, 16,  1),
                                                                                new WeaponStyleParry(      SkillType.Spear,            0, 0,  "Parry (1h)",  1, -99,  99,  1.0, 1),
                                                                                new WeaponStyleParry(      SkillType.Spear,            0, 0,  "Parry (2h)",  1, -99,  99,  1.0, 2)}),
              new WeaponBase(1, 0.1,25, Weapon.NAME_ThrowingStar,   new WeaponStyle[] {new WeaponStyleAttackThrown(0,         0, 6,      DieType.D4,  DamageType.IMP, 14,  1)}),
              new WeaponBase(2, 2,  75, Weapon.NAME_ThreePartStaff, new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.NunChucks, 2, 2, "Long Swing", 1, -5, 99, 8, DieType.D8, DamageType.BLUNT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 3, 4, 2),
                                                                                new WeaponStyleAttackMelee(SkillType.NunChucks, 2, 2, "Short Swing", 1, -5, 99, 5, DieType.D8, DamageType.BLUNT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 3, 2),
                                                                                new WeaponStyleParry(      SkillType.NunChucks,        2, 0,       "Parry",  0,  -5,  99,  1.0, 2)}),
              new WeaponBase(3, 9, 850, Weapon.NAME_TwoHandedSword, new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword, 0, 0, "Swing", 2, -3, 7, 13, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 3, 2),
                                                                                new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword, 0, 0, "Thrust", 2, -99, 2, 8, DieType.D10, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 3, 2),
                                                                                new WeaponStyleParry(      SkillType.TwoHanded_Sword,  0, 0,       "Parry",  1,  -3,  99,  1.0, 2)}),
              new WeaponBase(3,9,8500,Weapon.NAME_TwoHandedSword_Fine,new WeaponStyle[]{new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword, 0, 0, "Swing", 2, -4, 6, 14, DieType.D6, DamageType.CUT, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 3, 2),
                                                                                 new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword, 0, 0, "Thrust", 2, -99, 2, 8, DieType.D10, DamageType.IMP, AttackType.THRUST, WeaponStyleAttack.Charge.Never, 0, 2, 3, 2),
                                                                                 new WeaponStyleParry(      SkillType.TwoHanded_Sword,  0, 0,       "Parry",  1,  -3,  99,  1.0, 2)}),
              new WeaponBase(5, 7, 125, Weapon.NAME_WarHammer,      new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.TwoHanded_AxeMace, 0, 0, "Swing", 3, -3, 4, 18, DieType.D6, DamageType.IMP, AttackType.SWING, WeaponStyleAttack.Charge.Never, 0, 2, 3, 2),
                                                                                new WeaponStyleParry(      SkillType.TwoHanded_AxeMace,0, 0,       "Parry",  2, -99,   4,  1.0, 2)}),

              // size, lbs, $,  name,              SkillType,         min, Pen, dam,        var,     damType, rngBase, hands,      preparation steps (last to first)
              new MissileWeaponBase(0, 1,  10, Weapon.NAME_BlowGun,       SkillType.BlowGun,    0, 0,   5,  DieType.D4, DamageType.IMP,   12,  1, new String[] {"Inhale and raise blow gun", "Load dart", "Ready dart"}),
              new MissileWeaponBase(5, 5, 750, Weapon.NAME_BowComposite,  SkillType.Bow,        0, 0,  11, DieType.D10, DamageType.IMP,   60,  2, new String[] {"Draw bow", "Notch arrow", "Ready arrow"}),
              new MissileWeaponBase(4, 5, 400, Weapon.NAME_BowLongbow,    SkillType.Bow,        0, 0,  10, DieType.D10, DamageType.IMP,   50,  2, new String[] {"Draw bow", "Notch arrow", "Ready arrow"}),
              new MissileWeaponBase(3, 4, 200, Weapon.NAME_BowShortbow,   SkillType.Bow,        0, 0,   9, DieType.D10, DamageType.IMP,   40,  2, new String[] {"Draw bow", "Notch arrow", "Ready arrow"}),
              new MissileWeaponBase(4, 7, 200, Weapon.NAME_Crossbow,      SkillType.Crossbow,   0, 0,  10, DieType.D12, DamageType.IMP,   40,  2, new String[] {"Raise crossbow", "Notch bolt", "Ready bolt", "Cock crossbow", "Cock crossbow"}),
              new MissileWeaponBase(6, 9, 300, Weapon.NAME_CrossbowHeavy, SkillType.Crossbow,   0, 0,  12, DieType.D12, DamageType.IMP,   50,  2, new String[] {"Raise crossbow", "Notch bolt", "Ready bolt", "Cock crossbow", "Cock crossbow", "Cock crossbow"}),
              new MissileWeaponBase(2, 6, 150, Weapon.NAME_CrossbowLight, SkillType.Crossbow,   0, 0,   8, DieType.D12, DamageType.IMP,   30,  2, new String[] {"Raise crossbow", "Notch bolt", "Ready bolt", "Cock crossbow"}),
              new MissileWeaponBase(0, 1,   5, Weapon.NAME_Sling,         SkillType.Sling,      1, 0,   0,  DieType.D4, DamageType.BLUNT, 12,  1, new String[] {"Spin sling", "Load stone in sling", "Ready stone"}),
              new MissileWeaponBase(1, 3, 100, Weapon.NAME_StaffSling,    SkillType.Sling,      1, 0,   7,  DieType.D6, DamageType.BLUNT, 20,  2, new String[] {"Spin sling", "Load stone in staff sling", "Ready stone"}),
              };
      // sort this array, so they can be listed in alphabetical order
      Arrays.sort(weaponsList, (o1, o2) -> o1.getName().compareTo(o2.getName()));
      Arrays.sort(specialWeaponsList, (o1, o2) -> o1.getName().compareTo(o2.getName()));
   }

   public static List<String> getWeaponNames(boolean includeNaturalWeapons) {
      List<String> list = new ArrayList<>();
      for (SizelessWeapon element : weaponsList) {
         if (includeNaturalWeapons || element.isReal()) {
            list.add(element.getName());
         }
      }
      return list;
   }

   public static List<Weapon> getWeaponListForRace(Race race) {
      List<Weapon> list = new ArrayList<>();
      for (SizelessWeapon weapon : weaponsList) {
         Weapon copy = weapon.clone();
         copy.setRacialBase(race);
         list.add(copy);
      }
      return list;
   }

   static public Weapon getWeapon(String name, Race racialBase) {
      if (name != null) {
         for (SizelessWeapon element : weaponsList) {
            if (element.getName().equalsIgnoreCase(name)) {
               return element.copyWithRace(racialBase);
            }
         }
         // check for special weapons
         for (SizelessWeapon element : specialWeaponsList) {
            if (element.getName().equalsIgnoreCase(name)) {
               Weapon weap = element.clone();
               weap.setRacialBase(racialBase);
               return weap;
            }
         }
         // If we couldn't find a weapon, use Punch,
         // unless that is what we just tried to look for.
         if (name.equalsIgnoreCase(Weapon.NAME_Punch)) {
            return null;
         }
      }
      Weapon weapon = getWeapon(Weapon.NAME_Punch, racialBase);
      return (weapon == null) ? null : weapon.clone();
   }

   public static String generateHtmlTable() {
      String sb = HtmlBuilder.getHTMLHeader("TblWeapon", 600, 44) +
                  "<body>\n" +
                  "<H4>Weapon data:</H4>\n" +
                  "<H3>Melee Weapons:</H3>\n" +
                  "<div style=\"overflow: hidden;\" id=\"DivHeaderRow\">\n" +
                  "</div>\n" +
                  "<div style=\"overflow:scroll;overflow-x:hidden; border-width:0px; border-bottom:1px; border-style:solid;\"" +
                  " onscroll=\"OnScrollDiv(this)\" id=\"DivMainContent\">\n" +
                  getMeleeWeaponTable() +
                  "</div>" +
                  "<H3>Missile Weapons:</H3>" +
                  getMissileThrownWeaponTable(true/*missile*/, false/*thrown*/) +
                  "<H3>Thrown Weapons:</H3>" +
                  getMissileThrownWeaponTable(false/*missile*/, true/*thrown*/) +
                  "</body>";/*missile*//*thrown*//*missile*//*thrown*/
      return sb;
   }

   private static String getMeleeWeaponTable() {
      StringBuilder sb = new StringBuilder();
      sb.append("<table id=\"TblWeapon\" width='100%'>");
      sb.append("<tr class=\"header-row\">");
      sb.append("<th>Weapon<br/>Name</th>");
      sb.append("<th>Skill<br/>Name</th>");
      sb.append("<th>Style<br/>Name</th>");
      sb.append("<th>Hands<br/>Used</th>");
      sb.append("<th>Min.<br/>Skill</th>");
      sb.append("<th>Base<br/>Speed</th>");
      sb.append("<th>Fast<br/>STR</th>");
      sb.append("<th>Slow<br/>STR</th>");
      sb.append("<th>Damage<br/>+STR</th>");
      sb.append("<th>Variance<br/>Die</th>");
      sb.append("<th>Damage<br/>Type</th>");
      sb.append("<th>Parried<br/>Penalty</th>");
      sb.append("<th>Min<br/>Range</th>");
      sb.append("<th>Max<br/>Range</th>");
      sb.append("<th>Cost</th>");
      sb.append("<th>Weight</th>");
      sb.append("</tr>\n");

      int weapIndex=-1;
      List<Weapon> humanWeapons = getWeaponListForRace(Race.getRace(Race.NAME_Human, Race.Gender.MALE));
      for (Weapon weap : humanWeapons) {
         weapIndex++;
         int rowCount = weap.getAttackStyles().length + weap.getParryStyles().length;
         String[][] data = new String[rowCount][16];
         int row = 0;
         for (WeaponStyleAttack attack : weap.getAttackStyles()) {
            //          // thrown weapon attacks are shown in a separate table
//          if (!attack.isThrown()) {
            int col=0;
            data[row][col++] = weap.getName();
            if (attack.getSkillPenalty() == 0) {
               data[row][col++] = attack.getSkillType().getName();
            }
            else {
               data[row][col++] = attack.getSkillType().getName() + " - " + attack.getSkillPenalty();
            }
            data[row][col++] = attack.getName();
            data[row][col++] = String.valueOf(attack.getHandsRequired());
            data[row][col++] = String.valueOf(attack.getMinSkill());
            data[row][col++] = String.valueOf(attack.getSpeedBase());
            int fastStr = attack.getFastStr();
            int slowStr = attack.getSlowStr();
            data[row][col++] = (fastStr == 99) ? "--" : String.valueOf(fastStr);
            data[row][col++] = (slowStr == -99) ? "--" : String.valueOf(slowStr);
            data[row][col++] = String.valueOf(attack.getDamageMod());
            data[row][col++] = attack.getVarianceDie().toString();
            data[row][col++] = attack.getDamageType().shortname;
            data[row][col++] = String.valueOf(attack.getParryPenalty());
            data[row][col++] = String.valueOf(attack.getMinRange());
            data[row][col++] = String.valueOf(attack.getMaxRange());
            data[row][col++] = String.valueOf(weap.getCost());
            data[row][col++] = String.valueOf(weap.getWeight());
            row++;
//          }
         }
         for (WeaponStyleParry parry : weap.getParryStyles()) {
            int col=0;
            data[row][col++] = weap.getName();
            data[row][col++] = parry.getSkillType().getName();
            data[row][col++] = parry.getName();
            data[row][col++] = String.valueOf(parry.getHandsRequired());
            data[row][col++] = String.valueOf(parry.getMinSkill());
            data[row][col++] = String.valueOf(parry.getSpeedBase());
            int fastStr = parry.getFastStr();
            int slowStr = parry.getSlowStr();
            data[row][col++] = (fastStr == 99) ? "--" : String.valueOf(fastStr);
            data[row][col++] = (slowStr == -99) ? "--" : String.valueOf(slowStr);
            if (parry.getEffectiveness() == 1.0) {
               data[row][col++] = "&nbsp;";
            }
            else {
               data[row][col++] = "Parry effectiveness = " + parry.getEffectiveness();
            }
            col+=5;
            data[row][col++] = String.valueOf(weap.getCost());
            data[row][col++] = String.valueOf(weap.getWeight());
            row++;
         }
         for (int r=0 ; r<row ; r++) {
            sb.append(HtmlBuilder.buildRow(weapIndex));
            for (int c=0 ; c<16 ; c++) {
               int colSpan = 1;
               int rowSpan = 1;
               if ((r>=weap.getAttackStyles().length) && (c== 8)){
                  colSpan = 6;
               }
               if ((r>0) && ((data[r-1][c] != null) && (data[r-1][c].equals(data[r][c])) && (c!=2))) {
                  // skip this row, cause it was printed last time.
               }
               else {
                  while ((r+rowSpan)<row) {
                     if ((data[r+rowSpan][c] != null) && (data[r+rowSpan][c].equals(data[r][c])) && (c!=2)) {
                        rowSpan++;
                     }
                     else {
                        break;
                     }
                  }
                  if (c == 0) {
                     sb.append("<th ");
                  }
                  else {
                     sb.append("<td");
                  }
                  if (colSpan>1) {
                     if (data[r][c].length() > 6) {
                        if (data[r][c].contains(" = 1.5")) {
                           sb.append(" bgcolor=#BBFFB8");
                        }
                        else {
                           sb.append(" bgcolor=#FFD8D8");
                        }
                     }
                     sb.append(" colspan=").append(colSpan);
                  }
                  if (rowSpan>1) {
                     sb.append(" rowspan=").append(rowSpan);
                  }
                  sb.append(">");
                  sb.append(data[r][c]);
                  if (c == 0) {
                     sb.append("</th>");
                  }
                  else {
                     sb.append("</td>");
                  }
               }
               if ((r>=weap.getAttackStyles().length) && (c== 8)){
                  c+=6;
               }
            }
            sb.append("</tr>\n");
         }
      }
      sb.append("<tr><td>&nbsp;</td></tr>\n");
      sb.append("</table>");
      return sb.toString();
   }

   public static String getMissileThrownWeaponTable(boolean missile, boolean thrown)
   {
      StringBuilder sb = new StringBuilder();
      sb.append("<table width='100%'>");
      sb.append("<tr class=\"header-row\">");
      sb.append("<th>Weapon<br/>Name</th>");
      sb.append("<th>Skill<br/>Name</th>");
      sb.append("<th>Hands<br/>Used</th>");
      sb.append("<th>Min.<br/>Skill</th>");
      sb.append("<th>Preparation<br/>Steps</th>");
      sb.append("<th>Damage<br/>+STR</th>");
      sb.append("<th>Variance<br/>Die</th>");
      sb.append("<th>Damage<br/>Type</th>");
      sb.append("<th>Range<br/>Base</th>");
      sb.append("<th>Cost</th>");
      sb.append("<th>Weight</th>");
      sb.append("</tr>\n");
      int htmlRow = 0;
      for (SizelessWeapon weap : weaponsList) {
         for (WeaponStyleAttack attack : weap.getAttackStyles()) {
            if (attack instanceof WeaponStyleAttackRanged) {
               if ((missile && attack.isMissile()) || (thrown && attack.isThrown())) {
                  WeaponStyleAttackRanged rangedAttack = (WeaponStyleAttackRanged) attack;
                  sb.append(HtmlBuilder.buildRow(htmlRow++));
                  sb.append("<th>").append(weap.getName()).append("</th>");
                  sb.append("<td>").append(rangedAttack.getSkillType().getName());
                  if (rangedAttack.getSkillPenalty() != 0) {
                     sb.append(" - ").append(rangedAttack.getSkillPenalty());
                  }
                  sb.append("</td>");
                  sb.append("<td>").append(String.valueOf(rangedAttack.getHandsRequired())).append("</td>");
                  sb.append("<td>").append(String.valueOf(rangedAttack.getMinSkill())).append("</td>");
                  sb.append("<td class='alignLeft'>").append(rangedAttack.getPreparationStepsAsHTML("")).append("</td>");
                  sb.append("<td>").append(String.valueOf(rangedAttack.getDamageMod())).append("</td>");
                  sb.append("<td>").append(rangedAttack.getVarianceDie()).append("</td>");
                  sb.append("<td>").append(rangedAttack.getDamageType().shortname).append("</td>");
                  sb.append("<td>").append(String.valueOf(rangedAttack.getRangeBase())).append("</td>");
                  sb.append("<td>").append(weap.getCost()).append("</td>");
                  sb.append("<td>").append(weap.getWeight()).append("</td>");
                  sb.append("</tr>\n");
               }
            }
         }
      }
      sb.append("</table>");
      return sb.toString();
   }

}
