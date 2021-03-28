/*
 * Created on Jun 14, 2006
 *
 */
package ostrowski.combat.protocol;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Enums;
import ostrowski.protocol.SerializableObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TargetPriorities extends SerializableObject implements Enums
{
   final List<Integer> orderedEnemyIDs = new ArrayList<>();
   public TargetPriorities() {}
   public TargetPriorities(List<Character> enemyCharacters) {
      setEnemies(enemyCharacters);
   }

   public void setEnemies(List<Character> enemyCharacters) {
      orderedEnemyIDs.clear();
      for (Character enemy : enemyCharacters) {
         orderedEnemyIDs.add(enemy.uniqueID);
      }
   }
   public List<Integer> getOrderedEnemyIdsList() {
      return orderedEnemyIDs;
   }
   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(orderedEnemyIDs, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         readIntoListInteger(orderedEnemyIDs, in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

}
