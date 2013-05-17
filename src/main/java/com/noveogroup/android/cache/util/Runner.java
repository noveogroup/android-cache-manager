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

/**
 * Background runner.
 */
public class Runner implements Runnable {

    private final Runnable runnable;
    private final Object lock = new Object();
    private final int priority;
    private Thread thread = null;

    /**
     * Creates new instance.
     */
    public Runner() {
        this(Thread.NORM_PRIORITY, null);
    }

    /**
     * Creates new instance.
     *
     * @param priority the thread priority.
     */
    public Runner(int priority) {
        this(priority, null);
    }

    /**
     * Creates new instance.
     *
     * @param runnable the runnable.
     */
    public Runner(Runnable runnable) {
        this(Thread.NORM_PRIORITY, runnable);
    }

    /**
     * Creates new instance.
     *
     * @param priority the thread priority.
     * @param runnable the runnable.
     */
    public Runner(int priority, Runnable runnable) {
        this.priority = priority;
        this.runnable = runnable;
    }

    /**
     * Creates runner thread.
     *
     * @param priority the thread priority.
     * @param runnable the runnable to execute (it is not a runnable that user set before).
     * @return the thread.
     */
    protected Thread createThread(int priority, Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setPriority(priority);
        return thread;
    }

    /**
     * Starts a process if it isn't running now.
     */
    public final void start() {
        synchronized (lock) {
            if (thread != null) {
                return;
            } else {
                thread = createThread(priority, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Runner.this.run();
                        } finally {
                            synchronized (lock) {
                                thread = null;
                            }
                        }
                    }
                });
                thread.start();
            }
        }
    }

    @Override
    public void run() {
        if (runnable != null) {
            runnable.run();
        }
    }

}
