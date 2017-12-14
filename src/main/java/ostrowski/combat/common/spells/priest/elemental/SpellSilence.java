package ostrowski.combat.common.spells.priest.elemental;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;

public class SpellSilence extends ExpiringPriestSpell
{
   public static final String NAME = "Silence";

   public SpellSilence() {
      this(null, 0);
   }

   public SpellSilence(Class<PriestElementalSpell> group, int affinity) {
      super(NAME, (short)5/*baseExpirationTimeInTurns*/, (short)5/*bonusTimeInTurnsPerPower*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell causes all air pressure waves to disappear." +
      		" The end result is that no sound passes through the area of effect." +
      		" This effects all sounds emanating from or entering the area of effect." +
      		" The more power put into the spell, the larger the area of effect, or the longer the spell remains in effect." +
      		" At 1 point of power, the area of effect is a circle with a diameter of 1 hex, lasting for 5 turns." +
      		" Each additional point of power either doubles the diameter of the area, or doubles the duration." +
      		" In addition, one point of power may be spent to allow the spell to follow a mobile target." +
      		" The spell will then follow the target wherever it goes." +
      		"<br/>For example, a 3-power spell could be cast in any of the following permutations:<ul>" +
      		"<li>1-hex diameter, lasting for 20 turns." +
      		"<li>2-hex diameter, lasting for 10 turns." +
      		"<li>3-hex diameter, lasting for 5 turns." +
      		"<li>1-hex diameter, lasting for 10 turns, following a character." +
      		"<li>2-hex diameter, lasting for 5 turns, following a character." +
      		"</ul>" +
      		" This spell will deaden all sounds that pass through an area, but does not prevent sounds from bouncing around the area of affect." +
      		" So if a spell is cast in the front doorway of a house, no sounds will pass through the doorway," +
      		" however, sounds from outside may still enter the house through the walls or windows.";
   }


   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE;
   }


}
