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
    public static java.util.ArrayList<Messenger.messageEntry> messageIndex;
    
    /**
     * Storage of tag etries
     */
    public static java.util.ArrayList<tagEntry> tagIndex;
    
    /**
     * Index for new message
     */
    private static Integer newIndex = 0;
    
    /**
     * Init message index handle component
     */
    public static void init() {
        messageIndex = indexReader.readBaseIndex();
        java.util.ListIterator<Messenger.messageEntry> messageIter = messageIndex.listIterator();
        tagIndex = new java.util.ArrayList<tagEntry>();
        while (messageIter.hasNext()) {
            Messenger.messageEntry currEntry = messageIter.next();
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
     * Message entry class
     */
    public static class messageEntry {
        
        /**
         * Index of message
         */
        public String INDEX;
        
        /**
         * Message's directories
         */
        public String[] DIRS;
        
        /**
         * Header of message;
         */
        public String HEADER;
        
        /**
         * Date of message release;
         */
        public String DATE;
        
        /**
         * Author of message
         */
        public String AUTHOR;
        
        /**
         * Message's tags
         */
        public String[] TAGS;
        
        messageEntry() {
            
        }
        
        /**
         * Default constructor
         * @param givenArray array with main variables (INDEX, HEADER, DATE, AUTHOR);
         * @param givenDirs array with directories;
         * @param givenTags array with tags;
         */
        messageEntry(String[] givenArray, String[] givenDirs, String[] givenTags) {
            INDEX = givenArray[0];
            HEADER = givenArray[1];
            DATE = givenArray[2];
            AUTHOR = givenArray[3];
            DIRS = givenDirs;
            TAGS = givenTags;
        }
        
        /**
         * Translate message object into csv index line
         * @return csv formated string
         */
        public String toCsv() {
            String[] currDirs = this.DIRS;
            String dirMessage = "[";
            for (Integer dirIndex = 0; dirIndex < currDirs.length; dirIndex++) {
                if (dirIndex < currDirs.length - 1) {
                    dirMessage += currDirs[dirIndex] + ",";
                } else {
                    dirMessage += currDirs[dirIndex] + "]";
                }
            }
            String[] currTags = this.TAGS;
            String tagMessage = "[";
            for (Integer tagIndex = 0; tagIndex < currTags.length; tagIndex++) {
                if (tagIndex < currTags.length - 1) {
                    tagMessage += currTags[tagIndex] + ",";
                } else {
                    tagMessage += currTags[tagIndex] + "]";
                }
            }
            return this.INDEX + "," + dirMessage + ",{" + this.HEADER + "}," + this.DATE + ",{" + this.AUTHOR + "}," + tagMessage;
        }
    }
    
    /**
     * Message class
     * (extends messageEntry)
     * @see messageEntry
     */
    public static class Message extends Messenger.messageEntry {
        
        /**
         * Message content
         */
        public String CONTENT;
        
        /**
         * Default constructor (test)
         * @param givenHeader header of message
         * @param givenAuthor author of message
         * @param givenDirs directories for message
         * @param givenTags message's tags
         * @param givenContent content of message
         */
        Message(String givenHeader, String givenAuthor, String[] givenDirs, String[] givenTags, String givenContent) {
            this.HEADER = givenHeader;
            this.AUTHOR = givenAuthor;
            this.DIRS = givenDirs;
            this.TAGS = givenTags;
            this.CONTENT = givenContent;
        }
        
        /**
         * Construct message from csv line (net protocol)
         * @param parsedArray array parsed by csvHandler
         */
        Message(String[] parsedArray, String[] parsedDirs, String[] parsedTags) {
            this.DIRS = parsedDirs;
            this.TAGS = parsedTags;
            if (parsedArray.length == 1) {
                this.HEADER = parsedArray[0];
            } else {
                this.INDEX = parsedArray[0];
                this.HEADER = parsedArray[1];
            }
        }
        
        /**
         * Return message entry from Message
         * @return messageEntry object
         */
        public Messenger.messageEntry returnEntry() {
            return (Messenger.messageEntry) this;
        }
    }
    
    /**
     * Tag entry
     */
    public static class tagEntry {
        
        /**
         * Name of the tag
         */
        public String NAME;
        
        /**
         * Indexes of messages which contains this tag
         */
        public java.util.ArrayList<String> INDEXES;
        
        /**
         * Default costructor
         * @param givenName name of new created tag
         */
        tagEntry(String givenName) {
            NAME = givenName;
            INDEXES = new java.util.ArrayList<String>();
            if (RibbonServer.CURR_STATE == RibbonServer.SYS_STATES.READY) {
                RibbonServer.logAppend(LOG_ID, 3, "додано ключове слово: '" + NAME + "'");
            }
        }
        
        /**
         * Return csv form of tag
         * @return csv line with tag name and it's index
         */
        public String toCsv() {
            String returned = this.NAME + ",[";
            java.util.ListIterator<String> indexIter = this.INDEXES.listIterator();
            while (indexIter.hasNext()) {
                returned += ((indexIter.nextIndex() > 0) ? "," : "") + indexIter.next();
            }
            return returned + "]";
        }
    }
    
    /**
     * Find out if there is tag with given name
     * @param tagName name of the tag
     * @return tagEntry or null
     */
    private static tagEntry isTagExist(String tagName) {
        java.util.ListIterator<tagEntry> tagIter = Messenger.tagIndex.listIterator();
        while (tagIter.hasNext()) {
            tagEntry currTag = tagIter.next();
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
        tagEntry namedTag = Messenger.isTagExist(tagName);
        if (namedTag == null) {
            namedTag = new tagEntry(tagName);
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
    public static void addMessageToIndex(Messenger.Message givenMessage) {
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
        java.util.ListIterator<tagEntry> tagIter = Messenger.tagIndex.listIterator();
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
            java.util.ListIterator<messageEntry> messageIter = Messenger.messageIndex.listIterator(Integer.parseInt(givenIndex));
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
    public static Messenger.messageEntry getMessageEntryByIndex(String givenIndex) {
        java.util.ListIterator<Messenger.messageEntry> messageIter = Messenger.messageIndex.listIterator();
        while (messageIter.hasNext()) {
            Messenger.messageEntry currEntry = messageIter.next();
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
    public static void deleteMessageEntryFromIndex(Messenger.messageEntry givenEntry) {
        Messenger.messageIndex.remove(givenEntry);
        for (Integer removeTagIndex = 0; removeTagIndex < givenEntry.TAGS.length; removeTagIndex++) {
            Messenger.tagEntry currTag = Messenger.isTagExist(givenEntry.TAGS[removeTagIndex]);
            if (currTag.INDEXES.size() == 1 && (currTag.INDEXES.get(0).equals(givenEntry.INDEX))) {
                Messenger.tagIndex.remove(currTag);
            } else {
                currTag.INDEXES.remove(givenEntry.INDEX);
            }
        }
        Thread delayedUpdate = new Thread() {
            @Override
            public void run() {
                indexReader.updateBaseIndex(messageIndex);
            }
        };
        delayedUpdate.start();
    }
}
