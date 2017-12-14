/*
 * Created on Oct 23, 2006
 *
 */
package ostrowski.combat.common.things;

import org.eclipse.swt.graphics.RGB;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DrawnObject;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.wounds.Wound;

public class Leg extends Limb
{
   public Leg() {}
   public Leg(LimbType limbType, Race racialBase) {
      super(limbType, racialBase);
   }

   @Override
   public Weapon getWeapon(Character character)
   {
      if (character.isAnimal()) {
         if (character.getRace().hasProperty(Race.PROPERTIES_CLAWS)) {
            return Weapon.getWeapon(Weapon.NAME_Claws, character.getRace());
         }
         return null;
      }
      return Weapon.getWeapon(Weapon.NAME_KarateKick, character.getRace());
   }

   @Override
   public DrawnObject drawThing(int narrowDiameter, int wideDiameter, RGB foreground, RGB background)
   {
      DrawnObject obj = new DrawnObject(foreground, background);
      if (getLocationSide() == Wound.Side.LEFT) {
         wideDiameter *= -1;
      }

      obj.addPoint((wideDiameter * 19)/32, 0);                        // outer hip
      obj.addPoint((wideDiameter * 16)/32, 0-((narrowDiameter * 16)/32)); // outer knee
      obj.addPoint((wideDiameter *  5)/32, 0-((narrowDiameter * 25)/32)); // outer ankle
      obj.addPoint((wideDiameter *  5)/32, 0-((narrowDiameter * 31)/32)); // top toes
      obj.addPoint((wideDiameter *  4)/32, 0-((narrowDiameter * 32)/32)); // bottom toes
      obj.addPoint((wideDiameter *  2)/32, 0-((narrowDiameter * 23)/32)); // heel
      obj.addPoint((wideDiameter *  4)/32, 0-((narrowDiameter * 22)/32)); // ankle
      obj.addPoint((wideDiameter * 13)/32, 0-((narrowDiameter * 13)/32)); // knee
      obj.addPoint((wideDiameter * 14)/32, 0);                        // hip
      obj.addPoint((wideDiameter * 16)/32, (narrowDiameter * 3)/32);    // hip
      return obj;
   }

   @Override
   public Object clone() {
      return new Leg(_limbType, getRacialBase());
   }

}
