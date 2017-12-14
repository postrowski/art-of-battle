/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.server.Arena;

public class SpellFlight extends ExpiringMageSpell
{
   public static final String NAME = "Flight";
   public SpellFlight() {
      super(NAME, (short)10, (short)10, new Class[] {SpellCreateForce.class, SpellLevitate.class},
            new MageCollege[] {MageCollege.AIR, MageCollege.EVOCATION, MageCollege.ENERGY});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTargetName() + " begins to fly.";
      }
      return getTargetName() + " is flying.";
   }
   @Override
   public String describeSpell() {
      StringBuilder sb = new StringBuilder();
      sb.append("The 'Flight' spell allows the subject to fly. ");
      sb.append("The speed at which the subject can fly is dependent upon the power put into the spell:<br/>");
      Table table = new Table();
      table.addRow(new TableRow(-1, "Power", "Movement rate"));
      for (byte power = 1; power < 8; power++) {
         table.addRow(new TableRow(power-1, ""+power, ""+getMovementRate(power)));
      }
      sb.append(table.toString());
      sb.append("<br/>");
      sb.append("The subject of the spell may change their altitude my spending their movement in the vetical");
      sb.append(" direction, changing their altitude by (up to) 3 feet for each movement point spent. ");
      sb.append("The spell gives the subject a warning that it will expire, a number");
      sb.append(" of turns prior to its expiry equal to the power put into the spell. ");
      sb.append("If the subject of the spell is still high in the air when the spell expires,");
      sb.append(" they will fall to the ground, and will likely not survive the impact.");
      return sb.toString();
   }

   private static byte getMovementRate(byte power) {
      return (byte)(power * 2);
   }

   @Override
   public void applyEffects(Arena arena) {
   }
   @Override
   public void removeEffects(Arena arena) {
   }

   @Override
   public byte getModifiedMovementPerRound(byte previousMovementRate) {
      if (!isExpired() && (previousMovementRate>0)) {
         return getMovementRate(getPower());
      }
      return previousMovementRate;
   }

   @Override
   public boolean isBeneficial() {
      return true;
   }

   @Override
   public Boolean isCastInBattle() {
      return true;
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE_FIGHTING;
   }

   @Override
   public boolean isIncompatibleWith(Spell spell) {
      if (spell.getClass() == SpellLevitate.class) {
         return true;
      }
      return super.isIncompatibleWith(spell);
   }

   @Override
   public boolean takesPrecedenceOver(Spell spell) {
      if (spell.getClass() == SpellLevitate.class) {
         return true;
      }
      return super.takesPrecedenceOver(spell);
   }
}
