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

package com.noveogroup.android.cache.disk;

import android.os.SystemClock;
import android.util.Log;
import com.noveogroup.android.cache.io.DefaultSerializer;
import com.noveogroup.android.cache.io.FileSource;
import com.noveogroup.android.cache.io.Serializer;
import com.noveogroup.android.cache.util.Runner;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * This class stores and manages synchronization of disk cache settings.
 */
class PropertyManager {

//    private void sync() {
//        MetaData common = serializer.load(new FileSource(propertiesFile));
//        MetaData diffBackup = new MetaData();
//        synchronized (lock) {
//            common.putAll(diff);
//
//            properties.clear();
//            properties.putAll(common);
//
//            diffBackup.putAll(diff);
//            diff.clear();
//        }
//
//        try {
//            File tempFile = owner.createFile();
//            serializer.save(new FileSource(tempFile), common);
//            if (!tempFile.renameTo(propertiesFile)) {
//                throw new IOException("cannot move " + tempFile + " to " + propertiesFile);
//            }
//        } catch (IOException e) {
//            synchronized (lock) {
//                diffBackup.putAll(diff);
//                diff.clear();
//                diff.putAll(diffBackup);
//            }
//            throw e;
//        }
//    }

    private static final String KEY_CLEAN_TIME_DELAY = "clean-time-delay";
    private static final String KEY_CLEAN_ACCESS_COUNT = "clean-access-count";
    private static final String KEY_MAX_AGE = "max-age";
    private static final String KEY_MAX_SIZE = "max-size";
    private static final String KEY_EXPIRATION_TIME = "expiration-time";

    private static final String PROPERTIES_FILENAME = "properties.meta";
    private static final long LOAD_DELAY = 60 * 1000;
    private static final long SAVE_DELAY_BASE = 3 * 1000;
    private static final long SAVE_DELAY_DIFF = 7 * 1000;

    private final DiskCacheCore owner;
    private final File propertiesFile;

    private final Object lock = new Object();
    private final MetaData properties = new MetaData();
    private final MetaData diff = new MetaData();
    private final Serializer<MetaData> serializer = new DefaultSerializer<MetaData>();
    private long lastUpdateTime = 0;

    private final Runner propertySaver = new Runner(Thread.MAX_PRIORITY) {
        @Override
        public void run() {
            try {
                Thread.sleep((long) (SAVE_DELAY_BASE + Math.random() * SAVE_DELAY_DIFF));
            } catch (InterruptedException ignored) {
                return;
            }

            synchronized (lock) {
                try {
                    // we should save only diff
                    MetaData metaData = serializer.load(new FileSource(propertiesFile));
                    metaData.putAll(diff);
                    File file = owner.createFile();
                    serializer.save(new FileSource(file), metaData);
                    if (file.renameTo(propertiesFile)) {
                        throw new IOException("cannot move file from " + file + " to " + propertiesFile);
                    }

                    // properties will be merged only if saving has been successfully done
                    diff.clear();
                    properties.clear();
                    properties.putAll(metaData);
                    lastUpdateTime = SystemClock.uptimeMillis();
                } catch (IOException e) {
                    Log.v(DiskCacheCore.TAG, "cannot save properties", e);
                }
            }
        }
    };

    /**
     * Creates new instance of property manager.
     *
     * @param owner the owner cache.
     */
    public PropertyManager(DiskCacheCore owner) {
        this.owner = owner;
        this.propertiesFile = new File(owner.getCacheDirectory(), PROPERTIES_FILENAME);
        loadProperties();
    }

    private void loadProperties() {
        if (SystemClock.uptimeMillis() - lastUpdateTime >= LOAD_DELAY) {
            MetaData metaData;
            try {
                metaData = serializer.load(new FileSource(propertiesFile));
            } catch (IOException e) {
                Log.v(DiskCacheCore.TAG, "cannot load properties", e);
                metaData = new MetaData();
            }

            diff.clear();
            properties.clear();
            properties.putAll(metaData);
            lastUpdateTime = SystemClock.uptimeMillis();
        }
    }

    private <T extends Serializable> T getValue(String key, T defaultValue) {
        synchronized (lock) {
            loadProperties();
            return diff.getValue(key, properties.getValue(key, defaultValue));
        }
    }

    private <T extends Serializable> void putValue(String key, T value) {
        synchronized (lock) {
            diff.putValue(key, value);
            propertySaver.start();
        }
    }

    /**
     * Returns clean time delay.
     *
     * @param defaultValue a default value.
     * @return clean time delay value.
     */
    public long getCleanTimeDelay(long defaultValue) {
        return getValue(KEY_CLEAN_TIME_DELAY, defaultValue);
    }

    /**
     * Sets clean time delay.
     *
     * @param cleanTimeDelay new value of clean time delay.
     */
    public void setCleanTimeDelay(long cleanTimeDelay) {
        putValue(KEY_CLEAN_TIME_DELAY, cleanTimeDelay);
    }

    /**
     * Returns clean access count.
     *
     * @param defaultValue a default value.
     * @return clean access count value.
     */
    public long getCleanAccessCount(long defaultValue) {
        return getValue(KEY_CLEAN_ACCESS_COUNT, defaultValue);
    }

    /**
     * Sets clean access count.
     *
     * @param cleanAccessCount new value of clean access count.
     */
    public void setCleanAccessCount(long cleanAccessCount) {
        putValue(KEY_CLEAN_ACCESS_COUNT, cleanAccessCount);
    }

    /**
     * Returns max age.
     *
     * @param defaultValue a default value.
     * @return max age value.
     */
    public long getMaxAge(long defaultValue) {
        return getValue(KEY_MAX_AGE, defaultValue);
    }

    /**
     * Sets max age.
     *
     * @param maxAge new value of max age.
     */
    public void setMaxAge(long maxAge) {
        putValue(KEY_MAX_AGE, maxAge);
    }

    /**
     * Returns max size.
     *
     * @param defaultValue a default value.
     * @return max size value.
     */
    public long getMaxSize(long defaultValue) {
        return getValue(KEY_MAX_SIZE, defaultValue);
    }

    /**
     * Sets max size.
     *
     * @param maxSize new value of max size.
     */
    public void setMaxSize(long maxSize) {
        putValue(KEY_MAX_SIZE, maxSize);
    }

    /**
     * Returns expiration time.
     *
     * @param defaultValue a default value.
     * @return expiration time value.
     */
    public long getExpirationTime(long defaultValue) {
        return getValue(KEY_EXPIRATION_TIME, defaultValue);
    }

    /**
     * Sets expiration time.
     *
     * @param expirationTime new value of expiration time.
     */
    public void setExpirationTime(long expirationTime) {
        putValue(KEY_EXPIRATION_TIME, expirationTime);
    }

}