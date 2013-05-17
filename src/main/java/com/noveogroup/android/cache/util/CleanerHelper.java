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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class CleanerHelper {

    private CleanerHelper() {
        throw new UnsupportedOperationException();
    }

    /**
     * The class represents an item that can be cleaned.
     */
    public static abstract class Item<Source> {

        /**
         * The source of the item.
         */
        protected final Source source;

        /**
         * The age of the item.
         */
        protected final long age;

        /**
         * The size of the item.
         */
        protected final long size;

        /**
         * Creates new cleanable item.
         *
         * @param source the source of the item.
         * @param age    the age.
         * @param size   the size.
         */
        public Item(Source source, long age, long size) {
            this.source = source;
            this.age = age;
            this.size = size;
        }

        /**
         * Returns the source of the item.
         *
         * @return the source of the item.
         */
        public Source source() {
            return source;
        }

        /**
         * Returns the age of the item.
         *
         * @return the age of the item.
         */
        public long age() {
            return age;
        }

        /**
         * Returns the size of the item.
         *
         * @return the size of the item.
         */
        public long size() {
            return size;
        }

        /**
         * Deletes the item.
         */
        public abstract void delete();


        /**
         * Checks if the item CAN be deleted.
         *
         * @return true if the item CAN be deleted.
         */
        public abstract boolean canDelete();

        /**
         * Checks if the item SHOULD be deleted.
         *
         * @return true if the item SHOULD be deleted.
         */
        public abstract boolean shouldDelete();

    }

    /**
     * Loads items from sources.
     *
     * @param <S> a type of the items.
     */
    public static interface Loader<I extends Item, S> {

        /**
         * Creates an item from the file.
         *
         * @param source the source.
         * @return the item.
         */
        public I load(S source);

    }

    /**
     * Loads items from the file list using the specified loader.
     * Deletes items that CAN and SHOULD be deleted.
     * Returns two lists of expired (CAN but SHOULD NOT be deleted)
     * and protected (CANNOT be deleted) items.
     *
     * @param loader        the loader.
     * @param sources       the sources list.
     * @param expiredList   a list of expired items.
     * @param protectedList a list of protected items.
     */
    public static <I extends Item, S> void entities(Loader<I, S> loader, List<S> sources,
                                                    List<I> expiredList, List<I> protectedList) {
        for (S source : sources) {
            Thread.yield();
            I item = loader.load(source);
            if (item != null) {
                if (item.canDelete()) {
                    if (item.shouldDelete()) {
                        item.delete();
                    } else {
                        expiredList.add(item);
                    }
                } else {
                    protectedList.add(item);
                }
            }
        }
    }

    private static <I extends Item> long getSize(List<I> list) {
        long size = 0;
        for (I item : list) {
            Thread.yield();
            size += item.size();
        }
        return size;
    }

    private static <I extends Item> void cleanList(List<I> list, long sizeToDelete) {
        Collections.sort(list, new Comparator<Item>() {
            @Override
            public int compare(Item item1, Item item2) {
                Thread.yield();
                return new Long(item2.age()).compareTo(item1.age());
            }
        });

        long deletedSize = 0;
        for (I item : list) {
            Thread.yield();
            if (deletedSize >= sizeToDelete) {
                break;
            } else {
                deletedSize += item.size();
                item.delete();
            }
        }
    }

    /**
     * Cleans items to make their total size less (or as close as it can be done) than the specified maximum.
     *
     * @param expiredList   an expired list of the items.
     * @param protectedList a protected list of the items.
     * @param maxSize       the max size.
     */
    public static <I extends Item> void clean(List<I> expiredList, List<I> protectedList, long maxSize) {
        // negative value means no restrictions
        if (maxSize < 0) {
            return;
        }

        // calculate size
        long expiredSize = getSize(expiredList);
        long protectedSize = getSize(protectedList);
        long size = expiredSize + protectedSize;

        if (size > maxSize) {
            // clean list of expired items
            cleanList(expiredList, size - maxSize);

            if (protectedSize > maxSize) {
                // clean a half of list of protected items
                List<I> half = protectedList.subList(0, protectedList.size() / 2);
                cleanList(half, (protectedSize - maxSize) / 2);
            }
        }
    }

}
