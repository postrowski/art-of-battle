package ostrowski.combat.common.orientations;

import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.things.Head;
import ostrowski.combat.common.things.Leg;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.server.ArenaLocation;

import java.io.DataOutputStream;
import java.io.IOException;

public class OrientationSingleQuadraped extends OrientationSerpentine
{
   public OrientationSingleQuadraped() {
      super(1);
   }

   public static String getSerializeName() {
      return "O-SQ";
   }
   @Override
   public void serializeNameToStream(DataOutputStream out) throws IOException {
      writeToStream(getSerializeName(), out);
   }

   @Override
   public int getBodyWideDiameter(int size)   { return (int) (size * .65);}
   @Override
   public int getBodyNarrowDiameter(int size) { return size;}
   @Override
   public int getLimbOffsetY(Limb limb, int size, ArenaLocation loc) {
      if (limb != null) {
         if (limb instanceof Head) {
            return -(size / 3);
         }
      }
      return super.getLimbOffsetY(limb, size, loc);
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
