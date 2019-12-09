/*
 * Created on May 11, 2006
 *
 */
package ostrowski.combat.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.enums.Enums;
import ostrowski.protocol.SerializableObject;

public class ServerStatus extends SerializableObject implements Enums
{
   private byte[]              _roomOnTeam            = new byte[TEAM_NAMES.length];
   private List<Character>     _combatants            = new ArrayList<>();
   private final List<Integer> _combatantsWaitingByID = new ArrayList<>();

   public ServerStatus() {}
   public ServerStatus(CombatMap map, List<Character> combatants,
                       List<Character> combatantsWaitingToConnect)
   {
      byte[] roomOnTeam = map.getAvailableCombatantsOnTeams();
      _combatants = combatants;
      for (byte team=0 ; team<_roomOnTeam.length ; team++) {
         _roomOnTeam[team] = roomOnTeam[team];
      }
      for (Character chr : combatantsWaitingToConnect) {
         _combatantsWaitingByID.add(chr._uniqueID);
      }
   }

   public boolean isRoomOnTeam(byte team) {
      return _roomOnTeam[team] > 0;
   }
//   public Object writeReplace() throws ObjectStreamException { return null; }
//   public Object readResolve() throws ObjectStreamException {  return null; }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(_combatants, out);
         writeToStream(_combatantsWaitingByID, out);
         writeToStream((byte)(_roomOnTeam.length), out);
         for (byte element : _roomOnTeam) {
            writeToStream(element, out);
         }
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
         byte size = readByte(in);
         _roomOnTeam = new byte[size];
         for (int i=0 ; i<size ; i++) {
            _roomOnTeam[i] = readByte(in);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public ArrayList<Character> getCombatants() {
      ArrayList<Character> list = new ArrayList<>();
      list.addAll(_combatants);
      return list;
   }
   public ArrayList<Character> getCombatantsWaitingToConnect() {
      ArrayList<Character> list = new ArrayList<>();
      for (Character chr : _combatants) {
         if (_combatantsWaitingByID.contains(chr._uniqueID)) {
            list.add(chr);
         }
      }
      return list;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("ServerStatus.");
      return sb.toString();
   }
}
