package ostrowski.combat.common;

import ostrowski.protocol.SerializableObject;

import java.util.ArrayList;
import java.util.List;

public abstract class EnumOptions <T> extends SerializableObject implements Cloneable
{
   protected final List<T> list = new ArrayList<>();
   protected EnumOptions() {}


   public void add(T opt) {
      if (!list.contains(opt)) {
         list.add(opt);
      }
   }

   public void add(T... opts) {
      for (T opt : opts) {
         if (!list.contains(opt)) {
            list.add(opt);
         }
      }
   }
   @SafeVarargs
   public final void remove(T... opts) {
      for (T opt : opts) {
         list.remove(opt);
      }
   }
   public boolean contains(T opt) {
      return list.contains(opt);
   }

   public T get(int index) {
      return list.get(index);
   }

   public int size() {
      return list.size();
   }

   public void clear() {
      list.clear();
   }

}
