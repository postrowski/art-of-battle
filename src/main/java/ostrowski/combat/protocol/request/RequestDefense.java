/*
 * Created on May 16, 2006
 *
 */
package ostrowski.combat.protocol.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DefenseOptions;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DefenseOption;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.protocol.RequestOption;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.SemaphoreAutoLocker;

public class RequestDefense extends SyncRequest implements Enums
{
   private int        attackerID        = -1;
   private byte       attackActions     = 0;
   private DamageType damageType        = DamageType.NONE;
   private byte       minimumDamage     = 0;
   private double     damageExpected    = 0;
   private double     toHitRollExpected = 0;
   private DiceSet    toHitDice         = new DiceSet();
   private boolean    rangedAttack      = false;
   private boolean    chargeAttack      = false;
   private boolean    grappleAttack     = false;
   private RANGE      range             = RANGE.OUT_OF_RANGE;
   private Spell      attackingSpell    = null;

   @Override
   protected String getAllowedKeyStrokesForOption(int optionID) {
      int actionsUsed = getDefenseActionsUsed(optionID);
      if (actionsUsed == 0) {
         return "0";
      }
      if (actionsUsed == 1) {
         return "123456789";
      }
      if (actionsUsed == 2) {
         return "abcdefghijklmnopqrstuvwxyz";
      }
      if (actionsUsed == 3) {
         return "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
      }
      if (actionsUsed == 4) {
         return "<ctrl>a<ctrl>b<ctrl>c<ctrl>d<ctrl>e<ctrl>f<ctrl>g<ctrl>h<ctrl>i<ctrl>j<ctrl>k<ctrl>l<ctrl>m<ctrl>n<ctrl>o<ctrl>p<ctrl>q<ctrl>r<ctrl>s<ctrl>t<ctrl>u<ctrl>v<ctrl>w<ctrl>x<ctrl>y<ctrl>z";
      }
      return "<alt>a<alt>b<alt>c<alt>d<alt>e<alt>f<alt>g<alt>h<alt>i<alt>j<alt>k<alt>l<alt>m<alt>n<alt>o<alt>p<alt>q<alt>r<alt>s<alt>t<alt>u<alt>v<alt>w<alt>x<alt>y<alt>z";
   }

   @Override
   public synchronized void copyDataInto(SyncRequest newObj)
   {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         super.copyDataInto(newObj);
         if (newObj instanceof RequestDefense) {
            RequestDefense reqDef = (RequestDefense) newObj;
            reqDef.attackerID = attackerID;
            reqDef.attackActions = attackActions;
            reqDef.damageType = damageType;
            reqDef.minimumDamage = minimumDamage;
            reqDef.damageExpected = damageExpected;
            reqDef.toHitRollExpected = toHitRollExpected;
            reqDef.toHitDice = toHitDice;
            reqDef.rangedAttack = rangedAttack;
            reqDef.chargeAttack = chargeAttack;
            reqDef.grappleAttack = grappleAttack;
            reqDef.range = range;
         }
      }
   }

   public RequestDefense() {
      // default c'tor used by factor method to serialize in from a stream.
   }
   public RequestDefense(Character attacker, RequestAction attack, RANGE range) {
      attackerID = attacker.uniqueID;
      attackActions = attack.getAttackActions(true/*considerSpellAsAttack*/);
      toHitRollExpected = attack.getExpectedAttackRoll(attacker, true/*includeSkill*/, true/*includeWoundsAndPain*/, range);
      toHitDice = attack.getAttackDice(attacker, true/*includeSkill*/, true/*includeWoundsAndPain*/, range);
      rangedAttack = attack.isRangedAttack();
      chargeAttack = attack.isCharge();
      grappleAttack = attack.isGrappleAttack() || attack.isCounterAttack();
      this.range = range;
      attackingSpell = attack.getSpell();

      WeaponStyleAttack style = attack.getWeaponStyleAttack(attacker);
      if (style != null) {
         minimumDamage = attack.getMinimumDamage(attacker);
         damageType = style.getDamageType();
         damageExpected = minimumDamage + style.getVarianceDie().getAverageRoll(true/*allowExplodes*/);
      }
   }

   @Override
   public boolean isCancelable() {
       return false;
   }
   public int            getAttackerID()        { return attackerID; }
   public byte           getAttackActions()     { return attackActions; }
   public byte           getMinimumDamage()     { return minimumDamage; }
   public double         getExpectedDamage()    { return damageExpected; }
   public double         getExpectedToHitRoll() { return toHitRollExpected; }
   public DiceSet        getExpectedToHitDice() { return toHitDice; }
   public boolean        isRangedAttack()       { return rangedAttack; }
   public boolean        isChargeAttack()       { return chargeAttack; }
   public boolean        isGrapple()            { return grappleAttack; }
   public Spell          getSpell()             { return attackingSpell; }
   public RANGE          getRange()             { return range; }
   public DamageType     getDamageType()        { return damageType; }
   public int            getDefenseIndex()      { return getAnswerID(); }

   public DefenseOptions getDefenseOptions()    { return new DefenseOptions(getAnswerID()); }
   public static DefenseOptions getDefenseOptions(int answerID)  { return new DefenseOptions(answerID); }

   public static byte getDefenseActionsUsed(int answerID) {
      return getDefenseOptions(answerID).getDefenseActionsUsed();
   }
   public String getDefenseName(boolean tensePast, Character defender, RequestAction attack) {
      return getDefenseName(getAnswerID(), tensePast, defender, attack);
   }
   public static String getDefenseName(int answerID, boolean tensePast, Character defender, RequestAction attack) {
      return getDefenseOptions(answerID).getDefenseName(tensePast, defender, attack);
   }
   public void addOption(DefenseOptions defOpts, int TN, boolean enabled, Character defender, RequestAction attack) {
      String defenseName = defOpts.getDefenseName(false/*pastTense*/, defender, attack);
      if ((defenseName != null) && (defenseName.length() > 0)) {
         if (enabled) {
            defenseName += " (TN="+TN+")";
         }
         addOption(new RequestOption(defenseName, defOpts.getIntValue(), enabled));
      }
   }

   public byte getActionsUsed()
   {
      return getDefenseActionsUsed(getAnswerID());
   }
   public boolean isRetreat() {
      return getDefenseOptions().contains(DefenseOption.DEF_RETREAT);
   }

   public static byte getDefenseCounterActions(int defOption) {
      return getDefenseOptions(defOption).getDefenseCounterActions();
   }
   @Override
   public void serializeFromStream(DataInputStream in) {
      super.serializeFromStream(in);
      try {
         attackerID = readInt(in);
         attackActions = readByte(in);
         damageType = ostrowski.combat.common.enums.DamageType.getByValue(readByte(in));
         minimumDamage = readByte(in);
         damageExpected = readDouble(in);
         toHitRollExpected = readDouble(in);
         toHitDice.serializeFromStream(in);
         rangedAttack = readBoolean(in);
         chargeAttack = readBoolean(in);
         grappleAttack = readBoolean(in);
         byte rangeByte     = readByte(in);
         for (RANGE range : RANGE.values()) {
            if (range.ordinal() == rangeByte) {
               this.range = range;
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out) {
      super.serializeToStream(out);
      try {
         writeToStream(attackerID, out);
         writeToStream(attackActions, out);
         writeToStream(damageType.value, out);
         writeToStream(minimumDamage, out);
         writeToStream(damageExpected, out);
         writeToStream(toHitRollExpected, out);
         toHitDice.serializeToStream(out);
         writeToStream(rangedAttack, out);
         writeToStream(chargeAttack, out);
         writeToStream(grappleAttack, out);
         writeToStream((byte)(range.ordinal()), out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
