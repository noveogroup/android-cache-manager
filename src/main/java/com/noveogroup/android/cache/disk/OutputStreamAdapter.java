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
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * InputStreamAdapter can be used to represent RandomAccessFile as a OutputStream object.
 */
class OutputStreamAdapter extends OutputStream {

    private final Object lock;
    private boolean isClosed;
    private final RandomAccessFile randomAccessFile;
    private final long beginPosition;
    private long currentPosition;

    /**
     * Creates new adapter.
     *
     * @param lock             the lock object.
     * @param randomAccessFile the random access file.
     * @param beginPosition    position of the begin of the stream.
     */
    public OutputStreamAdapter(Object lock, RandomAccessFile randomAccessFile, long beginPosition) {
        if (lock == null || randomAccessFile == null) {
            throw new NullPointerException();
        }

        this.lock = lock;
        this.isClosed = false;
        this.randomAccessFile = randomAccessFile;
        this.beginPosition = beginPosition;
        this.currentPosition = beginPosition;
    }

    /**
     * Creates new adapter.
     *
     * @param randomAccessFile the random access file.
     * @param beginPosition    position of the begin of the stream.
     */
    public OutputStreamAdapter(RandomAccessFile randomAccessFile, long beginPosition) {
        this(new Object(), randomAccessFile, beginPosition);
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

    /**
     * Returns count of bytes already written.
     *
     * @return length of the stream.
     */
    public long length() {
        return currentPosition - beginPosition;
    }

    @Override
    public void write(int b) throws IOException {
        synchronized (lock) {
            if (!isClosed) {
                randomAccessFile.seek(currentPosition);
                randomAccessFile.write(b);
                currentPosition = randomAccessFile.getFilePointer();
            }
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            if (!isClosed) {
                randomAccessFile.seek(currentPosition);
                randomAccessFile.write(b, off, len);
                currentPosition = randomAccessFile.getFilePointer();
            }
        }
    }

}
