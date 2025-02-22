package org.kingdoms.utils.internal.identity;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.StringVal;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;

/**
 * Used because {@link QuantumIdentityHashMap} allocates more memory for the value.
 */
public class QuantumIdentityHashSet<K> extends AbstractSet<K> implements java.io.Serializable, Cloneable {
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
     * Constructs a new, empty identity hash map with a default expected
     * maximum size (21).
     */
    public QuantumIdentityHashSet() {
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
    public QuantumIdentityHashSet(int expectedMaxSize) {
        if (expectedMaxSize < 0) throw new IllegalArgumentException("expectedMaxSize is negative: " + expectedMaxSize);
        init(capacity(expectedMaxSize));
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
                (expectedMaxSize <= MINIMUM_CAPACITY / 3) ? MINIMUM_CAPACITY :
                        Integer.highestOneBit(expectedMaxSize + (expectedMaxSize << 1));
    }

    /**
     * Returns index for Object x.
     * https://stackoverflow.com/questions/20953390/what-is-the-fastest-hash-function-for-pointers
     * <p>
     * int hash = System.identityHashCode(x);
     * return (length - 1) & (hash ^ (hash >>> 16));
     */
    private static int hash(Object x, int length) {
        return System.identityHashCode(x) % length; // (hash & 0x7FFFFFFF) % tab.length
    }

    /**
     * Circularly traverses table of size len.
     */
    private static int nextKeyIndex(int i, int len) {
        return ++i < len ? i : 0;
    }

    /**
     * Initializes object to be an empty map with the specified initial
     * capacity.
     */
    private void init(int initCapacity) {
        table = new Object[initCapacity];
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
     * The {@link #contains(Object)}} operation may be used to
     * distinguish these two cases.
     *
     * @see #add(Object)
     */
    @Override
    public boolean contains(@NonNull Object key) {
        Objects.requireNonNull(key, "This set cannot contain nulls");
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
    @SuppressWarnings("ReturnOfInnerClass")
    @NonNull
    @Override
    public Iterator<K> iterator() {
        return new IdentityHashSetIterator<>();
    }

    /**
     * Associates the specified value with the specified key in this identity
     * hash map.  If the map previously contained a mapping for the key, the
     * old value is replaced.
     *
     * @param key the key with which the specified value is to be associated
     * @return the previous value associated with {@code key}, or
     * {@code null} if there was no mapping for {@code key}.
     * (A {@code null} return can also indicate that the map
     * previously associated {@code null} with {@code key}.)
     * @see Object#equals(Object)
     * @see #contains(Object)
     */
    @Override
    public boolean add(@NonNull K key) {
        while (true) {
            Object[] tab = table;
            int len = tab.length;
            int i = hash(key, len);

            for (Object item = tab[i]; item != null; i = nextKeyIndex(i, len)) item = tab[i];

            int s = size + 1;
            // Use optimized form of 3 * s.
            // Next capacity is len, 2 * current capacity.
            if (s + (s << 1) > len && resize(len)) continue;

            modCount++;
            tab[i] = key;
            size = s;
            return false;
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
        // or (newCapacity & (newCapacity - 1)) == 0;
        newCapacity *= 2;

        int oldLength = table.length;
        if (oldLength == MAXIMUM_CAPACITY) { // can't expand any further
            if (size == MAXIMUM_CAPACITY - 1) throw new IllegalStateException("Capacity exhausted.");
            return false;
        }
        if (oldLength >= newCapacity) return false;

        Object[] tab = table;
        Object[] newTable = new Object[newCapacity];

        // Rehash
        for (int j = 0; j < oldLength; j++) {
            Object key = tab[j];
            if (key != null) {
                int i = hash(key, newCapacity);
                while (newTable[i] != null) i = nextKeyIndex(i, newCapacity);
                newTable[i] = key;
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
    public void putAll(Set<? extends K> m) {
        int n = m.size();
        if (n == 0) return;
        if (n > size) resize(capacity(n)); // conservatively pre-expand
        this.addAll(m);
    }

    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map
     * @return true, otherwise false if there was no mapping for {@code key}.
     */
    @Override
    public boolean remove(@Nullable Object key) {
        if (key == null) return false;
        Object[] tab = table;
        int len = tab.length;
        int i = hash(key, len);

        while (true) {
            Object item = tab[i];
            if (item == key) {
                modCount++;
                size--;
                tab[i] = null;
                closeDeletion(i);
                return true;
            }
            if (item == null) return false;
            i = nextKeyIndex(i, len);
        }
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        for (Object obj : c) {
            if (!contains(obj)) return false;
        }
        return true;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends K> c) {
        for (K obj : c) add(obj);
        return true;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        Iterator<K> it = iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Returns a string representation of this collection.  The string
     * representation consists of a list of the collection's elements in the
     * order they are returned by its iterator, enclosed in square brackets
     * ({@code "QuantumIdentityHashSet[]"}).  Adjacent elements are separated by the characters
     * {@code ", "} (comma and space).  Elements are converted to strings as
     * by {@link String#valueOf(Object)}.
     *
     * @return a string representation of this collection
     */
    @Override
    public String toString() {
        if (isEmpty()) return "QuantumIdentityHashSet[]";

        Iterator<K> it = iterator();
        StringBuilder sb = new StringBuilder(22 + (size * 3));
        sb.append("QuantumIdentityHashSet[");

        while (true) {
            K next = it.next();
            sb.append(next);
            if (!it.hasNext()) return sb.append(']').toString();
            sb.append(',').append(' ');
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
                tab[i] = null;
                d = i;
            }
        }
    }

    /**
     * Removes all of the mappings from this map.
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
            QuantumIdentityHashSet<?> m = (QuantumIdentityHashSet<?>) super.clone();
            m.table = table.clone();
            return m;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /*
     * Must revert from AbstractSet's impl to AbstractCollection's, as
     * the former contains an optimization that results in incorrect
     * behavior when c is a smaller "normal" (non-identity-based) Set.
     */
    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;

        if (size > c.size()) {
            for (Object e : c) modified |= remove(e);
        } else {
            for (Iterator<?> i = iterator(); i.hasNext(); ) {
                if (c.contains(i.next())) {
                    i.remove();
                    modified = true;
                }
            }
        }

        return modified;
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[0]);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        Object[] tab = table;
        int expectedModCount = modCount;
        if (a.length < size) a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
        int ti = 0;

        for (Object key : tab) {
            if (key != null) { // key present ?
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

    @StringVal({"all", "%%__RESOURCE__%%", "%%__NONCE__%%", "%%__USER__%%", "%%__SIGNATURE__%%",
            "%%__VERIFY_TOKEN__%%", "%%__USERNAME__%%", "%%__RESOURCE_VERSION__%%", "%%__UPLOAD__%%",
            "%%__LICENSE__%%", "%%__LICENSE_5__%%", "%%__PHRASE__%%", "%%__POLYMART__%%", "%%__TIMESTAMP__%%",
            "%%__RSA_PUBLIC_KEY__%%", "%%__BUILTBYBIT__%%", "%%__VERSION__%%", "%%__FILEHASH__%%", "%%__NULLED__%%",})
    public static final class ____________ {
        public static final int a = 0;
    }

    public Spliterator<K> spliterator() {
        return new KeySpliterator<>(QuantumIdentityHashSet.this, 0, -1, 0, 0);
    }

    /**
     * Saves the state of the {@code QuantumIdentityHashSet} instance to a stream
     * (i.e., serializes it).
     *
     * @serialData The <i>size</i> of the HashMap (the number of key-value
     * mappings) ({@code int}), followed by the key (Object) and
     * value (Object) for each key-value mapping represented by the
     * QuantumIdentityHashSet.  The key-value mappings are emitted in no
     * particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
        // Write out and any hidden stuff
        s.defaultWriteObject();

        // Write out size (number of Mappings)
        s.writeInt(size);

        Object[] tab = table;

        // Write out keys and values (alternating)
        for (Object key : tab) {
            if (key != null) s.writeObject(key);
        }
    }

    /**
     * Reconstitutes the {@code QuantumIdentityHashSet} instance from a stream (i.e.,
     * deserializes it).
     */
    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void forEach(Consumer<? super K> action) {
        Objects.requireNonNull(action);
        Object[] tab = table;
        int expectedModCount = modCount;

        for (Object key : tab) {
            if (key != null) action.accept((K) key);
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
        }
    }

    /**
     * Similar form as array-based Spliterators, but skips blank elements,
     * and guestimates size as decreasing by half per split.
     */
    private static class IdentityHashSetSpliterator<K> {
        final QuantumIdentityHashSet<K> map;
        int index;             // current index, modified on advance/split
        int fence;             // -1 until first use; then one past last index
        int est;               // size estimate
        int expectedModCount;  // initialized when fence set

        IdentityHashSetSpliterator(QuantumIdentityHashSet<K> map, int origin, int fence, int est, int expectedModCount) {
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

    private static final class KeySpliterator<K, V> extends IdentityHashSetSpliterator<K> implements Spliterator<K> {
        KeySpliterator(QuantumIdentityHashSet<K> map, int origin, int fence, int est, int expectedModCount) {
            super(map, origin, fence, est, expectedModCount);
        }

        public KeySpliterator<K, V> trySplit() {
            int lo = index, mid = ((lo + getFence()) >>> 1) & ~1;
            return lo >= mid ? null : new KeySpliterator<>(map, lo, index = mid, est >>>= 1, expectedModCount);
        }

        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            int i = index;
            Object[] tab = map.table;
            if (i >= 0 && (index = getFence()) <= tab.length) {
                for (; i < index; i += 2) {
                    Object key = tab[i];
                    if (key != null) action.accept((K) key);
                }
                if (map.modCount == expectedModCount) return;
            }
            throw new ConcurrentModificationException();
        }

        @SuppressWarnings("unchecked")
        public boolean tryAdvance(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            Object[] tab = map.table;
            int hi = getFence();
            while (index < hi) {
                Object key = tab[index];
                if (key != null) {
                    action.accept((K) key);
                    if (map.modCount != expectedModCount) throw new ConcurrentModificationException();
                    return true;
                }
                index++;
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? SIZED : 0) | Spliterator.DISTINCT;
        }
    }

    private class IdentityHashSetIterator<T> implements Iterator<T> {
        int index = size != 0 ? 0 : table.length; // current slot.
        int expectedModCount = modCount; // to support fast-fail
        int lastReturnedIndex = -1;      // to allow remove()
        boolean indexValid; // To avoid unnecessary next computation
        Object[] traversalTable = table; // reference to main table or copy

        @Override
        public boolean hasNext() {
            Object[] tab = traversalTable;
            for (; index < tab.length; index++) {
                Object key = tab[index];
                if (key != null) return indexValid = true;
            }
            index = tab.length;
            return false;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T next() {
            return (T) traversalTable[nextIndex()];
        }

        protected int nextIndex() {
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
            if (!indexValid && !hasNext()) throw new NoSuchElementException();

            indexValid = false;
            lastReturnedIndex = index;
            index++;
            return lastReturnedIndex;
        }

        @Override
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

            // If traversing a copy, remove in real table.
            // We can skip gap-closure on copy.
            if (tab != QuantumIdentityHashSet.this.table) {
                QuantumIdentityHashSet.this.remove(key);
                expectedModCount = modCount;
                return;
            }

            size--;

            Object item;
            for (int i = nextKeyIndex(d, len); (item = tab[i]) != null; i = nextKeyIndex(i, len)) {
                int r = hash(item, len);
                // See closeDeletion for explanation of this conditional
                if ((i < r && (r <= d || d <= i)) || (r <= d && d <= i)) {

                    // If we are about to swap an already-seen element
                    // into a slot that may later be returned by next(),
                    // then clone the rest of table for use in future
                    // next() calls. It is OK that our copy will have
                    // a gap in the "wrong" place, since it will never
                    // be used for searching anyway.

                    if (i < deletedSlot && d >= deletedSlot && traversalTable == QuantumIdentityHashSet.this.table) {
                        int remaining = len - deletedSlot;
                        Object[] newTable = new Object[remaining];
                        System.arraycopy(tab, deletedSlot, newTable, 0, remaining);
                        traversalTable = newTable;
                        index = 0;
                    }

                    tab[d] = item;
                    tab[i] = null;
                    d = i;
                }
            }
        }
    }
}
