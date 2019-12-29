/*
 * Created on May 13, 2007
 */
package ostrowski.combat.common.spells.mage;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.SpecialDamage;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.server.Arena;

public class SpellFlamingWeapon extends ExpiringMageSpell implements ICastInBattle
{
   public static final String NAME               = "Flaming Weapon";
   public boolean             _forMissileWeapons = false;
   protected Weapon           _weapon            = null;
   private SpecialDamage      _specDam;

   public SpellFlamingWeapon() {
      super(NAME, (short) 10/*baseExpirationTimeInTurns*/, (short) 5/*bonusTimeInTurnsPerPower*/,
            new Class[] { SpellControlFire.class, SpellControlTemperature.class, SpellCreateFire.class},
            new MageCollege[] { MageCollege.CONJURATION, MageCollege.FIRE});
   }

   protected SpellFlamingWeapon(String name, Class<? extends MageSpell>[] requiredClasses) {
      super(name, (short) 10/*baseExpirationTimeInTurns*/, (short) 5/*bonusTimeInTurnsPerPower*/, requiredClasses,
            new MageCollege[] { MageCollege.CONJURATION, MageCollege.FIRE});
      _forMissileWeapons = true;
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (_weapon == null) {
         return null;
      }
      StringBuilder sb = new StringBuilder();
      if (firstTime) {
         sb.append(getTargetName()).append("'s ");
      }
      sb.append(_weapon.getName());
      if (firstTime) {
         sb.append(" bursts into a ");
      }
      else {
         sb.append(" is burning with a ");
      }
      sb.append(getPower()).append("-point flame (");
      if (_weapon.isMissileWeapon() != _forMissileWeapons) {
         sb.append("which will do NOTHING, since it is ");
         if (!_weapon.isMissileWeapon()) {
            sb.append("not ");
         }
         sb.append("a missile weapon!)");
      }
      else {
         int wounds = getWounds(getPower());
         int pain = getPain(getPower());
         if (wounds > 0) {
            if (wounds > 0) {
               sb.append("+");
            }
            sb.append(wounds).append(" wounds, and ");
         }
         if (pain > 0) {
            sb.append("+");
         }
         sb.append(pain).append(" pain.)");
      }
      return sb.toString();
   }

   @Override
   public String describeSpell() {
      Table table = new Table();
      table.addRow(new TableRow(-1, "Power", "Extra pain", "Extra wounds"));
      for (byte power = 1; power < 8; power++) {
         table.addRow(new TableRow(power-1, ""+power, ""+getPain(power), ""+getWounds(power)));
      }
      StringBuilder sb = new StringBuilder();
      sb.append("The '").append(getName()).append("' spell causes the subject's primary weapon (or hands, if no weapon is held) to");
      sb.append(" ignite into flames. Any time the weapon hits an opponent, and penetrates the armor (does at least 1 point of damage)");
      sb.append(" then the flames of the weapon add additional pain and wounds, based upon the power of the spell:");
      sb.append(table);
      sb.append("<br/>This spell has no effect when cast on missile weapons, as the flames do not propagate to the missile.");
      sb.append(" Use the 'Flaming Missile Weapon' spell for that.");
      sb.append(" The '").append(getName()).append("' spell is suitable for casting on thrown weapons.");
      return sb.toString();
   }

   protected byte getWounds(byte power) {
      switch (power) {
         case 0:
            return 0;
         case 1:
            return 0;
         case 2:
            return 1;
         case 3:
            return 1;
         case 4:
            return 2;
         case 5:
            return 2;
         case 6:
            return 3;
         case 7:
            return 3;
         case 8:
            return 4;
      }
      return 0;
   }

   protected byte getPain(byte power) {
      switch (power) {
         case 0:
            return 2;
         case 1:
            return 3;
         case 2:
            return 3;
         case 3:
            return 4;
         case 4:
            return 4;
         case 5:
            return 5;
         case 6:
            return 5;
         case 7:
            return 6;
         case 8:
            return 6;
      }
      return 0;
   }

   @Override
   public void applyEffects(Arena arena) {
      Weapon weapon = getTarget().getWeapon();
      if (weapon != null) {
         _weapon = weapon;
         if (_weapon.isMissileWeapon() == _forMissileWeapons) {
            _specDam = new SpecialDamage(SpecialDamage.MOD_FLAMING);
            _specDam.setPainModifier(getPain(getPower()));
            _specDam.setWoundModifier(getWounds(getPower()));
            weapon.setSpecialDamageModifier(_specDam, getName() + " spell (power level " + getPower() + ": +" + getPain(getPower()) + " pain, +"
                                                      + getWounds(getPower()) + " wounds)");
         }
      }
   }

   @Override
   public void removeEffects(Arena arena) {
      if (_specDam != null) {
         _specDam.setPainModifier((byte) 0);
         _specDam.setWoundModifier((byte) 0);
         if (_weapon != null) {
            // TODO: what if the weapon has other special damage modifiers?
            _weapon.setSpecialDamageModifier(_specDam, "");
         }
      }
   }

   @Override
   public boolean isBeneficial() {
      return true;
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE;
   }

   LimbType _limbType = null;

   @Override
   public void setCasterAndTargetFromIDs(List<Character> combatants) {
      super.setCasterAndTargetFromIDs(combatants);
      if (_limbType != null) {
         Limb limb = getTarget().getLimb(_limbType);
         _weapon = limb.getWeapon(getTarget());
         _weapon.setSpecialDamageModifier(_specDam, "Flaming Weapon spell (power level " + getPower() + ": +" + getPain(getPower()) + " pain, +"
                                                    + getWounds(getPower()) + " wounds)");
      }
   }

   @Override
   public Element getXMLObject(Document parentDoc, String newLine) {
      Element node = super.getXMLObject(parentDoc, newLine);
      for (Limb limb : getTarget().getLimbs()) {
         if (_weapon == limb.getWeapon(getTarget())) {
            node.setAttribute("weaponLimbIndex", String.valueOf(limb._limbType.value));
            break;
         }
      }
      return node;
   }

   @Override
   public void readFromXMLObject(NamedNodeMap namedNodeMap) {
      super.readFromXMLObject(namedNodeMap);
      Node node = namedNodeMap.getNamedItem("weaponLimbIndex");
      if (node != null) {
         _limbType = LimbType.getByValue(Byte.parseByte(node.getNodeValue()));
         _specDam = new SpecialDamage(SpecialDamage.MOD_FLAMING);
         _specDam.setPainModifier(getPain(getPower()));
         _specDam.setWoundModifier(getWounds(getPower()));
      }
   }
}
