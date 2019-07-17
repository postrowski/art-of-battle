package ostrowski.combat.common.spells.priest;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterGenerator;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.enums.AI_Type;
import ostrowski.combat.common.enums.Facing;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.spells.priest.offensive.SpellSummonChampion;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.CombatServer;

public abstract class SpellSummonBeing extends ExpiringPriestSpell implements ICastInBattle
{
   public SpellSummonBeing() {};
   public SpellSummonBeing(String subClassName, Class<? extends IPriestGroup> group, int affinity) {
      super(subClassName, (short)10/*baseExpirationTimeInTurns*/, (short)10 /*bonusTimeInTurnsPerPower*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      StringBuilder sb = new StringBuilder();
      sb.append("The '").append(getName()).append("' spell brings a ").append(summonedTypeName());
      sb.append(" chosen by the deity to aid the priest.");
      if (this instanceof SpellSummonChampion) {
         sb.append(" It is up to the GM to decide the nature of the ").append(summonedTypeName()).append(".");
         sb.append(" It may be a warrior, archer, mage, even a thief, or anything else that the GM sees fit for the given situation.");
      }
      sb.append(" The more power put into the spell, the stronger the ").append(summonedTypeName()).append(" will be.");
      sb.append(" In general, each point of the spell corresponds to ").append(getCharPointsPerPowerPoint()).append(" character points in the character summoned.");
      sb.append(" Exceptions to the point value of the summoned ").append(summonedTypeName()).append(" may be made by the GM for exceptions condition.");
      sb.append(" The summoned ").append(summonedTypeName()).append(" will always act in the best interests of the deity.");
      sb.append(" Once the task has been finished (the battle won, etc.), the ").append(summonedTypeName()).append(" will disappear.");
      sb.append(" A Priest may only summon a single ").append(summonedTypeName()).append(" at one time.");
      return sb.toString();
   }

   @Override
   public void applyEffects(Arena arena) {
      int pointTotal = getBaseCharPoints() + (getPower() * getCharPointsPerPowerPoint());
      String charSoruce = pointTotal + "  " + summonedTypeRaceName();
      Character summonedBeing = CharacterGenerator.generateRandomCharacter(charSoruce, arena, true/*printCharacter*/);
      ArenaLocation loc = ArenaLocation.getForwardMovement(getCaster().getHeadCoordinates(), getCaster().getFacing(), arena.getCombatMap());
      loc = (ArenaLocation) loc.clone();
      int count = 0;
      while (!arena.addCombatant(summonedBeing, _caster._teamID, loc._x, loc._y, AI_Type.NORM/*aiEngineType*/)) {
         Facing direction = Facing.getByValue((byte) (CombatServer.random() * Facing.values().length));
         loc._x += direction.moveX;
         loc._y += direction.moveY;
         if (count ++ > 20) {
            arena.sendMessageTextToAllClients(getCasterName() + "'s " + getName() +
                                              " spell failed, because there is no room for the "
                                              +summonedTypeName()+" to appear.", false/*popUp*/);
            return;
         }
      }
      arena.sendMessageTextToAllClients(getCasterName() + " casts a " + getName() +
                                        " spell, causing " + summonedBeing.getName() + " to enter: " + summonedBeing.print(), false/*popUp*/);
   }
   @Override
   public void removeEffects(Arena arena) {
      super.removeEffects(arena);
   }

   public abstract String summonedTypeName();
   public abstract String summonedTypeRaceName();
   public abstract int getCharPointsPerPowerPoint();
   public int getBaseCharPoints() {
      Race race = Race.getRace(summonedTypeName(), Gender.MALE);
      if (race != null) {
         if (race.isNpc()) {
            return race.getCost() - getCharPointsPerPowerPoint();
         }
      }
      return 0;
   }
   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public boolean requiresTargetToCast() {
      return false;
   }

   @Override
   public Spell getActiveSpellIncompatibleWith(Character target) {
      // a priest may have up to 3 of any single summoned type active at one time.
      int maximumSummonedBeingsAllowed = 3;
      for (Spell currentActiveSpell : target.getActiveSpells()) {
         if (currentActiveSpell.getClass() == this.getClass()) {
            if (--maximumSummonedBeingsAllowed == 0) {
               return currentActiveSpell;
            }
         }
         else {
            if (isIncompatibleWith(currentActiveSpell)) {
               if (currentActiveSpell.takesPrecedenceOver(this)) {
                  return currentActiveSpell;
               }
            }
         }
      }
      return null;
   }

}
