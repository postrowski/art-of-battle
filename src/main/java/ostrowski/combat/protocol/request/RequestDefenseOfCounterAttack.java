/*
 * Created on May 16, 2006
 */
package ostrowski.combat.protocol.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Enums;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.SemaphoreAutoLocker;

public class RequestDefenseOfCounterAttack extends RequestDefense implements Enums
{
   @Override
   public synchronized void copyDataInto(SyncRequest newObj)
   {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         super.copyDataInto(newObj);
//         if (newObj instanceof RequestDefenseOfCounterAttack) {
//            RequestDefenseOfCounterAttack reqDef = (RequestDefenseOfCounterAttack) newObj;
//         }
      }
   }

   public RequestDefenseOfCounterAttack() {
      // default c'tor used by factor method to serialize in from a stream.
   }
   public RequestDefenseOfCounterAttack(Character attacker, RequestAction attack, RANGE range) {
      super(attacker, attack, range);
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      super.serializeFromStream(in);
   }
   @Override
   public void serializeToStream(DataOutputStream out) {
      super.serializeToStream(out);
   }
}
