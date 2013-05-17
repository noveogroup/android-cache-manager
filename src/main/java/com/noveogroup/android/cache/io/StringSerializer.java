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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * loads and save string as  byte[], not as object.
 * Set charset which needs in constructor, otherwise default platform charset will be used
 */
public class StringSerializer extends AbstractSerializer<String> {

    private static final String DEFAULT_CHARSET = "UTF-8";

    private final String charset;
    private final ByteArraySerializer serializer;

    public StringSerializer() {
        this(DEFAULT_CHARSET);
    }

    public StringSerializer(String charset) {
        this.charset = charset;
        this.serializer = new ByteArraySerializer();
    }


    @Override
    protected void save(ObjectOutput objectOutput, String value) throws IOException {
        serializer.save(objectOutput, value.getBytes(charset));
    }

    @Override
    protected String load(ObjectInput objectInput) throws IOException {
        return new String(serializer.load(objectInput), charset);
    }

}
