/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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
package org.kingdoms.utils.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("all")
public final class UnsafeHashMap<K, V> implements Map<K, V>, Cloneable {
    public static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
    public static final int MAXIMUM_CAPACITY = 1 << 30;
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;
    public static final int TREEIFY_THRESHOLD = 8;
    public static final int UNTREEIFY_THRESHOLD = 6;
    public static final int MIN_TREEIFY_CAPACITY = 64;
    public final float loadFactor;
    public transient Set<K> keySet;
    public transient Collection<V> values;
    public transient Node<K, V>[] table;
    public transient Set<Map.Entry<K, V>> entrySet;
    private transient int size;
    private transient int modCount;
    private int threshold;

    public UnsafeHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY) initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);

        this.loadFactor = loadFactor;
        this.threshold = tableSizeFor(initialCapacity);
    }

    /**
     * Constructs an empty <code>HashMap</code> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public UnsafeHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty <code>HashMap</code> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public UnsafeHashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }

    /**
     * Constructs a new <code>HashMap</code> with the same mappings as the
     * specified <code>Map</code>.  The <code>HashMap</code> is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <code>Map</code>.
     *
     * @param m the map whose mappings are to be placed in this map
     * @throws NullPointerException if the specified map is null
     */
    public UnsafeHashMap(Map<? extends K, ? extends V> m) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(m, false);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <K, V> UnsafeHashMap<K, V> of(Map.Entry<? extends K, ? extends V>... entries) {
        UnsafeHashMap<K, V> map = new UnsafeHashMap<>();
        map.putEntries(false, entries);
        return map;
    }

    public static int hash(Object key) {
        if (key == null) throw new NullPointerException("Cannot hash null key to map");
        int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    public static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            Class<?> c;
            Type[] ts, as;
            Type t;
            ParameterizedType p;
            if ((c = x.getClass()) == String.class) // bypass checks
                return c;
            if ((ts = c.getGenericInterfaces()) != null) {
                for (Type type : ts) {
                    if (((t = type) instanceof ParameterizedType) &&
                            ((p = (ParameterizedType) t).getRawType() ==
                                    Comparable.class) &&
                            (as = p.getActualTypeArguments()) != null &&
                            as.length == 1 && as[0] == c) // type arg is c
                        return c;
                }
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"}) // for cast to Comparable
    public static int compareComparables(Class<?> kc, Object k, Object x) {
        return x == null || x.getClass() != kc ? 0 : ((Comparable) k).compareTo(x);
    }

    public static int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    public void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
        putMapEntries(m, false, evict);
    }

    /**
     * Implements Map.putAll and Map constructor
     *
     * @param m     the map
     * @param evict false when initially constructing this map, else
     *              true (relayed to method afterNodeInsertion).
     */
    public void putMapEntries(Map<? extends K, ? extends V> m, boolean ifAbsent, boolean evict) {
        int s = m.size();
        if (s > 0) {
            if (table == null) { // pre-size
                float ft = ((float) s / loadFactor) + 1.0F;
                int t = ((ft < (float) MAXIMUM_CAPACITY) ? (int) ft : MAXIMUM_CAPACITY);
                if (t > threshold) threshold = tableSizeFor(t);
            } else if (s > threshold) resize();

            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                putVal(hash(key), key, value, ifAbsent, evict);
            }
        }
    }

    @SafeVarargs
    public final void putEntries(boolean evict, Map.Entry<? extends K, ? extends V>... entries) {
        int s = entries.length;
        if (s > 0) {
            if (table == null) { // pre-size
                float ft = ((float) s / loadFactor) + 1.0F;
                int t = ((ft < (float) MAXIMUM_CAPACITY) ? (int) ft : MAXIMUM_CAPACITY);
                if (t > threshold) threshold = tableSizeFor(t);
            } else if (s > threshold) resize();

            for (Map.Entry<? extends K, ? extends V> e : entries) {
                K key = e.getKey();
                V value = e.getValue();
                putVal(hash(key), key, value, false, evict);
            }
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public V get(Object key) {
        Node<K, V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

    @Nullable
    public Node<K, V> getNode(int hash, @NonNull Object key) {
        if (table == null || table.length == 0) return null;

        Node<K, V> first = table[(table.length - 1) & hash];
        if (first == null) return null;

        K k;
        // always check first node
        if (first.hash == hash && ((k = first.key) == key || key.equals(k))) return first;

        Node<K, V> e = first.next;
        if (e == null) return null;
        if (first instanceof TreeNode) return ((TreeNode<K, V>) first).getTreeNode(hash, key);

        do {
            if (e.hash == hash && ((k = e.key) == key || key.equals(k))) return e;
        } while ((e = e.next) != null);
        return null;
    }

    public boolean containsKey(@NonNull Object key) {
        return getNode(hash(key), key) != null;
    }

    @Nullable
    public V put(@NonNull K key, @Nullable V value) {
        return putVal(hash(key), key, value, false, true);
    }

    public V putVal(int hash, @NonNull K key, @Nullable V value, boolean onlyIfAbsent, boolean evict) {
        Node<K, V>[] tab = table;
        int n;
        if (tab == null || (n = tab.length) == 0) n = (tab = resize()).length;

        int i = (n - 1) & hash;
        Node<K, V> p = tab[i];
        if (p == null) tab[i] = newNode(hash, key, value, null);
        else {
            Node<K, V> e;
            K k;
            if (p.hash == hash && ((k = p.key) == key || key.equals(k)))
                e = p;
            else if (p instanceof TreeNode)
                e = ((TreeNode<K, V>) p).putTreeVal(this, tab, hash, key, value);
            else {
                for (int binCount = 0; ; ++binCount) {
                    if ((e = p.next) == null) {
                        p.next = newNode(hash, key, value, null);
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        break;
                    }
                    if (e.hash == hash && ((k = e.key) == key || key.equals(k))) break;
                    p = e;
                }
            }
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null) e.value = value;
                afterNodeAccess(e);
                return oldValue;
            }
        }

        modCount++;
        if (++size > threshold) resize();
        afterNodeInsertion(evict);
        return null;
    }

    public Node<K, V>[] resize() {
        Node<K, V>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        int oldThr = threshold;
        int newCap, newThr = 0;
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            } else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                    oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; // double threshold
        } else if (oldThr > 0) // initial capacity was placed in threshold
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        if (newThr == 0) {
            float ft = (float) newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float) MAXIMUM_CAPACITY ?
                    (int) ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        @SuppressWarnings({"unchecked", "rawtypes"})
        Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
        table = newTab;
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
                Node<K, V> e;
                if ((e = oldTab[j]) != null) {
                    oldTab[j] = null;
                    if (e.next == null)
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof TreeNode)
                        ((TreeNode<K, V>) e).split(this, newTab, j, oldCap);
                    else { // preserve order
                        Node<K, V> loHead = null, loTail = null;
                        Node<K, V> hiHead = null, hiTail = null;
                        Node<K, V> next;
                        do {
                            next = e.next;
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null) loHead = e;
                                else loTail.next = e;
                                loTail = e;
                            } else {
                                if (hiTail == null) hiHead = e;
                                else hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }

    public void treeifyBin(Node<K, V>[] tab, int hash) {
        int n, index;
        Node<K, V> e;
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY) resize();
        else if ((e = tab[index = (n - 1) & hash]) != null) {
            TreeNode<K, V> hd = null, tl = null;
            do {
                TreeNode<K, V> p = replacementTreeNode(e, null);
                if (tl == null)
                    hd = p;
                else {
                    p.prev = tl;
                    tl.next = p;
                }
                tl = p;
            } while ((e = e.next) != null);
            if ((tab[index] = hd) != null) hd.treeify(tab);
        }
    }

    public void putAll(@NonNull Map<? extends K, ? extends V> m) {
        putMapEntries(m, true);
    }

    public void putAllIfAbsent(@NonNull Map<? extends K, ? extends V> m) {
        putMapEntries(m, true, true);
    }

    public V remove(@NonNull Object key) {
        Node<K, V> e = removeNode(hash(key), key, null, false, true);
        return e == null ? null : e.value;
    }

    /**
     * Implements Map.remove and related methods
     *
     * @param hash       hash for key
     * @param key        the key
     * @param value      the value to match if matchValue, else ignored
     * @param matchValue if true only remove if value is equal
     * @param movable    if false do not move other nodes while removing
     * @return the node, or null if none
     */
    @Nullable
    public Node<K, V> removeNode(int hash, @NonNull Object key, @Nullable Object value, boolean matchValue, boolean movable) {
        if (table == null || table.length == 0) return null;

        int index = (table.length - 1) & hash;
        Node<K, V> p = table[index];
        if (p == null) return null;
        Node<K, V>[] tab = table;

        Node<K, V> node = null, e;
        K k;
        V v;
        if (p.hash == hash && ((k = p.key) == key || key.equals(k))) node = p;
        else if ((e = p.next) != null) {
            if (p instanceof TreeNode)
                node = ((TreeNode<K, V>) p).getTreeNode(hash, key);
            else {
                do {
                    if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
                        node = e;
                        break;
                    }
                    p = e;
                } while ((e = e.next) != null);
            }
        }

        if (node != null && (!matchValue || (v = node.value) == value || (value != null && value.equals(v)))) {
            if (node instanceof TreeNode) ((TreeNode<K, V>) node).removeTreeNode(this, tab, movable);
            else if (node == p) tab[index] = node.next;
            else p.next = node.next;

            modCount++;
            size--;
            afterNodeRemoval(node);
            return node;
        }

        return null;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    @SuppressWarnings("ExplicitArrayFilling")
    public void clear() {
        modCount++;
        if (table != null && size > 0) {
            Node<K, V>[] tab = table;
            size = 0;
            for (int i = 0; i < tab.length; ++i) tab[i] = null;
        }
    }

    /**
     * Returns <code>true</code> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested
     * @return <code>true</code> if this map maps one or more keys to the
     * specified value
     */
    public boolean containsValue(@NonNull Object value) {
        if (table != null && size > 0) {
            Node<K, V>[] tab = table;
            for (Node<K, V> e : table) {
                for (; e != null; e = e.next) {
                    V v = e.value;
                    if (v == value || value.equals(v)) return true;
                }
            }
        }
        return false;
    }

    @Override
    public @NonNull Set<K> keySet() {
        return keySet == null ? keySet = new KeySet() : keySet;
    }

    @Override
    public @NonNull Collection<V> values() {
        return values == null ? values = new Values() : values;
    }

    public void initValues() {
        values = new Values();
    }

    @Override
    public @NonNull Set<Map.Entry<K, V>> entrySet() {
        return entrySet == null ? entrySet = new EntrySet() : entrySet;
    }

    @Override
    @Nullable
    public V getOrDefault(@NonNull Object key, @Nullable V defaultValue) {
        Node<K, V> node = getNode(hash(key), key);
        return node == null ? defaultValue : node.value;
    }

    @Override
    @Nullable
    public V putIfAbsent(@NonNull K key, @Nullable V value) {
        return putVal(hash(key), key, value, true, true);
    }

    @Override
    public boolean remove(@NonNull Object key, @Nullable Object value) {
        return removeNode(hash(key), key, value, true, true) != null;
    }

    @Override
    public boolean replace(@NonNull K key, V oldValue, V newValue) {
        Node<K, V> node = getNode(hash(key), key);
        if (node == null) return false;

        if (Objects.equals(node.value, oldValue)) {
            node.value = newValue;
            afterNodeAccess(node);
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public V replace(@NonNull K key, @Nullable V value) {
        Node<K, V> node = getNode(hash(key), key);
        if (node == null) return null;

        V oldValue = node.value;
        node.value = value;
        afterNodeAccess(node);
        return oldValue;
    }

    @Override
    @Nullable
    public V computeIfAbsent(@NonNull K key, @NonNull Function<? super K, ? extends V> mappingFunction) {
        if (mappingFunction == null) throw new NullPointerException();
        int hash = hash(key);
        Node<K, V>[] tab;
        Node<K, V> first;
        int n, i;
        int binCount = 0;
        TreeNode<K, V> t = null;
        Node<K, V> old = null;
        if (size > threshold || (tab = table) == null || (n = tab.length) == 0) n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K, V>) first).getTreeNode(hash, key);
            else {
                Node<K, V> e = first;
                K k;
                do {
                    if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
                        old = e;
                        break;
                    }
                    binCount++;
                } while ((e = e.next) != null);
            }
            V oldValue;
            if (old != null && (oldValue = old.value) != null) {
                afterNodeAccess(old);
                return oldValue;
            }
        }
        V v = mappingFunction.apply(key);
        if (v == null) return null;
        else if (old != null) {
            old.value = v;
            afterNodeAccess(old);
            return v;
        } else if (t != null) t.putTreeVal(this, tab, hash, key, v);
        else {
            tab[i] = newNode(hash, key, v, first);
            if (binCount >= TREEIFY_THRESHOLD - 1)
                treeifyBin(tab, hash);
        }
        modCount++;
        size++;
        afterNodeInsertion(true);
        return v;
    }

    @Override
    public V computeIfPresent(@NonNull K key, @NonNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null) throw new NullPointerException();

        int hash = hash(key);
        Node<K, V> e = getNode(hash, key);
        if (e == null) return null;

        V v = e.value; // Old Value
        if (v == null) return null;

        v = remappingFunction.apply(key, v); // New Value
        if (v != null) {
            e.value = v;
            afterNodeAccess(e);
            return v;
        } else removeNode(hash, key, null, false, true);
        return null;
    }

    @Override
    public V compute(@NonNull K key, @NonNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null) throw new NullPointerException();

        int hash = hash(key);
        Node<K, V>[] tab;
        Node<K, V> first;
        int n, i;
        if (size > threshold || table == null || (n = table.length) == 0) n = (tab = resize()).length;
        else tab = table;

        TreeNode<K, V> t = null;
        Node<K, V> old = null;
        int binCount = 0;
        if ((first = table[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K, V>) first).getTreeNode(hash, key);
            else {
                Node<K, V> e = first;
                K k;
                do {
                    if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }

        V oldValue = old == null ? null : old.value;
        V v = remappingFunction.apply(key, oldValue);
        if (old != null) {
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            } else removeNode(hash, key, null, false, true);
        } else if (v != null) {
            if (t != null) t.putTreeVal(this, tab, hash, key, v);
            else {
                tab[i] = newNode(hash, key, v, first);
                if (binCount >= TREEIFY_THRESHOLD - 1) treeifyBin(tab, hash);
            }

            modCount++;
            size++;
            afterNodeInsertion(true);
        }
        return v;
    }

    @Override
    public V merge(@NonNull K key, @Nullable V value, @NonNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (value == null) throw new NullPointerException();
        if (remappingFunction == null) throw new NullPointerException();

        int hash = hash(key);
        Node<K, V>[] tab;
        Node<K, V> first;
        int n, i;
        int binCount = 0;
        TreeNode<K, V> t = null;
        Node<K, V> old = null;
        if (size > threshold || (tab = table) == null || (n = tab.length) == 0) n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode) old = (t = (TreeNode<K, V>) first).getTreeNode(hash, key);
            else {
                Node<K, V> e = first;
                K k;
                do {
                    if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
                        old = e;
                        break;
                    }
                    binCount++;
                } while ((e = e.next) != null);
            }
        }

        if (old != null) {
            V v;
            if (old.value != null) v = remappingFunction.apply(old.value, value);
            else v = value;
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            } else removeNode(hash, key, null, false, true);
            return v;
        }

        if (t != null) t.putTreeVal(this, tab, hash, key, value);
        else {
            tab[i] = newNode(hash, key, value, first);
            if (binCount >= TREEIFY_THRESHOLD - 1) treeifyBin(tab, hash);
        }
        modCount++;
        size++;
        afterNodeInsertion(true);
        return value;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) throw new NullPointerException();
        if (size > 0 && table != null) {
            Node<K, V>[] tab = table;
            int mc = modCount;
            for (Node<K, V> kvNode : tab) {
                for (Node<K, V> e = kvNode; e != null; e = e.next)
                    action.accept(e.key, e.value);
            }
            if (modCount != mc) throw new ConcurrentModificationException();
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) throw new NullPointerException();
        if (size > 0 && table != null) {
            Node<K, V>[] tab = table;
            int mc = modCount;
            for (Node<K, V> kvNode : tab) {
                for (Node<K, V> e = kvNode; e != null; e = e.next) {
                    e.value = function.apply(e.key, e.value);
                }
            }
            if (modCount != mc) throw new ConcurrentModificationException();
        }
    }

    /**
     * Returns a shallow copy of this <code>HashMap</code> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        UnsafeHashMap<K, V> result;
        try {
            result = (UnsafeHashMap<K, V>) super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }

        result.reinitialize();
        result.putMapEntries(this, false);
        return result;
    }

    public int capacity() {
        return table != null
                ? table.length : threshold > 0
                ? threshold : DEFAULT_INITIAL_CAPACITY;
    }

    // Create a regular (non-tree) node
    public Node<K, V> newNode(int hash, K key, V value, Node<K, V> next) {
        return new Node<>(hash, key, value, next);
    }

    // For conversion from TreeNodes to plain nodes
    public Node<K, V> replacementNode(Node<K, V> p, Node<K, V> next) {
        return new Node<>(p.hash, p.key, p.value, next);
    }

    /* ------------------------------------------------------------ */
    // Cloning and serialization

    // Create a tree bin node
    public TreeNode<K, V> newTreeNode(int hash, K key, V value, Node<K, V> next) {
        return new TreeNode<>(hash, key, value, next);
    }

    // For treeifyBin
    public TreeNode<K, V> replacementTreeNode(Node<K, V> p, Node<K, V> next) {
        return new TreeNode<>(p.hash, p.key, p.value, next);
    }

    /**
     * Reset to initial default state.  Called by clone and readObject.
     */
    public void reinitialize() {
        table = null;
        entrySet = null;
        keySet = null;
        values = null;
        modCount = 0;
        threshold = 0;
        size = 0;
    }

    // Callbacks to allow LinkedHashMap post-actions
    void afterNodeAccess(Node<K, V> p) {
    }

    void afterNodeInsertion(boolean evict) {
    }

    void afterNodeRemoval(Node<K, V> p) {
    }

    public static class Node<K, V> implements Map.Entry<K, V> {
        public final int hash;
        public final K key;
        public V value;
        public Node<K, V> next;

        public Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public String toString() {
            return key + "=" + value;
        }

        public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public boolean equals(Object o) {
            if (o == this) return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                return Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue());
            }
            return false;
        }
    }

    public static class HashMapSpliterator<K, V> {
        public final UnsafeHashMap<K, V> map;
        public Node<K, V> current;         // current node
        public int index;                  // current index, modified on advance/split
        public int fence;                  // one past last index
        public int est;                    // size estimate
        public int expectedModCount;       // for comodification checks

        public HashMapSpliterator(UnsafeHashMap<K, V> m, int origin,
                                  int fence, int est, int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        public final int getFence() { // initialize fence and size on first use
            if (fence >= 0) return fence;

            est = map.size;
            expectedModCount = map.modCount;
            Node<K, V>[] tab = map.table;
            return fence = tab == null ? 0 : tab.length;
        }

        public final long estimateSize() {
            getFence(); // force init
            return est;
        }
    }

    public static final class KeySpliterator<K, V> extends HashMapSpliterator<K, V> implements Spliterator<K> {
        KeySpliterator(UnsafeHashMap<K, V> m, int origin, int fence, int est,
                       int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public KeySpliterator<K, V> trySplit() {
            int hi = getFence(), mid = (index + hi) >>> 1;
            return (index >= mid || current != null) ? null :
                    new KeySpliterator<>(map, index, index = mid, est >>>= 1, expectedModCount);
        }

        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();

            int i, hi, mc;
            UnsafeHashMap<K, V> m = map;
            Node<K, V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            } else mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K, V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.key);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc) throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();

            int hi;
            Node<K, V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        K k = current.key;
                        current = current.next;
                        action.accept(k);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                    Spliterator.DISTINCT;
        }
    }

    public static final class ValueSpliterator<K, V> extends HashMapSpliterator<K, V> implements Spliterator<V> {
        ValueSpliterator(UnsafeHashMap<K, V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public ValueSpliterator<K, V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                    new ValueSpliterator<>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        public void forEachRemaining(Consumer<? super V> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            UnsafeHashMap<K, V> m = map;
            Node<K, V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            } else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K, V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.value);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K, V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        V v = current.value;
                        current = current.next;
                        action.accept(v);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0);
        }
    }

    /* ------------------------------------------------------------ */
    // LinkedHashMap support


    /*
     * The following package-protected methods are designed to be
     * overridden by LinkedHashMap, but not by any other subclass.
     * Nearly all other internal methods are also package-protected
     * but are declared final, so can be used by LinkedHashMap, view
     * classes, and HashSet.
     */

    public static final class EntrySpliterator<K, V>
            extends HashMapSpliterator<K, V>
            implements Spliterator<Map.Entry<K, V>> {
        EntrySpliterator(UnsafeHashMap<K, V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public EntrySpliterator<K, V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                    new EntrySpliterator<>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            UnsafeHashMap<K, V> m = map;
            Node<K, V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            } else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K, V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K, V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        Node<K, V> e = current;
                        current = current.next;
                        action.accept(e);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                    Spliterator.DISTINCT;
        }
    }

    public static class Entry<K, V> extends UnsafeHashMap.Node<K, V> {
        Entry(int hash, K key, V value, UnsafeHashMap.Node<K, V> next) {
            super(hash, key, value, next);
        }
    }

    /**
     * Entry for Tree bins. Extends LinkedHashMap.Entry (which in turn
     * extends Node) so can be used as extension of either regular or
     * linked node.
     */
    public static final class TreeNode<K, V> extends UnsafeHashMap.Entry<K, V> {
        TreeNode<K, V> parent;  // red-black tree links
        TreeNode<K, V> left;
        TreeNode<K, V> right;
        TreeNode<K, V> prev;    // needed to unlink next upon deletion
        boolean red;

        TreeNode(int hash, K key, V val, Node<K, V> next) {
            super(hash, key, val, next);
        }

        /**
         * Ensures that the given root is the first node of its bin.
         */
        public static <K, V> void moveRootToFront(Node<K, V>[] tab, TreeNode<K, V> root) {
            int n;
            if (root != null && tab != null && (n = tab.length) > 0) {
                int index = (n - 1) & root.hash;
                TreeNode<K, V> first = (TreeNode<K, V>) tab[index];
                if (root != first) {
                    Node<K, V> rn;
                    tab[index] = root;
                    TreeNode<K, V> rp = root.prev;

                    if ((rn = root.next) != null) ((TreeNode<K, V>) rn).prev = rp;
                    if (rp != null) rp.next = rn;
                    if (first != null) first.prev = root;

                    root.next = first;
                    root.prev = null;
                }
                assert checkInvariants(root);
            }
        }

        /**
         * Tie-breaking utility for ordering insertions when equal
         * hashCodes and non-comparable. We don't require a total
         * order, just a consistent insertion rule to maintain
         * equivalence across rebalancings. Tie-breaking further than
         * necessary simplifies testing a bit.
         */
        public static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null || (d = a.getClass().getName().compareTo(b.getClass().getName())) == 0)
                d = System.identityHashCode(a) <= System.identityHashCode(b) ? -1 : 1;
            return d;
        }

        public static <K, V> TreeNode<K, V> rotateLeft(TreeNode<K, V> root, TreeNode<K, V> p) {
            TreeNode<K, V> r, pp, rl;
            if (p != null && (r = p.right) != null) {
                if ((rl = p.right = r.left) != null)
                    rl.parent = p;
                if ((pp = r.parent = p.parent) == null)
                    (root = r).red = false;
                else if (pp.left == p)
                    pp.left = r;
                else
                    pp.right = r;
                r.left = p;
                p.parent = r;
            }
            return root;
        }

        public static <K, V> TreeNode<K, V> rotateRight(TreeNode<K, V> root, TreeNode<K, V> p) {
            TreeNode<K, V> l, pp, lr;
            if (p != null && (l = p.left) != null) {
                if ((lr = p.left = l.right) != null)
                    lr.parent = p;
                if ((pp = l.parent = p.parent) == null)
                    (root = l).red = false;
                else if (pp.right == p)
                    pp.right = l;
                else
                    pp.left = l;
                l.right = p;
                p.parent = l;
            }
            return root;
        }

        public static <K, V> TreeNode<K, V> balanceInsertion(TreeNode<K, V> root, TreeNode<K, V> x) {
            x.red = true;
            for (TreeNode<K, V> xp, xpp, xppl, xppr; ; ) {
                if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (!xp.red || (xpp = xp.parent) == null)
                    return root;
                if (xp == (xppl = xpp.left)) {
                    if ((xppr = xpp.right) != null && xppr.red) {
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.right) {
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                } else {
                    if (xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }

        @SuppressWarnings("ConstantValue")
        public static <K, V> TreeNode<K, V> balanceDeletion(TreeNode<K, V> root, TreeNode<K, V> x) {
            for (TreeNode<K, V> xp, xpl, xpr; ; ) {
                if (x == null || x == root)
                    return root;
                else if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (x.red) {
                    x.red = false;
                    return root;
                } else if ((xpl = xp.left) == x) {
                    if ((xpr = xp.right) != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    if (xpr == null)
                        x = xp;
                    else {
                        TreeNode<K, V> sl = xpr.left, sr = xpr.right;
                        if ((sr == null || !sr.red) &&
                                (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        } else {
                            if (sr == null || !sr.red) {
                                if (sl != null) sl.red = false;
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ?
                                        null : xp.right;
                            }
                            if (xpr != null) {
                                xpr.red = xp != null && xp.red;
                                if ((sr = xpr.right) != null)
                                    sr.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                } else { // symmetric
                    if (xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (xpl == null)
                        x = xp;
                    else {
                        TreeNode<K, V> sl = xpl.left, sr = xpl.right;
                        if ((sl == null || !sl.red) &&
                                (sr == null || !sr.red)) {
                            xpl.red = true;
                            x = xp;
                        } else {
                            if (sl == null || !sl.red) {
                                if (sr != null)
                                    sr.red = false;
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ?
                                        null : xp.left;
                            }
                            if (xpl != null) {
                                xpl.red = xp != null && xp.red;
                                if ((sl = xpl.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }

        /**
         * Recursive invariant check
         */
        public static <K, V> boolean checkInvariants(TreeNode<K, V> t) {
            TreeNode<K, V> tb = t.prev;
            if (tb != null && tb.next != t) return false;

            TreeNode<K, V> tn = (TreeNode<K, V>) t.next;
            if (tn != null && tn.prev != t) return false;

            TreeNode<K, V> tp = t.parent;
            if (tp != null && t != tp.left && t != tp.right) return false;

            TreeNode<K, V> tl = t.left;
            if (tl != null && (tl.parent != t || tl.hash > t.hash)) return false;

            TreeNode<K, V> tr = t.right;
            if (tr != null && (tr.parent != t || tr.hash < t.hash)) return false;
            if (t.red && tl != null && tl.red && tr != null && tr.red) return false;
            if (tl != null && !checkInvariants(tl)) return false;
            return tr == null || checkInvariants(tr);
        }

        /**
         * Returns root of tree containing this node.
         */
        public TreeNode<K, V> root() {
            for (TreeNode<K, V> r = this, p; ; ) {
                if ((p = r.parent) == null) return r;
                r = p;
            }
        }

        /**
         * Finds the node starting at root p with the given hash and key.
         * The kc argument caches comparableClassFor(key) upon first use
         * comparing keys.
         */
        public TreeNode<K, V> find(int h, Object k, Class<?> kc) {
            TreeNode<K, V> p = this;
            do {
                int ph, dir;
                K pk;
                TreeNode<K, V> pl = p.left, pr = p.right, q;
                if ((ph = p.hash) > h)
                    p = pl;
                else if (ph < h)
                    p = pr;
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                else if (pl == null)
                    p = pr;
                else if (pr == null)
                    p = pl;
                else if ((kc != null ||
                        (kc = comparableClassFor(k)) != null) &&
                        (dir = compareComparables(kc, k, pk)) != 0)
                    p = (dir < 0) ? pl : pr;
                else if ((q = pr.find(h, k, kc)) != null)
                    return q;
                else
                    p = pl;
            } while (p != null);
            return null;
        }

        /**
         * Calls find for root node.
         */
        public TreeNode<K, V> getTreeNode(int h, Object k) {
            return ((parent != null) ? root() : this).find(h, k, null);
        }

        /* ------------------------------------------------------------ */
        // Red-black tree methods, all adapted from CLR

        /**
         * Forms tree of the nodes linked from this node.
         * return root of tree
         */
        public void treeify(Node<K, V>[] tab) {
            TreeNode<K, V> root = null;
            for (TreeNode<K, V> x = this, next; x != null; x = next) {
                next = (TreeNode<K, V>) x.next;
                x.left = x.right = null;
                if (root == null) {
                    x.parent = null;
                    x.red = false;
                    root = x;
                } else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K, V> p = root; ; ) {
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h)
                            dir = -1;
                        else if (ph < h)
                            dir = 1;
                        else if ((kc == null &&
                                (kc = comparableClassFor(k)) == null) ||
                                (dir = compareComparables(kc, k, pk)) == 0)
                            dir = tieBreakOrder(k, pk);

                        TreeNode<K, V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0)
                                xp.left = x;
                            else
                                xp.right = x;
                            root = balanceInsertion(root, x);
                            break;
                        }
                    }
                }
            }
            moveRootToFront(tab, root);
        }

        /**
         * Returns a list of non-TreeNodes replacing those linked from
         * this node.
         */
        public Node<K, V> untreeify(UnsafeHashMap<K, V> map) {
            Node<K, V> hd = null, tl = null;
            for (Node<K, V> q = this; q != null; q = q.next) {
                Node<K, V> p = map.replacementNode(q, null);
                if (tl == null) hd = p;
                else tl.next = p;
                tl = p;
            }
            return hd;
        }

        /**
         * Tree version of putVal.
         */
        @Nullable
        public TreeNode<K, V> putTreeVal(@NonNull UnsafeHashMap<K, V> map, @NonNull Node<K, V>[] tab, int h, @NonNull K k, @Nullable V v) {
            Class<?> kc = null;
            boolean searched = false;
            TreeNode<K, V> root = (parent != null) ? root() : this;
            for (TreeNode<K, V> p = root; ; ) {
                int ph = p.hash;
                int dir;
                K pk;

                if (ph > h) dir = -1;
                else if (ph < h) dir = 1;
                else if ((pk = p.key) == k || k.equals(pk)) return p;
                else if ((kc == null &&
                        (kc = comparableClassFor(k)) == null) ||
                        (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        TreeNode<K, V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&
                                (q = ch.find(h, k, kc)) != null) ||
                                ((ch = p.right) != null && (q = ch.find(h, k, kc)) != null))
                            return q;
                    }
                    dir = tieBreakOrder(k, pk);
                }

                TreeNode<K, V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    Node<K, V> xpn = xp.next;
                    TreeNode<K, V> x = map.newTreeNode(h, k, v, xpn);
                    if (dir <= 0) xp.left = x;
                    else xp.right = x;

                    xp.next = x;
                    x.parent = x.prev = xp;
                    if (xpn != null) ((TreeNode<K, V>) xpn).prev = x;
                    moveRootToFront(tab, balanceInsertion(root, x));
                    return null;
                }
            }
        }

        /**
         * Removes the given node, that must be present before this call.
         * This is messier than typical red-black deletion code because we
         * cannot swap the contents of an interior node with a leaf
         * successor that is pinned by "next" pointers that are accessible
         * independently during traversal. So instead we swap the tree
         * linkages. If the current tree appears to have too few nodes,
         * the bin is converted back to a plain bin. (The test triggers
         * somewhere between 2 and 6 nodes, depending on tree structure).
         */
        @SuppressWarnings("ConstantValue")
        public void removeTreeNode(UnsafeHashMap<K, V> map, Node<K, V>[] tab, boolean movable) {
            if (tab == null || tab.length == 0) return;

            int index = (tab.length - 1) & hash;
            TreeNode<K, V> first = (TreeNode<K, V>) tab[index], root = first, rl;
            TreeNode<K, V> succ = (TreeNode<K, V>) next, pred = prev;
            if (pred == null)
                tab[index] = first = succ;
            else
                pred.next = succ;
            if (succ != null)
                succ.prev = pred;
            if (first == null)
                return;
            if (root.parent != null)
                root = root.root();
            if (root == null || root.right == null ||
                    (rl = root.left) == null || rl.left == null) {
                tab[index] = first.untreeify(map);  // too small
                return;
            }
            TreeNode<K, V> p = this, pl = left, pr = right, replacement;
            if (pl != null && pr != null) {
                TreeNode<K, V> s = pr, sl;
                while ((sl = s.left) != null) // find successor
                    s = sl;
                boolean c = s.red;
                s.red = p.red;
                p.red = c; // swap colors
                TreeNode<K, V> sr = s.right;
                TreeNode<K, V> pp = p.parent;
                if (s == pr) { // p was s's direct parent
                    p.parent = s;
                    s.right = p;
                } else {
                    TreeNode<K, V> sp = s.parent;
                    if ((p.parent = sp) != null) {
                        if (s == sp.left)
                            sp.left = p;
                        else
                            sp.right = p;
                    }
                    if ((s.right = pr) != null)
                        pr.parent = s;
                }
                p.left = null;
                if ((p.right = sr) != null)
                    sr.parent = p;
                if ((s.left = pl) != null)
                    pl.parent = s;
                if ((s.parent = pp) == null)
                    root = s;
                else if (p == pp.left)
                    pp.left = s;
                else
                    pp.right = s;
                if (sr != null)
                    replacement = sr;
                else
                    replacement = p;
            } else if (pl != null) replacement = pl;
            else if (pr != null) replacement = pr;
            else replacement = p;

            if (replacement != p) {
                TreeNode<K, V> pp = replacement.parent = p.parent;
                if (pp == null)
                    root = replacement;
                else if (p == pp.left)
                    pp.left = replacement;
                else
                    pp.right = replacement;
                p.left = p.right = p.parent = null;
            }

            TreeNode<K, V> r = p.red ? root : balanceDeletion(root, replacement);

            if (replacement == p) {  // detach
                TreeNode<K, V> pp = p.parent;
                p.parent = null;
                if (pp != null) {
                    if (p == pp.left)
                        pp.left = null;
                    else if (p == pp.right)
                        pp.right = null;
                }
            }

            if (movable) moveRootToFront(tab, r);
        }

        /**
         * Splits nodes in a tree bin into lower and upper tree bins,
         * or untreeifies if now too small. Called only from resize;
         * see above discussion about split bits and indices.
         *
         * @param map   the map
         * @param tab   the table for recording bin heads
         * @param index the index of the table being split
         * @param bit   the bit of hash to split on
         */
        public void split(UnsafeHashMap<K, V> map, Node<K, V>[] tab, int index, int bit) {
            TreeNode<K, V> b = this;
            // Relink into lo and hi lists, preserving order
            TreeNode<K, V> loHead = null, loTail = null;
            TreeNode<K, V> hiHead = null, hiTail = null;
            int lc = 0, hc = 0;
            for (TreeNode<K, V> e = b, next; e != null; e = next) {
                next = (TreeNode<K, V>) e.next;
                e.next = null;
                if ((e.hash & bit) == 0) {
                    if ((e.prev = loTail) == null) loHead = e;
                    else loTail.next = e;
                    loTail = e;
                    ++lc;
                } else {
                    if ((e.prev = hiTail) == null)
                        hiHead = e;
                    else
                        hiTail.next = e;
                    hiTail = e;
                    ++hc;
                }
            }

            if (loHead != null) {
                if (lc <= UNTREEIFY_THRESHOLD)
                    tab[index] = loHead.untreeify(map);
                else {
                    tab[index] = loHead;
                    if (hiHead != null) // (else is already treeified)
                        loHead.treeify(tab);
                }
            }
            if (hiHead != null) {
                if (hc <= UNTREEIFY_THRESHOLD)
                    tab[index + bit] = hiHead.untreeify(map);
                else {
                    tab[index + bit] = hiHead;
                    if (loHead != null)
                        hiHead.treeify(tab);
                }
            }
        }
    }

    public final class KeySet extends AbstractSet<K> {
        public int size() {
            return size;
        }

        public void clear() {
            UnsafeHashMap.this.clear();
        }

        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        public boolean contains(Object o) {
            return containsKey(o);
        }

        public boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }

        public Spliterator<K> spliterator() {
            return new KeySpliterator<>(UnsafeHashMap.this, 0, -1, 0, 0);
        }

        public void forEach(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            if (size > 0 && table != null) {
                Node<K, V>[] tab = table;
                int mc = modCount;
                for (Node<K, V> e : tab) {
                    for (; e != null; e = e.next) action.accept(e.key);
                }
                if (modCount != mc) throw new ConcurrentModificationException();
            }
        }
    }

    private final class Values extends AbstractCollection<V> {
        public int size() {
            return size;
        }

        public void clear() {
            UnsafeHashMap.this.clear();
        }

        @SuppressWarnings("ReturnOfInnerClass")
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        public boolean contains(Object o) {
            return containsValue(o);
        }

        public Spliterator<V> spliterator() {
            return new ValueSpliterator<>(UnsafeHashMap.this, 0, -1, 0, 0);
        }

        public void forEach(Consumer<? super V> action) {
            if (action == null) throw new NullPointerException();
            if (size > 0 && table != null) {
                Node<K, V>[] tab = table;
                int mc = modCount;
                for (Node<K, V> e : tab) {
                    for (; e != null; e = e.next) action.accept(e.value);
                }
                if (modCount != mc) throw new ConcurrentModificationException();
            }
        }
    }

    private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        public int size() {
            return size;
        }

        public void clear() {
            UnsafeHashMap.this.clear();
        }

        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            Object key = e.getKey();
            Node<K, V> candidate = getNode(hash(key), key);
            return e.equals(candidate);
        }

        public boolean remove(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }

        public Spliterator<Map.Entry<K, V>> spliterator() {
            return new EntrySpliterator<>(UnsafeHashMap.this, 0, -1, 0, 0);
        }

        public void forEach(Consumer<? super Map.Entry<K, V>> action) {
            if (action == null) throw new NullPointerException();
            if (size > 0 && table != null) {
                Node<K, V>[] tab = table;
                int mc = modCount;
                for (Node<K, V> e : tab) {
                    for (; e != null; e = e.next) action.accept(e);
                }
                if (modCount != mc) throw new ConcurrentModificationException();
            }
        }
    }

    public abstract class HashIterator {
        public Node<K, V> next;        // next entry to return
        public Node<K, V> current;     // current entry
        public int expectedModCount;  // for fast-fail
        public int index;             // current slot

        @SuppressWarnings("StatementWithEmptyBody")
        public HashIterator() {
            expectedModCount = modCount;
            current = next = null;
            index = 0;
            if (table != null && size > 0) { // advance to first entry
                Node<K, V>[] tab = table;
                do {
                } while (index < tab.length && (next = tab[index++]) == null);
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        @SuppressWarnings("StatementWithEmptyBody")
        public final Node<K, V> nextNode() {
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
            if (next == null) throw new NoSuchElementException();
            if (table == null) return next;

            Node<K, V> e = next;
            if ((next = (current = e).next) == null) {
                Node<K, V>[] tab = table;
                do {
                } while (index < tab.length && (next = tab[index++]) == null);
            }
            return e;
        }

        public final void remove() {
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
            if (current == null) throw new IllegalStateException();

            removeNode(current.hash, current.key, null, false, false);
            expectedModCount = modCount;
            current = null;
        }
    }

    public final class KeyIterator extends HashIterator implements Iterator<K> {
        public K next() {
            return nextNode().key;
        }
    }

    public final class ValueIterator extends HashIterator implements Iterator<V> {
        public V next() {
            return nextNode().value;
        }
    }

    public final class EntryIterator extends HashIterator implements Iterator<Map.Entry<K, V>> {
        public Map.Entry<K, V> next() {
            return nextNode();
        }
    }
}
