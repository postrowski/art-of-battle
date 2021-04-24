/*
 * Created on May 13, 2007
 */
package ostrowski.combat.common.spells.mage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.SpecialDamage;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.server.Arena;

import java.util.List;

public class SpellFlamingWeapon extends ExpiringMageSpell implements ICastInBattle
{
   public static final String NAME              = "Flaming Weapon";
   public boolean        forMissileWeapons = false;
   protected Weapon      weapon            = null;
   private SpecialDamage specDam;
   LimbType limbType = null;


   public SpellFlamingWeapon() {
      super(NAME, (short) 10/*baseExpirationTimeInTurns*/, (short) 5/*bonusTimeInTurnsPerPower*/,
            new Class[] { SpellControlFire.class, SpellControlTemperature.class, SpellCreateFire.class},
            new SkillType[] {SkillType.Spellcasting_Conjuration, SkillType.Spellcasting_Fire});
   }

   protected SpellFlamingWeapon(Class<? extends MageSpell>[] requiredClasses) {
      super(SpellFlamingMissileWeapon.NAME, (short) 10/*baseExpirationTimeInTurns*/, (short) 5/*bonusTimeInTurnsPerPower*/, requiredClasses,
            new SkillType[] { SkillType.Spellcasting_Conjuration, SkillType.Spellcasting_Fire});
      forMissileWeapons = true;
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (weapon == null) {
         return null;
      }
      StringBuilder sb = new StringBuilder();
      if (firstTime) {
         sb.append(getTargetName()).append("'s ");
      }
      sb.append(weapon.getName());
      if (firstTime) {
         sb.append(" bursts into a ");
      }
      else {
         sb.append(" is burning with a ");
      }
      sb.append(getPower()).append("-point flame (");
      if (weapon.isMissileWeapon() != forMissileWeapons) {
         sb.append("which will do NOTHING, since it is ");
         if (!weapon.isMissileWeapon()) {
            sb.append("not ");
         }
         sb.append("a missile weapon!)");
      }
      else {
         int wounds = getWounds(getPower());
         int pain = getPain(getPower());
         if (wounds > 0) {
            sb.append("+").append(wounds).append(" wounds");
            if (pain > 0) {
               sb.append(", and");
            }
         }
         if (pain > 0) {
            sb.append("+").append(pain).append(" pain");
         }
         sb.append(".)");
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
      return "The '" + getName() + "' spell causes the subject's primary weapon (or hands, if no weapon is held) to" +
             " ignite into flames. Any time the weapon hits an opponent, and penetrates the armor (does at least 1 point of damage)" +
             " then the flames of the weapon add additional pain and wounds, based upon the power of the spell:" +
             table +
             "<br/>This spell has no effect when cast on missile weapons, as the flames do not propagate to the missile." +
             " Use the 'Flaming Missile Weapon' spell for that." +
             " The '" + getName() + "' spell is suitable for casting on thrown weapons.";
   }

   protected byte getWounds(byte power) {
      switch (power) {
         case 0:
         case 1: return 0;
         case 2:
         case 3: return 1;
         case 4:
         case 5: return 2;
         case 6:
         case 7: return 3;
         case 8: return 4;
      }
      return 0;
   }

   protected byte getPain(byte power) {
      switch (power) {
         case 0: return 2;
         case 1:
         case 2: return 3;
         case 3:
         case 4: return 4;
         case 5:
         case 6: return 5;
         case 7: 
         case 8: return 6;
      }
      return 0;
   }

   @Override
   public void applyEffects(Arena arena) {
      Weapon weapon = getTarget().getWeapon();
      if (weapon != null) {
         this.weapon = weapon;
         if (this.weapon.isMissileWeapon() == forMissileWeapons) {
            specDam = new SpecialDamage(SpecialDamage.MOD_FLAMING);
            specDam.setPainModifier(getPain(getPower()));
            specDam.setWoundModifier(getWounds(getPower()));
            weapon.setSpecialDamageModifier(specDam, getName() + " spell (power level " + getPower() + ": +" + getPain(getPower()) + " pain, +"
                                                     + getWounds(getPower()) + " wounds)");
         }
      }
   }

   @Override
   public void removeEffects(Arena arena) {
      if (specDam != null) {
         specDam.setPainModifier((byte) 0);
         specDam.setWoundModifier((byte) 0);
         if (weapon != null) {
            // TODO: what if the weapon has other special damage modifiers?
            weapon.setSpecialDamageModifier(specDam, "");
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

   @Override
   public void setCasterAndTargetFromIDs(List<Character> combatants) {
      super.setCasterAndTargetFromIDs(combatants);
      if ((limbType != null) && (target != null)) {
         Limb limb = target.getLimb(limbType);
         weapon = limb.getWeapon(target);
         weapon.setSpecialDamageModifier(specDam, "Flaming Weapon spell (power level " +
                                                  getPower() + ": +" + getPain(getPower()) + " pain, +"
                                                  + getWounds(getPower()) + " wounds)");
      }
   }

   @Override
   public Element getXMLObject(Document parentDoc, String newLine) {
      Element node = super.getXMLObject(parentDoc, newLine);
      if (target != null) {
         for (Limb limb : target.getLimbs()) {
            if (weapon == limb.getWeapon(target)) {
               node.setAttribute("weaponLimbIndex", String.valueOf(limb.limbType.value));
               break;
            }
         }
      }
      return node;
   }

   @Override
   public void readFromXMLObject(NamedNodeMap namedNodeMap) {
      super.readFromXMLObject(namedNodeMap);
      Node node = namedNodeMap.getNamedItem("weaponLimbIndex");
      if (node != null) {
         limbType = LimbType.getByValue(Byte.parseByte(node.getNodeValue()));
         specDam = new SpecialDamage(SpecialDamage.MOD_FLAMING);
         specDam.setPainModifier(getPain(getPower()));
         specDam.setWoundModifier(getWounds(getPower()));
      }
   }
}
