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
   private String            name;
   private RequestActionType actionType;
   private LimbType          limbType;
   private boolean           enabled;

   public static final int LIMB_MULTIPLIER = 200;

   public RequestActionOption() {}
   public RequestActionOption(String name, RequestActionType actionType, LimbType limbType, boolean enabled) {
      this.name = name;
      this.actionType = actionType;
      this.limbType = limbType;
      this.enabled = enabled;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public int getIntValue() {
      if (this.actionType == RequestActionType.OPT_CANCEL_ACTION) {
         return SyncRequest.OPT_CANCEL_ACTION;
      }
      return this.actionType.ordinal() + (this.limbType.ordinal() * LIMB_MULTIPLIER);
   }

   public RequestActionType getValue() {
      return this.actionType;
   }

   public LimbType getLimbType() {
      return this.limbType;
   }

   @Override
   public boolean isEnabled() {
      return this.enabled;
   }

   @Override
   public void setAnswerStr(String name) {
      this.name = name;
   }

   @Override
   public void setAnswerID(int value) {
      this.actionType = RequestActionType.lookupByOrdinal(value % LIMB_MULTIPLIER);
      this.limbType = LimbType.getByValue((byte)(value / LIMB_MULTIPLIER));
   }

   @Override
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         writeToStream(name, out);
         writeToStream(actionType.ordinal(), out);
         writeToStream(limbType.ordinal(), out);
         writeToStream(enabled, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         name =                                   readString(in);
         actionType = RequestActionType.lookupByOrdinal(readInt(in));
         limbType =         LimbType.getByValue((byte)readInt(in));
         enabled =                                   readBoolean(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public String toString() {
      return name + ": " + actionType + " - " + limbType.name;
   }
}
