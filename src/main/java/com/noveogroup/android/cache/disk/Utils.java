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

import android.util.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

final class Utils {

    private Utils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Calculates size of file or directory.
     *
     * @param file the file or the directory.
     * @return the size.
     */
    public static long calculateSize(File file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        if (!file.isDirectory()) {
            return file.length();
        } else {
            long size = 0;
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    size += calculateSize(f);
                }
            }
            return size;
        }
    }

    /**
     * Returns a list of files containing in the specified path.
     *
     * @param file        the path.
     * @param recursively if files should be found recursively.
     * @return the list of the files.
     */
    public static List<File> listFiles(File file, boolean recursively) {
        final ArrayList<File> list = new ArrayList<File>();
        listFiles(file, recursively, list);
        return list;
    }

    private static void listFiles(File file, boolean recursively, List<File> list) {
        if (!file.isDirectory()) {
            if (file.exists()) {
                list.add(file);
            }
        } else {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (recursively) {
                        listFiles(f, recursively, list);
                    } else {
                        list.add(f);
                    }
                }
            }
        }
    }

    /**
     * Creates new temporary file or directory in the specified directory. A name of the file will
     * start with the specified prefix and end with the specified suffix.
     *
     * @param directory the directory.
     * @param prefix    the prefix of the name of the file.
     * @param suffix    the suffix of the name of the file.
     * @param parent    the parent directory.
     * @return the temporary file.
     * @throws IOException if I/O error occurred and a file could not be created.
     */
    public static File createTempFile(boolean directory, String prefix, String suffix, File parent) throws IOException {
        // create parent directory
        parent.mkdirs();

        // fix the prefix
        if (prefix == null) {
            prefix = "";
        }
        // to prevent possible duplications add timestamp to prefix
        prefix = String.format("%08X-%s", System.currentTimeMillis(), prefix);
        File file = File.createTempFile(prefix, suffix, parent);
        if (!directory) {
            return file;
        } else {
            // delete created temp file and make a directory instead
            file.delete();
            file.mkdirs();
            // check that the directory is created
            if (!file.isDirectory()) {
                // throw an exception just like File.createTempFile
                throw new IOException("temp directory hasn't been created: " + file);
            }
            return file;
        }
    }

    /**
     * Deletes a file or a directory. Similar to {@link #deleteRecursively(java.io.File)}.
     *
     * @param file the file or the directory.
     * @return true if and only if the file is successfully deleted.
     * @see #deleteRecursively(java.io.File)
     */
    public static boolean delete(File file) {
        return deleteRecursively(file);
    }

    /**
     * Deletes a file or a directory.
     *
     * @param file the file or the directory.
     * @return true if and only if the file is successfully deleted.
     */
    public static boolean deleteRecursively(File file) {
        if (file == null) {
            return true;
        }
        boolean success = true;
        if (file.isDirectory()) {
            try {
                // check symlink
                File testFile = new File(file, "test");
                if (testFile.getCanonicalFile().equals(testFile.getAbsoluteFile())) {
                    success = deleteContent(file);
                }
            } catch (IOException e) {
                success = false;
            }
        }
        boolean isFileDeleted = file.delete();
        if (!isFileDeleted) {
            if (file.exists()) {
                Log.v(DiskCacheCore.TAG, "cannot delete " + file);
            } else {
                isFileDeleted = true;
            }
        }
        success = success && isFileDeleted;
        return success;
    }

    /**
     * Deletes a content of a directory.
     *
     * @param directory the directory.
     * @return true if and only if the content is successfully deleted.
     */
    public static boolean deleteContent(File directory) {
        if (directory == null) {
            return true;
        }
        boolean success = true;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File child : files) {
                success = success && deleteRecursively(child);
            }
        }
        return success;
    }

    /**
     * Copies bytes from input stream to output stream.
     *
     * @param inputStream  the input stream.
     * @param outputStream the output stream.
     * @throws IOException if I/O error occurred.
     */
    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024];
        for (int length; (length = inputStream.read(buffer)) >= 0; outputStream.write(buffer, 0, length)) ;
    }

    /**
     * Reads bytes from input stream.
     *
     * @param inputStream the input stream.
     * @return the content of the stream.
     * @throws IOException if I/O error occurred.
     */
    public static byte[] readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        copy(inputStream, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

}
