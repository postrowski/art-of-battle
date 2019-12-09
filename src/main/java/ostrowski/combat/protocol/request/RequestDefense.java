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
   private int            _attackerID        = -1;
   private byte           _attackActions     = 0;
   private DamageType     _damageType        = DamageType.NONE;
   private byte           _minimumDamage     = 0;
   private double         _damageExpected    = 0;
   private double         _toHitRollExpected = 0;
   private DiceSet        _toHitDice         = new DiceSet();
   private boolean        _rangedAttack      = false;
   private boolean        _chargeAttack      = false;
   private boolean        _grappleAttack     = false;
   private RANGE          _range             = RANGE.OUT_OF_RANGE;
   private Spell          _attackingSpell    = null;

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
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         super.copyDataInto(newObj);
         if (newObj instanceof RequestDefense) {
            RequestDefense reqDef = (RequestDefense) newObj;
            reqDef._attackerID        = _attackerID;
            reqDef._attackActions     = _attackActions;
            reqDef._damageType        = _damageType;
            reqDef._minimumDamage     = _minimumDamage;
            reqDef._damageExpected    = _damageExpected;
            reqDef._toHitRollExpected = _toHitRollExpected;
            reqDef._toHitDice         = _toHitDice;
            reqDef._rangedAttack      = _rangedAttack;
            reqDef._chargeAttack      = _chargeAttack;
            reqDef._grappleAttack     = _grappleAttack;
            reqDef._range             = _range;
         }
      }
   }

   public RequestDefense() {
      // default c'tor used by factor method to serialize in from a stream.
   }
   public RequestDefense(Character attacker, RequestAction attack, RANGE range) {
      _attackerID        = attacker._uniqueID;
      _attackActions     = attack.getAttackActions(true/*considerSpellAsAttack*/);
      _toHitRollExpected = attack.getExpectedAttackRoll(attacker, true/*includeSkill*/, true/*includeWoundsAndPain*/, range);
      _toHitDice         = attack.getAttackDice(attacker, true/*includeSkill*/, true/*includeWoundsAndPain*/, range);
      _rangedAttack      = attack.isRangedAttack();
      _chargeAttack      = attack.isCharge();
      _grappleAttack     = attack.isGrappleAttack() || attack.isCounterAttack();
      _range             = range;
      _attackingSpell    = attack.getSpell();

      WeaponStyleAttack style = attack.getWeaponStyleAttack(attacker);
      if (style != null) {
         _minimumDamage  = attack.getMinimumDamage(attacker);
         _damageType     = style.getDamageType();
         _damageExpected = _minimumDamage + style.getVarianceDie().getAverageRoll(true/*allowExplodes*/);
      }
   }

   @Override
   public boolean isCancelable() {
       return false;
   }
   public int            getAttackerID()        { return _attackerID; }
   public byte           getAttackActions()     { return _attackActions; }
   public byte           getMinimumDamage()     { return _minimumDamage; }
   public double         getExpectedDamage()    { return _damageExpected; }
   public double         getExpectedToHitRoll() { return _toHitRollExpected; }
   public DiceSet        getExpectedToHitDice() { return _toHitDice; }
   public boolean        isRangedAttack()       { return _rangedAttack; }
   public boolean        isChargeAttack()       { return _chargeAttack; }
   public boolean        isGrapple()            { return _grappleAttack; }
   public Spell          getSpell()             { return _attackingSpell; }
   public RANGE          getRange()             { return _range; }
   public DamageType     getDamageType()        { return _damageType; }
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

   public static byte getDefenseMagicPointsUsed(int defOption) {
      return getDefenseOptions(defOption).getDefenseMagicPointsUsed();
   }

   public static byte getDefenseCounterActions(int defOption) {
      return getDefenseOptions(defOption).getDefenseCounterActions();
   }
   @Override
   public void serializeFromStream(DataInputStream in) {
      super.serializeFromStream(in);
      try {
         _attackerID        = readInt(in);
         _attackActions     = readByte(in);
         _damageType        = ostrowski.combat.common.enums.DamageType.getByValue(readByte(in));
         _minimumDamage     = readByte(in);
         _damageExpected    = readDouble(in);
         _toHitRollExpected = readDouble(in);
         _toHitDice.serializeFromStream(in);
         _rangedAttack      = readBoolean(in);
         _chargeAttack      = readBoolean(in);
         _grappleAttack     = readBoolean(in);
         byte rangeByte     = readByte(in);
         for (RANGE range : RANGE.values()) {
            if (range.ordinal() == rangeByte) {
               _range = range;
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
         writeToStream(_attackerID, out);
         writeToStream(_attackActions, out);
         writeToStream(_damageType.value, out);
         writeToStream(_minimumDamage, out);
         writeToStream(_damageExpected, out);
         writeToStream(_toHitRollExpected, out);
         _toHitDice.serializeToStream(out);
         writeToStream(_rangedAttack, out);
         writeToStream(_chargeAttack, out);
         writeToStream(_grappleAttack, out);
         writeToStream((byte)(_range.ordinal()), out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
