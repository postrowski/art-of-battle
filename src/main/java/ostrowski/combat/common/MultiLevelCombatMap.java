package ostrowski.combat.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import ostrowski.protocol.SerializableObject;

public class MultiLevelCombatMap extends SerializableObject implements Cloneable {

   List<CombatMap> _levels = new ArrayList<>();

   @Override
   public void serializeToStream(DataOutputStream out) {
      // TODO Auto-generated method stub

   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      // TODO Auto-generated method stub

   }

}
