/*
 * Created on May 11, 2006
 *
 */
package ostrowski.combat.protocol;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.enums.Enums;
import ostrowski.protocol.SerializableObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServerStatus extends SerializableObject implements Enums
{
   private List<Character>         _combatants            = new ArrayList<>();
   private final List<Integer>     _combatantsWaitingByID = new ArrayList<>();

   public ServerStatus() {}
   public ServerStatus(CombatMap map, List<Character> combatants,
                       List<Character> combatantsWaitingToConnect)
   {
      _combatants = combatants;
      for (Character chr : combatantsWaitingToConnect) {
         _combatantsWaitingByID.add(chr._uniqueID);
      }
   }

//   public Object writeReplace() throws ObjectStreamException { return null; }
//   public Object readResolve() throws ObjectStreamException {  return null; }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(_combatants, out);
         writeToStream(_combatantsWaitingByID, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   static public int readIntoListCharacter(List<Character> data, DataInputStream in) throws IOException {
      data.clear();
      for (SerializableObject obj : readIntoListSerializableObject(in)) {
         if (obj instanceof Character) {
            data.add((Character) obj);
         }
      }
      return data.size();
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         readIntoListCharacter(_combatants, in);
         readIntoListInteger(_combatantsWaitingByID, in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public List<Character> getCombatants() {
      return new ArrayList<>(_combatants);
   }
   public List<Character> getCombatantsWaitingToConnect() {
      List<Character> list = new ArrayList<>();
      for (Character chr : _combatants) {
         if (_combatantsWaitingByID.contains(chr._uniqueID)) {
            list.add(chr);
         }
      }
      return list;
   }

   @Override
   public String toString() {
      return "ServerStatus.";
   }
}
