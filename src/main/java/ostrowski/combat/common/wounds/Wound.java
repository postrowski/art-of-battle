package ostrowski.combat.common.wounds;

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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
 * Created on May 4, 2006
 */
public class Wound extends SerializableObject implements Enums, Cloneable
{
   byte           damage        = 0;
   DamageType     damageType    = DamageType.BLUNT;
   long           effectsMask   = EFFECT_NONE;
   Location       locationBase  = Location.MISSED;
   Location       location      = Location.MISSED;
   Side           locationSide  = Side.ANY;
   Pair           locationPair  = Pair.ANY;
   String         description   = "none";
   private String invalidReason = null;
   SpecialDamage specialDamageModifier = new SpecialDamage(0);

   // pain & bleeding
   byte painLevel       = 0;
   byte wounds          = 0;
   byte bleedRate       = 0;
   byte healedWounds    = 0;
   byte healedBleedRate = 0;

   // penalties are positive integers, so a 2 means a -2 to action.
   // A negative value means no use possible, such as a severed limb.
   byte penaltyArm      = 0;
   byte penaltyMove     = 0;
   byte knockedBackDist = 0;

   public Wound(byte damage, Location location, String description, int painLevel, int wounds, int bleedRate,
                int armPenalty, int movePenalty, int knockedDownDist, DamageType damageType, long effectsMask,
                Character target) {
      this.damage = damage;
      this.description = description;
      this.effectsMask = effectsMask;
      this.painLevel = (byte) painLevel;
      this.wounds = (byte) wounds;
      this.bleedRate = (byte) bleedRate;
      knockedBackDist = (byte) knockedDownDist;
      this.damageType = damageType;
      locationBase = location;
      this.location = location;

      if (target != null) {
         // resolve any generalities.
         // If this fails because the target doesn't have the location specified (ie, an 'arm' hit on a giant snake),
         // then the results will be placed in the '_invalidReason' String, indicating that this Wound is invalid.
         setInvalidReason(target.placeWound(this));
      }
      if ((this.location == Location.ARM) || (this.location == Location.LIMB) || (this.location == Location.BODY)) {
         penaltyArm = (byte) armPenalty;
      }
      if ((this.location == Location.LEG) || (this.location == Location.LIMB) || (this.location == Location.BODY)) {
         penaltyMove = (byte) movePenalty;
      }
   }

   public Wound() {
      // used only for serialization
   }

   public void setSpecialDamageModifier(SpecialDamage specialDamageModifier) {
      this.specialDamageModifier = specialDamageModifier;
      wounds += specialDamageModifier.getWoundModifier();
      painLevel += specialDamageModifier.getPainModifier();
   }

   public SpecialDamage getSpecialDamageModifier() {
      return specialDamageModifier;
   }

   public byte getLevel() {
      return damage;
   }

   public DamageType getDamageType() {
      return damageType;
   }

   public String getLocationName() {
      return getLocationName(location, locationSide, locationPair);
   }

   public String getBaseLocationName() {
      return getLocationName(locationBase, Side.ANY, Pair.ANY);
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
      this.damageType = damageType;
   }

   public void setLocation(Location location) {
      this.location = location;
   }

   public void setLocationSide(Side side) {
      locationSide = side;
   }

   public void setLocationPair(Pair pair) {
      locationPair = pair;
   }

   public boolean isKnockDown() {
      return ((effectsMask & (EFFECT_KNOCKDOWN | EFFECT_KNOCKOUT | EFFECT_COMA | EFFECT_DEATH)) != 0);
   }

   public boolean isKnockOut() {
      return ((effectsMask & (EFFECT_KNOCKOUT | EFFECT_COMA | EFFECT_DEATH)) != 0);
   }

   public boolean isFatal() {
      return ((effectsMask & EFFECT_DEATH) != 0);
   }

   public boolean isUnreadyWeapon() {
      return ((effectsMask & EFFECT_WEAPON_UNREADY) != 0);
   }

   public boolean isDropWeapon() {
      return ((effectsMask & EFFECT_WEAPON_DROPPED) != 0);
   }

   public boolean isSeveredArm() {
      return (penaltyArm == -2) && (location == Location.ARM);
   }

   public boolean isSeveredWing() {
      return (penaltyArm == -2) && (location == Location.WING);
   }

   public boolean isSeveredLeg() {
      return (penaltyMove == -2);
   }

   public byte getPain() {
      return painLevel;
   }

   public String getDescription() {
      return description;
   }

   public long getEffectsMask() {
      return effectsMask;
   }

   public void setEffectsMask(long effectsMask) {
      this.effectsMask = effectsMask;
   }

   public byte getWounds() {
      return wounds;
   }

   public byte getEffectiveWounds() {
      return (byte) (wounds - healedWounds);
   }

   public byte getBleedRate() {
      return bleedRate;
   }

   public byte getEffectiveBleedRate() {
      return (byte) (bleedRate - healedBleedRate);
   }

   public boolean regrowLimb() {
      boolean limbRegrown = false;
      if (penaltyArm == -2) {
         penaltyArm = 0;
         limbRegrown = true;
      }
      if (penaltyMove == -2) {
         penaltyMove = 0;
         limbRegrown = true;
      }
      if (limbRegrown) {
         healedWounds = wounds;
         healedBleedRate = bleedRate;
      }
      return limbRegrown;
   }

   public boolean healWound(byte woundReduction, byte bleedingReduction) {
      if (healedBleedRate < bleedingReduction) {
         healedBleedRate = bleedingReduction;
         if (healedBleedRate > bleedRate) {
            healedBleedRate = bleedRate;
         }
      }
      if (healedWounds < woundReduction) {
         healedWounds = woundReduction;
         if (healedWounds > wounds) {
            healedWounds = wounds;
         }
      }
      if (((wounds - healedWounds) == 0) && ((bleedRate - healedBleedRate) == 0)) {
         // Severed limbs are not re-grown by a cureWound spell.
         if ((penaltyArm != -2) && (penaltyMove != -2)) {
            // If all the wounds and bleeding have been stopped,
            // then the would is cured, so don't let any of the effect (crippling, etc.) continue to affect
            effectsMask = 0;
            penaltyArm = 0;
            penaltyMove = 0;
            // return true if there are no remaining effects (wounds, bleeding, or penalties)
            return true;
         }
      }
      // return false if there are still effects (wounds, bleeding, or penalties)
      return false;
   }

   public Location getLocation() {
      return location;
   }

   public Side getLocationSide() {
      return locationSide;
   }

   public Pair getLocationPair() {
      return locationPair;
   }

   public byte getPenaltyArm() {
      if (penaltyArm <= 0) {
         return penaltyArm;
      }
      return (byte) Math.max((penaltyArm - healedWounds), 0);
   }

   public byte getPenaltyMove() {
      if (penaltyMove <= 0) {
         return penaltyMove;
      }
      return (byte) Math.max((penaltyMove - healedWounds), 0);
   }

   public byte getKnockedBackDist() {
      return knockedBackDist;
   }

   public String describeWound() {
      StringBuilder sb = new StringBuilder();
      sb.append(damage).append(" points of ").append(damageType.fullname).append(" damage, resulting in ");
      sb.append(description).append(" to the ").append(getLocationName());
      String effects = describeEffects();
      if (effects.length() > 0) {
         sb.append(" with the following effect(s): ").append(effects);
      }

      if (painLevel > 0) {
         sb.append(", ").append(painLevel).append(" points of pain");
      }
      if (wounds > 0) {
         sb.append(", ").append(wounds).append(" wounds");
      }
      if (bleedRate > 0) {
         sb.append(", bleeding at ").append(bleedRate);
      }
      if (penaltyArm != 0) {
         sb.append(", ");
         switch (locationPair) {
            case SECOND: sb.append("second "); break;
            case THIRD:  sb.append("third "); break;
         }
         switch (locationSide) {
            case LEFT:  sb.append("left "); break;
            case RIGHT: sb.append("right "); break;
         }
         if (penaltyArm > 0) {
            sb.append("arm penalty of ").append(penaltyArm);
         }
         else if (penaltyArm == -1) {
            sb.append("arm crippled");
         }
         else if (penaltyArm == -2) {
            sb.append("arm severed");
         }
         else {
            DebugBreak.debugBreak("Don't know how to describe arm penalty of " + penaltyArm);
         }
      }
      if (penaltyMove > 0) {
         sb.append(", move and retreat penalty of ").append(penaltyMove);
         if (penaltyMove == -1) {
            sb.append(", unable to stand ");
         }
         else if (penaltyMove == -2) {
            sb.append(", unable to stand (leg severed) ");
         }
      }
      if (knockedBackDist > 0) {
         sb.append(", knocking target back ").append(knockedBackDist).append(" hexes");
      }
      return sb.toString();
   }

   public String describeEffects() {
      List<String> effects = new ArrayList<>();
      if ((effectsMask & EFFECT_DEATH) != 0) {
         effects.add("death");
      }
      if ((effectsMask & EFFECT_COMA) != 0) {
         effects.add("coma");
      }
      if ((effectsMask & EFFECT_DECAPITATE) != 0) {
         effects.add("decapitated");
      }
      if ((effectsMask & EFFECT_BRAIN_DESTROY) != 0) {
         effects.add("brain destroyed");
      }
      if ((effectsMask & EFFECT_BRAIN_DAMAGE) != 0) {
         effects.add("brain damaged");
      }
      if ((effectsMask & EFFECT_HEART) != 0) {
         effects.add("heart hit");
      }
      if ((effectsMask & EFFECT_ORGAN_DAM) != 0) {
         effects.add("organ damage");
      }
      if ((effectsMask & EFFECT_LUNG_DAM) != 0) {
         effects.add("lung damaged");
      }
      if ((effectsMask & EFFECT_VENA_CAVA) != 0) {
         effects.add("vena cava ruptured");
      }
      if ((effectsMask & EFFECT_KNOCKOUT) != 0) {
         effects.add("knocked unconscious");
      }
      if ((effectsMask & EFFECT_PARAPLEGIC) != 0) {
         effects.add("paraplegic");
      }
      if ((effectsMask & EFFECT_QUADRIPLEGIC) != 0) {
         effects.add("quadriplegic");
      }
      if ((effectsMask & EFFECT_BLINDED_1) != 0) {
         effects.add("blinded in one eye");
      }
      if ((effectsMask & EFFECT_KNOCKDOWN) != 0) {
         effects.add("knocked down");
      }
      if ((effectsMask & EFFECT_WEAPON_UNREADY) != 0) {
         effects.add("weapon unreadied");
      }
      if ((effectsMask & EFFECT_WEAPON_DROPPED) != 0) {
         effects.add("weapon dropped");
      }
      if ((effectsMask & EFFECT_CONCUSSION) != 0) {
         effects.add("concussion");
      }
      if ((effectsMask & EFFECT_OFF_BALANCE_1) != 0) {
         effects.add("off-balance (1-action)");
      }
      if ((effectsMask & EFFECT_OFF_BALANCE_2) != 0) {
         effects.add("off-balance (2-actions)");
      }
      if ((effectsMask & EFFECT_OFF_BALANCE_3) != 0) {
         effects.add("off-balance (3-actions)");
      }
      if ((effectsMask & EFFECT_WEAPON_KNOCKED_AWAY) != 0) {
         effects.add("weapon knocked away");
      }

      StringBuilder sb = new StringBuilder();
      if (effects.size() > 0) {
         sb.append(effects.remove(0));
         while (effects.size() > 0) {
            if (effects.size() == 1) {
               // General hits on limbs should show "weapon dropped OR knocked down", depending on which limb is hit.
               if (location == Location.LIMB) {
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
      return LimbType.get(location, locationSide, locationPair);
   }

   public byte getPenaltyLimb() {
      if (location == Location.ARM) {
         return penaltyArm;
      }
      if (location == Location.LEG) {
         return penaltyMove;
      }
      return 0;
   }

   public boolean isBlinding() {
      return (location == Location.EYE) || ((effectsMask & EFFECT_BLINDED_1) != 0);
   }

   public boolean isDecapitating() {
      return ((effectsMask & EFFECT_DECAPITATE) != 0);
   }

   public String describeArmPenalty() {
      return describePenalty(getPenaltyArm());
   }

   public String describeMovePenalty() {
      return describePenalty(getPenaltyMove());
   }

   public String describePenalty(byte value) {
      if (damageType == DamageType.GENERAL) {
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
         damage = readByte(in);
         damageType = DamageType.getByValue(readByte(in));
         effectsMask = readLong(in);
         locationBase = Location.values()[readInt(in)];
         location = Location.values()[readInt(in)];
         locationSide = Side.values()[readInt(in)];
         locationPair = Pair.values()[readInt(in)];
         description = readString(in);
         specialDamageModifier = new SpecialDamage(readInt(in));
         painLevel = readByte(in);
         wounds = readByte(in);
         bleedRate = readByte(in);
         healedWounds = readByte(in);
         healedBleedRate = readByte(in);
         penaltyArm = readByte(in);
         penaltyMove = readByte(in);
         knockedBackDist = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         writeToStream(damage, out);
         writeToStream(damageType.value, out);
         writeToStream(effectsMask, out);
         writeToStream(locationBase.ordinal(), out);
         writeToStream(location.ordinal(), out);
         writeToStream(locationSide.ordinal(), out);
         writeToStream(locationPair.ordinal(), out);
         writeToStream(description, out);
         writeToStream(specialDamageModifier.getBits(), out);
         writeToStream(painLevel, out);
         writeToStream(wounds, out);
         writeToStream(bleedRate, out);
         writeToStream(healedWounds, out);
         writeToStream(healedBleedRate, out);
         writeToStream(penaltyArm, out);
         writeToStream(penaltyMove, out);
         writeToStream(knockedBackDist, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public Element getXMLObject(Document parentDoc, String newLine) {
      Element mainElement = parentDoc.createElement("Wound");
      mainElement.setAttribute("damage", String.valueOf(damage));
      mainElement.setAttribute("damageType", String.valueOf(damageType.value));
      mainElement.setAttribute("effectsMask", String.valueOf(effectsMask));
      mainElement.setAttribute("locationBase", String.valueOf(locationBase));
      mainElement.setAttribute("location", String.valueOf(location));
      mainElement.setAttribute("locationSide", String.valueOf(locationSide));
      mainElement.setAttribute("locationPair", String.valueOf(locationPair));
      mainElement.setAttribute("description", String.valueOf(description));
      mainElement.setAttribute("specialDamageModifier", String.valueOf(specialDamageModifier.getBits()));
      mainElement.setAttribute("painLevel", String.valueOf(painLevel));
      mainElement.setAttribute("wounds", String.valueOf(wounds));
      mainElement.setAttribute("bleedRate", String.valueOf(bleedRate));
      mainElement.setAttribute("healedWounds", String.valueOf(healedWounds));
      mainElement.setAttribute("healedBleedRate", String.valueOf(healedBleedRate));
      mainElement.setAttribute("penaltyArm", String.valueOf(penaltyArm));
      mainElement.setAttribute("penaltyMove", String.valueOf(penaltyMove));
      mainElement.setAttribute("knockedBackDist", String.valueOf(knockedBackDist));
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
      damage = Byte.parseByte(attributes.getNamedItem("damage").getNodeValue());
      damageType = DamageType.getByValue(Byte.parseByte(attributes.getNamedItem("damageType").getNodeValue()));
      effectsMask = Long.parseLong(attributes.getNamedItem("effectsMask").getNodeValue());
      locationBase = Location.valueOf(attributes.getNamedItem("locationBase").getNodeValue());
      location = Location.valueOf(attributes.getNamedItem("location").getNodeValue());
      locationSide = Side.valueOf(attributes.getNamedItem("locationSide").getNodeValue());
      locationPair = Pair.valueOf(attributes.getNamedItem("locationPair").getNodeValue());
      description = attributes.getNamedItem("description").getNodeValue();
      specialDamageModifier.setBits(Integer.parseInt(attributes.getNamedItem("specialDamageModifier").getNodeValue()));
      painLevel = Byte.parseByte(attributes.getNamedItem("painLevel").getNodeValue());
      wounds = Byte.parseByte(attributes.getNamedItem("wounds").getNodeValue());
      bleedRate = Byte.parseByte(attributes.getNamedItem("bleedRate").getNodeValue());
      healedWounds = Byte.parseByte(attributes.getNamedItem("healedWounds").getNodeValue());
      healedBleedRate = Byte.parseByte(attributes.getNamedItem("healedBleedRate").getNodeValue());
      penaltyArm = Byte.parseByte(attributes.getNamedItem("penaltyArm").getNodeValue());
      penaltyMove = Byte.parseByte(attributes.getNamedItem("penaltyMove").getNodeValue());
      knockedBackDist = Byte.parseByte(attributes.getNamedItem("knockedBackDist").getNodeValue());
      return true;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Wound: ").append(damage);
      sb.append("-point ").append(damageType.name());
      sb.append(" ").append(getWounds());
      if (healedWounds > 0) {
         sb.append("(").append(getEffectiveWounds()).append(")");
      }
      sb.append(" wounds, ");

      sb.append(getBleedRate());
      if (healedBleedRate > 0) {
         sb.append("(").append(getEffectiveBleedRate()).append(")");
      }
      sb.append(" bleed, ");

      if (penaltyArm > 0) {
         sb.append(" arm penaly:").append(penaltyArm);
      }
      if (penaltyMove > 0) {
         sb.append(" move/retreat penaly:").append(penaltyMove);
      }
      return sb.toString();
   }

   public boolean isValid() {
      return (getInvalidReason() == null);
   }

   public boolean isCrippling() {
      return (getPenaltyLimb() < 0);
   }


   public void setInvalidReason(String invalidReason) {
      this.invalidReason = invalidReason;
   }
   public String getInvalidReason() {
      return invalidReason;
   }

   public Wound clone() {
      try {
         return (Wound) super.clone();
      } catch (CloneNotSupportedException e) {
         return null;
      }
   }
}
