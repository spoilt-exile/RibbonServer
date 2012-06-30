/*
 * This code is distributed under terms of GNU GPLv2.
 * *See LICENSE file.
 * ©UKRINFORM 2011-2012
 */

package ribbonserver;

/**
 * Groups accounting class
 * @author Stanislav Nepochatov
 */
public class Groups {
    
    Groups() {
        groupStore = csvHandler.readGroups();
        groupStore.add(new Groups.groupEntry(new String[] {"ADM", "SYS: Administration group"}));
        RibbonServer.logAppend(LOG_ID, 3, "індекс груп опрацьвано (" + groupStore.size() + ")");
    }
    
    private String LOG_ID = "Обробник груп";
    
    /**
     * Group's store array
     */
    private java.util.ArrayList<Groups.groupEntry> groupStore = new java.util.ArrayList();
    
    /**
     * Group entry class
     */
    public static class groupEntry {
        
        /**
         * Name of the group
         */
        public String GROUP_NAME;
        
        /**
         * Group commentary
         */
        public String COMM;
        
        groupEntry(String[] givenArray) {
            GROUP_NAME = givenArray[0];
            COMM = givenArray[1];
        }
    }
    
    /**
     * Find out if there is group with given name
     * @param givenGroupName given name to search
     * @return true if group existed/false if not
     */
    public Boolean isGroupExisted(String givenGroupName) {
        java.util.ListIterator<Groups.groupEntry> groupIter = this.groupStore.listIterator();
        while (groupIter.hasNext()) {
            if (groupIter.next().GROUP_NAME.equals(givenGroupName)) {
                return true;
            }
        }
        return false;
    }
}
