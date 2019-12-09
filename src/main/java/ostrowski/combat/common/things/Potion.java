/*
 * Created on Nov 29, 2006
 *
 */
package ostrowski.combat.common.things;

import java.util.ArrayList;

import org.eclipse.swt.graphics.RGB;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DrawnObject;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.healing.SpellCureSeriousWound;
import ostrowski.combat.common.spells.priest.healing.SpellHeal;
import ostrowski.combat.common.spells.priest.offensive.SpellSpeed;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.BattleTerminatedException;

public class Potion extends Thing implements Cloneable
{
   public static final String POTION_MINOR_HEALING   = "Potion:Minor Healing";
   public static final String POTION_HEALING         = "Potion:Healing";
   public static final String POTION_MAJOR_HEALING   = "Potion:Major Healing";
   public static final String POTION_FULL_HEALING    = "Potion:Complete Healing";
   public static final String POTION_INCR_STRENGTH   = "Potion:Increase Strength";
   public static final String POTION_INCR_NIMBLENESS = "Potion:Increase Nimbleness";
   public static final String POTION_INCR_DEXTERITY  = "Potion:Increase Dexterity";
   public static final String POTION_SPEED           = "Potion:Speed";
   public static final String POTION_MANA            = "Potion:Restore Mana";
   byte _strength; // potency (points healed, strength gained, etc.)
   byte _duration; // number of turns this potion remains in effect.
   private RGB _background;

   public Potion()
   {
   }

   public Potion(String name, int strength, int duration, int cost, RGB background)
   {
      super(name, null/*racialBase*/, cost, 1/*weight*/, (byte)0/*passive Defense*/);
      _strength = (byte) strength;
      _duration = (byte) duration;
      _background = background;
   }

   @Override
   public Potion clone() {
      return new Potion(_name, _strength, _duration, _cost, _background);
   }

   public byte getStrength() {
      return _strength;
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
   public String getDefenseName(boolean tensePast, Character defender) {
      return null;
   }

   @Override
   public ArrayList<SkillType> getDefenseSkillTypes() {
      return null;
   }

   @Override
   public boolean canBeApplied() {
      return true;
   }
   @Override
   public String getApplicationName() {
      return "Drink " + getName().substring("Potion:".length()) + " potion";
   }

   /**
    * Returns true is the item is consumed when applied (thus disappearing)
    * @param character
    * @param arena
    */
   @Override
   public boolean apply(Character character, Arena arena) throws BattleTerminatedException {
      PriestSpell spell = null;
      if (_name.equals(POTION_MINOR_HEALING)) {
         spell = new SpellCureSeriousWound();
      }
      else if (_name.equals(POTION_HEALING)) {
         spell = new SpellCureSeriousWound();
      }
      else if (_name.equals(POTION_MAJOR_HEALING)) {
         spell = new SpellCureSeriousWound();
      }
      else if (_name.equals(POTION_FULL_HEALING)) {
         spell = new SpellHeal();
      }
      else if (_name.equals(POTION_SPEED)) {
         spell = new SpellSpeed();
      }

      if (spell != null) {
         spell.setPower(_strength);
         spell.setTarget(character);
         spell.completeSpell();
         spell.applySpell(character/*target*/, arena);
      }
      else {
         int index = _name.indexOf("Increase ");
         if (index != -1) {
            String attributeName = _name.substring(index + "Increase ".length());
            for (Attribute attr : Attribute.values()) {
               if (attributeName.equalsIgnoreCase(attr.shortName) ||
                   attributeName.equalsIgnoreCase(attr.name())) {
                  character.setAttribute(attr, (byte) (character.getAttributeLevel(attr) + _strength), false/*containInLimits*/);
                  break;
               }
            }
         }
         if (_name.equals(POTION_MANA)) {
            character.resetSpellPoints();
         }
      }
      // Potions, once used disappear - so return 'true'
      return true;
   }

   static Potion[] _potionList = new Potion[] {                //  strength, duration, cost
                                  new Potion(POTION_MINOR_HEALING,     1,           0,  250, new RGB(  0,   0, 100)),
                                  new Potion(POTION_HEALING,           3,           0,  500, new RGB(  0,   0, 150)),
                                  new Potion(POTION_MAJOR_HEALING,     5,           0, 1000, new RGB(  0,   0, 200)),
                                  new Potion(POTION_FULL_HEALING,     10,           0, 5000, new RGB(  0,   0, 255)),
                                  new Potion(POTION_INCR_STRENGTH,     3,          50,  600, new RGB(255,   0,   0)),
                                  new Potion(POTION_INCR_NIMBLENESS,   3,          50,  750, new RGB(255, 255,   0)),
                                  new Potion(POTION_INCR_DEXTERITY,    3,          50, 1500, new RGB(  0, 255,   0)),
                                  new Potion(POTION_SPEED,             3,          50, 3000, new RGB(222, 222, 222)),
                                  new Potion(POTION_MANA,              1,           0, 5000, new RGB(  0, 222, 222)),
   };
   public static Potion getPotion(String thingName, Race racialBase)
   {
      for (Potion potion : _potionList) {
         if (potion.getName().equals(thingName))
         {
            Potion duplicatePotion = potion.clone();
            duplicatePotion.setRacialBase(racialBase);
            return duplicatePotion;
         }
      }
      return null;
   }

   @Override
   public DrawnObject drawThing(int size, RGB foreground, RGB background) {
      int radius = size/5;
      int pointCount = 50;
      DrawnObject potion = new DrawnObject(foreground, _background);
      double stepSize  = (2 * Math.PI)/pointCount;
      for (int point=2 ; point<(pointCount-2) ; point++) {
         double ang = point * stepSize;
         int x = (int) Math.round(Math.sin(ang) * radius);
         int y = (int) Math.round(Math.cos(ang) * radius);
         // Extend the stem of the potion for the first and last points.
         if ((point==2) || (point==(pointCount-3))) {
            y = radius * 2;
         }
         potion.addPoint(x, y);
      }
      return potion;
   }

   @Override
   public String getName() {
      return super.getName();//"Potion";
   }

   public boolean isBeneficial() {
      return true;
   }
   public boolean isHealing() {
      return getName().endsWith("Healing");
   }
   @Override
   public boolean isSizeAdjusted() {
      return false;
   }

   public static ArrayList<String> getPotionNames() {
      ArrayList<String> names = new ArrayList<>();
      for (Potion potion : _potionList) {
         names.add(potion.getName());
      }
      return names;
   }
}
