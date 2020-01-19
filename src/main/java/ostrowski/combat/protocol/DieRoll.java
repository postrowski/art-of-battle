/*
 * Created on May 11, 2006
 *
 */
package ostrowski.combat.protocol;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.graphics.objects3d.Thing;
import ostrowski.protocol.SerializableObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class DieRoll extends SerializableObject implements Enums
{
   boolean _allowExplodes;
   int _characterIdRolling;
   int _rgbDieColor;
   RollType _rollType;
   Map<DieType, List<List<Integer>>> _results = new HashMap<>();

   public DieRoll() {}
   public DieRoll(Character roller, int rgbDieColor, boolean allowExplodes, RollType rollType,
                  Map<DieType, List<List<Integer>>> results)
   {
      _characterIdRolling = roller._uniqueID;
      _rgbDieColor = rgbDieColor;
      _allowExplodes = allowExplodes;
      _rollType = rollType;
      _results = results;
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(_characterIdRolling, out);
         writeToStream(_rgbDieColor, out);
         writeToStream(_allowExplodes, out);
         writeToStream(_rollType.ordinal(), out);
         writeToStream(_results.size(), out);
         for (Map.Entry<DieType, List<List<Integer>>> e : _results.entrySet()) {
            writeToStream(e.getKey().ordinal(), out);
            writeToStream(e.getValue().size(), out);
            for (List<Integer> results : e.getValue()) {
               writeToStream(results, out);
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         _characterIdRolling = readInteger(in);
         _rgbDieColor = readInteger(in);
         _allowExplodes = readBoolean(in);
         _rollType = RollType.values()[readInt(in)];
         _results.clear();
         Integer size = readInteger(in);
         for (int i=0 ; i<size ; i++) {
            Integer dieTypeOrdinal = readInteger(in);
            DieType die = DieType.values()[dieTypeOrdinal];
            Integer dieCount = readInteger(in);
            List<List<Integer>> resultsThisDieType = new ArrayList<>();
            _results.put(die, resultsThisDieType);
            for (int j=0 ; j<dieCount ; j++) {
               ArrayList<Integer> resultsThisDie = new ArrayList<>();
               readIntoListInteger(resultsThisDie, in);
               resultsThisDieType.add(resultsThisDie);
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("DieRoll, characterID ").append(_characterIdRolling);
      if (_allowExplodes) {
         sb.append(" explodable");
      }
      sb.append(" Type:").append(_rollType.toString());
      for (Map.Entry<DieType, List<List<Integer>>> e : _results.entrySet()) {
         sb.append(" DieType ").append(e.getKey()).append(": ");
         boolean firstA = true;
         for (List<Integer> results : e.getValue()) {
            if (!firstA) {
               sb.append(", ");
            }
            firstA = false;
            sb.append("{");
            boolean first = true;
            for (Integer roll: results) {
               if (!first) {
                  sb.append(", ");
               }
               first = false;
               sb.append(roll);
            }
            sb.append("}");
         }
      }
      return sb.toString();
   }
}
