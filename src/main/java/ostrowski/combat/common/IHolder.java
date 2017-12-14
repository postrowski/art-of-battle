package ostrowski.combat.common;

import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestGrapplingHoldMaintain;
import ostrowski.combat.server.Arena;

public interface IHolder
{
   public Byte getHoldingLevel();
   public String getName();
   public RequestGrapplingHoldMaintain getGrapplingHoldMaintain(Character actor, RequestAction action, Arena arena);
   public void applyHoldMaintenance(RequestGrapplingHoldMaintain grappleMaintain, Arena arena);
   public Character getHoldTarget();
   public void setHoldTarget(Character holdTarget);
   public byte getAdjustedStrength();
}
