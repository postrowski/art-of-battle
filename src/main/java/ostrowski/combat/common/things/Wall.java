/*
 * Created on Nov 22, 2006
 *
 */
package ostrowski.combat.common.things;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.enums.TerrainWall;

public class Wall extends Thing implements Enums
{
   public TerrainWall _orientation = TerrainWall.VERT_CENTER;

   public Wall() {
      // default 'ctor used for serialization
   }
   public Wall(TerrainWall orientation) {
      super("Wall", null/*racialBase*/, 0/*cost*/, 100/*weight*/, (byte)0/*pd*/);
      _orientation = orientation;
   }
   public Wall(String name, double weight, TerrainWall orientation) {
      super(name, null/*racialBase*/, 0/*cost*/, 100/*weight*/, (byte)0/*pd*/);
      _orientation = orientation;
   }

   public boolean equals(Wall other) {
      if (other == null) {
         return false;
      }
      if (_orientation  != other._orientation) {
         return false;
      }
      return super.equals(other);
   }

   @Override
   public void copyData(Thing source)
   {
      super.copyData(source);
      if (source instanceof Wall) {
         Wall doorSource = (Wall) source;
         _orientation = doorSource._orientation;
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      super.serializeFromStream(in);
      try {
         _orientation = TerrainWall.getByBitMask(readLong(in));
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out)
   {
      super.serializeToStream(out);
      try {
         writeToStream(_orientation.bitMask, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public String toString()
   {
      return "Wall";
   }
   @Override
   public Wall clone() {
      return new Wall(_name, _weight, _orientation);
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
}
