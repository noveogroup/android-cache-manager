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

package com.noveo.android.cache.disk;

import android.util.Log;
import com.noveo.android.cache.io.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Android Disk Cache.
 *
 * @param <K> a type of keys.
 * @see com.noveo.android.cache Description of Android Disk Cache
 */
public class DiskCache<K> extends DiskCacheCore<K> {

    /**
     * Creates new disk cache instance using default key manager and serializer.
     *
     * @param cacheDirectory the cache directory.
     * @param keyClass       to determine key type.
     * @param <K>            type of keys.
     * @return new instance.
     */
    public static <K extends Serializable> DiskCache<K> create(File cacheDirectory, Class<K> keyClass) {
        return new DiskCache<K>(false, cacheDirectory, new DefaultKeyManager<K>(), new DefaultSerializer<K>());
    }

    /**
     * Creates new disk cache instance using default key manager and serializer.
     *
     * @param debugMode      true if debug mode is on.
     * @param cacheDirectory the cache directory.
     * @param keyClass       to determine key type.
     * @param <K>            type of keys.
     * @return new instance.
     */
    public static <K extends Serializable> DiskCache<K> create(boolean debugMode, File cacheDirectory, Class<K> keyClass) {
        return new DiskCache<K>(debugMode, cacheDirectory, new DefaultKeyManager<K>(), new DefaultSerializer<K>());
    }

    /**
     * Creates new disk cache instance using default key manager.
     *
     * @param cacheDirectory the cache directory.
     * @param serializer     a key serializer.
     */
    public static <K> DiskCache<K> create(File cacheDirectory, Serializer<K> serializer) {
        return new DiskCache<K>(false, cacheDirectory, new DefaultKeyManager<K>(), serializer);
    }

    /**
     * Creates new disk cache instance using default key manager.
     *
     * @param debugMode      true if debug mode is on.
     * @param cacheDirectory the cache directory.
     * @param serializer     a key serializer.
     */
    public static <K> DiskCache<K> create(boolean debugMode, File cacheDirectory, Serializer<K> serializer) {
        return new DiskCache<K>(debugMode, cacheDirectory, new DefaultKeyManager<K>(), serializer);
    }

    /**
     * Creates new disk cache instance.
     *
     * @param cacheDirectory the cache directory.
     * @param keyManager     a key manager.
     * @param serializer     a key serializer.
     */
    public static <K> DiskCache<K> create(File cacheDirectory, KeyManager<K> keyManager, Serializer<K> serializer) {
        return new DiskCache<K>(false, cacheDirectory, keyManager, serializer);
    }

    /**
     * Creates new disk cache instance.
     *
     * @param debugMode      true if debug mode is on.
     * @param cacheDirectory the cache directory.
     * @param keyManager     a key manager.
     * @param serializer     a key serializer.
     */
    public static <K> DiskCache<K> create(boolean debugMode, File cacheDirectory, KeyManager<K> keyManager, Serializer<K> serializer) {
        return new DiskCache<K>(debugMode, cacheDirectory, keyManager, serializer);
    }

    private volatile boolean verbose;

    /**
     * Creates new disk cache instance.
     *
     * @param debugMode      true if debug mode is on.
     * @param cacheDirectory the cache directory.
     * @param keyManager     a key manager.
     * @param serializer     a key serializer.
     */
    public DiskCache(boolean debugMode, File cacheDirectory, KeyManager<K> keyManager, Serializer<K> serializer) {
        super(debugMode, cacheDirectory, keyManager, serializer);
        this.verbose = debugMode;
    }

    /**
     * Returns true if verbose mode is turned on.
     *
     * @return the verbose status.
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Turns verbose mode on and off.
     *
     * @param verbose the verbose status.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Checks if the cache contains an entry corresponding
     * to the specified key.
     *
     * @param key the key.
     * @return true if the entry exists.
     */
    public boolean contains(K key) {
        return search(key) != null;
    }

    /**
     * Removes an entry that is corresponding to the specified key.
     *
     * @param key the key
     * @return true is the entry is successfully removed.
     */
    public boolean remove(K key) {
        Entry<K> entry = entry(key);
        if (entry == null) {
            return false;
        } else {
            try {
                Utils.delete(entry.getFile());
                entry.remove();
                return true;
            } catch (IOException e) {
                Log.w(DiskCache.TAG, "cannot remove an entry", e);
                if (verbose) {
                    throw new RuntimeException(e);
                }
                return false;
            }
        }
    }

    private <K> boolean commitEntry(Entry<K> entry) {
        try {
            entry.access();
            entry.commit();
            return true;
        } catch (IOException e) {
            Log.w(DiskCache.TAG, "cannot commit an entry", e);
            if (verbose) {
                throw new RuntimeException(e);
            }
            return false;
        }
    }

    /**
     * Returns meta data corresponding to the specified key.
     *
     * @param key the key.
     * @return the meta data.
     */
    public MetaData getMetaData(K key) {
        Entry<K> entry = entry(key);
        if (entry.exists()) {
            commitEntry(entry);
        }
        return entry.getMetaData();
    }

    /**
     * Sets new meta data for an entry that is corresponding
     * to the specified key.
     *
     * @param key      the key.
     * @param metaData the meta data.
     */
    public void putMetaData(K key, MetaData metaData) {
        Entry<K> entry = entry(key);
        entry.setMetaData(metaData);
        commitEntry(entry);
    }

    /**
     * Loads a value from the file that is corresponding to the specified key.
     * The value will be loaded using the specified serializer.
     *
     * @param key             the key.
     * @param valueSerializer the value serializer.
     * @param <V>             a type of the value.
     * @return the value.
     */
    public <V> V get(K key, Serializer<V> valueSerializer) {
        Entry<K> entry = search(key);
        if (entry == null) {
            return null;
        } else {
            File file = entry.getFile();
            if (file == null) {
                return null;
            }

            if (!commitEntry(entry)) {
                return null;
            }

            try {
                FileSource source = new FileSource(file);
                return valueSerializer.load(source);
            } catch (IOException e) {
                Log.w(DiskCache.TAG, "cannot load an entry", e);
                if (verbose) {
                    throw new RuntimeException(e);
                }
                return null;
            }

        }
    }

    /**
     * Loads a value from the file that is corresponding to the specified key.
     * The value will be loaded using default serializer.
     *
     * @param key the key.
     * @param <V> a type of the value.
     * @return the value.
     */
    public <V extends Serializable> V get(K key) {
        return get(key, new DefaultSerializer<V>());
    }

    /**
     * Saves an entry to the cache. The key will be linked to the specified
     * value. The value will be saved using the specified serializer.
     *
     * @param key             the key.
     * @param value           the value
     * @param valueSerializer the value serializer.
     * @param <V>             a type of the value.
     */
    public <V> void put(K key, V value, Serializer<V> valueSerializer) {
        File file;
        try {
            file = createFile();
            FileSource source = new FileSource(file);
            valueSerializer.save(source, value);
        } catch (IOException e) {
            Log.w(DiskCache.TAG, "cannot save a value", e);
            if (verbose) {
                throw new RuntimeException(e);
            }
            return;
        }

        Entry<K> entry = entry(key);
        entry.setFile(file);
        commitEntry(entry);
    }

    /**
     * Saves an entry to the cache. The key will be linked to the specified
     * value. The value will be saved using default serializer.
     *
     * @param key   the key.
     * @param value the value
     * @param <V>   a type of the value.
     */
    public <V extends Serializable> void put(K key, V value) {
        put(key, value, null, new DefaultSerializer<V>());
    }

    /**
     * Saves an entry to the cache. The key will be linked to the specified
     * value and meta data. The value will be saved using the specified
     * serializer.
     *
     * @param key             the key.
     * @param value           the value
     * @param metaData        the meta data.
     * @param valueSerializer the value serializer.
     * @param <V>             a type of the value.
     */
    public <V> void put(K key, V value, MetaData metaData, Serializer<V> valueSerializer) {
        File file;
        try {
            file = createFile();
            FileSource source = new FileSource(file);
            valueSerializer.save(source, value);
        } catch (IOException e) {
            Log.w(DiskCache.TAG, "cannot save a value", e);
            if (verbose) {
                throw new RuntimeException(e);
            }
            return;
        }

        Entry<K> entry = entry(key);
        entry.setFile(file);
        entry.setMetaData(metaData);
        commitEntry(entry);
    }

    /**
     * Saves an entry to the cache. The key will be linked to the specified
     * value and meta data. The value will be saved using default serializer.
     *
     * @param key      the key.
     * @param value    the value
     * @param metaData the meta data.
     * @param <V>      a type of the value.
     */
    public <V extends Serializable> void put(K key, V value, MetaData metaData) {
        put(key, value, metaData, new DefaultSerializer<V>());
    }

}
