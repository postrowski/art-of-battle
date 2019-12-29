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

public class Wing extends Limb
{
   public enum Type {
      Bat,
      Fairy,
      Feathered
   }
   //static public byte TYPE_BAT = 0; // bats, demons, dragons
   //static public byte TYPE_FAIRY = 1; // pixies, insects
   //static public byte TYPE_FEATHERED = 2; // birds
   Wing.Type _wingType = Wing.Type.Bat;
   public Wing() {}
   public Wing(LimbType id, Wing.Type type, Race racialBase) {
      super(id, racialBase);
      _wingType = type;
   }
   @Override
   public Weapon getWeapon(Character character) {
      return null;
   }

   @Override
   public DrawnObject drawThing(int narrowDiameter, int wideDiameter, RGB foreground, RGB background)
   {
      if (getLocationSide() == Wound.Side.LEFT) {
         wideDiameter *= -1;
      }

      DrawnObject obj = new DrawnObject(foreground, background);
      if (_wingType == Wing.Type.Bat) {
         obj.addPoint((wideDiameter *  4)/32, -((narrowDiameter * 5) / 32));  // front shoulder
         obj.addPoint((wideDiameter *  7)/32, -((narrowDiameter * 4) / 32));  // front elbow
         obj.addPoint((wideDiameter * 11)/32, -((narrowDiameter * 7) / 32));  // front elbow
         obj.addPoint((wideDiameter * 17)/32, -((narrowDiameter * 2) / 32));  // front wrist
         obj.addPoint((wideDiameter * 19)/32, -((narrowDiameter * -4) / 32));  // wing tip
         obj.addPoint((wideDiameter * 17)/32, -((narrowDiameter * -5) / 32));  // back wrist
         obj.addPoint((wideDiameter * 15)/32, -((narrowDiameter * -12) / 32));  // back outer extension
         obj.addPoint((wideDiameter * 13)/32, -((narrowDiameter * -6) / 32));  // back inner draw
         obj.addPoint((wideDiameter * 10)/32, -((narrowDiameter * -15) / 32));  // back inner extension
         obj.addPoint((wideDiameter *  4)/32, -((narrowDiameter * -6) / 32));  // back shoulder
      }
      else if (_wingType == Wing.Type.Fairy) {
         obj.addPoint((wideDiameter *  4)/32, -((narrowDiameter * 5) / 32));  // front shoulder
         obj.addPoint((wideDiameter *  7)/32, -((narrowDiameter * 4) / 32));  // front elbow
         obj.addPoint((wideDiameter * 11)/32, -((narrowDiameter * 7) / 32));  // front elbow
         obj.addPoint((wideDiameter * 13)/32, -((narrowDiameter * 5) / 32));  // front edge
         obj.addPoint((wideDiameter * 17)/32, -((narrowDiameter * 2) / 32));  // front wrist
         obj.addPoint((wideDiameter * 19)/32, -((narrowDiameter * -4) / 32));  // wing tip
         obj.addPoint((wideDiameter * 17)/32, -((narrowDiameter * -10) / 32));  // back wrist
         obj.addPoint((wideDiameter * 10)/32, -((narrowDiameter * -8) / 32));  // back inner extension
         obj.addPoint((wideDiameter *  4)/32, -((narrowDiameter * -10) / 32));  // back shoulder
      }
      else if (_wingType == Wing.Type.Feathered) {
         obj.addPoint((wideDiameter *  4)/32, -((narrowDiameter * 5) / 32));  // front shoulder
         obj.addPoint((wideDiameter *  7)/32, -((narrowDiameter * 4) / 32));  // front elbow
         obj.addPoint((wideDiameter * 11)/32, -((narrowDiameter * 7) / 32));  // front elbow
         obj.addPoint((wideDiameter * 13)/32, -((narrowDiameter * 5) / 32));  // front edge
         obj.addPoint((wideDiameter * 17)/32, -((narrowDiameter * 2) / 32));  // front wrist
         obj.addPoint((wideDiameter * 19)/32, -((narrowDiameter * -4) / 32));  // wing tip
         obj.addPoint((wideDiameter * 17)/32, -((narrowDiameter * -10) / 32));  // back wrist
         obj.addPoint((wideDiameter * 10)/32, -((narrowDiameter * -8) / 32));  // back inner extension
         obj.addPoint((wideDiameter *  4)/32, -((narrowDiameter * -10) / 32));  // back shoulder
      }
      return obj;
   }

   @Override
   public Wing clone() {
      return new Wing(_limbType, _wingType, getRacialBase());
   }
}
