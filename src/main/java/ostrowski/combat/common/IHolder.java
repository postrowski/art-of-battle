package ostrowski.combat.common;

import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestGrapplingHoldMaintain;
import ostrowski.combat.server.Arena;

public interface IHolder
{
   Byte getHoldingLevel();
   String getName();
   RequestGrapplingHoldMaintain getGrapplingHoldMaintain(Character actor, RequestAction action, Arena arena);
   void applyHoldMaintenance(RequestGrapplingHoldMaintain grappleMaintain, Arena arena);
   Character getHoldTarget();
   void setHoldTarget(Character holdTarget);
   byte getAdjustedStrength();
}
