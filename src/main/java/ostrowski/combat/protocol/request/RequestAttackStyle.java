package ostrowski.combat.protocol.request;

import ostrowski.DebugBreak;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.AttackType;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.things.LimbType;
import ostrowski.protocol.RequestOption;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.SemaphoreAutoLocker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RequestAttackStyle extends SyncRequest implements Enums {
   static class AttackStyleInfo {
      final DamageType damageType;
      final AttackType attackType;
      final DiceSet    attackDice;
      public AttackStyleInfo(DamageType damageType, AttackType attackType, DiceSet attackDice) {
         this.damageType = damageType;
         this.attackType = attackType;
         this.attackDice = attackDice;
      }
      public AttackStyleInfo() {
         damageType = DamageType.NONE;
         attackType = null;
         attackDice = new DiceSet();
      }
      public boolean isThrown() {
         return attackType == AttackType.THROW;
      }
      public boolean isMissile() {
         return attackType == AttackType.MISSILE;
      }
      public boolean isGrapple() {
         return attackType == AttackType.GRAPPLE;
      }
      public boolean isCounterAttack() {
         return attackType == AttackType.COUNTER_ATTACK;
      }
      public AttackType getAttackType() {
         return attackType;
      }
      public DamageType getDamageType() {
         return damageType;
      }
      public DiceSet getAttackDice() {
         return attackDice;
      }
      @Override
      public String toString() {
         return " damageType=" + damageType + ", attackType=" + attackType + ", attackDice=" + attackDice;
      }
   }

   public int                   actorID    = -1;
   public int                   targetID   = -1;
   public LimbType              limbType   = null;
   final  List<AttackStyleInfo> attackInfo = new ArrayList<>();


   public RequestAttackStyle() {
       // c'tor used by the SerializableFactory class, when reading in a object of this class
   }
   public RequestAttackStyle(int actorID, int targetID, LimbType limbType) {
       this.actorID = actorID;
       this.targetID = targetID;
       this.limbType = limbType;
   }

   public boolean isRanged() {
       return (isThrown() || isMissile());
   }
   public boolean isThrown() {
      return attackInfo.get(getAnswerIndex()).isThrown();
   }
   public boolean isMissile() {
      return attackInfo.get(getAnswerIndex()).isMissile();
   }
   public boolean isGrapple() {
      return attackInfo.get(getAnswerIndex()).isGrapple();
   }
   public boolean isCounterAttack() {
      return attackInfo.get(getAnswerIndex()).isCounterAttack();
   }

   public DamageType getDamageType() {
      return attackInfo.get(getAnswerIndex()).getDamageType();
   }
   public DiceSet getAttackDice() {
      return attackInfo.get(getAnswerIndex()).getAttackDice();
   }
   @Override
   public void copyAnswer(SyncRequest source) {
      if (source instanceof RequestAttackStyle) {
         RequestAttackStyle src = (RequestAttackStyle)source;
         attackInfo.clear();
         attackInfo.addAll(src.attackInfo);
      }
      super.copyAnswer(source);
   }
   @Override
   public synchronized void copyDataInto(SyncRequest newObj) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         super.copyDataInto(newObj);
         if (newObj instanceof RequestAttackStyle) {
            RequestAttackStyle dest = (RequestAttackStyle)newObj;
            dest.attackInfo.clear();
            dest.attackInfo.addAll(attackInfo);
         }
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in) {
      super.serializeFromStream(in);
      try {
         actorID = readInt(in);
         targetID = readInt(in);
         limbType = LimbType.getByValue(readByte(in));
         short size     = readShort(in);
         attackInfo.clear();
         for (short i=0 ; i<size ; i++) {
            DamageType damType = DamageType.getByValue(readByte(in));
            AttackType attType = AttackType.getByValue(readByte(in));
            DiceSet attDice = new DiceSet();
            attDice.serializeFromStream(in);
            attackInfo.add(new AttackStyleInfo(damType, attType, attDice));
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out) {
      super.serializeToStream(out);
      try {
         writeToStream(actorID, out);
         writeToStream(targetID, out);
         writeToStream(((byte)(limbType.value)), out);
         short size = (short) attackInfo.size();
         writeToStream(size, out);
         for (short i=0 ; i<size ; i++) {
            if (attackInfo.get(i).getDamageType() == null) {
               writeToStream((byte)-1, out);
            }
            else {
               writeToStream(attackInfo.get(i).getDamageType().value, out);
            }
            AttackType attackType = attackInfo.get(i).getAttackType();
            if (attackType == null) {
               writeToStream((byte)-1, out);
            }
            else {
               writeToStream(attackType.value, out);
            }
            attackInfo.get(i).getAttackDice().serializeToStream(out);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Deprecated
   public void addAttackOption(int optionID, String optionStr, boolean enabled, AttackType attackType,
                               DiceSet attackDice, DamageType damageType) {
      super.addOption(new RequestOption(optionStr, optionID, enabled));
      while (optionID < attackInfo.size()) {
         attackInfo.add(new AttackStyleInfo());
      }
      attackInfo.add(new AttackStyleInfo(damageType, attackType, attackDice));
   }
   @Override
   public void addOption(int optionID, String optionStr, boolean enabled) {
      if (optionID == OPT_CANCEL_ACTION) {
         super.addOption(new RequestActionOption("Cancel attack",  RequestActionType.OPT_CANCEL_ACTION, LimbType.BODY, enabled));
         //super.addOption(optionID, "Cancel attack", enabled);
         attackInfo.add(new AttackStyleInfo());
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
      return super.toString() + attackInfo;
   }
}
