package ostrowski.combat.common;

import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.protocol.DieRoll;
import ostrowski.combat.protocol.request.RequestDieRoll;
import ostrowski.combat.server.CombatServer;
import ostrowski.combat.server.Configuration;
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

   static        TreeSet<DiceSet>          diceAllowed = null;
   static final  HashMap<String, Double>   map         = new HashMap<>();
   private final HashMap<DieType, Integer> diceCount   = new HashMap<>() {
      {
         for (DieType dieType : DieType.values()) {
            put(dieType, 0);
         }
      }

      private static final long serialVersionUID = 1L;
   };
   public        double                    multiplier        = 1.0;
   public        StringBuilder             lastDieRolls      = null;
   private       boolean                   lastRollIsAllOnes = false;

   public DiceSet() {
   }

   public DiceSet(int d1, int d4, int d6, int d8, int d10, int d12, int d20, int dbell, double multiplier) {
      diceCount.put(DieType.D1, d1);
      diceCount.put(DieType.D4, d4);
      diceCount.put(DieType.D6, d6);
      diceCount.put(DieType.D8, d8);
      diceCount.put(DieType.D10, d10);
      diceCount.put(DieType.D12, d12);
      diceCount.put(DieType.D20, d20);
      diceCount.put(DieType.Dbell, dbell);
      this.multiplier = multiplier;
   }

   public DiceSet(String diceDescription) {
      for (DieType die : DieType.values()) {
         diceCount.put(die, 0);
      }
      multiplier = 1;

      diceDescription = diceDescription.trim();
      int locStart = diceDescription.indexOf('(');
      int locEnd = diceDescription.lastIndexOf(')');
      if ((locStart != -1) && (locEnd != -1)) {
         DiceSet pureDice = new DiceSet(diceDescription.substring(locStart + 1, locEnd));
         for (DieType die : DieType.values()) {
            diceCount.put(die, pureDice.diceCount.get(die));
         }
         multiplier *= pureDice.multiplier;

         String remainder = diceDescription.substring(locEnd + 1).trim();
         if ((remainder.charAt(0) == '/') || (remainder.charAt(0) == '*')) {
            String number = remainder.substring(1).trim();
            if (remainder.charAt(0) == '/') {
               multiplier /= Integer.parseInt(number);
            }
            else {
               multiplier *= Float.parseFloat(number);
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
                  diceCount.put(DieType.D1, nDieCount);
                  break;
               case 4:
                  diceCount.put(DieType.D4, nDieCount);
                  break;
               case 6:
                  diceCount.put(DieType.D6, nDieCount);
                  break;
               case 8:
                  diceCount.put(DieType.D8, nDieCount);
                  break;
               case 10:
                  diceCount.put(DieType.D10, nDieCount);
                  break;
               //case 10+: diceCount.put(DieType.Dbell, nDieCount); break;
               case 12:
                  diceCount.put(DieType.D12, nDieCount);
                  break;
               case 13:
                  diceCount.put(DieType.Dbell, nDieCount);
                  break;
               case 20:
                  diceCount.put(DieType.D20, nDieCount);
                  break;
            }
         }
      }
   }

   public int getDiceCount(DieType type) {
      return diceCount.get(type);
   }

   public DiceSet addBonus(int bonus) {
      if (bonus == 0) {
         return this;
      }
      return new DiceSet(diceCount.get(DieType.D1) + (int) (multiplier * bonus),
                         diceCount.get(DieType.D4), diceCount.get(DieType.D6),
                         diceCount.get(DieType.D8), diceCount.get(DieType.D10),
                         diceCount.get(DieType.D12), diceCount.get(DieType.D20),
                         diceCount.get(DieType.Dbell), multiplier);
   }

   public DiceSet addDie(DiceSet addend) {
      if (multiplier != addend.multiplier) {
         throw new InvalidParameterException("incompatible multipliers between added dice");
      }
      return new DiceSet(diceCount.get(DieType.D1) + addend.diceCount.get(DieType.D1),
                         diceCount.get(DieType.D4) + addend.diceCount.get(DieType.D4),
                         diceCount.get(DieType.D6) + addend.diceCount.get(DieType.D6),
                         diceCount.get(DieType.D8) + addend.diceCount.get(DieType.D8),
                         diceCount.get(DieType.D10) + addend.diceCount.get(DieType.D10),
                         diceCount.get(DieType.D12) + addend.diceCount.get(DieType.D12),
                         diceCount.get(DieType.D20) + addend.diceCount.get(DieType.D20),
                         diceCount.get(DieType.Dbell) + addend.diceCount.get(DieType.Dbell), multiplier);
   }

   public double getAverageRoll(boolean allowExplodes) {
      if (allowExplodes) {
         return getExpectedRoll();
      }
      double total = 0;
      for (DieType dieType : DieType.values()) {
         if (dieType == DieType.Dbell) {
            total += (1 + 10) * diceCount.get(dieType);
         }
         else {
            total += (1 + dieType.getSides()) * diceCount.get(dieType);
         }
      }
      return (total * multiplier) / 2.0;
   }

   @Deprecated
   public int roll(boolean allowExplodes, Character actor, RollType rollType) {
      return roll(allowExplodes, actor, rollType, null);
   }
   public int roll(boolean allowExplodes, Character actor, RollType rollType, String messageToRoller) {
      Map<DieType, List<List<Integer>>> results = new HashMap<>();
      int roll = roll(allowExplodes, results);
      if ((CombatServer._this != null) && (actor != null)) {
         if (Configuration.rollDice() && messageToRoller != null && !messageToRoller.isEmpty()) {
            RequestDieRoll requestDieRoll = new RequestDieRoll(messageToRoller, this, rollType);
            CombatServer._this.getArena().sendObjectToCombatant(actor, requestDieRoll);
            while (!requestDieRoll.isAnswered()) {
               try {
                  Thread.sleep(100);
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
            }
         }
         DieRoll dieRoll = new DieRoll(actor, 0/*dieColor*/, allowExplodes, rollType, results);
         CombatServer._this.getArena().sendEventToAllClients(dieRoll);
      }
      return roll;
   }
   private int roll(boolean allowExplodes, Map<DieType, List<List<Integer>>> results) {
      results.clear();
      lastRollIsAllOnes = true;
      lastDieRolls = new StringBuilder();
      lastDieRolls.append("{ ");
      int sum = diceCount.get(DieType.D1);
      boolean dieActuallyRolled = false;
      for (DieType dieType : new DieType[] { DieType.D4, DieType.D6, DieType.D8, DieType.D10, DieType.D12, DieType.D20, DieType.Dbell}) {
         List<List<Integer>> rollDataThisDieType = new ArrayList<>();
         results.put(dieType, rollDataThisDieType);
         for (int die = 0; die < diceCount.get(dieType); die++) {
            dieActuallyRolled = true;
            List<Integer> fullRollData = new ArrayList<>();
            rollDataThisDieType.add(fullRollData);
            int dieRoll = rollDie(allowExplodes /*up*/, allowExplodes /*down*/, dieType, fullRollData);
            sum += dieRoll;
            if ((dieRoll != 1) || (dieType == DieType.Dbell)) {
               lastRollIsAllOnes = false;
            }
         }
      }
      if (!dieActuallyRolled) {
         lastRollIsAllOnes = false;
      }
      int add = diceCount.get(DieType.D1);
      if (add != 0) {
         if (add > 0) {
            lastDieRolls.append("+");
         }
         lastDieRolls.append(add);
      }
      lastDieRolls.append("}");
      return (int) Math.floor((sum * multiplier) + .4999999);
   }

   private int rollDie(boolean allowExplodesUp, boolean allowExplodesDown, DieType dieType, List<Integer> fullRollData) {
      int sides = dieType.getSides();
      int roll = (int) (Math.floor(CombatServer.random() * sides)) + 1;
      fullRollData.add(roll);
      int modRoll = roll;
      if (dieType == DieType.Dbell) {
         if (roll == 11) {
            lastDieRolls.append("-");
            if (!allowExplodesDown) {
               return 0;
            }
            modRoll = -10;
            allowExplodesUp = false;
         }
         else if (roll == 12) {
            lastDieRolls.append("+");
            if (!allowExplodesUp) {
               return 10;
            }
            modRoll = 10;
            allowExplodesDown = false;
         }
         else {
            lastDieRolls.append(roll);
            if (allowExplodesDown && !allowExplodesUp) {
               // this change make a '7' rolled after a '-' be a -7.
               modRoll = 10 - modRoll;
            }
         }
         lastDieRolls.append("/10±");
      }
      else {
         lastDieRolls.append(modRoll).append("/").append(dieType.getSides());
      }

      lastDieRolls.append(", ");
      if (allowExplodesUp || (dieType == DieType.Dbell)) {
         if (allowExplodesUp && (roll == dieType.getSides())) {
            lastDieRolls.append("+");
            return modRoll + rollDie(allowExplodesUp, allowExplodesDown, dieType, fullRollData);
         }
         if ((roll == 11) && (dieType == DieType.Dbell)) {
            lastDieRolls.append("-");
            return modRoll + rollDie(allowExplodesUp, allowExplodesDown, dieType, fullRollData);
         }
      }
      return modRoll;
   }

   public String getLastDieRoll() {
      if (lastDieRolls == null) {
         return null;
      }
      return lastDieRolls.toString();
   }

   public boolean lastRollRolledAllOnes() {
      return lastRollIsAllOnes;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      int typesOfDice = 0;
      for (DieType dieType : new DieType[] { DieType.D4, DieType.D6, DieType.D8, DieType.D10, DieType.D12, DieType.D20, DieType.Dbell}) {
         if (diceCount.get(dieType) > 0) {
            if (sb.length() > 0) {
               sb.append(" + ");
            }
            sb.append(diceCount.get(dieType));
            if (dieType == DieType.Dbell) {
               sb.append("d10±");
            }
            else {
               sb.append('d').append(dieType.getSides());
            }
            typesOfDice++;
         }
      }
      if (diceCount.get(DieType.D1) != 0) {
         if (diceCount.get(DieType.D1) > 0) {
            sb.append("+");
         }
         sb.append(diceCount.get(DieType.D1));
      }
      if (multiplier != 1) {
         if (typesOfDice > 1) {
            sb.insert(0, '(');
            sb.append(')');
         }
         if (multiplier < 1) {
            sb.append(" / ").append((int) (1 / multiplier));
         }
         else {
            sb.append(" * ").append((int) multiplier);
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
      if (multiplier != other.multiplier) {
         return false;
      }
      for (DieType dieType : DieType.values()) {
         if (!diceCount.get(dieType).equals(other.diceCount.get(dieType))) {
            return false;
         }
      }
      return true;
   }

   @Override
   public int hashCode() {
      int hash = (int) (multiplier * 100);
      for (DieType dieType : DieType.values()) {
         hash = hash << 2;
         hash += diceCount.get(dieType);
      }
      return hash;
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         for (DieType dieType : DieType.values()) {
            diceCount.put(dieType, readInt(in));
         }
         multiplier = readDouble(in);
         lastDieRolls = new StringBuilder(readString(in));
         if (lastDieRolls.length() == 0) {
            lastDieRolls = null;
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         for (DieType dieType : DieType.values()) {
            writeToStream(diceCount.get(dieType), out);
         }
         writeToStream(multiplier, out);
         if (lastDieRolls == null) {
            writeToStream("", out);
         }
         else {
            writeToStream(lastDieRolls.toString(), out);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public double getOddsForTN(double TN) {
      return getOddsForTN(diceCount.get(DieType.D4), diceCount.get(DieType.D6), diceCount.get(DieType.D8), diceCount.get(DieType.D10),
                          diceCount.get(DieType.D12), diceCount.get(DieType.D20), diceCount.get(DieType.Dbell), TN - diceCount.get(DieType.D1), // TN
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
      Double results = map.get(key);
      if (results == null) {
         double chance = 0.0;
         int dice = diceCount.get(die);
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
         map.put(key, results);
      }
      return results;
   }

   static final HashMap<DieType, Double> EXPECTED = new HashMap<>();
   static {
      EXPECTED.put(DieType.D1, 1.0);
      EXPECTED.put(DieType.D4, 3.33333333277187);
      EXPECTED.put(DieType.D6, 4.19999997624934);
      EXPECTED.put(DieType.D8, 5.14285670500248);
      EXPECTED.put(DieType.D10, 6.1111064);
      EXPECTED.put(DieType.D12, 7.09088670602387);
      EXPECTED.put(DieType.D20, 11.04965625);
      EXPECTED.put(DieType.Dbell, 5.5);
   }

   public double getExpectedRoll() {
      double expectedRoll = 0;
      for (DieType dieType : DieType.values()) {
         expectedRoll += diceCount.get(dieType) * EXPECTED.get(dieType);
      }
      return expectedRoll;
   }

   public static DiceSet getDieClosestToExpectedRoll(double expectedRoll) {
      if (diceAllowed == null) {
         getAllowedDice();
      }

      double difference;
      double previousDifference = 1000;
      DiceSet previousDiceSet = null;
      for (DiceSet dice : diceAllowed) {
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
      if (diceAllowed != null) {
         return diceAllowed;
      }
      diceAllowed = new TreeSet<>((dice1, dice2) -> Double.compare(dice1.getExpectedRoll(), dice2.getExpectedRoll()));
      // We allow dice sets that combine not more that two different types of dice
      // Further, we allow up to 6 total dice, in any combination

      // First, allow D4, D6 * D8 combination that subtract numbers from the die:
      diceAllowed.add(new DiceSet("1d4-8"));
      diceAllowed.add(new DiceSet("1d4-7"));
      diceAllowed.add(new DiceSet("1d4-6"));
      diceAllowed.add(new DiceSet("1d6-6"));
      diceAllowed.add(new DiceSet("1d4-5"));
      diceAllowed.add(new DiceSet("1d6-5"));
      diceAllowed.add(new DiceSet("1d4-4"));
      diceAllowed.add(new DiceSet("1d4-3"));
      diceAllowed.add(new DiceSet("1d6-4"));
      diceAllowed.add(new DiceSet("1d8-5"));
      diceAllowed.add(new DiceSet("1d4-2"));
      diceAllowed.add(new DiceSet("1d6-3"));
      diceAllowed.add(new DiceSet("1d8-4"));
      diceAllowed.add(new DiceSet("1d6-2"));
      diceAllowed.add(new DiceSet("1d4-1"));
      diceAllowed.add(new DiceSet("1d6-1"));

      // then, consider single dice types in multiples of 1,2,3,4,5 and 6:
      for (DieType die1 : new DieType[] { DieType.D4, DieType.D6, DieType.D8, DieType.D10, DieType.D12, DieType.D20}) {
         for (byte diceCount1 = 1; diceCount1 <= 6; diceCount1++) {
            DiceSet dice = DiceSet.getGroupDice(die1, diceCount1);
            if (dice.isInRange()) {
               diceAllowed.add(dice);
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
                  if (dice != null) {
                     dice = dice.addDie(DiceSet.getGroupDice(die2, diceCount2));
                     if (dice != null) {
                        if (dice.isInRange()) {
                           diceAllowed.add(dice);
                        }
                     }
                  }
               }
            }
         }
      }
      return diceAllowed;
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
         count += diceCount.get(dieType);
      }
      return count;
   }
}
