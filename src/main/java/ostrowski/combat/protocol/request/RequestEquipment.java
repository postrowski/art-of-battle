package ostrowski.combat.protocol.request;

import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.things.Hand;
import ostrowski.combat.common.things.LimbType;
import ostrowski.protocol.SyncRequest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RequestEquipment extends SyncRequest implements Enums {

   final List<String>   readyEqu = new ArrayList<>();
   final List<String>   applyEqu = new ArrayList<>();
   final List<LimbType> hand     = new ArrayList<>();
   public RequestEquipment() {
   }
   public RequestEquipment(RequestAction parentReq) {
   }

   public LimbType getLimb() {
      if (answer instanceof RequestActionOption) {
         RequestActionOption act = (RequestActionOption) answer;
         return act.getLimbType();
      }
      return null;
   }

   public RequestActionType getActionType() {
      if (answer instanceof RequestActionOption) {
         RequestActionOption reqActOpt = (RequestActionOption) answer;
         return reqActOpt.getValue();
      }
      return null;
   }

   public int getEquipmentIndex() {
      RequestActionType actType = getActionType();
      if (actType != null) {
         if (actType.isReadyEquip()) {
            return actType.getIndexOfReadyEquipActions();
         }
         if (actType.isApplyEquip()) {
            return actType.getIndexOfApplyEquipActions();
         }
      }
      return -1;
   }
   public void addDropSheathOptions(Hand hand, byte actionsAvailable) {
      String equipmentName = hand.getHeldThingName();
      addOption(new RequestActionOption("drop " + equipmentName, RequestActionType.OPT_EQUIP_UNEQUIP_DROP, hand.limbType, true));
      if (actionsAvailable>=2) {
         addOption(new RequestActionOption("sheath " + equipmentName + " (2 actions)",
                                           RequestActionType.OPT_EQUIP_UNEQUIP_SHEATH, hand.limbType, true));
      }
      else {
         addOption(new RequestActionOption("sheath " + equipmentName + " (would require 2 actions)",
                                           RequestActionType.OPT_EQUIP_UNEQUIP_SHEATH, hand.limbType, false));
      }
   }
   public void addReadyOption(String equipmentName, int equipmentIndex, LimbType hand, boolean enabled) {
      String name = "ready " + equipmentName;
      name += " (" + hand.name + ")";
      RequestActionType type = RequestActionType.getEquipUnequipReadyActionByIndex(readyEqu.size());
      addOption(new RequestActionOption(name, type, hand, enabled));
      readyEqu.add(equipmentName);
      this.hand.add(hand);
   }
   public void addApplyOption(String equipmentName, int equipmentIndex, LimbType hand, boolean enabled) {
      String name = "apply " + equipmentName;
      RequestActionType type = RequestActionType.getEquipUnequipApplyActionByIndex(applyEqu.size());
      addOption(new RequestActionOption(name, type, hand, enabled));
      applyEqu.add(equipmentName);
   }
   @Override
   public void addOption(int optionID, String optionStr, boolean enabled) {
      if (optionID == OPT_CANCEL_ACTION) {
         super.addOption(new RequestActionOption("Cancel 'ready equipment' action",
                                                 RequestActionType.OPT_CANCEL_ACTION, LimbType.BODY, enabled));
         //super.addOption(OPT_CANCEL_ACTION, "Cancel 'ready equipment' action", enabled);
         return;
      }
      throw new IllegalArgumentException();
   }
   public byte getActionsUsed()
   {
      RequestActionType actType = getActionType();
      if (actType != null) {
         actType.getActionsUsed((byte)0);
      }
      return 0;
   }

   public String getEquToReady() { return (readyEqu.get(getEquipmentIndex())); }
   public String getEquToApply() { return (applyEqu.get(getEquipmentIndex())); }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      super.serializeFromStream(in);
      try {
         readIntoListString(readyEqu, in);
         readIntoListString(applyEqu, in);
         hand.clear();
         int count = readInt(in);
         for (int i=0 ; i<count ; i++) {
            byte value = readByte(in);
            hand.add(LimbType.getByValue(value));
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out)
   {
      super.serializeToStream(out);
      try {
         writeToStream(readyEqu, out);
         writeToStream(applyEqu, out);
         writeToStream(hand.size(), out);
         for (LimbType hand : hand) {
            writeToStream(hand.value, out);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public boolean isApply()  { return answer != null && ((RequestActionOption) answer).getValue().isApplyEquip();}
   public boolean isDrop()   { return answer != null && ((RequestActionOption) answer).getValue().isDrop();      }
   public boolean isReady()  { return answer != null && ((RequestActionOption) answer).getValue().isReadyEquip();}
   public boolean isSheath() { return answer != null && ((RequestActionOption) answer).getValue().isSheath();    }

   @Override
   protected String getAllowedKeyStrokesForOption(int optionID) {
      return "1234567890";
   }

}
