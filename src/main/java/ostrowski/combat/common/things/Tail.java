/*
 * Created on Oct 23, 2006
 *
 */
package ostrowski.combat.common.things;

import org.eclipse.swt.graphics.RGB;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DrawnObject;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.orientations.OrientationReptilian;

public class Tail extends Limb
{
   public Tail() {}
   public Tail(LimbType id, Race race) {
      super(id, race);
   }
   @Override
   public Weapon getWeapon(Character character)
   {
      if (character.hasAdvantage(Race.PROPERTIES_TAIL)) {
         return Weapon.getWeapon(Weapon.NAME_TailStrike, character.getRace());
      }
      return null;
   }
//   static Weapon _tailStrike = Weapon.getWeapon(Weapon.NAME_TailStrike);

   @Override
   public DrawnObject drawThing(int narrowDiameter, int size, RGB foreground, RGB background) {
      DrawnObject tail = new DrawnObject(foreground, background);
      double tailWidth = size/3.0;
      int baseSize = 1;
      Orientation orientation = getRacialBase().getBaseOrientation();
      if (orientation instanceof OrientationReptilian) {
         baseSize = ((OrientationReptilian)orientation)._baseSize;
      }
      double tailLength = narrowDiameter * 1.2 * baseSize;
      tail.addPoint(tailWidth/2.0, tailLength / 3.6);
      tail.addPoint(0, tailLength);
      tail.addPoint(0-(tailWidth/2.0), tailLength / 3.6);
      return tail;
   }
   @Override
   public Tail clone() {
      return new Tail(_limbType, getRacialBase());
   }

}
