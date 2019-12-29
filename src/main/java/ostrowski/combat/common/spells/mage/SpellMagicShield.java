/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.things.Hand;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Shield;
import ostrowski.combat.common.things.Thing;
import ostrowski.combat.server.Arena;

public class SpellMagicShield extends ExpiringMageSpell implements ICastInBattle
{
   public static final String NAME = "Magic Shield";
   public SpellMagicShield() {
      super(NAME, (short)10/*baseExpirationTimeInTurns*/, (short)5/*bonusTimeInTurnsPerPower*/,
            new Class[] {SpellCreateEarth.class, SpellCreateForce.class, SpellPush.class},
            new MageCollege[] {MageCollege.EARTH, MageCollege.ENERGY, MageCollege.PROTECTION});
   }


   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      Table table = new Table();
      table.addRow(new TableRow(-1, "Power", "Shield type"));
      for (int p=1 ; p<Shield._shieldList.length ; p++) {
         table.addRow(new TableRow(p-1).addHeader("" + p)
                                       .addTD(new TableData(getDescriptionForpower(p)).setAlignLeft()));
      }
      return "The '" + getName() + "' spell creates a weightless magical shield in the caster's hand that can be used like a normal shield." +
             " The power put into the spell determines the size of the shield, and thus the PD of the shield." +
             table;
   }
   private static String getDescriptionForpower(int p) {
      if ((p == 0) || (p >= Shield._shieldList.length)) {
         return "";
      }
      Shield shield = Shield._shieldList[p];
      if (shield != null) {
         return shield._name + " (PD " + shield._passiveDefense + ")";
      }
      return "";
   }

   @Override
   public void applyEffects(Arena arena) {
      Hand hand = (Hand) getCaster().getLimb(LimbType.HAND_LEFT);
      String shieldType = null;
      int power = getPower();
      if (power > 5) {
         power= 5;
      }
      switch (power) {
         case 1: shieldType = "Buckler"; break;
         case 2: shieldType = "Small Shield"; break;
         case 3: shieldType = "Medium Shield"; break;
         case 4: shieldType = "Large Shield"; break;
         case 5: shieldType = "Tower Shield"; break;
      }
      Shield magicShield = Shield.getShield(shieldType, getCaster().getRace());
      magicShield._name   = "Magic " + shieldType;
      magicShield._weight = 0;
      magicShield._cost   = 1;// don't set to zero, because that causes isReal() to return false.
      hand.setHeldThing(magicShield, getCaster());
      getCaster().refreshDefenses();
   }
   @Override
   public void removeEffects(Arena arena) {
      Hand hand = (Hand) getCaster().getLimb(LimbType.HAND_LEFT);
      Thing heldThing = hand.getHeldThing();
      if (heldThing.getName().startsWith("Magic ")) {
         hand.setHeldThing(null, getCaster());
         getCaster().refreshDefenses();
      }
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_SELF;
   }

}
