package ostrowski.combat.common;

import java.util.ArrayList;
import java.util.List;

public class EnumOptions <T> implements Cloneable
{
   protected final List<T> _list = new ArrayList<>();
   protected EnumOptions() {}


   public void add(T opt) {
      if (!_list.contains(opt)) {
         _list.add(opt);
      }
   }

   public void add(@SuppressWarnings("unchecked") T... opts) {
      for (T opt : opts) {
         if (!_list.contains(opt)) {
            _list.add(opt);
         }
      }
   }
   @SafeVarargs
   public final void remove(T... opts) {
      for (T opt : opts) {
         _list.remove(opt);
      }
   }
   public boolean contains(T opt) {
      return _list.contains(opt);
   }

   public T get(int index) {
      return _list.get(index);
   }

   public int size() {
      return _list.size();
   }

   public void addAll(EnumOptions<T> options) {
      for (T opt : options._list) {
         add(opt);
      }
   }

   public void clear() {
      _list.clear();
   }

   public EnumOptions<T> logicAndWithSet(EnumOptions<T> availableOptions) {
      EnumOptions<T> newDefOpts = new EnumOptions<>();
      for (T defOpt : _list) {
         if (availableOptions.contains(defOpt)) {
            newDefOpts.add(defOpt);
         }
      }
      return newDefOpts;
   }
}
