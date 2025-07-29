/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.kingdoms.utils.internal.enumeration;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.data.Pair;

import java.util.*;

/**
 * A specialized {@link Map} implementation for use with enum type keys.  All
 * of the keys in an enum map must come from a single enum type that is
 * specified, explicitly or implicitly, when the map is created.  Enum maps
 * are represented internally as arrays.  This representation is extremely
 * compact and efficient.
 *
 * <p>Enum maps are maintained in the <i>natural order</i> of their keys
 * (the order in which the enum constants are declared).  This is reflected
 * in the iterators returned by the collections views ({@link #keySet()},
 * {@link #entrySet()}, and {@link #values()}).
 *
 * <p>Iterators are not allowed.
 *
 * <p>Null keys are not permitted.  Attempts to insert a null key will
 * throw {@link NullPointerException}.  Attempts to test for the
 * presence of a null key or to remove one will, however, function properly.
 * Null values are permitted.
 *
 * <P>Like most collection implementations {@code EnumMap} is not
 * synchronized. If multiple threads access an enum map concurrently, and at
 * least one of the threads modifies the map, it should be synchronized
 * externally.  This is typically accomplished by synchronizing on some
 * object that naturally encapsulates the enum map.  If no such object exists,
 * the map should be "wrapped" using the {@link Collections#synchronizedMap}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access:
 *
 * <pre>
 *     Map&lt;EnumKey, V&gt; m
 *         = Collections.synchronizedMap(new EnumMap&lt;EnumKey, V&gt;(...));
 * </pre>
 *
 * <p>Implementation note: All basic operations execute in constant time.
 * They are likely (though not guaranteed) to be faster than their
 * {@link HashMap} counterparts.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @see EnumMap
 */
public class QuickEnumMap<K extends Enum<K>, V> implements Map<K, V> {
    /**
     * Array representation of this map.  The ith element is the value
     * to which universe[i] is currently mapped, or null if it isn't
     * mapped to anything, or NULL if it's mapped to null.
     */
    private final transient V[] vals;

    /**
     * The number of mappings in this map.
     */
    private transient int size = 0;

    private final transient K[] universe;

    /**
     * Creates an empty enum map with the fixed table size.
     */
    @SuppressWarnings("unchecked")
    public QuickEnumMap(K[] universe) {
        this.universe = universe;
        vals = (V[]) new Object[universe.length];
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns {@code size() == 0}.
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the
     * specified value.
     *
     * @param value the value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to this value
     */
    @Override
    public boolean containsValue(Object value) {
        for (Object val : vals)
            if (value.equals(val))
                return true;

        return false;
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified
     * key.
     *
     * @param key the key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified
     * key
     */
    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
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
     */
    @Override
    public V get(Object key) {
        Objects.requireNonNull(key, "QuickEnumMap may not contain null keys");
        return vals[((Enum<?>) key).ordinal()];
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with specified key, or
     * {@code null} if there was no mapping for key.  (A {@code null}
     * return can also indicate that the map previously associated
     * {@code null} with the specified key.)
     * @throws NullPointerException if the specified key is null
     */
    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key, "QuickEnumMap may not contain null keys");
        Objects.requireNonNull(value, "QuickEnumMap may not contain null values");

        int index = key.ordinal();
        V oldValue = vals[index];
        vals[index] = value;
        if (oldValue == null) size++;
        return oldValue;
    }

    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param key the key whose mapping is to be removed from the map
     * @return the previous value associated with specified key, or
     * {@code null} if there was no entry for key.  (A {@code null}
     * return can also indicate that the map previously associated
     * {@code null} with the specified key.)
     */
    @Override
    public V remove(Object key) {
        Objects.requireNonNull(key, "QuickEnumMap may not contain null keys");
        int index = ((Enum<?>) key).ordinal();
        V oldValue = vals[index];
        vals[index] = null;
        if (oldValue != null) size--;
        return oldValue;
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     *
     * @param m the mappings to be stored in this map
     * @throws NullPointerException the specified map is null, or if
     *                              one or more keys in the specified map are null
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            vals[entry.getKey().ordinal()] = entry.getValue();
        }
    }

    @Override
    public void clear() {
        Arrays.fill(vals, null);
        size = 0;
    }

    @NonNull
    @Override
    public Set<K> keySet() {
        Set<K> keys = new QuickEnumSet<>(universe);

        for (int i = 0; i < vals.length; i++) {
            if (vals[i] != null) {
                keys.add(universe[i]);
            }
        }

        return Collections.unmodifiableSet(keys);
    }

    @NonNull
    @Override
    public Collection<V> values() {
        List<V> values = new ArrayList<>(size);

        for (V val : vals)
            if (val != null)
                values.add(val);

        return Collections.unmodifiableList(values);
    }

    @NonNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entries = new HashSet<>(size);

        for (int i = 0; i < vals.length; i++) {
            V val = vals[i];
            if (val != null) entries.add(Pair.of(universe[i], val));
        }

        return Collections.unmodifiableSet(entries);
    }

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException("Cannot check equality between QuickEnumMap");
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("Cannot genereate hashcode for QuickEnumMap");
    }
}
