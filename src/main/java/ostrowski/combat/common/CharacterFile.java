/*
 * Created on Sep 5, 2006
 */
package ostrowski.combat.common;

import ostrowski.combat.server.CombatServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class CharacterFile
{
   public final  HashMap<String, Character> _nameToCharMap = new HashMap<>();
   private final String                     _directory;

   public CharacterFile(String fileName) {
      _directory = "Characters";
      loadNameToCharMapFromFile();
   }

   public void loadNameToCharMapFromFile() {
      File sourceFile = new File("Character.data");
      if (sourceFile.exists() && sourceFile.canRead()) {
         try (FileReader fileReader = new FileReader(sourceFile);
              BufferedReader input = new BufferedReader(fileReader)) {
            String inputLine = null;
            while ((inputLine = input.readLine()) != null) {
               Character newChar = new Character();
               if (newChar.serializeFromString(inputLine)) {
                  putCharacter(newChar);
                  writeNameToCharMapToFile(newChar.getName(), false/*overwriteOldCharactes*/);
               }
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
         sourceFile.delete();
         return;
      }
      File sourceDir = new File(_directory);
      if (sourceDir.exists() && sourceDir.isDirectory()) {
         File[] charFiles = sourceDir.listFiles();
         for (File charFile : charFiles) {
            Character newChar = new Character();
            if (newChar.serializeFromFile(charFile)) {
               putCharacter(newChar);
            }
         }
      }
   }

   public void writeNameToCharMapToFile(String characterName) {
      writeNameToCharMapToFile(characterName, true/*overwriteOldCharactes*/);
   }

   public void writeNameToCharMapToFile(String characterName, boolean overwriteOldCharactes) {
      File sourceDir = new File(_directory);
      try {
         if ((!sourceDir.exists()) || (!sourceDir.isDirectory())) {
            if (sourceDir.exists()) {
               // It must be a single file, and not a directory.
               // Delete it, so we can create a directory in its place.
               sourceDir.delete();
            }
            sourceDir.mkdirs();
         }
         File destFile = new File(_directory + File.separator + characterName + ".xml");
         if (destFile.exists()) {
            if (!overwriteOldCharactes) {
               return;
            }
            destFile.delete();
         }
         destFile.createNewFile();
         if (destFile.exists() && destFile.canWrite()) {
            Character character = _nameToCharMap.get(characterName.toLowerCase());
            if (character != null) {
               character.serializeToFile(destFile);
            }
         }
      } catch (IOException e1) {
         e1.printStackTrace();
      }
   }

   public List<String> getCharacterNames() {
      TreeSet<String> names = new TreeSet<>(_nameToCharMap.keySet());
      List<String> strNames = new ArrayList<>(_nameToCharMap.size());
      for (String name : names) {
         Character combatant = _nameToCharMap.get(name);
         if (CombatServer._isServer || !combatant.getRace().isNpc()) {
            strNames.add(combatant.getName());
         }
      }
      return strNames;
   }

   public Character delCharacter(String name) {
      return _nameToCharMap.remove(name.toLowerCase());
   }

   public Character getCharacter(String name) {
      List<Character> charactersMatchingName = new ArrayList<>();
      String lowerCaseName = name.toLowerCase();
      for (String charName : _nameToCharMap.keySet()) {
         if (lowerCaseName.contains(charName.toLowerCase())) {
            Character character = _nameToCharMap.get(charName);
            if (character != null) {
               charactersMatchingName.add(character);
            }
         }
      }
      if (charactersMatchingName.isEmpty()) {
         return null;
      }
      // Find the character with the longest name, and return a clone of that.
      while (charactersMatchingName.size() > 1) {
         if (charactersMatchingName.get(0).getName().length() > charactersMatchingName.get(1).getName().length()) {
            charactersMatchingName.remove(1);
         }
         else {
            charactersMatchingName.remove(1);
         }
      }
      // Only one left, return a clone of it.
      return charactersMatchingName.get(0).clone();
   }

   public Character putCharacter(Character character) {
      return _nameToCharMap.put(character.getName().toLowerCase(), character.clone());
   }

}
