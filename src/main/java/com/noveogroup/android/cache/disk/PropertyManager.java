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

import java.io.Serializable;

/**
 * This class stores disk cache settings.
 */
class PropertyManager {

    private static final String KEY_CLEAN_TIME_DELAY = "clean-time-delay";
    private static final String KEY_CLEAN_ACCESS_COUNT = "clean-access-count";
    private static final String KEY_MAX_AGE = "max-age";
    private static final String KEY_MAX_SIZE = "max-size";
    private static final String KEY_EXPIRATION_TIME = "expiration-time";

    private final Object lock = new Object();
    private final MetaData properties = new MetaData();

    /**
     * Creates new instance of property manager.
     */
    public PropertyManager() {
    }

    private <T extends Serializable> T getValue(String key, T defaultValue) {
        synchronized (lock) {
            return properties.getValue(key, defaultValue);
        }
    }

    private <T extends Serializable> void putValue(String key, T value) {
        synchronized (lock) {
            properties.putValue(key, value);
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