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
   boolean   entering    = true;
   Character character   = null;
   byte      team        = 0;
   byte      indexOnTeam = 0;
   public Character getCharacter()   { return character; }
   public boolean   isEntering()     { return entering; }
   public byte      getTeam()        { return team;   }
   public byte      getIndexOnTeam() { return indexOnTeam;   }

   public EnterArena() {}
   public EnterArena(Character character, boolean entering, byte team, byte indexOnTeam) {
      this.character = character;
      this.entering = entering;
      this.team = team;
      this.indexOnTeam = indexOnTeam;
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         character.serializeToStream(out);
         writeToStream(entering, out);
         writeToStream(team, out);
         writeToStream(indexOnTeam, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         character = new Character();
         character.serializeFromStream(in);
         entering = readBoolean(in);
         team = readByte(in);
         indexOnTeam = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public String toString() {
      return "EnterArena: " +
             character +
             ", entering: " + entering +
             ", team: " + team +
             ", indexOnTeam: " + indexOnTeam;
   }
}
