package ostrowski.combat.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ostrowski.DebugBreak;
import ostrowski.combat.common.enums.DefenseOption;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.protocol.IRequestOption;

public class DefenseOptions extends EnumOptions<DefenseOption> implements Comparable<DefenseOptions>, IRequestOption
{
   private boolean enabled;
   private String  name;

   public DefenseOptions() {}
   public DefenseOptions(int valuesBitMap) {
      setAnswerID(valuesBitMap);
   }

   public DefenseOptions(DefenseOption... opts) {
      super.add(opts);
   }
   @Override
   public void add(DefenseOption... opts) {
      super.add(opts);
   }

   @Override
   public void add(DefenseOption opt) {
      super.add(opt);
   }

   public DefenseOptions add(DefenseOptions opts) {
      for (DefenseOption opt : opts.list) {
         if (!list.contains(opt)) {
            list.add(opt);
         }
      }
      return this;
   }

   public byte getDefenseMagicPointsUsed() {
      if (contains(DefenseOption.DEF_MAGIC_5)) {
         return 5;
      }
      if (contains(DefenseOption.DEF_MAGIC_4)) {
         return 4;
      }
      if (contains(DefenseOption.DEF_MAGIC_3)) {
         return 3;
      }
      if (contains(DefenseOption.DEF_MAGIC_2)) {
         return 2;
      }
      if (contains(DefenseOption.DEF_MAGIC_1)) {
         return 1;
      }
      return 0;
   }
   public byte getDefenseCounterActions() {
      if (contains(DefenseOption.DEF_COUNTER_GRAB_3) || contains(DefenseOption.DEF_COUNTER_THROW_3)) {
         return 3;
      }
      if (contains(DefenseOption.DEF_COUNTER_GRAB_2) || contains(DefenseOption.DEF_COUNTER_THROW_2)) {
         return 2;
      }
      if (contains(DefenseOption.DEF_COUNTER_GRAB_1) || contains(DefenseOption.DEF_COUNTER_THROW_1)) {
         return 1;
      }
      return 0;
   }

   public boolean isDefensesValid() {
      boolean counterAttacking = false;
      for (DefenseOption opt1 : list) {
         if (opt1.isCounterAttack()) {
            counterAttacking = true;
         }
         for (DefenseOption opt2 : list) {
            if (!opt1.isCompatibleWith(opt2)) {
               return false;
            }
         }
      }
      // When counter attacking, one or more block/parrys must be used:
      if (counterAttacking) {
         // a defense that doesn't include a block can't use counter defenses:
         return list.contains(DefenseOption.DEF_LEFT) ||
                list.contains(DefenseOption.DEF_LEFT_2) ||
                list.contains(DefenseOption.DEF_LEFT_3) ||
                list.contains(DefenseOption.DEF_RIGHT) ||
                list.contains(DefenseOption.DEF_RIGHT_2) ||
                list.contains(DefenseOption.DEF_RIGHT_3);
      }
      return true;
   }

   public byte getDefenseActionsUsed() {
      byte actionsUsed = 0;
      for (DefenseOption opt : list) {
         actionsUsed += opt.getActionsUsed();
      }
      return actionsUsed;
   }

   public String getDefenseName(boolean pastTense, Character actor, RequestAction action) {
      StringBuilder sb = new StringBuilder();
      int index = 0;
      for (DefenseOption defOpt : list) {
         if (defOpt != DefenseOption.DEF_PD) {
            if (index > 0) {
               if (index == (list.size() - 1)) {
                  sb.append(" and ");
               }
               else {
                  sb.append(", ");
               }
            }
            sb.append(defOpt.getName(pastTense, actor, action));
         }
         index++;
      }
      if (sb.length() == 0) {
         if (pastTense) {
            return "does not defend";
         }
         return "do not defend";
      }
      return sb.toString();
   }

   public DefenseOptions logicAndWithSet(DefenseOptions availableOptions) {
      DefenseOptions newDefOpts = new DefenseOptions();
      for (DefenseOption defOpt : list) {
         if (availableOptions.contains(defOpt)) {
            newDefOpts.add(defOpt);
         }
      }
      return newDefOpts;
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof DefenseOptions)) {
         return false;
      }
      return this.getIntValue() == ((DefenseOptions) other).getIntValue();
   }
   @Override
   public int hashCode() {
      return getIntValue();
   }
   @Override
   public DefenseOptions clone() {
      return new DefenseOptions(this.getIntValue());
   }
   @Override
   public String toString() {
      return list.toString();
   }

   @Override
   public int compareTo(DefenseOptions other) {
      return Integer.compare(this.getIntValue(), other.getIntValue());
   }
   public int getDefenseCounterDefenseActions() {
      if (contains(DefenseOption.DEF_COUNTER_DEFENSE_3)) {
         return 3;
      }
      if (contains(DefenseOption.DEF_COUNTER_DEFENSE_2)) {
         return 2;
      }
      if (contains(DefenseOption.DEF_COUNTER_DEFENSE_1)) {
         return 1;
      }
      return 0;
   }
   public boolean isCounterAttackGrab() {
      for (DefenseOption opt : list) {
         if (opt.isCounterAttackGrab()) {
            return true;
         }
      }
      return false;
   }
   public boolean isCounterAttackThrow() {
      for (DefenseOption opt : list) {
         if (opt.isCounterAttackThrow()) {
            return true;
         }
      }
      return false;
   }
   public boolean isCounterAttack() {
      for (DefenseOption opt : list) {
         if (opt.isCounterAttack()) {
            return true;
         }
      }
      return false;
   }
   @Override
   public String getName() {
      return name;
   }

   @Override
   public boolean isEnabled() {
      return enabled;
   }
   @Override
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }
   @Override
   public int getIntValue() {
      int value = 0;
      for (DefenseOption opt : list) {
         value |= opt.getValue();
      }
      return value;
   }
   @Override
   public void setAnswerStr(String name) {
      this.name = name;
   }
   @Override
   public void setAnswerID(int valuesBitMap) {
      for (DefenseOption opt : DefenseOption.values()) {
         if ((valuesBitMap & opt.getValue()) != 0) {
            add(opt);
         }
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         writeToStream(this.getIntValue(), out);
         writeToStream(this.getName(), out);
      } catch (IOException e) {
         e.printStackTrace();
         DebugBreak.debugBreak();
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         this.setAnswerID(readInteger(in));
         this.setAnswerStr(readString(in));
      } catch (IOException e) {
         e.printStackTrace();
         DebugBreak.debugBreak();
      }
   }
}
