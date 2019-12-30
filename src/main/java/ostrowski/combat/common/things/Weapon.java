package ostrowski.combat.common.things;

import org.eclipse.swt.graphics.RGB;
import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.*;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.enums.AttackType;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.html.HtmlBuilder;
import ostrowski.combat.common.weaponStyles.*;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack.Charge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/*
 * Created on May 3, 2006
 *
 */

public class Weapon extends Thing {
   public    WeaponStyleAttack[]        _attackStyles;
   public    WeaponStyleCounterAttack[] _counterattackStyles;
   public    WeaponStyleAttackGrapple[] _grapplingStyles;
   public    WeaponStyleParry[]         _parryStyles;
   protected int                 _size;
   private   SpecialDamage       _specialDamageModifier    = new SpecialDamage(0);
   private   String              _specialDamageExplanation = "";

   public WeaponStyleAttack[] getAttackStyles() {
      return _attackStyles;
   }
   public WeaponStyleAttack getAttackStyle(int index) {
      if (index < _attackStyles.length) {
         return _attackStyles[index];
      }
      System.err.println("asked a " + getName() +" for style #"+index+", which doesn't exist.");
      DebugBreak.debugBreak();
      return null;
   }
   public WeaponStyleAttackGrapple[] getGrapplingStyles() {
      return _grapplingStyles;
   }
   public WeaponStyleAttackGrapple getGrappleStyle(int index) {
      if (index < _grapplingStyles.length) {
         return _grapplingStyles[index];
      }
      System.err.println("asked a " + getName() +" for grappling style #"+index+", which doesn't exist.");
      DebugBreak.debugBreak();
      return null;
   }
   public WeaponStyleCounterAttack getCounterAttackStyle(int index) {
      if (index < _counterattackStyles.length) {
         return _counterattackStyles[index];
      }
      System.err.println("asked a " + getName() +" for counter-attack style #"+index+", which doesn't exist.");
      DebugBreak.debugBreak();
      return null;
   }

   public Weapon() {}
   public Weapon(int size, Race racialBase, double weight, int cost, String name, WeaponStyle[] styleModes) {
      super(name, racialBase, cost, weight, (byte)0/*passiveDefense*/);
      _size        = size;
      int attackCount = 0;
      int counterattackCount = 0;
      int grappleCount = 0;
      int parryCount = 0;
      for (WeaponStyle element : styleModes) {
         if (element instanceof WeaponStyleCounterAttack) {
            counterattackCount++;
         }
         else if (element instanceof WeaponStyleAttackGrapple) {
            grappleCount++;
         }
         else if (element instanceof WeaponStyleAttack) {
            attackCount++;
         }
         else if (element instanceof WeaponStyleParry) {
            parryCount++;
         }
      }
      _counterattackStyles = new WeaponStyleCounterAttack[counterattackCount];
      _grapplingStyles     = new WeaponStyleAttackGrapple[grappleCount];
      _attackStyles        = new WeaponStyleAttack[attackCount];
      _parryStyles         = new WeaponStyleParry[parryCount];

      attackCount        = 0;
      counterattackCount = 0;
      grappleCount       = 0;
      parryCount         = 0;

      for (WeaponStyle element : styleModes) {
         WeaponStyle styleCopy = element.clone();
         if (styleCopy instanceof WeaponStyleCounterAttack) {
            _counterattackStyles[counterattackCount++] = (WeaponStyleCounterAttack) styleCopy;
         }
         else if (styleCopy instanceof WeaponStyleAttackGrapple) {
            _grapplingStyles[grappleCount++] = (WeaponStyleAttackGrapple) styleCopy;
         }
         else if (styleCopy instanceof WeaponStyleAttack) {
            _attackStyles[attackCount++] = (WeaponStyleAttack) styleCopy;
         }
         else if (styleCopy instanceof WeaponStyleParry) {
            _parryStyles[parryCount++] = (WeaponStyleParry) styleCopy;
         }
         styleCopy.setWeapon(this);
      }
   }
   @Override
   public Weapon clone() {
      WeaponStyle[] styles = new WeaponStyle[_counterattackStyles.length + _grapplingStyles.length + _attackStyles.length + _parryStyles.length];
      int styleCount = 0;
      for (WeaponStyleAttack _attackStyle : _attackStyles) {
         styles[styleCount++] = _attackStyle.clone();
      }
      for (WeaponStyleCounterAttack _counterattackStyle : _counterattackStyles) {
         styles[styleCount++] = _counterattackStyle.clone();
      }
      for (WeaponStyleAttackGrapple _grapplingStyle : _grapplingStyles) {
         styles[styleCount++] = _grapplingStyle.clone();
      }
      for (WeaponStyleParry _parryStyle : _parryStyles) {
         styles[styleCount++] = _parryStyle.clone();
      }
      return new Weapon(_size, getRacialBase(), _weight, _cost, _name, styles);
   }
   @Override
   public void copyData(Thing source) {
      super.copyData(source);
      if (source instanceof Weapon) {
         Weapon weap = (Weapon) source;
         _size = weap._size;
         _attackStyles        = new WeaponStyleAttack[weap._attackStyles.length];
         _counterattackStyles = new WeaponStyleCounterAttack[weap._counterattackStyles.length];
         _grapplingStyles     = new WeaponStyleAttackGrapple[weap._grapplingStyles.length];
         _parryStyles         = new WeaponStyleParry[weap._parryStyles.length];

         for(int i=0 ; i<_attackStyles.length ; i++) {
            _attackStyles[i] = (WeaponStyleAttack) weap._attackStyles[i].clone();
         }
         for(int i=0 ; i<_parryStyles.length ; i++) {
            _parryStyles[i] = weap._parryStyles[i].clone();
         }
         for(int i=0 ; i<_counterattackStyles.length ; i++) {
            _counterattackStyles[i] = weap._counterattackStyles[i].clone();
         }
         for(int i=0 ; i<_grapplingStyles.length ; i++) {
            _grapplingStyles[i] = weap._grapplingStyles[i].clone();
         }
      }
   }

   static public Weapon getWeapon(String name, Race racialBase) {
      if (name != null) {
         for (SizelessWeapon element : _weaponsList) {
            if (element.getName().equalsIgnoreCase(name)) {
               return element.copyWithRace(racialBase);
            }
         }
         // check for special weapons
         for (SizelessWeapon element : _specialWeaponsList) {
            if (element.getName().equalsIgnoreCase(name)) {
               Weapon weap = element.clone();
               weap.setRacialBase(racialBase);
               return weap;
            }
         }
         // If we couldn't find a weapon, use Punch,
         // unless that is what we just tried to look for.
         if (name.equalsIgnoreCase(NAME_Punch)) {
            return null;
         }
      }
      return getWeapon(NAME_Punch, racialBase).clone();
   }
   public boolean isUnarmedStyle () {
      return _size<0;
   }
   @Override
   public String getActiveDefenseName() {
      if (_parryStyles.length > 0) {
         return "parry";
      }
      return null;
   }
   @Override
   public List<SkillType> getDefenseSkillTypes() {
      List<SkillType> results = new ArrayList<>();
      for (WeaponStyleParry element : _parryStyles) {
         results.add(element._skillType);
      }
      return results;
   }
   @Override
   public byte getBestDefenseOption(Character wielder, LimbType useHand, boolean canUse2Hands, DamageType damType,
                                    boolean isGrappleAttack, short distance) {
      byte bestDef = 0;
      for (WeaponStyleParry parryStyle : _parryStyles) {
         if ((parryStyle.getHandsRequired() == 1) || (canUse2Hands)) {
            if (parryStyle.getSkillType() == SkillType.Shield) {
               useHand = null;
            }
            boolean defAllowed = true;
            if (!isReal()) {
               defAllowed = parryStyle.canDefendAgainstDamageType(damType, isGrappleAttack, distance);
               // unarmed combat doesn't have penalty for off-hand defense
               useHand = null;
            }
            if (defAllowed) {
                byte skillLevel = (byte) (wielder.getSkillLevel(parryStyle.getSkillType(), useHand, false/*sizeAdjust*/,
                                                                false/*adjustForEncumbrance*/, true/*adjustForHolds*/)
                                          - parryStyle.getSkillPenalty());
                byte parryLevel = Rules.getParryLevel(skillLevel, parryStyle.getEffectiveness());
                if (bestDef < parryLevel) {
                   bestDef = parryLevel;
                }
            }
         }
      }
      return bestDef;
   }

   public byte getMinSkillToAttack() {
      byte minSkill = 127;
      for (WeaponStyleAttack attack : _attackStyles) {
         if (attack.getMinSkill() < minSkill) {
            minSkill = attack.getMinSkill();
         }
      }
      for (WeaponStyleAttackGrapple grapple : _grapplingStyles) {
         if (grapple.getMinSkill() < minSkill) {
            minSkill = grapple.getMinSkill();
         }
      }
      return minSkill;
   }
   static final SizelessWeapon[] _specialWeaponsList;
   static final SizelessWeapon[] _weaponsList;
   public static final String NAME_Claws              = "Claws";
   public static final String NAME_TailStrike         = "Tail";
   public static final String NAME_Fangs              = "Fangs";
   public static final String NAME_SturgeBeak         = "Beak";
   public static final String NAME_Tusks              = "Tusks";
   public static final String NAME_HornGore           = "Horn Gore";
   public static final String NAME_HeadButt           = "Head";
   public static final String NAME_KarateKick         = "Leg";
   public static final String NAME_Punch              = "Hand";
   public static final String NAME_Axe                = "Axe";
   public static final String NAME_ThrowingAxe        = "Throwing Axe";
   public static final String NAME_BastardSword       = "Bastard Sword";
   public static final String NAME_BastardSword_Fine  = "Bastard Sword, Fine";
   public static final String NAME_Club               = "Club";
   public static final String NAME_Dagger             = "Dagger";
   public static final String NAME_Flail              = "Flail";
   public static final String NAME_GreatAxe           = "Great Axe";
   public static final String NAME_Halberd            = "Halberd";
   public static final String NAME_Javelin            = "Javelin";
   public static final String NAME_Knife              = "Knife";
   public static final String NAME_Katana             = "Katana";
   public static final String NAME_Katana_Fine        = "Katana, Fine";
   public static final String NAME_Lance              = "Lance";
   public static final String NAME_Longsword          = "Longsword";
   public static final String NAME_Longsword_Fine     = "Longsword, Fine";
   public static final String NAME_Broadsword         = "Broadsword";
   public static final String NAME_Mace               = "Mace";
   public static final String NAME_Nunchucks          = "Nun chucks";
   public static final String NAME_Maul               = "Maul";
   public static final String NAME_MorningStar        = "Morning Star";
   public static final String NAME_PickAxe            = "Pick-Axe";
   public static final String NAME_Quarterstaff       = "Quarterstaff";
   public static final String NAME_Rapier             = "Rapier";
   public static final String NAME_Sabre              = "Sabre";
   public static final String NAME_Shortsword         = "Shortsword";
   public static final String NAME_Spear              = "Spear";
   public static final String NAME_ThrowingStar       = "Throwing Star";
   public static final String NAME_ThreePartStaff     = "Three Part Staff";
   public static final String NAME_TwoHandedSword     = "Two-Handed Sword";
   public static final String NAME_TwoHandedSword_Fine= "Two-Handed Sword, Fine";
   public static final String NAME_WarHammer          = "War Hammer";
   public static final String NAME_BlowGun            = "Blow Gun";
   public static final String NAME_BowComposite       = "Bow, Composite";
   public static final String NAME_BowLongbow         = "Bow, Longbow";
   public static final String NAME_BowShortbow        = "Bow, Shortbow";
   public static final String NAME_Crossbow           = "Crossbow";
   public static final String NAME_CrossbowHeavy      = "Crossbow, Heavy";
   public static final String NAME_CrossbowLight      = "Crossbow, Light";
   public static final String NAME_Sling              = "Sling";
   public static final String NAME_StaffSling         = "Staff Sling";

   static {
      _specialWeaponsList = new SizelessWeapon[] {
       //           size,lbs,  $, name                                              2                       (SkillType,       min,pen.,   StyleName,spd,Str-,Str+, dam,       Die,        damageType,       attackType,         charge type,  pry-,ranges hands)
       new WeaponBase(-1,0,   0, NAME_HornGore,       new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling,         0, 0,   "Horn Gore",  0, -99,  99,   3,   DieType.D6,   DamageType.IMP,  AttackType.THRUST,    Charge.Anytime,  0,  1,   1, 0)}),
       new WeaponBase(-1,0,   0, NAME_Fangs,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling,         0, 0,   "Bite",       0, -99,  99,   3,   DieType.D6,   DamageType.CUT,  AttackType.THRUST,  Charge.With4Legs,  0,  0,   1, 0)}),
       new WeaponBase(-1,0,   0, NAME_SturgeBeak,     new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling,         0, 0,   "Beak",       0, -99,  99,   6,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,Charge.WhenMounted,  0,  0,   1, 0)}),
       new WeaponBase(-1,0,   0, NAME_Tusks,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling,         0, 0,   "Tusk slash", 0, -99,  99,   4,   DieType.D4,   DamageType.CUT,  AttackType.THRUST,  Charge.With4Legs,  0,  1,   1, 0)}),
       new WeaponBase(-1,0,   0, NAME_Claws,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling,         0, 0,   "Claw",       0, -99,  99,   4,   DieType.D6,   DamageType.CUT,  AttackType.THRUST,      Charge.Never,  0,  0,   1, 1)}),
       new WeaponBase(-1,0,   0, NAME_TailStrike,     new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling,         0, 0,   "Tail strike",1, -99,  99,   2,   DieType.D6,   DamageType.BLUNT, AttackType.SWING,      Charge.Never,  0, -2,   1, 0)}),
      };

       _weaponsList = new SizelessWeapon[] {
       //           size,lbs,  $, name                                             2               (SkillType,                 min,pen.,   StyleName,spd,Str-,Str+, dam,  Die,            damageType,     attackType,      charge type, pry-,ranges hands)
       new WeaponBase(-1,0,   0, NAME_KarateKick,     new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Karate,           3, 2,        "Kick",  1, -99,   4,   2,   DieType.D6, DamageType.BLUNT,  AttackType.THRUST,  Charge.Never,  0,  1,  2, 0),
                                                                         new WeaponStyleAttackMelee(SkillType.Karate,           5, 4,   "Spin kick",  1, -99,   4,   4,   DieType.D6, DamageType.BLUNT,  AttackType.THRUST,  Charge.Never,  0,  2,  2, 0),
                                                                         new WeaponStyleAttackMelee(SkillType.Brawling,         3, 2, "Knee strike",  0,  -2,  99,  -2,   DieType.D4, DamageType.BLUNT,  AttackType.THRUST,  Charge.Never,  0,  0,  1, 0),
                                                                         new WeaponStyleAttackMelee(SkillType.Wrestling,        3, 2, "Knee strike",  0,  -2,  99,  -2,   DieType.D4, DamageType.BLUNT,  AttackType.THRUST,  Charge.Never,  0,  0,  1, 0)}),
       new WeaponBase(-1,0,   0, NAME_HeadButt,       new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling,         1, 0,   "Head Butt",  0, -99,  99,  -3,   DieType.D6, DamageType.BLUNT,  AttackType.THRUST,  Charge.Never,  0, -1,  1, 0)}),
       new WeaponBase(-1,0,   0, NAME_Punch,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Brawling,         1, 0,       "Punch",  0,  -2,  99,   0,   DieType.D4, DamageType.BLUNT,  AttackType.THRUST,  Charge.Never,  0,  0,  1, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.Boxing,           1, 0,       "Punch",  0,  -2,  99,   1,   DieType.D4, DamageType.BLUNT,  AttackType.THRUST,  Charge.Never,  0,  0,  1, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.Karate,           1, 0,       "Punch",  0,  -2,  99,   0,   DieType.D4, DamageType.BLUNT,  AttackType.THRUST,  Charge.Never,  0,  0,  1, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.Brawling,         3, 2,"Elbow strike",  0,  -2,  99,  -1,   DieType.D4, DamageType.BLUNT,  AttackType.THRUST,  Charge.Never,  0, -1,  1, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.Wrestling,        3, 2,"Elbow strike",  0,  -2,  99,  -1,   DieType.D4, DamageType.BLUNT,  AttackType.THRUST,  Charge.Never,  0, -1,  1, 1),
                                                                         new WeaponStyleCounterAttack(SkillType.Aikido,         3, 0,"Defensive Throw",0, -2,  99,   0,   DieType.D4, DamageType.BLUNT,  AttackType.COUNTER_ATTACK,         0,  0,  2, 2),
                                                                         new WeaponStyleCounterAttack(SkillType.Aikido,         1, 0,"Defensive Grab", 0, -2,  99,   0,   DieType.D4, DamageType.BLUNT,  AttackType.COUNTER_ATTACK,         0,  0,  2, 2),
                                                                         new WeaponStyleAttackGrapple(SkillType.Aikido,         5, 4,"Offensive Grab",                                                                                      0,  0,  1, 2),
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
       new WeaponBase(3, 4,  50, NAME_Axe,            new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.AxeMace,          0, 0,       "Swing",  2,   0,   6,  10,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleParry(      SkillType.AxeMace,          0, 0,       "Parry",  1,   0,  99,  1.0, 1)}),
       new WeaponBase(3, 3,  60, NAME_ThrowingAxe,    new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.AxeMace,          0, 0,       "Swing",  1,  -4,   9,   7,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleAttackThrown(0,         0, 5,      DieType.D6,  DamageType.CUT, 10, 1),
                                                                         new WeaponStyleParry(      SkillType.AxeMace,          0, 0,       "Parry",  1, -99,   9,  1.0, 1)}),
       //           size,lbs,  $, name                                             2               (SkillType,                 min,pen.,   StyleName,spd,Str-,Str+, dam,  Die,             damageType,     attackType,       charge type, pry-,ranges hands)
       new WeaponBase(3, 6, 700, NAME_BastardSword,   new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Sword,            0, 0,  "Swing (1h)",  2,  -3,   8,  10,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 3, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.Sword,            0, 0, "Thrust (1h)",  1,  -4,  99,   7,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 3, 1),
                                                                         new WeaponStyleParry(      SkillType.Sword,            0, 0,  "Parry (1h)",  1,  -3,  99,  1.0, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword,  0, 0,  "Swing (2h)",  2,  -5,   4,  12,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 3, 2),
                                                                         new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword,  0, 0, "Thrust (2h)",  2, -99,   1,   8,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 3, 2),
                                                                         new WeaponStyleParry(      SkillType.TwoHanded_Sword,  0, 0,  "Parry (2h)",  1,  -5,  99,  1.0, 2)}),
       new WeaponBase(3, 6,7000,NAME_BastardSword_Fine,new WeaponStyle[]{new WeaponStyleAttackMelee(SkillType.Sword,            0, 0,  "Swing (1h)",  2,  -4,   7,  11,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 3, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.Sword,            0, 0, "Thrust (1h)",  1,  -5,  99,   7,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 3, 1),
                                                                         new WeaponStyleParry(      SkillType.Sword,            0, 0,  "Parry (1h)",  1,  -4,  99,  1.0, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword,  0, 0,  "Swing (2h)",  2,  -6,   3,  13,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 3, 2),
                                                                         new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword,  0, 0, "Thrust (2h)",  2, -99,   1,   8,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 3, 2),
                                                                         new WeaponStyleParry(      SkillType.TwoHanded_Sword,  0, 0,  "Parry (2h)",  1,  -6,  99,  1.0, 2)}),
       new WeaponBase(2, 3,  20, NAME_Club,           new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.AxeMace,          0, 0,       "Swing",  1,  -2,   7,   7,   DieType.D4, DamageType.BLUNT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleParry(      SkillType.AxeMace,          0, 0,       "Parry",  1, -99,   7,  1.0, 1)}),
       new WeaponBase(1, 1,  25, NAME_Dagger,         new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Knife,            0, 0,      "Thrust",  0,  -1,  99,   7,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 0, 1, 1),
                                                                         new WeaponStyleParry(      SkillType.Knife,            0, 0,       "Parry",  0,  -1,  99,  0.5, 1)}),
       new WeaponBase(3,10, 125, NAME_Flail,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Flail,            1, 0,       "Swing",  2,  -3,   7,   8,  DieType.D10, DamageType.BLUNT,   AttackType.SWING,  Charge.Never,  4, 2, 3, 2),
                                                                         new WeaponStyleParry(      SkillType.Flail,            1, 0,       "Parry",  1,  -3,  99,  0.5, 2)}),
       new WeaponBase(5, 8, 200, NAME_GreatAxe,       new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.TwoHanded_AxeMace,0, 0,       "Swing",  3,  -2,   5,  14,   DieType.D8,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 3, 2),
                                                                         new WeaponStyleParry(      SkillType.TwoHanded_AxeMace,0, 0,       "Parry",  2, -99,   5,  1.0, 2)}),
       new WeaponBase(3,13, 300, NAME_Halberd,        new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Polearm,          0, 0, "Swing (cut)",  3,   0,   7,  14,   DieType.D8,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 4, 2),
                                                                         new WeaponStyleAttackMelee(SkillType.Polearm,          0, 0, "Swing (imp)",  3,   0,   7,  14,  DieType.D12,   DamageType.IMP,   AttackType.SWING,  Charge.Never,  0, 2, 4, 2),
                                                                         new WeaponStyleAttackMelee(SkillType.Polearm,          0, 0,      "Thrust",  2,  -1,   7,   8,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 3, 4, 2),
                                                                         new WeaponStyleParry(      SkillType.Polearm,          0, 0,       "Parry",  2, -99,   7,  1.0, 2)}),
       new WeaponBase(3,15, 750, NAME_Lance,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Jousting,         0, 0,       "Joust",  0, -99,  99,   8,  DieType.D12,   DamageType.IMP,  AttackType.THRUST,Charge.WhenMounted,10,3,3,1)}),
       new WeaponBase(3, 3,  40, NAME_Javelin,        new WeaponStyle[] {new WeaponStyleAttackThrown(0,         0, 4,      DieType.D12,  DamageType.IMP, 20,  1)}),
       new WeaponBase(0, 1,  40, NAME_Knife,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Knife,            0, 0,       "Swing",  0,  -1,  99,   5,   DieType.D4,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 0, 1, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.Knife,            0, 0,      "Thrust",  0,  -1,  99,   7,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 0, 1, 1),
                                                                         new WeaponStyleAttackThrown(0,         0, 5,      DieType.D6,  DamageType.IMP, 10,  1),
                                                                         new WeaponStyleParry(      SkillType.Knife,            0, 0,       "Parry",  0,  -1,  99,  0.5, 1)}),
       //           size,lbs,  $, name                                             2               (SkillType,                 min,pen.,   StyleName,spd,Str-,Str+, dam,  Die,             damageType,     attackType,       charge type, pry-,ranges hands)
       new WeaponBase(3, 3,1200, NAME_Katana,         new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Sword,            0, 0,  "Swing (1h)",  2,  -5,   1,   9,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.Sword,            0, 0, "Thrust (1h)",  1,  -3,   7,   7,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleParry(      SkillType.Sword,            0, 0,  "Parry (1h)",  1,  -5,  99,  1.0, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword,  0, 0,  "Swing (2h)",  1,  -6,   3,  11,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 2),
                                                                         new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword,  0, 0, "Thrust (2h)",  1, -99,   1,   8,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 2, 2),
                                                                         new WeaponStyleParry(      SkillType.TwoHanded_Sword,  0, 0,  "Parry (2h)",  1, -99,   3,  1.0, 2)}),
       new WeaponBase(3, 3,12000, NAME_Katana_Fine,   new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Sword,            0, 0,  "Swing (1h)",  2,  -6,   0,  10,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.Sword,            0, 0, "Thrust (1h)",  1,  -3,   7,   7,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleParry(      SkillType.Sword,            0, 0,  "Parry (1h)",  1,  -6,  99,  1.0, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword,  0, 0,  "Swing (2h)",  1,  -6,   2,  12,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 2),
                                                                         new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword,  0, 0, "Thrust (2h)",  1, -99,   1,   8,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 2, 2),
                                                                         new WeaponStyleParry(      SkillType.TwoHanded_Sword,  0, 0,  "Parry (2h)",  1, -99,   2,  1.0, 2)}),
       new WeaponBase(3, 4, 550, NAME_Longsword,      new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Sword,            0, 0,       "Swing",  2,  -5,   2,  10,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.Sword,            0, 0,      "Thrust",  1,  -2,   8,   7,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleParry(      SkillType.Sword,            0, 0,       "Parry",  1,  -5,  99,  1.0, 1)}),
       new WeaponBase(3, 4,5000, NAME_Longsword_Fine, new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Sword,            0, 0,       "Swing",  2,  -6,   1,  11,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.Sword,            0, 0,      "Thrust",  1,  -2,   8,   7,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleParry(      SkillType.Sword,            0, 0,       "Parry",  1,  -6,  99,  1.0, 1)}),
       new WeaponBase(3, 5, 650, NAME_Broadsword,     new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Sword,            0, 0,       "Swing",  2,  -2,   5,  11,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.Sword,            0, 0,      "Thrust",  1,  -2,   9,   7,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleParry(      SkillType.Sword,            0, 0,       "Parry",  1,  -2,  99,  1.0, 1)}),
       new WeaponBase(3, 7,  50, NAME_Mace,           new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.AxeMace,          0, 0,       "Swing",  2,  -5,   2,   8,   DieType.D4, DamageType.BLUNT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleParry(      SkillType.AxeMace,          0, 0,       "Parry",  1,  -5,  99,  1.0, 1)}),
       new WeaponBase(2, 2,  25, NAME_Nunchucks,      new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.NunChucks,        1, 0,  "Swing (1h)",  1, -99,  99,   4,   DieType.D8, DamageType.BLUNT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.NunChucks,        1, 0,  "Swing (2h)",  0,  -8,  99,   4,   DieType.D8, DamageType.BLUNT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 2),
                                                                         new WeaponStyleParry(      SkillType.NunChucks,        1, 0,       "Parry",  0,  -8,  99,  1.0, 2)}),
       new WeaponBase(5,12, 100, NAME_Maul,           new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.TwoHanded_AxeMace,0, 0,       "Swing",  3,  -2,   6,  12,   DieType.D4, DamageType.BLUNT,   AttackType.SWING,  Charge.Never,  0, 2, 3, 2),
                                                                         new WeaponStyleParry(      SkillType.TwoHanded_AxeMace,0, 0,       "Parry",  2, -99,   6,  1.0, 2)}),
       new WeaponBase(3, 8, 100, NAME_MorningStar,    new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Flail,            1, 0,       "Swing",  2,  -1,   9,   5,  DieType.D10, DamageType.BLUNT,   AttackType.SWING,  Charge.Never,  4, 2, 2, 1),
                                                                         new WeaponStyleParry(      SkillType.Flail,            1, 0,       "Parry",  1,  -1,  99,  0.5, 1)}),
       new WeaponBase(2, 4,  75, NAME_PickAxe,        new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.AxeMace,          0, 0,       "Swing",  2,  -2,   8,  11,  DieType.D10,   DamageType.IMP,   AttackType.SWING,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleParry(      SkillType.AxeMace,          0, 0,       "Parry",  1,  -2,  99,  1.0, 1)}),
       new WeaponBase(3, 4,  20, NAME_Quarterstaff,   new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Quarterstaff,     0, 0,       "Swing",  1,  -3,  99,   8,   DieType.D4, DamageType.BLUNT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 2),
                                                                         new WeaponStyleAttackMelee(SkillType.Quarterstaff,     0, 0,      "Thrust",  0,  -3,  99,   1,   DieType.D8, DamageType.BLUNT,  AttackType.THRUST,Charge.WhenMounted,0,2,2, 2),
                                                                         new WeaponStyleParry(      SkillType.Quarterstaff,     0, 0,       "Parry",  0,  -3,  99,  1.5, 2)}),
       new WeaponBase(2, 2, 600, NAME_Rapier,         new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Fencing,          0, 0,      "Thrust",  1, -99,   1,   7,   DieType.D6,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleParry(      SkillType.Fencing,          0, 0,       "Parry",  1, -99,   1,  1.5, 1)}),
       new WeaponBase(2, 3, 900, NAME_Sabre,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Fencing,          0, 0,       "Swing",  1, -99,   4,   6,   DieType.D4,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.Fencing,          0, 0,      "Thrust",  1, -99,   2,   7,   DieType.D6,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleParry(      SkillType.Fencing,          0, 0,       "Parry",  1, -99,   2,  1.5, 1)}),
       new WeaponBase(2, 3, 400, NAME_Shortsword,     new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Sword,            0, 0,       "Swing",  1,  -2,   7,   8,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.Sword,            0, 0,      "Thrust",  1, -99,   3,   7,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 2, 1),
                                                                         new WeaponStyleParry(      SkillType.Sword,            0, 0,       "Parry",  1, -99,   3,  1.0, 1)}),
       //           size,lbs,  $, name                                             2               (SkillType,                 min,pen.,   StyleName,spd,Str-,Str+, dam,  Die,             damageType,     attackType,       charge type, pry-,ranges hands)
       new WeaponBase(3, 5,  60, NAME_Spear,          new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.Spear,            0, 0, "Thrust (1h)",  2, -99,   6,   7,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 3, 1),
                                                                         new WeaponStyleAttackMelee(SkillType.Spear,            0, 0, "Thrust (2h)",  1,  -2,  99,   8,   DieType.D8,   DamageType.IMP,  AttackType.THRUST,Charge.WhenMounted,0,2,3, 2),
                                                                         new WeaponStyleAttackThrown(0,         0, 5,      DieType.D12,  DamageType.IMP, 16,  1),
                                                                         new WeaponStyleParry(      SkillType.Spear,            0, 0,  "Parry (1h)",  1, -99,  99,  1.0, 1),
                                                                         new WeaponStyleParry(      SkillType.Spear,            0, 0,  "Parry (2h)",  1, -99,  99,  1.0, 2)}),
       new WeaponBase(1, 0.1,25, NAME_ThrowingStar,   new WeaponStyle[] {new WeaponStyleAttackThrown(0,         0, 6,      DieType.D4,  DamageType.IMP, 14,  1)}),
       new WeaponBase(2, 2,  75, NAME_ThreePartStaff, new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.NunChucks,        2, 2,  "Long Swing",  1,  -5,  99,   8,   DieType.D8, DamageType.BLUNT,   AttackType.SWING,  Charge.Never,  0, 3, 4, 2),
                                                                         new WeaponStyleAttackMelee(SkillType.NunChucks,        2, 2, "Short Swing",  1,  -5,  99,   5,   DieType.D8, DamageType.BLUNT,   AttackType.SWING,  Charge.Never,  0, 2, 3, 2),
                                                                         new WeaponStyleParry(      SkillType.NunChucks,        2, 0,       "Parry",  0,  -5,  99,  1.0, 2)}),
       new WeaponBase(3, 9, 850, NAME_TwoHandedSword, new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword,  0, 0,       "Swing",  2,  -3,   7,  13,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 3, 2),
                                                                         new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword,  0, 0,      "Thrust",  2, -99,   2,   8,  DieType.D10,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 3, 2),
                                                                         new WeaponStyleParry(      SkillType.TwoHanded_Sword,  0, 0,       "Parry",  1,  -3,  99,  1.0, 2)}),
       new WeaponBase(3,9,8500,NAME_TwoHandedSword_Fine,new WeaponStyle[]{new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword, 0, 0,       "Swing",  2,  -4,   6,  14,   DieType.D6,   DamageType.CUT,   AttackType.SWING,  Charge.Never,  0, 2, 3, 2),
                                                                         new WeaponStyleAttackMelee(SkillType.TwoHanded_Sword,  0, 0,      "Thrust",  2, -99,   2,   8,  DieType.D10,   DamageType.IMP,  AttackType.THRUST,  Charge.Never,  0, 2, 3, 2),
                                                                         new WeaponStyleParry(      SkillType.TwoHanded_Sword,  0, 0,       "Parry",  1,  -3,  99,  1.0, 2)}),
       new WeaponBase(5, 7, 125, NAME_WarHammer,      new WeaponStyle[] {new WeaponStyleAttackMelee(SkillType.TwoHanded_AxeMace,0, 0,       "Swing",  3,  -3,   4,  18,   DieType.D6,   DamageType.IMP,   AttackType.SWING,  Charge.Never,  0, 2, 3, 2),
                                                                         new WeaponStyleParry(      SkillType.TwoHanded_AxeMace,0, 0,       "Parry",  2, -99,   4,  1.0, 2)}),

                       // size, lbs, $,  name,              SkillType,         min, Pen, dam,        var,     damType, rngBase, hands,      preparation steps (last to first)
       new MissileWeaponBase(0, 1,  10, NAME_BlowGun,       SkillType.BlowGun,    0, 0,   5,  DieType.D4, DamageType.IMP,   12,  1, new String[] {"Inhale and raise blow gun", "Load dart", "Ready dart"}),
       new MissileWeaponBase(5, 5, 750, NAME_BowComposite,  SkillType.Bow,        0, 0,  11, DieType.D10, DamageType.IMP,   60,  2, new String[] {"Draw bow", "Notch arrow", "Ready arrow"}),
       new MissileWeaponBase(4, 5, 400, NAME_BowLongbow,    SkillType.Bow,        0, 0,  10, DieType.D10, DamageType.IMP,   50,  2, new String[] {"Draw bow", "Notch arrow", "Ready arrow"}),
       new MissileWeaponBase(3, 4, 200, NAME_BowShortbow,   SkillType.Bow,        0, 0,   9, DieType.D10, DamageType.IMP,   40,  2, new String[] {"Draw bow", "Notch arrow", "Ready arrow"}),
       new MissileWeaponBase(4, 7, 200, NAME_Crossbow,      SkillType.Crossbow,   0, 0,  10, DieType.D12, DamageType.IMP,   40,  2, new String[] {"Raise crossbow", "Notch bolt", "Ready bolt", "Cock crossbow", "Cock crossbow"}),
       new MissileWeaponBase(6, 9, 300, NAME_CrossbowHeavy, SkillType.Crossbow,   0, 0,  12, DieType.D12, DamageType.IMP,   50,  2, new String[] {"Raise crossbow", "Notch bolt", "Ready bolt", "Cock crossbow", "Cock crossbow", "Cock crossbow"}),
       new MissileWeaponBase(2, 6, 150, NAME_CrossbowLight, SkillType.Crossbow,   0, 0,   8, DieType.D12, DamageType.IMP,   30,  2, new String[] {"Raise crossbow", "Notch bolt", "Ready bolt", "Cock crossbow"}),
       new MissileWeaponBase(0, 1,   5, NAME_Sling,         SkillType.Sling,      1, 0,   0,  DieType.D4, DamageType.BLUNT, 12,  1, new String[] {"Spin sling", "Load stone in sling", "Ready stone"}),
       new MissileWeaponBase(1, 3, 100, NAME_StaffSling,    SkillType.StaffSling, 1, 0,   7,  DieType.D6, DamageType.BLUNT, 20,  2, new String[] {"Spin sling", "Load stone in staff sling", "Ready stone"}),
       };
       // sort this array, so they can be listed in alphabetical order
       Arrays.sort(_weaponsList, new Comparator<SizelessWeapon>() {
           @Override
         public int compare(SizelessWeapon o1, SizelessWeapon o2) {
               return o1.getName().compareTo(o2.getName());
           }
       });
       Arrays.sort(_specialWeaponsList, new Comparator<SizelessWeapon>() {
          @Override
         public int compare(SizelessWeapon o1, SizelessWeapon o2) {
             return o1.getName().compareTo(o2.getName());
          }
       });
   }

   public static void main(String[] args) {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      for (SizelessWeapon weapon : _weaponsList) {
         sb.append("{");
         sb.append(" name: '").append(weapon.getName());
         sb.append("', weight: ").append(weapon.getWeight());
         sb.append(", cost: ").append(weapon.getCost());
         sb.append(", styles: [");
         boolean first = true;
         for (WeaponStyleAttack style : weapon.getAttackStyles()) {
            if (!first) {
               sb.append(",\n       ");
            }
            first = false;
            sb.append("{ name: '").append(style.getName());
            sb.append("', skill: '").append(style.getSkillType().getName());
            sb.append("', minSkill: ").append(style.getMinSkill());
            sb.append(", skillPenalty: ").append(style.getSkillPenalty());
            sb.append(", speedBase: ").append(style.getSpeedBase());
            sb.append(", slowStr: ").append(style.getSlowStr());
            sb.append(", fastStr: ").append(style.getFastStr());
            sb.append(", hands: ").append(style.getHandsRequired());
            sb.append(", attackType: '").append(style.getAttackType().toString().charAt(0)).append(style.getAttackType().toString().toLowerCase().substring(1));
            sb.append("', damageDie: '").append(style.getVarianceDie());
            sb.append("', damageBonus: ").append(style.getDamage((byte) 0));
            sb.append(", damageType: '").append(style.getDamageType().shortname);
            sb.append("', minRange: ").append(style.getMinRange());
            sb.append(", maxRange: ").append(style.getMaxRange());
            sb.append(", parryPenalty: ").append(style.getParryPenalty());
            sb.append("}");
         }
         sb.append("]},\n");
      }
      sb.append("]");
      String results = sb.toString();
      System.out.println(results);
   }

   public static List<String> getWeaponNames(boolean includeNaturalWeapons) {
      List<String> list = new ArrayList<>();
      for (SizelessWeapon element : _weaponsList) {
         if (includeNaturalWeapons || element.isReal()) {
            list.add(element.getName());
         }
      }
      return list;
   }

   // Return true if all weapon styles require both hands.
   public boolean isOnlyTwoHanded()
   {
      for (WeaponStyleAttack attackStyle : _attackStyles) {
         if (!attackStyle.isTwoHanded()) {
            return false;
         }
      }
      return true;
   }
   public boolean isTwoHanded(int attackIndex)
   {
      return _attackStyles[attackIndex].isTwoHanded();
   }
   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder();
      sb.append("Weapon: ").append(_name);
      sb.append(", size: ").append(_size);
      sb.append(", attacks:{");
      for (int i=0 ; i<_attackStyles.length ; i++) {
         sb.append(i).append(':').append(_attackStyles[i]);
      }
      return sb.toString();
   }

   private static List<Weapon> getWeaponListForRace(Race race) {
      List<Weapon> list = new ArrayList<>();
      for (SizelessWeapon weapon : _weaponsList) {
         Weapon copy = weapon.clone();
         copy.setRacialBase(race);
         list.add(copy);
      }
      return list;
   }

   public static String generateHtmlTable() {
      StringBuilder sb = new StringBuilder();
      sb.append(HtmlBuilder.getHTMLHeader("TblWeapon", 600, 44));
      sb.append("<body>\n");
      sb.append("<H4>Weapon data:</H4>\n");
      sb.append("<H3>Melee Weapons:</H3>\n");
      sb.append("<div style=\"overflow: hidden;\" id=\"DivHeaderRow\">\n");
      sb.append("</div>\n");
      sb.append("<div style=\"overflow:scroll;overflow-x:hidden; border-width:0px; border-bottom:1px; border-style:solid;\" onscroll=\"OnScrollDiv(this)\" id=\"DivMainContent\">\n");
      sb.append(getMeleeWeaponTable());
      sb.append("</div>");
      sb.append("<H3>Missile Weapons:</H3>");
      sb.append(getMissileThrownWeaponTable(true/*missile*/, false/*thrown*/));
      sb.append("<H3>Thrown Weapons:</H3>");
      sb.append(getMissileThrownWeaponTable(false/*missile*/, true/*thrown*/));
      sb.append("</body>");
      return sb.toString();
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
      List<Weapon> humanWeapons = getWeaponListForRace(Race.getRace(Race.NAME_Human, Gender.MALE));
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
      for (SizelessWeapon weap : _weaponsList) {
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
   public boolean isMissileWeapon()
   {
      return false;
   }
   public boolean isOnlyThrowable() {
      for (WeaponStyleAttack attack : _attackStyles) {
         if (!attack.isThrown()) {
            return false;
         }
      }
      return true;
   }
   public boolean isThrowable() {
      return getThrownAttackStyle() != null;
   }
   public WeaponStyleAttackThrown getThrownAttackStyle() {
      for (WeaponStyleAttack attack : _attackStyles) {
         if (attack.isThrown()) {
            return (WeaponStyleAttackThrown) attack;
         }
      }
      return null;
   }

   public short getWeaponMaxRange(Character wielder) {
      return getWeaponMaxRange(true/*allowRanged*/, false/*onlyChargeTypes*/, wielder);
   }
   public short getWeaponMaxRange(boolean allowRanged, boolean onlyChargeTypes, Character wielder) {
      short maxRange = -1;
      for (WeaponStyleAttack attackStyle : _attackStyles) {
         if (attackStyle.getMaxRange() > maxRange) {
            if (!onlyChargeTypes || attackStyle.canCharge(wielder.isMounted(), wielder.getLegCount() > 3)) {
               if (allowRanged || !attackStyle.isRanged()) {
                  Skill styleSkill = wielder.getSkill(attackStyle.getSkillType());
                  if ((styleSkill != null) && (styleSkill.getLevel() >= attackStyle.getMinSkill())) {
                     maxRange = attackStyle.getMaxRange();
                  }
               }
            }
         }
      }
      for (WeaponStyleAttackGrapple grapplingStyle : _grapplingStyles) {
         if (grapplingStyle.getMaxRange() > maxRange) {
            if (!onlyChargeTypes || grapplingStyle.canCharge(wielder.isMounted(), wielder.getLegCount() > 3)) {
               if (allowRanged || !grapplingStyle.isRanged()) {
                  Skill styleSkill = wielder.getSkill(grapplingStyle.getSkillType());
                  if ((styleSkill != null) && (styleSkill.getLevel() >= grapplingStyle.getMinSkill())) {
                     maxRange = grapplingStyle.getMaxRange();
                  }
               }
            }
         }
      }
      return maxRange;
   }

   public short getWeaponMinRange(boolean allowRanged, boolean onlyChargeTypes, Character wielder) {
      short minRange = 10000;
      for (WeaponStyleAttack attackStyle : _attackStyles) {
         if (attackStyle.getMinRange() < minRange) {
            if (!onlyChargeTypes || attackStyle.canCharge(wielder.isMounted(), wielder.getLegCount() > 3)) {
               if (allowRanged || !attackStyle.isRanged()) {
                  // For Karate kicks, make sure the attacker has at least 2 non-crippled legs.
                  // Wrestling / Brawling skill types can still use knee strikes
                  if ((_name == NAME_KarateKick) && (attackStyle.getSkillType() == SkillType.Karate)) {
                     int uncrippledLegCount = 0;
                     for (Limb limb : wielder.getLimbs()) {
                        if (limb._limbType.isLeg() && !limb.isCrippled() && !limb.isSevered()) {
                           uncrippledLegCount++;
                        }
                     }
                     if (uncrippledLegCount < 2) {
                        // don't allow this attack style
                        continue;
                     }
                  }
                  Skill styleSkill = wielder.getSkill(attackStyle.getSkillType());
                  if ((styleSkill != null) && (styleSkill.getLevel() >= attackStyle.getMinSkill())) {
                     minRange = attackStyle.getMinRange();
                  }
               }
            }
         }
      }
      for (WeaponStyleAttackGrapple grapplingStyle : _grapplingStyles) {
         if (grapplingStyle.getMinRange() < minRange) {
            if (allowRanged || !grapplingStyle.isRanged()) {
               if (!onlyChargeTypes || grapplingStyle.canCharge(wielder.isMounted(), wielder.getLegCount() > 3)) {
                  Skill styleSkill = wielder.getSkill(grapplingStyle.getSkillType());
                  if ((styleSkill != null) && (styleSkill.getLevel() >= grapplingStyle.getMinSkill())) {
                     minRange = grapplingStyle.getMinRange();
                  }
               }
            }
         }
      }
      return minRange;
   }

   public boolean isWeaponInRange(short minDistance, short maxDistance, boolean allowRanged, boolean onlyChargeTypes, Character wielder) {
      short maxRange = getWeaponMaxRange(allowRanged, onlyChargeTypes, wielder);
      if (maxRange < minDistance) {
         return false;
      }
      short minRange = getWeaponMinRange(allowRanged, onlyChargeTypes, wielder);
      return minRange <= maxDistance;
   }
   public byte getWeaponMaxDamage(Character actor)
   {
      byte maxDamage = -127;
      for (WeaponStyleAttack element : _attackStyles) {
         // bug-fix: If adjustForPain is passed as 'true', then when an attacker is hurt enough that his pain
         //          is greater than his skill, it will appear that he can't attack here.
         if (actor.getSkillLevel(element, false/*adjustForPain*/, null/*useHand*/, true/*sizeAdjust*/, true/*adjustForEncumbrance*/, true/*adjustForHolds*/) > 0) {
            if (element.getDamageMod() > maxDamage) {
               maxDamage = element.getDamageMod();
            }
         }
      }
      return maxDamage;
   }

   @Override
   public String getDefenseName(boolean tensePast, Character defender) {
      return (tensePast) ? "parries" : "parry";
   }

   public int getSize() {
      return _size;
   }

   public void setSpecialDamageModifier(SpecialDamage specialDamageModifier, String specialDamageExplanation) {
      _specialDamageModifier = specialDamageModifier;
      _specialDamageExplanation = specialDamageExplanation;
   }
   public SpecialDamage getSpecialDamageModifier() {
      return _specialDamageModifier;
   }
   public String getSpecialDamageModifierExplanation() {
      return _specialDamageExplanation;
   }
   public WeaponStyleAttackRanged getRangedStyle() {
      if (this instanceof MissileWeapon) {
         return (WeaponStyleAttackRanged)getAttackStyle(0);
      }
      return getThrownAttackStyle();
  }

   @Override
   public byte getHandUseagePenalties(LimbType limbType, Character wielder, SkillType skillType) {
      // nunchucks can be used with either hand:
      if (_name == NAME_Nunchucks) {
         return 0;
      }
      if (_name == NAME_Punch) {
         if (skillType != null) {
            if (skillType == SkillType.Boxing) {
               return 0;
            }
         }
      }
      if (isOnlyTwoHanded()) {
         byte penaltyThis  = Rules.getHandUsePenalty(limbType, wielder.getRace().getArmCount());
         byte penaltyOther = Rules.getHandUsePenalty(limbType.getPairedType(), wielder.getRace().getArmCount());
         return (byte) Math.min(penaltyThis, penaltyOther);
      }
      return super.getHandUseagePenalties(limbType, wielder, skillType);
   }

   @Override
   public DrawnObject drawThing(int size, RGB foreground, RGB background) {
      if (!isReal()) {
         return null;
      }
      DrawnObject obj = new DrawnObject(foreground, background);
      drawOutline(obj, size);
      return obj;
   }
   public void drawOutline(DrawnObject obj, int size) {
      // the handle should be at (0,0)
      if (_name.equals(NAME_TwoHandedSword)) {drawSword        (obj, size*1.0, size*1.0);                return;}
      if (_name.equals(NAME_TwoHandedSword_Fine)) {drawSword   (obj, size*1.0, size*1.0);                return;}
      if (_name.equals(NAME_BastardSword))   {drawSword        (obj, size*0.9, size*1.0);                return;}
      if (_name.equals(NAME_BastardSword_Fine))   {drawSword   (obj, size*0.9, size*1.0);                return;}
      if (_name.equals(NAME_Dagger))         {drawSword        (obj, size*0.5, size*0.75);               return;}
      if (_name.equals(NAME_Knife))          {drawSword        (obj, size*0.5, size*1.0);                return;}
      if (_name.equals(NAME_Katana))         {drawSword        (obj, size*0.75,size*0.9);                return;}
      if (_name.equals(NAME_Katana_Fine))    {drawSword        (obj, size*0.75,size*0.9);                return;}
      if (_name.equals(NAME_Longsword))      {drawSword        (obj, size*0.8, size*1.0);                return;}
      if (_name.equals(NAME_Longsword_Fine)) {drawSword        (obj, size*0.8, size*1.0);                return;}
      if (_name.equals(NAME_Broadsword))     {drawSword        (obj, size*0.8, size*1.2);                return;}
      if (_name.equals(NAME_Rapier))         {drawSword        (obj, size*0.7, size*0.25);               return;}
      if (_name.equals(NAME_Shortsword))     {drawSword        (obj, size*0.7, size*1.0);                return;}
      if (_name.equals(NAME_BlowGun))        {drawClub         (obj, size*0.7, size*0.5, false);         return;}
      if (_name.equals(NAME_Club))           {drawClub         (obj, size*0.7, size*1.0, false);         return;}
      if (_name.equals(NAME_Mace))           {drawClub         (obj, size*0.7, size*1.0, true);          return;}
      if (_name.equals(NAME_Quarterstaff))   {drawQuarterstaff (obj, size*1.0, size*1.0, false);         return;}
      if (_name.equals(NAME_StaffSling))     {drawQuarterstaff (obj, size*1.0, size*1.0, true);          return;}
      if (_name.equals(NAME_Sling))          {drawQuarterstaff (obj, size*0.5, size*1.0, true);          return;}
      if (_name.equals(NAME_Spear))          {drawSpear        (obj, size*1.0, size*1.0);                return;}
      if (_name.equals(NAME_Javelin))        {drawJavelin      (obj, size*0.9, size*1.0);                return;}
      if (_name.equals(NAME_GreatAxe))       {drawGreatAxe     (obj, size*1.0, size*0.7, size*1.0, size*1.0, _name); return;}
      if (_name.equals(NAME_Axe))            {drawGreatAxe     (obj, size*0.7, size*0.7, size*0.7, size*0.7, _name); return;}
      if (_name.equals(NAME_ThrowingAxe))    {drawGreatAxe     (obj, size*0.7, size*0.7, size*0.7, size*0.7, _name); return;}
      if (_name.equals(NAME_Halberd))        {drawGreatAxe     (obj, size*1.5, size*0.5, size*0.8, size*0.8, _name); return;}
      if (_name.equals(NAME_PickAxe))        {drawPickAxe      (obj, size*0.7, size*1.0);                return;}
      if (_name.equals(NAME_WarHammer))      {drawWarHammerMaul(obj, size*0.7, size*1.0, true);          return;}
      if (_name.equals(NAME_Maul))           {drawWarHammerMaul(obj, size*0.7, size*1.0, false);         return;}
      if (_name.equals(NAME_BowComposite))   {drawBow          (obj, size*1.0, size*1.0);                return;}
      if (_name.equals(NAME_BowLongbow))     {drawBow          (obj, size*1.0, size*1.0);                return;}
      if (_name.equals(NAME_BowShortbow))    {drawBow          (obj, size*1.0, size*1.0);                return;}
      if (_name.equals(NAME_Crossbow))       {drawCrossBow     (obj, size*1.0, size*1.0);                return;}
      if (_name.equals(NAME_CrossbowHeavy))  {drawCrossBow     (obj, size*1.0, size*1.0);                return;}
      if (_name.equals(NAME_CrossbowLight))  {drawCrossBow     (obj, size*1.0, size*1.0);                return;}
      if (_name.equals(NAME_Flail))          {}
      if (_name.equals(NAME_Nunchucks))      {}
      if (_name.equals(NAME_MorningStar))    {}
      if (_name.equals(NAME_ThrowingStar))   {}
      if (_name.equals(NAME_ThreePartStaff)) {}
      // Default, for now just draw a sword.
      drawSword(obj, size*1.0, size*1.0);
   }
   private static void drawBow(DrawnObject obj, double length, double width) {
      // the handle should be at (0,0)
      // draw the outside edge of the bow:
      obj.addPoint(((width * -8)/32), ((length * 6)/32));
      obj.addPoint(((width * -7)/32), ((length *4.7)/32));
      obj.addPoint(((width * -6)/32), ((length *3.6)/32));
      obj.addPoint(((width * -5)/32), ((length *2.5)/32));
      obj.addPoint(((width * -4)/32), ((length *1.8)/32));
      obj.addPoint(((width * -3)/32), ((length *1.2)/32));
      obj.addPoint(((width * -2)/32), ((length *0.6)/32));
      obj.addPoint(((width * -1)/32), ((length *0.3)/32));
      obj.addPoint(((width *  0)/32), ((length *  0)/32));
      // make a mirror-image of the bow:
      for (int j=obj.getPointCount()-2 ; j>=0 ; j--) {
         obj.addPoint(Math.abs(obj.getXPoint(j)), obj.getYPoint(j));
      }
      // draw the inside edge of the bow:
      for (int j=obj.getPointCount()-1 ; j>=0 ; j--) {
         obj.addPoint(obj.getXPoint(j), obj.getYPoint(j)-((length*2)/32));
      }
      // draw the string:
      obj.addPoint(((width * -8)/32), ((length * 6)/32));
      obj.addPoint(((width * 8)/32),  ((length * 6)/32));
   }
   private static void drawCrossBow(DrawnObject obj, double length, double width) {
      // the handle should be at (0,0)
      // draw the inside edge of the bow:
      obj.addPoint(((width * -8)/32), ((length *-2.0)/32));
      obj.addPoint(((width * -7)/32), ((length *-3.3)/32));
      obj.addPoint(((width * -6)/32), ((length *-4.4)/32));
      obj.addPoint(((width * -5)/32), ((length *-5.5)/32));
      obj.addPoint(((width * -4)/32), ((length *-6.2)/32));
      obj.addPoint(((width * -3)/32), ((length *-6.8)/32));
      obj.addPoint(((width * -2)/32), ((length *-7.4)/32));
      obj.addPoint(((width * -1)/32), ((length *-7.7)/32));
      obj.addPoint(((width * -1)/32), ((length * 8)/32));
      // make a mirror-image of the bow:
      for (int j=obj.getPointCount()-1 ; j>=0 ; j--) {
         obj.addPoint(Math.abs(obj.getXPoint(j)), obj.getYPoint(j));
      }
      // draw the outside edge of the bow:
      for (int j=obj.getPointCount()-1 ; j>=0 ; j--) {
         if ((j != 9) && (j != 8)) {
            obj.addPoint(obj.getXPoint(j), obj.getYPoint(j)-((length*2)/32));
         }
         else {
            obj.addPoint(obj.getXPoint(j), ((length *-11)/32));
         }
      }
      // draw the string:
      obj.addPoint(((width * -8)/32), ((length * -2)/32));
      obj.addPoint(((width * 8)/32),  ((length * -2)/32));
   }

   private static void drawSword(DrawnObject obj, double length, double width)
   {
      // the handle should be at (0,0)
      obj.addPoint(((width * -2)/32), ((length *   4)/32));
      obj.addPoint(((width * -2)/32), ((length *  -2)/32));
      obj.addPoint(((width * -5)/32), ((length *  -2)/32));
      obj.addPoint(((width * -2)/32), ((length *  -3)/32));
      obj.addPoint(((width *  0)/32), ((length * -26)/32));
      obj.addPoint(((width *  2)/32), ((length *  -3)/32));
      obj.addPoint(((width *  5)/32), ((length *  -2)/32));
      obj.addPoint(((width *  2)/32), ((length *  -2)/32));
      obj.addPoint(((width *  2)/32), ((length *   4)/32));
   }
   private static void drawClub(DrawnObject obj, double length, double width, boolean withHead)
   {
      // the handle should be at (0,0)
      obj.addPoint(((width * -2)/32), ((length *   4)/32));
      obj.addPoint(((width * -2)/32), ((length * -24)/32));
      if (withHead) {
         obj.addPoint(((width * -1)/32), ((length * -24)/32));
         obj.addPoint(((width * -1)/32), ((length * -26)/32));
         obj.addPoint(((width * -4)/32), ((length * -29)/32));
         obj.addPoint(((width *  0)/32), ((length * -32)/32));
         obj.addPoint(((width *  4)/32), ((length * -29)/32));
         obj.addPoint(((width *  1)/32), ((length * -26)/32));
         obj.addPoint(((width *  1)/32), ((length * -24)/32));
      }
      obj.addPoint(((width *  2)/32), ((length * -23)/32));
      obj.addPoint(((width *  2)/32), ((length *   4)/32));
   }
   private static void drawQuarterstaff(DrawnObject obj, double length, double width, boolean withSlingHead)
   {
      // the handle should be at (0,0)
      obj.addPoint(((width * -2)/32), ((length *  26)/32));
      obj.addPoint(((width * -2)/32), ((length * -26)/32));
      obj.addPoint(((width *  2)/32), ((length * -26)/32));
      if (withSlingHead) {
         obj.addPoint(((width *  5)/32), ((length *  -26)/32));
         obj.addPoint(((width *  7)/32), ((length *  -20)/32));
         obj.addPoint(((width *  7)/32), ((length *  -19)/32));
         obj.addPoint(((width *  6)/32), ((length *  -20)/32));
         obj.addPoint(((width *  2)/32), ((length *  -25)/32));
      }
      obj.addPoint(((width *  2)/32), ((length *  26)/32));
   }
   private static void drawSpear(DrawnObject obj, double length, double width)
   {
      // the handle should be at (0,0)
      obj.addPoint(((width * -2)/32), ((length *  26)/32));
      obj.addPoint(((width * -2)/32), ((length * -23)/32));
      obj.addPoint(((width * -4)/32), ((length * -25)/32));
      obj.addPoint(((width *  0)/32), ((length * -30)/32));
      obj.addPoint(((width *  4)/32), ((length * -25)/32));
      obj.addPoint(((width *  2)/32), ((length * -23)/32));
      obj.addPoint(((width *  2)/32), ((length *  26)/32));
   }
   private static void drawGreatAxe(DrawnObject obj, double poleLength, double poleWidth, double headLength, double headWidth, String name)
   {
      // the handle should be at (0,0)
      obj.addPoint(((poleWidth *  2)/32), ((poleLength *  10)/32));
      obj.addPoint(((poleWidth * -2)/32), ((poleLength *  10)/32));
      obj.addPoint(((poleWidth * -2)/32), ((headLength * -15)/32));
      obj.addPoint(((headWidth * -4)/32), ((headLength * -13)/32));
      obj.addPoint(((headWidth * -4)/32), ((headLength *  -8)/32));
      obj.addPoint(((headWidth * -6)/32), ((headLength * -10)/32));
      obj.addPoint(((headWidth * -8)/32), ((headLength * -13)/32));
      obj.addPoint(((headWidth * -9)/32), ((headLength * -17)/32));
      obj.addPoint(((headWidth * -9)/32), ((headLength * -19)/32));
      obj.addPoint(((headWidth * -8)/32), ((headLength * -23)/32));
      obj.addPoint(((headWidth * -6)/32), ((headLength * -26)/32));
      obj.addPoint(((headWidth * -4)/32), ((headLength * -28)/32));
      obj.addPoint(((headWidth * -4)/32), ((headLength * -23)/32));
      obj.addPoint(((headWidth * -2)/32), ((headLength * -21)/32));

      obj.addPoint(((poleWidth * -2)/32), ((headLength * -25)/32));
      obj.addPoint(((poleWidth *  2)/32), ((headLength * -25)/32));
      if (name.equals(NAME_Halberd)){
         // make a point on the back side:
         obj.addPoint(((headWidth * 1)/32),  ((headLength * -16)/32));
         obj.addPoint(((headWidth * 11)/32), ((headLength * -18)/32));
         obj.addPoint(((headWidth * 1)/32),  ((headLength * -20)/32));
      }
      else if (name.equals(NAME_GreatAxe)){
         // make a mirror-image of the head blade:
         for (int j=obj.getPointCount()-2 ; j>0 ; j--) {
            obj.addPoint(Math.abs(obj.getXPoint(j)), obj.getYPoint(j));
         }
      }
   }
   private static void drawPickAxe(DrawnObject obj, double length, double width)
   {
      // the handle should be at (0,0)
      obj.addPoint(((width *  2)/32), ((length *  10)/32));
      obj.addPoint(((width * -2)/32), ((length *  10)/32));
      obj.addPoint(((width * -2)/32), ((length * -15)/32));
      obj.addPoint(((width * -6)/32), ((length * -14)/32));
      obj.addPoint(((width *-10)/32), ((length * -13)/32));
      obj.addPoint(((width *-14)/32), ((length * -12)/32));
      obj.addPoint(((width *-18)/32), ((length * -11)/32));
      obj.addPoint(((width *-14)/32), ((length * -14)/32));
      obj.addPoint(((width *-10)/32), ((length * -17)/32));
      obj.addPoint(((width * -6)/32), ((length * -18)/32));
      obj.addPoint(((width * -2)/32), ((length * -19)/32));

      obj.addPoint(((width * -2)/32), ((length * -25)/32));
      obj.addPoint(((width *  2)/32), ((length * -25)/32));

      obj.addPoint(((width * 2)/32), ((length * -19)/32));
      obj.addPoint(((width *11)/32), ((length * -17)/32));
      obj.addPoint(((width * 6)/32), ((length * -14)/32));
      obj.addPoint(((width * 2)/32), ((length * -15)/32));
   }
   private static void drawWarHammerMaul(DrawnObject obj, double length, double width, boolean withPoint)
   {
      // the handle should be at (0,0)
      obj.addPoint(((width *  2)/32), ((length *  20)/32));
      obj.addPoint(((width * -2)/32), ((length *  20)/32));

      if (withPoint) {
         obj.addPoint(((width * -2)/32), ((length * -15)/32));
         obj.addPoint(((width * -6)/32), ((length * -17)/32));
         obj.addPoint(((width *-18)/32), ((length * -20)/32));
         obj.addPoint(((width * -6)/32), ((length * -23)/32));
         obj.addPoint(((width * -2)/32), ((length * -25)/32));
      }
      else {
         obj.addPoint(((width * -2)/32), ((length * -15)/32));
         obj.addPoint(((width * -6)/32), ((length * -15)/32));
         obj.addPoint(((width *-10)/32), ((length * -17)/32));
         obj.addPoint(((width *-10)/32), ((length * -24)/32));
         obj.addPoint(((width * -6)/32), ((length * -26)/32));
         obj.addPoint(((width * -2)/32), ((length * -26)/32));
      }

      obj.addPoint(((width * -2)/32), ((length * -30)/32));
      obj.addPoint(((width *  2)/32), ((length * -30)/32));

      obj.addPoint(((width * 2)/32), ((length * -26)/32));
      obj.addPoint(((width * 6)/32), ((length * -26)/32));
      obj.addPoint(((width *10)/32), ((length * -24)/32));
      obj.addPoint(((width *10)/32), ((length * -17)/32));
      obj.addPoint(((width * 6)/32), ((length * -15)/32));
      obj.addPoint(((width * 2)/32), ((length * -15)/32));
   }
   private static void drawJavelin(DrawnObject obj, double length, double width)
   {
      // the handle should be at (0,0)
      obj.addPoint(((width * -2)/32), ((length *  26)/32));
      obj.addPoint(((width * -2)/32), ((length * -26)/32));
      obj.addPoint(((width *  0)/32), ((length * -29)/32));
      obj.addPoint(((width *  2)/32), ((length * -26)/32));
      obj.addPoint(((width *  2)/32), ((length *  26)/32));
   }

   public boolean canMeleeAttack() {
      return _attackStyles.length > 0;
   }
   public boolean canGrappleAttack(Character self) {

      int armCount = -1;
      for (WeaponStyleAttackGrapple grapple : _grapplingStyles ) {
         if (self.getSkillLevel(grapple.getSkillType(), null, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, true/*adjustForHolds*/) >= grapple.getMinSkill()) {
            if (armCount == -1) {
               armCount = self.getUncrippledArmCount(true/*reduceCountForTwoHandedWeaponsHeld*/);
            }
            return armCount >= grapple.getHandsRequired();
         }
      }
      return false;
   }

   public WeaponStyleParry[] getParryStyles() {
      return _parryStyles;
   }
}

interface SizelessWeapon extends Cloneable {
   String getName();
   Weapon copyWithRace(Race racialBase);
   Weapon clone();
   boolean isReal();
   double getWeight();
   int getCost();
   WeaponStyleParry[] getParryStyles();
   WeaponStyleAttack[] getAttackStyles();
}

class WeaponBase extends Weapon implements SizelessWeapon
{
   public WeaponBase(int size, double weight, int cost, String name, WeaponStyle[] styleModes) {
      super(size, null/*racialBase*/, weight, cost, name, styleModes);
   }
   @Override
   public Weapon copyWithRace(Race racialBase) {
      setRacialBase(racialBase);
      Weapon weap = clone();
      setRacialBase(null);
      return weap;
   }
}

class MissileWeaponBase extends MissileWeapon implements SizelessWeapon
{
   public MissileWeaponBase(int size, double weight, int cost, String name,
                            SkillType skillType, int minSkill, int skillPenalty, int damageMod,
                            DieType varianceDie, DamageType damageType, int rangeBase,
                            int handsRequired, String[] preparationSteps)
   {
      super(size, null/*racialBase*/, weight, cost, name, skillType, minSkill, skillPenalty, damageMod,
            varianceDie, damageType, rangeBase, handsRequired, preparationSteps);
   }
   public MissileWeaponBase(int size, double weight, int cost, String name, WeaponStyle style)
   {
      super(size, null/*racialBase*/, weight, cost, name, style);
   }
   @Override
   public Weapon copyWithRace(Race racialBase) {
      setRacialBase(racialBase);
      Weapon weap = clone();
      setRacialBase(null);
      return weap;
   }
}
