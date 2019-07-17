/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableHeader;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.server.Arena;

public class SpellSlow extends ResistedMageSpell implements ICastInBattle
{
   public static final String NAME = "Slow";
   public SpellSlow() {
      super(NAME, Attribute.Health, (byte) 2/*resistedActions*/, true/*expires*/,
            new Class[] {SpellAffectArea.class, SpellControlTime.class},
            new MageCollege[] {MageCollege.CONJURATION, MageCollege.ENERGY, MageCollege.EVOCATION, MageCollege.PROTECTION});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      byte deltaActionsPerTurn = getActionsChangePerTurnModifier(getPower());
      byte deltaActionsPerRound = getActionsChangePerRoundModifier(getPower());
      if (firstTime) {
         return getTargetName() + "'s slows down: " + (0-deltaActionsPerTurn) + " actions per turn, " +
                (0-deltaActionsPerRound) + " actions each round." ;
      }
      return "(actions reduced " + (0-deltaActionsPerTurn) + " per turn, " + (0-deltaActionsPerRound) + " each round.)" ;
   }
   @Override
   public String describeSpell() {
      StringBuilder sb = new StringBuilder();
      sb.append("The 'Slow' spell reduces the subject's available actions by an amount dependent upon the power:<br/>");
      Table table = new Table();
      TableRow header1 = new TableRow(-1);
      TableRow header2 = new TableRow(-1);
      table.addRow(header1);
      table.addRow(header2);
      header1.addHeader(new TableHeader("Power").setRowSpan(2));
      header1.addHeader(new TableHeader("Action reduction").setColSpan(2));
      header2.addHeader(new TableHeader("Per turn")).addHeader("Per round");
      for (byte power=1 ; power<9 ; power++) {
         table.addRow(new TableRow(power-1, ""+power,
                                            ""+(0-getActionsChangePerTurnModifier(power)),
                                            ""+(0-getActionsChangePerRoundModifier(power))));
      }
      sb.append(table.toString());
      return sb.toString();
   }

   @Override
   public void applyEffects(Arena arena) {
      byte deltaActionsPerTurn = getActionsChangePerTurnModifier(getPower());
      byte deltaActionsPerRound = getActionsChangePerRoundModifier(getPower());
      getTarget().getCondition().adjustActions(deltaActionsPerTurn, deltaActionsPerRound);
   }

   @Override
   public void removeEffects(Arena arena) {
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
         case 1: return -1;
         case 2: return -2;
         case 3: return -2;
         case 4: return -3;
         case 5: return -3;
         case 6: return -4;
         case 7: return -4;
         case 8: return -5;
      }
      return 0;
   }
   public static byte getActionsChangePerRoundModifier(byte power) {
      switch (power) {
         case 1: return 0;
         case 2: return 0;
         case 3: return -1;
         case 4: return -1;
         case 5: return -2;
         case 6: return -2;
         case 7: return -3;
         case 8: return -3;
      }
      return 0;
   }

}
