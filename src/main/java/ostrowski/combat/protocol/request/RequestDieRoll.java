/*
 * Created on May 16, 2006
 *
 */
package ostrowski.combat.protocol.request;

import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.things.LimbType;
import ostrowski.protocol.SyncRequest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RequestDieRoll extends SyncRequest
{
   private DiceSet _diceSet;
   private Enums.RollType _rollType;

   public RequestDieRoll(String messageToRoller, DiceSet diceSet, Enums.RollType rollType) {
      _message = messageToRoller;
      this._diceSet = diceSet;
      this._rollType = rollType;
      addOption(new RequestActionOption("Roll " + _diceSet.toString(),
                                        RequestActionType.OPT_NO_ACTION, LimbType.BODY, true));
   }
   public RequestDieRoll() {
      addOption(new RequestActionOption("Roll ", RequestActionType.OPT_NO_ACTION, LimbType.BODY, true));
   }

   @Override
   protected String getAllowedKeyStrokesForOption(int optionID) {
      return "\n";
   }
   @Override
   public void serializeFromStream(DataInputStream in) {
      super.serializeFromStream(in);
      try {
         _diceSet          = new DiceSet(readString(in));
         byte rollType     = readByte(in);
         for (Enums.RollType rt : Enums.RollType.values()) {
            if (rt.ordinal() == rollType) {
               _rollType = rt;
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out) {
      super.serializeToStream(out);
      try {
         writeToStream(_diceSet.toString(), out);
         writeToStream((byte)(_rollType.ordinal()), out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
