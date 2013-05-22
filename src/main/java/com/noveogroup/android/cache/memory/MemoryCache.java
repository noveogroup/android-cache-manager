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

package com.noveogroup.android.cache.memory;

import android.os.SystemClock;
import com.noveogroup.android.cache.io.DefaultKeyManager;
import com.noveogroup.android.cache.io.KeyManager;
import com.noveogroup.android.cache.util.AbstractBackgroundCleaner;
import com.noveogroup.android.cache.util.CleanerHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Android Memory Cache
 *
 * @param <K> a type of keys.
 * @param <V> a type of values.
 */
public class MemoryCache<K, V> {

    /**
     * Default value of clean time delay.
     */
    public static final long DEFAULT_CLEAN_TIME_DELAY = 3 * 60 * 1000;
    /**
     * Default value of clean modification count.
     */
    public static final long DEFAULT_CLEAN_MODIFICATION_COUNT = 100;
    /**
     * Default value of max age.
     */
    public static final long DEFAULT_MAX_AGE = 60 * 60 * 1000L;
    /**
     * Default value of max size.
     */
    public static final long DEFAULT_MAX_SIZE = 256L;
    /**
     * Default value of expiration time.
     */
    public static final long DEFAULT_EXPIRATION_TIME = 60 * 1000L;

    private static class KeyHolder<K, V> {

        private final MemoryCache<K, V> owner;
        private final K key;

        public KeyHolder(MemoryCache<K, V> owner, K key) {
            this.owner = owner;
            this.key = key;
        }

        public K getKey() {
            return key;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            KeyHolder<K, V> keyHolder = (KeyHolder<K, V>) o;
            return owner.keyManager.equals(key, keyHolder.key);
        }

        @Override
        public int hashCode() {
            return owner.keyManager.hashCode(key);
        }

    }

    private static class ValueHolder<K, V> implements Reference<V> {

        private final MemoryCache<K, V> owner;
        private V value;
        private long accessTime;

        public ValueHolder(MemoryCache<K, V> owner, V value) {
            this.owner = owner;
            this.value = value;
            this.accessTime = SystemClock.uptimeMillis();
        }

        public synchronized long getAccessTime() {
            return accessTime;
        }

        public synchronized long size() {
            return value == null ? 0 : owner.calculateSize(value);
        }

        @Override
        public synchronized void clear() {
            if (value != null) {
                owner.releaseValue(value);
                value = null;
            }
        }

        @Override
        public synchronized V get() {
            accessTime = SystemClock.uptimeMillis();
            return value;
        }

    }

    /**
     * Cache access object.
     */
    public static class Access<K, V> {

        private final MemoryCache<K, V> owner;
        private final Object token;

        private Access(MemoryCache<K, V> owner, Object token) {
            this.owner = owner;
            this.token = token;
        }

        /**
         * Erases a part of cache associated with this access object.
         */
        public void erase() {
            synchronized (owner.lock) {
                for (ValueHolder<K, V> valueHolder : owner.associations.getAssociated(token)) {
                    owner.associations.disassociate(valueHolder, token);
                    if (owner.associations.getAssociations(valueHolder).isEmpty()) {
                        valueHolder.clear();
                    }
                }
            }
        }

        /**
         * Starts background cleaning process.
         */
        public void clean() {
            owner.cleaner.clean();
        }

        /**
         * Returns size of a part of cache associated with this access object.
         *
         * @return the size of associated part of cache.
         */
        public long size() {
            synchronized (owner.lock) {
                owner.cleaner.access(false, owner.getCleanTimeDelay(), owner.getCleanModificationCount());

                long size = 0;
                for (ValueHolder valueHolder : owner.associations.getAssociated(token)) {
                    size += valueHolder.size();
                }
                return size;
            }
        }

        /**
         * Returns a reference to cached value.
         *
         * @param key the key.
         * @return the reference to the value.
         */
        public Reference<V> get(K key) {
            synchronized (owner.lock) {
                owner.cleaner.access(false, owner.getCleanTimeDelay(), owner.getCleanModificationCount());

                KeyHolder<K, V> keyHolder = new KeyHolder<K, V>(owner, key);

                ValueHolder<K, V> valueHolder = owner.cache.get(keyHolder);

                if (valueHolder != null) {
                    if (valueHolder.get() == null) {
                        owner.cache.remove(keyHolder);
                        owner.associations.remove(valueHolder);
                        valueHolder = null;
                    }
                }

                if (valueHolder != null) {
                    owner.associations.associate(valueHolder, token);
                }

                return valueHolder != null ? valueHolder : new ValueHolder<K, V>(owner, null);
            }
        }

        /**
         * Puts a value to the cache and refers it with a key.
         *
         * @param key   the key.
         * @param value the value.
         */
        public void put(K key, V value) {
            synchronized (owner.lock) {
                owner.cleaner.access(true, owner.getCleanTimeDelay(), owner.getCleanModificationCount());

                KeyHolder<K, V> keyHolder = new KeyHolder<K, V>(owner, key);

                ValueHolder<K, V> valueHolder = owner.cache.get(keyHolder);
                if (valueHolder != null) {
                    valueHolder.clear();
                    owner.associations.remove(valueHolder);
                }

                if (value != null) {
                    valueHolder = new ValueHolder<K, V>(owner, value);
                    owner.cache.put(keyHolder, valueHolder);
                    owner.associations.add(valueHolder);
                    owner.associations.associate(valueHolder, token);
                } else {
                    owner.cache.remove(keyHolder);
                }
            }
        }

    }

    private class CleanerItem extends CleanerHelper.Item<ValueHolder> {

        public CleanerItem(ValueHolder valueHolder) {
            super(valueHolder,
                    System.currentTimeMillis() - valueHolder.getAccessTime(),
                    valueHolder.size());
        }

        @Override
        public void delete() {
            source.clear();
        }

        @Override
        public boolean canDelete() {
            return age < 0 || age > getExpirationTime();
        }

        @Override
        public boolean shouldDelete() {
            return source.get() == null || (getMaxAge() >= 0 && age > getMaxAge());
        }

    }

    private final Object lock = new Object();
    private final Map<KeyHolder<K, V>, ValueHolder<K, V>> cache = new HashMap<KeyHolder<K, V>, ValueHolder<K, V>>();
    private final AssociationSet<ValueHolder<K, V>, Object> associations = new AssociationSet<ValueHolder<K, V>, Object>();

    private final Access<K, V> access = new Access<K, V>(this, null);

    private final KeyManager<K> keyManager;

    private volatile long cleanTimeDelay = DEFAULT_CLEAN_TIME_DELAY;
    private volatile long cleanModificationCount = DEFAULT_CLEAN_MODIFICATION_COUNT;
    private volatile long maxAge = DEFAULT_MAX_AGE;
    private volatile long maxSize = DEFAULT_MAX_SIZE;
    private volatile long expirationTime = DEFAULT_EXPIRATION_TIME;

    private AbstractBackgroundCleaner cleaner = new AbstractBackgroundCleaner() {
        @Override
        protected void cleanCache() {
            ArrayList<ValueHolder> list;
            synchronized (lock) {
                list = new ArrayList<ValueHolder>(cache.values());
            }

            List<CleanerItem> expiredList = new ArrayList<CleanerItem>(list.size());
            List<CleanerItem> protectedList = new ArrayList<CleanerItem>(list.size());
            CleanerHelper.entities(new CleanerHelper.Loader<CleanerItem, ValueHolder>() {
                @Override
                public CleanerItem load(ValueHolder source) {
                    return new CleanerItem(source);
                }
            }, list, expiredList, protectedList);
            CleanerHelper.clean(expiredList, protectedList, maxSize);
        }
    };

    /**
     * Creates new memory cache.
     */
    public MemoryCache() {
        this(new DefaultKeyManager<K>());
    }

    /**
     * Creates new memory cache.
     *
     * @param keyManager a key manager.
     */
    public MemoryCache(KeyManager<K> keyManager) {
        this.keyManager = keyManager;
    }

    /**
     * Releases a value.
     *
     * @param value the value.
     */
    protected void releaseValue(V value) {
    }

    /**
     * Returns size of the value.
     *
     * @param value the value.
     * @return the size of the value.
     */
    protected long calculateSize(V value) {
        return 1;
    }

    /**
     * Returns clean time delay value.
     *
     * @return clean time delay.
     */
    public long getCleanTimeDelay() {
        return cleanTimeDelay;
    }

    /**
     * Sets clean time delay.
     *
     * @param cleanTimeDelay new clean time delay.
     */
    public void setCleanTimeDelay(long cleanTimeDelay) {
        this.cleanTimeDelay = cleanTimeDelay;
    }

    /**
     * Returns clean modification count value.
     *
     * @return clean modification count.
     */
    public long getCleanModificationCount() {
        return cleanModificationCount;
    }

    /**
     * Sets clean modification count.
     *
     * @param cleanModificationCount new clean modification count.
     */
    public void setCleanModificationCount(long cleanModificationCount) {
        this.cleanModificationCount = cleanModificationCount;
    }

    /**
     * Returns max age value.
     *
     * @return max age.
     */
    public long getMaxAge() {
        return maxAge;
    }

    /**
     * Sets max age value.
     *
     * @param maxAge new max age.
     */
    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    /**
     * Returns max size value.
     *
     * @return max size.
     */
    public long getMaxSize() {
        return maxSize;
    }

    /**
     * Sets max size value.
     *
     * @param maxSize new max size.
     */
    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Returns expiration time value.
     *
     * @return expiration time.
     */
    public long getExpirationTime() {
        return expirationTime;
    }

    /**
     * Sets expiration time value.
     *
     * @param expirationTime new expiration time.
     */
    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    /**
     * Erases the cache.
     */
    public void erase() {
        synchronized (lock) {
            for (ValueHolder valueHolder : cache.values()) {
                valueHolder.clear();
            }
            cache.clear();
            associations.clear();
        }
    }

    /**
     * Starts background cleaning process.
     */
    public void clean() {
        cleaner.clean();
    }

    /**
     * Returns size of whole cache.
     *
     * @return the size.
     */
    public long size() {
        synchronized (lock) {
            cleaner.access(false, getCleanTimeDelay(), getCleanModificationCount());

            long size = 0;
            for (ValueHolder valueHolder : cache.values()) {
                size += valueHolder.size();
            }
            return size;
        }
    }

    /**
     * Returns a main access object associated with <code>null</code> token.
     *
     * @return the main access object.
     */
    public Access<K, V> access() {
        return access;
    }

    /**
     * Returns a main access object associated with the specified token.
     *
     * @param token the token.
     * @return the main access object associated with the token.
     */
    public Access<K, V> access(Object token) {
        return new Access<K, V>(this, token);
    }

    /**
     * Returns a reference to cached value.
     * <p/>
     * <b>Note</b>: The main access object is used in this helper method.
     *
     * @param key the key.
     * @return the reference to the value.
     */
    public Reference<V> get(K key) {
        return access.get(key);
    }

    /**
     * Puts a value to the cache and refers it with a key.
     * <p/>
     * <b>Note</b>: The main access object is used in this helper method.
     *
     * @param key   the key.
     * @param value the value.
     */
    public void put(K key, V value) {
        access.put(key, value);
    }

}
