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

/*
 * Created on May 3, 2006
 *
 */

/**
 * @author Paul
 *
 */
public class Condition extends SerializableObject implements Enums {
   private byte          _actionsAvailable               = 0;
   private byte          _finalDefensiveActionsAvailable = 0;
   private byte          _maxActionsPerRound             = 3;
   final   StringBuilder _actionsAvailAudit              = new StringBuilder();
   // wounds:
   private byte          _wounds                         = 0;
   private byte          _bleedRate                      = 0;
   private byte          _penaltyPain                    = 0;

   // penalties are positive integers so a 2 means a -2 to actions.
   // A negative value means no use possible, such as a severed limb.
   private byte _penaltyMove                = 0;
   private byte _actionsSpentThisRound      = 0;
   private byte _initiative                 = 0;
   private byte _movementAvailableThisRound = 0;
   private byte _movementAvailableEachRound = 0;

   private boolean        _hasMovedThisRound            = false;
   private boolean        _attackedThisRound            = false;
   private boolean        _moveComplete                 = false;
   private boolean        _movingEvasively              = false;
   private boolean        _movedLastAction              = false;
   private DefenseOptions _defenseOptionsTakenThisRound = new DefenseOptions();

   private Orientation _orientation = null;
   private boolean     _collapsed   = false;
   private boolean     _isConscious = true;
   private boolean     _isAlive     = true;

   private short _priestSpellPointsMax       = 0;
   private short _mageSpellPointsMax         = 0;
   private short _priestSpellPointsAvailable = 0;
   private short _mageSpellPointsAvailable   = 0;

   private ArrayList<Wound> _woundsList = new ArrayList<>();

   public Condition() {
      // ctor used for all Serializable objects
   }
   public Condition(Orientation orientation) {
      _orientation = orientation.clone();
   }
   public Condition(Character character) {
      setOrientation(character.getRace().getBaseOrientation());
   }
   public byte getActionsAvailable(boolean usedForDefenseOnly) {
      if (usedForDefenseOnly && (_finalDefensiveActionsAvailable != 0)) {
         return _finalDefensiveActionsAvailable;
      }
      if (_actionsAvailable < 0) {
         return 0;
      }
      return _actionsAvailable;
   }
   public byte getActionsAvailableThisRound(boolean usedForDefenseOnly) {
      if (usedForDefenseOnly && (_finalDefensiveActionsAvailable != 0)) {
         return _finalDefensiveActionsAvailable;
      }
      return (byte) Math.min(_maxActionsPerRound -_actionsSpentThisRound, _actionsAvailable);
   }
   public byte getActionsSpentThisRound() {
      return _actionsSpentThisRound;
   }
   public byte getPenaltyPain()                         { return _penaltyPain; }
   public byte getWoundsAndPainPenalty()                { return (byte) (_penaltyPain + _wounds); }
   public byte getWounds()                              { return _wounds; }
   public byte getBleedRate()                           { return _bleedRate; }
   public byte getPenaltyRetreat  (boolean includeWounds)  {
      if (_penaltyMove < 0) {
         return _penaltyMove;
      }
      return (byte) (_penaltyMove + (includeWounds ? _wounds : 0));
   }
   public byte getPenaltyMove()                          { return _penaltyMove; }
   public byte getInitiative()                           { return _initiative; }
   public Position getPosition()                         { return _orientation.getPosition(); }
   public boolean isCollapsed()                          { return _collapsed; }
   public boolean isConscious()                          { return _isConscious; }
   public boolean isAlive()                              { return _isAlive; }
   public boolean isStanding()                           { return (_orientation.getPosition() == Position.STANDING); }
   public void setMovingEvasively(boolean movingEvasively) { _movingEvasively = movingEvasively; }
   public boolean isMovingEvasively() { return _movingEvasively;}
   public boolean hasMovedLastAction() { return _movedLastAction; }
   public byte getMovementAvailableThisRound(boolean movingEvasively) {
      if (!_isAlive || !_isConscious || _collapsed) {
         return 0;
      }

      if (movingEvasively != _movingEvasively) {
         _movingEvasively = movingEvasively;
         if (movingEvasively) {
            // Divide our movement allowance by two, rounding up
            _movementAvailableThisRound = (byte) ((_movementAvailableThisRound+1)/2);
         }
      }
      return _movementAvailableThisRound;
   }
   public void raiseFromDead(boolean asZombie) {
      _isAlive     = true;
      _isConscious = true;
      _collapsed   = false;
   }
   public void applyMovementCost(byte movementCost) {
      _movementAvailableThisRound -= movementCost;
      if (movementCost > 0) {
         _hasMovedThisRound = true;
      }
      if (_movementAvailableThisRound <= 0) {
         _movementAvailableThisRound = 0;
         _moveComplete = true;
      }
   }

   public short getPriestSpellPointsAvailable() {
      return _priestSpellPointsAvailable;
   }
   public void setPriestSpellPointsAvailable(short priestSpellPointsAvailable) {
      _priestSpellPointsAvailable = priestSpellPointsAvailable;
   }

   public short getMageSpellPointsAvailable() {
      return _mageSpellPointsAvailable;
   }
   public void setMageSpellPointsAvailable(short mageSpellPointsAvailable) {
      _mageSpellPointsAvailable = mageSpellPointsAvailable;
   }


   public boolean hasAttackedThisRound() {
       return _attackedThisRound;
   }

   public boolean canAttack() {
      if (_collapsed) {
         return false;
      }
      if (!_isConscious) {
         return false;
      }
      if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) <= 0) {
         return false;
      }
      if (_attackedThisRound) {
         return false;
      }
      return getPositionAdjustmentForAttack() != -99;
   }
   public boolean canDefend() {
      if (!_isConscious) {
         return false;
      }
      return getActionsAvailableThisRound(true/*usedForDefenseOnly*/) > 0;
   }
   private boolean canRetreat() {
      if (_collapsed) {
         return false;
      }
      if (!_isConscious) {
         return false;
      }
      if (getActionsAvailableThisRound(true/*usedForDefenseOnly*/) <= 0) {
         return false;
      }
      if (_attackedThisRound) {
         return false;
      }
      if (_penaltyMove < 0) {
         return false;
      }
      return (_movementAvailableThisRound >= 2) || !_hasMovedThisRound;
   }
   public boolean canAdvance() {
      if (!_isConscious) {
         return false;
      }
      if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) <= 0) {
         return false;
      }
      if (_penaltyMove < 0) {
         return false;
      }
      return _movementAvailableThisRound >= 1;
   }
   public void adjustActions(byte actionsThisRound, byte actionsPerRound) {
      boolean hasActionsThisRound = (_actionsAvailable > 0);
      _actionsAvailable   += actionsThisRound;
      _maxActionsPerRound += actionsPerRound;

      if ((_actionsAvailable <= 0) && hasActionsThisRound) {
         _actionsAvailable = 1;
      }
      if (_maxActionsPerRound <= 0) {
         _maxActionsPerRound = 1;
      }
   }

   public void setInitiative(byte initiative) {
      _initiative = initiative;
   }
   public void reducePain(byte painReduction) {
      _penaltyPain -= painReduction;
      if (_penaltyPain < 0) {
         _penaltyPain = 0;
      }
      if (_penaltyPain < Rules.getCollapsePainLevel()) {
         _collapsed = false;
      }
   }
   public void initializeActionsAndMovementForNewTurn(byte actionsAvailable, byte maxActionsPerRound, byte movementAllowance) {
      _actionsAvailable = actionsAvailable;
      _finalDefensiveActionsAvailable = 0;
      _maxActionsPerRound = maxActionsPerRound;
      _actionsAvailAudit.setLength(0);
      _actionsAvailAudit.append(" New turn, ").append(_actionsAvailable).append(" actions available.");

      _movementAvailableEachRound = movementAllowance;
      if ((_penaltyMove > 0) && (_movementAvailableThisRound > 0)) {
         _movementAvailableEachRound = (byte) Math.max(1, _movementAvailableEachRound - _penaltyMove);
      }
      _movementAvailableThisRound = _movementAvailableEachRound;
   }

   // return 'true' if any actions remains to be spent
   public boolean endRound() {
      if ((_actionsSpentThisRound == 0) && (_actionsAvailable > 0)) {
         _actionsAvailAudit.append(" no action/end of round(1 action).");
         _actionsAvailable--;
      }
      _actionsSpentThisRound = 0;
      _attackedThisRound  = false;
      _hasMovedThisRound  = false;
      _moveComplete       = false;
      _defenseOptionsTakenThisRound.clear();
      _movementAvailableThisRound = _movementAvailableEachRound;

      return (_actionsAvailable > 0);
   }
   public boolean isMoveComplete()   {
      return _moveComplete;
   }
   public void setMoveComplete() {
      _moveComplete = true;
   }

   public boolean hasMovedThisRound() {
      return _hasMovedThisRound;
   }
   public int getAvailableActions(StringBuilder sbReasons, boolean isBeingHeld) {
      int actionsAllowed = 0;
      if (_isConscious && _isAlive) {
         if (getActionsAvailableThisRound(true/*usedForDefenseOnly*/) > 0) {
            if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) > 0) {
               if (!_collapsed) {
                  if (canAttack()) {
                     actionsAllowed |= ACTION_ATTACK;
                  }
                  if ((_movementAvailableThisRound > 0)  && !isBeingHeld) {
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
      if ((!_isConscious) || _collapsed) {
         return 0;
      }
      if (getActionsAvailableThisRound(false/*usedForDefenseOnly*/) < 1) {
         return 0;
      }
      int availPos = _orientation.getAvailablePositions();

      if (!canStand()) {
         // don't allow the player to stand or crouch
         availPos &= ~ACTION_CROUCH;
         availPos &= ~ACTION_STAND;
      }
      return availPos;
   }
   public DefenseOptions getAvailableDefenseOptions() {
      DefenseOptions actionsAllowed = new DefenseOptions();
      if (_isConscious) {
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
      _orientation.setPosition(newPosition, map, actor);
      coordinatesToRedraw.addAll(getCoordinates());
      if (CombatServer._this != null) {
         CombatServer._this.redrawMap(coordinatesToRedraw );
      }
   }
   public void applyWound(Wound wound, Arena arena, Character character) {
      _orientation.applyWound(wound, arena.getCombatMap(), character);

      boolean wasConscious = _isConscious && _isAlive;
      if (wound.isKnockOut()) {
         _isConscious = false;
      }
      if (wound.isFatal()) {
         _isAlive = false;
      }

      if (!character.hasAdvantage(Advantage.NO_PAIN)) {
         _penaltyPain += wound.getPain();
      }
      _wounds      += wound.getEffectiveWounds();
      _bleedRate   += wound.getEffectiveBleedRate();

      if ((wound.getWounds() != 0) ||
          (wound.getBleedRate() != 0) ||
          (wound.getPenaltyLimb() != 0) ||
          (wound.getPenaltyMove() != 0)    ) {
         _woundsList.add(wound);
      }

      if (_wounds >= Rules.getUnconsciousWoundLevel(character.getAttributeLevel(Attribute.Toughness))) {
         _isConscious = false;
      }
      if (wasConscious && (!_isConscious || !_isAlive)) {
         character.dropAllEquipment(arena);
         character.releaseHold();
         setPosition(Position.PRONE_BACK, arena.getCombatMap(), character);
      }
      else if (wound.isKnockDown()) {
         setPosition(Position.PRONE_BACK, arena.getCombatMap(), character);
      }

      // If we are not crippled on a limb, all wounding is additive.
      // If we are crippled, don't adjust the particular penalty.
      if (_penaltyMove     >=0) {
         _penaltyMove     += wound.getPenaltyMove();
      }
      // Check for crippling injuries
      if (wound.getPenaltyMove()     < 0) {
         _penaltyMove  = wound.getPenaltyMove();
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
      _actionsAvailAudit.append("action, answerID=").append(action.getAnswerID());
      _actionsAvailAudit.append("(").append(action.getActionsUsed()).append(" actions)");
      _actionsAvailable -= action.getActionsUsed();
      if (_actionsAvailable < 0) {
         DebugBreak.debugBreak(actor.getName() + " has " + _actionsAvailable + " actions to use. Full audit (inclusive)=" + _actionsAvailAudit);
         _actionsAvailable = 0;
      }
//      if (action.isChangePosition()) {
//         // change position actions always report they require 2 actions, when they may not need that much
//         byte actionsUsed = action.getActionsUsed();
//         if (actionsUsed > _actionsNeededToChangePosition)
//            actionsUsed = _actionsNeededToChangePosition;
//         _actionsSpentThisRound += actionsUsed;
//      }
//      else
      {
         _actionsSpentThisRound += action.getActionsUsed();
      }
      if (action.isChannelEnergy()) {
         _mageSpellPointsAvailable -= action.getActionsUsed();
         if (_mageSpellPointsAvailable < 0) {
            DebugBreak.debugBreak();
         }
      }
      if (action.isCompletePriestSpell()) {
         if (!currentSpell.isInate()) {
            _priestSpellPointsAvailable -= currentSpell.getSpellPoints();
            if (_priestSpellPointsAvailable < 0) {
               DebugBreak.debugBreak();
            }
         }
      }
      if (action.isAttack()) {
         _attackedThisRound = true;
      }
      _movingEvasively = (action.isAdvance() && action.isEvasiveMove());
      _movedLastAction = action.isAdvance();

      if (action.isFinalDefense()) {
         _finalDefensiveActionsAvailable += action.getActionsUsed();
      }
      if (action.isChangePosition()) {
         setPosition(mapActionPosToPosition(action._positionRequest.getActionType()), arena.getCombatMap(), actor);
//         // Are we in the middle of changing position?
//         if ((_actionNeededToChangePosition > 0) && (_movingToPosition != _position)) {
//            // allow return to previous position with one action
//            if (_position == mapActionPosToPosition(action._positionRequest.getAnswerID()))
//            {
//               _actionsNeededToChangePosition = 0;
//               _movingToPosition = _position;
//            }
//            // allow continue move to next position
//            else if (_movingToPosition == mapActionPosToPosition(action._positionRequest.getAnswerID()))
//            {
//               _actionsNeededToChangePosition -= action.getActionsUsed();
//               if (_actionsNeededToChangePosition <= 0) {
//                  // add back any excess actions spent
//                  _actionsAvailable -= _actionsNeededToChangePosition;
//                  setPosition(_movingToPosition);
//               }
//            }
//         }
//         else {
//            // Allow movement to next position
//            _movingToPosition = mapActionPosToPosition(action._positionRequest.getAnswerID());
//            _actionsNeededToChangePosition = (byte) (Rules.getActionsToChangePosition() - action.getActionsUsed());
//            if (_actionsNeededToChangePosition <= 0) {
//               setPosition(_movingToPosition);
//            }
//         }
      }
   }
   public void applyHoldMaintenance(RequestGrapplingHoldMaintain holdMaintainenance) {
      _actionsAvailAudit.append("hold maintain, answerID=").append(holdMaintainenance.getAnswerID());
      _actionsAvailAudit.append("(").append(holdMaintainenance.getActionsUsed()).append(" actions)");
      if (_finalDefensiveActionsAvailable > 0) {
         _finalDefensiveActionsAvailable -= holdMaintainenance.getActionsUsed();
         if (_finalDefensiveActionsAvailable < 0) {
            _finalDefensiveActionsAvailable = 0;
         }
      }
      else {
         _actionsAvailable -= holdMaintainenance.getActionsUsed();
         if (_actionsAvailable < 0) {
            _actionsAvailable = 0;
            DebugBreak.debugBreak();
         }
         _actionsSpentThisRound += holdMaintainenance.getActionsUsed();
      }
   }

   public void applyDefense(RequestDefense defense) {
      _actionsAvailAudit.append("defense, answerID=").append(defense.getAnswerID());
      DefenseOptions defUsed = new DefenseOptions(defense.getAnswerID());
      byte actionsUsed = defense.getActionsUsed();
      _actionsAvailAudit.append("(").append(actionsUsed).append(" actions)");
      if (_finalDefensiveActionsAvailable > 0) {
         _finalDefensiveActionsAvailable -= actionsUsed;
         if (_finalDefensiveActionsAvailable < 0) {
            _finalDefensiveActionsAvailable = 0;
         }
      }
      else {
         _actionsAvailable -= actionsUsed;
         if (_actionsAvailable < 0) {
            _actionsAvailable = 0;
            DebugBreak.debugBreak("applyDefense: can't use " + actionsUsed +
                             " actions for " + defense.getAnswer() + " (ID=" + defense.getAnswerID() + ")" +
                             ", _finalDefensiveActionsAvailable=" + _finalDefensiveActionsAvailable +
                             "\n_actionsAvailAudit = " + _actionsAvailAudit);
         }
         _actionsSpentThisRound += actionsUsed;
      }
      _defenseOptionsTakenThisRound.add(defUsed);
      int magicPointsUsed = defUsed.getDefenseMagicPointsUsed();
      // TODO: how to we deal with mage-priests?
      if (magicPointsUsed > 0) {
         if (_mageSpellPointsAvailable >= magicPointsUsed) {
            _mageSpellPointsAvailable -= magicPointsUsed;
         }
         else if (_priestSpellPointsAvailable >= magicPointsUsed) {
            _priestSpellPointsAvailable -= magicPointsUsed;
         }
         else {
            // neither one can handle the points needed.
            DebugBreak.debugBreak("applyDefense: can't use " + actionsUsed +
                             " actions for " + defense.getAnswer() + " (ID=" + defense.getAnswerID() + ")" +
                             ", magicPointsUsed=" + magicPointsUsed +
                             "_mageSpellPointsAvailable = " + _mageSpellPointsAvailable +
                             "_priestSpellPointsAvailable = " + _priestSpellPointsAvailable);
            _mageSpellPointsAvailable = 0;
            _priestSpellPointsAvailable = 0;
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
//      if (_attackedThisRound)  bitFields |= SERIAL_BIT_AttackedThisRound;
//      if (_moveComplete)       bitFields |= SERIAL_BIT_MoveComplete;
//      if (_movingEvasively)    bitFields |= SERIAL_BIT_MovingEvasively;
//      if (_collapsed)          bitFields |= SERIAL_BIT_Collapsed;
//      if (_isConscious)        bitFields |= SERIAL_BIT_IsConscious;
//      if (_isAlive)            bitFields |= SERIAL_BIT_IsAlive;
//      byte[] buf = new byte[] {   _position,
//                                  _actionsAvailable,
//                                  _actionsSpentThisRound,
//                                  _actionsNeededToChangePosition,
//                                  _wounds,
//                                  _bleedRate,
//                                  _penaltyPain,
//                                  _penaltyMove,
//                                  _movingToPosition,
//                                  _initiative,
//                                  _movementAvailableThisRound,
//                                  _movementAvailableEachRound,
//                                  bitFields
//      };
//      return buf;
//   }
   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         writeToStream(_actionsAvailable, out);
         writeToStream(_actionsSpentThisRound, out);
         writeToStream(_wounds, out);
         writeToStream(_bleedRate, out);
         writeToStream(_penaltyPain, out);
         writeToStream(_penaltyMove, out);
         writeToStream(_initiative, out);
         writeToStream(_movementAvailableThisRound, out);
         writeToStream(_movementAvailableEachRound, out);
         byte bitFields = 0;
         if (_attackedThisRound) {
            bitFields |= SERIAL_BIT_AttackedThisRound;
         }
         if (_moveComplete) {
            bitFields |= SERIAL_BIT_MoveComplete;
         }
         if (_movingEvasively) {
            bitFields |= SERIAL_BIT_MovingEvasively;
         }
         if (_collapsed) {
            bitFields |= SERIAL_BIT_Collapsed;
         }
         if (_isConscious) {
            bitFields |= SERIAL_BIT_IsConscious;
         }
         if (_isAlive) {
            bitFields |= SERIAL_BIT_IsAlive;
         }
         writeToStream(bitFields, out);
         writeToStream(_defenseOptionsTakenThisRound.getIntValue(), out);
         writeToStream(_mageSpellPointsAvailable, out);
         writeToStream(_mageSpellPointsMax, out);
         writeToStream(_priestSpellPointsAvailable, out);
         writeToStream(_priestSpellPointsMax, out);
         _orientation.serializeToStream(out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         _actionsAvailable            = in.readByte();
         _actionsSpentThisRound       = in.readByte();
         _wounds                      = in.readByte();
         _bleedRate                   = in.readByte();
         _penaltyPain                 = in.readByte();
         _penaltyMove                 = in.readByte();
         _initiative                  = in.readByte();
         _movementAvailableThisRound  = in.readByte();
         _movementAvailableEachRound  = in.readByte();
         byte bitFields               = in.readByte();
         _attackedThisRound           = (bitFields & SERIAL_BIT_AttackedThisRound) != 0;
         _moveComplete                = (bitFields & SERIAL_BIT_MoveComplete) != 0;
         _movingEvasively             = (bitFields & SERIAL_BIT_MovingEvasively) != 0;
         _collapsed                   = (bitFields & SERIAL_BIT_Collapsed) != 0;
         _isConscious                 = (bitFields & SERIAL_BIT_IsConscious) != 0;
         _isAlive                     = (bitFields & SERIAL_BIT_IsAlive) != 0;
         _defenseOptionsTakenThisRound = new DefenseOptions(in.readInt());
         _mageSpellPointsAvailable    = in.readShort();
         _mageSpellPointsMax          = in.readShort();
         _priestSpellPointsAvailable  = in.readShort();
         _priestSpellPointsMax        = in.readShort();
         setOrientation(Orientation.serializeOrientationFromStream(in));
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public Condition clone()
   {
      Condition duplicate = new Condition(_orientation);
      duplicate._actionsAvailable           = _actionsAvailable;
      duplicate._actionsSpentThisRound      = _actionsSpentThisRound;
      duplicate._wounds                     = _wounds;
      duplicate._bleedRate                  = _bleedRate;
      duplicate._penaltyPain                = _penaltyPain;
      duplicate._penaltyMove                = _penaltyMove;
      duplicate._initiative                 = _initiative;
      duplicate._attackedThisRound          = _attackedThisRound;
      duplicate._defenseOptionsTakenThisRound = _defenseOptionsTakenThisRound.clone();
      duplicate._movementAvailableThisRound = _movementAvailableThisRound;
      duplicate._movementAvailableEachRound = _movementAvailableEachRound;
      duplicate._moveComplete               = _moveComplete;
      duplicate._movingEvasively            = _movingEvasively;
      duplicate._collapsed                  = _collapsed;
      duplicate._isConscious                = _isConscious;
      duplicate._isAlive                    = _isAlive;
      duplicate._mageSpellPointsAvailable   = _mageSpellPointsAvailable;
      duplicate._mageSpellPointsMax         = _mageSpellPointsMax;
      duplicate._priestSpellPointsAvailable = _priestSpellPointsAvailable;
      duplicate._priestSpellPointsMax       = _priestSpellPointsMax;
      duplicate.setOrientation(_orientation.clone());
      return duplicate;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(", actionsAvailable: ").append(_actionsAvailable);
      sb.append(", pain: ").append(_penaltyPain);
      sb.append(", wounds: ").append(_wounds);
      sb.append(", bleed: ").append(_bleedRate);
      sb.append(", movePenalty: ").append(_penaltyMove);
      sb.append(", actionsSpent: ").append(_actionsSpentThisRound);
      sb.append(", initiative: ").append(_initiative);
      sb.append(", attackedThisRound: ").append(_attackedThisRound);
      sb.append(", defensesOptionsTakenThisRound: ").append(_defenseOptionsTakenThisRound);
      sb.append(", movementAvailableThisRound: ").append(_movementAvailableThisRound);
      sb.append(", movementAvailableEachRound:").append(_movementAvailableEachRound);
      sb.append(", moveComplete: ").append(_moveComplete);
      sb.append(", movingEvasively:").append(_movingEvasively);
      sb.append(", collapsed: ").append(_collapsed);
      sb.append(", isConscious: ").append(_isConscious);
      sb.append(", isAlive: ").append(_isAlive);
      sb.append(", mageSpellPointsAvailable: ").append(_mageSpellPointsAvailable);
      sb.append(", mageSpellPointsMax: ").append(_mageSpellPointsMax);
      sb.append(", priestSpellPointsAvailable: ").append(_priestSpellPointsAvailable);
      sb.append(", priestSpellPointsMax: ").append(_priestSpellPointsMax);
      sb.append(", orientation: ").append(_orientation);
      return sb.toString();
   }
   public void collapseFromPain(CombatMap map, Character actor)
   {
      _collapsed = true;
      setPosition(Position.PRONE_BACK, map, actor);
   }
   public ArrayList<Wound> getWoundsList() {
      return _woundsList;
   }
   public String healWound(Wound woundToCure, byte woundReduction, byte bleedingReduction)
   {
      if (_woundsList.contains(woundToCure)) {
         if (woundToCure.healWound(woundReduction, bleedingReduction)) {
            _woundsList.remove(woundToCure);
         }

         recomputeWoundEffects();
         return "";
      }
      DebugBreak.debugBreak();
      return null;
   }
   /**
    * This method regenerates 1 point of a wound on any wound in the list of wounds,
    * regardless of if the wound has been healed yet
    * @param woundReduction
    */
   public void regenerateWound(byte woundReduction) {
      while ((woundReduction > 0) && (_woundsList.size() > 0)) {
         for (Wound wound : _woundsList) {
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
               _woundsList.remove(wound);
               // stop the iteration, since we have modified the _woundsList, and we'd get a ConcurrentModificationException
               regenerateWound(woundReduction);
               break;
            }
         }
      }
      recomputeWoundEffects();
   }
   public void recomputeWoundEffects() {
      // recompute all our penalties, in case something became cured:
      _wounds = 0;
      _bleedRate = 0;
      _penaltyMove = 0;
      for (Wound wound : _woundsList) {
         _wounds      += wound.getEffectiveWounds();
         _bleedRate   += wound.getEffectiveBleedRate();
         // If we are not crippled on a limb, all wounding is additive.
         // If we are crippled, don't adjust the particular penalty.
         if (_penaltyMove     >=0) {
            _penaltyMove     += wound.getPenaltyMove();
         }
         // Check for crippling injuries
         if (wound.getPenaltyMove()     < 0) {
            _penaltyMove = wound.getPenaltyMove();
         }
      }
   }
   public String regrowLimb(Wound woundToCure)
   {
      if (_woundsList.contains(woundToCure)) {
         byte origWound          = woundToCure.getEffectiveWounds();
         byte origBleedRate      = woundToCure.getEffectiveBleedRate();
         if (woundToCure.regrowLimb()) {
            _wounds    -= origWound;
            _bleedRate -= origBleedRate;
            return "";
         }
      }
      DebugBreak.debugBreak();
      return null;
   }
   public void resetSpellPoints(Advantage advMagicalAptitude, byte divineAffinity, byte divinePower)
   {
      _priestSpellPointsMax = Rules.getMaxPriestSpellPoints(divineAffinity, divinePower);
      short magicalAptitude = 0;
      if (advMagicalAptitude != null) {
         magicalAptitude = (byte) (advMagicalAptitude.getLevel() + 1);
      }
      _mageSpellPointsMax = Rules.getMaxMageSpellPoint(magicalAptitude);
      _priestSpellPointsAvailable = _priestSpellPointsMax;
      _mageSpellPointsAvailable   = _mageSpellPointsMax;
   }

   public Facing getFacing() {
      return _orientation.getFacing();
   }
   public Orientation getOrientation() {
      return _orientation;
   }
   public boolean isInCoordinates(ArenaCoordinates loc) {
      return _orientation.isInLocation(loc);
   }
   public ArrayList<ArenaCoordinates> getCoordinates() {
      if (_orientation == null) {
         return new ArrayList<>();
      }
      return _orientation.getCoordinates();
   }
   public ArenaCoordinates getHeadCoordinates() {
      return _orientation.getHeadCoordinates();
   }
   public ArenaLocation getLimbLocation(LimbType limbType, CombatMap map) {
      return _orientation.getLimbLocation(limbType, map);
   }
   public ArenaCoordinates getLimbCoordinates(LimbType limbType) {
      return _orientation.getLimbCoordinates(limbType);
   }
   public ArrayList<Orientation> getPossibleAdvanceOrientations(CombatMap map) {
      return _orientation.getPossibleAdvanceOrientations(map, true/*blockByCharacters*/);
   }
   public boolean setHeadLocation(Character character, ArenaLocation headLocation, Facing facing, CombatMap map, Diagnostics diag) {
      return _orientation.setHeadLocation(character, headLocation, facing, map, diag, true/*allowTwisting*/);
   }
//   public boolean setLocations(ArrayList<ArenaLocation> newLocations, byte newFacing, CombatMap map, Diagnostics diag) {
//      return _orientation.setLocations(newLocations, newFacing, map, diag);
//   }
   public boolean setOrientation(Orientation destination) {
      if (destination.equals(_orientation)) {
         return false;
      }
      _orientation = destination;
      return true;
   }
   public byte getActionsNeededToChangePosition() {
      return _orientation.getActionsNeededToChangePosition();
   }
   public boolean canStand() {
      return (_penaltyMove >= 0) && (!isCollapsed());
   }
   public String getPositionName() {
      return _orientation.getPositionName();
   }
   public byte getPositionAdjustedDefenseOption(DefenseOption defOption, byte def) {
      return _orientation.getPositionAdjustedDefenseOption(defOption, def);
   }
   public byte getPositionAdjustmentForAttack() {
      return _orientation.getPositionAdjustmentForAttack();
   }
   public Element getXMLObject(Document parentDoc, String newLine) {
      Element mainElement = parentDoc.createElement("Condition");
      mainElement.setAttribute("actionsAvailable",              String.valueOf(_actionsAvailable));
      mainElement.setAttribute("finalDefensiveActionsAvailable",String.valueOf(_finalDefensiveActionsAvailable));
      mainElement.setAttribute("maxActionsPerRound",            String.valueOf(_maxActionsPerRound));
      mainElement.setAttribute("actionsSpentThisRound",         String.valueOf(_actionsSpentThisRound));
      mainElement.setAttribute("initiative",                    String.valueOf(_initiative));
      mainElement.setAttribute("movementAvailableThisRound",    String.valueOf(_movementAvailableThisRound));
      mainElement.setAttribute("movementAvailableEachRound",    String.valueOf(_movementAvailableEachRound));
      mainElement.setAttribute("hasMovedThisRound",             String.valueOf(_hasMovedThisRound));
      mainElement.setAttribute("attackedThisRound",             String.valueOf(_attackedThisRound));
      mainElement.setAttribute("defenseOptionsTakenThisRound",  String.valueOf(_defenseOptionsTakenThisRound.getIntValue()));
      mainElement.setAttribute("moveComplete",                  String.valueOf(_moveComplete));
      mainElement.setAttribute("movingEvasively",               String.valueOf(_movingEvasively));
      mainElement.setAttribute("movedLastAction",               String.valueOf(_movedLastAction));
      mainElement.setAttribute("collapsed",                     String.valueOf(_collapsed));
      mainElement.setAttribute("isConscious",                   String.valueOf(_isConscious));
      mainElement.setAttribute("isAlive",                       String.valueOf(_isAlive));
      mainElement.setAttribute("priestSpellPointsMax",          String.valueOf(_priestSpellPointsMax));
      mainElement.setAttribute("mageSpellPointsMax",            String.valueOf(_mageSpellPointsMax));
      mainElement.setAttribute("priestSpellPointsAvailable",    String.valueOf(_priestSpellPointsAvailable));
      mainElement.setAttribute("mageSpellPointsAvailable",      String.valueOf(_mageSpellPointsAvailable));

      mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      mainElement.appendChild(_orientation.getXMLObject(parentDoc, newLine + "  "));
      mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      Element woundsElement = parentDoc.createElement("Wounds");
      mainElement.appendChild(woundsElement);
      mainElement.appendChild(parentDoc.createTextNode(newLine));

      if (_woundsList.size() > 0) {
         for (Wound wound : _woundsList) {
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
      _actionsAvailable              = Byte.parseByte(attributes.getNamedItem("actionsAvailable").getNodeValue());
      _finalDefensiveActionsAvailable= Byte.parseByte(attributes.getNamedItem("finalDefensiveActionsAvailable").getNodeValue());
      _maxActionsPerRound            = Byte.parseByte(attributes.getNamedItem("maxActionsPerRound").getNodeValue());
      _actionsSpentThisRound         = Byte.parseByte(attributes.getNamedItem("actionsSpentThisRound").getNodeValue());
      _initiative                    = Byte.parseByte(attributes.getNamedItem("initiative").getNodeValue());
      _movementAvailableThisRound    = Byte.parseByte(attributes.getNamedItem("movementAvailableThisRound").getNodeValue());
      _movementAvailableEachRound    = Byte.parseByte(attributes.getNamedItem("movementAvailableEachRound").getNodeValue());
      _hasMovedThisRound             = Boolean.parseBoolean(attributes.getNamedItem("hasMovedThisRound").getNodeValue());
      _attackedThisRound             = Boolean.parseBoolean(attributes.getNamedItem("attackedThisRound").getNodeValue());
      _moveComplete                  = Boolean.parseBoolean(attributes.getNamedItem("moveComplete").getNodeValue());
      _movingEvasively               = Boolean.parseBoolean(attributes.getNamedItem("movingEvasively").getNodeValue());
      _movedLastAction               = Boolean.parseBoolean(attributes.getNamedItem("movedLastAction").getNodeValue());
      _collapsed                     = Boolean.parseBoolean(attributes.getNamedItem("collapsed").getNodeValue());
      _isConscious                   = Boolean.parseBoolean(attributes.getNamedItem("isConscious").getNodeValue());
      _isAlive                       = Boolean.parseBoolean(attributes.getNamedItem("isAlive").getNodeValue());
      _priestSpellPointsMax          = Short.parseShort(attributes.getNamedItem("priestSpellPointsMax").getNodeValue());
      _mageSpellPointsMax            = Short.parseShort(attributes.getNamedItem("mageSpellPointsMax").getNodeValue());
      _priestSpellPointsAvailable    = Short.parseShort(attributes.getNamedItem("priestSpellPointsAvailable").getNodeValue());
      _mageSpellPointsAvailable      = Short.parseShort(attributes.getNamedItem("mageSpellPointsAvailable").getNodeValue());
      Node node = attributes.getNamedItem("defenseOptionsTakenThisRound");
      _defenseOptionsTakenThisRound  = new DefenseOptions((node == null) ? 0 : Integer.parseInt(node.getNodeValue()));

      _woundsList = new ArrayList<>();
      NodeList children = element.getChildNodes();
      for (int index=0 ; index<children.getLength() ; index++) {
         Node child = children.item(index);
         if (child.getNodeName().equals("Orientation")) {
//          _orientation = new Orientation();
            _orientation.serializeFromXmlObject(child);
         }
         if (child.getNodeName().equals("Wounds")) {
            NodeList grandChildren = child.getChildNodes();
            for (int i=0 ; i<grandChildren.getLength() ; i++) {
               Node grandChild = grandChildren.item(i);
               if (grandChild.getNodeName().equals("Wound")) {
                  Wound wound = new Wound();
                  wound.serializeFromXmlObject(grandChild);
                  _woundsList.add(wound);
               }
            }
            recomputeWoundEffects();
         }
      }
      return true;
   }
   public void awaken() {
      if (_isAlive) {
         _isConscious = true;
      }
   }
   public byte getMaxActionsPerTurn() {
      return _maxActionsPerRound;
   }
}
