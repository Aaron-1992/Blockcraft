package bc;

import java.util.Arrays;

/**
 * Update 6.18: small primitive collections for the redstone network solver.
 *
 * A large dust network used to allocate tens of thousands of boxed {@code Long} and {@code Integer} objects on
 * every block change: discovery coordinates, the wire index map, the BFS queue, the visited set and the re-mesh
 * set all lived in {@code HashMap<Long, ...>} / {@code HashSet<Long>}. These open-addressed tables keep the same
 * behaviour with no per-entry objects, and the instances are reused per thread by {@link Redstone319}.
 */
final class Packed {
   private Packed() {
   }

   static int slot(long key, int mask) {
      long h = key * -7046029254386353131L;
      h ^= h >>> 32;
      return (int)h & mask;
   }

   /** A primitive long set with stable iteration order over its slots. */
   static final class Set {
      private long[] keys = new long[16];
      private boolean[] used = new boolean[16];
      private int size;

      void clear() {
         Arrays.fill(this.used, false);
         this.size = 0;
      }

      int size() {
         return this.size;
      }

      boolean isEmpty() {
         return this.size == 0;
      }

      boolean add(long key) {
         if ((this.size + 1) * 4 >= this.keys.length * 3) {
            this.grow();
         }
         int mask = this.keys.length - 1;
         int i = slot(key, mask);
         while (this.used[i]) {
            if (this.keys[i] == key) {
               return false;
            }
            i = i + 1 & mask;
         }
         this.used[i] = true;
         this.keys[i] = key;
         this.size++;
         return true;
      }

      boolean contains(long key) {
         int mask = this.keys.length - 1;
         int i = slot(key, mask);
         while (this.used[i]) {
            if (this.keys[i] == key) {
               return true;
            }
            i = i + 1 & mask;
         }
         return false;
      }

      private void grow() {
         long[] oldKeys = this.keys;
         boolean[] oldUsed = this.used;
         this.keys = new long[oldKeys.length * 2];
         this.used = new boolean[oldKeys.length * 2];
         this.size = 0;
         for (int i = 0; i < oldKeys.length; i++) {
            if (oldUsed[i]) {
               this.add(oldKeys[i]);
            }
         }
      }

      /** Number of slots to scan with {@link #at(int)}. */
      int slots() {
         return this.keys.length;
      }

      boolean used(int i) {
         return this.used[i];
      }

      long at(int i) {
         return this.keys[i];
      }
   }

   /** A primitive long -> int table. Values are wire indices (always >= 0); -1 means "absent". */
   static final class Map {
      private long[] keys = new long[16];
      private int[] values = new int[16];
      private boolean[] used = new boolean[16];
      private int size;

      void clear() {
         Arrays.fill(this.used, false);
         this.size = 0;
      }

      int size() {
         return this.size;
      }

      int get(long key) {
         int mask = this.keys.length - 1;
         int i = slot(key, mask);
         while (this.used[i]) {
            if (this.keys[i] == key) {
               return this.values[i];
            }
            i = i + 1 & mask;
         }
         return -1;
      }

      boolean containsKey(long key) {
         return this.get(key) >= 0;
      }

      void put(long key, int value) {
         if ((this.size + 1) * 4 >= this.keys.length * 3) {
            this.grow();
         }
         int mask = this.keys.length - 1;
         int i = slot(key, mask);
         while (this.used[i]) {
            if (this.keys[i] == key) {
               this.values[i] = value;
               return;
            }
            i = i + 1 & mask;
         }
         this.used[i] = true;
         this.keys[i] = key;
         this.values[i] = value;
         this.size++;
      }

      private void grow() {
         long[] oldKeys = this.keys;
         int[] oldValues = this.values;
         boolean[] oldUsed = this.used;
         this.keys = new long[oldKeys.length * 2];
         this.values = new int[oldKeys.length * 2];
         this.used = new boolean[oldKeys.length * 2];
         this.size = 0;
         for (int i = 0; i < oldKeys.length; i++) {
            if (oldUsed[i]) {
               this.put(oldKeys[i], oldValues[i]);
            }
         }
      }
   }

   /** Growable int queue for the BFS relaxation (no boxed Integers). */
   static final class IntQueue {
      private int[] a = new int[64];
      private int head, tail;

      void clear() {
         this.head = this.tail = 0;
      }

      boolean isEmpty() {
         return this.head == this.tail;
      }

      void add(int v) {
         if (this.tail == this.a.length) {
            if (this.head > 0) {
               System.arraycopy(this.a, this.head, this.a, 0, this.tail - this.head);
               this.tail -= this.head;
               this.head = 0;
            } else {
               this.a = Arrays.copyOf(this.a, this.a.length * 2);
            }
         }
         this.a[this.tail++] = v;
      }

      int poll() {
         int v = this.a[this.head++];
         if (this.head == this.tail) {
            this.head = this.tail = 0;
         }
         return v;
      }
   }

   /** Growable (x, y, z) queue for wire discovery (no int[] per wire). */
   static final class CoordQueue {
      private int[] xs = new int[64], ys = new int[64], zs = new int[64];
      private int head, tail;

      void clear() {
         this.head = this.tail = 0;
      }

      boolean isEmpty() {
         return this.head == this.tail;
      }

      void add(int x, int y, int z) {
         if (this.tail == this.xs.length) {
            if (this.head > 0) {
               int n = this.tail - this.head;
               System.arraycopy(this.xs, this.head, this.xs, 0, n);
               System.arraycopy(this.ys, this.head, this.ys, 0, n);
               System.arraycopy(this.zs, this.head, this.zs, 0, n);
               this.tail = n;
               this.head = 0;
            } else {
               this.xs = Arrays.copyOf(this.xs, this.xs.length * 2);
               this.ys = Arrays.copyOf(this.ys, this.ys.length * 2);
               this.zs = Arrays.copyOf(this.zs, this.zs.length * 2);
            }
         }
         this.xs[this.tail] = x;
         this.ys[this.tail] = y;
         this.zs[this.tail] = z;
         this.tail++;
      }

      int size() { return this.tail; }
      int xAt(int i) { return this.xs[i]; }
      int yAt(int i) { return this.ys[i]; }
      int zAt(int i) { return this.zs[i]; }
      int x() { return this.xs[this.head]; }
      int y() { return this.ys[this.head]; }
      int z() { return this.zs[this.head]; }

      void poll() {
         this.head++;
         if (this.head == this.tail) {
            this.head = this.tail = 0;
         }
      }
   }
}
