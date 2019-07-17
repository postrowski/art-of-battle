/*
 * Created on Jun 14, 2006
 *
 */
package ostrowski.combat.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Enums;
import ostrowski.protocol.SerializableObject;

public class TargetPriorities extends SerializableObject implements Enums
{
   ArrayList<Integer> _orderedEnemyIDs = new ArrayList<>();
   public TargetPriorities() {}
   public TargetPriorities(List<Character> enemyCharacters) {
      setEnemies(enemyCharacters);
   }

   public void setEnemies(List<Character> enemyCharacters) {
      _orderedEnemyIDs.clear();
      for (Character enemy : enemyCharacters) {
         _orderedEnemyIDs.add(Integer.valueOf(enemy._uniqueID));
      }
   }
   public ArrayList<Integer> getOrderedEnemyIdsList() {
      return _orderedEnemyIDs;
   }
   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(_orderedEnemyIDs, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         readIntoListInteger(_orderedEnemyIDs, in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

}
