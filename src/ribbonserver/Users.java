/*
 * This code is distributed under terms of GNU GPLv2.
 * *See LICENSE file.
 * ©UKRINFORM 2011-2012
 */

package ribbonserver;

/**
 * User accounting and authentication class
 * @author Stanislav Nepochatov
 */
public class Users {
    
    Users() {
        userStore = csvHandler.readUsers();
        RibbonServer.logAppend(LOG_ID, 3, "індекс користувачів опрацьвано (" + userStore.size() + ")");
    }
    
    private String LOG_ID = "Обробник користувачів";
    
    /**
     * User's store array
     */
    private java.util.ArrayList<userEntry> userStore;
    
    /**
     * User entry class
     */
    public static class userEntry {
        
        /**
         * a1 endian constructor
         * @param givenArray array with login and password
         * @deprecated 
         */
        userEntry(String[] givenArray) {
            USER_NAME = givenArray[0];
            PASSWORD = givenArray[1];
        }
        
        /**
         * a2 endian constructor
         * @param givenStruct result of csvHandler.complexParseLine
         * @since RibbonServer a2
         */
        userEntry(java.util.ArrayList<String[]> givenStruct) {
            String[] baseArray = givenStruct.get(0);
            String[] groupsArray = givenStruct.get(1);
            USER_NAME = baseArray[0];
            COMM = baseArray[1];
            H_PASSWORD = baseArray[2];
            IS_ENABLED = baseArray[3].equals("1") ? true : false;
            GROUPS = groupsArray;
            for (Integer groupIndex = 0; groupIndex < GROUPS.length; groupIndex++) {
                if (!RibbonServer.groupObj.isGroupExisted(GROUPS[groupIndex])) {
                    GROUPS[groupIndex] = null;
                }
            }
        }
        
        /**
         * User name
         */
        public String USER_NAME;
        
        /**
         * User's password
         * @deprecated plain password is unsafe.
         */
        public String PASSWORD;
        
        /** a2 endian **/
        
        /**
         * MD5 hashsumm of password
         * @since RibbonServer a2
         */
        public String H_PASSWORD;
        
        /**
         * Comment string
         * @since RibbonServer a2
         */
        public String COMM;
        
        /**
         * Array with groups
         * @since RibbonServer a2
         */
        public String[] GROUPS;
        
        /**
         * State of users account
         * @since RibbonServer a2
         */
        public Boolean IS_ENABLED;
        
        /**
         * Translate user object into csv index line
         * @return csv formated string
         */
        public String toCsv() {
            return "{" + this.USER_NAME + "},{" + this.PASSWORD + "}";
        }
    }
    
    /**
     * Check if given user existed it the system
     * @param givenName name of user
     * @param givenDir name of dir to post
     * @return true - if user existed/false - if not
     */
    public Boolean checkUser(String givenName, String givenDir) {
        java.util.ListIterator<userEntry> userIter = this.userStore.listIterator();
        while (userIter.hasNext()) {
            if (userIter.next().USER_NAME.equals(givenName)) {
                return true;
            }
        }
        if (RibbonServer.CURR_ANON_MODE == RibbonServer.ANON_MODES.OVERRIDE) {
            return true;
        } else if (RibbonServer.CURR_ANON_MODE == RibbonServer.ANON_MODES.NORMAL) {
            return RibbonServer.dirObj.getAnonMode(givenDir);
        }
        return false;
    }
    
    /**
     * Login user or return error 
     * @param tryUser user entry of user which will be examened
     * @return null or error message
     * @deprecated a1 specific
     */
    public String loginUser(Users.userEntry tryUser) {
        Users.userEntry findedUser = null;
        java.util.ListIterator<Users.userEntry> usersIter = this.userStore.listIterator();
        while (usersIter.hasNext()) {
            Users.userEntry currUser = usersIter.next();
            if (currUser.USER_NAME.equals(tryUser.USER_NAME)) {
                findedUser = currUser;
                break;
            }
        }
        if (findedUser != null) {
            if (findedUser.PASSWORD.equals(tryUser.PASSWORD)) {
                if (findedUser.PASSWORD.charAt(0) == '*') {
                    return "Користувача заблоковано!";
                } else {
                    return null;
                }
            } else {
                return "Невірний пароль!";
            }
        } else {
            return "Користувача не знайдено!";
        }
    }
    
    /**
     * Login user or return error 
     * @param givenName name of user which is trying to login
     * @param givenHash md5 hash of user's password
     * @return null or error message
     * @since RibbonServer a2
     */
    public String PROC_LOGIN_USER(String givenName, String givenHash) {
        Users.userEntry findedUser = null;
        java.util.ListIterator<Users.userEntry> usersIter = this.userStore.listIterator();
        while (usersIter.hasNext()) {
            Users.userEntry currUser = usersIter.next();
            if (currUser.USER_NAME.equals(givenName)) {
                findedUser = currUser;
                break;
            }
        }
        if (findedUser != null) {
            if (findedUser.H_PASSWORD.equals(givenHash)) {
                if (!findedUser.IS_ENABLED) {
                    return "Користувача заблоковано!";
                } else {
                    return null;
                }
            } else {
                return "Невірний пароль!";
            }
        } else {
            return "Користувача не знайдено!";
        }
    }
}
