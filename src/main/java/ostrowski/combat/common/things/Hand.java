package ostrowski.combat.common.things;

import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.*;
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Hand extends Limb {
   public Thing   heldThing          = null;
   public byte    preparedState     = 0;
   public boolean attackedThisRound = false;
   private byte   defenseStyle = 0;

   public Hand() {}
   public Hand(LimbType id, Race racialBase) {
      super(id, racialBase);
   }
   @Override
   public String getHeldThingName() {
      if (heldThing == null) {
         return "";
      }
      return heldThing.getName();
   }
   @Override
   public Thing getHeldThing() { return heldThing;}
   /**
    * returns true if we are able to set the current held thing.
    */
   @Override
   public boolean setHeldThing(Thing thing, Character self) {
      if (thing != null) {
//         if (thing.getRacialBase() == null) {
//            //DebugBreak.debugBreak(thing.getName() + "Thing has no racial base.");
//         }
//         else if (!self.getRace().getName().equals(thing.getRacialBase().getName())) {
//           // Rules.debugBreak();
//         }
         if (thing.getName().equals(Weapon.NAME_KarateKick)) {
            DebugBreak.debugBreak();
         }
         if ((heldThing != null) && (heldThing.isReal())) {
            return false;
         }
         if (isCrippled()) {
            return false;
         }
      }
      else {
         // Are we going from nothing to nothing?
         if (heldThing == null) {
            return true;
         }
      }
      heldThing = thing;
      setActionsNeededToReady((byte) 0);
      setAttackStyle((byte) 0);
      defenseStyle = 0;
      if (heldThing instanceof Weapon) {
         Weapon weap = (Weapon) heldThing;
         if (weap.isMissileWeapon()) {
            WeaponStyleAttackRanged style = (WeaponStyleAttackRanged) weap.getAttackStyle(0);
            preparedState = style.getNumberOfPreparationSteps();
         }
         else {
            WeaponStyleAttackThrown style = weap.getThrownAttackStyle();
            if (style != null) {
               preparedState = style.getNumberOfPreparationSteps();
            }
            else {
               preparedState = 0;
            }
         }
         if (weap.isOnlyTwoHanded()) {
            Hand otherHand = (Hand) self.getLimb(limbType.getPairedType());
            if (otherHand.heldThing != null) {
               setActionsNeededToReady((byte) 1);
            }
         }
      }
      Hand otherHand = (Hand) self.getLimb(limbType.getPairedType());
      if ((otherHand.heldThing instanceof Weapon)) {
         Weapon otherHandWeapon = (Weapon) otherHand.heldThing;
         if (otherHandWeapon.isOnlyTwoHanded()) {
            otherHand.setActionsNeededToReady((byte) 1);
         }
      }

      return true;
   }

   public byte getPreparedState() { return preparedState; }
   public void setPreparedState(byte state) { preparedState = state; }
   public void prepareState(byte actions) {
      if ((heldThing != null) && heldThing instanceof Weapon) {
         preparedState -= actions;
         if (preparedState < 0) {
            preparedState = 0;
         }
      }
   }
   public String getWeaponPrepareState() {
      if ((heldThing != null) && heldThing instanceof Weapon) {
         Weapon weap = (Weapon) heldThing;
         WeaponStyleAttackRanged rangedStyle;
         if (heldThing instanceof MissileWeapon) {
            rangedStyle = (WeaponStyleAttackRanged)weap.getAttackStyle(0);
         }
         else {
            rangedStyle = weap.getThrownAttackStyle();
         }
         if (rangedStyle != null) {
            return rangedStyle.getPreparationStepName(weap.getName(), preparedState);
         }
      }
      return "";
   }
   public boolean canPrepare() {
      if (heldThing == null) {
         return false;
      }
      if (heldThing instanceof Weapon) {
         if (((Weapon) heldThing).getRangedStyle() == null) {
            return false;
         }
      }
      return ((preparedState != 0) && (getActionsNeededToReady() == 0));
   }

   public void setParryStyle(byte style) {
      defenseStyle = style;
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
      if (heldThing == null) {
         if (self != null) {
            if (self.getRace().hasProperty(Race.PROPERTIES_CLAWS)) {
               return Weapons.getWeapon(Weapon.NAME_Claws, self.getRace());
            }
            if (self.hasAdvantage(Race.PROPERTIES_0_ARMS)) {
               //Rules.debugBreak();
               return null;
            }

            Hand otherHand = (Hand) self.getLimb(limbType.getPairedType());
            if ((otherHand != null) && (otherHand.heldThing != null)) {
               if (otherHand.heldThing instanceof Weapon) {
                  Weapon otherHandWeapon = (Weapon) otherHand.heldThing;
                  if (otherHandWeapon.isOnlyTwoHanded()) {
                     return null;
                  }
               }
            }
            return Weapons.getWeapon(Weapon.NAME_Punch, self.getRace());
         }
      }
      if (heldThing instanceof Weapon) {
         return (Weapon) heldThing;
      }
      return null;
   }

   @Override
   public Hand clone()
   {
      Hand newHand = new Hand(limbType, getRacialBase());
      this.copyDataInto(newHand);
      return newHand;
   }
   @Override
   public void copyDataInto(Limb dest)
   {
      super.copyDataInto(dest);
      if (dest instanceof Hand) {
         Hand hand = (Hand) dest;
         hand.heldThing = (heldThing == null) ? null : heldThing.clone();
         hand.attackedThisRound = attackedThisRound;
         hand.preparedState = preparedState;
         hand.defenseStyle = defenseStyle;
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         super.serializeFromStream(in);
         String thingName = readString(in);
         if (thingName.length() == 0) {
            heldThing = null;
         }
         else {
            heldThing = Thing.getThing(thingName, getRacialBase());
            if (heldThing != null) {
               heldThing.serializeFromStream(in);
            }
         }
         attackedThisRound = readBoolean(in);
         preparedState = readByte(in);
         defenseStyle = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         super.serializeToStream(out);
         writeToStream(((heldThing == null) ? "" : heldThing.getName()), out);
         if (heldThing != null) {
            heldThing.serializeToStream(out);
         }
         writeToStream(attackedThisRound, out);
         writeToStream(preparedState, out);
         writeToStream(defenseStyle, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public byte getAttackTime(byte attrStr, Character attacker) {
      Weapon weap = getWeapon(attacker);
      if (weap != null) {
         if (weap.attackStyles.length > getAttackStyle()) {
            return weap.attackStyles[getAttackStyle()].getSpeed(attrStr);
         }
      }
      return 0;
   }
   @Override
   public byte getDefenseTime(byte attrStr, Character attacker) {
      Weapon weap = getWeapon(attacker);
      if (weap != null) {
         if ((defenseStyle >= 0) && (weap.parryStyles.length > defenseStyle))
          {
            return weap.parryStyles[defenseStyle].getSpeed(attrStr);
         // If we are holding a shield, the 'parry' (== block) time will be zero
         }
      }
      return 0;
   }
   @Override
   public String getDefenseName(boolean tensePast, Character defender) {
      if (heldThing == null) {
         Weapon weap = getWeapon(defender);
         if (weap == null) {
            return null;
         }
         return weap.getDefenseName(tensePast, defender);
      }
      return heldThing.getDefenseName(tensePast, defender);
   }
   @Override
   public void applyAction(RequestAction action, byte attrStr, Character actor, Arena arena) throws BattleTerminatedException
   {
      if (limbType == action.getLimb()) {
         if (action.isPrepareRanged()) {
            prepareState((byte)1/*actions*/);
         }
         else if (action.isAttack()) {
            setAttackStyle((byte) action.styleRequest.getAnswerIndex());
            resetMissileWeapon();
            setActionsNeededToReady((byte) (getActionsNeededToReady() + getAttackTime(attrStr, actor)));
            attackedThisRound = true;
         }
         else if (action.isReadyWeapon()) {
            setActionsNeededToReady((byte) (getActionsNeededToReady() - action.getActionsUsed()));
         }
         else if (action.isApplyItem()) {
            if (heldThing.apply(actor, arena)) {
               heldThing = null;
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
      if (heldThing != null) {
         if (heldThing instanceof Weapon) {
            Weapon weap = (Weapon) heldThing;
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
      Thing thingDropped = heldThing;
      heldThing = null;
      return thingDropped;
   }
   @Override
   public boolean isEmpty()
   {
      if (heldThing == null) {
         return true;
      }
      return !heldThing.isReal();
   }
   @Override
   public boolean canDefendAgainstRangedWeapons() {
      if (heldThing != null) {
         return heldThing.canDefendAgainstRangedWeapons();
      }
      return false;
   }
   @Override
   public boolean canDefend(Character defender, boolean attackIsRanged, short distance, boolean attackIsCharge, boolean attackIsGrapple, DamageType damageType, boolean checkState) {
      if (isCrippled()) {
         return false;
      }
      if (checkState) {
         if (getActionsNeededToReady() != 0) {
            return false;
         }
         if (attackedThisRound) {
            return false;
         }
      }
      if (heldThing == null) {
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

         Limb pairedLimb = defender.getLimb(limbType.getPairedType());
         if ((pairedLimb != null) && (!pairedLimb.isCrippled())) {
            Weapon pairedHandsWeapon = pairedLimb.getWeapon(defender);
            if (pairedHandsWeapon != null) {
               int defStyle = ((Hand)pairedLimb).defenseStyle;
               if ((pairedHandsWeapon.parryStyles.length > defStyle) && (defStyle > 0)) {
                  WeaponStyleParry otherParry = pairedHandsWeapon.parryStyles[defStyle];
                  if (otherParry.isTwoHanded()) {
                     return false;
                  }
               }
            }
         }
         Weapon punch = Weapons.getWeapon(Weapon.NAME_Punch, defender.getRace());
         if (punch != null) {
            WeaponStyleParry[] parrySkills = punch.getParryStyles();
            byte bestLevel = -1;
            byte bestIndex = -1;
            for (byte i = 0; i < parrySkills.length; i++) {
               WeaponStyleParry parrySkill = parrySkills[i];
               if (parrySkill.canDefendAgainstDamageType(damageType, attackIsGrapple, distance)) {
                  SkillType styleType = parrySkill.getSkillType();
                  byte skillLevel = defender.getSkillLevel(styleType, limbType, false, true, true);
                  if (skillLevel >= parrySkill.getMinSkill()) {
                     byte skillEffectiveLevel = (byte) (skillLevel * parrySkill.getEffectiveness());
                     if ((bestIndex == -1) || (skillEffectiveLevel > bestLevel)) {
                        bestLevel = skillEffectiveLevel;
                        bestIndex = i;
                     }
                  }
               }
            }
            if (bestIndex != -1) {
               defenseStyle = bestIndex;
               return true;
            }
         }
         return false;
      }
      if (heldThing instanceof Weapon) {
         if (attackIsRanged || attackIsCharge) {
            return false;
         }
         Weapon weap = (Weapon) heldThing;
         return (weap.parryStyles.length > 0);
      }
      return heldThing instanceof Shield;
      // not a shield or weapon - can't defend!
   }
   @Override
   public boolean canAttack(Character character) {
      if (super.canAttack(character)) {
         if (attackedThisRound) {
            return false;
         }
         Weapon weap = getWeapon(character);
         // Missile weapons must be prepared before they can be used to attack.
         if (weap instanceof MissileWeapon) {
            return (preparedState == 0);
         }
         return true;
      }
      return false;
   }
   public boolean canCounterAttack(Character defender, boolean grab) {
      if (super.canAttack(defender)) {
         if (attackedThisRound) {
            return false;
         }
         Weapon punch = Weapons.getWeapon(Weapon.NAME_Punch, defender.getRace());
         if (punch == null) {
            return false;
         }
         for (WeaponStyleCounterAttack parrySkill : punch.counterattackStyles) {
            if (parrySkill.getName().toLowerCase().contains("grab") == grab) {
               SkillType styleType = parrySkill.getSkillType();
               byte skill = defender.getSkillLevel(styleType, limbType, false, true, true);
               if (skill >= parrySkill.getMinSkill()) {
                  return true;
               }
            }
         }
      }
      return false;
   }
   @Override
   public void endRound()
   {
      attackedThisRound = false;
   }
   public byte getDefenseTN(Character self, boolean rangedAttack, short distance, boolean isChargeAttack,
                            boolean isGrappleAttack, DamageType damageType) {
      // If this hand is crippled, return 0
      if (isCrippled()) {
         return 0;
      }

      byte defLevel = getDefenseTNWithoutWounds(self, rangedAttack, distance, isChargeAttack,
                                                isGrappleAttack, damageType, true/*checkState*/);
      defLevel -= getWoundPenalty();
      if (defLevel < 0) {
         defLevel = 0;
      }
      return defLevel;
   }
   @Override
   public byte getDefenseTNWithoutWounds(Character self, boolean rangedAttack, short distance, boolean isChargeAttack,
                                         boolean isGrappleAttack, DamageType damageType, boolean checkState) {
      if (canDefend(self, rangedAttack, distance, isChargeAttack, isGrappleAttack, damageType, checkState)) {
         Weapon weap = getWeapon(self);
         if (weap != null) {
            if ((defenseStyle < 0) || (defenseStyle >= weap.parryStyles.length)) {
               DebugBreak.debugBreak();
               return 0;
            }
            LimbType handUse = limbType;
            if (!weap.isReal()) {
               // unarmed combat doesn't get penalties for off-hand when used to parry:
               handUse = null;
            }
            byte parrySkill = self.getSkillLevel(weap.parryStyles[defenseStyle].getSkillType(), handUse, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, true/*adjustForHolds*/);
            return Rules.getParryLevel(parrySkill, weap.parryStyles[defenseStyle].getEffectiveness());
         }
         else if (heldThing instanceof Shield) {
            byte shieldSkill = self.getSkillLevel(SkillType.Shield, limbType, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, true/*adjustForHolds*/);
            return Rules.getBlockLevel(shieldSkill);
         }
      }
      return 0;
   }
   @Override
   public byte getPenaltyForMassiveDamage(Character self, byte minDamageAttacking, short distance,
                                          boolean rangedAttack, boolean isChargeAttack, boolean isGrappleAttack,
                                          DamageType damageType, boolean checkState) {
      double tnWithHand = getDefenseTNWithoutWounds(self, rangedAttack, distance, isChargeAttack, isGrappleAttack,
                                                    damageType, checkState);
      byte maxDamageDealt = -127;
      Weapon weap = getWeapon(self);
      if (weap != null) {
         byte bestOptLevel = weap.getBestDefenseOption(self, limbType, false/*canUseTwoHands*/, damageType,
                                                       isGrappleAttack, distance);
         // Aikido can only be used within 2 hexes
         if (distance < 3) {
            for (WeaponStyleParry parryStyle : weap.parryStyles) {
               if (parryStyle.getSkillType() == SkillType.Aikido) {
                  // Aikido uses each hand effectively, so don't consider hand penalties
                  byte skillLevel = (byte) (self.getSkillLevel(parryStyle.getSkillType(), null, false/*sizeAdjust*/,
                                                               false/*adjustForEncumbrance*/, true/*adjustForHolds*/)
                                            - parryStyle.getSkillPenalty());
                  byte parryLevel = Rules.getParryLevel(skillLevel, parryStyle.getEffectiveness());
                  if (bestOptLevel == parryLevel) {
                     Rules.diag("getPenaltyForMassiveDamage: self=" + self.getName() +
                                ", handID=" + limbType +
                                ", maxDamageDealt=" + maxDamageDealt +
                                ", minDamageAttacking=" + minDamageAttacking +
                                ", tnWithHand=" + tnWithHand +
                                ", penalty=0, due to Aikido skill parry");
                     return 0;
                  }
               }
            }
         }
         maxDamageDealt = getWeaponMaxDamage(self.getPhysicalDamageBase(), self);
      }
      else if (heldThing instanceof Shield) {
         // Block with shields act like a parry, with shields doing 0 damage
         maxDamageDealt = self.getPhysicalDamageBase();
         maxDamageDealt += ((Shield) heldThing).getDamage();
         // TODO: allow the option to defend at full TN, but if a near-miss or a hit
         //       occurs, damage is applied, but reduced by the shield (15 points)
      }
      if (maxDamageDealt != -127) {
         int damageDiff = minDamageAttacking - maxDamageDealt;
         if (damageDiff > 0) {
            // For every 10 points of damage, reduce the defense skill by 50%
            byte penalty = (byte) Math.round(tnWithHand - (tnWithHand / Math.pow(2, (damageDiff / 10))));
            Rules.diag("getPenaltyForMassiveDamage: self=" + self.getName() +
                       ", handID=" + limbType +
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
      if (heldThing != null) {
         return "parry".equalsIgnoreCase(heldThing.getActiveDefenseName());
      }
      return (character.getSkillLevel(SkillType.Aikido, null, false/*sizeAdjust*/, false/*adjustForEncumbrance*/, false/*adjustForHolds*/) != 0) ||
             (character.getSkillLevel(SkillType.Karate, null, false/*sizeAdjust*/, false/*adjustForEncumbrance*/, false/*adjustForHolds*/) != 0) ||
             (character.getSkillLevel(SkillType.Boxing, null, false/*sizeAdjust*/, false/*adjustForEncumbrance*/, false/*adjustForHolds*/) != 0) ||
             (character.getSkillLevel(SkillType.Brawling, null, false/*sizeAdjust*/, false/*adjustForEncumbrance*/, false/*adjustForHolds*/) != 0) ||
             (character.getSkillLevel(SkillType.Wrestling, null, false/*sizeAdjust*/, false/*adjustForEncumbrance*/, false/*adjustForHolds*/) != 0);
//      Weapon punch = Weapons.getWeapon("punch");
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
      if (heldThing == null) {
         return false;
      }
      Hand otherHand = (Hand) self.getLimb(limbType.getPairedType());
      if (otherHand.heldThing == null) {
         return super.canBeReadied(self);
      }

      if (heldThing instanceof Weapon) {
         Weapon weap = (Weapon) heldThing;
         if (weap.isOnlyTwoHanded()) {
            if (otherHand.heldThing != null) {
               return false;
            }
         }
      }
      if (otherHand.heldThing instanceof Weapon) {
         Weapon otherHandWeapon = (Weapon) otherHand.heldThing;
         if (otherHandWeapon.isOnlyTwoHanded()) {
            return false;
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
      obj.addPoint((wideDiameter * 16)/32, -((narrowDiameter * 16)/32));  // outer elbow
      obj.addPoint((wideDiameter *  5)/32, -((narrowDiameter * 25)/32));  // outer wrist
      obj.addPoint((wideDiameter *  4)/32, -((narrowDiameter * 22)/32));  // wrist
      obj.addPoint((wideDiameter * 13)/32, -((narrowDiameter * 13)/32));  // elbow
      obj.addPoint((wideDiameter * 14)/32, 0);                         // shoulder
      obj.addPoint((wideDiameter * 16)/32, (narrowDiameter * 3)/32);     // shoulder
      return obj;
   }

   @Override
   public Element getXMLObject(Document parentDoc, String newLine) {
      Element mainElement = super.getXMLObject(parentDoc, newLine);
      mainElement.setAttribute("preparedState", String.valueOf(preparedState));
      mainElement.setAttribute("attackedThisRound", String.valueOf(attackedThisRound));
      mainElement.setAttribute("defenseStyle", String.valueOf(defenseStyle));
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
      preparedState = Byte.parseByte(attributes.getNamedItem("preparedState").getNodeValue());
      attackedThisRound = Boolean.parseBoolean(attributes.getNamedItem("attackedThisRound").getNodeValue());
      defenseStyle = Byte.parseByte(attributes.getNamedItem("defenseStyle").getNodeValue());
      return true;
   }
}
