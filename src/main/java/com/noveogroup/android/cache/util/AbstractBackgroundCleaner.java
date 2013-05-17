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

package com.noveogroup.android.cache.util;

import android.os.SystemClock;

/**
 * Abstract background cache cleaner.
 */
public abstract class AbstractBackgroundCleaner {

    private final Object lock = new Object();
    private boolean threadAlive = false;

    private long lastCleanTime = 0;
    private long accessCount = 0;

    /**
     * Starts a cleaning process if it isn't running now.
     */
    public void clean() {
        synchronized (lock) {
            lastCleanTime = SystemClock.uptimeMillis();
            accessCount = 0;

            if (threadAlive) {
                return;
            } else {
                new Thread() {
                    @Override
                    public void run() {
                        setPriority(Thread.MIN_PRIORITY);
                        try {
                            cleanCache();
                        } finally {
                            synchronized (lock) {
                                threadAlive = false;
                            }
                        }
                    }
                }.start();
                threadAlive = true;
            }
        }
    }

    /**
     * Should be called when user makes some modification in the cache.
     * Can cause starting of cleaning process if there was a lot of
     * modifications or cleaning process had been running long time ago.
     *
     * @param cleanTimeDelay   the time delay between cleanings.
     * @param cleanAccessCount the maximum count of access between cleanings.
     */
    public void access(long cleanTimeDelay, long cleanAccessCount) {
        synchronized (lock) {
            accessCount++;

            if (lastCleanTime == 0) {
                lastCleanTime = SystemClock.uptimeMillis();
            }
            if (SystemClock.uptimeMillis() - lastCleanTime > cleanTimeDelay || accessCount > cleanAccessCount) {
                clean();
            }
        }
    }

    /**
     * Subclasses implements this method to do cleaning.
     * This method will be called in the background thread.
     */
    protected abstract void cleanCache();

}
