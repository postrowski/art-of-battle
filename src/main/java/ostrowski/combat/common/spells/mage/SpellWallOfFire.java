/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.spells.IAreaSpell;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.common.wounds.WoundCharts;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.combat.server.ArenaLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellWallOfFire extends ExpiringMageSpell implements IAreaSpell, ICastInBattle
{
   private ArenaLocation      targetLocation;
   private Arena              arena;
   public static final String NAME = "Wall Of Fire";
   public SpellWallOfFire() {
      super(NAME, (short)10/*baseExpirationTimeInTurns*/,
            (short)(5)/*bonusTimeInTurnsPerPower*/,
            new Class[] {SpellCreateFire.class, SpellControlFire.class,
                               SpellControlTemperature.class, SpellThrowSpell.class},
                               new MageCollege[] {MageCollege.CONJURATION, MageCollege.FIRE});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getCasterName() + " creates a wall of fire (level " + getPower() +
                "), with a radius equal to the casting power of " + getPower();
      }
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell causes a wall of fire to fill several hexes, 6 feet high." +
               " The fire is thigh enough to partially obscure vision of things on the other side." +
               " This obscurity gives a PD bonus of +1 per hex to any target that is on the other side of the fire." +
               " At one power point, the spell covers 3 hexes anywhere the caster chooses." +
               " The farthest affected hex may be up to 8 hexes away." +
               " Each additional point of power either doubles the size of the affected area or the maximum distance." +
               " So, a 3-power spell could cover 12 hexes within 8 hexes, or 6 hexes up to 16 hexes away, or 3 hexes up to 32 hexes away." +
               " Anyone travelling the fire hex will take 1d6 damage." +
               " Anyone passing through multiple hexes, takes 1d6 per hex through which they pass, up to 3d6 maximum." +
               " If anyone is not able to exit the fire by the end of their movement round, they suffer the maximum 3d6 damage total." +
               " Armor protects against this damage, but Racial Build Adjustment does not.";
   }

   @Override
   public void applyEffects(Arena arena) {
      CombatMap map = arena.getCombatMap();
      short minCol = (short) Math.max(             0, ((targetLocation.x - (getPower() * 2)) + 2));
      short maxCol = (short) Math.min(map.getSizeX(), ((targetLocation.x + (getPower() * 2)) - 2));
      short minRow = (short) Math.max(             0, ((targetLocation.y - (getPower() * 2)) + 2));
      short maxRow = (short) Math.min(map.getSizeY(), ((targetLocation.y + (getPower() * 2)) - 2));
      for (short col = minCol ; col<=maxCol ; col++) {
         for (short row = minRow ; row<=maxRow ; row++) {
            if ((row % 2) != (col % 2)) {
               row++;
            }
            ArenaLocation mapLoc = map.getLocationQuick(col, row);
            //mapLoc.registerAsWatcher(this, null);
            if (mapLoc != null) {
               short dist = ArenaCoordinates.getDistance(mapLoc, targetLocation);
               if (dist < getPower()) {
                  mapLoc.addSpell(this);
               }
            }
         }
      }
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_AREA;
   }

   @Override
   public RANGE getRange(short distanceInHexes) {
      return RANGE.SHORT;
   }

   @Override
   public boolean requiresTargetToCast() {
      return false;
   }

   @Override
   public void setTargetLocation(ArenaLocation targetLocation, Arena arena) {
      this.targetLocation = targetLocation;
      this.arena = arena;
   }

   @Override
   public ArenaLocation getTargetLocation() {
      return targetLocation;
   }

   @Override
   public short getMaxRange(Character caster) {
      return (short) (8 + (getPower() * 3));
   }

   @Override
   public short getMinRange(Character caster) {
      return 3;
   }

   @Override
   public byte getRadiusOfAffect() {
      return getPower();
   }

   private final HashMap<Character, Integer> timeInHexPerCharacter = new HashMap<>();
   private void applyDamage(Character charToTakeDamage) {
      Integer timeInHex = timeInHexPerCharacter.remove(charToTakeDamage);
      if (timeInHex == null) {
         timeInHex = 1;
      }
      else if (timeInHex > 3) {
         timeInHex = 3;
      }
      StringBuilder sb = new StringBuilder();
      // roll 1d6 per time in hex
      DiceSet damageDice = new DiceSet(0, 0, timeInHex, 0, 0, 0, 0, 0/*dBell*/, 1.0);
      String rollMessage = getCasterName() + ", " + charToTakeDamage.getName() + " is in the area of effect of your " +
                           getName() + " spell, roll damage.";
      int damage = damageDice.roll(true/*allowExplodes*/, getCaster(), RollType.DAMAGE_SPELL, rollMessage);
      sb.append("Damage rolled is ").append(damageDice);
      sb.append(", rolling ").append(damageDice.getLastDieRoll());
      sb.append(", for a total of ").append(damage).append(" fire damage.<br/>");


      StringBuilder alterationExplanationBuffer = new StringBuilder();

      byte defenderBuild = charToTakeDamage.getBuild(DamageType.FIRE);
      defenderBuild = (byte) (defenderBuild - charToTakeDamage.getRace().getBuildModifier());
      byte reducedDamage = (byte) (damage - defenderBuild);
      sb.append(charToTakeDamage.getName()).append(" has a build vs. ").append(DamageType.FIRE.fullname);
      sb.append(" of ").append(defenderBuild);

      if (reducedDamage < 0) {
         sb.append(", which blocks all damage.");
      }
      else {
         sb.append(", reducing the damage to ").append(reducedDamage).append("<br/>");

         Wound wound = WoundCharts.getWound(reducedDamage, DamageType.FIRE, charToTakeDamage, alterationExplanationBuffer);
         if (wound != null) {
            sb.append(charToTakeDamage.getName()).append(" suffers the following wound:<br/>");
            sb.append(wound.describeWound());
            if (alterationExplanationBuffer.length() > 0) {
               sb.append(", which is modified by ").append(alterationExplanationBuffer);
            }
            sb.append("<br/>");
            List<Wound> woundsList = new ArrayList<>();
            woundsList.add(wound);
            Map<Character, List<Wound>> wounds = new HashMap<>();
            wounds.put(charToTakeDamage, woundsList);
            arena.battle.applyWounds(wounds);
         }
      }
      arena.sendMessageTextToAllClients(sb.toString(), false/*popUp*/);
      sb.setLength(0);
   }

   @Override
   public void affectCharacterOnEntry(Character enteringCharacter) {
      Integer timeInHex = timeInHexPerCharacter.get(enteringCharacter);
      if (timeInHex == null) {
         timeInHex = 1;
      }
      else {
         timeInHex++;
      }
      timeInHexPerCharacter.put(enteringCharacter, timeInHex);
   }

   @Override
   public void affectCharacterOnExit(Character exitingCharacter) {
      applyDamage(exitingCharacter);
   }

   @Override
   public void affectCharacterOnRoundStart(Character characterInHex) {
      affectCharacterOnEntry(characterInHex);
   }

   @Override
   public void affectCharacterOnRoundEnd(Character characterInHex) {
      applyDamage(characterInHex);
   }

   @Override
   public String getImageResourceName() {
      return "/res/spell_fire.png";
   }

   @Override
   public void affectCharacterOnActivation(Character chr) {
      affectCharacterOnEntry(chr);
   }

   @Override
   public void affectCharacterOnDeactivation(Character chr) {
   }
}
