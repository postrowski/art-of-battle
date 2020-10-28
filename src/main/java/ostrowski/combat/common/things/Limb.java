/*
 * Created on Oct 5, 2006
 *
 */
package ostrowski.combat.common.things;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.DrawnObject;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DefenseOption;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestDefense;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.BattleTerminatedException;
import ostrowski.protocol.SerializableObject;

public abstract class Limb extends Thing implements Cloneable {
   public  LimbType   _limbType          = null;

    // penalties are positive integers so a 2 means a -2 to actions.
    // A negative value means no use possible, such as a severed limb.
    private final List<Wound> _wounds = new ArrayList<>();
    private byte  _attackStyle  = 0;
    private byte  _actionsNeededToReady   = 0;

    public Limb() {
       super(null/*name*/, null/*racialBase*/, 0/*cost*/, 10/*weight*/, (byte)0/*passiveDefense*/);
    }
    public Limb(LimbType type, Race racialBase) {
       super(type.name, racialBase, 0/*cost*/, 10/*weight*/, (byte)0/*passiveDefense*/);
       _limbType = type;
       if (type.isHand() != (this instanceof Hand)) { DebugBreak.debugBreak(); }
       if (type.isHead() != (this instanceof Head)) { DebugBreak.debugBreak(); }
       if (type.isLeg()  != (this instanceof Leg )) { DebugBreak.debugBreak(); }
       if (type.isTail() != (this instanceof Tail)) { DebugBreak.debugBreak(); }
       if (type.isWing() != (this instanceof Wing)) { DebugBreak.debugBreak(); }
    }
    public byte getWoundPenalty() {
       byte penalty = 0;
       for (Wound wound : _wounds) {
          // severed limbs (penalty == -2) trump all other injuries to this limb,
          // even non-severing crippling injuries (penalty == -1)
          if (wound.getPenaltyLimb() == -2) {
            return -2;
         }
          if (wound.getPenaltyLimb() == -1) {
            penalty = -1;
         }
          // If this limb is not crippled, all wounds are additive.
          if (penalty >= 0) {
            penalty += wound.getPenaltyLimb();
         }
       }
       return penalty;
    }
    public boolean isCrippled() { return (getWoundPenalty() < 0);}
    public boolean isSevered() { return (getWoundPenalty() == -2);}

    public boolean removeWound(Wound wound) {
       return _wounds.remove(wound);
    }

    public boolean applyWound(Wound wound) {
       if (_limbType == wound.getLimb()) {
          if (wound.getPenaltyLimb() != 0) {
             _wounds.add(wound);
          }
          if (wound.isUnreadyWeapon()) {
             setActionsNeededToReady((byte) (_actionsNeededToReady+1));
          }
          return true;
       }
       return false;
    }
    public byte getAttackStyle() {
       return _attackStyle;
    }
    public void setAttackStyle(byte style) {
       _attackStyle = style;
    }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      super.serializeFromStream(in);
       try {
          LimbType limbType = LimbType.getByValue(readByte(in));
          if (_limbType == null) {
             _limbType = limbType;
          }
          else if (_limbType != limbType) {
             DebugBreak.debugBreak("serialized wrong object type!");
             throw new RuntimeException("serialized wrong object type!");
          }
           String raceName = readString(in);
           setRacialBase(Race.getRace(raceName, Gender.MALE));
           _wounds.clear();
           for (SerializableObject obj : readIntoListSerializableObject(in)) {
              if (obj instanceof Wound) {
                 _wounds.add((Wound) obj);
              }
           }
           _attackStyle  = readByte(in);
           _actionsNeededToReady = readByte(in);
       } catch (IOException e) {
           e.printStackTrace();
        }
     }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      super.serializeToStream(out);
      try {
         writeToStream((byte)(_limbType.value), out);
         writeToStream(getRacialBase().getName(), out);
         writeToStream(_wounds, out);
         writeToStream(_attackStyle, out);
         writeToStream(_actionsNeededToReady, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public Element getXMLObject(Document parentDoc, String newLine) {
      Element mainElement = parentDoc.createElement("Limb");
      mainElement.setAttribute("id", String.valueOf(_limbType.value));
      mainElement.setAttribute("name", String.valueOf(getName()));
      mainElement.setAttribute("attackStyle", String.valueOf(_attackStyle));
      mainElement.setAttribute("actionsNeededToReady", String.valueOf(_actionsNeededToReady));
      return mainElement;
   }
   public boolean serializeFromXmlObject(Node element)
   {
      if (!element.getNodeName().equals("Limb")) {
         return false;
      }
      NamedNodeMap attributes = element.getAttributes();
      if (attributes == null) {
         return false;
      }
      _limbType                   = LimbType.getByValue(Byte.parseByte(attributes.getNamedItem("id").getNodeValue()));
                                             attributes.getNamedItem("name").getNodeValue();
      _attackStyle          = Byte.parseByte(attributes.getNamedItem("attackStyle").getNodeValue());
      _actionsNeededToReady = Byte.parseByte(attributes.getNamedItem("actionsNeededToReady").getNodeValue());
      return true;
   }

   public void endRound()
   {
   }
   public abstract Weapon getWeapon(Character character);
   public boolean canAttack(Character character) {
      if (isCrippled()) {
         return false;
      }
      if (getActionsNeededToReady() != 0) {
         return false;
      }
      Weapon weap = getWeapon(character);
      if (weap != null) {
         for (WeaponStyleAttack style : weap.getAttackStyles()) {
            if (style.canAttack(character, weap, this)) {
               return true;
            }
         }
         for (WeaponStyleAttack style : weap.getGrapplingStyles()) {
            if (style.canAttack(character, weap, this)) {
               return true;
            }
         }
      }
      return false;
   }
   public byte getActionsNeededToReady() { return _actionsNeededToReady; }
   public void setActionsNeededToReady(byte actions) {
      // If we aren't carrying anything, it can't become un-ready
      if (getHeldThing() != null) {
         _actionsNeededToReady = actions;
      }
   }

   public boolean canDefend(Character defender, boolean attackIsRanged, short distance, boolean attackIsCharge,
                            boolean attackIsGrapple, DamageType damageType, boolean checkState) {
      return false;
   }
   @Override
   public boolean canDefendAgainstRangedWeapons() {
      return false;
   }
   @Override
   public String getDefenseName(boolean tensePast, Character defender) {
      return null;
   }
   public byte getDefenseTNWithoutWounds(Character character, boolean isRangedAttack, short distance, boolean isChargeAttack,
                                         boolean isGrappleAttack, DamageType damageType, boolean checkState) {
      return 0;
   }
   public byte getDefenseTime(byte attribute, Character attacker) {
      return 0;
   }
   public byte getPenaltyForMassiveDamage(Character character, byte minimumDamage, short distance,
                                          boolean isRangedAttack, boolean isChargeAttack, boolean isGrappleAttack,
                                          DamageType damageType, boolean checkState) {
      return 0;
   }
   public String getHeldThingName() {
      return null;
   }
   public void copyDataInto(Limb dest) {
      dest._wounds.clear();
      dest._wounds.addAll(_wounds);
      dest._attackStyle          = _attackStyle;
      dest._actionsNeededToReady = _actionsNeededToReady;
   }
   @SuppressWarnings("unused")
   public void applyAction(RequestAction action, byte attribute, Character actor, Arena arena) throws BattleTerminatedException {
   }
   public void applyDefense(RequestDefense defense, byte attribute, Character defender) {
   }
   public Thing getHeldThing() {
      return null;
   }
   /**
    * returns true if we are able to set the current held thing.
    */
   public boolean setHeldThing(Thing thing, Character self) {
      return false;
   }
   public Thing dropThing() {
      return null;
   }
   public boolean isEmpty() {
      return true;
   }

   public DefenseOption getDefOption() {
      switch (_limbType) {
         case HAND_RIGHT:   return DefenseOption.DEF_RIGHT;
         case HAND_LEFT:    return DefenseOption.DEF_LEFT;
         case HAND_RIGHT_2: return DefenseOption.DEF_RIGHT_2;
         case HAND_LEFT_2:  return DefenseOption.DEF_LEFT_2;
         case HAND_RIGHT_3: return DefenseOption.DEF_RIGHT_3;
         case HAND_LEFT_3:  return DefenseOption.DEF_LEFT_3;
      }
      return null;
   }
   public Wound.Pair getLocationPair() {
      switch (_limbType) {
         case HAND_RIGHT:
         case HAND_LEFT:
         case LEG_RIGHT:
         case LEG_LEFT:
            return Wound.Pair.FIRST;
         case HAND_RIGHT_2:
         case HAND_LEFT_2:
         case LEG_RIGHT_2:
         case LEG_LEFT_2:
            return Wound.Pair.SECOND;
         case HAND_RIGHT_3:
         case HAND_LEFT_3:
         case LEG_RIGHT_3:
         case LEG_LEFT_3:
            return Wound.Pair.THIRD;
      }
      return Wound.Pair.ANY;
   }
   public Wound.Side getLocationSide() {
      switch (_limbType) {
         case HAND_RIGHT:
         case HAND_RIGHT_2:
         case HAND_RIGHT_3:
         case LEG_RIGHT:
         case LEG_RIGHT_2:
         case LEG_RIGHT_3:
         case WING_RIGHT:
            return Wound.Side.RIGHT;
         case HAND_LEFT:
         case HAND_LEFT_2:
         case HAND_LEFT_3:
         case LEG_LEFT:
         case LEG_LEFT_2:
         case LEG_LEFT_3:
         case WING_LEFT:
            return Wound.Side.LEFT;
      }
      return Wound.Side.ANY;
   }

   public String getSimpleName() {
      String simpleName = getName();
      int lastSpace = simpleName.lastIndexOf(' ');
      return simpleName.substring(lastSpace+1);
   }
   @Override
   public abstract Limb clone();

   @Override
   public String toString() {
      return getName();
   }
   public boolean canBeReadied(Character self) {
      return getActionsNeededToReady() > 0;
   }
   public DrawnObject drawThing(int narrowDiameter, int wideDiameter, RGB foreground, RGB background) {
      return null;
   }

   public short getWeaponMaxRange(Character character, boolean allowRanged, boolean onlyChargeTypes) {
      Weapon weap = getWeapon(character);
      if (weap != null) {
         if (allowRanged || !(weap instanceof MissileWeapon)) {
            return weap.getWeaponMaxRange(allowRanged, onlyChargeTypes, character);
         }
      }
      return -1;
   }

   public short getWeaponMinRange(Character character, boolean allowRanged, boolean onlyChargeTypes) {
      Weapon weap = getWeapon(character);
      if (weap != null) {
         return weap.getWeaponMinRange(allowRanged, onlyChargeTypes, character);
      }
      return -1;
   }

   @Override
   public String getActiveDefenseName() {
      return null;
   }
   @Override
   public byte getBestDefenseOption(Character wielder, LimbType useHand, boolean canUse2Hands, DamageType damType,
                                    boolean isGrappleAttack, short distance) {
      return 0;
   }
   @Override
   public List<SkillType> getDefenseSkillTypes() {
      return null;
   }
}