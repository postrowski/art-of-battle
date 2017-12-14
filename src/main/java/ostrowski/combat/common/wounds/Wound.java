package ostrowski.combat.common.wounds;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.SpecialDamage;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.things.LimbType;
import ostrowski.protocol.SerializableObject;

/*
 * Created on May 4, 2006
 */
public class Wound extends SerializableObject implements Enums
{
   byte           _damage                = 0;
   DamageType     _damageType            = DamageType.BLUNT;
   long           _effectsMask           = EFFECT_NONE;
   Location       _locationBase          = Location.MISSED;
   Location       _location              = Location.MISSED;
   Side           _locationSide          = Side.ANY;
   Pair           _locationPair          = Pair.ANY;
   String         _description           = "none";
   private String _invalidReason         = null;

   SpecialDamage  _specialDamageModifier = new SpecialDamage(0);

   public void setSpecialDamageModifier(SpecialDamage specialDamageModifier) {
      _specialDamageModifier = specialDamageModifier;
      _wounds += specialDamageModifier.getWoundModifier();
      _painLevel += specialDamageModifier.getPainModifier();
   }

   public SpecialDamage getSpecialDamageModifier() {
      return _specialDamageModifier;
   }

   // pain & bleeding
   byte _painLevel       = 0;
   byte _wounds          = 0;
   byte _bleedRate       = 0;
   byte _healedWounds    = 0;
   byte _healedBleedRate = 0;

   // penalties are positive integers, so a 2 means a -2 to action.
   // A negative value means no use possible, such as a severed limb.
   byte _penaltyArm      = 0;
   byte _penaltyMove     = 0;
   byte _knockedBackDist = 0;

   public Wound(byte damage, Location location, String description, int painLevel, int wounds, int bleedRate, int armPenalty, int movePenalty,
                int knockedDownDist, DamageType damageType, long effectsMask, Character target) {
      _damage = damage;
      _description = description;
      _effectsMask = effectsMask;
      _painLevel = (byte) painLevel;
      _wounds = (byte) wounds;
      _bleedRate = (byte) bleedRate;
      _knockedBackDist = (byte) knockedDownDist;
      _damageType = damageType;
      _locationBase = location;
      _location = location;

      if (target != null) {
         // resolve any generalities.
         // If this fails because the target doesn't have the location specified (ie, an 'arm' hit on a giant snake),
         // then the results will be placed in the '_invalidReason' String, indicating that this Wound is invalid.
         setInvalidReason(target.placeWound(this));
      }
      if ((_location == Location.ARM) || (_location == Location.LIMB) || (_location == Location.BODY)) {
         _penaltyArm = (byte) armPenalty;
      }
      if ((_location == Location.LEG) || (_location == Location.LIMB) || (_location == Location.BODY)) {
         _penaltyMove = (byte) movePenalty;
      }
   }

   public Wound() {
      // used only for serialization
   }

   public byte getLevel() {
      return _damage;
   }

   public DamageType getDamageType() {
      return _damageType;
   }

   public String getLocationName() {
      return getLocationName(_location, _locationSide, _locationPair);
   }

   public String getBaseLocationName() {
      return getLocationName(_locationBase, Side.ANY, Pair.ANY);
   }

   public String getLocationName(Location location, Side side, Pair pair) {
      if ((location == Location.ARM) || (location == Location.WING) || (location == Location.LEG) || (location == Location.EYE)) {
         StringBuilder sb = new StringBuilder();
         // determine pair
         //             if (pair == Pair.FIRST)  sb.append("first ");
         switch (pair) {
            case SECOND: sb.append("second "); break;
            case THIRD:  sb.append("third ");  break;
            case ANY:    sb.append("any ");    break;
            case ALL:    sb.append("every ");  break;
         }

         // determine side
         switch (side) {
            case LEFT:   sb.append("left "); break;
            case RIGHT:  sb.append("right "); break;
            case ANY:
               // don't print "any any "
               if (pair != Pair.ANY) {
                  sb.append("any ");
               }
               break;
            case BOTH:
               sb.append("both ");
         }

         // determine limb
         switch (location) {
            case ARM:  sb.append("arm");  break;
            case WING: sb.append("wing"); break;
            case LEG:  sb.append("leg");  break;
            case EYE:  sb.append("eye");  break;
         }
         if (side == Side.BOTH) {
            sb.append("s");
         }

         return sb.toString();
      }
      switch (location) {
         case BODY:    return "body";
         case HEAD:    return "head";
         case NECK:    return "neck";
         case WEAPON:  return "weapon";
         case LIMB:    return "random limb";
         case MISSED:  return "nothing";
      }
      return null;
   }

   public void setDamageType(DamageType damageType) {
      _damageType = damageType;
   }

   public void setLocation(Location location) {
      _location = location;
   }

   public void setLocationSide(Side side) {
      _locationSide = side;
   }

   public void setLocationPair(Pair pair) {
      _locationPair = pair;
   }

   public boolean isKnockDown() {
      return ((_effectsMask & (EFFECT_KNOCKDOWN | EFFECT_KNOCKOUT | EFFECT_COMA | EFFECT_DEATH)) != 0);
   }

   public boolean isKnockOut() {
      return ((_effectsMask & (EFFECT_KNOCKOUT | EFFECT_COMA | EFFECT_DEATH)) != 0);
   }

   public boolean isFatal() {
      return ((_effectsMask & EFFECT_DEATH) != 0);
   }

   public boolean isUnreadyWeapon() {
      return ((_effectsMask & EFFECT_WEAPON_UNREADY) != 0);
   }

   public boolean isDropWeapon() {
      return ((_effectsMask & EFFECT_WEAPON_DROPPED) != 0);
   }

   public boolean isSeveredArm() {
      return (_penaltyArm == -2) && (_location == Location.ARM);
   }

   public boolean isSeveredWing() {
      return (_penaltyArm == -2) && (_location == Location.WING);
   }

   public boolean isSeveredLeg() {
      return (_penaltyMove == -2);
   }

   public byte getPain() {
      return _painLevel;
   }

   public String getDescription() {
      return _description;
   }

   public long getEffectsMask() {
      return _effectsMask;
   }

   public void setEffectsMask(long effectsMask) {
      _effectsMask = effectsMask;
   }

   public byte getWounds() {
      return _wounds;
   }

   public byte getEffectiveWounds() {
      return (byte) (_wounds - _healedWounds);
   }

   public byte getBleedRate() {
      return _bleedRate;
   }

   public byte getEffectiveBleedRate() {
      return (byte) (_bleedRate - _healedBleedRate);
   }

   public boolean regrowLimb() {
      boolean limbRegrown = false;
      if (_penaltyArm == -2) {
         _penaltyArm = 0;
         limbRegrown = true;
      }
      if (_penaltyMove == -2) {
         _penaltyMove = 0;
         limbRegrown = true;
      }
      if (limbRegrown) {
         _healedWounds = _wounds;
         _healedBleedRate = _bleedRate;
      }
      return limbRegrown;
   }

   public boolean healWound(byte woundReduction, byte bleedingReduction) {
      if (_healedBleedRate < bleedingReduction) {
         _healedBleedRate = bleedingReduction;
         if (_healedBleedRate > _bleedRate) {
            _healedBleedRate = _bleedRate;
         }
      }
      if (_healedWounds < woundReduction) {
         _healedWounds = woundReduction;
         if (_healedWounds > _wounds) {
            _healedWounds = _wounds;
         }
      }
      if (((_wounds - _healedWounds) == 0) && ((_bleedRate - _healedBleedRate) == 0)) {
         if ((_penaltyArm == -2) || (_penaltyMove == -2) ) {
            // Severed limbs are not re-grown by a cureWound spell.
         }
         else {
            // If all the wounds and bleeding have been stopped,
            // then the would is cured, so don't let any of the effect (crippling, etc.) continue to affect
            _effectsMask = 0;
            _penaltyArm = 0;
            _penaltyMove = 0;
            // return true if there are no remaining effects (wounds, bleeding, or penalties)
            return true;
         }
      }
      // return false if there are still effects (wounds, bleeding, or penalties)
      return false;
   }

   public Location getLocation() {
      return _location;
   }

   public Side getLocationSide() {
      return _locationSide;
   }

   public Pair getLocationPair() {
      return _locationPair;
   }

   public byte getPenaltyArm() {
      if (_penaltyArm <= 0) {
         return _penaltyArm;
      }
      return (byte) Math.max((_penaltyArm - _healedWounds), 0);
   }

   public byte getPenaltyMove() {
      if (_penaltyMove <= 0) {
         return _penaltyMove;
      }
      return (byte) Math.max((_penaltyMove - _healedWounds), 0);
   }

   public byte getKnockedBackDist() {
      return _knockedBackDist;
   }

   public String describeWound() {
      StringBuilder sb = new StringBuilder();
      sb.append(_damage).append(" points of ").append(_damageType.fullname).append(" damage, resulting in ");
      sb.append(_description).append(" to the ").append(getLocationName());
      String effects = describeEffects();
      if (effects.length() > 0) {
         sb.append(" with the following effect(s): ").append(effects);
      }

      if (_painLevel > 0) {
         sb.append(", ").append(_painLevel).append(" points of pain");
      }
      if (_wounds > 0) {
         sb.append(", ").append(_wounds).append(" wounds");
      }
      if (_bleedRate > 0) {
         sb.append(", bleeding at ").append(_bleedRate);
      }
      if (_penaltyArm != 0) {
         sb.append(", ");
         switch (_locationPair) {
            case SECOND: sb.append("second "); break;
            case THIRD:  sb.append("third "); break;
         }
         switch (_locationSide) {
            case LEFT:  sb.append("left "); break;
            case RIGHT: sb.append("right "); break;
         }
         if (_penaltyArm > 0) {
            sb.append("arm penalty of ").append(_penaltyArm);
         }
         else if (_penaltyArm == -1) {
            sb.append("arm crippled");
         }
         else if (_penaltyArm == -2) {
            sb.append("arm severed");
         }
         else {
            DebugBreak.debugBreak("Don't know how to describe arm penalty of " + _penaltyArm);
         }
      }
      if (_penaltyMove > 0) {
         sb.append(", move and retreat penalty of ").append(_penaltyMove);
         if (_penaltyMove == -1) {
            sb.append(", unable to stand ");
         }
         else if (_penaltyMove == -2) {
            sb.append(", unable to stand (leg severed) ");
         }
      }
      if (_knockedBackDist > 0) {
         sb.append(", knocking target back ").append(_knockedBackDist).append(" hexes");
      }
      return sb.toString();
   }

   public String describeEffects() {
      ArrayList<String> effects = new ArrayList<>();
      if ((_effectsMask & EFFECT_DEATH) != 0) {
         effects.add("death");
      }
      if ((_effectsMask & EFFECT_COMA) != 0) {
         effects.add("coma");
      }
      if ((_effectsMask & EFFECT_DECAPITATE) != 0) {
         effects.add("decapitated");
      }
      if ((_effectsMask & EFFECT_BRAIN_DESTROY) != 0) {
         effects.add("brain destroyed");
      }
      if ((_effectsMask & EFFECT_BRAIN_DAMAGE) != 0) {
         effects.add("brain damaged");
      }
      if ((_effectsMask & EFFECT_HEART) != 0) {
         effects.add("heart hit");
      }
      if ((_effectsMask & EFFECT_ORGAN_DAM) != 0) {
         effects.add("organ damage");
      }
      if ((_effectsMask & EFFECT_LUNG_DAM) != 0) {
         effects.add("lung damaged");
      }
      if ((_effectsMask & EFFECT_VENA_CAVA) != 0) {
         effects.add("vena cava ruptured");
      }
      if ((_effectsMask & EFFECT_KNOCKOUT) != 0) {
         effects.add("knocked unconscious");
      }
      if ((_effectsMask & EFFECT_PARAPLEGIC) != 0) {
         effects.add("paraplegic");
      }
      if ((_effectsMask & EFFECT_QUADRIPLEGIC) != 0) {
         effects.add("quadriplegic");
      }
      if ((_effectsMask & EFFECT_BLINDED_1) != 0) {
         effects.add("blinded in one eye");
      }
      if ((_effectsMask & EFFECT_KNOCKDOWN) != 0) {
         effects.add("knocked down");
      }
      if ((_effectsMask & EFFECT_WEAPON_UNREADY) != 0) {
         effects.add("weapon unreadied");
      }
      if ((_effectsMask & EFFECT_WEAPON_DROPPED) != 0) {
         effects.add("weapon dropped");
      }
      if ((_effectsMask & EFFECT_CONCUSSION) != 0) {
         effects.add("concussion");
      }
      if ((_effectsMask & EFFECT_OFF_BALANCE_1) != 0) {
         effects.add("off-balance (1-action)");
      }
      if ((_effectsMask & EFFECT_OFF_BALANCE_2) != 0) {
         effects.add("off-balance (2-actions)");
      }
      if ((_effectsMask & EFFECT_OFF_BALANCE_3) != 0) {
         effects.add("off-balance (3-actions)");
      }
      if ((_effectsMask & EFFECT_WEAPON_KNOCKED_AWAY) != 0) {
         effects.add("weapon knocked away");
      }

      StringBuilder sb = new StringBuilder();
      if (effects.size() > 0) {
         sb.append(effects.remove(0));
         while (effects.size() > 0) {
            if (effects.size() == 1) {
               // General hits on limbs should show "weapon dropped OR knocked down", depending on which limb is hit.
               if (_location == Location.LIMB) {
                  sb.append(" OR ");
               }
               else {
                  sb.append(" and ");
               }
            }
            else {
               sb.append(", ");
            }
            sb.append(effects.remove(0));
         }
      }
      return sb.toString();
   }

   public LimbType getLimb() {
      return LimbType.get(_location, _locationSide, _locationPair);
   }

   public byte getPenaltyLimb() {
      if (_location == Location.ARM) {
         return _penaltyArm;
      }
      if (_location == Location.LEG) {
         return _penaltyMove;
      }
      return 0;
   }

   public boolean isBlinding() {
      return (_location == Location.EYE) || ((_effectsMask & EFFECT_BLINDED_1) != 0);
   }

   public boolean isDecapitating() {
      return ((_effectsMask & EFFECT_DECAPITATE) != 0);
   }

   public String describeArmPenalty() {
      return describePenalty(getPenaltyArm());
   }

   public String describeMovePenalty() {
      return describePenalty(getPenaltyMove());
   }

   public String describePenalty(byte value) {
      if (_damageType == DamageType.GENERAL) {
         if ((getPenaltyArm() == -2) || (getPenaltyMove() == -2)) {
            return "severed?";
         }
         if ((getPenaltyArm() == -1) || (getPenaltyMove() == -1)) {
            return "crippled?";
         }
      }
      switch (value) {
         case 0:
            return "-";
         case -1:
            return "crippled";
         case -2:
            return "severed";
      }
      return String.valueOf(value);
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         _damage = readByte(in);
         _damageType = DamageType.getByValue(readByte(in));
         _effectsMask = readLong(in);
         _locationBase = Location.values()[readInt(in)];
         _location = Location.values()[readInt(in)];
         _locationSide = Side.values()[readInt(in)];
         _locationPair = Pair.values()[readInt(in)];
         _description = readString(in);
         _specialDamageModifier = new SpecialDamage(readInt(in));
         _painLevel = readByte(in);
         _wounds = readByte(in);
         _bleedRate = readByte(in);
         _healedWounds = readByte(in);
         _healedBleedRate = readByte(in);
         _penaltyArm = readByte(in);
         _penaltyMove = readByte(in);
         _knockedBackDist = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         writeToStream(_damage, out);
         writeToStream(_damageType.value, out);
         writeToStream(_effectsMask, out);
         writeToStream(_locationBase.ordinal(), out);
         writeToStream(_location.ordinal(), out);
         writeToStream(_locationSide.ordinal(), out);
         writeToStream(_locationPair.ordinal(), out);
         writeToStream(_description, out);
         writeToStream(_specialDamageModifier.getBits(), out);
         writeToStream(_painLevel, out);
         writeToStream(_wounds, out);
         writeToStream(_bleedRate, out);
         writeToStream(_healedWounds, out);
         writeToStream(_healedBleedRate, out);
         writeToStream(_penaltyArm, out);
         writeToStream(_penaltyMove, out);
         writeToStream(_knockedBackDist, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public Element getXMLObject(Document parentDoc, String newLine) {
      Element mainElement = parentDoc.createElement("Wound");
      mainElement.setAttribute("damage", String.valueOf(_damage));
      mainElement.setAttribute("damageType", String.valueOf(_damageType.value));
      mainElement.setAttribute("effectsMask", String.valueOf(_effectsMask));
      mainElement.setAttribute("locationBase", String.valueOf(_locationBase));
      mainElement.setAttribute("location", String.valueOf(_location));
      mainElement.setAttribute("locationSide", String.valueOf(_locationSide));
      mainElement.setAttribute("locationPair", String.valueOf(_locationPair));
      mainElement.setAttribute("description", String.valueOf(_description));
      mainElement.setAttribute("specialDamageModifier", String.valueOf(_specialDamageModifier.getBits()));
      mainElement.setAttribute("painLevel", String.valueOf(_painLevel));
      mainElement.setAttribute("wounds", String.valueOf(_wounds));
      mainElement.setAttribute("bleedRate", String.valueOf(_bleedRate));
      mainElement.setAttribute("healedWounds", String.valueOf(_healedWounds));
      mainElement.setAttribute("healedBleedRate", String.valueOf(_healedBleedRate));
      mainElement.setAttribute("penaltyArm", String.valueOf(_penaltyArm));
      mainElement.setAttribute("penaltyMove", String.valueOf(_penaltyMove));
      mainElement.setAttribute("knockedBackDist", String.valueOf(_knockedBackDist));
      return mainElement;
   }

   public boolean serializeFromXmlObject(Node element) {
      if (!element.getNodeName().equals("Wound")) {
         return false;
      }
      NamedNodeMap attributes = element.getAttributes();
      if (attributes == null) {
         return false;
      }
      _damage = Byte.parseByte(attributes.getNamedItem("damage").getNodeValue());
      _damageType = DamageType.getByValue(Byte.parseByte(attributes.getNamedItem("damageType").getNodeValue()));
      _effectsMask = Long.parseLong(attributes.getNamedItem("effectsMask").getNodeValue());
      _locationBase = Location.valueOf(attributes.getNamedItem("locationBase").getNodeValue());
      _location = Location.valueOf(attributes.getNamedItem("location").getNodeValue());
      _locationSide = Side.valueOf(attributes.getNamedItem("locationSide").getNodeValue());
      _locationPair = Pair.valueOf(attributes.getNamedItem("locationPair").getNodeValue());
      _description = attributes.getNamedItem("description").getNodeValue();
      _specialDamageModifier.setBits(Integer.parseInt(attributes.getNamedItem("specialDamageModifier").getNodeValue()));
      _painLevel = Byte.parseByte(attributes.getNamedItem("painLevel").getNodeValue());
      _wounds = Byte.parseByte(attributes.getNamedItem("wounds").getNodeValue());
      _bleedRate = Byte.parseByte(attributes.getNamedItem("bleedRate").getNodeValue());
      _healedWounds = Byte.parseByte(attributes.getNamedItem("healedWounds").getNodeValue());
      _healedBleedRate = Byte.parseByte(attributes.getNamedItem("healedBleedRate").getNodeValue());
      _penaltyArm = Byte.parseByte(attributes.getNamedItem("penaltyArm").getNodeValue());
      _penaltyMove = Byte.parseByte(attributes.getNamedItem("penaltyMove").getNodeValue());
      _knockedBackDist = Byte.parseByte(attributes.getNamedItem("knockedBackDist").getNodeValue());
      return true;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Wound: ").append(_damage);
      sb.append("-point ").append(_damageType.name());
      sb.append(" ").append(getWounds());
      if (_healedWounds > 0) {
         sb.append("(").append(getEffectiveWounds()).append(")");
      }
      sb.append(" wounds, ");

      sb.append(getBleedRate());
      if (_healedBleedRate > 0) {
         sb.append("(").append(getEffectiveBleedRate()).append(")");
      }
      sb.append(" bleed, ");

      if (_penaltyArm > 0) {
         sb.append(" arm penaly:").append(_penaltyArm);
      }
      if (_penaltyMove > 0) {
         sb.append(" move/retreat penaly:").append(_penaltyMove);
      }
      return sb.toString();
   }

   public boolean isValid() {
      return (getInvalidReason() == null);
   }

   public boolean isCrippling() {
      return (getPenaltyLimb() < 0);
   }


   public void setInvalidReason(String _invalidReason) {
      this._invalidReason = _invalidReason;
   }
   public String getInvalidReason() {
      return _invalidReason;
   }
}
