package ostrowski.combat.common;

import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.protocol.DieRoll;
import ostrowski.combat.server.CombatServer;
import ostrowski.protocol.SerializableObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.*;

/*
 * Created on May 3, 2006
 */

/**
 * @author Paul
 *
 */
public class DiceSet extends SerializableObject implements Enums
{

   public int getDiceCount(DieType type) {
      return _diceCount.get(type);
   }

   private final HashMap<DieType, Integer> _diceCount         = new HashMap<>() {
     {
        for (DieType dieType : DieType.values()) {
           put(dieType, 0);
        }
     }
     private static final long serialVersionUID = 1L;
  };

   public double                     _multiplier        = 1.0;
   public StringBuilder              _lastDieRolls      = null;
   private boolean                   _lastRollIsAllOnes = false;

   public DiceSet() {
   }

   public DiceSet(int d1, int d4, int d6, int d8, int d10, int d12, int d20, int dbell, double multiplier) {
      _diceCount.put(DieType.D1, d1);
      _diceCount.put(DieType.D4, d4);
      _diceCount.put(DieType.D6, d6);
      _diceCount.put(DieType.D8, d8);
      _diceCount.put(DieType.D10, d10);
      _diceCount.put(DieType.D12, d12);
      _diceCount.put(DieType.D20, d20);
      _diceCount.put(DieType.Dbell, dbell);
      _multiplier = multiplier;
   }

   public DiceSet(String diceDescription) {
      for (DieType die : DieType.values()) {
         _diceCount.put(die, 0);
      }
      _multiplier = 1;

      diceDescription = diceDescription.trim();
      int locStart = diceDescription.indexOf('(');
      int locEnd = diceDescription.lastIndexOf(')');
      if ((locStart != -1) && (locEnd != -1)) {
         DiceSet pureDice = new DiceSet(diceDescription.substring(locStart + 1, locEnd));
         for (DieType die : DieType.values()) {
            _diceCount.put(die, pureDice._diceCount.get(die));
         }
         _multiplier *= pureDice._multiplier;

         String remainder = diceDescription.substring(locEnd + 1).trim();
         if ((remainder.charAt(0) == '/') || (remainder.charAt(0) == '*')) {
            String number = remainder.substring(1).trim();
            if (remainder.charAt(0) == '/') {
               _multiplier /= Integer.parseInt(number);
            }
            else {
               _multiplier *= Float.parseFloat(number);
            }
         }
      }
      else {
         diceDescription = diceDescription.replaceAll("-", "+-");
         StringTokenizer st = new StringTokenizer(diceDescription, "+");
         while (st.hasMoreTokens()) {
            String dice = st.nextToken().trim();
            if ((dice.charAt(0) == 'd') || (dice.charAt(0) == 'D')) {
               dice = "1" + dice;
            }
            StringTokenizer st2 = new StringTokenizer(dice, "dD");
            String dieCount = st2.nextToken().trim();
            String dieType;
            if (st2.hasMoreTokens()) {
               dieType = st2.nextToken().trim();
            }
            else {
               dieType = "1";
            }
            int nDieCount = Integer.parseInt(dieCount);

            int nDieType = dieType.equals("10±") ? 13 : Integer.parseInt(dieType);
            switch (nDieType) {
               case 1:
                  _diceCount.put(DieType.D1, nDieCount);
                  break;
               case 4:
                  _diceCount.put(DieType.D4, nDieCount);
                  break;
               case 6:
                  _diceCount.put(DieType.D6, nDieCount);
                  break;
               case 8:
                  _diceCount.put(DieType.D8, nDieCount);
                  break;
               case 10:
                  _diceCount.put(DieType.D10, nDieCount);
                  break;
               //case 10+: _diceCount.put(DieType.Dbell, nDieCount); break;
               case 12:
                  _diceCount.put(DieType.D12, nDieCount);
                  break;
               case 13:
                  _diceCount.put(DieType.Dbell, nDieCount);
                  break;
               case 20:
                  _diceCount.put(DieType.D20, nDieCount);
                  break;
            }
         }
      }
   }

   public DiceSet addBonus(int bonus) {
      if (bonus == 0) {
         return this;
      }
      return new DiceSet(_diceCount.get(DieType.D1) + (int) (_multiplier * bonus),
                         _diceCount.get(DieType.D4),  _diceCount.get(DieType.D6),
                         _diceCount.get(DieType.D8),  _diceCount.get(DieType.D10),
                         _diceCount.get(DieType.D12), _diceCount.get(DieType.D20),
                         _diceCount.get(DieType.Dbell), _multiplier);
   }

   public DiceSet addDie(DiceSet addend) {
      if (_multiplier != addend._multiplier) {
         throw new InvalidParameterException("incompatible multipliers between added dice");
      }
      return new DiceSet(_diceCount.get(DieType.D1) + addend._diceCount.get(DieType.D1),
                         _diceCount.get(DieType.D4) + addend._diceCount.get(DieType.D4),
                         _diceCount.get(DieType.D6) + addend._diceCount.get(DieType.D6),
                         _diceCount.get(DieType.D8) + addend._diceCount.get(DieType.D8),
                         _diceCount.get(DieType.D10) + addend._diceCount.get(DieType.D10),
                         _diceCount.get(DieType.D12) + addend._diceCount.get(DieType.D12),
                         _diceCount.get(DieType.D20) + addend._diceCount.get(DieType.D20),
                         _diceCount.get(DieType.Dbell) + addend._diceCount.get(DieType.Dbell), _multiplier);
   }

   public double getAverageRoll(boolean allowExplodes) {
      if (allowExplodes) {
         return getExpectedRoll();
      }
      double total = 0;
      for (DieType dieType : DieType.values()) {
         if (dieType == DieType.Dbell) {
            total += (1 + 10) * _diceCount.get(dieType);
         }
         else {
            total += (1 + dieType.getSides()) * _diceCount.get(dieType);
         }
      }
      return (total * _multiplier) / 2.0;
   }

   public int roll(boolean allowExplodes, Character actor, RollType rollType) {
      Map<DieType, List<List<Integer>>> results = new HashMap<>();
      int roll = roll(allowExplodes, results);
      if ((CombatServer._this != null) && (actor != null)) {
         DieRoll dieRoll = new DieRoll(actor, 0/*dieColor*/, allowExplodes, rollType, results);
         CombatServer._this.getArena().sendEventToAllClients(dieRoll);
      }
      return roll;
   }
   private int roll(boolean allowExplodes, Map<DieType, List<List<Integer>>> results) {
      results.clear();
      _lastRollIsAllOnes = true;
      _lastDieRolls = new StringBuilder();
      _lastDieRolls.append("{ ");
      int sum = _diceCount.get(DieType.D1);
      boolean dieActuallyRolled = false;
      for (DieType dieType : new DieType[] { DieType.D4, DieType.D6, DieType.D8, DieType.D10, DieType.D12, DieType.D20, DieType.Dbell}) {
         List<List<Integer>> rollDataThisDieType = new ArrayList<>();
         results.put(dieType, rollDataThisDieType);
         for (int die = 0; die < _diceCount.get(dieType); die++) {
            dieActuallyRolled = true;
            List<Integer> fullRollData = new ArrayList<>();
            rollDataThisDieType.add(fullRollData);
            int dieRoll = rollDie(allowExplodes /*up*/, allowExplodes /*down*/, dieType, fullRollData);
            sum += dieRoll;
            if ((dieRoll != 1) || (dieType == DieType.Dbell)) {
               _lastRollIsAllOnes = false;
            }
         }
      }
      if (!dieActuallyRolled) {
         _lastRollIsAllOnes = false;
      }
      int add = _diceCount.get(DieType.D1);
      if (add != 0) {
         if (add > 0) {
            _lastDieRolls.append("+");
         }
         _lastDieRolls.append(add);
      }
      _lastDieRolls.append("}");
      return (int) Math.floor((sum * _multiplier) + .4999999);
   }

   private int rollDie(boolean allowExplodesUp, boolean allowExplodesDown, DieType dieType, List<Integer> fullRollData) {
      int sides = dieType.getSides();
      int roll = (int) (Math.floor(CombatServer.random() * sides)) + 1;
      fullRollData.add(roll);
      int modRoll = roll;
      if (dieType == DieType.Dbell) {
         if (roll == 11) {
            _lastDieRolls.append("-");
            if (!allowExplodesDown) {
               return 0;
            }
            modRoll = -10;
            allowExplodesUp = false;
         }
         else if (roll == 12) {
            _lastDieRolls.append("+");
            if (!allowExplodesUp) {
               return 10;
            }
            modRoll = 10;
            allowExplodesDown = false;
         }
         else {
            _lastDieRolls.append(roll);
            if (allowExplodesDown && !allowExplodesUp) {
               // this change make a '7' rolled after a '-' be a -7.
               modRoll = 10 - modRoll;
            }
         }
         _lastDieRolls.append("/10±");
      }
      else {
         _lastDieRolls.append(modRoll).append("/").append(dieType.getSides());
      }

      _lastDieRolls.append(", ");
      if (allowExplodesUp || (dieType == DieType.Dbell)) {
         if (allowExplodesUp && (roll == dieType.getSides())) {
            _lastDieRolls.append("+");
            return modRoll + rollDie(allowExplodesUp, allowExplodesDown, dieType, fullRollData);
         }
         if ((roll == 11) && (dieType == DieType.Dbell)) {
            _lastDieRolls.append("-");
            return modRoll + rollDie(allowExplodesUp, allowExplodesDown, dieType, fullRollData);
         }
      }
      return modRoll;
   }

   public String getLastDieRoll() {
      if (_lastDieRolls == null) {
         return null;
      }
      return _lastDieRolls.toString();
   }

   public boolean lastRollRolledAllOnes() {
      return _lastRollIsAllOnes;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      int typesOfDice = 0;
      for (DieType dieType : new DieType[] { DieType.D4, DieType.D6, DieType.D8, DieType.D10, DieType.D12, DieType.D20, DieType.Dbell}) {
         if (_diceCount.get(dieType) > 0) {
            if (sb.length() > 0) {
               sb.append(" + ");
            }
            sb.append(_diceCount.get(dieType));
            if (dieType == DieType.Dbell) {
               sb.append("d10±");
            }
            else {
               sb.append('d').append(dieType.getSides());
            }
            typesOfDice++;
         }
      }
      if (_diceCount.get(DieType.D1) != 0) {
         if (_diceCount.get(DieType.D1) > 0) {
            sb.append("+");
         }
         sb.append(_diceCount.get(DieType.D1));
      }
      if (_multiplier != 1) {
         if (typesOfDice > 1) {
            sb.insert(0, '(');
            sb.append(')');
         }
         if (_multiplier < 1) {
            sb.append(" / ").append((int) (1 / _multiplier));
         }
         else {
            sb.append(" * ").append((int) _multiplier);
         }
      }
      return sb.toString();
   }

   public static DiceSet getSingleDie(DieType dieType) {
      return getGroupDice(dieType, 1);
   }

   public static DiceSet getGroupDice(DieType dieType, int count) {
      switch (dieType) {
         case D1:      return new DiceSet(count, 0, 0, 0, 0, 0, 0, 0, 1);
         case D4:      return new DiceSet(0, count, 0, 0, 0, 0, 0, 0, 1);
         case D6:      return new DiceSet(0, 0, count, 0, 0, 0, 0, 0, 1);
         case D8:      return new DiceSet(0, 0, 0, count, 0, 0, 0, 0, 1);
         case D10:     return new DiceSet(0, 0, 0, 0, count, 0, 0, 0, 1);
         case D12:     return new DiceSet(0, 0, 0, 0, 0, count, 0, 0, 1);
         case D20:     return new DiceSet(0, 0, 0, 0, 0, 0, count, 0, 1);
         case Dbell:   return new DiceSet(0, 0, 0, 0, 0, 0, 0, count, 1);
      }
      return null;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof DiceSet)) {
         return false;
      }
      DiceSet other = (DiceSet) obj;
      if (_multiplier != other._multiplier) {
         return false;
      }
      for (DieType dieType : DieType.values()) {
         if (_diceCount.get(dieType) != other._diceCount.get(dieType)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public int hashCode() {
      int hash = (int) (_multiplier * 100);
      for (DieType dieType : DieType.values()) {
         hash = hash << 2;
         hash += _diceCount.get(dieType);
      }
      return hash;
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         for (DieType dieType : DieType.values()) {
            _diceCount.put(dieType, readInt(in));
         }
         _multiplier = readDouble(in);
         _lastDieRolls = new StringBuilder(readString(in));
         if (_lastDieRolls.length() == 0) {
            _lastDieRolls = null;
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         for (DieType dieType : DieType.values()) {
            writeToStream(_diceCount.get(dieType), out);
         }
         writeToStream(_multiplier, out);
         if (_lastDieRolls == null) {
            writeToStream("", out);
         }
         else {
            writeToStream(_lastDieRolls.toString(), out);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public double getChanceOfAllOnes() {
      if (_diceCount.get(DieType.Dbell) > 0) {
         return 1.0 / (Math.pow(12, _diceCount.get(DieType.Dbell)));
      }

      return 1.0 / (Math.pow(4, _diceCount.get(DieType.D4)) * Math.pow(6, _diceCount.get(DieType.D6)) * Math.pow(8, _diceCount.get(DieType.D8))
                    * Math.pow(10, _diceCount.get(DieType.D10)) * Math.pow(12, _diceCount.get(DieType.D12)) * Math.pow(20, _diceCount.get(DieType.D20)));
   }

   public double getOddsForTN(double TN) {
      return getOddsForTN(_diceCount.get(DieType.D4), _diceCount.get(DieType.D6), _diceCount.get(DieType.D8), _diceCount.get(DieType.D10),
                          _diceCount.get(DieType.D12), _diceCount.get(DieType.D20), _diceCount.get(DieType.Dbell), TN - _diceCount.get(DieType.D1), // TN
                          0); // rollSoFar
   }

   private double getOddsForTN(int d4s, int d6s, int d8s, int d10s, int d12s, int d20s, int dBell, double TN, int rollSoFar) {
      if (d4s > 0) {
         return getOddsForTNWithOneDie(DieType.D4, d4s - 1, d6s, d8s, d10s, d12s, d20s, dBell, TN, rollSoFar);
      }
      if (d6s > 0) {
         return getOddsForTNWithOneDie(DieType.D6, d4s, d6s - 1, d8s, d10s, d12s, d20s, dBell, TN, rollSoFar);
      }
      if (d8s > 0) {
         return getOddsForTNWithOneDie(DieType.D8, d4s, d6s, d8s - 1, d10s, d12s, d20s, dBell, TN, rollSoFar);
      }
      if (d10s > 0) {
         return getOddsForTNWithOneDie(DieType.D10, d4s, d6s, d8s, d10s - 1, d12s, d20s, dBell, TN, rollSoFar);
      }
      if (d12s > 0) {
         return getOddsForTNWithOneDie(DieType.D12, d4s, d6s, d8s, d10s, d12s - 1, d20s, dBell, TN, rollSoFar);
      }
      if (d20s > 0) {
         return getOddsForTNWithOneDie(DieType.D20, d4s, d6s, d8s, d10s, d12s, d20s - 1, dBell, TN, rollSoFar);
      }
      if (dBell > 0) {
         return getOddsForTNWithOneDie(DieType.Dbell, d4s, d6s, d8s, d10s, d12s, d20s, dBell - 1, TN, rollSoFar);
      }
      return 0;
   }

   static final HashMap<String, Double> _map = new HashMap<>();

   private double getOddsForTNWithOneDie(DieType die, int d4s, int d6s, int d8s, int d10s, int d12s, int d20s, int dBell, double TN, int rollSoFar) {
      if (die == DieType.Dbell) {
         double odds = 1.0;
         double needed = TN - rollSoFar;
         boolean belowZero = (needed <= 0);
         while (needed <= 0) {
            odds *= 1.0/12.0;
            needed += 10;
         }
         while (needed > 10) {
            odds *= 1.0/12.0;
            needed -= 10;
         }
         if (belowZero) {
            odds = 1-(odds*(needed/ 12.0));
         }
         else {
            odds *= (12 - needed) / 12.0;
         }
         return odds;
      }
      if (TN <= Math.round(rollSoFar)) {
         // always do it
         return 1;
      }
      // We can speed up the processing by caching all results for re-use.
      String key = die + ";" + d4s + ";" + d6s + ";" + d8s + ";" + d10s + ";" + d12s + ";" + d20s + ";" + dBell + ";" + TN + ";" + rollSoFar;
      Double results = _map.get(key);
      if (results == null) {
         double chance = 0.0;
         int dice = _diceCount.get(die);
         for (int roll = 1; roll <= dice; roll++) {
            if (TN > Math.round(rollSoFar + roll)) {
               if (roll == dice) {
                  int modRoll = roll;
                  if (die == DieType.Dbell) {
                     modRoll = 10;
                  }
                  // explode the die, by adding another die of the same size.
                  chance += getOddsForTN(d4s + (die == DieType.D4 ? 1 : 0), d6s + (die == DieType.D6 ? 1 : 0), d8s + (die == DieType.D8 ? 1 : 0),
                                         d10s + (die == DieType.D10 ? 1 : 0), d12s + (die == DieType.D12 ? 1 : 0), d20s + (die == DieType.D20 ? 1 : 0),
                                         dBell + (die == DieType.Dbell ? 1 : 0), TN, (rollSoFar + modRoll));
               }
               else {
                  if ((roll == 11) && (die == DieType.Dbell)) {
                     // implode the die, by subtracting 10.
                     chance += getOddsForTN(d4s, d6s, d8s, d10s, d12s, d20s, dBell + 1, TN, (rollSoFar + -10));
                  }
                  else {
                     chance += getOddsForTN(d4s, d6s, d8s, d10s, d12s, d20s, dBell, TN, (rollSoFar + roll));
                  }
               }
            }
            else {
               chance += 1;
            }
         }
         results = chance / dice;
         _map.put(key, results);
      }
      return results;
   }

   static final HashMap<DieType, Double> _EXPECTED = new HashMap<>();
   static {
      _EXPECTED.put(DieType.D1, 1.0);
      _EXPECTED.put(DieType.D4, 3.33333333277187);
      _EXPECTED.put(DieType.D6, 4.19999997624934);
      _EXPECTED.put(DieType.D8, 5.14285670500248);
      _EXPECTED.put(DieType.D10, 6.1111064);
      _EXPECTED.put(DieType.D12, 7.09088670602387);
      _EXPECTED.put(DieType.D20, 11.04965625);
      _EXPECTED.put(DieType.Dbell, 5.5);
   }

   public double getExpectedRoll() {
      double expectedRoll = 0;
      for (DieType dieType : DieType.values()) {
         expectedRoll += _diceCount.get(dieType) * _EXPECTED.get(dieType);
      }
      return expectedRoll;
   }

   static TreeSet<DiceSet> _diceAllowed = null;

   public static DiceSet getDieClosestToExpectedRoll(double expectedRoll) {
      if (_diceAllowed == null) {
         getAllowedDice();
      }

      double difference = 1000;
      double previousDifference = 1000;
      DiceSet previousDiceSet = null;
      for (DiceSet dice : _diceAllowed) {
         difference = dice.getExpectedRoll() - expectedRoll;
         if (difference == 0) {
            return dice;
         }
         if ((difference > 0) && (previousDiceSet == null)) {
            return dice;
         }

         if ((previousDifference < 0) && (difference > 0)) {
            // we are between the current and the previous dice
            if ((0 - previousDifference) > difference) {
               return dice;
            }
            return previousDiceSet;
         }
         previousDiceSet = dice;
         previousDifference = difference;
      }
      return null;
   }

   public static TreeSet<DiceSet> getAllowedDice() {
      if (_diceAllowed != null) {
         return _diceAllowed;
      }
      _diceAllowed = new TreeSet<>(new Comparator<>() {
         @Override
         public int compare(DiceSet dice1, DiceSet dice2) {
            return Double.compare(dice1.getExpectedRoll(), dice2.getExpectedRoll());
         }
      });
      // We allow dice sets that combine not more that two different types of dice
      // Further, we allow up to 6 total dice, in any combination

      // First, allow D4, D6 * D8 combination that subtract numbers from the die:
      _diceAllowed.add(new DiceSet("1d4-8"));
      _diceAllowed.add(new DiceSet("1d4-7"));
      _diceAllowed.add(new DiceSet("1d4-6"));
      _diceAllowed.add(new DiceSet("1d6-6"));
      _diceAllowed.add(new DiceSet("1d4-5"));
      _diceAllowed.add(new DiceSet("1d6-5"));
      _diceAllowed.add(new DiceSet("1d4-4"));
      _diceAllowed.add(new DiceSet("1d4-3"));
      _diceAllowed.add(new DiceSet("1d6-4"));
      _diceAllowed.add(new DiceSet("1d8-5"));
      _diceAllowed.add(new DiceSet("1d4-2"));
      _diceAllowed.add(new DiceSet("1d6-3"));
      _diceAllowed.add(new DiceSet("1d8-4"));
      _diceAllowed.add(new DiceSet("1d6-2"));
      _diceAllowed.add(new DiceSet("1d4-1"));
      _diceAllowed.add(new DiceSet("1d6-1"));

      // then, consider single dice types in multiples of 1,2,3,4,5 and 6:
      for (DieType die1 : new DieType[] { DieType.D4, DieType.D6, DieType.D8, DieType.D10, DieType.D12, DieType.D20}) {
         for (byte diceCount1 = 1; diceCount1 <= 6; diceCount1++) {
            DiceSet dice = DiceSet.getGroupDice(die1, diceCount1);
            if (dice.isInRange()) {
               _diceAllowed.add(dice);
            }
         }
      }

      // Now consider dice combinations of two different types
      for (DieType die1 : new DieType[] { DieType.D4, DieType.D6, DieType.D8, DieType.D10, DieType.D12}) {
         for (DieType die2 : new DieType[] { DieType.D6, DieType.D8, DieType.D10, DieType.D12, DieType.D20}) {
            if (die2.ordinal() <= die1.ordinal()) {
               continue;
            }
            // Some combinations should not be used:
            if ((die1 == DieType.D4) && (die2 == DieType.D8)) {
               continue; // use a  D6 instead of the D4 or  D8
            }
            if ((die1 == DieType.D4) && (die2 == DieType.D10)) {
               continue; // use a  D6 and D8 instead
            }
            if ((die1 == DieType.D4) && (die2 == DieType.D12)) {
               continue; // use a  D8 instead of the D4 or D12
            }
            if ((die1 == DieType.D4) && (die2 == DieType.D20)) {
               continue; // use a D12 instead of the D4 or D20
            }
            if ((die1 == DieType.D6) && (die2 == DieType.D10)) {
               continue; // use a  D8 instead of the D6 or D10
            }
            if ((die1 == DieType.D6) && (die2 == DieType.D12)) {
               continue; // use a  D8 and D10 instead
            }
            if ((die1 == DieType.D8) && (die2 == DieType.D12)) {
               continue; // use a D10 instead of the D8 or D12
            }
            // We always have at least one of each dice type, but never more than 5
            for (byte diceCount1 = 1; diceCount1 <= 5; diceCount1++) {
               for (byte diceCount2 = (byte) 1; diceCount2 <= (6 - diceCount1); diceCount2++) {

                  DiceSet dice = DiceSet.getGroupDice(die1, diceCount1);
                  dice = dice.addDie(DiceSet.getGroupDice(die2, diceCount2));
                  if (dice.isInRange()) {
                     _diceAllowed.add(dice);
                  }
               }
            }
         }
      }
      return _diceAllowed;
   }

   private boolean isInRange() {
      double expRoll = getExpectedRoll();
      switch (getDiceCount()) {
         case 1:  return (expRoll > 0.0) && (expRoll <= 6.5);
         case 2:  return (expRoll > 6.5) && (expRoll <= 12.5);
         case 3:  return (expRoll > 12.5) && (expRoll <= 18.5);
         case 4:  return (expRoll > 18.5) && (expRoll <= 28.5);
         case 5:  return (expRoll > 28.5) && (expRoll <= 35.5);
         case 6:  return (expRoll > 35.5) && (expRoll <= 99.9);
      }
      return false;
   }

   public int getDiceCount() {
      int count = 0;
      for (DieType dieType : new DieType[] { DieType.D4, DieType.D6, DieType.D8, DieType.D10, DieType.D12, DieType.D20, DieType.Dbell}) {
         count += _diceCount.get(dieType);
      }
      return count;
   }
}
