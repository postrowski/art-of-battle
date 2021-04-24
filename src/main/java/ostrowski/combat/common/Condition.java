package ostrowski.combat.common;

import org.w3c.dom.*;
import ostrowski.DebugBreak;
import ostrowski.combat.common.enums.*;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestActionType;
import ostrowski.combat.protocol.request.RequestDefense;
import ostrowski.combat.protocol.request.RequestGrapplingHoldMaintain;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.CombatServer;
import ostrowski.protocol.SerializableObject;
import ostrowski.util.Diagnostics;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/*
 * Created on May 3, 2006
 *
 */

/**
 * @author Paul
 *
 */
public class Condition extends SerializableObject implements Enums {
   private byte          actionsAvailable               = 0;
   private byte          finalDefensiveActionsAvailable = 0;
   private byte          maxActionsPerRound             = 3;
   final   StringBuilder actionsAvailAudit              = new StringBuilder();
   // wounds:
   private byte          wounds                         = 0;
   private byte          bleedRate                      = 0;
   private byte          penaltyPain                    = 0;

   // penalties are positive integers so a 2 means a -2 to actions.
   // A negative value means no use possible, such as a severed limb.
   private byte penaltyMove                = 0;
   private byte actionsSpentThisRound      = 0;
   private byte initiative                 = 0;
   private byte movementAvailableThisRound = 0;
   private byte movementAvailableEachRound = 0;

   private boolean        hasMovedThisRound            = false;
   private boolean        attackedThisRound            = false;
   private boolean        moveComplete                 = false;
   private boolean        movingEvasively              = false;
   private boolean        movedLastAction              = false;
   private DefenseOptions defenseOptionsTakenThisRound = new DefenseOptions();

   private Orientation orientation = null;
   private boolean     collapsed   = false;
   private boolean     isConscious = true;
   private boolean     isAlive     = true;

   private short priestSpellPointsMax       = 0;
   private short mageSpellPointsMax         = 0;
   private short priestSpellPointsAvailable = 0;
   private short mageSpellPointsAvailable   = 0;

   private List<Wound> woundsList = new ArrayList<>();

   public Condition() {
      // ctor used for all Serializable objects
   }
   public Condition(Orientation orientation) {
      this.orientation = orientation.clone();
   }
   public Condition(Character character) {
      setOrientation(character.getRace().getBaseOrientation());
   }
   public byte getActionsAvailable(boolean usedForDefenseOnly) {
      if (usedForDefenseOnly && (finalDefensiveActionsAvailable != 0)) {
         return finalDefensiveActionsAvailable;
      }
      if (actionsAvailable < 0) {
         return 0;
      }
      return actionsAvailable;
   }
   public byte getActionsAvailableThisRound(boolean usedForDefenseOnly) {
      if (usedForDefenseOnly && (finalDefensiveActionsAvailable != 0)) {
         return finalDefensiveActionsAvailable;
      }
      return (byte) Math.min(maxActionsPerRound - actionsSpentThisRound, actionsAvailable);
   }
   public byte getActionsSpentThisRound() {
      return actionsSpentThisRound;
   }
   public byte getPenaltyPain()                         { return penaltyPain; }
   public byte getWoundsAndPainPenalty()                { return (byte) (penaltyPain + wounds); }
   public byte getWounds()                              { return wounds; }
   public byte getBleedRate()                           { return bleedRate; }
   public byte getPenaltyRetreat  (boolean includeWounds)  {
      if (penaltyMove < 0) {
         return penaltyMove;
      }
      return (byte) (penaltyMove + (includeWounds ? wounds : 0));
   }
   public byte getPenaltyMove()                          { return penaltyMove; }
   public byte getInitiative()                           { return initiative; }
   public Position getPosition()                         { return orientation.getPosition(); }
   public boolean isCollapsed()                          { return collapsed; }
   public boolean isConscious()                          { return isConscious; }
   public boolean isAlive()                              { return isAlive; }
   public boolean isStanding()                           { return (orientation.getPosition() == Position.STANDING); }
   public void setMovingEvasively(boolean movingEvasively) { this.movingEvasively = movingEvasively; }
   public boolean isMovingEvasively() { return movingEvasively;}
   public boolean hasMovedLastAction() { return movedLastAction; }
   public byte getMovementAvailableThisRound(boolean movingEvasively) {
      if (!isAlive || !isConscious || collapsed) {
         return 0;
      }

      if (movingEvasively != this.movingEvasively) {
         this.movingEvasively = movingEvasively;
         if (movingEvasively) {
            // Divide our movement allowance by two, rounding up
            movementAvailableThisRound = (byte) ((movementAvailableThisRound + 1) / 2);
         }
      }
      return movementAvailableThisRound;
   }
   public void raiseFromDead(boolean asZombie) {
      isAlive = true;
      isConscious = true;
      collapsed = false;
   }
   public void applyMovementCost(byte movementCost) {
      movementAvailableThisRound -= movementCost;
      if (movementCost > 0) {
         hasMovedThisRound = true;
      }
      if (movementAvailableThisRound <= 0) {
         movementAvailableThisRound = 0;
         moveComplete = true;
      }
   }

   public short getPriestSpellPointsAvailable() {
      return priestSpellPointsAvailable;
   }
   public void setPriestSpellPointsAvailable(short priestSpellPointsAvailable) {
      this.priestSpellPointsAvailable = priestSpellPointsAvailable;
   }

   public short getMageSpellPointsAvailable() {
      return mageSpellPointsAvailable;
   }
   public void setMageSpellPointsAvailable(short mageSpellPointsAvailable) {
      this.mageSpellPointsAvailable = mageSpellPointsAvailable;
   }


   public boolean hasAttackedThisRound() {
       return attackedThisRound;
   }

   public boolean canAttack() {
      if (collapsed) {
         return false;
      }
      if (!isConscious) {
         return false;
      }
      if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) <= 0) {
         return false;
      }
      if (attackedThisRound) {
         return false;
      }
      return getPositionAdjustmentForAttack() != -99;
   }
   private boolean canRetreat() {
      if (collapsed) {
         return false;
      }
      if (!isConscious) {
         return false;
      }
      if (getActionsAvailableThisRound(true/*usedForDefenseOnly*/) <= 0) {
         return false;
      }
      if (attackedThisRound) {
         return false;
      }
      if (penaltyMove < 0) {
         return false;
      }
      return (movementAvailableThisRound >= 2) || !hasMovedThisRound;
   }
   public boolean canAdvance() {
      if (!isConscious) {
         return false;
      }
      if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) <= 0) {
         return false;
      }
      if (penaltyMove < 0) {
         return false;
      }
      return movementAvailableThisRound >= 1;
   }
   public void adjustActions(byte actionsThisRound, byte actionsPerRound) {
      boolean hasActionsThisRound = (actionsAvailable > 0);
      actionsAvailable += actionsThisRound;
      maxActionsPerRound += actionsPerRound;

      if ((actionsAvailable <= 0) && hasActionsThisRound) {
         actionsAvailable = 1;
      }
      if (maxActionsPerRound <= 0) {
         maxActionsPerRound = 1;
      }
   }

   public void setInitiative(byte initiative) {
      this.initiative = initiative;
   }
   public void reducePain(byte painReduction, byte toughnessLevel) {
      penaltyPain -= painReduction;
      if (penaltyPain < 0) {
         penaltyPain = 0;
      }
      if (penaltyPain < Rules.getCollapsePainLevel(toughnessLevel)) {
         collapsed = false;
      }
   }
   public void initializeActionsAndMovementForNewTurn(byte actionsAvailable, byte maxActionsPerRound, byte movementAllowance) {
      this.actionsAvailable = actionsAvailable;
      finalDefensiveActionsAvailable = 0;
      this.maxActionsPerRound = maxActionsPerRound;
      actionsAvailAudit.setLength(0);
      actionsAvailAudit.append(" New turn, ").append(this.actionsAvailable).append(" actions available.");

      movementAvailableEachRound = movementAllowance;
      if ((penaltyMove > 0) && (movementAvailableThisRound > 0)) {
         movementAvailableEachRound = (byte) Math.max(1, movementAvailableEachRound - penaltyMove);
      }
      movementAvailableThisRound = movementAvailableEachRound;
   }

   // return 'true' if any actions remains to be spent
   public boolean endRound() {
      if ((actionsSpentThisRound == 0) && (actionsAvailable > 0)) {
         actionsAvailAudit.append(" no action/end of round(1 action).");
         actionsAvailable--;
      }
      actionsSpentThisRound = 0;
      attackedThisRound = false;
      hasMovedThisRound = false;
      moveComplete = false;
      defenseOptionsTakenThisRound.clear();
      movementAvailableThisRound = movementAvailableEachRound;

      return (actionsAvailable > 0);
   }
   public boolean isMoveComplete()   {
      return moveComplete;
   }
   public void setMoveComplete() {
      moveComplete = true;
   }

   public boolean hasMovedThisRound() {
      return hasMovedThisRound;
   }
   public int getAvailableActions(StringBuilder sbReasons, boolean isBeingHeld) {
      int actionsAllowed = 0;
      if (isConscious && isAlive) {
         if (getActionsAvailableThisRound(true/*usedForDefenseOnly*/) > 0) {
            if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) > 0) {
               if (!collapsed) {
                  if (canAttack()) {
                     actionsAllowed |= ACTION_ATTACK;
                  }
                  if ((movementAvailableThisRound > 0) && !isBeingHeld) {
                     actionsAllowed |= ACTION_MOVE;
                     actionsAllowed |= getAvailablePositions();
                  }
               }
            }
            actionsAllowed |= ACTION_DEFEND;
         }
      }
      return actionsAllowed;
   }
   public int getAvailablePositions() {
      if ((!isConscious) || collapsed) {
         return 0;
      }
      if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) < 1) {
         return 0;
      }
      int availPos = orientation.getAvailablePositions();

      if (!canStand()) {
         // don't allow the player to stand or crouch
         availPos &= ~ACTION_CROUCH;
         availPos &= ~ACTION_STAND;
      }
      return availPos;
   }
   public DefenseOptions getAvailableDefenseOptions() {
      DefenseOptions actionsAllowed = new DefenseOptions();
      if (isConscious) {
//         // DODGE is allowed, as long as no action has been spent yet this round.
//         if ((getActionsSpentThisRound() == 0) || (getActionsAvailableThisRound() > 0)) {
//            actionsAllowed.add(DefenseOption.DEF_DODGE);
//         }
         if (getActionsAvailableThisRound(true/*usedForDefenseOnly*/) >= 1) {
            // If we can't move, we can't dodge or retreat
            if (getPenaltyMove() >= 0) {
               actionsAllowed.add(DefenseOption.DEF_DODGE);
               if (canRetreat()) {
                  actionsAllowed.add(DefenseOption.DEF_RETREAT);
               }
            }
         }
      }
      return actionsAllowed;
   }

   public void setPosition(Position newPosition, CombatMap map, Character actor) {
      Collection<ArenaCoordinates> coordinatesToRedraw = new ArrayList<>(getCoordinates());
      orientation.setPosition(newPosition, map, actor);
      coordinatesToRedraw.addAll(getCoordinates());
      if (CombatServer._this != null) {
         CombatServer._this.redrawMap(coordinatesToRedraw );
      }
   }
   public void applyWound(Wound wound, Arena arena, Character character) {

      boolean wasConscious = isConscious && isAlive;
      if (wound.isKnockOut()) {
         isConscious = false;
      }
      if (wound.isFatal()) {
         isAlive = false;
      }

      if (!character.hasAdvantage(Advantage.NO_PAIN)) {
         penaltyPain += wound.getPain();
      }
      wounds += wound.getEffectiveWounds();
      bleedRate += wound.getEffectiveBleedRate();

      if ((wound.getWounds() != 0) ||
          (wound.getBleedRate() != 0) ||
          (wound.getPenaltyLimb() != 0) ||
          (wound.getPenaltyMove() != 0)    ) {
         woundsList.add(wound);
      }

      if (wounds >= Rules.getUnconsciousWoundLevel(character.getAttributeLevel(Attribute.Toughness))) {
         isConscious = false;
      }
      if (wasConscious && (!isConscious || !isAlive)) {
         character.dropAllEquipment(arena);
         character.releaseHold();
         setPosition(Position.PRONE_BACK, arena.getCombatMap(), character);
      }
      else if (wound.isKnockDown()) {
         setPosition(Position.PRONE_BACK, arena.getCombatMap(), character);
      }

      // If we are not crippled on a limb, all wounding is additive.
      // If we are crippled, don't adjust the particular penalty.
      if (penaltyMove >= 0) {
         penaltyMove += wound.getPenaltyMove();
      }
      // Check for crippling injuries
      if (wound.getPenaltyMove()     < 0) {
         penaltyMove = wound.getPenaltyMove();
      }
   }

   static final HashMap<RequestActionType, Position> mapActionPosToPosition = new HashMap<>();
   static {
      mapActionPosToPosition.put(RequestActionType.OPT_CHANGE_POS_STAND,         Position.STANDING);
      mapActionPosToPosition.put(RequestActionType.OPT_CHANGE_POS_KNEEL,         Position.KNEELING);
      mapActionPosToPosition.put(RequestActionType.OPT_CHANGE_POS_CROUCH,        Position.CROUCHING);
      mapActionPosToPosition.put(RequestActionType.OPT_CHANGE_POS_SIT,           Position.SITTING);
      mapActionPosToPosition.put(RequestActionType.OPT_CHANGE_POS_LAYDOWN_BACK,  Position.PRONE_BACK);
      mapActionPosToPosition.put(RequestActionType.OPT_CHANGE_POS_LAYDOWN_FRONT, Position.PRONE_FRONT);
   }
   public static Position mapActionPosToPosition(RequestActionType actionPos) {
      Position results = mapActionPosToPosition.get(actionPos);
      if (results == null) {
         DebugBreak.debugBreak();
         return null;
      }
      return results;
   }
   // Call this once for each action spent.
   public void applyAction(RequestAction action, Character actor, Spell currentSpell, Arena arena) {
      actionsAvailAudit.append("action, answerID=").append(action.getAnswerID());
      actionsAvailAudit.append("(").append(action.getActionsUsed()).append(" actions)");
      actionsAvailable -= action.getActionsUsed();
      if (actionsAvailable < 0) {
         DebugBreak.debugBreak(actor.getName() + " has " + actionsAvailable + " actions to use. Full audit (inclusive)=" + actionsAvailAudit);
         actionsAvailable = 0;
      }
//      if (action.isChangePosition()) {
//         // change position actions always report they require 2 actions, when they may not need that much
//         byte actionsUsed = action.getActionsUsed();
//         if (actionsUsed > actionsNeededToChangePosition)
//            actionsUsed = actionsNeededToChangePosition;
//         actionsSpentThisRound += actionsUsed;
//      }
//      else
      {
         actionsSpentThisRound += action.getActionsUsed();
      }
      if (action.isChannelEnergy()) {
         mageSpellPointsAvailable -= action.getActionsUsed();
         if (mageSpellPointsAvailable < 0) {
            DebugBreak.debugBreak();
         }
      }
      if (action.isCompletePriestSpell()) {
         if (!currentSpell.isInnate()) {
            priestSpellPointsAvailable -= currentSpell.getSpellPoints();
            if (priestSpellPointsAvailable < 0) {
               DebugBreak.debugBreak();
            }
         }
      }
      if (action.isAttack()) {
         attackedThisRound = true;
      }
      movingEvasively = (action.isAdvance() && action.isEvasiveMove());
      movedLastAction = action.isAdvance();

      if (action.isFinalDefense()) {
         finalDefensiveActionsAvailable += action.getActionsUsed();
      }
      if (action.isChangePosition()) {
         setPosition(mapActionPosToPosition(action.positionRequest.getActionType()), arena.getCombatMap(), actor);
//         // Are we in the middle of changing position?
//         if ((actionNeededToChangePosition > 0) && (movingToPosition != position)) {
//            // allow return to previous position with one action
//            if (position == mapActionPosToPosition(action.positionRequest.getAnswerID()))
//            {
//               actionsNeededToChangePosition = 0;
//               movingToPosition = position;
//            }
//            // allow continue move to next position
//            else if (movingToPosition == mapActionPosToPosition(action.positionRequest.getAnswerID()))
//            {
//               actionsNeededToChangePosition -= action.getActionsUsed();
//               if (actionsNeededToChangePosition <= 0) {
//                  // add back any excess actions spent
//                  actionsAvailable -= actionsNeededToChangePosition;
//                  setPosition(movingToPosition);
//               }
//            }
//         }
//         else {
//            // Allow movement to next position
//            movingToPosition = mapActionPosToPosition(action.positionRequest.getAnswerID());
//            actionsNeededToChangePosition = (byte) (Rules.getActionsToChangePosition() - action.getActionsUsed());
//            if (actionsNeededToChangePosition <= 0) {
//               setPosition(movingToPosition);
//            }
//         }
      }
   }
   public void applyHoldMaintenance(RequestGrapplingHoldMaintain holdMaintainenance) {
      actionsAvailAudit.append("hold maintain, answerID=").append(holdMaintainenance.getAnswerID());
      actionsAvailAudit.append("(").append(holdMaintainenance.getActionsUsed()).append(" actions)");
      if (finalDefensiveActionsAvailable > 0) {
         finalDefensiveActionsAvailable -= holdMaintainenance.getActionsUsed();
         if (finalDefensiveActionsAvailable < 0) {
            finalDefensiveActionsAvailable = 0;
         }
      }
      else {
         actionsAvailable -= holdMaintainenance.getActionsUsed();
         if (actionsAvailable < 0) {
            actionsAvailable = 0;
            DebugBreak.debugBreak();
         }
         actionsSpentThisRound += holdMaintainenance.getActionsUsed();
      }
   }

   public void applyDefense(RequestDefense defense) {
      actionsAvailAudit.append("defense, answerID=").append(defense.getAnswerID());
      DefenseOptions defUsed = new DefenseOptions(defense.getAnswerID());
      byte actionsUsed = defense.getActionsUsed();
      actionsAvailAudit.append("(").append(actionsUsed).append(" actions)");
      if (finalDefensiveActionsAvailable > 0) {
         finalDefensiveActionsAvailable -= actionsUsed;
         if (finalDefensiveActionsAvailable < 0) {
            finalDefensiveActionsAvailable = 0;
         }
      }
      else {
         actionsAvailable -= actionsUsed;
         if (actionsAvailable < 0) {
            actionsAvailable = 0;
            DebugBreak.debugBreak("applyDefense: can't use " + actionsUsed +
                                  " actions for " + defense.getAnswer() + " (ID=" + defense.getAnswerID() + ")" +
                                  ", finalDefensiveActionsAvailable=" + finalDefensiveActionsAvailable +
                                  "\nactionsAvailAudit = " + actionsAvailAudit);
         }
         actionsSpentThisRound += actionsUsed;
      }
      defenseOptionsTakenThisRound.add(defUsed);
      int magicPointsUsed = defUsed.getDefenseMagicPointsUsed();
      // TODO: how to we deal with mage-priests?
      if (magicPointsUsed > 0) {
         if (mageSpellPointsAvailable >= magicPointsUsed) {
            mageSpellPointsAvailable -= magicPointsUsed;
         }
         else if (priestSpellPointsAvailable >= magicPointsUsed) {
            priestSpellPointsAvailable -= magicPointsUsed;
         }
         else {
            // neither one can handle the points needed.
            DebugBreak.debugBreak("applyDefense: can't use " + actionsUsed +
                                  " actions for " + defense.getAnswer() + " (ID=" + defense.getAnswerID() + ")" +
                                  ", magicPointsUsed=" + magicPointsUsed +
                                  "mageSpellPointsAvailable = " + mageSpellPointsAvailable +
                                  "priestSpellPointsAvailable = " + priestSpellPointsAvailable);
            mageSpellPointsAvailable = 0;
            priestSpellPointsAvailable = 0;
         }
      }
   }
   private static final byte SERIAL_BIT_AttackedThisRound   = 1<<0;
   private static final byte SERIAL_BIT_MoveComplete        = 1<<1;
   private static final byte SERIAL_BIT_MovingEvasively     = 1<<2;
   private static final byte SERIAL_BIT_Collapsed           = 1<<3;
   private static final byte SERIAL_BIT_IsConscious         = 1<<4;
   private static final byte SERIAL_BIT_IsAlive             = 1<<5;

//   public byte[] getSeializeBuffer()
//   {
//      byte bitFields = 0;
//      if (attackedThisRound)  bitFields |= SERIAL_BIT_AttackedThisRound;
//      if (moveComplete)       bitFields |= SERIAL_BIT_MoveComplete;
//      if (movingEvasively)    bitFields |= SERIAL_BIT_MovingEvasively;
//      if (collapsed)          bitFields |= SERIAL_BIT_Collapsed;
//      if (isConscious)        bitFields |= SERIAL_BIT_IsConscious;
//      if (isAlive)            bitFields |= SERIAL_BIT_IsAlive;
//      byte[] buf = new byte[] {   position,
//                                  actionsAvailable,
//                                  actionsSpentThisRound,
//                                  actionsNeededToChangePosition,
//                                  wounds,
//                                  bleedRate,
//                                  penaltyPain,
//                                  penaltyMove,
//                                  movingToPosition,
//                                  initiative,
//                                  movementAvailableThisRound,
//                                  movementAvailableEachRound,
//                                  bitFields
//      };
//      return buf;
//   }
   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         writeToStream(actionsAvailable, out);
         writeToStream(actionsSpentThisRound, out);
         writeToStream(wounds, out);
         writeToStream(bleedRate, out);
         writeToStream(penaltyPain, out);
         writeToStream(penaltyMove, out);
         writeToStream(initiative, out);
         writeToStream(movementAvailableThisRound, out);
         writeToStream(movementAvailableEachRound, out);
         byte bitFields = 0;
         if (attackedThisRound) {
            bitFields |= SERIAL_BIT_AttackedThisRound;
         }
         if (moveComplete) {
            bitFields |= SERIAL_BIT_MoveComplete;
         }
         if (movingEvasively) {
            bitFields |= SERIAL_BIT_MovingEvasively;
         }
         if (collapsed) {
            bitFields |= SERIAL_BIT_Collapsed;
         }
         if (isConscious) {
            bitFields |= SERIAL_BIT_IsConscious;
         }
         if (isAlive) {
            bitFields |= SERIAL_BIT_IsAlive;
         }
         writeToStream(bitFields, out);
         writeToStream(defenseOptionsTakenThisRound.getIntValue(), out);
         writeToStream(mageSpellPointsAvailable, out);
         writeToStream(mageSpellPointsMax, out);
         writeToStream(priestSpellPointsAvailable, out);
         writeToStream(priestSpellPointsMax, out);
         orientation.serializeToStream(out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         actionsAvailable = in.readByte();
         actionsSpentThisRound = in.readByte();
         wounds = in.readByte();
         bleedRate = in.readByte();
         penaltyPain = in.readByte();
         penaltyMove = in.readByte();
         initiative = in.readByte();
         movementAvailableThisRound = in.readByte();
         movementAvailableEachRound = in.readByte();
         byte bitFields               = in.readByte();
         attackedThisRound = (bitFields & SERIAL_BIT_AttackedThisRound) != 0;
         moveComplete = (bitFields & SERIAL_BIT_MoveComplete) != 0;
         movingEvasively = (bitFields & SERIAL_BIT_MovingEvasively) != 0;
         collapsed = (bitFields & SERIAL_BIT_Collapsed) != 0;
         isConscious = (bitFields & SERIAL_BIT_IsConscious) != 0;
         isAlive = (bitFields & SERIAL_BIT_IsAlive) != 0;
         defenseOptionsTakenThisRound = new DefenseOptions(in.readInt());
         mageSpellPointsAvailable = in.readShort();
         mageSpellPointsMax = in.readShort();
         priestSpellPointsAvailable = in.readShort();
         priestSpellPointsMax = in.readShort();
         setOrientation(Orientation.serializeOrientationFromStream(in));
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public Condition clone()
   {
      Condition duplicate = new Condition(orientation);
      duplicate.actionsAvailable = actionsAvailable;
      duplicate.actionsSpentThisRound = actionsSpentThisRound;
      duplicate.wounds = wounds;
      duplicate.bleedRate = bleedRate;
      duplicate.penaltyPain = penaltyPain;
      duplicate.penaltyMove = penaltyMove;
      duplicate.initiative = initiative;
      duplicate.attackedThisRound = attackedThisRound;
      duplicate.defenseOptionsTakenThisRound = defenseOptionsTakenThisRound.clone();
      duplicate.movementAvailableThisRound = movementAvailableThisRound;
      duplicate.movementAvailableEachRound = movementAvailableEachRound;
      duplicate.moveComplete = moveComplete;
      duplicate.movingEvasively = movingEvasively;
      duplicate.collapsed = collapsed;
      duplicate.isConscious = isConscious;
      duplicate.isAlive = isAlive;
      duplicate.mageSpellPointsAvailable = mageSpellPointsAvailable;
      duplicate.mageSpellPointsMax = mageSpellPointsMax;
      duplicate.priestSpellPointsAvailable = priestSpellPointsAvailable;
      duplicate.priestSpellPointsMax = priestSpellPointsMax;
      duplicate.woundsList = new ArrayList<>();
      for (Wound wound : this.woundsList) {
         duplicate.woundsList.add(wound.clone());
      }
      duplicate.setOrientation(orientation.clone());
      return duplicate;
   }

   @Override
   public String toString() {
      return ", actionsAvailable: " + actionsAvailable +
             ", pain: " + penaltyPain +
             ", wounds: " + wounds +
             ", bleed: " + bleedRate +
             ", movePenalty: " + penaltyMove +
             ", actionsSpent: " + actionsSpentThisRound +
             ", initiative: " + initiative +
             ", attackedThisRound: " + attackedThisRound +
             ", defensesOptionsTakenThisRound: " + defenseOptionsTakenThisRound +
             ", movementAvailableThisRound: " + movementAvailableThisRound +
             ", movementAvailableEachRound:" + movementAvailableEachRound +
             ", moveComplete: " + moveComplete +
             ", movingEvasively:" + movingEvasively +
             ", collapsed: " + collapsed +
             ", isConscious: " + isConscious +
             ", isAlive: " + isAlive +
             ", mageSpellPointsAvailable: " + mageSpellPointsAvailable +
             ", mageSpellPointsMax: " + mageSpellPointsMax +
             ", priestSpellPointsAvailable: " + priestSpellPointsAvailable +
             ", priestSpellPointsMax: " + priestSpellPointsMax +
             ", orientation: " + orientation;
   }
   public void collapseFromPain(CombatMap map, Character actor)
   {
      collapsed = true;
      setPosition(Position.PRONE_BACK, map, actor);
   }
   public List<Wound> getWoundsList() {
      return woundsList;
   }
   public boolean healWound(Wound woundToCure, byte woundReduction, byte bleedingReduction)
   {
      boolean woundCured = false;
      if (woundsList.contains(woundToCure)) {
         woundCured = woundToCure.healWound(woundReduction, bleedingReduction);
         if (woundCured) {
            woundsList.remove(woundToCure);
         }

         recomputeWoundEffects();
         return woundCured;
      }
      DebugBreak.debugBreak();
      return woundCured;
   }
   /**
    * This method regenerates 1 point of a wound on any wound in the list of wounds,
    * regardless of if the wound has been healed yet
    * @param woundReduction
    */
   public void regenerateWound(byte woundReduction) {
      while ((woundReduction > 0) && (woundsList.size() > 0)) {
         for (Wound wound : woundsList) {
            byte woundsWoundLevel = wound.getEffectiveWounds();
            if (woundsWoundLevel == 0) {
               // if this wound is just an arm penalty or move penalty,
               woundsWoundLevel = wound.getPenaltyLimb();
            }
            if (woundsWoundLevel >= woundReduction) {
               wound.healWound((byte) (woundReduction + (wound.getWounds() - wound.getEffectiveWounds())),
                               (byte) (woundReduction + (wound.getBleedRate() - wound.getEffectiveBleedRate())));
               woundReduction = 0;
               break;
            }
            woundReduction -= woundsWoundLevel;
            if (wound.healWound((byte) (woundsWoundLevel + (wound.getWounds() - wound.getEffectiveWounds())),
                                (byte) (woundsWoundLevel + (wound.getBleedRate() - wound.getEffectiveBleedRate())))) {
               woundsList.remove(wound);
               // stop the iteration, since we have modified the woundsList, and we'd get a ConcurrentModificationException
               regenerateWound(woundReduction);
               break;
            }
         }
      }
      recomputeWoundEffects();
   }
   public void recomputeWoundEffects() {
      // recompute all our penalties, in case something became cured:
      wounds = 0;
      bleedRate = 0;
      penaltyMove = 0;
      for (Wound wound : woundsList) {
         wounds += wound.getEffectiveWounds();
         bleedRate += wound.getEffectiveBleedRate();
         // If we are not crippled on a limb, all wounding is additive.
         // If we are crippled, don't adjust the particular penalty.
         if (penaltyMove >= 0) {
            penaltyMove += wound.getPenaltyMove();
         }
         // Check for crippling injuries
         if (wound.getPenaltyMove()     < 0) {
            penaltyMove = wound.getPenaltyMove();
         }
      }
   }
   public String regrowLimb(Wound woundToCure)
   {
      if (woundsList.contains(woundToCure)) {
         byte origWound          = woundToCure.getEffectiveWounds();
         byte origBleedRate      = woundToCure.getEffectiveBleedRate();
         if (woundToCure.regrowLimb()) {
            wounds -= origWound;
            bleedRate -= origBleedRate;
            return "";
         }
      }
      DebugBreak.debugBreak();
      return null;
   }
   public void resetSpellPoints(Advantage advMagicalAptitude, byte divineAffinity, byte divinePower)
   {
      priestSpellPointsMax = Rules.getMaxPriestSpellPoints(divineAffinity, divinePower);
      short magicalAptitude = 0;
      if (advMagicalAptitude != null) {
         magicalAptitude = (byte) (advMagicalAptitude.getLevel() + 1);
      }
      mageSpellPointsMax = Rules.getMaxMageSpellPoint(magicalAptitude);
      priestSpellPointsAvailable = priestSpellPointsMax;
      mageSpellPointsAvailable = mageSpellPointsMax;
   }

   public Facing getFacing() {
      return orientation.getFacing();
   }
   public Orientation getOrientation() {
      return orientation;
   }
   public boolean isInCoordinates(ArenaCoordinates loc) {
      return orientation.isInLocation(loc);
   }
   public List<ArenaCoordinates> getCoordinates() {
      if (orientation == null) {
         return new ArrayList<>();
      }
      return orientation.getCoordinates();
   }
   public ArenaCoordinates getHeadCoordinates() {
      return orientation.getHeadCoordinates();
   }
   public ArenaLocation getLimbLocation(LimbType limbType, CombatMap map) {
      return orientation.getLimbLocation(limbType, map);
   }

   public List<Orientation> getPossibleAdvanceOrientations(CombatMap map) {
      return orientation.getPossibleAdvanceOrientations(map, true/*blockByCharacters*/);
   }
   public boolean setHeadLocation(Character character, ArenaLocation headLocation, Facing facing, CombatMap map, Diagnostics diag) {
      return orientation.setHeadLocation(character, headLocation, facing, map, diag, true/*allowTwisting*/);
   }
//   public boolean setLocations(List<ArenaLocation> newLocations, byte newFacing, CombatMap map, Diagnostics diag) {
//      return orientation.setLocations(newLocations, newFacing, map, diag);
//   }
   public boolean setOrientation(Orientation destination) {
      if (destination.equals(orientation)) {
         return false;
      }
      orientation = destination;
      return true;
   }
   public byte getActionsNeededToChangePosition() {
      return orientation.getActionsNeededToChangePosition();
   }
   public boolean canStand() {
      return (penaltyMove >= 0) && (!isCollapsed());
   }
   public String getPositionName() {
      return orientation.getPositionName();
   }
   public byte getPositionAdjustedDefenseOption(DefenseOption defOption, byte def) {
      return orientation.getPositionAdjustedDefenseOption(defOption, def);
   }
   public byte getPositionAdjustmentForAttack() {
      return orientation.getPositionAdjustmentForAttack();
   }
   public Element getXMLObject(Document parentDoc, String newLine) {
      Element mainElement = parentDoc.createElement("Condition");
      mainElement.setAttribute("actionsAvailable",              String.valueOf(actionsAvailable));
      mainElement.setAttribute("finalDefensiveActionsAvailable",String.valueOf(finalDefensiveActionsAvailable));
      mainElement.setAttribute("maxActionsPerRound",            String.valueOf(maxActionsPerRound));
      mainElement.setAttribute("actionsSpentThisRound",         String.valueOf(actionsSpentThisRound));
      mainElement.setAttribute("initiative",                    String.valueOf(initiative));
      mainElement.setAttribute("movementAvailableThisRound",    String.valueOf(movementAvailableThisRound));
      mainElement.setAttribute("movementAvailableEachRound",    String.valueOf(movementAvailableEachRound));
      mainElement.setAttribute("hasMovedThisRound",             String.valueOf(hasMovedThisRound));
      mainElement.setAttribute("attackedThisRound",             String.valueOf(attackedThisRound));
      mainElement.setAttribute("defenseOptionsTakenThisRound",  String.valueOf(defenseOptionsTakenThisRound.getIntValue()));
      mainElement.setAttribute("moveComplete",                  String.valueOf(moveComplete));
      mainElement.setAttribute("movingEvasively",               String.valueOf(movingEvasively));
      mainElement.setAttribute("movedLastAction",               String.valueOf(movedLastAction));
      mainElement.setAttribute("collapsed",                     String.valueOf(collapsed));
      mainElement.setAttribute("penaltyPain",                   String.valueOf(penaltyPain));
      mainElement.setAttribute("isConscious",                   String.valueOf(isConscious));
      mainElement.setAttribute("isAlive",                       String.valueOf(isAlive));
      mainElement.setAttribute("priestSpellPointsMax",          String.valueOf(priestSpellPointsMax));
      mainElement.setAttribute("mageSpellPointsMax",            String.valueOf(mageSpellPointsMax));
      mainElement.setAttribute("priestSpellPointsAvailable",    String.valueOf(priestSpellPointsAvailable));
      mainElement.setAttribute("mageSpellPointsAvailable",      String.valueOf(mageSpellPointsAvailable));

      mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      mainElement.appendChild(orientation.getXMLObject(parentDoc, newLine + "  "));
      mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      Element woundsElement = parentDoc.createElement("Wounds");
      mainElement.appendChild(woundsElement);
      mainElement.appendChild(parentDoc.createTextNode(newLine));

      if (woundsList.size() > 0) {
         for (Wound wound : woundsList) {
            woundsElement.appendChild(parentDoc.createTextNode(newLine + "    "));
            woundsElement.appendChild(wound.getXMLObject(parentDoc, newLine + "    "));
         }
         woundsElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      }
      return mainElement;
   }
   public boolean serializeFromXmlObject(Node element)
   {
      if (!element.getNodeName().equals("Condition")) {
         return false;
      }
      NamedNodeMap attributes = element.getAttributes();
      if (attributes == null) {
         return false;
      }
      actionsAvailable = Byte.parseByte(attributes.getNamedItem("actionsAvailable").getNodeValue());
      finalDefensiveActionsAvailable = Byte.parseByte(attributes.getNamedItem("finalDefensiveActionsAvailable").getNodeValue());
      maxActionsPerRound = Byte.parseByte(attributes.getNamedItem("maxActionsPerRound").getNodeValue());
      actionsSpentThisRound = Byte.parseByte(attributes.getNamedItem("actionsSpentThisRound").getNodeValue());
      initiative = Byte.parseByte(attributes.getNamedItem("initiative").getNodeValue());
      movementAvailableThisRound = Byte.parseByte(attributes.getNamedItem("movementAvailableThisRound").getNodeValue());
      movementAvailableEachRound = Byte.parseByte(attributes.getNamedItem("movementAvailableEachRound").getNodeValue());
      hasMovedThisRound = Boolean.parseBoolean(attributes.getNamedItem("hasMovedThisRound").getNodeValue());
      attackedThisRound = Boolean.parseBoolean(attributes.getNamedItem("attackedThisRound").getNodeValue());
      moveComplete = Boolean.parseBoolean(attributes.getNamedItem("moveComplete").getNodeValue());
      movingEvasively = Boolean.parseBoolean(attributes.getNamedItem("movingEvasively").getNodeValue());
      movedLastAction = Boolean.parseBoolean(attributes.getNamedItem("movedLastAction").getNodeValue());
      collapsed = Boolean.parseBoolean(attributes.getNamedItem("collapsed").getNodeValue());
      penaltyPain = Byte.parseByte(attributes.getNamedItem("penaltyPain").getNodeValue());
      isConscious = Boolean.parseBoolean(attributes.getNamedItem("isConscious").getNodeValue());
      isAlive = Boolean.parseBoolean(attributes.getNamedItem("isAlive").getNodeValue());
      priestSpellPointsMax = Short.parseShort(attributes.getNamedItem("priestSpellPointsMax").getNodeValue());
      mageSpellPointsMax = Short.parseShort(attributes.getNamedItem("mageSpellPointsMax").getNodeValue());
      priestSpellPointsAvailable = Short.parseShort(attributes.getNamedItem("priestSpellPointsAvailable").getNodeValue());
      mageSpellPointsAvailable = Short.parseShort(attributes.getNamedItem("mageSpellPointsAvailable").getNodeValue());
      Node node = attributes.getNamedItem("defenseOptionsTakenThisRound");
      defenseOptionsTakenThisRound = new DefenseOptions((node == null) ? 0 : Integer.parseInt(node.getNodeValue()));

      woundsList = new ArrayList<>();
      NodeList children = element.getChildNodes();
      for (int index=0 ; index<children.getLength() ; index++) {
         Node child = children.item(index);
         if (child.getNodeName().equals("Orientation")) {
//          orientation = new Orientation();
            orientation.serializeFromXmlObject(child);
         }
         if (child.getNodeName().equals("Wounds")) {
            NodeList grandChildren = child.getChildNodes();
            for (int i=0 ; i<grandChildren.getLength() ; i++) {
               Node grandChild = grandChildren.item(i);
               if (grandChild.getNodeName().equals("Wound")) {
                  Wound wound = new Wound();
                  wound.serializeFromXmlObject(grandChild);
                  woundsList.add(wound);
               }
            }
            recomputeWoundEffects();
         }
      }
      return true;
   }
   public void awaken() {
      if (isAlive) {
         isConscious = true;
      }
   }

}
