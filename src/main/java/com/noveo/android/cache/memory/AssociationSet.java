/*
 * Copyright (c) 2013 Noveo Group
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Except as contained in this notice, the name(s) of the above copyright holders
 * shall not be used in advertising or otherwise to promote the sale, use or
 * other dealings in this Software without prior written authorization.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.noveo.android.cache.memory;

import java.util.*;

/**
 * An association set.
 *
 * @param <V> a type of the values.
 * @param <K> a type of the keys.
 */
class AssociationSet<V, K> {

    private static final int DEFAULT_MAP_SIZE = 4;

    private final Map<V, Set<K>> values = new HashMap<V, Set<K>>();
    private final Map<K, Set<V>> associations = new HashMap<K, Set<V>>();

    /**
     * Clears the set.
     */
    public void clear() {
        values.clear();
        associations.clear();
    }

    /**
     * Adds a value to the set.
     *
     * @param value the value.
     */
    public void add(V value) {
        Set<K> associations = values.get(value);
        if (associations == null) {
            associations = new HashSet<K>(DEFAULT_MAP_SIZE);
            values.put(value, associations);
        }
    }

    /**
     * Associates a value with keys.
     *
     * @param value the value.
     * @param keys  the keys.
     */
    public void associate(V value, K... keys) {
        associate(value, Arrays.asList(keys));
    }

    /**
     * Associates a value with keys.
     *
     * @param value the value.
     * @param keys  the keys.
     */
    public void associate(V value, Iterable<K> keys) {
        Set<K> keySet = values.get(value);
        if (keySet != null) {
            for (K key : keys) {
                keySet.add(key);

                Set<V> set = associations.get(key);
                if (set == null) {
                    set = new HashSet<V>(DEFAULT_MAP_SIZE);
                    associations.put(key, set);
                }
                set.add(value);
            }
        }
    }

    /**
     * Disassociates a value with keys.
     *
     * @param value the value.
     * @param keys  the keys.
     */
    public void disassociate(V value, K... keys) {
        disassociate(value, Arrays.asList(keys));
    }

    /**
     * Disassociates a value with keys.
     *
     * @param value the value.
     * @param keys  the keys.
     */
    public void disassociate(V value, Iterable<K> keys) {
        Set<K> keySet = values.get(value);
        if (keySet != null) {
            for (K key : keys) {
                keySet.remove(key);


                Set<V> set = associations.get(key);
                if (set != null) {
                    set.remove(value);
                    if (set.isEmpty()) {
                        associations.remove(key);
                    }
                }
            }
        }
    }

    /**
     * Removes a value from the set.
     *
     * @param value the value.
     * @return true if the set contains the value and it is successfully removed.
     */
    public boolean remove(V value) {
        Set<K> keySet = values.remove(value);
        if (keySet == null) {
            return false;
        } else {
            disassociate(value, keySet);
            return true;
        }
    }

    /**
     * Returns a set of keys associated with the value.
     *
     * @param value the value.
     * @return the set of associations.
     */
    public Set<K> getAssociations(V value) {
        Set<K> set = values.get(value);
        if (set == null) {
            set = Collections.emptySet();
        } else {
            set = new HashSet<K>(set);
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * Returns a set of associated values from the set.
     *
     * @param key the key to find values associated with.
     * @return the set of values.
     */
    public Set<V> getAssociated(K key) {
        Set<V> set = associations.get(key);
        if (set == null) {
            set = Collections.emptySet();
        } else {
            set = new HashSet<V>(set);
        }
        return Collections.unmodifiableSet(set);
    }

}
