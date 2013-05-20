/**
 * This file is part of RibbonServer application (check README).
 * Copyright (C) 2012-2013 Stanislav Nepochatov
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
**/

package ribbonserver;

/**
 * Messages store, indexing, search and manipulation class
 * @author Stanislav Nepochatov
 */
public final class Messenger {
    
    private static String LOG_ID = "ПОВІДОМЛЕННЯ";
    
    /**
     * Storage of message entries
     */
    public static java.util.ArrayList<MessageClasses.MessageEntry> messageIndex;
    
    /**
     * Storage of tag etries
     */
    public static java.util.ArrayList<MessageClasses.TagEntry> tagIndex;
    
    /**
     * Index for new message
     */
    private static Integer newIndex = 0;
    
    /**
     * Message index list sync lock.
     */
    private static final Object messageLock = new Object();
    
    /**
     * Tag index list sync lock.
     */
    private static final Object tagLock = new Object();
    
    /**
     * Init message index handle component
     */
    public static void init() {
        messageIndex = IndexReader.readBaseIndex();
        java.util.ListIterator<MessageClasses.MessageEntry> messageIter = messageIndex.listIterator();
        tagIndex = new java.util.ArrayList<>();
        while (messageIter.hasNext()) {
            MessageClasses.MessageEntry currEntry = messageIter.next();
            if (currEntry.INDEX != null) {
                if (Integer.parseInt(currEntry.INDEX) > newIndex) {
                    newIndex = Integer.parseInt(currEntry.INDEX);
                }
            } else {
                continue;
            }
            String[] currDirs = currEntry.DIRS;
            for (Integer dirIndex = 0; dirIndex < currDirs.length; dirIndex++) {
                Directories.addIndexToDir(currDirs[dirIndex], currEntry.INDEX);
            }
            addToTagIndex(currEntry);
            /**for (Integer tag_Index = 0; tag_Index < currTags.length; tag_Index++) {
                tagMessage(currTags[tag_Index], currEntry.INDEX);
            }**/
        }
        RibbonServer.logAppend(LOG_ID, 3, "база повідомлень завантажена (" + messageIndex.size() + ")");
    }
    
    /**
     * Find out if there is tag with given name
     * @param tagName name of the tag
     * @return tagEntry or null
     */
    private static MessageClasses.TagEntry isTagExist(String tagName) {
        synchronized (tagLock) {
            java.util.ListIterator<MessageClasses.TagEntry> tagIter = Messenger.tagIndex.listIterator();
            while (tagIter.hasNext()) {
                MessageClasses.TagEntry currTag = tagIter.next();
                if (currTag.NAME.equals(tagName)) {
                    return currTag;
                }
            }
            return null;
        }
    }
    
    /**
     * Add to tag index or create new tag.
     * @param givenEntry message entry with tags;
     */
    public static void addToTagIndex(MessageClasses.MessageEntry givenEntry) {
        synchronized (tagLock) {
            for (String currTag : givenEntry.TAGS) {
                MessageClasses.TagEntry namedTag = Messenger.isTagExist(currTag);
                if (namedTag == null) {
                    namedTag = new MessageClasses.TagEntry(currTag);
                    namedTag.INDEXES.add(givenEntry.INDEX);
                    Messenger.tagIndex.add(namedTag);
                } else {
                    namedTag.INDEXES.add(givenEntry.INDEX);
                }
            }
        }
    }
    
    /**
     * Modify tag index (may create or delete tags).
     * @param oldEntry message entry with tags to modify;
     * @param newEntry message entry with new tags;
     */
    public static void modTagIndex(MessageClasses.MessageEntry oldEntry, MessageClasses.MessageEntry newEntry) {
        removeTagIndex(oldEntry);
        addToTagIndex(newEntry);
    }
    
    /**
     * Remove message presense from tag index.
     * @param givenEntry entry with tags to remove;
     */
    public static void removeTagIndex(MessageClasses.MessageEntry givenEntry) {
        synchronized (tagLock) {
            for (String currTag : givenEntry.TAGS) {
                MessageClasses.TagEntry namedTag = Messenger.isTagExist(currTag);
                if (namedTag != null) {
                    namedTag.INDEXES.remove(givenEntry.INDEX);
                }
            }
        }
    }
    
    /**
     * Get index for new message
     * @return string expresion of new index
     */
    private static synchronized String getNewIndex() {
        String newIndexStr = String.valueOf(++newIndex);
        while (newIndexStr.length() < 10) {
            newIndexStr = "0" + newIndexStr;
        }
        return newIndexStr;
    }
    
    /**
     * Add message entry to index and update original message object
     * @param givenEntry given message entry
     */
    public static void addMessageToIndex(MessageClasses.Message givenMessage) {
        givenMessage.INDEX = Messenger.getNewIndex();
        givenMessage.DATE = RibbonServer.getCurrentDate();
        synchronized (messageLock) {
            Messenger.messageIndex.add(givenMessage.returnEntry());
        }
        addToTagIndex(givenMessage);
    }
    
    /**
     * <b>[RIBBON a1]</b><br>
     * Return tags in csv line form
     * @return all tags as csv line
     */
    public static String PROC_GET_TAGS() {
        synchronized (tagLock) {
            StringBuffer getBuf = new StringBuffer();
            java.util.ListIterator<MessageClasses.TagEntry> tagIter = Messenger.tagIndex.listIterator();
            while (tagIter.hasNext()) {
                getBuf.append("RIBBON_UCTL_LOAD_TAG:").append(tagIter.next().toCsv()).append("\n");
            }
            return getBuf.append("END:").toString();
        }
    }
    
    /**
     * <b>[RIBBON a1]</b><br>
     * Return messages beginning form specified index.
     * @return messages on csv form;
     */
    public static String PROC_LOAD_BASE_FROM_INDEX(String givenIndex) {
        StringBuffer getBuf = new StringBuffer();
        if (Integer.parseInt(givenIndex) > Messenger.newIndex) {
            return "END:";
        } else {
            synchronized (messageLock) {
                java.util.ListIterator<MessageClasses.MessageEntry> messageIter = Messenger.messageIndex.listIterator(Integer.parseInt(givenIndex));
                while (messageIter.hasNext()) {
                    getBuf.append("RIBBON_UCTL_LOAD_INDEX:").append(messageIter.next().toCsv()).append("\n");
                }
            }
        }
        return getBuf.append("END:").toString();
    }
    
    /**
     * Get message entry object by index or null if message is absent.
     * @param givenIndex index of message for search
     * @return message entry object or null
     */
    public static MessageClasses.MessageEntry getMessageEntryByIndex(String givenIndex) {
        synchronized (tagLock) {
            java.util.ListIterator<MessageClasses.MessageEntry> messageIter = Messenger.messageIndex.listIterator();
            while (messageIter.hasNext()) {
                MessageClasses.MessageEntry currEntry = messageIter.next();
                if (currEntry.INDEX.equals(givenIndex)) {
                    return currEntry;
                }
            }
        }
        return null;
    }
    
    /**
     * Delete messege entry from messenger index and check tags
     * @param givenEntry entry to delete
     */
    public static void deleteMessageEntryFromIndex(MessageClasses.MessageEntry givenEntry) {
        synchronized (messageLock) {
            Messenger.messageIndex.remove(givenEntry);
        }
        Messenger.removeTagIndex(givenEntry);
        IndexReader.updateBaseIndex();
    }
}
