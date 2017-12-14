package ostrowski.combat.common.things;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Advantage;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.DefenseOptions;
import ostrowski.combat.common.DrawnObject;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.Skill;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackRanged;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackThrown;
import ostrowski.combat.common.weaponStyles.WeaponStyleCounterAttack;
import ostrowski.combat.common.weaponStyles.WeaponStyleParry;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestDefense;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.BattleTerminatedException;

public class Hand extends Limb {
   public Thing _heldThing           = null;
   public byte  _preparedState       = 0;
   public boolean _attackedThisRound = false;

   private byte _defenseStyle        = 0;

   public Hand() {}
   public Hand(LimbType id, Race racialBase) {
      super(id, racialBase);
   }
   @Override
   public String getHeldThingName() {
      if (_heldThing == null) {
         return "";
      }
      return _heldThing.getName();
   }
   @Override
   public Thing getHeldThing() { return _heldThing;}
   /**
    * returns true if we are able to set the current held thing.
    * @see ostrowski.combat.common.Limb#setHeldThing(ostrowski.combat.common.things.Thing)
    */
   @Override
   public boolean setHeldThing(Thing thing, Character self) {
      if (thing != null) {
         if (thing.getRacialBase() == null) {
            //DebugBreak.debugBreak(thing.getName() + "Thing has no racial base.");
         }
         else if (!self.getRace().getName().equals(thing.getRacialBase().getName())) {
           // Rules.debugBreak();
         }
         if (thing.getName().equals(Weapon.NAME_KarateKick)) {
            DebugBreak.debugBreak();
         }
         if ((_heldThing != null) && (_heldThing.isReal())) {
            return false;
         }
         if (isCrippled()) {
            return false;
         }
      }
      else {
         // Are we going from nothing to nothing?
         if (_heldThing == null) {
            return true;
         }
      }
      _heldThing = thing;
      setActionsNeededToReady((byte) 0);
      setAttackStyle((byte) 0);
      _defenseStyle = 0;
      if (_heldThing instanceof Weapon) {
         Weapon weap = (Weapon) _heldThing;
         if (weap.isMissileWeapon()) {
            WeaponStyleAttackRanged style = (WeaponStyleAttackRanged) weap.getAttackStyle(0);
            _preparedState = style.getNumberOfPreparationSteps();
         }
         else {
            WeaponStyleAttackThrown style = weap.getThrownAttackStyle();
            if (style != null) {
               _preparedState = style.getNumberOfPreparationSteps();
            }
            else {
               _preparedState = 0;
            }
         }
         if (weap.isOnlyTwoHanded()) {
            Hand otherHand = (Hand) self.getLimb(_limbType.getPairedType());
            if (otherHand._heldThing != null) {
               setActionsNeededToReady((byte) 1);
            }
         }
      }
      Hand otherHand = (Hand) self.getLimb(_limbType.getPairedType());
      if (otherHand._heldThing != null) {
         if (otherHand._heldThing instanceof Weapon) {
            Weapon otherHandWeapon = (Weapon) otherHand._heldThing;
            if (otherHandWeapon != null) {
               if (otherHandWeapon.isOnlyTwoHanded()) {
                  otherHand.setActionsNeededToReady((byte) 1);
               }
            }
         }
      }

      return true;
   }

   public byte getPreparedState() { return _preparedState; }
   public void setPreparedState(byte state) { _preparedState = state; }
   public void prepareState(byte actions) {
      if (_heldThing != null) {
         if (_heldThing instanceof Weapon) {
            _preparedState -= actions;
            if (_preparedState < 0) {
               _preparedState = 0;
            }
         }
      }
   }
   public String getWeaponPrepareState() {
      if (_heldThing != null) {
         if (_heldThing instanceof Weapon) {
            Weapon weap = (Weapon)_heldThing;
            WeaponStyleAttackRanged rangedStyle;
            if (_heldThing instanceof MissileWeapon) {
               rangedStyle = (WeaponStyleAttackRanged)weap.getAttackStyle(0);
            }
            else {
               rangedStyle = weap.getThrownAttackStyle();
            }
            if (rangedStyle != null) {
               return rangedStyle.getPreparationStepName(weap.getName(), _preparedState);
            }
         }
      }
      return "";
   }
   public boolean canPrepare() {
      if (_heldThing == null) {
         return false;
      }
      if (_heldThing instanceof Weapon) {
         if (((Weapon)_heldThing).getRangedStyle() == null) {
            return false;
         }
      }
      return ((_preparedState != 0) && (getActionsNeededToReady() == 0));
   }

   public byte getParryStyle() {
      return _defenseStyle;
   }
   public void setParryStyle(byte style) {
      _defenseStyle = style;
   }

   public byte getWeaponMaxDamage(byte damageBase, Character attacker) {
      Weapon weap = getWeapon(attacker);
      if (weap != null) {
         byte dam = weap.getWeaponMaxDamage(attacker);
         if (dam != -127) {
            return (byte) (dam + damageBase);
         }
      }
      return -127;
   }

   @Override
   public Weapon getWeapon(Character self) {
      if (_heldThing == null) {
         if (self != null) {
            if (self.getRace().hasProperty(Race.PROPERTIES_CLAWS)) {
               return Weapon.getWeapon(Weapon.NAME_Claws, self.getRace());
            }
            if (self.hasAdvantage(Race.PROPERTIES_0_ARMS)) {
               //Rules.debugBreak();
               return null;
            }

            Hand otherHand = (Hand) self.getLimb(_limbType.getPairedType());
            if ((otherHand != null) && (otherHand._heldThing != null)) {
               if (otherHand._heldThing instanceof Weapon) {
                  Weapon otherHandWeapon = (Weapon) otherHand._heldThing;
                  if (otherHandWeapon != null) {
                     if (otherHandWeapon.isOnlyTwoHanded()) {
                        return null;
                     }
                  }
               }
            }
         }
         return Weapon.getWeapon(Weapon.NAME_Punch, self.getRace());
      }
      if (_heldThing instanceof Weapon) {
         return (Weapon) _heldThing;
      }
      return null;
   }
   public Weapon getWeapon() {
      return getWeapon(null);
   }

   @Override
   public Object clone()
   {
      Hand newHand = new Hand(_limbType, getRacialBase());
      this.copyDataInto(newHand);
      return newHand;
   }
   @Override
   public void copyDataInto(Limb dest)
   {
      super.copyDataInto(dest);
      if (dest instanceof Hand) {
         Hand hand = (Hand) dest;
         hand._heldThing         = (_heldThing == null) ? null : (Thing) _heldThing.clone();
         hand._attackedThisRound = _attackedThisRound;
         hand._preparedState     = _preparedState;
         hand._defenseStyle      = _defenseStyle;
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         super.serializeFromStream(in);
         String thingName = readString(in);
         if ((thingName == null) || (thingName.length() == 0)) {
            _heldThing = null;
         }
         else {
            _heldThing = Thing.getThing(thingName, getRacialBase());
            if (_heldThing != null) {
               _heldThing.serializeFromStream(in);
            }
         }
         _attackedThisRound = readBoolean(in);
         _preparedState     = readByte(in);
         _defenseStyle      = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         super.serializeToStream(out);
         writeToStream(((_heldThing == null) ? "" : _heldThing.getName()), out);
         if (_heldThing != null) {
            _heldThing.serializeToStream(out);
         }
         writeToStream(_attackedThisRound, out);
         writeToStream(_preparedState, out);
         writeToStream(_defenseStyle, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public byte getAttackTime(byte attrStr, Character attacker) {
      Weapon weap = getWeapon(attacker);
      if (weap != null) {
         if (weap._attackStyles.length > getAttackStyle()) {
            return weap._attackStyles[getAttackStyle()].getSpeed(attrStr);
         }
      }
      return 0;
   }
   @Override
   public byte getDefenseTime(byte attrStr, Character attacker) {
      Weapon weap = getWeapon(attacker);
      if (weap != null) {
         if (( _defenseStyle >= 0) && (weap._parryStyles.length > _defenseStyle))
          {
            return weap._parryStyles[_defenseStyle].getSpeed(attrStr);
         // If we are holding a shield, the 'parry' (== block) time will be zero
         }
      }
      return 0;
   }
   @Override
   public String getDefenseName(boolean tensePast, Character defender) {
      if (_heldThing == null) {
         Weapon weap = getWeapon(defender);
         if (weap == null) {
            return null;
         }
         return weap.getDefenseName(tensePast, defender);
      }
      return _heldThing.getDefenseName(tensePast, defender);
   }
   @Override
   public void applyAction(RequestAction action, byte attrStr, Character actor, Arena arena) throws BattleTerminatedException
   {
      if (_limbType == action.getLimb()) {
         if (action.isPrepareRanged()) {
            prepareState((byte)1/*actions*/);
         }
         else if (action.isAttack()) {
            setAttackStyle((byte) action._styleRequest.getAnswerIndex());
            resetMissileWeapon();
            setActionsNeededToReady((byte) (getActionsNeededToReady() + getAttackTime(attrStr, actor)));
            _attackedThisRound = true;
         }
         else if (action.isReadyWeapon()) {
            setActionsNeededToReady((byte) (getActionsNeededToReady() - action.getActionsUsed()));
         }
         else if (action.isApplyItem()) {
            if (_heldThing.apply(actor, arena)) {
               _heldThing = null;
            }
         }
      }
   }
   @Override
   public void applyDefense(RequestDefense defense, byte attrStr, Character defender)
   {
      if (new DefenseOptions(defense.getAnswerID()).contains(getDefOption())) {
         setActionsNeededToReady((byte) (getActionsNeededToReady() + getDefenseTime(attrStr, defender)));
      }
   }
   public boolean resetMissileWeapon()
   {
      if (_heldThing != null) {
         if (_heldThing instanceof Weapon) {
            Weapon weap = (Weapon) _heldThing;
            // reset the weaponState
            if (weap.isMissileWeapon()) {
               WeaponStyleAttackRanged style = (WeaponStyleAttackRanged) weap.getAttackStyle(0);
               setPreparedState(style.getNumberOfPreparationSteps());
               return true;
            }
         }
      }
      return false;
   }

   @Override
   public Thing dropThing()
   {
      Thing thingDropped = _heldThing;
      _heldThing = null;
      return thingDropped;
   }
   @Override
   public boolean isEmpty()
   {
      if (_heldThing == null) {
         return true;
      }
      return !_heldThing.isReal();
   }
   @Override
   public boolean canDefendAgainstRangedWeapons() {
      if (_heldThing != null) {
         return _heldThing.canDefendAgainstRangedWeapons();
      }
      return false;
   }
   @Override
   public boolean canDefend(Character defender, boolean attackIsRanged, boolean attackIsCharge, boolean attackIsGrapple, DamageType damageType, boolean checkState) {
      if (isCrippled()) {
         return false;
      }
      if (checkState) {
         if (getActionsNeededToReady() != 0) {
            return false;
         }
         if (_attackedThisRound) {
            return false;
         }
      }
      if (_heldThing == null) {
         if (attackIsRanged) {
            return false;
         }
         if (defender.hasAdvantage(Advantage.HANDS_0) ||
             defender.hasAdvantage(Race.PROPERTIES_0_ARMS) ||
             defender.hasAdvantage(Race.PROPERTIES_CLAWS)) {
            return false;
         }
         // Fire and electric attacks can not be defended with an unarmed combat skill
         if ((damageType == DamageType.FIRE) || (damageType == DamageType.ELECTRIC)) {
            return false;
         }

         Limb pairedLimb = defender.getLimb(_limbType.getPairedType());
         if ((pairedLimb != null) && (!pairedLimb.isCrippled())) {
            Weapon pairedHandsWeapon = pairedLimb.getWeapon(defender);
            if (pairedHandsWeapon != null) {
               int defStyle = ((Hand)pairedLimb)._defenseStyle;
               if ((pairedHandsWeapon._parryStyles.length > defStyle) && (defStyle>0)) {
                  WeaponStyleParry otherParry = pairedHandsWeapon._parryStyles[defStyle];
                  if (otherParry.isTwoHanded()) {
                     return false;
                  }
               }
            }
         }
         Weapon punch = Weapon.getWeapon(Weapon.NAME_Punch, defender.getRace());

         WeaponStyleParry[] parrySkills = punch.getParryStyles();
         byte bestLevel = -1;
         byte bestIndex = -1;
         for (byte i=0 ; i<parrySkills.length ; i++) {
            WeaponStyleParry parrySkill = parrySkills[i];
            if (parrySkill.canDefendAgainstDamageType(damageType, attackIsGrapple)) {
               SkillType styleType = parrySkill.getSkillType();
               Skill skill = defender.getSkill(styleType);
               if (skill != null) {
                  if (skill.getLevel() >= parrySkill.getMinSkill()) {
                     byte skillLevel = (byte) (skill.getLevel() * parrySkill.getEffectiveness());
                     if ((bestIndex == -1) || (skillLevel > bestLevel)) {
                        bestLevel = skillLevel;
                        bestIndex = i;
                     }
                  }
               }
            }
         }
         if (bestIndex != -1) {
            _defenseStyle = bestIndex;
            return true;
         }
         return false;
      }
      if (_heldThing instanceof Weapon) {
         if (attackIsRanged || attackIsCharge) {
            return false;
         }
         Weapon weap = (Weapon) _heldThing;
         return (weap._parryStyles.length > 0);
      }
      if (_heldThing instanceof Shield) {
         return true;
      }
      // not a shield or weapon - can't defend!
      return false;
   }
   @Override
   public boolean canAttack(Character character) {
      if (super.canAttack(character)) {
         if (_attackedThisRound) {
            return false;
         }
         Weapon weap = getWeapon(character);
         // Missile weapons must be prepared before they can be used to attack.
         if (weap instanceof MissileWeapon) {
            return (_preparedState == 0);
         }
         return true;
      }
      return false;
   }
   public boolean canCounterAttack(Character defender, boolean grab) {
      if (super.canAttack(defender)) {
         if (_attackedThisRound) {
            return false;
         }
         Weapon punch = Weapon.getWeapon(Weapon.NAME_Punch, defender.getRace());
         for (WeaponStyleCounterAttack parrySkill : punch._counterattackStyles) {
            if (parrySkill.getName().toLowerCase().contains("grab") == grab) {
               SkillType styleType = parrySkill.getSkillType();
               Skill skill = defender.getSkill(styleType);
               if (skill != null) {
                  if (skill.getLevel() >= parrySkill.getMinSkill()) {
                     return true;
                  }
               }
            }
         }
      }
      return false;
   }
   @Override
   public void endRound()
   {
      _attackedThisRound = false;
   }
   public byte getDefenseTN(Character self, boolean rangedAttack, boolean isChargeAttack, boolean isGrappleAttack, DamageType damageType) {
      // If this hand is crippled, return 0
      if (isCrippled()) {
         return 0;
      }

      byte defLevel = getDefenseTNWithoutWounds(self, rangedAttack, isChargeAttack, isGrappleAttack, damageType, true/*checkState*/);
      defLevel -= getWoundPenalty();
      if (defLevel < 0) {
         defLevel = 0;
      }
      return defLevel;
   }
   @Override
   public byte getDefenseTNWithoutWounds(Character self, boolean rangedAttack, boolean isChargeAttack, boolean isGrappleAttack, DamageType damageType, boolean checkState) {
      if (canDefend(self, rangedAttack, isChargeAttack, isGrappleAttack, damageType, checkState)) {
         Weapon weap = getWeapon(self);
         if (weap != null) {
            if ((_defenseStyle < 0) || (_defenseStyle >= weap._parryStyles.length)) {
               DebugBreak.debugBreak();
               return 0;
            }
            LimbType handUse = _limbType;
            if (!weap.isReal()) {
               // unarmed combat doesn't get penalties for off-hand when used to parry:
               handUse = null;
            }
            byte parrySkill = self.getSkillLevel(weap._parryStyles[_defenseStyle].getSkillType(), handUse, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, true/*adjustForHolds*/);
            return Rules.getParryLevel(parrySkill, weap._parryStyles[_defenseStyle].getEffectiveness());
         }
         else if (_heldThing instanceof Shield) {
            byte shieldSkill = self.getSkillLevel(SkillType.Shield, _limbType, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, true/*adjustForHolds*/);
            return Rules.getBlockLevel(shieldSkill);
         }
      }
      return 0;
   }
   @Override
   public byte getPenaltyForMassiveDamage(Character self, byte minDamageAttacking,
                                          boolean rangedAttack, boolean isChargeAttack, boolean isGrappleAttack,
                                          DamageType damageType, boolean checkState) {
      double tnWithHand = getDefenseTNWithoutWounds(self, rangedAttack, isChargeAttack, isGrappleAttack, damageType, checkState);
      byte maxDamageDealt = -127;
      Weapon weap = getWeapon(self);
      if (weap != null) {
         byte bestOptLevel = weap.getBestDefenseOption(self, _limbType, false/*canUseTwoHands*/, damageType, isGrappleAttack);
         for (WeaponStyleParry parryStyle : weap._parryStyles) {
            if (parryStyle.getSkillType() == SkillType.Aikido)  {
               byte skillLevel = (byte) (self.getSkillLevel(parryStyle.getSkillType(), _limbType, false/*sizeAdjust*/,
                                                            false/*adjustForEncumbrance*/, true/*adjustForHolds*/)
                                         - parryStyle.getSkillPenalty());
               byte parryLevel = Rules.getParryLevel(skillLevel, parryStyle.getEffectiveness());
               if (bestOptLevel == parryLevel) {
                  Rules.diag("getPenaltyForMassiveDamage: self=" + self.getName() +
                             ", handID=" + _limbType +
                             ", maxDamageDealt=" + maxDamageDealt +
                             ", minDamageAttacking=" + minDamageAttacking +
                             ", tnWithHand=" + tnWithHand +
                             ", penalty=0, due to Aikido skill parry");
                  return 0;
               }
            }
         }
         maxDamageDealt = getWeaponMaxDamage(self.getPhysicalDamageBase(), self);
      }
      else if (_heldThing instanceof Shield) {
         // Block with shields act like a parry, with shields doing 0 damage
         maxDamageDealt = self.getPhysicalDamageBase();
         maxDamageDealt += ((Shield) _heldThing).getDamage();
         // TODO: allow the option to defend at full TN, but if a near-miss or a hit
         //       occurs, damage is applied, but reduced by the shield (15 points)
      }
      if (maxDamageDealt != -127) {
         int damageDiff = minDamageAttacking - maxDamageDealt;
         if (damageDiff > 0) {
            // For every 10 points of damage, reduce the defense skill by 50%
            byte penalty = (byte) Math.round(tnWithHand - (tnWithHand / Math.pow(2, (damageDiff / 10))));
            Rules.diag("getPenaltyForMassiveDamage: self=" + self.getName() +
                       ", handID=" + _limbType +
                       ", maxDamageDealt=" + maxDamageDealt +
                       ", minDamageAttacking=" + minDamageAttacking +
                       ", tnWithHand=" + tnWithHand +
                       ", penalty=" + penalty);
            return penalty;
         }
      }
      return 0;
   }

   public boolean isDefenseParry(Character character) {
      if (_heldThing != null) {
         return "parry".equalsIgnoreCase(_heldThing.getActiveDefenseName());
      }
      if ((character.getSkillLevel(SkillType.Aikido,    null, false/*sizeAdjust*/, false/*adjustForEncumbrance*/, false/*adjustForHolds*/) == 0) &&
          (character.getSkillLevel(SkillType.Karate,    null, false/*sizeAdjust*/, false/*adjustForEncumbrance*/, false/*adjustForHolds*/) == 0) &&
          (character.getSkillLevel(SkillType.Boxing,    null, false/*sizeAdjust*/, false/*adjustForEncumbrance*/, false/*adjustForHolds*/) == 0) &&
          (character.getSkillLevel(SkillType.Brawling,  null, false/*sizeAdjust*/, false/*adjustForEncumbrance*/, false/*adjustForHolds*/) == 0) &&
          (character.getSkillLevel(SkillType.Wrestling, null, false/*sizeAdjust*/, false/*adjustForEncumbrance*/, false/*adjustForHolds*/) == 0)) {
         return false;
      }
      return true;
//      Weapon punch = Weapon.getWeapon("punch");
//      return punch.getActiveDefenseName().equals("parry");
   }
   public static byte getHandIndex(Pair armPair, Side armSide)
   {
      byte index = 0;
      switch (armPair) {
         case FIRST:  index = 0; break;
         case SECOND: index = 2; break;
         case THIRD:  index = 4; break;
      }
      switch (armSide) {
         case RIGHT:  index += 0; break;
         case LEFT:   index += 1; break;
      }
      return index;
   }
   @Override
   public boolean canBeReadied(Character self) {
      if (_heldThing == null) {
         return false;
      }
      Hand otherHand = (Hand) self.getLimb(_limbType.getPairedType());
      if (otherHand._heldThing == null) {
         return super.canBeReadied(self);
      }

      if (_heldThing instanceof Weapon) {
         Weapon weap = (Weapon) _heldThing;
         if (weap.isOnlyTwoHanded()) {
            if (otherHand._heldThing != null) {
               return false;
            }
         }
      }
      if (otherHand._heldThing instanceof Weapon) {
         Weapon otherHandWeapon = (Weapon) otherHand._heldThing;
         if (otherHandWeapon != null) {
            if (otherHandWeapon.isOnlyTwoHanded()) {
               return false;
            }
         }
      }
      return super.canBeReadied(self);
   }

   @Override
   public DrawnObject drawThing(int narrowDiameter, int wideDiameter, RGB foreground, RGB background)
   {
      if (getLocationSide() == Wound.Side.LEFT) {
         wideDiameter *= -1;
      }

      DrawnObject obj = new DrawnObject(foreground, background);
      obj.addPoint((wideDiameter * 19)/32, 0);                         // outer shoulder
      obj.addPoint((wideDiameter * 16)/32, 0-((narrowDiameter * 16)/32));  // outer elbow
      obj.addPoint((wideDiameter *  5)/32, 0-((narrowDiameter * 25)/32));  // outer wrist
      obj.addPoint((wideDiameter *  4)/32, 0-((narrowDiameter * 22)/32));  // wrist
      obj.addPoint((wideDiameter * 13)/32, 0-((narrowDiameter * 13)/32));  // elbow
      obj.addPoint((wideDiameter * 14)/32, 0);                         // shoulder
      obj.addPoint((wideDiameter * 16)/32, (narrowDiameter * 3)/32);     // shoulder
      return obj;
   }

   @Override
   public Element getXMLObject(Document parentDoc, String newLine) {
      Element mainElement = super.getXMLObject(parentDoc, newLine);
      mainElement.setAttribute("preparedState", String.valueOf(_preparedState));
      mainElement.setAttribute("attackedThisRound", String.valueOf(_attackedThisRound));
      mainElement.setAttribute("defenseStyle", String.valueOf(_defenseStyle));
      return mainElement;
   }
   @Override
   public boolean serializeFromXmlObject(Node element)
   {
      if (!super.serializeFromXmlObject(element)) {
         return false;
      }
      NamedNodeMap attributes = element.getAttributes();
      if (attributes == null) {
         return false;
      }
      _preparedState     = Byte.parseByte(attributes.getNamedItem("preparedState").getNodeValue());
      _attackedThisRound = Boolean.parseBoolean(attributes.getNamedItem("attackedThisRound").getNodeValue());
      _defenseStyle      = Byte.parseByte(attributes.getNamedItem("defenseStyle").getNodeValue());
      return true;
   }
}
