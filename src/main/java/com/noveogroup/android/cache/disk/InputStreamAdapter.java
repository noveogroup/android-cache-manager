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

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * InputStreamAdapter can be used to represent RandomAccessFile as a InputStream object.
 */
class InputStreamAdapter extends InputStream {

    private final Object lock;
    private boolean isClosed;
    private final RandomAccessFile randomAccessFile;
    private long currentPosition;
    private final long endPosition;

    /**
     * Creates new adapter.
     *
     * @param lock             the lock object.
     * @param randomAccessFile the random access file.
     * @param beginPosition    position of the begin of the stream.
     * @param endPosition      position of the end of the stream.
     */
    public InputStreamAdapter(Object lock, RandomAccessFile randomAccessFile, long beginPosition, long endPosition) {
        if (lock == null || randomAccessFile == null) {
            throw new NullPointerException();
        }
        if (beginPosition > endPosition) {
            throw new IllegalArgumentException();
        }

        this.lock = lock;
        this.isClosed = false;
        this.randomAccessFile = randomAccessFile;
        this.currentPosition = beginPosition;
        this.endPosition = endPosition;
    }

    /**
     * Creates new adapter.
     *
     * @param randomAccessFile the random access file.
     * @param beginPosition    position of the begin of the stream.
     * @param endPosition      position of the end of the stream.
     */
    public InputStreamAdapter(RandomAccessFile randomAccessFile, long beginPosition, long endPosition) {
        this(new Object(), randomAccessFile, beginPosition, endPosition);
    }

    @Override
    public int available() throws IOException {
        synchronized (lock) {
            if (isClosed) {
                return 0;
            } else {
                long available = endPosition - currentPosition;
                return (int) Math.max(0, Math.min(Integer.MAX_VALUE, available));
            }
        }
    }

    /**
     * This method is called when the stream is closing.
     *
     * @throws IOException if I/O error occurred.
     */
    protected void onClose() throws IOException {
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            if (!isClosed) {
                isClosed = true;
                onClose();
            }
        }
    }

    @Override
    public int read() throws IOException {
        synchronized (lock) {
            if (available() > 0) {
                randomAccessFile.seek(currentPosition);
                int read = randomAccessFile.read();
                currentPosition = randomAccessFile.getFilePointer();
                return read;
            } else {
                return -1;
            }
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            int available = available();
            if (available > 0) {
                randomAccessFile.seek(currentPosition);
                int read = randomAccessFile.read(b, off, Math.min(len, available));
                currentPosition = randomAccessFile.getFilePointer();
                return read;
            } else {
                return -1;
            }
        }
    }

    @Override
    public long skip(long n) throws IOException {
        synchronized (lock) {
            int available = available();
            n = Math.min(n, available);
            randomAccessFile.seek(currentPosition + n);
            currentPosition = randomAccessFile.getFilePointer();
            return n;
        }
    }

}
