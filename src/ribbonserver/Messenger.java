/*
 * This code is distributed under terms of GNU GPLv2.
 * *See LICENSE file.
 * ©UKRINFORM 2011-2012
 */

package ribbonserver;

/**
 * Messages store, indexing, search and manipulation class
 * @author Stanislav Nepochatov
 */
public final class Messenger {
    
    private static String LOG_ID = "Індексатор повідомлень";
    
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
            String[] currTags = currEntry.TAGS;
            for (Integer tag_Index = 0; tag_Index < currTags.length; tag_Index++) {
                tagMessage(currTags[tag_Index], currEntry.INDEX);
            }
        }
        RibbonServer.logAppend(LOG_ID, 3, "база повідомлень завантажена (" + messageIndex.size() + ")");
    }
    
    /**
     * Find out if there is tag with given name
     * @param tagName name of the tag
     * @return tagEntry or null
     */
    private static MessageClasses.TagEntry isTagExist(String tagName) {
        java.util.ListIterator<MessageClasses.TagEntry> tagIter = Messenger.tagIndex.listIterator();
        while (tagIter.hasNext()) {
            MessageClasses.TagEntry currTag = tagIter.next();
            if (currTag.NAME.equals(tagName)) {
                return currTag;
            }
        }
        return null;
    }
    
    /**
     * Add to tag index or create new tag
     * @param tagName name of the tag
     * @param index index of message which contain this tag
     */
    public static void tagMessage(String tagName, String index) {
        MessageClasses.TagEntry namedTag = Messenger.isTagExist(tagName);
        if (namedTag == null) {
            namedTag = new MessageClasses.TagEntry(tagName);
            namedTag.INDEXES.add(index);
            Messenger.tagIndex.add(namedTag);
        } else {
            namedTag.INDEXES.add(index);
        }
    }
    
    /**
     * Get index for new message
     * @return string expresion of new index
     */
    private static String getNewIndex() {
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
        Messenger.messageIndex.add(givenMessage.returnEntry());
        String[] currTags = givenMessage.TAGS;
        for (Integer tag_Index = 0; tag_Index < currTags.length; tag_Index++) {
            tagMessage(currTags[tag_Index], givenMessage.INDEX);
        }
    }
    
    /**
     * <b>[RIBBON a1]</b><br>
     * Return tags in csv line form
     * @return all tags as csv line
     */
    public static String PROC_GET_TAGS() {
        String returned = "";
        java.util.ListIterator<MessageClasses.TagEntry> tagIter = Messenger.tagIndex.listIterator();
        while (tagIter.hasNext()) {
            returned += "RIBBON_UCTL_LOAD_TAG:" + tagIter.next().toCsv() + "\n";
        }
        return returned + "END:";
    }
    
    /**
     * <b>[RIBBON a1]</b><br>
     * Return messages beginning form specified index.
     * @return messages on csv form;
     */
    public static String PROC_LOAD_BASE_FROM_INDEX(String givenIndex) {
        String returned = "";
        if (Integer.parseInt(givenIndex) > Messenger.newIndex) {
            return "END:";
        } else {
            java.util.ListIterator<MessageClasses.MessageEntry> messageIter = Messenger.messageIndex.listIterator(Integer.parseInt(givenIndex));
            while (messageIter.hasNext()) {
                returned += "RIBBON_UCTL_LOAD_INDEX:" + messageIter.next().toCsv() + "\n";
            }
        }
        return returned + "END:";
    }
    
    /**
     * Get message entry object by index or null if message is absent.
     * @param givenIndex index of message for search
     * @return message entry object or null
     */
    public static MessageClasses.MessageEntry getMessageEntryByIndex(String givenIndex) {
        java.util.ListIterator<MessageClasses.MessageEntry> messageIter = Messenger.messageIndex.listIterator();
        while (messageIter.hasNext()) {
            MessageClasses.MessageEntry currEntry = messageIter.next();
            if (currEntry.INDEX.equals(givenIndex)) {
                return currEntry;
            }
        }
        return null;
    }
    
    /**
     * Delete messege entry from messenger index and check tags
     * @param givenEntry entry to delete
     */
    public static void deleteMessageEntryFromIndex(MessageClasses.MessageEntry givenEntry) {
        Messenger.messageIndex.remove(givenEntry);
        for (Integer removeTagIndex = 0; removeTagIndex < givenEntry.TAGS.length; removeTagIndex++) {
            MessageClasses.TagEntry currTag = Messenger.isTagExist(givenEntry.TAGS[removeTagIndex]);
            if (currTag.INDEXES.size() == 1 && (currTag.INDEXES.get(0).equals(givenEntry.INDEX))) {
                Messenger.tagIndex.remove(currTag);
            } else {
                currTag.INDEXES.remove(givenEntry.INDEX);
            }
        }
        IndexReader.updateBaseIndex();
    }
}
