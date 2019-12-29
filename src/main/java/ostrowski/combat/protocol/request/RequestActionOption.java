package ostrowski.combat.protocol.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ostrowski.combat.common.things.LimbType;
import ostrowski.protocol.IRequestOption;
import ostrowski.protocol.SerializableObject;
import ostrowski.protocol.SyncRequest;

public class RequestActionOption extends SerializableObject implements IRequestOption
{
   private String            _name;
   private RequestActionType _actionType;
   private LimbType          _limbType;
   private boolean           _enabled;

   public static final int LIMB_MULTIPLIER = 200;

   public RequestActionOption() {}
   public RequestActionOption(String name, RequestActionType actionType, LimbType limbType, boolean enabled) {
      this._name       = name;
      this._actionType = actionType;
      this._limbType   = limbType;
      this._enabled    = enabled;
   }

   @Override
   public String getName() {
      return this._name;
   }

   @Override
   public int getIntValue() {
      if (this._actionType == RequestActionType.OPT_CANCEL_ACTION) {
         return SyncRequest.OPT_CANCEL_ACTION;
      }
      return this._actionType.ordinal() + (this._limbType.ordinal() * LIMB_MULTIPLIER);
   }

   public RequestActionType getValue() {
      return this._actionType;
   }

   public LimbType getLimbType() {
      return this._limbType;
   }

   @Override
   public boolean isEnabled() {
      return this._enabled;
   }

   @Override
   public void setAnswerStr(String name) {
      this._name = name;
   }

   @Override
   public void setAnswerID(int value) {
      this._actionType = RequestActionType.lookupByOrdinal(value % LIMB_MULTIPLIER);
      this._limbType = LimbType.getByValue((byte)(value / LIMB_MULTIPLIER));
   }

   @Override
   public void setEnabled(boolean enabled) {
      this._enabled = enabled;
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         writeToStream(_name, out);
         writeToStream(_actionType.ordinal(), out);
         writeToStream(_limbType.ordinal(), out);
         writeToStream(_enabled, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         _name       =                                   readString(in);
         _actionType = RequestActionType.lookupByOrdinal(readInt(in));
         _limbType   =         LimbType.getByValue((byte)readInt(in));
         _enabled    =                                   readBoolean(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public String toString() {
      return _name + ": " + _actionType + " - " + _limbType.name;
   }
}
