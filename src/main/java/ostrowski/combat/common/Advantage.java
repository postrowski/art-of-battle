/*
 * Created on Oct 30, 2006
 *
 */
package ostrowski.combat.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.html.HtmlBuilder;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.protocol.SerializableObject;

public class Advantage extends SerializableObject implements Cloneable, Enums
{
   String   _name;
   boolean  _hasEffectInSimulator;
   int[]    _costs;
   ArrayList<String> _requirements;
   public ArrayList<String> _conflicts;
   ArrayList<String> _levels;
   byte      _level = 0;
   String   _description;

   public Advantage()   {
      // This c'tor is used by the serialization routines.
   }
   public Advantage(String name, boolean hasEffectInSimulator, String[] levels, int[] costs, String[] requirements, String[] conflicts, String description)
   {
      _name  = name;
      _hasEffectInSimulator = hasEffectInSimulator;
      _costs = costs;
      _requirements = new ArrayList<>();
      _conflicts    = new ArrayList<>();
      _levels       = new ArrayList<>();
      for (String element : levels) {
         _levels.add(element);
      }
      for (String element : requirements) {
         _requirements.add(element);
      }
      for (String element : conflicts) {
         _conflicts.add(element);
      }
      _description = description;
   }

   public static final String  ABSOLUTE_DIRECTION   = "Absolute Direction";
   public static final String  ABSOLUTE_TIMING      = "Absolute Timing";
   public static final String  ADDICTION            = "Addiction";
   public static final String  ALERTNESS            = "Alertness";
   public static final String  AMBIDEXTROUS         = "Ambidextrous";
   public static final String  APPEARANCE           = "Appearance";
   public static final String  ARMS_4               = "Four Arms";
   public static final String  AUDIOGRAPHIC_MEMORY  = "Audiographic memory";
   public static final String  BAD_TEMPER           = "Bad Temper";
   public static final String  BERSERKER            = "Berserker";
   public static final String  CODE_OF_CONDUCT      = "Code of Conduct";
   public static final String  COMPULSIVE_LIAR      = "Compulsive Liar";
   public static final String  DELUSIONAL           = "Delusional";
   public static final String  DISFIGURED_ARMS      = "Disfigured Arms";
   public static final String  DISFIGURED_FACE      = "Disfigured Face";
   public static final String  DISFIGURED_HANDS     = "Disfigured Hands";
   public static final String  DISFIGURED_LEGS      = "Disfigured Legs";
   public static final String  DIVINE_AFFINITY_     = "Divine Aff. - ";
   public static final String  DIVINE_POWER         = "Divine Power";
   public static final String  EPILEPTIC            = "Epileptic";
   public static final String  EUNUCH               = "Eunuch";
   public static final String  GREEDY               = "Greedy";
   public static final String  HANDS_0              = "No hands";
   public static final String  HEADS_2              = "Two Heads";
   public static final String  HERO_POINTS          = "Hero points";
   public static final String  HEARING              = "Hearing";
   public static final String  INFRA_VISION         = "Infravision";
   public static final String  INTOLERANT           = "Intolerant";
   public static final String  HONEST               = "Honest";
   public static final String  LAZY                 = "Lazy";
   public static final String  LITERACY             = "Literacy";
   public static final String  KNIGHTHOOD           = "Knighthood";
   public static final String  LECHEROUS            = "Lecherous";
   public static final String  LEGS_4               = "4 Legs";
   public static final String  MAGICAL_APTITUDE     = "Magical Aptitude";
   public static final String  MAGIC_RESISTANCE     = "Magic Resistance";
   public static final String  MEGALOMANIAC         = "Megalomaniac";
   public static final String  MUTE                 = "Mute";
   public static final String  NIGHT_VISION         = "Night Vision";
   public static final String  NO_PAIN              = "No pain";
   public static final String  PERIPHERAL_VISION    = "Peripheral Vision";
   public static final String  PHOBIA               = "Phobia";
   public static final String  PHOTOGRAPHIC_MEMORY  = "Photographic memory";
   public static final String  PSYCHOTIC            = "Psychotic";
   public static final String  QUIRKS               = "Quirks";
   public static final String  RANK_MILITARY_ENLISTED="Rank: Military, Enlisted";
   public static final String  RANK_MILITARY_OFFICER= "Rank: Military, Officer";
   public static final String  RANK_SOCIAL_MALE     = "Rank: Social (m)";
   public static final String  RANK_SOCIAL_FEMALE   = "Rank: Social (f)";
   public static final String  REGENERATION         = "Regeneration";
   public static final String  SADISTIC             = "Sadistic";
   public static final String  SENSE_OF_DUTY        = "Sense of Duty";
   public static final String  SPLIT_PERSONALITIES  = "Split Personalities";
   public static final String  UNDEAD               = "Undead";
   public static final String  UNUSUAL_BACKGROUND   = "Unusual Background";
   public static final String  VISION               = "Vision";
   public static final String  WEALTH               = "Wealth";
   public static final String  WEALTH_MULTIPLIER_x1 = "Wealth Multiplier:1";
   public static final String  WEALTH_MULTIPLIER_x2 = "Wealth Multiplier:2";
   public static final String  WEALTH_MULTIPLIER_x3 = "Wealth Multiplier:3";
   public static final String  WEALTH_MULTIPLIER_2  = "Wealth Multiplier:1/2";
   public static final String  WEALTH_MULTIPLIER_3  = "Wealth Multiplier:1/3";
   public static final String  WEALTH_MULTIPLIER_4  = "Wealth Multiplier:1/4";
   public static final String  WEALTH_MULTIPLIER_5  = "Wealth Multiplier:1/5";
   public static final String  WINGED_FLIGHT        = "Winged Flight";

   // make a reference to the Rules object before we get into the static initializer, because that
   // makes a reference to the PriestSpells, which need the Rules object to already have been loaded.
   static final String _dummy = Rules.diagCompName;
   public static ArrayList<Advantage> _advList = new ArrayList<>();
   static {
      _advList.add(new Advantage(ABSOLUTE_DIRECTION,  false, new String[] {}, new int[] { 5},       new String[] {}, new String[] {}, "Absolute Direction lets the bearer know which direction north is at all times, even when underground. These individuals will almost never become lost."));
      _advList.add(new Advantage(ABSOLUTE_TIMING,     false, new String[] {}, new int[] { 5},       new String[] {}, new String[] {}, "Absolute Timing allows the bearer to know exactly what of day it is at all times. They are also extremely accurate at estimating length of time."));
      _advList.add(new Advantage(ADDICTION,          false,
                                 new String[] {"Cigarettes", "Alcohol (Minor)", "Alcohol (Major)", "Soft Drugs", "Hard Drugs", "Gambling (Minor)", "Gambling (Major)", "Serial Killer"},
                                 new int[] {             -1,                -5,               -10,          -10,          -30,                -5,                -20,             -10}, new String[] {}, new String[] {}, "Characters may be addiction to many things: cigarettes, drugs, alcohol, sex, lying (see “Compulsive Liar”), cell phones, stealing (see “Kleptomania”), gambling, even murder (serial killers). Addictions vary in degree and effect. The point value given for an addiction depends upon several factors: how expensive the addiction is, how much time the addiction requires, how much of a social stigma is associated with the addiction, how much effort is required to hide the addiction, and the level of consequences from the addition (jail time, health risks, etc.)."));
      _advList.add(new Advantage(ALERTNESS,          false,
                                 new String[] {"Oblivious", "Unaware", "Normal", "Alert", "Very Alert", "Exceptionally Alert"},
                                 new int[] {           -10,        -5,        0,       5,           15,                   30}, new String[] {}, new String[] {}, "Some people are more aware of their surroundings than others. Characters with levels of Alertness add or subtract values to any IQ roll made to hear, feel, taste, see or otherwise notice anything unusual in their surroundings. These bonuses or penalties are added to all saving throws for noticing things in their surroundings."));
      _advList.add(new Advantage(AMBIDEXTROUS,         true, new String[] {}, new int[] {10},       new String[] {}, new String[] {HANDS_0, DISFIGURED_ARMS, DISFIGURED_HANDS}, "Ambidextrous individuals can use either hand equally well, and may even fight with two swords at once, using each one as well as the other."));
      _advList.add(new Advantage(APPEARANCE,          false,
                                 new String[] {"Hideous", "Ugly", "Unattractive", "Normal", "Attractive", "Very Attractive", "Extremely Attractive"},
                                 new int[] {         -30,    -20,            -10,        0,           10,                20,                     40}, new String[] {}, new String[] {}, "The appearance advantage directly modifies the SOC attribute for all interactions with heterosexual members of the opposite sex, or homosexual members of their own sex. Members of other races are only affected by half the adjustment amount (rounded toward zero)."));
      _advList.add(new Advantage(BAD_TEMPER,          false, new String[] {}, new int[] {-5},       new String[] {}, new String[] {}, "Having a Bad Temper causes the bearer to resort to the use of force more often than trying to talk it out. In hostile situations, the bearer of the 'bad temper' disadvantage has a -2 penalty to all SOC rolls."));
      _advList.add(new Advantage(BERSERKER,            true, new String[] {}, new int[]{-10},       new String[] {}, new String[] {}, "A Berserker character is one who enters a rage any time they experience pain. While in this state, they are not able to think clearly, and are focused solely on attacking whoever it was that inflicted their pain. In this state, a Berserker is not able to move evasively, retreat, parry, block or cast spells. They may also not ready a new weapon, but may re-ready anything already in their hand(s). The only active defense a berserking character may take is to dodge. In exchange for these penalties, a berserking character is unaffected by pain while in his enraged state. Wounds still affect the character as normal. Any time a berserker character is hit, and takes pain, he must roll 2-action IQ against a TN of his pain level to avoid going into the berserking state. Once in this state, he will remain berserking until the character that inflicted the pain is dead or unconscious. Once that happens, the berserker must make a 1-action IQ roll against a TN of his current pain level, plus 3, or he will continue his berserking rage, attacking the next nearest enemy in front of him. If the berserker rolls the minimum possible roll (a 1 on each die rolled), then the berserking character will attack the nearest person in front of him, friend or foe alike!"));
      _advList.add(new Advantage(CODE_OF_CONDUCT,     false,
                                 new String[] {"Honest", "Fair Fighter", "Chivalrous", "Bushido", "Severe", "Major", "Average", "Minor"},
                                 new int[] {         -5,            -10,          -15,       -20,      -20,     -15,       -10,      -5}, new String[] {}, new String[] {}, "A Code of Conduct restricts how the character may act. Common examples: \"Never attack an unarmed foe\" (minor), \"Never attack first\" (average), \"Donate 50% of all money earned to charity\" (major), \"Pacifist\" (severe)."));
      _advList.add(new Advantage(COMPULSIVE_LIAR,     false, new String[] {}, new int[]{-10},       new String[] {}, new String[] {HONEST, MUTE}, "A compulsive liar will make up stories constantly, just to be amusing. Other people that know the character is a compulsive liar will treat this person at a -1 in social situation, and will be very doubtful of anything they say."));
      _advList.add(new Advantage(DELUSIONAL         , false, new String[] {}, new int[]{-10},       new String[] {}, new String[] {}, "A delusional character suffers from significant delusions. They tend to believe in conspiracy theories, and are often more gullible than other characters. The delusions they suffer are completely real to them, and they will act accordingly, up to the point of mistrusting even close long-term friends."));
      for (String deity : PriestSpell._deities) {
         String[] exclusions = new String[PriestSpell._deities.size()];
         int i=0;
         for (String otherDeity : PriestSpell._deities) {
            if (otherDeity != deity) {
               exclusions[i++] = DIVINE_AFFINITY_ + otherDeity;
            }
         }
         _advList.add(new Advantage(DIVINE_AFFINITY_ + deity, true,
                                    new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"},
                                       new int[] { 10,  20,  30,  40,  50,  60,  70,  80,  90,  100}, new String[] {}, exclusions, "Divine affinity grants a character a personal relationship with a single specified God or Deity. This relationship allows the bearer to cast priest spells in the name of the God or Deity, provided they also have the Divine Power advantage. Note: Divine Affinity may vary per religion. For example, Divine Affinity to most demons only cost 5 points per level. For more details, see the earlier section on priest magic."));
      }
      _advList.add(new Advantage(DIVINE_POWER,         true,
                                 new String[] {"1", "2", "3", "4", "5"},
                                    new int[] {  5,  15,  30,  50,  75}, new String[] {}, new String[] {}, "A character with the Diving Power advantage is capable of channeling divine power from any and all Gods with whom they have Divine Affinity. For more details, see the earlier section on priest magic."));
      _advList.add(new Advantage(DISFIGURED_ARMS,     false,
                                 new String[] {"No arms", "Crippled arms", "Missing right arm", "Crippled right arm", "Missing left arm", "Crippled left arm"},
                                    new int[] {     -150,            -125,                -100,                  -50,                -50,                 -35}, new String[] {}, new String[] {}, "Physical disfigurement may adversely affect the way a person is received in social settings, and obvious hampers their physical abilities."));
      _advList.add(new Advantage(DISFIGURED_FACE,     false,
                                 new String[] {"Missing eye(s)", "Severe scars", "Scars", "Minor scars"},
                                    new int[] {             -20,            -15,     -10,            -5}, new String[] {}, new String[] {}, "Physical disfigurement may adversely affect the way a person is received in social settings, and obvious hampers their physical abilities."));
      _advList.add(new Advantage(DISFIGURED_HANDS,    false,
                                 new String[] {"No hands", "Crippled hands", "Missing right hand", "Crippled right hand", "Missing left hand", "Crippled left hand"},
                                    new int[] {      -100,              -75,                  -75,                   -40,                 -40,                 -20,}, new String[] {}, new String[] {}, "Physical disfigurement may adversely affect the way a person is received in social settings, and obvious hampers their physical abilities."));
      _advList.add(new Advantage(DISFIGURED_LEGS,     false,
                                 new String[] {"No legs", "Crippled legs", "Missing 1 leg", "Crippled leg", "Severe limp (-3 move/dodge)", "Limp (-2 move/dodge)", "Minor limp (-1 move/dodge)"},
                                   new int[] {      -150,           -125,            -100,            -50,                           -25,                    -15,                          -10}, new String[] {}, new String[] {}, "Physical disfigurement may adversely affect the way a person is received in social settings, and obvious hampers their physical abilities."));
      _advList.add(new Advantage(EPILEPTIC          , false, new String[] {}, new int[]{-15}, new String[] {}, new String[] {}, ""));
      _advList.add(new Advantage(EUNUCH,              false, new String[] {}, new int[]{ -5}, new String[] {}, new String[] {}, "Eunuchs are male characters that have had their testicles removed so they are not able to have sex. If done before puberty, this causes their voices to never change. Eunuchs are often used as guards to guard females, as they are the only males that can be trusted with 100% certainty. Eunuchs are looked down upon in society as not being fully men. They are immune to the appearance advantage of others, and to all attempts at sex appeal."));
      _advList.add(new Advantage(ARMS_4,               true, new String[] {}, new int[]  {0}, new String[] {ARMS_4}, new String[] {""}, "Having four arms allows you to wield two weapons and carry two shields. Anything held in your second set of arms is used at a -2 penalty, unless the bearer also has the ambidextrous advantage. The off-hand penalty (of -4) is cumulative with the off-set penalty of -2, for a total of -6. The ‘four arms’ advantage may not be purchased, but some races (notably insect men) have this advantage intrinsically."));
      _advList.add(new Advantage(LEGS_4,               true, new String[] {}, new int[]  {0},       new String[] {LEGS_4}, new String[] {""}, "Having four legs allows creatures to move faster, as reflected by their increases movement rate listed for their race. Furthermore, if a 4-legged character loses a leg in combat, the effects are limited: instead of falling over, they only suffer a -2 penalty to their movement rate. Finally, penalties to movement rate from wounds are cut in half, rounding down, for those with four (or more) legs. The ‘Four legs’ advantage may not be purchased, but some races (notably centaurs) have this advantage intrinsically."));
      _advList.add(new Advantage(GREEDY,              false, new String[] {}, new int[] {-5},             new String[] {}, new String[] {}, "Greedy characters are always looking for ways to increase their personal wealth. They are very well motivated by the prospect of a monetary pay-out, and will often try to cheat even their closest friend out of money, if they think they can get away with the deception."));
      _advList.add(new Advantage(HEARING,             false,
                                 new String[] {"Deaf", "Poor Hearing", "Normal Hearing", "Acute Hearing"},
                                    new int[] {   -30,            -10,        0,              10}, new String[] {}, new String[] {}, "Different levels of hearing affect how well a character can hear sounds. To successfully hear something important, characters must make a saving throw roll against their IQ. Characters with Acute Hearing get a +5 to all hearing rolls, while characters with Poor Hearing get a -5 to all hearing rolls (such as a saving throw to hear that someone is following you.) The saving throw bonuses and penalties from the Hearing and Alertness advantages are cumulative."));
      _advList.add(new Advantage(HERO_POINTS,          true,
                                 new String[] {"0", "1", "2", "3"},
                                    new int[] {  0,  25,  50,  75}, new String[] {}, new String[] {}, "A character may buy hero points at the start of a campaign. Hero points allow a character to survive fatal blows and events as described in the Hero Points section of the rules book."));
      _advList.add(new Advantage(HONEST,              false, new String[] {}, new int[] {-5},             new String[] {}, new String[] {COMPULSIVE_LIAR}, "Honest character find it very difficult to lie, will always avoid doing so. If they do have to lie, the lie will often be obvious. Other individuals that know about the bearers honest will usually treat this person favorably, and believe what they say to be truthful."));
      _advList.add(new Advantage(INFRA_VISION,        false, new String[] {}, new int[]  {0}, new String[] {INFRA_VISION}, new String[] {NIGHT_VISION}, "Characters with infra vision are able to see the infrared spectrum of light. All objects emit varying levels of infrared radiation, which can be seen by these individuals. This allows them to see in total darkness. The ‘Infra-vision’ advantage may not be purchased, but some races (notably Orcs and Elves) have this advantage intrinsically."));
      _advList.add(new Advantage(INTOLERANT,          false, new String[] {}, new int[]{-15},             new String[] {}, new String[] {}, "Intolerant characters must pick what they are intolerant of. Some common intolerants includes: intolerance of other (or particular) races, intolerance of those of other religions or intolerance of magic-use. Intolerant characters will always treat those that they are intolerant of poorly, and with disgust and disrespect. Oftentimes, the manner in which they treat the subject of their intolerance lead to physical confrontation."));
      _advList.add(new Advantage(LAZY,                false, new String[] {}, new int[] {-5},             new String[] {}, new String[] {}, "Lazy character try to avoid work at all costs."));
      _advList.add(new Advantage(LITERACY,            false, new String[] {}, new int[] {10},             new String[] {}, new String[] {}, "In medieval time, and in many fantasy campaigns, knowing how to read and write is not commonplace. In these worlds, Literacy is an advantage costing 10 points, while being illiterate does not alter your character’s points at all. In modern times, or in campaigns where the GM rules that Literacy is the norm, Illiteracy is a -10 point disadvantage."));
      _advList.add(new Advantage(KNIGHTHOOD,          false, new String[] {}, new int[] {10},             new String[] {}, new String[] {}, "Individuals may be knighted by a king for their exceptional support of the kingdom. Knighted individuals are awarded a family coat-of-arms, and must always be address as ‘Sir’ or ‘Dame’, and their honor should never be called into question. Being knighted affords the knight a higher status, equivalent to a boost to their social rank advantage described later."));
      _advList.add(new Advantage(LECHEROUS,           false, new String[] {}, new int[] {-5},             new String[] {}, new String[] {}, "Lecherous characters find it difficult to avoid flirt with attractive members of the opposite sex. They are easily manipulated by members of the opposite sex."));
      _advList.add(new Advantage(MAGICAL_APTITUDE,   false,
                                 new String[] {"+1", "+2", "+3", "+4", "+5"},
                                    new int[] {  10,   20,   30,   40,   50}, new String[] {}, new String[] {MAGIC_RESISTANCE}, "A character with the Magical Aptitude advantage can channel magical energy, giving them the ability to cast magic spells, so long as he or she has learned the spell. A character may not have both the ‘Magic Resistance’ and any level of ‘Magical Aptitude’"));
      _advList.add(new Advantage(MAGIC_RESISTANCE,     true,
                                 new String[] {"+1", "+2", "+3", "+4", "+5"},
                                    new int[] {   5,   10,   15,   20,   25}, new String[] {}, new String[] {MAGICAL_APTITUDE}, "A character with Magic Resistance gets a bonus to all resistance rolls to avoid the effects of magic. Against Mage spells, for every level of Magic Resistance, they get a +3 to their resistance roll. Against Priest spells, every level of Magic Resistance reduces the effective power of all priest spells. If the spell’s effective power drops below 1, the spell is automatically ineffective. For example: Margo the Mage casts a ‘Control Mind’ spell on Fred. The ‘Control Mind’ spell is resisted by IQ + 5 + d10±. Fred has an IQ of 1, and 1 level of Magic Resistance, so he rolls 6 + d10±. His Magic Resistance advantage allows him to add +5 to this roll. This resistance is always in effect, even when the character is unconscious. Furthermore, this advantage may interfere with beneficial spells, if that spell is resisted (such as a ‘Stasis’ spell cast by a priest to suspend the character’s bleeding.). A character may not have both the ‘Magic Resistance’ and any level of ‘Magical Aptitude’. Characters with the ‘Magic Resistance’ advantage are not restricted from having the ‘Divine Power’ advantage."));
      _advList.add(new Advantage(MEGALOMANIAC,        false, new String[] {}, new int[]{-10},             new String[] {}, new String[] {}, "A megalomaniac is someone who seeks to control everything around them. Given the opportunity, they will seek to dominate the entire world."));
      _advList.add(new Advantage(MUTE,                false, new String[] {}, new int[]{-15},             new String[] {}, new String[] {COMPULSIVE_LIAR}, "A mute is unable to speak. If they have learned the sign language skill, they may still comunicate with others that can sign. A mute character often suffers from social stigma from those that are not empathetic to their condition. As a result, a mute suffers a -1 in all social settings, except for those involving those that are simpathetic to their condition."));
      _advList.add(new Advantage(NIGHT_VISION,        false, new String[] {}, new int[] {10}, new String[] {NIGHT_VISION}, new String[] {INFRA_VISION}, "Characters with night vision are able to see reasonably well in low-light conditions. The vision of a character with night vision will be the same from daytime to a night lit by the full moon. They will even be able to see in a star-lit night, at a penalty, but much less significant than those without the night vision advantage."));
      _advList.add(new Advantage(NO_PAIN,              true, new String[] {}, new int[]{125},      new String[] {NO_PAIN}, new String[] {}, "Characters that feel no pain are never affected by pain caused by wounds. They are still affected by their wound level, however. The ‘no pain’ advantage may not be purchased, but some races (such as all undead races) have this advantage intrinsically."));
      _advList.add(new Advantage(PERIPHERAL_VISION,    true, new String[] {}, new int[] {10}, new String[] {PERIPHERAL_VISION}, new String[] {}, "Characters with peripheral vision can see 360 degree around them."));
      _advList.add(new Advantage(PHOBIA             , false,
                                 new String[] {"Minor", "Moderate", "Major", "Crippling"},
                                    new int[]{      -1,         -5,     -10,         -15}, new String[] {}, new String[] {}, "Phobias affect a character's ability to deal with certain situations. The level of phobias varies from a minor to crippling. A minor phobia causes a character to avoid dealing with the subject of their phobia, or suffering a -2 penalty to all rolls related to the subject of their phobia. a Moderate phobia incurs a -4 penalty to all rolls. A severe phobia means that the individual is incapable of dealing directly with the subject of their phobia, and a crippling phobia sends the character into a hysterical state, unable to do anything, expect for running away from the subject of their phobia. The cost of a phobia should also be influenced by the frequency of the subject. Even a crippling phobia of dragons is not worth many points, since dragons are so rare. A phobia of wide open spaces (agoraphobia) is much more significant, and would be worth more points as a disadvantage."));
      _advList.add(new Advantage(PHOTOGRAPHIC_MEMORY, false, new String[] {}, new int[] {25},             new String[] {}, new String[] {}, "Individuals with photographic memory are able to recall fine details in images and sounds from recent memory after having only a few moments of exposure. They also have extraordinary long-term recollection of events and details. They cannot recall conversations from over a month ago word-for-word, but they can recall many details much longer than normal individuals."));
      _advList.add(new Advantage(PSYCHOTIC          , false, new String[] {}, new int[]{-10},             new String[] {}, new String[] {}, "Psychotic indivuals have a difficulty discerning what is real, and what is not real. Psychotic individuals often have delusions, which always seem very real, no matter how bizarre they seem to normal people."));
      _advList.add(new Advantage(QUIRKS,     false,
                                 new String[] {"x1", "x2", "x3", "x4", "x5"},
                                 new int[] {    -1,   -2,   -3,   -4,   -5 }, new String[] {}, new String[] {}, "Quirks add flavor to a character’s background. They have little or no game impact. A character may have up to 5 unique quirks. Sample quirks are: whistles often, never uses silver wear, complains a lot, calls everyone by a nickname, always wears a hat, sings in the shower, keeps a diary/journal, snores loudly, bad breath, keeps a pet mouse in his pocket, etc."));
      _advList.add(new Advantage(RANK_MILITARY_ENLISTED,     false,
                                 new String[] {"Conscript", "Private", "Sergeant", "High Sergeant", "Sergeant Major"},
                                 new int[] {            -5,         0,          5,              10,               15}, new String[] {}, new String[] {RANK_MILITARY_OFFICER}, "Rank is a pecking order within society. There are multiple types of rank, such as military rank or social rank. In all cases, the higher a person’s rank, the more respect they command, and the more preferential treatment they get. Rank also determines how an individual must be addresses, as well as how lower ranked individuals are required to interact with them. Failure to obey a higher ranked individual or not treating them with the required etiquette and respect may result in severe penalties, such as public floggings, jail time, fines, loss or rank and in some cases even execution. A single individual may have both a single social rank as well as a single military rank. Higher ranks usually go hand-in-hand with an increased wealth level and often a sense of duty (especially the military ranks), each of which are separately purchased advantages and disadvantages."));
      _advList.add(new Advantage(RANK_MILITARY_OFFICER,     false,
                                 new String[] {"Lieutenant", "Captain", "Major", "Colonel", "General"},
                                 new int[] {            10,        20,      25,        30,        35}, new String[] {}, new String[] {RANK_MILITARY_ENLISTED}, "Rank is a pecking order within society. There are multiple types of rank, such as military rank or social rank. In all cases, the higher a person’s rank, the more respect they command, and the more preferential treatment they get. Rank also determines how an individual must be addresses, as well as how lower ranked individuals are required to interact with them. Failure to obey a higher ranked individual or not treating them with the required etiquette and respect may result in severe penalties, such as public floggings, jail time, fines, loss or rank and in some cases even execution. A single individual may have both a single social rank as well as a single military rank. Higher ranks usually go hand-in-hand with an increased wealth level and often a sense of duty (especially the military ranks), each of which are separately purchased advantages and disadvantages."));
      _advList.add(new Advantage(RANK_SOCIAL_MALE,       false,
                                 new String[] {"Slave", "Peasant", "Serf", "Commoner", "Baron", "Viscount","Count", "Duke", "Prince", "King", "Emperor"},
                                 new int[] {       -15,       -10,     -5,          0,       5,         10,     15,     25,       50,    100,       300}, new String[] {Race.Gender.MALE._name}, new String[] {Race.Gender.FEMALE._name, RANK_SOCIAL_FEMALE}, "Rank is a pecking order within society. There are multiple types of rank, such as military rank or social rank. In all cases, the higher a person’s rank, the more respect they command, and the more preferential treatment they get. Rank also determines how an individual must be addresses, as well as how lower ranked individuals are required to interact with them. Failure to obey a higher ranked individual or not treating them with the required etiquette and respect may result in severe penalties, such as public floggings, jail time, fines, loss or rank and in some cases even execution. A single individual may have both a single social rank as well as a single military rank. Higher ranks usually go hand-in-hand with an increased wealth level and often a sense of duty (especially the military ranks), each of which are separately purchased advantages and disadvantages."));
      _advList.add(new Advantage(RANK_SOCIAL_FEMALE,       false,
                                 new String[] {"Slave", "Peasant", "Serf", "Commoner", "Baronness", "Viscountess","Countess", "Duchess", "Princess", "Queen", "Empress"},
                                 new int[] {       -15,       -10,     -5,          0,           5,            10,        15,        20,         30,      50,       100}, new String[] {Race.Gender.FEMALE._name}, new String[] {Race.Gender.MALE._name, RANK_SOCIAL_MALE}, "Rank is a pecking order within society. There are multiple types of rank, such as military rank or social rank. In all cases, the higher a person’s rank, the more respect they command, and the more preferential treatment they get. Rank also determines how an individual must be addresses, as well as how lower ranked individuals are required to interact with them. Failure to obey a higher ranked individual or not treating them with the required etiquette and respect may result in severe penalties, such as public floggings, jail time, fines, loss or rank and in some cases even execution. A single individual may have both a single social rank as well as a single military rank. Higher ranks usually go hand-in-hand with an increased wealth level and often a sense of duty (especially the military ranks), each of which are separately purchased advantages and disadvantages."));
      _advList.add(new Advantage(REGENERATION,         true, new String[] {}, new int[] { 0}, new String[] {REGENERATION}, new String[] {}, "Regeneration allows an individual to heal their wounds very quickly. Any time a pain roll is rolled, and the pain reduction is 10 points or more, the character heals one wound from any random previous damage. Crippled limbs become un-crippled after all the wounds on that limb are healed.. The ‘regeneration’ advantage may not be purchased, but some races (notably trolls) have this advantage intrinsically."));
      _advList.add(new Advantage(SADISTIC,            false, new String[] {}, new int[] {-5},             new String[] {}, new String[] {}, "Sadists take pleasure in putting other individuals in pain."));
      _advList.add(new Advantage(SENSE_OF_DUTY,     false,
                                 new String[] {"Complete (slave)", "Full-time", "Part-time", },
                                 new int[] {                  -40,         -20,         -10, }, new String[] {}, new String[] {}, "A sense of duty requires the character to spend a portion of their time following the orders of others."));
      _advList.add(new Advantage(SPLIT_PERSONALITIES, false, new String[] {}, new int[]{-20},             new String[] {}, new String[] {}, "Characters with split personalities are just like two (or more) people. They have different mental advantages or disadvantages, and even different levels of intelligence, dexterity, toughness, and social abilities."));
      _advList.add(new Advantage(UNUSUAL_BACKGROUND,  false,
                                 new String[] {"5", "10", "15", "20", "25"},
                                    new int[] { 5,   10,   15,   20,   25}, new String[] {}, new String[] {}, "Players must specify a background story to explain their character’s skill set and personal back story. Backgrounds that are very unusual may require an additional points cost. This should be used as tool by the GM to keep characters balanced. For example, it should be very unusual for any new character to spend more than one third their total character points on any single skill or attribute without an unusual background. A character that has been a captive slave forced into a combat area since age 10 would explain a character’s high weapon skill. A character that has lived his entire life as a part of a travelling circus troupe would explain a character’s high NIM attribute and acrobatics skill. It is up to the GM to determine the cost of the background, and when it is required."));
      _advList.add(new Advantage(HEADS_2,              true, new String[] {}, new int[] { 0},      new String[] {HEADS_2}, new String[] {""}, "Having two heads allows the being to be more alert, allowing two IQ rolls (one for each head) where a normal character only gets one roll. Also, a character with two head may survive having a single head cut off, but this will lower their NIM and DEX score by one point. Characters with two heads often talk to themselves, and even get into arguments with themselves. The ‘two heads’ advantage may not be purchased, but some races (some types of giants or monsters) have this advantage intrinsically."));
      _advList.add(new Advantage(UNDEAD,               true, new String[] {}, new int[]{ 15},       new String[] {UNDEAD}, new String[] {}, "Undead characters do not sleep, eat, drink, bleed or feel pain. They are completely under the control of their creator."));
      _advList.add(new Advantage(VISION,              false,
                                 new String[] {"blind", "poor sight", "Near sighted", "Far sighted", "Normal", "Acute vision"},
                                    new int[] {   -100,          -15,            -10,           -10,        0,             10}, new String[] {}, new String[] {}, "Various levels of vision allow character to see better or worse than normal character. Poor Sighted and Near Sighted characters suffer a range penalty for using missile ranges at distances greater than short range: Targets at medium range are at an additional -2 to hit, while targets at long range are at an additional -4 to hit. Poor Sighted and Far Sighted character suffer a penalty at any fine detail work, such as sewing, and also find it difficult to read anything but large text writings. Vision levels also increase or decrease saving throw rolls for noticing visual concerns. Acute vision gives a +5 to vision-based IQ saving throws, and Poor Sight, Near Sight and Far Sight gives a -5 to vision-based IQ saving throws (in the range at which they are visually penalized.) The saving throw bonuses and penalties from the Vision and Alertness advantages are cumulative."));
      _advList.add(new Advantage(WEALTH,               true,
                                 new String[] {"$0","$200", "$500", "$1,000", "$2,000", "$3,000", "$5,000", "$10,000", "$30,000", "$100,000", "$500,000", "$1,500,000", "$10,000,000"},
                                    new int[] { -20,   -10,     -5,       0,       5,      10,      15,       20,       25,        30,       40,          50,          75}, new String[] {}, new String[] {}, "Wealth defines the amount of money a character is allowed to spend on starting equipement. As a character accumulates for wealth, they must spend their available character points to for the point cost of their new wealth level, or give away their new found wealth as they see fit."));
      _advList.add(new Advantage(WEALTH_MULTIPLIER_x2, true, new String[] {}, new int[]{ 10},      new String[] {WEALTH_MULTIPLIER_x2}, new String[] {}, "Wealth multipliers are assigned to each race. This value multiplies the starting wealth for any character."));
      _advList.add(new Advantage(WEALTH_MULTIPLIER_x3, true, new String[] {}, new int[]{ 20},      new String[] {WEALTH_MULTIPLIER_x3}, new String[] {}, "Wealth multipliers are assigned to each race. This value multiplies the starting wealth for any character."));
      _advList.add(new Advantage(WEALTH_MULTIPLIER_2,  true, new String[] {}, new int[]{ -5},      new String[] {WEALTH_MULTIPLIER_2}, new String[] {}, "Wealth multipliers are assigned to each race. This value multiplies the starting wealth for any character."));
      _advList.add(new Advantage(WEALTH_MULTIPLIER_3,  true, new String[] {}, new int[]{-10},      new String[] {WEALTH_MULTIPLIER_3}, new String[] {}, "Wealth multipliers are assigned to each race. This value multiplies the starting wealth for any character."));
      _advList.add(new Advantage(WEALTH_MULTIPLIER_4,  true, new String[] {}, new int[]{-15},      new String[] {WEALTH_MULTIPLIER_4}, new String[] {}, "Wealth multipliers are assigned to each race. This value multiplies the starting wealth for any character."));
      _advList.add(new Advantage(WEALTH_MULTIPLIER_5,  true, new String[] {}, new int[]{-20},      new String[] {WEALTH_MULTIPLIER_5}, new String[] {}, "Wealth multipliers are assigned to each race. This value multiplies the starting wealth for any character."));
      _advList.add(new Advantage(WINGED_FLIGHT,        true, new String[] {}, new int[]  {0},      new String[] {WINGED_FLIGHT}, new String[] {}, "Winged flight allows the individual to fly through the air. The ‘winged flight’ advantage may not be purchased, but some races (notably Fairies) have this advantage intrinsically."));
   }

   public static ArrayList<String> getAdvantagesNames(ArrayList<String> existingProperties, Race race) {
      ArrayList<String> list = new ArrayList<>();
      for (Advantage advantage : _advList) {
         if (advantage.isAllowed(existingProperties, race)) {
            if (!existingProperties.contains(advantage.getName())) {
               list.add(advantage._name);
            }
         }
      }
      return list;
   }

   boolean isAllowed(ArrayList<String> existingProperties, Race race)
   {
      for (String conflict : _conflicts) {
         if (existingProperties.contains(conflict)) {
            return false;
         }
         if ((race != null) && (race.getName().equals(conflict))) {
            return false;
         }
         if ((race != null) && (race.getGender()._name.equals(conflict))) {
            return false;
         }
      }
      for (String requirement : _requirements) {
         if (!existingProperties.contains(requirement)) {
            if (race == null) {
               return false;
            }
            if (!race.getName().equals(requirement)) {
               if (!race.getGender()._name.equals(requirement)) {
                  return false;
               }
            }
         }
      }
      return true;
   }

   public static Advantage getAdvantage(String name)
   {
      if (name != null) {
         for (Advantage advantage : _advList) {
            if (advantage._name.equalsIgnoreCase(name)) {
               return advantage.clone();
            }
         }
         // Consider the case of "AdvantageName:LevelName", such as "Code Of Conduct:Honest"
         name = name.toLowerCase();
         for (Advantage advantage : _advList) {
            if (name.startsWith(advantage._name.toLowerCase())) {
               Advantage adv = advantage.clone();
               String selectedLevelName = name.substring(advantage._name.length()+1).trim();
               if (adv.setLevelByName(selectedLevelName)) {
                  return adv;
               }
            }
         }
      }
      return null;
   }

   public String getName()                  { return _name;}
   public int getCost(Race race) {
      if (race != null) {
         ArrayList<Advantage> racialAdvantages = race.getAdvantagesList();
         for (Advantage adv  : racialAdvantages) {
            if (adv.getName().equals(getName())) {
               int costForRacialLevel = _costs[adv._level];
               return _costs[_level] - costForRacialLevel;
            }
         }
      }
      return _costs[_level];
   }
   public byte getLevel()                   { return _level;}
   public String getLevelName()             {
      if ((_levels.size() <= _level) || (_level < 0)) {
         return "";
      }
      return _levels.get(_level);
   }
   public void setLevel(byte level)         { _level = level;}
   public boolean setLevelByName(String levelName) {
      byte i=0;
      for (String level : _levels) {
         if (level.equalsIgnoreCase(levelName)) {
            _level = i;
            return true;
         }
         i++;
      }
      return false;
   }

   public boolean hasLevels()               { return _levels.size() > 0;}
   public ArrayList<String> getLevelNames() {
      ArrayList<String> namesCopy = new ArrayList<>();
      namesCopy.addAll(_levels);
      return namesCopy;
   }
   public String getDescripton() {
      return _description;
   }

   @Override
   public Advantage clone() {
      String[] type = new String[0];
      return new Advantage(_name, false, _levels.toArray(type), _costs, _requirements.toArray(type), _conflicts.toArray(type), "");
   }
   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(_name, out);
         writeToStream(_level, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         _name = readString(in);
         _level = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
      Advantage base = getAdvantage(_name);
      _name          = base._name;
      _costs         = base._costs;
      _requirements  = base._requirements;
      _conflicts     = base._conflicts;
      _levels        = base._levels;
   }
   @Override
   public String toString() {
      return _name + (((_levels != null) && (_level>=0) && (_levels.size() > _level)) ? (": " + _levels.get(_level)) : "");
   }
   public static String generateHtmlTable() {
      StringBuilder sb = new StringBuilder();
      sb.append(HtmlBuilder.getHTMLHeader());
      sb.append("<body>");
      sb.append("<H3>Advantages:</H3>");
      sb.append("<table>");
      sb.append("<tr class=\"header-row\"><th>Name</th><th>Cost</th><th>Description</th></tr>");
      int r=0;
      boolean divineAffinityDisplayed = false;
      for (Advantage adv : _advList) {
         String name = adv.getName();
         if (name.startsWith(DIVINE_AFFINITY_)) {
            if (divineAffinityDisplayed) {
               continue;
            }
            divineAffinityDisplayed = true;
            name = DIVINE_AFFINITY_;
         }
         String color = (((r++)%2) == 0) ? "#FFFFFF" : "#F4F4F4";
         sb.append("<tr bgcolor=").append(color).append(">");
         sb.append("<th>").append(name);
         sb.append("</th>");
         if (adv.hasLevels()) {
            ArrayList<String> levelNames = adv.getLevelNames();
            sb.append("<td>");

            Table table = new Table();
            table.addRow(new TableRow(-1, "Level", "Cost"));
            for (int i=0 ; i<levelNames.size() ; i++) {
               TableRow row = new TableRow(i);
               row.addTD(new TableData(levelNames.get(i)).setAttribute("nowrap", "true"));
               row.addTD(new TableData(adv._costs[i]).setAttribute("nowrap", "true"));
               table.addRow(row);
            }
            sb.append(table.toString());
            sb.append("</td>");
         }
         else {
            sb.append("<td>").append(adv._costs[0]).append("</td>");
         }
         sb.append("<td class='alignLeft'>").append(adv.getDescripton());
         if (!adv._hasEffectInSimulator) {
            sb.append("<br/><i>No effect in this simulator</i>");
         }
         sb.append("</td>");
         sb.append("</tr>");
      }
      sb.append("</table>");
      sb.append("</body>");
      return sb.toString();
   }

   @Override
   public boolean equals(Object other) {
      if (this == other) {
         return true;
      }
      if (other instanceof Advantage) {
         Advantage otherAdv = (Advantage) other;
         return _name.equals(otherAdv._name) && (_level == otherAdv._level);
      }
      return false;
   }
}
