package ostrowski.combat.common.orientations;

import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.swt.graphics.RGB;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DrawnObject;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.things.Leg;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.server.ArenaLocation;

public class OrientationDoubleCentaur extends OrientationSerpentine
{
   public OrientationDoubleCentaur() {
      super(2);
   }

   public static String getSerializeName() {
      return "O-DC";
   }
   @Override
   public void serializeNameToStream(DataOutputStream out) throws IOException {
      writeToStream(getSerializeName(), out);
   }

   @Override
   public DrawnObject getBodyOutlines(Character character, int wideDiameter, int narrowDiameter, ArenaLocation loc, int[] bounds, RGB foreground, RGB background)
   {
      DrawnObject charOutlines = DrawnObject.createElipse((int) (wideDiameter*.8), narrowDiameter*3, wideDiameter, foreground, background);
      int yOffset;
      int xOffset = 0;
      if (loc.sameCoordinates(_coordinates.get(0))) {
         yOffset = (int) (narrowDiameter*1.25);
      }
      else {
         yOffset = (int) (0-(narrowDiameter*1.25));
      }
      charOutlines.offsetPoints(xOffset, yOffset);
      if (loc.sameCoordinates(_coordinates.get(0))) {
         charOutlines.addChild(DrawnObject.createElipse(wideDiameter, narrowDiameter, wideDiameter, foreground, background));
      }
      else {
         charOutlines.addChild(DrawnObject.createElipse((int)(wideDiameter*.8), (int)(narrowDiameter*1.2), wideDiameter, foreground, background));
      }
      return charOutlines;
   }

   @Override
   public boolean shouldDraw(Limb limb) {
      if ((limb == null) || (limb.isSevered())) {
         return false;
      }
      if (limb instanceof Leg) {
         return (getPosition() == Position.PRONE_BACK) || (getPosition() == Position.PRONE_FRONT);
      }
      return true;
   }
}
