/*
 * Created on May 11, 2006
 *
 */
package ostrowski.combat.protocol;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.protocol.SerializableObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class DieRoll extends SerializableObject implements Enums
{
   boolean                           allowExplodes;
   int                               characterIdRolling;
   int                               rgbDieColor;
   RollType                          rollType;
   Map<DieType, List<List<Integer>>> results = new HashMap<>();

   public DieRoll() {}
   public DieRoll(Character roller, int rgbDieColor, boolean allowExplodes, RollType rollType,
                  Map<DieType, List<List<Integer>>> results)
   {
      characterIdRolling = roller.uniqueID;
      this.rgbDieColor = rgbDieColor;
      this.allowExplodes = allowExplodes;
      this.rollType = rollType;
      this.results = results;
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(characterIdRolling, out);
         writeToStream(rgbDieColor, out);
         writeToStream(allowExplodes, out);
         writeToStream(rollType.ordinal(), out);
         writeToStream(results.size(), out);
         for (Map.Entry<DieType, List<List<Integer>>> e : results.entrySet()) {
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
         characterIdRolling = readInteger(in);
         rgbDieColor = readInteger(in);
         allowExplodes = readBoolean(in);
         rollType = RollType.values()[readInt(in)];
         results.clear();
         Integer size = readInteger(in);
         for (int i=0 ; i<size ; i++) {
            Integer dieTypeOrdinal = readInteger(in);
            DieType die = DieType.values()[dieTypeOrdinal];
            Integer dieCount = readInteger(in);
            List<List<Integer>> resultsThisDieType = new ArrayList<>();
            results.put(die, resultsThisDieType);
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
      sb.append("DieRoll, characterID ").append(characterIdRolling);
      if (allowExplodes) {
         sb.append(" explodable");
      }
      sb.append(" Type:").append(rollType.toString());
      for (Map.Entry<DieType, List<List<Integer>>> e : results.entrySet()) {
         List<List<Integer>> listList = e.getValue();
         if (listList == null || listList.isEmpty()) {
            continue;
         }
         sb.append(" DieType ").append(e.getKey()).append(": ");
         boolean firstA = true;
         for (List<Integer> results : listList) {
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
