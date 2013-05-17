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

package com.noveogroup.android.cache.io;

import java.io.*;

/**
 * Simple implementation of serializer that provides
 * {@link ObjectInput} and {@link ObjectOutput} to user.
 *
 * @param <T> the type of values.
 */
public abstract class AbstractSerializer<T> implements Serializer<T> {

    /**
     * User should implement this method to save a value to a provided object output.
     *
     * @param objectOutput the object output.
     * @param value        the value.
     * @throws IOException if I/O error occurred.
     */
    protected abstract void save(ObjectOutput objectOutput, T value) throws IOException;

    @Override
    public void save(OutputSource outputSource, T value) throws IOException {
        OutputStream outputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            outputStream = outputSource.openOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
            save(objectOutputStream, value);
        } finally {
            try {
                if (objectOutputStream != null) {
                    objectOutputStream.close();
                }
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        }
    }

    /**
     * User should implement this method to load a value from a provided object input.
     *
     * @param objectInput the object input.
     * @return the value.
     * @throws IOException if I/O error occurred.
     */
    protected abstract T load(ObjectInput objectInput) throws IOException;

    @Override
    public T load(InputSource inputSource) throws IOException {
        InputStream inputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            inputStream = inputSource.openInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
            return load(objectInputStream);
        } finally {
            try {
                if (objectInputStream != null) {
                    objectInputStream.close();
                }
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
    }

}
