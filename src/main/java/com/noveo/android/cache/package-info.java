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

/**
 * Provides classes of Android Cache Manager.
 * <p>
 * Android Cache Manager contains an Android Disk Cache that implements
 * LRU algorithm to manage cache data stored on the disk.
 * This cache has the following features:
 * <ul>
 * <li>Short access time</li>
 * <li>Flexible cleaning settings</li>
 * The cache cleans its storage according to user's settings such as
 * max age of file within the cache and max size of the cache.
 * <li>Thread-safe implementation</li>
 * The same Android Disk Cache can be used by different threads of
 * one JVM, and even by different JVMs.
 * <li>User-friendly interface</li>
 * In addition to main implementation of cache storage there is
 * a simple implementation. New instances of cache can be created
 * without any difficulties just using default settings.
 * <li>Meta data support</li>
 * Users can store additional meta-data with any cache entry.
 * <li>Useful debug mode</li>
 * When debugging is turned on disk cache will emulate additional time delay
 * for I/O operations, delete entries unexpectedly and immediately report
 * any exceptions.
 * <li>Detailed logging</li>
 * <li>Fail-safety</li>
 * </ul>
 * </p>
 */
package com.noveo.android.cache;
