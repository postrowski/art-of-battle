package ostrowski.combat.common.spells.priest.offensive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.SpecialDamage;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.common.wounds.WoundChart;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.BattleTerminatedException;

public class SpellCallLightning extends PriestSpell implements IRangedSpell, ICastInBattle
{
   public static final String NAME = "Call Lightning";
   public SpellCallLightning() {};
   public SpellCallLightning(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      return "The '"+getName()+"' spell brings a number of lightning bolts down upon the target."+
             " The number of lightning bolts to come down is equal to the effective power of the casting for the range of the target." +
             " A d12 is rolled for each bolt. If the die roll is equal to or less than the caster's Divine Affinity, the bolt hits the target." +
             " Lightning bolts may not be defended against, because they happen instantaneously." +
             " Each bolt that hits the subject does 2d12 damage. If multiple bolts hit the subject, their damages are computed individually." +
             "<br/>If a bolt misses, a d4 is rolled to determine how many hexes away the bolt lands, and then another die is rolled to see in which direction the bolt missed.";
   }
   @Override
   public short getRangeBase() {
      return 10;
   }
   @Override
   public byte getRangeDefenseAdjustmentToPD(short distanceInHexes) {
      return 0;
   }
   @Override
   public byte getRangeDefenseAdjustmentPerAction(short distanceInHexes) {
      return 0;
   }
   @Override
   public void applySpell(Character target, Arena arena) throws BattleTerminatedException
   {
      StringBuilder sb = new StringBuilder();
      sb.append(getCasterName()).append(" casts a 'Call Lightning' spell on ").append(getTargetName());
      byte effectivePower = getEffectivePower();
      sb.append(". ").append(getCasterName()).append(" calls down ").append(effectivePower).append(" bolts:");
      arena.sendMessageTextToAllClients(sb.toString(), false/*popUp*/);
      sb.setLength(0);
      for (byte i=0 ; i<effectivePower ; i++) {
         switch (i) {
            case 0: sb.append("<br/>The first bolt rolls a ");   break;
            case 1: sb.append("<br/>The second bolt rolls a ");  break;
            case 2: sb.append("<br/>The third bolt rolls a ");   break;
            case 3: sb.append("<br/>The fourth bolt rolls a ");  break;
            case 4: sb.append("<br/>The fifth bolt rolls a ");   break;
            case 5: sb.append("<br/>The sixth bolt rolls a ");   break;
            case 6: sb.append("<br/>The seventh bolt rolls a "); break;
            case 7: sb.append("<br/>The eigth bolt rolls a ");   break;
         }
         DiceSet dice = new DiceSet(0, 0, 0, 0, 0, 1, 0, 0/*dBell*/, 1.0);
         int roll = dice.roll(false/*allowExplodes*/);
         sb.append(dice.getLastDieRoll());
         Character hitCharacter = null;
         if (roll <= getCastingLevel()) {
            sb.append(" which is below or equal to ").append(getCaster().getHisHer()).append(" affinity level of ").append(getCastingLevel()).append(" so the bolt hits ").append(getTargetName());
            hitCharacter = getTarget();
         }
         else {
            sb.append(" which is above ").append(getCaster().getHisHer()).append(" affinity level of ").append(getCastingLevel()).append(" so the bolt misses the target.");
            DiceSet distDice = new DiceSet("1d4");
            int distance = distDice.roll(false/*allowExplodes*/);
            sb.append(" a d4 is rolled, rolling a ").append(distance);
            List<Character> charsAtRange =  new ArrayList<>();
            for (Character other : arena.getCombatants()) {
               short minDist = Arena.getMinDistance(target, other);
               short maxDist = Arena.getMaxDistance(target, other);
               // large target example: target is 3-5 hexes away. distances of 3,4&5 should be in range:
               if ((minDist <= distance) && (maxDist >= distance)) {
                  charsAtRange.add(other);
               }
            }
            if (charsAtRange.size() == 0) {
               sb.append(". No characters exist at ").append(distance).append(" hexes away from ").append(target.getName());
            }
            else {
               int hexesCount = distance * 6;
               sb.append(". At a range of ").append(distance).append(", there are ").append(hexesCount);
               sb.append(" hexes, with these characters in them: ");
               boolean first = true;
               for (Character chr : charsAtRange) {
                  if (!first) {
                     sb.append(" ,");
                  }
                  first = false;
                  sb.append(chr.getName());
               }
               DiceSet targetDice = null;
               if (distance == 1) {
                  targetDice = new DiceSet("1d6");
               }
               if (distance == 2) {
                  targetDice = new DiceSet("1d12");
               }
               if (distance == 3) {
                  targetDice = new DiceSet("1d20");
               }
               if (distance == 4) {
                  targetDice = new DiceSet("1d20");
               }
               int targetIndex = targetDice.roll(false/*allowExplodes*/);
               sb.append("<br/>Rolls a ").append(targetDice.toString());
               sb.append(", rolling ").append(targetDice.getLastDieRoll());
               if (charsAtRange.size() < targetIndex) {
                  sb.append(", missing all other characters.");
               }
               else {
                  hitCharacter = charsAtRange.get(targetIndex);
                  sb.append(" which hits ").append(hitCharacter.getName());
               }
            }
         }
         HashMap<Character, ArrayList<Wound>> wounds = new HashMap<>();
         if (hitCharacter != null) {
            DiceSet damageDice = new DiceSet(0, 0, 0, 0, 0, 2, 0, 0/*dBell*/, 1.0);
            boolean computeDamage = false;
            if (!computeDamage) {
               byte successRollOverTN = (byte) (getCastingLevel() - roll);
               arena._battle.resolveDamage(getCaster(), hitCharacter, "Lightning bolt", "2d12",
                                           0/*baseDamage*/, (byte)0/*bonusDamage*/, damageDice, DamageType.ELECTRIC/*damageType*/,
                                           new SpecialDamage(SpecialDamage.MOD_NONE), ""/*specialDamageModifierExplanation*/,
                                           sb, wounds, successRollOverTN, false/*isCharge*/);
            }
            else {
               damageDice = getCaster().adjustDieRoll(damageDice, RollType.DAMAGE_SPELL, hitCharacter/*target*/);
               int damage = damageDice.roll(true/*allowExplodes*/);
               sb.append(" Damage rolled is ").append(damageDice.toString());
               sb.append(", rolling ").append(damageDice.getLastDieRoll());
               sb.append(", for a total of ").append(damage).append(" electrical damage.<br/>");

               byte defenderBuild = hitCharacter.getBuild(DamageType.ELECTRIC);
               byte reducedDamage = (byte)(damage - defenderBuild);
               sb.append(hitCharacter.getName()).append(" has a build vs. ").append(DamageType.ELECTRIC.fullname);
               sb.append(" of ").append(defenderBuild);
               if (reducedDamage < 0) {
                  sb.append(", which blocks all damage.");
               }
               else {
                  sb.append(", reducing the damage to ").append(reducedDamage).append("<br/>");

                  StringBuilder alterationExplanationBuffer = new StringBuilder();
                  Wound wound = WoundChart.getWound(reducedDamage, DamageType.ELECTRIC, hitCharacter, alterationExplanationBuffer);
                  sb.append(hitCharacter.getName()).append(" suffers the following wound:<br/>");
                  sb.append(wound.describeWound());
                  if (alterationExplanationBuffer.length() > 0) {
                     sb.append(", which is modified by ").append(alterationExplanationBuffer.toString());
                  }
                  sb.append("<br/>");

                  ArrayList<Wound> woundsList = wounds.get(hitCharacter);
                  if (woundsList == null) {
                     woundsList = new ArrayList<>();
                     wounds.put(hitCharacter, woundsList);
                  }
                  woundsList.add(wound);
               }
            }
         }
         arena.sendMessageTextToAllClients(sb.toString(), false/*popUp*/);
         sb.setLength(0);
         arena._battle.applyWounds(wounds);
      }
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OTHER_FIGHTING;
   }
}
