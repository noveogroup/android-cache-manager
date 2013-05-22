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

import android.os.SystemClock;
import android.util.Log;
import com.noveogroup.android.cache.io.KeyManager;
import com.noveogroup.android.cache.io.Serializer;
import com.noveogroup.android.cache.util.AbstractBackgroundCleaner;
import com.noveogroup.android.cache.util.CleanerHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Disk Cache Core.
 */
public class DiskCacheCore<K> {

    /**
     * The log tag for Android Disk Cache.
     */
    public static final String TAG = "NoveoDiskCache";

    private static final String SUBDIRECTORY_META_DATA = "meta-data";
    private static final String SUBDIRECTORY_STORAGE = "storage";

    private static final long DEBUG_TIMEOUT = 250;
    private static final float DEBUG_CLEAN_PROBABILITY = 0.1f;

    /**
     * Default value of clean time delay.
     */
    public static final long DEFAULT_CLEAN_TIME_DELAY = 10 * 60 * 1000;
    /**
     * Default value of clean modification count.
     */
    public static final long DEFAULT_CLEAN_MODIFICATION_COUNT = 1000;
    /**
     * Default value of max age.
     */
    public static final long DEFAULT_MAX_AGE = 7 * 24 * 60 * 60 * 1000L;
    /**
     * Default value of max size.
     */
    public static final long DEFAULT_MAX_SIZE = 10 * 1024 * 1024L;
    /**
     * Default value of expiration time.
     */
    public static final long DEFAULT_EXPIRATION_TIME = 12 * 60 * 60 * 1000L;

    private class FileItem extends CleanerHelper.Item<File> {

        public FileItem(File file) {
            super(file,
                    System.currentTimeMillis() - file.lastModified(),
                    Utils.calculateSize(file));
        }

        @Override
        public void delete() {
            Utils.delete(source);
        }

        @Override
        public boolean canDelete() {
            return age < 0 || age > getExpirationTime();
        }

        @Override
        public boolean shouldDelete() {
            return getMaxAge() >= 0 && age > getMaxAge();
        }

    }

    private class EntryItem extends CleanerHelper.Item<Entry<K>> {

        public EntryItem(Entry<K> entry) {
            super(entry,
                    System.currentTimeMillis() - entry.getAccessTime(),
                    Utils.calculateSize(entry.entryFile) + Utils.calculateSize(entry.getFile()));
        }

        @Override
        public void delete() {
            try {
                source.remove();
            } catch (IOException e) {
                Log.v(DiskCacheCore.TAG, "cannot delete entry", e);
            }
        }

        @Override
        public boolean canDelete() {
            return age < 0 || age > getExpirationTime();
        }

        @Override
        public boolean shouldDelete() {
            return getMaxAge() >= 0 && age > getMaxAge();
        }

    }

    private class Cleaner extends AbstractBackgroundCleaner {

        private List<File> getProtectedFileList(List<EntryItem> entryList) {
            List<File> protectedFileList = new ArrayList<File>(entryList.size());
            for (EntryItem item : entryList) {
                File file = item.source().getFile();
                if (file != null) {
                    protectedFileList.add(file);
                }
            }
            return protectedFileList;
        }

        @Override
        protected void cleanCache() {
            long time = SystemClock.uptimeMillis();
            Log.v(TAG, "clean cache ...");

            // get entry files list
            List<File> entryFileList = Utils.listFiles(metaDataDirectory, true);
            List<EntryItem> expiredEntryFileList = new ArrayList<EntryItem>(entryFileList.size());
            List<EntryItem> protectedEntryFileList = new ArrayList<EntryItem>(entryFileList.size());
            CleanerHelper.entities(new CleanerHelper.Loader<EntryItem, File>() {
                @Override
                public EntryItem load(File source) {
                    try {
                        return new EntryItem(new Entry<K>(DiskCacheCore.this, source));
                    } catch (IOException e) {
                        Log.v(DiskCacheCore.TAG, "cannot load an entry", e);
                        Utils.delete(source);
                        return null;
                    }
                }
            }, entryFileList, expiredEntryFileList, protectedEntryFileList);

            // get protected files
            List<File> protectedFileList = new ArrayList<File>();
            protectedFileList.addAll(getProtectedFileList(expiredEntryFileList));
            protectedFileList.addAll(getProtectedFileList(protectedEntryFileList));

            // get temp files list
            List<File> tempFileList = Utils.listFiles(storageDirectory, true);
            tempFileList.removeAll(protectedFileList);
            List<FileItem> expiredFileItemList = new ArrayList<FileItem>(tempFileList.size());
            List<FileItem> protectedFileItemList = new ArrayList<FileItem>(tempFileList.size());
            CleanerHelper.entities(new CleanerHelper.Loader<FileItem, File>() {
                @Override
                public FileItem load(File source) {
                    return new FileItem(source);
                }
            }, tempFileList, expiredFileItemList, protectedFileItemList);

            // merge lists
            List<CleanerHelper.Item> expiredList = new ArrayList<CleanerHelper.Item>();
            expiredList.addAll(expiredEntryFileList);
            expiredList.addAll(expiredFileItemList);
            List<CleanerHelper.Item> protectedList = new ArrayList<CleanerHelper.Item>();
            protectedList.addAll(protectedEntryFileList);
            protectedList.addAll(protectedFileItemList);

            // clean
            CleanerHelper.clean(expiredList, protectedList, getMaxSize());

            time = SystemClock.uptimeMillis() - time;
            Log.v(TAG, String.format("done [clean cache] %.3f sec", time / 1000.f));
        }

    }

    private final File cacheDirectory;
    private final File metaDataDirectory;
    private final File storageDirectory;

    private final KeyManager<K> keyManager;
    private final Serializer<K> serializer;

    private volatile long debugTimeout;
    private volatile float debugCleanProbability;

    private final PropertyManager propertyManager;
    private final Cleaner cleaner = new Cleaner();

    /**
     * Creates new disk cache core instance.
     *
     * @param debugMode      true if debug mode is on.
     * @param cacheDirectory the cache directory.
     * @param keyManager     a key manager.
     * @param serializer     a key serializer.
     */
    public DiskCacheCore(boolean debugMode, File cacheDirectory, KeyManager<K> keyManager, Serializer<K> serializer) {
        this.cacheDirectory = cacheDirectory;
        this.keyManager = keyManager;
        this.serializer = serializer;
        this.metaDataDirectory = new File(cacheDirectory, SUBDIRECTORY_META_DATA);
        this.storageDirectory = new File(cacheDirectory, SUBDIRECTORY_STORAGE);
        this.debugTimeout = debugMode ? DEBUG_TIMEOUT : 0;
        this.debugCleanProbability = debugMode ? DEBUG_CLEAN_PROBABILITY : 0.f;
        this.propertyManager = new PropertyManager();
    }

    /**
     * Returns an additional debug timeout for I/O operations.
     *
     * @return the timeout.
     */
    public long getDebugTimeout() {
        return debugTimeout;
    }

    /**
     * Sets an additional debug timeout for I/O operations.
     *
     * @param debugTimeout new timeout.
     */
    public void setDebugTimeout(long debugTimeout) {
        this.debugTimeout = debugTimeout;
    }

    /**
     * Returns a probability of unexpected debug cleaning.
     *
     * @return the probability.
     */
    public float getDebugCleanProbability() {
        return debugCleanProbability;
    }

    /**
     * Sets a probability of unexpected debug cleaning.
     *
     * @param debugCleanProbability new probability.
     */
    public void setDebugCleanProbability(float debugCleanProbability) {
        if (debugCleanProbability < 0.f || debugCleanProbability > 1.f) {
            throw new IllegalArgumentException("probability value should be in [0.0; 1.0]");
        }

        this.debugCleanProbability = debugCleanProbability;
    }

    /**
     * Returns a root cache directory.
     * <p><b>Attention</b>: It is not a directory where the cache
     * stores its files.</p>
     *
     * @return the cache directory.
     * @see #getStorageDirectory()
     */
    public File getCacheDirectory() {
        return cacheDirectory;
    }

    /**
     * Returns the cache storage directory. This directory contains files
     * stored in the cache. Users can create and modify the content of this
     * directory at their own risk.
     *
     * @return the directory of the cache storage.
     */
    public File getStorageDirectory() {
        return storageDirectory;
    }

    /**
     * Returns clean time delay.
     *
     * @return the clean time delay.
     */
    public long getCleanTimeDelay() {
        return propertyManager.getCleanTimeDelay(DEFAULT_CLEAN_TIME_DELAY);
    }

    /**
     * Sets clean time delay.
     * <p/>
     * Cleaning will be performed one time in the each period set by this value.
     *
     * @param cleanTimeDelay new value of clean time delay.
     * @see AbstractBackgroundCleaner
     */
    public void setCleanTimeDelay(long cleanTimeDelay) {
        propertyManager.setCleanTimeDelay(cleanTimeDelay);
    }

    /**
     * Returns clean modification count.
     *
     * @return the clean modification count.
     */
    public long getCleanModificationCount() {
        return propertyManager.getCleanModificationCount(DEFAULT_CLEAN_MODIFICATION_COUNT);
    }

    /**
     * Sets clean modification count.
     * <p/>
     * Cleaning will be performed after a number of modification set by this value.
     *
     * @param cleanAccessCount new value of clean modification count.
     * @see AbstractBackgroundCleaner
     */
    public void setCleanModificationCount(long cleanAccessCount) {
        propertyManager.setCleanModificationCount(cleanAccessCount);
    }

    /**
     * Returns max age.
     *
     * @return the max age.
     */
    public long getMaxAge() {
        return propertyManager.getMaxAge(DEFAULT_MAX_AGE);
    }

    /**
     * Sets max age.
     * <p/>
     * This parameter manages cleaning process.
     * All files older than max age will be deleted during cleaning.
     * Negative value means cache should delete file just after it expires.
     *
     * @param maxAge new value of max age.
     * @see CleanerHelper
     */
    public void setMaxAge(long maxAge) {
        propertyManager.setMaxAge(maxAge);
    }

    /**
     * Returns max size of cache.
     *
     * @return the max size.
     */
    public long getMaxSize() {
        return propertyManager.getMaxSize(DEFAULT_MAX_SIZE);
    }

    /**
     * Sets max size.
     * <p/>
     * This parameter manages cleaning process.
     * When size of cache directory grows more than this value
     * cleaning process goes to its second phase. In this phase
     * it will sort cache entries by age and will delete them
     * in LRU-order. In other words this parameter manages
     * the cleaning process to start the second phase.
     * <p><b>Attention</b>: Actual size of the cache can be
     * larger than maximum. Some cleaning parameters can
     * set "protection" from deleting for some entries.</p>
     * Negative value means cache should delete file just after it expires.
     *
     * @param maxSize new value of max size.
     * @see CleanerHelper
     */
    public void setMaxSize(long maxSize) {
        propertyManager.setMaxSize(maxSize);
    }

    /**
     * Returns expiration time.
     *
     * @return the expiration time.
     */
    public long getExpirationTime() {
        return propertyManager.getExpirationTime(DEFAULT_EXPIRATION_TIME);
    }

    /**
     * Sets expiration time.
     * <p/>
     * This parameter protects the entries
     * from being deleted during cleaning. The entries younger than
     * value set by this parameter will be excepted from the list
     * of entries can-be-deleted at the second phase of cleaning.
     * <p><b>Attention</b>: This parameter cannot be used to prohibit
     * deleting because its protection is some "advice" to cleaning
     * algorithm.</p>
     * Negative value means any file can be deleted just after creation.
     *
     * @param expirationTime new value of expiration time.
     * @see CleanerHelper
     */
    public void setExpirationTime(long expirationTime) {
        propertyManager.setExpirationTime(expirationTime);
    }

    /**
     * Totally erases the cache. This method deletes a content of
     * cache directory and get the cache to its initial state.
     *
     * @see #clean()
     */
    public void erase() {
        Utils.deleteContent(cacheDirectory);
    }

    /**
     * Forces a start of cleaning process. Cleaning will be done
     * in a background thread - so this method is not blocking.
     *
     * @see #erase()
     */
    public void clean() {
        cleaner.clean();
    }

    /**
     * Returns a size of the cache.
     *
     * @return the size of the cache.
     */
    public long size() {
        cleaner.access(false, getCleanTimeDelay(), getCleanModificationCount());
        return Utils.calculateSize(cacheDirectory);
    }

    /**
     * Creates new temp file into the cache storage.
     *
     * @return the temp file.
     * @throws IOException if I/O error occurred.
     */
    public File createFile() throws IOException {
        return createFile(false, "", "");
    }

    /**
     * Creates new directory file into the cache storage.
     *
     * @return the temp directory.
     * @throws IOException if I/O error occurred.
     */
    public File createDirectory() throws IOException {
        return createFile(true, "", "");
    }

    /**
     * Creates new temp file into the cache storage.
     *
     * @param prefix a desired prefix of name.
     * @param suffix a desired suffix of name.
     * @return the temp file.
     * @throws IOException if I/O error occurred.
     */
    public File createFile(String prefix, String suffix) throws IOException {
        return createFile(false, prefix, suffix);
    }

    /**
     * Creates new temp directory into the cache storage.
     *
     * @param prefix a desired prefix of name.
     * @param suffix a desired suffix of name.
     * @return the temp directory.
     * @throws IOException if I/O error occurred.
     */
    public File createDirectory(String prefix, String suffix) throws IOException {
        return createFile(true, prefix, suffix);
    }

    /**
     * Creates new temp file into the cache storage.
     *
     * @param directory true if a directory should be created, false otherwise.
     * @param prefix    a desired prefix of name.
     * @param suffix    a desired suffix of name.
     * @return the temp file or directory.
     * @throws IOException if I/O error occurred.
     */
    public File createFile(boolean directory, String prefix, String suffix) throws IOException {
        cleaner.access(true, getCleanTimeDelay(), getCleanModificationCount());
        return Utils.createTempFile(directory, prefix, suffix, storageDirectory);
    }

    /**
     * Checks if specified file is contained in the cache storage.
     *
     * @param file the file.
     * @return true if and only if the cache contains the specified file.
     * @throws NullPointerException if the file is null.
     */
    public boolean containsFile(File file) {
        cleaner.access(false, getCleanTimeDelay(), getCleanModificationCount());

        if (file.getParentFile().equals(storageDirectory)) {
            return false;
        }

        boolean exists = file.exists();

        // cause force cleaning
        if (Math.random() < debugCleanProbability) {
            Log.d(DiskCache.TAG, "clean simulation. force delete file: " + file);
            file.delete();
        }

        return exists;
    }

    /**
     * Updates last modification time of specified file in the cache storage.
     *
     * @param file the file.
     * @throws IOException              if I/O error occurred.
     * @throws NullPointerException     if the file is null.
     * @throws IllegalArgumentException if the cache doesn't own the file.
     */
    public void touchFile(File file) throws IOException {
        if (file.getParentFile().equals(storageDirectory)) {
            throw new IllegalArgumentException("cache doesn't own file " + file);
        }

        cleaner.access(true, getCleanTimeDelay(), getCleanModificationCount()); // user possible has changed the content of the file
        if (!file.setLastModified(System.currentTimeMillis())) {
            throw new IOException("cannot touch file " + file);
        }

        // cause force cleaning
        if (Math.random() < debugCleanProbability) {
            Log.d(DiskCache.TAG, "clean simulation. force delete file: " + file);
            file.delete();
        }
    }

    /**
     * Removes specified file from cache storage.
     *
     * @param file the file.
     * @throws IOException              if I/O error occurred.
     * @throws NullPointerException     if the file is null.
     * @throws IllegalArgumentException if the cache doesn't own the file.
     */
    public void removeFile(File file) throws IOException {
        if (file.getParentFile().equals(storageDirectory)) {
            throw new IllegalArgumentException("cache doesn't own file " + file);
        }

        cleaner.access(true, getCleanTimeDelay(), getCleanModificationCount());

        if (!Utils.delete(file)) {
            throw new IOException("cannot remove file " + file);
        }
    }

    /**
     * Cache entry.
     *
     * @param <K> a type of keys.
     */
    public static class Entry<K> extends DiskCacheEntry<K> {

        private final DiskCacheCore<K> owner;
        private File entryFile;

        private Entry(DiskCacheCore<K> owner, K key) {
            this.owner = owner;
            this.entryFile = null;
            setKey(key);
            setCreateTime(System.currentTimeMillis());
            setAccessTime(System.currentTimeMillis());
            setMetaData(new MetaData());
            setFile(null);
        }

        private Entry(DiskCacheCore<K> owner, File entryFile) throws IOException {
            this.owner = owner;
            this.entryFile = entryFile;
            loadEntry(entryFile, owner.serializer);

            // cause force cleaning
            if (Math.random() < owner.debugCleanProbability) {
                Log.d(DiskCache.TAG, "clean simulation. force delete file: " + entryFile);
                entryFile.delete();
            }

            // wait for the additional debug timeout
            SystemClock.sleep(owner.debugTimeout);
        }

        /**
         * Sets access time to now.
         */
        public void access() {
            setAccessTime(System.currentTimeMillis());
        }

        /**
         * Checks if the entry exists.
         *
         * @return true if and only if this entry has been loaded or
         *         committed and it hasn't been deleted yet.
         */
        public boolean exists() {
            return entryFile != null && entryFile.exists();
        }

        /**
         * Removes entry if it exists.
         *
         * @throws IOException if I/O error occurred.
         */
        public void remove() throws IOException {
            owner.cleaner.access(true, owner.getCleanTimeDelay(), owner.getCleanModificationCount());

            if (entryFile != null) {
                boolean success = Utils.delete(entryFile);
                if (!success) {
                    if (entryFile.exists()) {
                        throw new IOException("cannot remove entry file: " + entryFile);
                    }
                }
                entryFile = null;
            }
        }

        /**
         * Saves the entry to new entry file or updates the original one.
         *
         * @throws IOException if I/O error occurred.
         */
        public void commit() throws IOException {
            owner.cleaner.access(true, owner.getCleanTimeDelay(), owner.getCleanModificationCount());

            File tempEntryFile = null;
            File tempFile = owner.createFile("entry-", "-temp");
            try {
                // wait for the additional debug timeout
                SystemClock.sleep(owner.debugTimeout);

                // save the entry
                saveEntry(tempFile, owner.serializer);

                // get hash code directory and create it
                File hashCodeDirectory = owner.getHashCodeDirectory(getKey());
                hashCodeDirectory.mkdirs();

                // move the temp file
                if (entryFile == null) {
                    tempEntryFile = Utils.createTempFile(false, "", "", hashCodeDirectory);
                    entryFile = tempEntryFile;
                }

                if (!tempFile.renameTo(entryFile)) {
                    throw new IOException("cannot move temp file " + tempFile + " to entry storage as " + tempEntryFile);
                }

                // temporary files is not temporary now
                tempFile = null;
                tempEntryFile = null;

                // cause force cleaning
                if (Math.random() < owner.debugCleanProbability) {
                    Log.d(DiskCache.TAG, "clean simulation. force delete file: " + entryFile);
                    entryFile.delete();
                }
            } finally {
                if (tempFile != null) {
                    Utils.delete(tempFile); // ignore possible errors
                }
                if (tempEntryFile != null) {
                    Utils.delete(tempEntryFile); // ignore possible errors
                }
            }
        }

    }

    private File getHashCodeDirectory(K key) {
        int hashCode = keyManager.hashCode(key);
        return new File(metaDataDirectory, String.format("%08X", hashCode));
    }

    /**
     * Creates new cache entry with the specified key.
     *
     * @param key a type of the key.
     * @return the cache entry.
     */
    public Entry<K> create(K key) {
        cleaner.access(false, getCleanTimeDelay(), getCleanModificationCount());
        return new Entry<K>(this, key);
    }

    /**
     * Searches a cache entry using the specified key.
     * Returns null if nothing was found.
     *
     * @param key a type of the key.
     * @return the cache entry or null.
     */
    public Entry<K> search(K key) {
        cleaner.access(false, getCleanTimeDelay(), getCleanModificationCount());
        File hashCodeDirectory = getHashCodeDirectory(key);
        File[] files = hashCodeDirectory.listFiles();
        if (files != null) {
            // sort files - the result should not depend on files order on the disk
            Arrays.sort(files);
            // find a file containing an entry with the specified key
            for (File file : files) {
                Entry<K> entry;
                try {
                    entry = new Entry<K>(this, file);
                } catch (Exception e) {
                    // ignore an exception
                    continue;
                }

                if (keyManager.equals(key, entry.getKey())) {
                    return entry;
                }
            }
        }
        return null;
    }

    /**
     * Tries to find an entry by the specified key and
     * creates a new one if nothing was found.
     * This method is useful composition of {@link #create(K)}
     * and {@link #search(K)} methods.
     *
     * @param key a type of the key.
     * @return the cache entry.
     * @see #create(K)
     * @see #search(K)
     */
    public Entry<K> entry(K key) {
        Entry<K> entry = search(key);
        return entry != null ? entry : create(key);
    }

}
