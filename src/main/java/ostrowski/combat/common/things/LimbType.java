package ostrowski.combat.common.things;

import java.util.ArrayList;
import java.util.HashMap;

import ostrowski.DebugBreak;
import ostrowski.combat.common.enums.Enums.Location;
import ostrowski.combat.common.enums.Enums.Pair;
import ostrowski.combat.common.enums.Enums.Side;

public enum LimbType {
    HEAD         (  0, Location.HEAD, Side.ANY,   1, "head"),
    HEAD_2       (  1, Location.HEAD, Side.ANY,   2, "2nd head"),
    HEAD_3       (  2, Location.HEAD, Side.ANY,   3, "3rd head"),
    HEAD_4       (  3, Location.HEAD, Side.ANY,   4, "4th head"),
    LEG_RIGHT    (  4, Location.LEG,  Side.RIGHT, 1, "right leg"),
    LEG_LEFT     (  5, Location.LEG,  Side.LEFT,  1, "left leg"),
    LEG_RIGHT_2  (  6, Location.LEG,  Side.RIGHT, 2, "2nd right leg"),
    LEG_LEFT_2   (  7, Location.LEG,  Side.LEFT , 2, "2nd left leg"),
    LEG_RIGHT_3  (  8, Location.LEG,  Side.RIGHT, 3, "3rd right leg"),
    LEG_LEFT_3   (  9, Location.LEG,  Side.LEFT,  3, "3rd left leg"),
    HAND_RIGHT   ( 10, Location.ARM,  Side.RIGHT, 1, "right hand"),
    HAND_LEFT    ( 11, Location.ARM,  Side.LEFT,  1, "left hand"),
    HAND_RIGHT_2 ( 12, Location.ARM,  Side.RIGHT, 2, "2nd right hand"),
    HAND_LEFT_2  ( 13, Location.ARM,  Side.LEFT,  2, "2nd left hand"),
    HAND_RIGHT_3 ( 14, Location.ARM,  Side.RIGHT, 3, "3rd right hand"),
    HAND_LEFT_3  ( 15, Location.ARM,  Side.LEFT,  3, "3rd left hand"),
    TAIL         ( 16, Location.TAIL, Side.ANY,   1, "tail"),
    WING_RIGHT   ( 17, Location.WING, Side.RIGHT, 1, "right wing"),
    WING_LEFT    ( 18, Location.WING, Side.LEFT,  1, "left wing"),
    BODY         ( 19, Location.BODY, Side.ANY,   1, "Body"),
    BODY_2       ( 20, Location.BODY, Side.ANY,   2, "Body"),
    BODY_3       ( 21, Location.BODY, Side.ANY,   3, "Body"),
    BODY_4       ( 22, Location.BODY, Side.ANY,   4, "Body"),
    BODY_5       ( 23, Location.BODY, Side.ANY,   5, "Body"),
    BODY_6       ( 24, Location.BODY, Side.ANY,   6, "Body"),
    BODY_7       ( 25, Location.BODY, Side.ANY,   7, "Body"),
    BODY_8       ( 26, Location.BODY, Side.ANY,   8, "Body");
    private LimbType(int val, Location baseType, Side side, int setId, String name) {
       this.value = (byte) val;
       this.baseType = baseType;
       this.side = side;
       this.setId = setId;
       this.name = name;
    };
    public final Byte value;
    public final Location baseType;
    public final Side side;
    public final int setId;
    public final String name;

    public boolean isHead() {
       return (baseType == Location.HEAD);
    }
    public boolean isHand() {
       return (baseType == Location.ARM);
    }
    public boolean isLeg() {
       return (baseType == Location.LEG);
    }
    public boolean isTail() {
       return (baseType == Location.TAIL);
    }
    public boolean isWing() {
       return (baseType == Location.WING);
    }
    static public LimbType get(Location loc, Side side, Pair pair) {
       for (LimbType t: values()) {
          if ((t.baseType == loc)
              || ((t.baseType == Location.HEAD) && ((loc == Location.EYE) || (loc == Location.NECK))))
          {
             if ((t.side == side) || (side == Side.ANY))
             {
                if (((pair == Pair.ANY)    && (t.setId == 1)) ||
                    ((pair == Pair.FIRST)  && (t.setId == 1)) ||
                    ((pair == Pair.SECOND) && (t.setId == 2)) ||
                    ((pair == Pair.THIRD)  && (t.setId == 3))) {
                   return t;
                }
             }
          }
       }
       if (loc != Location.WEAPON) {
         DebugBreak.debugBreak();
      }
       return null;
    }

    public LimbType getPairedType() {
       if (side == Side.ANY) {
         return this;
       }

       for (LimbType t: values()) {
          if ((t.baseType == this.baseType) &&
              (t.setId == this.setId) &&
              (t.side != this.side))
          {
             return t;
          }
       }
       return null;
    }
    private final static HashMap<Byte, LimbType>    _typeByValue = new HashMap<>();
    private final static HashMap<String, LimbType>  _typeByName  = new HashMap<>();
    public  final static ArrayList<LimbType>        _armTypes    = new ArrayList<>();

    static {
       for (LimbType type : values()) {
          _typeByValue.put(type.value, type);
          _typeByName.put(type.name.toLowerCase(), type);
          if (type.isHand()) {
             _armTypes.add(type);
          }
       }
    }
    public static LimbType getByValue(byte val) {
       return _typeByValue.get(val);
    }
    static public LimbType getByName(String name)
    {
       return _typeByName.get(name.toLowerCase());
    }
 }