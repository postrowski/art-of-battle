package ostrowski.combat.common.things;

import org.eclipse.swt.graphics.RGB;
import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.*;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.weaponStyles.*;

import java.util.ArrayList;
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


   public static void main(String[] args) {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      for (SizelessWeapon weapon : Weapons._weaponsList) {
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
                  if ((_name.equals(NAME_KarateKick)) && (attackStyle.getSkillType() == SkillType.Karate)) {
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
      if (_name.equals(NAME_Nunchucks)) {
         return 0;
      }
      if (_name.equals(NAME_Punch)) {
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
//      if (_name.equals(NAME_Flail))          {}
//      if (_name.equals(NAME_Nunchucks))      {}
//      if (_name.equals(NAME_MorningStar))    {}
//      if (_name.equals(NAME_ThrowingStar))   {}
//      if (_name.equals(NAME_ThreePartStaff)) {}
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
         if (self.getSkillLevel(grapple.getSkillType(), null, false/*sizeAdjust*/,
                                true/*adjustForEncumbrance*/, true/*adjustForHolds*/) >= grapple.getMinSkill()) {
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

   @Override
   public Weapon copyWithRace(Race racialBase) {
      setRacialBase(racialBase);
      Weapon weap = clone();
      setRacialBase(null);
      return weap;
   }
}
