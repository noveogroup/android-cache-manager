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

package com.noveo.android.cache.disk;

import com.noveo.android.cache.io.DefaultSerializer;
import com.noveo.android.cache.io.InputSource;
import com.noveo.android.cache.io.OutputSource;
import com.noveo.android.cache.io.Serializer;

import java.io.*;

/**
 * Class-helper that handles cache entries. Such entries a stored in
 * the cache entry files into the meta data subdirectory.
 *
 * @param <K> a type of the key.
 */
class DiskCacheEntry<K> {

    private static <K> void save(DiskCacheEntry<K> entry, File file, Serializer<K> keySerializer) throws IOException {
        RandomAccessFile accessFile = null;
        try {
            accessFile = new RandomAccessFile(file, "rw");
            final RandomAccessFile randomAccessFile = accessFile;

            // write a stub for length of a key
            final long keyLengthPosition = accessFile.getFilePointer();
            accessFile.writeLong(0);

            // write the key
            final long keyPosition = accessFile.getFilePointer();
            keySerializer.save(new OutputSource() {
                @Override
                public OutputStream openOutputStream() throws IOException {
                    return new OutputStreamAdapter(randomAccessFile, keyPosition) {
                        @Override
                        protected void onClose() throws IOException {
                            long keyLength = randomAccessFile.getFilePointer() - keyPosition;
                            randomAccessFile.seek(keyLengthPosition);
                            randomAccessFile.writeLong(keyLength);
                        }
                    };
                }
            }, entry.getKey());
            randomAccessFile.seek(keyLengthPosition);
            final long keyLength = accessFile.readLong();
            accessFile.seek(keyPosition + keyLength);

            // write create time and access time
            accessFile.writeLong(entry.getCreateTime());
            accessFile.writeLong(entry.getAccessTime());

            // write a stub for length of a meta data
            final long metaDataLengthPosition = accessFile.getFilePointer();
            accessFile.writeLong(0);

            // write the meta data
            DefaultSerializer<MetaData> metaDataSerializer = new DefaultSerializer<MetaData>();
            final long metaDataPosition = accessFile.getFilePointer();
            metaDataSerializer.save(new OutputSource() {
                @Override
                public OutputStream openOutputStream() throws IOException {
                    return new OutputStreamAdapter(randomAccessFile, metaDataPosition) {
                        @Override
                        protected void onClose() throws IOException {
                            long metaDataLength = randomAccessFile.getFilePointer() - metaDataPosition;
                            randomAccessFile.seek(metaDataLengthPosition);
                            randomAccessFile.writeLong(metaDataLength);
                        }
                    };
                }
            }, entry.getMetaData());
            randomAccessFile.seek(metaDataLengthPosition);
            final long metaDataLength = accessFile.readLong();
            accessFile.seek(metaDataPosition + metaDataLength);

            // write file path
            if (entry.getFile() == null) {
                accessFile.writeInt(-1);
            } else {
                String filePath = entry.getFile().getPath();
                accessFile.writeInt(filePath.length());
                accessFile.write(filePath.getBytes("UTF-8"));
            }
        } finally {
            if (accessFile != null) {
                accessFile.close();
            }
        }
    }

    private static <K> void load(DiskCacheEntry<K> entry, File file, Serializer<K> keySerializer) throws IOException {
        RandomAccessFile accessFile = null;
        try {
            accessFile = new RandomAccessFile(file, "rw");
            final RandomAccessFile randomAccessFile = accessFile;

            // read length of a key
            final long keyLength = accessFile.readLong();

            // read the key
            final long keyPosition = accessFile.getFilePointer();
            entry.setKey(keySerializer.load(new InputSource() {
                @Override
                public InputStream openInputStream() throws IOException {
                    return new InputStreamAdapter(randomAccessFile, keyPosition, keyPosition + keyLength);
                }
            }));
            accessFile.seek(keyPosition + keyLength);

            // read create time and access time
            entry.setCreateTime(accessFile.readLong());
            entry.setAccessTime(accessFile.readLong());

            // read length of a meta data
            final long metaDataLength = accessFile.readLong();

            // read the meta data
            DefaultSerializer<MetaData> metaDataSerializer = new DefaultSerializer<MetaData>();
            final long metaDataPosition = accessFile.getFilePointer();
            entry.setMetaData(metaDataSerializer.load(new InputSource() {
                @Override
                public InputStream openInputStream() throws IOException {
                    return new InputStreamAdapter(randomAccessFile, metaDataPosition, metaDataPosition + metaDataLength);
                }
            }));
            accessFile.seek(metaDataPosition + metaDataLength);

            // read file path
            int pathLength = accessFile.readInt();
            if (pathLength < 0) {
                entry.setFile(null);
            } else {
                long pathPosition = accessFile.getFilePointer();
                InputStreamAdapter pathInputStream = new InputStreamAdapter(accessFile, pathPosition, pathPosition + pathLength);
                byte[] pathBytes = Utils.readFully(pathInputStream);
                entry.setFile(new File(new String(pathBytes, "UTF-8")));
                accessFile.seek(pathPosition + pathLength);
            }
        } finally {
            if (accessFile != null) {
                accessFile.close();
            }
        }
    }

    private K key;
    private long createTime;
    private long accessTime;
    private MetaData metaData;
    private File file;

    /**
     * Creates new disk cache entry.
     */
    public DiskCacheEntry() {
    }

    /**
     * Returns the key.
     *
     * @return the key.
     */
    public K getKey() {
        return key;
    }

    /**
     * Sets a value of the key.
     *
     * @param key new key.
     */
    public void setKey(K key) {
        this.key = key;
    }

    /**
     * Returns create time.
     *
     * @return create time.
     */
    public long getCreateTime() {
        return createTime;
    }

    /**
     * Sets new create time value.
     *
     * @param createTime new create time value.
     */
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    /**
     * Returns access time.
     *
     * @return access time.
     */
    public long getAccessTime() {
        return accessTime;
    }

    /**
     * Sets new access time value.
     *
     * @param accessTime new access time value.
     */
    public void setAccessTime(long accessTime) {
        this.accessTime = accessTime;
    }

    /**
     * Returns meta data bundle.
     *
     * @return meta data bundle.
     */
    public MetaData getMetaData() {
        return metaData;
    }

    /**
     * Sets new meta data bundle.
     *
     * @param metaData new meta data bundle.
     */
    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }

    /**
     * Returns a file containing the content.
     *
     * @return a file containing the content.
     */
    public File getFile() {
        return file;
    }

    /**
     * Sets new file containing the content.
     *
     * @param file new file containing the content or null.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Saves the entry to a file using the specified key serializer.
     *
     * @param file          the file to store entry.
     * @param keySerializer the key serializer.
     * @throws IOException if I/O error occurred.
     */
    protected void saveEntry(File file, Serializer<K> keySerializer) throws IOException {
        save(this, file, keySerializer);
    }

    /**
     * Loads the entry from a file using the specified key serializer.
     *
     * @param file          the file that stores entry.
     * @param keySerializer the key serializer.
     * @throws IOException if I/O error occurred.
     */
    protected void loadEntry(File file, Serializer<K> keySerializer) throws IOException {
        load(this, file, keySerializer);
    }

}
