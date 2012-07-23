/*
 * This code is distributed under terms of GNU GPLv2.
 * *See LICENSE file.
 * ©UKRINFORM 2011-2012
 */

package ribbonserver;

import java.util.Arrays;

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
     * @deprecated old code
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
     * Check access to directory with specified mode;<br>
     * <br>
     * <b>Modes:</b><br>
     * 0 - attempt to read directory;<br>
     * 1 - attempt to release messege in directory;<br>
     * 2 - attempt to admin directory;
     * @param givenName user name which attempt to perform some action
     * @param givenDir directory path 
     * @param givenMode mode of action (read, write or admin)
     * @return result of access checking
     * @since RibbonServer a2
     */
    public Boolean checkAccess(String givenName, String givenDir, Integer givenMode) {
        java.util.ListIterator<userEntry> userIter = this.userStore.listIterator();
        userEntry findedUser = null;
        while (userIter.hasNext()) {
            userEntry currUser = userIter.next();
            if (currUser.USER_NAME.equals(givenName)) {
                findedUser = currUser;
                break;
            }
        }
        String[] keyArray = Arrays.copyOf(findedUser.GROUPS, findedUser.GROUPS.length + 1);
        keyArray[keyArray.length - 1] = findedUser.USER_NAME;
        Boolean findedAnswer = false;
        Directories.dirPermissionEntry fallbackPermission = null;
        Directories.dirPermissionEntry[] dirAccessArray = RibbonServer.dirObj.getDirAccess(givenDir);
        //for (Integer dirIndex = 0; dirIndex < dirAccessArray.length; dirIndex++) {
        for (Integer keyIndex = 0; keyIndex < keyArray.length; keyIndex++) {
            for (Integer dirIndex = 0; dirIndex < dirAccessArray.length; dirIndex++) {
                if (keyArray[keyIndex].equals("ADM")) {
                    return true;    //ADM is root-like group, all permission will be ignored
                }
                if (dirAccessArray[dirIndex].KEY.equals("ALL")) {
                    fallbackPermission = dirAccessArray[dirIndex];
                    continue;
                }
                if (dirAccessArray[dirIndex].KEY.equals(keyArray[keyIndex])) {
                    findedAnswer = dirAccessArray[dirIndex].checkByMode(givenMode);
                    if (findedAnswer == true) {
                        return findedAnswer;
                    }
                }
            }
        }
        if (fallbackPermission == null) {
            fallbackPermission = RibbonServer.dirObj.new dirPermissionEntry("ALL:" + RibbonServer.CURR_ALL_MASK);
        }
        if (findedAnswer == false) {
            findedAnswer = fallbackPermission.checkByMode(givenMode);
        }
        return findedAnswer;
    }
    
    /**
     * Check access to directories with specified mode;<br>
     * <br>
     * <b>Modes:</b><br>
     * 0 - attempt to read directory;<br>
     * 1 - attempt to release messege in directory;<br>
     * 2 - attempt to admin directory;
     * @param givenName user name which attempt to perform some action
     * @param givenDirs array with directories which should be checked
     * @return null if success or array index which checking failed
     */
    public Integer checkAccessForAll(String givenName, String[] givenDirs, Integer givenMode) {
        for (Integer dirIndex = 0; dirIndex < givenDirs.length; dirIndex++) {
            if (this.checkAccess(givenName, givenDirs[dirIndex], givenMode) == false) {
                return dirIndex;
            }
        }
        return null;
    }
    
    /**
     * Find out is user is administrator
     * @param givenName name to search
     * @return result of checking
     */
    public Boolean isUserAdmin(String givenName) {
        java.util.ListIterator<userEntry> userIter = this.userStore.listIterator();
        userEntry findedUser = null;
        while (userIter.hasNext()) {
            userEntry currUser = userIter.next();
            if (currUser.USER_NAME.equals(givenName)) {
                findedUser = currUser;
                break;
            }
        }
        if (findedUser == null) {
            return false;
        }
        for (String groupItem : findedUser.GROUPS) {
            if (groupItem.equals("ADM")) {
                return true;
            }
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
