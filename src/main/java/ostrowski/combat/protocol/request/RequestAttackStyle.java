package ostrowski.combat.protocol.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ostrowski.DebugBreak;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.AttackType;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.things.LimbType;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.SemaphoreAutoLocker;

public class RequestAttackStyle extends SyncRequest implements Enums {
   class AttackStyleInfo {
      DamageType _damageType;
      AttackType _attackType;
      DiceSet _attackDice;
      public AttackStyleInfo(DamageType damageType, AttackType attackType, DiceSet attackDice) {
         _damageType = damageType;
         _attackType = attackType;
         _attackDice = attackDice;
      }
      public AttackStyleInfo() {
         _damageType = DamageType.NONE;
         _attackType = null;
         _attackDice = new DiceSet();
      }
      public boolean isThrown() {
         return _attackType == AttackType.THROW;
      }
      public boolean isMissile() {
         return _attackType == AttackType.MISSILE;
      }
      public boolean isGrapple() {
         return _attackType == AttackType.GRAPPLE;
      }
      public boolean isCounterAttack() {
         return _attackType == AttackType.COUNTER_ATTACK;
      }
      public AttackType getAttackType() {
         return _attackType;
      }
      public DamageType getDamageType() {
         return _damageType;
      }
      public DiceSet getAttackDice() {
         return _attackDice;
      }
      @Override
      public String toString() {
         return " damageType=" + _damageType + ", attackType=" + _attackType + ", attackDice=" + _attackDice.toString();
      }
   }

   public int  _actorID   = -1;
   public int  _targetID  = -1;
   public LimbType _limbType = null;
   List<AttackStyleInfo> _attackInfo = new ArrayList<>();


   public RequestAttackStyle() {
       // c'tor used by the SerializableFactory class, when reading in a object of this class
   }
   public RequestAttackStyle(int actorID, int targetID, LimbType limbType) {
       _actorID   = actorID;
       _targetID  = targetID;
       _limbType = limbType;
   }

   public boolean isRanged() {
       return (isThrown() || isMissile());
   }
   public boolean isThrown() {
      return _attackInfo.get(getAnswerIndex()).isThrown();
   }
   public boolean isMissile() {
      return _attackInfo.get(getAnswerIndex()).isMissile();
   }
   public boolean isGrapple() {
      return _attackInfo.get(getAnswerIndex()).isGrapple();
   }
   public boolean isCounterAttack() {
      return _attackInfo.get(getAnswerIndex()).isCounterAttack();
   }

   public DamageType getDamageType() {
      return _attackInfo.get(getAnswerIndex()).getDamageType();
   }
   public DiceSet getAttackDice() {
      return _attackInfo.get(getAnswerIndex()).getAttackDice();
   }
   @Override
   public void copyAnswer(SyncRequest source) {
      if (source instanceof RequestAttackStyle) {
         RequestAttackStyle src = (RequestAttackStyle)source;
         _attackInfo.clear();
         _attackInfo.addAll(src._attackInfo);
      }
      super.copyAnswer(source);
   }
   @Override
   public synchronized void copyDataInto(SyncRequest newObj) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         super.copyDataInto(newObj);
         if (newObj instanceof RequestAttackStyle) {
            RequestAttackStyle dest = (RequestAttackStyle)newObj;
            dest._attackInfo.clear();
            dest._attackInfo.addAll(_attackInfo);
         }
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in) {
      super.serializeFromStream(in);
      try {
         _actorID       = readInt(in);
         _targetID      = readInt(in);
         _limbType      = LimbType.getByValue(readByte(in));
         short size     = readShort(in);
         _attackInfo.clear();
         for (short i=0 ; i<size ; i++) {
            DamageType damType = DamageType.getByValue(readByte(in));
            AttackType attType = AttackType.getByValue(readByte(in));
            DiceSet attDice = new DiceSet();
            attDice.serializeFromStream(in);
            _attackInfo.add(new AttackStyleInfo(damType, attType, attDice));
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out) {
      super.serializeToStream(out);
      try {
         writeToStream(_actorID, out);
         writeToStream(_targetID, out);
         writeToStream(((byte)(_limbType.value)), out);
         short size = (short) _attackInfo.size();
         writeToStream(size, out);
         for (short i=0 ; i<size ; i++) {
            if (_attackInfo.get(i).getDamageType() == null) {
               writeToStream((byte)-1, out);
            }
            else {
               writeToStream(_attackInfo.get(i).getDamageType().value, out);
            }
            AttackType attackType = _attackInfo.get(i).getAttackType();
            if (attackType == null) {
               writeToStream((byte)-1, out);
            }
            else {
               writeToStream(attackType.value, out);
            }
            _attackInfo.get(i).getAttackDice().serializeToStream(out);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Deprecated
   public void addAttackOption(int optionID, String optionStr, boolean enabled, AttackType attackType,
                               DiceSet attackDice, DamageType damageType) {
      super.addOption(optionID, optionStr, enabled);
      while (optionID < _attackInfo.size()) {
         _attackInfo.add(new AttackStyleInfo());
      }
      _attackInfo.add(new AttackStyleInfo(damageType, attackType, attackDice));
   }
   @Override
   public void addOption(int optionID, String optionStr, boolean enabled) {
      if (optionID == OPT_CANCEL_ACTION) {
         super.addOption(new RequestActionOption("Cancel attack",  RequestActionType.OPT_CANCEL_ACTION, LimbType.BODY, enabled));
         //super.addOption(optionID, "Cancel attack", enabled);
         _attackInfo.add(new AttackStyleInfo());
         return;
      }
      DebugBreak.debugBreak();
      throw new IllegalAccessError();
   }
   @Override
   protected String getAllowedKeyStrokesForOption(int optionID) {
      return "1234567890";
   }
   @Override
   public String toString() {
      return super.toString() + _attackInfo;
   }
}
