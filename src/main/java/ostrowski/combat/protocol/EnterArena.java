/*
 * Created on May 11, 2006
 *
 */
package ostrowski.combat.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ostrowski.combat.common.Character;
import ostrowski.protocol.SerializableObject;

public class EnterArena extends SerializableObject
{
   boolean   _entering    = true;
   Character _character   = null;
   byte      _team        = 0;
   byte      _indexOnTeam = 0;
   public Character getCharacter() { return _character; }
   public boolean   isEntering()   { return _entering; }
   public byte getTeam()  {      return _team;   }
   public byte getIndexOnTeam()  {      return _indexOnTeam;   }

   public EnterArena() {}
   public EnterArena(Character character, boolean entering, byte team, byte indexOnTeam) {
      _character = character;
      _entering  = entering;
      _team      = team;
      _indexOnTeam = indexOnTeam;
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         _character.serializeToStream(out);
         writeToStream(_entering, out);
         writeToStream(_team, out);
         writeToStream(_indexOnTeam, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         _character   = new Character();
         _character.serializeFromStream(in);
         _entering    = readBoolean(in);
         _team        = readByte(in);
         _indexOnTeam = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("EnterArena: ");
      sb.append(_character);
      sb.append(", entering: ").append(_entering);
      sb.append(", team: ").append(_team);
      sb.append(", indexOnTeam: ").append(_indexOnTeam);
      return sb.toString();
   }
}
