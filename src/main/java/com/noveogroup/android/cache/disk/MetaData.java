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
import java.util.HashMap;

/**
 * The class stores meta data of some cache entry.
 */
public class MetaData extends HashMap<String, Serializable> {

    /**
     * Creates new MetaData object.
     */
    public MetaData() {
        super();
    }

    /**
     * Creates new MetaData object.
     *
     * @param metaData an MetaData object to copy key-values from.
     */
    public MetaData(MetaData metaData) {
        super(metaData);
    }

    @Override
    public MetaData clone() {
        return (MetaData) super.clone();
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or null if this map contains no mapping for the key.
     *
     * @param key the key.
     * @param <T> type of value.
     * @return the value.
     */
    public <T extends Serializable> T getValue(String key) {
        return getValue(key, null);
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or a default value if this map contains no mapping for the key.
     *
     * @param key          the key.
     * @param defaultValue the default value.
     * @param <T>          type of the value.
     * @return the value.
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getValue(String key, T defaultValue) {
        T value = (T) get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Associates the specified value with the specified key in this map.
     *
     * @param key   the key.
     * @param value the value.
     * @param <T>   type of the value.
     * @return this MetaData object.
     */
    public <T extends Serializable> MetaData putValue(String key, T value) {
        return putValue(key, value, null);
    }

    /**
     * Associates the specified value (or a default one
     * if a parameter is null) with the specified key in this map.
     *
     * @param key          the key.
     * @param value        the value.
     * @param defaultValue the default value.
     * @param <T>          type of the value.
     * @return this MetaData object.
     */
    public <T extends Serializable> MetaData putValue(String key, T value, T defaultValue) {
        put(key, value != null ? value : defaultValue);
        return this;
    }

    /**
     * Check the specified condition and associates the specified value
     * with the specified key in this map.
     *
     * @param condition the condition.
     * @param key       the key.
     * @param value     the value.
     * @param <T>       type of the value.
     * @return this MetaData object.
     */
    public <T extends Serializable> MetaData putValueIf(boolean condition, String key, T value) {
        return putValueIf(condition, key, value, null);
    }

    /**
     * Check the specified condition and associates the specified value
     * (or a default one if a parameter is null) with the specified key in this map.
     *
     * @param condition    the condition.
     * @param key          the key.
     * @param value        the value.
     * @param defaultValue the default value.
     * @param <T>          type of the value.
     * @return this MetaData object.
     */
    public <T extends Serializable> MetaData putValueIf(boolean condition, String key, T value, T defaultValue) {
        if (condition) {
            put(key, value != null ? value : defaultValue);
        }
        return this;
    }

}
