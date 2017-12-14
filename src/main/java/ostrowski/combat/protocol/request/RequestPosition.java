package ostrowski.combat.protocol.request;

import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.things.LimbType;
import ostrowski.protocol.SyncRequest;

public class RequestPosition extends SyncRequest implements Enums {

   RequestAction _parentReq;
   public RequestPosition() { }
   public RequestPosition(RequestAction parentReq) {
      _parentReq = parentReq;
   }
   public RequestActionType getActionType() {
      if (_answer instanceof RequestActionOption) {
         RequestActionOption reqActOpt = (RequestActionOption) _answer;
         return reqActOpt.getValue();
      }
      return null;
   }
   public String getPositionName() {
      switch (getActionType()) {
         case OPT_CHANGE_POS_STAND:               return "stand";
         case OPT_CHANGE_POS_KNEEL:               return "kneel";
         case OPT_CHANGE_POS_CROUCH:              return "crouch";
         case OPT_CHANGE_POS_SIT:                 return "sit";
         case OPT_CHANGE_POS_LAYDOWN_BACK:        return "laying on back";
         case OPT_CHANGE_POS_LAYDOWN_FRONT:       return "laying on stomach";
      }
      return null;
  }
   public byte getActionsUsed()
   {
      RequestActionType actType = getActionType();
      if (actType != null) {
         return actType.getActionsUsed((byte) 0);
      }
      return 0;
   }
   public void addPositions(int availActions, Position currentPosition)
   {
      String kneel = ((currentPosition == Position.STANDING) ? "kneel down" : "kneel");
      String sit   = ((currentPosition == Position.STANDING) ? "sit down" : "sit up");

      if ((availActions & ACTION_STAND) != 0) {
         addOption(new RequestActionOption("stand up", RequestActionType.OPT_CHANGE_POS_STAND, LimbType.BODY, true));
      }
      if ((availActions & ACTION_KNEEL) != 0) {
         addOption(new RequestActionOption(kneel, RequestActionType.OPT_CHANGE_POS_KNEEL, LimbType.BODY, true));
      }
      if ((availActions & ACTION_CROUCH) != 0) {
         addOption(new RequestActionOption("crouch", RequestActionType.OPT_CHANGE_POS_CROUCH, LimbType.BODY, true));
      }
      if ((availActions & ACTION_SIT) != 0) {
         addOption(new RequestActionOption(sit, RequestActionType.OPT_CHANGE_POS_SIT, LimbType.BODY, true));
      }
      if ((availActions & ACTION_LAYDOWN_BACK) != 0) {
         addOption(new RequestActionOption("lay on back", RequestActionType.OPT_CHANGE_POS_LAYDOWN_BACK, LimbType.BODY, true));
      }
      if ((availActions & ACTION_LAYDOWN_FRONT) != 0) {
         addOption(new RequestActionOption("lay on stomach", RequestActionType.OPT_CHANGE_POS_LAYDOWN_FRONT, LimbType.BODY, true));
      }
   }
}
