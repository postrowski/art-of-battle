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

   final List<String> _readyEqu = new ArrayList<>();
   final List<String> _applyEqu = new ArrayList<>();
   final ArrayList<LimbType> _hand     = new ArrayList<>();
   RequestAction _parentReq;
   public RequestEquipment() {
   }
   public RequestEquipment(RequestAction parentReq) {
      _parentReq = parentReq;
   }

   public LimbType getLimb() {
      if (_answer instanceof RequestActionOption) {
         RequestActionOption act = (RequestActionOption) _answer;
         return act.getLimbType();
      }
      return null;
   }

   public RequestActionType getActionType() {
      if (_answer instanceof RequestActionOption) {
         RequestActionOption reqActOpt = (RequestActionOption) _answer;
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
      addOption(new RequestActionOption("drop " + equipmentName, RequestActionType.OPT_EQUIP_UNEQUIP_DROP, hand._limbType, true));
      if (actionsAvailable>=2) {
         addOption(new RequestActionOption("sheath " + equipmentName + " (2 actions)",
                                           RequestActionType.OPT_EQUIP_UNEQUIP_SHEATH, hand._limbType, true));
      }
      else {
         addOption(new RequestActionOption("sheath " + equipmentName + " (would require 2 actions)",
                                           RequestActionType.OPT_EQUIP_UNEQUIP_SHEATH, hand._limbType, false));
      }
   }
   public void addReadyOption(String equipmentName, int equipmentIndex, LimbType hand, boolean enabled) {
      String name = "ready " + equipmentName;
      name += " (" + hand.name + ")";
      RequestActionType type = RequestActionType.getEquipUnequipReadyActionByIndex(_readyEqu.size());
      addOption(new RequestActionOption(name, type, hand, enabled));
      _readyEqu.add(equipmentName);
      _hand.add(hand);
   }
   public void addApplyOption(String equipmentName, int equipmentIndex, LimbType hand, boolean enabled) {
      String name = "apply " + equipmentName;
      RequestActionType type = RequestActionType.getEquipUnequipApplyActionByIndex(_applyEqu.size());
      addOption(new RequestActionOption(name, type, hand, enabled));
      _applyEqu.add(equipmentName);
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

   public String getEquToReady() { return (_readyEqu.get(getEquipmentIndex())); }
   public String getEquToApply() { return (_applyEqu.get(getEquipmentIndex())); }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      super.serializeFromStream(in);
      try {
         readIntoListString(_readyEqu, in);
         readIntoListString(_applyEqu, in);
         _hand.clear();
         int count = readInt(in);
         for (int i=0 ; i<count ; i++) {
            byte value = readByte(in);
            _hand.add(LimbType.getByValue(value));
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
         writeToStream(_readyEqu, out);
         writeToStream(_applyEqu, out);
         writeToStream(_hand.size(), out);
         for (LimbType hand : _hand) {
            writeToStream(hand.value, out);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public boolean isApply()  { return ((RequestActionOption)_answer).getValue().isApplyEquip();}
   public boolean isDrop()   { return ((RequestActionOption)_answer).getValue().isDrop();      }
   public boolean isReady()  { return ((RequestActionOption)_answer).getValue().isReadyEquip();}
   public boolean isSheath() { return ((RequestActionOption)_answer).getValue().isSheath();    }

   @Override
   protected String getAllowedKeyStrokesForOption(int optionID) {
      return "1234567890";
   }

}
