/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.offensive;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableHeader;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.server.Arena;

public class SpellSpeed extends ExpiringPriestSpell implements ICastInBattle
{
   public static final String NAME = "Speed";
   public SpellSpeed() {
      this(null, 0);
   }

   public SpellSpeed(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, (short)10/*baseExpirationTimeInTurns*/, (short)2/*bonusTimeInTurnsPerPower*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      byte deltaActionsPerTurn = getActionsChangePerTurnModifier(getPower());
      byte deltaActionsPerRound = getActionsChangePerRoundModifier(getPower());

      if (firstTime) {
         return getTargetName() + "'s speeds up: +" + deltaActionsPerTurn + " actions per turn, +"
                                + deltaActionsPerRound + " actions each round." ;
      }
      return "(actions increased " + deltaActionsPerRound + " per turn, " + deltaActionsPerRound + " each round)" ;
   }
   @Override
   public String describeSpell() {
      StringBuilder sb = new StringBuilder();
      sb.append("The 'Speed' spell increases the subject's available actions by an amount dependent upon the power:<br/>");
      Table table = new Table();
      TableRow header1 = new TableRow(-1, new TableHeader("Effective power").setRowSpan(2),
                                          new TableHeader("Actions increase").setColSpan(2));
      table.addRow(header1);
      table.addRow(new TableRow(-1, "Per turn", "Per round"));
      for (byte power=1 ; power<=8 ; power++) {
         table.addRow(new TableRow(power-1, ""+power,
                                            ""+getActionsChangePerTurnModifier(power),
                                            ""+getActionsChangePerRoundModifier(power)));
      }
      sb.append(table);

      return sb.toString();
   }

   @Override
   public void applyEffects(Arena arena) {
      byte deltaActionsPerTurn = getActionsChangePerTurnModifier(getPower());
      byte deltaActionsPerRound = getActionsChangePerRoundModifier(getPower());
      getTarget().getCondition().adjustActions(deltaActionsPerTurn, deltaActionsPerRound);
   }

   // These methods are used by spell that raise or lower a being's actions per turn (speed & slow spells):
   @Override
   public byte getModifiedActionsPerTurn(byte previousActionsPerTurn) {
      if (!isExpired()) {
         return (byte) (previousActionsPerTurn + getActionsChangePerTurnModifier(getPower()));
      }
      return previousActionsPerTurn;
   }
   @Override
   public byte getModifiedActionsPerRound(byte previousActionsPerRound) {
      if (!isExpired()) {
         return (byte) (previousActionsPerRound + getActionsChangePerRoundModifier(getPower()));
      }
      return previousActionsPerRound;
   }

   public static byte getActionsChangePerTurnModifier(byte power) {
      switch (power) {
         case 1: return 1;
         case 2: return 2;
         case 3: return 2;
         case 4: return 3;
         case 5: return 3;
         case 6: return 4;
         case 7: return 4;
         case 8: return 5;
      }
      return 0;
   }
   public static byte getActionsChangePerRoundModifier(byte power) {
      switch (power) {
         case 1: return 0;
         case 2: return 0;
         case 3: return 1;
         case 4: return 1;
         case 5: return 2;
         case 6: return 2;
         case 7: return 3;
         case 8: return 3;
  }
      return 0;
   }

   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE_FIGHTING;
   }


}
