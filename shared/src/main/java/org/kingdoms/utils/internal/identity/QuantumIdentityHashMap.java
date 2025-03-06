/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.kingdoms.utils.internal.identity;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * This class implements the {@code Map} interface with a hash table, using
 * reference-equality in place of object-equality when comparing keys (and
 * values).  In other words, in an {@code QuantumIdentityHashMap}, two keys
 * {@code k1} and {@code k2} are considered equal if and only if
 * {@code (k1==k2)}.  (In normal {@code Map} implementations (like
 * {@code HashMap}) two keys {@code k1} and {@code k2} are considered equal
 * if and only if {@code (k1==null ? k2==null : k1.equals(k2))}.)
 *
 * <p><b>This class is <i>not</i> a general-purpose {@code Map}
 * implementation!  While this class implements the {@code Map} interface, it
 * intentionally violates {@code Map's} general contract, which mandates the
 * use of the {@code equals} method when comparing objects.  This class is
 * designed for use only in the rare cases wherein reference-equality
 * semantics are required.</b>
 *
 * <p>A typical use of this class is <i>topology-preserving object graph
 * transformations</i>, such as serialization or deep-copying.  To perform such
 * a transformation, a program must maintain a "node table" that keeps track
 * of all the object references that have already been processed.  The node
 * table must not equate distinct objects even if they happen to be equal.
 * Another typical use of this class is to maintain <i>proxy objects</i>.  For
 * example, a debugging facility might wish to maintain a proxy object for
 * each object in the program being debugged.
 *
 * <p>This class provides all of the optional map operations, and permits
 * {@code null} values and the {@code null} key.  This class makes no
 * guarantees as to the order of the map; in particular, it does not guarantee
 * that the order will remain constant over time.
 *
 * <p>This class provides constant-time performance for the basic
 * operations ({@code get} and {@code put}), assuming the system
 * identity hash function ({@link System#identityHashCode(Object)})
 * disperses elements properly among the buckets.
 *
 * <p>This class has one tuning parameter (which affects performance but not
 * semantics): <i>expected maximum size</i>.  This parameter is the maximum
 * number of key-value mappings that the map is expected to hold.  Internally,
 * this parameter is used to determine the number of buckets initially
 * comprising the hash table.  The precise relationship between the expected
 * maximum size and the number of buckets is unspecified.
 *
 * <p>If the size of the map (the number of key-value mappings) sufficiently
 * exceeds the expected maximum size, the number of buckets is increased.
 * Increasing the number of buckets ("rehashing") may be fairly expensive, so
 * it pays to create identity hash maps with a sufficiently large expected
 * maximum size.  On the other hand, iteration over collection views requires
 * time proportional to the number of buckets in the hash table, so it
 * pays not to set the expected maximum size too high if you are especially
 * concerned with iteration performance or memory usage.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access an identity hash map concurrently, and at
 * least one of the threads modifies the map structurally, it <i>must</i>
 * be synchronized externally.  (A structural modification is any operation
 * that adds or deletes one or more mappings; merely changing the value
 * associated with a key that an instance already contains is not a
 * structural modification.)  This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the map.
 * <p>
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedMap Collections.synchronizedMap}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the map:<pre>
 *   Map m = Collections.synchronizedMap(new QuantumIdentityHashMap(...));</pre>
 *
 * <p>The iterators returned by the {@code iterator} method of the
 * collections returned by all of this class's "collection view
 * methods" are <i>fail-fast</i>: if the map is structurally modified
 * at any time after the iterator is created, in any way except
 * through the iterator's own {@code remove} method, the iterator
 * will throw a {@link ConcurrentModificationException}.  Thus, in the
 * face of concurrent modification, the iterator fails quickly and
 * cleanly, rather than risking arbitrary, non-deterministic behavior
 * at an undetermined time in the future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>fail-fast iterators should be used only
 * to detect bugs.</i>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 * <p>This is a simple <i>linear-probe</i> hash table,
 *
 * @author Doug Lea and Josh Bloch
 * as described for example in texts by Sedgewick and Knuth.  The array
 * contains alternating keys and values, with keys at even indexes and values
 * at odd indexes. (This arrangement has better locality for large
 * tables than does using separate arrays.)  For many Java implementations
 * and operation mixes, this class will yield better performance than
 * {@link HashMap}, which uses <i>chaining</i> rather than linear-probing.
 * @see System#identityHashCode(Object)
 * @see Object#hashCode()
 * @see Collection
 * @see Map
 * @see HashMap
 * @see TreeMap
 * @since 1.4
 */

public class QuantumIdentityHashMap<K, V> implements Map<K, V>, java.io.Serializable, Cloneable {
    /**
     * The initial capacity used by the no-args constructor.
     * MUST be a power of two.  The value 32 corresponds to the
     * (specified) expected maximum size of 21, given a load factor
     * of 2/3.
     */
    private static final int DEFAULT_CAPACITY = 32;

    /**
     * The minimum capacity, used if a lower value is implicitly specified
     * by either of the constructors with arguments.  The value 4 corresponds
     * to an expected maximum size of 2, given a load factor of 2/3.
     * MUST be a power of two.
     */
    private static final int MINIMUM_CAPACITY = 4;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<29.
     * <p>
     * In fact, the map can hold no more than MAXIMUM_CAPACITY-1 items
     * because it has to have at least one slot with the key == null
     * in order to avoid infinite loops in get(), put(), remove()
     */
    private static final int MAXIMUM_CAPACITY = 1 << 29;
    private static final long serialVersionUID = 8188218128353913216L;
    /**
     * The table, resized as necessary. Length MUST always be a power of two.
     */
    transient Object[] table; // non-private to simplify nested class access
    /**
     * The number of key-value mappings contained in this identity hash map.
     *
     * @serial
     */
    int size;
    /**
     * The number of modifications, to support fast-fail iterators
     */
    transient int modCount;
    /**
     * This field is initialized to contain an instance of the entry set
     * view the first time this view is requested.  The view is stateless,
     * so there's no reason to create more than one.
     */
    private transient Set<Map.Entry<K, V>> entrySet;
    /**
     * Each of these fields are initialized to contain an instance of the
     * appropriate view the first time this view is requested.  The views are
     * stateless, so there's no reason to create more than one of each.
     *
     * <p>Since there is no synchronization performed while accessing these fields,
     * it is expected that java.util.Map view classes using these fields have
     * no non-final fields (or any fields at all except for outer-this). Adhering
     * to this rule would make the races on these fields benign.
     *
     * <p>It is also imperative that implementations read the field only once,
     * as in:
     *
     * <pre> {@code
     * public Set<K> keySet() {
     *   Set<K> ks = keySet;  // single racy read
     *   if (ks == null) {
     *     ks = new KeySet();
     *     keySet = ks;
     *   }
     *   return ks;
     * }
     * }</pre>
     */
    private transient Set<K> keySet;
    private transient Collection<V> values;

    /**
     * Constructs a new, empty identity hash map with a default expected
     * maximum size (21).
     */
    public QuantumIdentityHashMap() {
        init(DEFAULT_CAPACITY);
    }

    /**
     * Constructs a new, empty map with the specified expected maximum size.
     * Putting more than the expected number of key-value mappings into
     * the map may cause the internal data structure to grow, which may be
     * somewhat time-consuming.
     *
     * @param expectedMaxSize the expected maximum size of the map
     * @throws IllegalArgumentException if {@code expectedMaxSize} is negative
     */
    public QuantumIdentityHashMap(int expectedMaxSize) {
        if (expectedMaxSize < 0) throw new IllegalArgumentException("expectedMaxSize is negative: " + expectedMaxSize);
        init(capacity(expectedMaxSize));
    }

    /**
     * Constructs a new identity hash map containing the keys-value mappings
     * in the specified map.
     *
     * @param m the map whose mappings are to be placed into this map
     * @throws NullPointerException if the specified map is null
     */
    public QuantumIdentityHashMap(Map<? extends K, ? extends V> m) {
        // Allow for a bit of growth
        this((int) ((1 + m.size()) * 1.1));
        putAll(m);
    }

    /**
     * Returns the appropriate capacity for the given expected maximum size.
     * Returns the smallest power of two between MINIMUM_CAPACITY and
     * MAXIMUM_CAPACITY, inclusive, that is greater than (3 *
     * expectedMaxSize)/2, if such a number exists.  Otherwise returns
     * MAXIMUM_CAPACITY.
     */
    private static int capacity(int expectedMaxSize) {
        // assert expectedMaxSize >= 0;
        return (expectedMaxSize > MAXIMUM_CAPACITY / 3) ? MAXIMUM_CAPACITY :
                (expectedMaxSize <= 2 * MINIMUM_CAPACITY / 3) ? MINIMUM_CAPACITY :
                        Integer.highestOneBit(expectedMaxSize + (expectedMaxSize << 1));
    }

    /**
     * Returns index for Object x.
     */
    private static int hash(Object x, int length) {
        int h = System.identityHashCode(x);
        // Multiply by -254 to use the hash LSB and to ensure index is even
        return ((h << 1) - (h << 8)) & (length - 1);
    }

    /**
     * Circularly traverses table of size len.
     */
    private static int nextKeyIndex(int i, int len) {
        return i + 2 < len ? i + 2 : 0;
    }

    /**
     * Initializes object to be an empty map with the specified initial
     * capacity, which is assumed to be a power of two between
     * MINIMUM_CAPACITY and MAXIMUM_CAPACITY inclusive.
     */
    private void init(int initCapacity) {
        // assert (initCapacity & -initCapacity) == initCapacity; // power of 2
        // assert initCapacity >= MINIMUM_CAPACITY;
        // assert initCapacity <= MAXIMUM_CAPACITY;
        table = new Object[2 * initCapacity];
    }

    /**
     * Returns the number of key-value mappings in this identity hash map.
     *
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns {@code true} if this identity hash map contains no key-value
     * mappings.
     *
     * @return {@code true} if this identity hash map contains no key-value
     * mappings
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key == k)},
     * then this method returns {@code v}; otherwise it returns
     * {@code null}.  (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        Object[] tab = table;
        int len = tab.length;
        int i = hash(key, len);

        while (true) {
            Object item = tab[i];
            if (item == key) return (V) tab[i + 1];
            if (item == null) return null;
            i = nextKeyIndex(i, len);
        }
    }

    /**
     * Tests whether the specified object reference is a key in this identity
     * hash map.
     *
     * @param key possible key
     * @return {@code true} if the specified object reference is a key
     * in this map
     * @see #containsValue(Object)
     */
    @Override
    public boolean containsKey(Object key) {
        Object[] tab = table;
        int len = tab.length;
        int i = hash(key, len);

        while (true) {
            Object item = tab[i];
            if (item == key) return true;
            if (item == null) return false;
            i = nextKeyIndex(i, len);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Tests if the specified key-value mapping is in the map.
     *
     * @param key   possible key
     * @param value possible value
     * @return {@code true} if and only if the specified key-value
     * mapping is in the map
     */
    private boolean containsMapping(Object key, Object value) {
        Object[] tab = table;
        int len = tab.length;
        int i = hash(key, len);

        while (true) {
            Object item = tab[i];
            if (item == key) return tab[i + 1] == value;
            if (item == null) return false;
            i = nextKeyIndex(i, len);
        }
    }

    /**
     * Associates the specified value with the specified key in this identity
     * hash map.  If the map previously contained a mapping for the key, the
     * old value is replaced.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     * {@code null} if there was no mapping for {@code key}.
     * (A {@code null} return can also indicate that the map
     * previously associated {@code null} with {@code key}.)
     * @see Object#equals(Object)
     * @see #get(Object)
     * @see #containsKey(Object)
     */
    @Override
    public V put(@NonNull K key, @Nullable V value) {
        Objects.requireNonNull(key, "Cannot put null key");
        while (true) {
            Object[] tab = table;
            int len = tab.length;
            int i = hash(key, len);

            for (Object item; (item = tab[i]) != null; i = nextKeyIndex(i, len)) {
                if (item == key) {
                    @SuppressWarnings("unchecked")
                    V oldValue = (V) tab[i + 1];
                    tab[i + 1] = value;
                    return oldValue;
                }
            }

            int s = size + 1;
            // Use optimized form of 3 * s.
            // Next capacity is len, 2 * current capacity.
            if (s + (s << 1) > len && resize(len)) continue;

            modCount++;
            tab[i] = key;
            tab[i + 1] = value;
            size = s;
            return null;
        }
    }

    /**
     * Resizes the table if necessary to hold given capacity.
     *
     * @param newCapacity the new capacity, must be a power of two.
     * @return whether a resize did in fact take place
     */
    private boolean resize(int newCapacity) {
        // assert (newCapacity & -newCapacity) == newCapacity; // power of 2
        int newLength = newCapacity * 2;

        int oldLength = table.length;
        if (oldLength == 2 * MAXIMUM_CAPACITY) { // can't expand any further
            if (size == MAXIMUM_CAPACITY - 1) throw new IllegalStateException("Capacity exhausted.");
            return false;
        }
        if (oldLength >= newLength) return false;

        Object[] oldTable = table;
        Object[] newTable = new Object[newLength];

        for (int j = 0; j < oldLength; j += 2) {
            Object key = oldTable[j];
            if (key != null) {
                Object value = oldTable[j + 1];
                oldTable[j] = null;
                oldTable[j + 1] = null;
                int i = hash(key, newLength);
                while (newTable[i] != null) i = nextKeyIndex(i, newLength);
                newTable[i] = key;
                newTable[i + 1] = value;
            }
        }

        table = newTable;
        return true;
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     * @throws NullPointerException if the specified map is null
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        int n = m.size();
        if (n == 0) return;
        if (n > size) resize(capacity(n)); // conservatively pre-expand
        for (Entry<? extends K, ? extends V> e : m.entrySet()) put(e.getKey(), e.getValue());
    }

    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with {@code key}, or
     * {@code null} if there was no mapping for {@code key}.
     * (A {@code null} return can also indicate that the map
     * previously associated {@code null} with {@code key}.)
     */
    @Override
    public V remove(@NonNull Object key) {
        Objects.requireNonNull(key, "Quantum Identity HashMap cannot contain null keys");
        Object[] tab = table;
        int len = tab.length;
        int i = hash(key, len);

        while (true) {
            Object item = tab[i];
            if (item == key) {
                modCount++;
                size--;
                @SuppressWarnings("unchecked")
                V oldValue = (V) tab[i + 1];
                tab[i + 1] = null;
                tab[i] = null;
                closeDeletion(i);
                return oldValue;
            }
            if (item == null) return null;
            i = nextKeyIndex(i, len);
        }
    }

    /**
     * Removes the specified key-value mapping from the map if it is present.
     *
     * @param key   possible key
     * @param value possible value
     * @return {@code true} if and only if the specified key-value
     * mapping was in the map
     */
    private boolean removeMapping(Object key, Object value) {
        Object[] tab = table;
        int len = table.length;
        int i = hash(key, len);

        while (true) {
            Object item = tab[i];
            if (item == key) {
                if (tab[i + 1] != value) return false;
                modCount++;
                size--;
                tab[i] = null;
                tab[i + 1] = null;
                closeDeletion(i);
                return true;
            }

            if (item == null) return false;
            i = nextKeyIndex(i, len);
        }
    }

    /**
     * Rehash all possibly-colliding entries following a
     * deletion. This preserves the linear-probe
     * collision properties required by get, put, etc.
     *
     * @param d the index of a newly empty deleted slot
     */
    private void closeDeletion(int d) {
        // Adapted from Knuth Section 6.4 Algorithm R
        Object[] tab = table;
        int len = tab.length;

        // Look for items to swap into newly vacated slot
        // starting at index immediately following deletion,
        // and continuing until a null slot is seen, indicating
        // the end of a run of possibly-colliding keys.
        Object item;
        for (int i = nextKeyIndex(d, len); (item = tab[i]) != null;
             i = nextKeyIndex(i, len)) {
            // The following test triggers if the item at slot i (which
            // hashes to be at slot r) should take the spot vacated by d.
            // If so, we swap it in, and then continue with d now at the
            // newly vacated i.  This process will terminate when we hit
            // the null slot at the end of this run.
            // The test is messy because we are using a circular table.
            int r = hash(item, len);
            if ((i < r && (r <= d || d <= i)) || (r <= d && d <= i)) {
                tab[d] = item;
                tab[d + 1] = tab[i + 1];
                tab[i] = null;
                tab[i + 1] = null;
                d = i;
            }
        }
    }

    /**
     * Removes all the mappings from this map.
     * The map will be empty after this call returns.
     */
    @Override
    public void clear() {
        modCount++;
        Arrays.fill(table, null);
        size = 0;
    }

    /**
     * Returns a shallow copy of this identity hash map: the keys and values
     * themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    @Override
    public Object clone() {
        try {
            QuantumIdentityHashMap<?, ?> m = (QuantumIdentityHashMap<?, ?>) super.clone();
            m.entrySet = null;
            m.table = table.clone();
            return m;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    // Views

    /**
     * Returns an identity-based set view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are reflected in
     * the set, and vice-versa.  If the map is modified while an iteration
     * over the set is in progress, the results of the iteration are
     * undefined.  The set supports element removal, which removes the
     * corresponding mapping from the map, via the {@code Iterator.remove},
     * {@code Set.remove}, {@code removeAll}, {@code retainAll}, and
     * {@code clear} methods.  It does not support the {@code add} or
     * {@code addAll} methods.
     *
     * <p><b>While the object returned by this method implements the
     * {@code Set} interface, it does <i>not</i> obey {@code Set's} general
     * contract.  Like its backing map, the set returned by this method
     * defines element equality as reference-equality rather than
     * object-equality.  This affects the behavior of its {@code contains},
     * {@code remove}, {@code containsAll}, {@code equals}, and
     * {@code hashCode} methods.</b>
     *
     * <p><b>The {@code equals} method of the returned set returns {@code true}
     * only if the specified object is a set containing exactly the same
     * object references as the returned set.  The symmetry and transitivity
     * requirements of the {@code Object.equals} contract may be violated if
     * the set returned by this method is compared to a normal set.  However,
     * the {@code Object.equals} contract is guaranteed to hold among sets
     * returned by this method.</b>
     *
     * <p>The {@code hashCode} method of the returned set returns the sum of
     * the <i>identity hashcodes</i> of the elements in the set, rather than
     * the sum of their hashcodes.  This is mandated by the change in the
     * semantics of the {@code equals} method, in order to enforce the
     * general contract of the {@code Object.hashCode} method among sets
     * returned by this method.
     *
     * @return an identity-based set view of the keys contained in this map
     * @see Object#equals(Object)
     * @see System#identityHashCode(Object)
     */
    public @NonNull Set<K> keySet() {
        return keySet == null ? keySet = new KeySet() : keySet;
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress,
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the {@code Iterator.remove},
     * {@code Collection.remove}, {@code removeAll},
     * {@code retainAll} and {@code clear} methods.  It does not
     * support the {@code add} or {@code addAll} methods.
     *
     * <p><b>While the object returned by this method implements the
     * {@code Collection} interface, it does <i>not</i> obey
     * {@code Collection's} general contract.  Like its backing map,
     * the collection returned by this method defines element equality as
     * reference-equality rather than object-equality.  This affects the
     * behavior of its {@code contains}, {@code remove} and
     * {@code containsAll} methods.</b>
     */
    public @NonNull Collection<V> values() {
        return values == null ? values = new Values() : values;
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * Each element in the returned set is a reference-equality-based
     * {@code Map.Entry}.  The set is backed by the map, so changes
     * to the map are reflected in the set, and vice-versa.  If the
     * map is modified while an iteration over the set is in progress,
     * the results of the iteration are undefined.  The set supports
     * element removal, which removes the corresponding mapping from
     * the map, via the {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll} and {@code clear}
     * methods.  It does not support the {@code add} or
     * {@code addAll} methods.
     *
     * <p>Like the backing map, the {@code Map.Entry} objects in the set
     * returned by this method define key and value equality as
     * reference-equality rather than object-equality.  This affects the
     * behavior of the {@code equals} and {@code hashCode} methods of these
     * {@code Map.Entry} objects.  A reference-equality based {@code Map.Entry
     * e} is equal to an object {@code o} if and only if {@code o} is a
     * {@code Map.Entry} and {@code e.getKey()==o.getKey() &&
     * e.getValue()==o.getValue()}.  To accommodate these equals
     * semantics, the {@code hashCode} method returns
     * {@code System.identityHashCode(e.getKey()) ^
     * System.identityHashCode(e.getValue())}.
     *
     * <p><b>Owing to the reference-equality-based semantics of the
     * {@code Map.Entry} instances in the set returned by this method,
     * it is possible that the symmetry and transitivity requirements of
     * the {@link Object#equals(Object)} contract may be violated if any of
     * the entries in the set is compared to a normal map entry, or if
     * the set returned by this method is compared to a set of normal map
     * entries (such as would be returned by a call to this method on a normal
     * map).  However, the {@code Object.equals} contract is guaranteed to
     * hold among identity-based map entries, and among sets of such entries.
     * </b>
     *
     * @return a set view of the identity-mappings contained in this map
     */
    public @NonNull Set<Map.Entry<K, V>> entrySet() {
        return entrySet == null ? entrySet = new EntrySet() : entrySet;
    }

    /**
     * Saves the state of the {@code QuantumIdentityHashMap} instance to a stream
     * (i.e., serializes it).
     *
     * @serialData The <i>size</i> of the HashMap (the number of key-value
     * mappings) ({@code int}), followed by the key (Object) and
     * value (Object) for each key-value mapping represented by the
     * QuantumIdentityHashMap.  The key-value mappings are emitted in no
     * particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        // Write out and any hidden stuff
        s.defaultWriteObject();

        // Write out size (number of Mappings)
        s.writeInt(size);

        // Write out keys and values (alternating)
        Object[] tab = table;
        for (int i = 0; i < tab.length; i += 2) {
            Object key = tab[i];
            if (key != null) {
                s.writeObject(key);
                s.writeObject(tab[i + 1]);
            }
        }
    }

    /**
     * Reconstitutes the {@code QuantumIdentityHashMap} instance from a stream (i.e.,
     * deserializes it).
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        Object[] tab = table;
        int expectedModCount = modCount;

        for (int index = 0; index < tab.length; index += 2) {
            Object k = tab[index];
            if (k != null) action.accept((K) k, (V) tab[index + 1]);
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        Object[] tab = table;
        int expectedModCount = modCount;

        for (int index = 0; index < tab.length; index += 2) {
            Object k = tab[index];
            if (k != null) tab[index + 1] = function.apply((K) k, (V) tab[index + 1]);
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
        }
    }

    /**
     * Similar form as array-based Spliterators, but skips blank elements,
     * and guestimates size as decreasing by half per split.
     */
    static class IdentityHashMapSpliterator<K, V> {
        final QuantumIdentityHashMap<K, V> map;
        int index;             // current index, modified on advance/split
        int fence;             // -1 until first use; then one past last index
        int est;               // size estimate
        int expectedModCount;  // initialized when fence set

        IdentityHashMapSpliterator(QuantumIdentityHashMap<K, V> map, int origin, int fence, int est, int expectedModCount) {
            this.map = map;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getFence() { // initialize fence and size on first use
            if (fence < 0) {
                est = map.size;
                expectedModCount = map.modCount;
                fence = map.table.length;
            }
            return fence;
        }

        public final long estimateSize() {
            getFence(); // force init
            return est;
        }
    }

    static final class KeySpliterator<K, V>
            extends IdentityHashMapSpliterator<K, V>
            implements Spliterator<K> {
        KeySpliterator(QuantumIdentityHashMap<K, V> map, int origin, int fence, int est, int expectedModCount) {
            super(map, origin, fence, est, expectedModCount);
        }

        public KeySpliterator<K, V> trySplit() {
            int lo = index, mid = ((lo + getFence()) >>> 1) & ~1;
            return (lo >= mid) ? null : new KeySpliterator<>(map, lo, index = mid, est >>>= 1, expectedModCount);
        }

        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super K> action) {
            Objects.requireNonNull(action);
            int i, hi;
            QuantumIdentityHashMap<K, V> m = map;
            Object[] tab;

            if (m != null && (tab = m.table) != null && (i = index) >= 0 && (index = hi = getFence()) <= tab.length) {
                for (; i < hi; i += 2) {
                    Object key = tab[i];
                    if (key != null) action.accept((K) key);
                }
                if (m.modCount == expectedModCount) return;
            }
            throw new ConcurrentModificationException();
        }

        @SuppressWarnings("unchecked")
        public boolean tryAdvance(Consumer<? super K> action) {
            Objects.requireNonNull(action);
            Object[] tab = map.table;
            int hi = getFence();

            while (index < hi) {
                Object key = tab[index];
                index += 2;
                if (key != null) {
                    action.accept((K) key);
                    if (map.modCount != expectedModCount) throw new ConcurrentModificationException();
                    return true;
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? SIZED : 0) | Spliterator.DISTINCT;
        }
    }

    static final class ValueSpliterator<K, V>
            extends IdentityHashMapSpliterator<K, V>
            implements Spliterator<V> {
        ValueSpliterator(QuantumIdentityHashMap<K, V> m, int origin, int fence, int est, int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public ValueSpliterator<K, V> trySplit() {
            int hi = getFence(), lo = index, mid = ((lo + hi) >>> 1) & ~1;
            return (lo >= mid) ? null :
                    new ValueSpliterator<>(map, lo, index = mid, est >>>= 1, expectedModCount);
        }

        public void forEachRemaining(Consumer<? super V> action) {
            Objects.requireNonNull(action);
            int i, hi;
            QuantumIdentityHashMap<K, V> m;
            Object[] a;
            if ((m = map) != null && (a = m.table) != null && (i = index) >= 0 && (index = hi = getFence()) <= a.length) {
                for (; i < hi; i += 2) {
                    if (a[i] != null) {
                        @SuppressWarnings("unchecked") V v = (V) a[i + 1];
                        action.accept(v);
                    }
                }
                if (m.modCount == expectedModCount) return;
            }
            throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            Objects.requireNonNull(action);
            Object[] a = map.table;
            int hi = getFence();

            while (index < hi) {
                Object key = a[index];
                @SuppressWarnings("unchecked") V v = (V) a[index + 1];
                index += 2;
                if (key != null) {
                    action.accept(v);
                    if (map.modCount != expectedModCount) throw new ConcurrentModificationException();
                    return true;
                }
            }
            return false;
        }

        public int characteristics() {
            return fence < 0 || est == map.size ? SIZED : 0;
        }
    }

    static final class EntrySpliterator<K, V> extends IdentityHashMapSpliterator<K, V> implements Spliterator<Map.Entry<K, V>> {
        EntrySpliterator(QuantumIdentityHashMap<K, V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public EntrySpliterator<K, V> trySplit() {
            int lo = index, mid = ((lo + getFence()) >>> 1) & ~1;
            return lo >= mid ? null : new EntrySpliterator<>(map, lo, index = mid, est >>>= 1, expectedModCount);
        }

        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> action) {
            Objects.requireNonNull(action);
            int i, hi;
            QuantumIdentityHashMap<K, V> m;
            Object[] a;
            if ((m = map) != null && (a = m.table) != null && (i = index) >= 0 && (index = hi = getFence()) <= a.length) {
                for (; i < hi; i += 2) {
                    Object key = a[i];
                    if (key != null) {
                        @SuppressWarnings("unchecked") K k = (K) key;
                        @SuppressWarnings("unchecked") V v = (V) a[i + 1];
                        action.accept(new AbstractMap.SimpleImmutableEntry<>(k, v));
                    }
                }
                if (m.modCount == expectedModCount) return;
            }
            throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> action) {
            Objects.requireNonNull(action);
            Object[] a = map.table;
            int hi = getFence();

            while (index < hi) {
                Object key = a[index];
                @SuppressWarnings("unchecked") V v = (V) a[index + 1];
                index += 2;
                if (key != null) {
                    @SuppressWarnings("unchecked") K k = (K) key;
                    action.accept(new AbstractMap.SimpleImmutableEntry<>(k, v));
                    if (map.modCount != expectedModCount) throw new ConcurrentModificationException();
                    return true;
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? SIZED : 0) | Spliterator.DISTINCT;
        }
    }

    private abstract class IdentityHashMapIterator<T> implements Iterator<T> {
        int index = (size != 0 ? 0 : table.length); // current slot.
        int expectedModCount = modCount; // to support fast-fail
        int lastReturnedIndex = -1;      // to allow remove()
        boolean indexValid; // To avoid unnecessary next computation
        Object[] traversalTable = table; // reference to main table or copy

        public boolean hasNext() {
            Object[] tab = traversalTable;
            for (int i = index; i < tab.length; i += 2) {
                Object key = tab[i];
                if (key != null) {
                    index = i;
                    return indexValid = true;
                }
            }
            index = tab.length;
            return false;
        }

        protected int nextIndex() {
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
            if (!indexValid && !hasNext()) throw new NoSuchElementException();

            indexValid = false;
            lastReturnedIndex = index;
            index += 2;
            return lastReturnedIndex;
        }

        public void remove() {
            if (lastReturnedIndex == -1) throw new IllegalStateException();
            if (modCount != expectedModCount) throw new ConcurrentModificationException();

            expectedModCount = ++modCount;
            int deletedSlot = lastReturnedIndex;
            lastReturnedIndex = -1;
            // back up index to revisit new contents after deletion
            index = deletedSlot;
            indexValid = false;

            // Removal code proceeds as in closeDeletion except that
            // it must catch the rare case where an element already
            // seen is swapped into a vacant slot that will be later
            // traversed by this iterator. We cannot allow future
            // next() calls to return it again.  The likelihood of
            // this occurring under 2/3 load factor is very slim, but
            // when it does happen, we must make a copy of the rest of
            // the table to use for the rest of the traversal. Since
            // this can only happen when we are near the end of the table,
            // even in these rare cases, this is not very expensive in
            // time or space.

            Object[] tab = traversalTable;
            int len = tab.length;

            int d = deletedSlot;
            Object key = tab[d];
            tab[d] = null;        // vacate the slot
            tab[d + 1] = null;

            // If traversing a copy, remove in real table.
            // We can skip gap-closure on copy.
            if (tab != QuantumIdentityHashMap.this.table) {
                QuantumIdentityHashMap.this.remove(key);
                expectedModCount = modCount;
                return;
            }

            size--;

            Object item;
            for (int i = nextKeyIndex(d, len); (item = tab[i]) != null;
                 i = nextKeyIndex(i, len)) {
                int r = hash(item, len);
                // See closeDeletion for explanation of this conditional
                if ((i < r && (r <= d || d <= i)) || (r <= d && d <= i)) {

                    // If we are about to swap an already-seen element
                    // into a slot that may later be returned by next(),
                    // then clone the rest of table for use in future
                    // next() calls. It is OK that our copy will have
                    // a gap in the "wrong" place, since it will never
                    // be used for searching anyway.

                    if (i < deletedSlot && d >= deletedSlot && traversalTable == QuantumIdentityHashMap.this.table) {
                        int remaining = len - deletedSlot;
                        Object[] newTable = new Object[remaining];
                        System.arraycopy(tab, deletedSlot, newTable, 0, remaining);
                        traversalTable = newTable;
                        index = 0;
                    }

                    tab[d] = item;
                    tab[d + 1] = tab[i + 1];
                    tab[i] = null;
                    tab[i + 1] = null;
                    d = i;
                }
            }
        }
    }

    private class KeyIterator extends IdentityHashMapIterator<K> {
        @SuppressWarnings("unchecked")
        public K next() {
            return (K) traversalTable[nextIndex()];
        }
    }

    private class ValueIterator extends IdentityHashMapIterator<V> {
        @SuppressWarnings("unchecked")
        public V next() {
            return (V) traversalTable[nextIndex() + 1];
        }
    }

    private class EntryIterator
            extends IdentityHashMapIterator<Map.Entry<K, V>> {
        private Entry lastReturnedEntry;

        @SuppressWarnings("ReturnOfInnerClass")
        public Map.Entry<K, V> next() {
            return lastReturnedEntry = new Entry(nextIndex());
        }

        public void remove() {
            lastReturnedIndex = ((null == lastReturnedEntry) ? -1 : lastReturnedEntry.index);
            super.remove();
            lastReturnedEntry.index = lastReturnedIndex;
            lastReturnedEntry = null;
        }

        private class Entry implements Map.Entry<K, V> {
            private int index;

            private Entry(int index) {
                this.index = index;
            }

            @SuppressWarnings("unchecked")
            public K getKey() {
                checkIndexForEntryUse();
                return (K) traversalTable[index];
            }

            @SuppressWarnings("unchecked")
            public V getValue() {
                checkIndexForEntryUse();
                return (V) traversalTable[index + 1];
            }

            @SuppressWarnings("unchecked")
            public V setValue(V value) {
                checkIndexForEntryUse();
                V oldValue = (V) traversalTable[index + 1];
                traversalTable[index + 1] = value;
                // if shadowing, force into main table
                if (traversalTable != QuantumIdentityHashMap.this.table) put((K) traversalTable[index], value);
                return oldValue;
            }

            public boolean equals(Object o) {
                if (index < 0) return super.equals(o);
                if (!(o instanceof Map.Entry)) return false;
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                return e.getKey() == traversalTable[index] && e.getValue() == traversalTable[index + 1];
            }

            public int hashCode() {
                if (lastReturnedIndex < 0)
                    return super.hashCode();

                return System.identityHashCode(traversalTable[index]) ^ System.identityHashCode(traversalTable[index + 1]);
            }

            public String toString() {
                if (index < 0) return super.toString();
                return (traversalTable[index] + "=" + traversalTable[index + 1]);
            }

            private void checkIndexForEntryUse() {
                if (index < 0) throw new IllegalStateException("Entry was removed");
            }
        }
    }

    private class KeySet extends AbstractSet<K> {
        @SuppressWarnings("ReturnOfInnerClass")
        public @NonNull Iterator<K> iterator() {
            return new KeyIterator();
        }

        public int size() {
            return size;
        }

        public boolean contains(Object o) {
            return containsKey(o);
        }

        public boolean remove(Object o) {
            int oldSize = size;
            QuantumIdentityHashMap.this.remove(o);
            return size != oldSize;
        }

        /*
         * Must revert from AbstractSet's impl to AbstractCollection's, as
         * the former contains an optimization that results in incorrect
         * behavior when c is a smaller "normal" (non-identity-based) Set.
         */
        public boolean removeAll(Collection<?> c) {
            Objects.requireNonNull(c);
            boolean modified = false;
            for (Iterator<K> i = iterator(); i.hasNext(); ) {
                if (c.contains(i.next())) {
                    i.remove();
                    modified = true;
                }
            }
            return modified;
        }

        public void clear() {
            QuantumIdentityHashMap.this.clear();
        }

        @SuppressWarnings("ObjectInstantiationInEqualsHashCode")
        public int hashCode() {
            int result = 0;
            for (K key : this) result += System.identityHashCode(key);
            return result;
        }

        public Object[] toArray() {
            return toArray(new Object[0]);
        }

        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            int expectedModCount = modCount;
            int size = size();
            if (a.length < size) a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            Object[] tab = table;
            int ti = 0;

            for (int si = 0; si < tab.length; si += 2) {
                Object key;
                if ((key = tab[si]) != null) { // key present ?
                    // more elements than expected -> concurrent modification from other thread
                    if (ti >= size) throw new ConcurrentModificationException();
                    a[ti++] = (T) key; // unmask key
                }
            }

            // fewer elements than expected or concurrent modification from other thread detected
            if (ti < size || expectedModCount != modCount) throw new ConcurrentModificationException();
            // final null marker as per spec
            if (ti < a.length) a[ti] = null;
            return a;
        }

        public Spliterator<K> spliterator() {
            return new KeySpliterator<>(QuantumIdentityHashMap.this, 0, -1, 0, 0);
        }
    }

    private class Values extends AbstractCollection<V> {
        @SuppressWarnings("ReturnOfInnerClass")
        public @NonNull Iterator<V> iterator() {
            return new ValueIterator();
        }

        public int size() {
            return size;
        }

        public boolean contains(Object o) {
            return containsValue(o);
        }

        public boolean remove(Object o) {
            for (Iterator<V> i = iterator(); i.hasNext(); ) {
                if (i.next() == o) {
                    i.remove();
                    return true;
                }
            }
            return false;
        }

        public void clear() {
            QuantumIdentityHashMap.this.clear();
        }

        public Object[] toArray() {
            return toArray(new Object[0]);
        }

        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            int expectedModCount = modCount;
            int size = size();
            if (a.length < size) a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            Object[] tab = table;
            int ti = 0;

            for (int si = 0; si < tab.length; si += 2) {
                if (tab[si] != null) { // key present ?
                    // more elements than expected -> concurrent modification from other thread
                    if (ti >= size) {
                        throw new ConcurrentModificationException();
                    }
                    a[ti++] = (T) tab[si + 1]; // copy value
                }
            }

            // fewer elements than expected or concurrent modification from other thread detected
            if (ti < size || expectedModCount != modCount) throw new ConcurrentModificationException();
            // final null marker as per spec
            if (ti < a.length) a[ti] = null;
            return a;
        }

        public Spliterator<V> spliterator() {
            return new ValueSpliterator<>(QuantumIdentityHashMap.this, 0, -1, 0, 0);
        }
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        @SuppressWarnings("ReturnOfInnerClass")
        public @NonNull Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            return containsMapping(entry.getKey(), entry.getValue());
        }

        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            return removeMapping(entry.getKey(), entry.getValue());
        }

        public int size() {
            return size;
        }

        public void clear() {
            QuantumIdentityHashMap.this.clear();
        }

        /*
         * Must revert from AbstractSet's impl to AbstractCollection's, as
         * the former contains an optimization that results in incorrect
         * behavior when c is a smaller "normal" (non-identity-based) Set.
         */
        public boolean removeAll(Collection<?> c) {
            Objects.requireNonNull(c);
            boolean modified = false;
            for (Iterator<Map.Entry<K, V>> i = iterator(); i.hasNext(); ) {
                if (c.contains(i.next())) {
                    i.remove();
                    modified = true;
                }
            }
            return modified;
        }

        public Object[] toArray() {
            return toArray(new Object[0]);
        }

        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            int expectedModCount = modCount;
            int size = size();
            if (a.length < size) a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);

            Object[] tab = table;
            int ti = 0;
            for (int si = 0; si < tab.length; si += 2) {
                Object key;
                if ((key = tab[si]) != null) { // key present ?
                    // more elements than expected -> concurrent modification from other thread
                    if (ti >= size) throw new ConcurrentModificationException();
                    a[ti++] = (T) new AbstractMap.SimpleEntry<>(key, tab[si + 1]);
                }
            }

            // fewer elements than expected or concurrent modification from other thread detected
            if (ti < size || expectedModCount != modCount) throw new ConcurrentModificationException();
            // final null marker as per spec
            if (ti < a.length) a[ti] = null;
            return a;
        }

        public Spliterator<Map.Entry<K, V>> spliterator() {
            return new EntrySpliterator<>(QuantumIdentityHashMap.this, 0, -1, 0, 0);
        }
    }
}
