package ostrowski.combat.common.spells;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Enums.TargetType;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.ArenaLocation;

public interface IAreaSpell
{
   public void setTargetLocation(ArenaLocation targetLocation, Arena _arena);
   public ArenaLocation getTargetLocation();
   public TargetType getTargetType();
   public byte getRadiusOfAffect();
   public void affectCharacterOnEntry(    Character enteringCharacter);
   public void affectCharacterOnExit(     Character exitingCharacter);
   public void affectCharacterOnRoundStart(Character characterInHex);
   public void affectCharacterOnRoundEnd(  Character characterInHex);
   public String getImageResourceName();
   public void affectCharacterOnActivation(Character chr);
   public void affectCharacterOnDeactivation(Character chr);
}
