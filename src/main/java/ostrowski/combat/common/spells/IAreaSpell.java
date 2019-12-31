package ostrowski.combat.common.spells;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Enums.TargetType;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.ArenaLocation;

public interface IAreaSpell extends Cloneable
{
   void setTargetLocation(ArenaLocation targetLocation, Arena _arena);
   ArenaLocation getTargetLocation();
   TargetType getTargetType();
   byte getRadiusOfAffect();
   void affectCharacterOnEntry(Character enteringCharacter);
   void affectCharacterOnExit(Character exitingCharacter);
   void affectCharacterOnRoundStart(Character characterInHex);
   void affectCharacterOnRoundEnd(Character characterInHex);
   String getImageResourceName();
   void affectCharacterOnActivation(Character chr);
   void affectCharacterOnDeactivation(Character chr);

   public Spell clone();
}
