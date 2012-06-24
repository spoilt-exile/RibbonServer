/*
 * This code is distributed under terms of GNU GPLv2.
 * *See LICENSE file.
 * ©UKRINFORM 2011-2012
 */

package ribbonserver;

/**
 * Directories handle class
 * @author Stanislav Nepochatov
 */
public class Directories {
    
    /**
     * ID of this component or object for loging
     */
    private String LOG_ID = "Обробник напрямків";
    
    /**
     * Root directory
     */
    private dirEntry rootDir;
    
    /**
     * Default constructor
     */
    Directories() {
        rootDir = new dirEntry();
        java.util.ArrayList<dirSchema> readedDirs = csvHandler.readDirectories();
        java.util.ListIterator<dirSchema> readIter = readedDirs.listIterator();
        while (readIter.hasNext()) {
            dirSchema currDir = readIter.next();
            createDirs(currDir);
        }
        dumpTree();
    }
    
    /**
     * Directory scheme 
     * This class using to deliver schemas from csvHandler;
     */
    public static class dirSchema {
        
        dirSchema(String[] givenArray) {
            SH_DIR_PATH = givenArray[0];
            SH_COMM = givenArray[1];
            if (givenArray[2].equals("1")) {
                SH_ANON_MODE = true;
            } else {
                SH_ANON_MODE = false;
            }
        }
        
        dirSchema(String givenPath, String givenComm, String givenFlag) {
            SH_DIR_PATH = givenPath;
            SH_COMM = givenComm;
            if (givenFlag.equals("1")) {
                SH_ANON_MODE = true;
            } else {
                SH_ANON_MODE = false;
            }
        }
        
        public String SH_DIR_PATH;
        public String SH_COMM;
        public Boolean SH_ANON_MODE;
    }
    
    /**
     * Directory entry
     */
    private class dirEntry {

        /**
         * Default constructor (empty)
         * Usually this constructor used for creating root dir
         */
        dirEntry() {
            DIR_NAME = "";
            FULL_DIR_NAME = "";
            RibbonServer.logAppend(LOG_ID, 2, "додано кореневий напрямок");
        }
        
        /**
         * Chain constuctor
         * @param upperLevel all parent directories
         * @param rest rest of the creation query
         * @param givenComm commentary for directory
         * @param givenPath path for images
         * @param givenAnon anonymous mode flag
         */
        dirEntry(String upperLevel, String rest, String givenComm, Boolean givenAnon) {
            Integer joint;
            if ((joint = rest.indexOf(".")) != -1) {
                DIR_NAME = rest.substring(0, joint);
                if (upperLevel.isEmpty()) {
                    FULL_DIR_NAME = DIR_NAME;
                } else {
                    FULL_DIR_NAME = upperLevel + "." + DIR_NAME;
                }
                DIR_PATH = RibbonServer.BASE_PATH + "/" + FULL_DIR_NAME.toLowerCase().replaceAll("\\.", "/") + "/";
                ANON_MODE = false;
                new java.io.File(DIR_PATH).mkdirs();
                RibbonServer.logAppend(LOG_ID, 2, "додано порожній напрямок (" + FULL_DIR_NAME + ")");
                COMM = "Порожній напрямок";
                FOLDED_DIR.add(new dirEntry(FULL_DIR_NAME, rest.substring(joint + 1), givenComm, givenAnon));
            } else {
                DIR_NAME = rest;
                if (upperLevel.isEmpty()) {
                    FULL_DIR_NAME = DIR_NAME;
                } else {
                    FULL_DIR_NAME = upperLevel + "." + DIR_NAME;
                }
                COMM = givenComm;
                DIR_PATH = RibbonServer.BASE_PATH + "/" + FULL_DIR_NAME.toLowerCase().replaceAll("\\.", "/") + "/";
                ANON_MODE = givenAnon;
                if (givenAnon == true) {
                    RibbonServer.logAppend(LOG_ID, 2, "додано напрямок (" + FULL_DIR_NAME + ":" + COMM + ") з можливістю анонімного випуску");
                } else {
                    RibbonServer.logAppend(LOG_ID, 2, "додано напрямок (" + FULL_DIR_NAME + ":" + COMM + ")");
                }
                new java.io.File(DIR_PATH).mkdirs();
            }
        }
        
        /**
         * Short directrory name
         */
        public String DIR_NAME;
        
        /**
         * Full directory name (with parents)
         */
        public String FULL_DIR_NAME;

        /**
         * Arraylist of inner directiries
         */
        public java.util.ArrayList<dirEntry> FOLDED_DIR = new java.util.ArrayList<dirEntry>();
        
        /**
         * Arraylist of messages indexes
         */
        public java.util.ArrayList<String> DIR_INDEXCES = new java.util.ArrayList<String>();
        
        /**
         * Directory commentary
         */
        public String COMM;
        
        /**
         * Anonymous acces to directory
         */
        public Boolean ANON_MODE;
        
        /**
         * Path to dir messages
         */
        public String DIR_PATH;
        
        /**
         * Last searched directory
         */
        private dirEntry lastEntry;
        
        /**
         * Insert chain of directories in current directory
         * @param upperLevel all parent directories
         * @param rest rest of the creation query
         * @param givenComm commentary for directory
         * @param givenPath path for images
         * @param givenAnon anonymous mode flag
         */
        public void insertDir(String upperLevel, String rest, String givenComm, Boolean givenAnon) {
            Integer joint;
            if ((joint = rest.indexOf(".")) != -1) {
                String inserted_DIR_NAME = rest.substring(0, joint);
                String inserted_FULL_DIR_NAME;
                if (upperLevel.isEmpty()) {
                    inserted_FULL_DIR_NAME = upperLevel + inserted_DIR_NAME;
                } else {
                    inserted_FULL_DIR_NAME = upperLevel + "." + inserted_DIR_NAME;
                }
                if (this.hasFoldDir(inserted_DIR_NAME)) {
                    lastEntry.insertDir(inserted_FULL_DIR_NAME, rest.substring(joint + 1), givenComm, givenAnon);
                } else {
                    if (this.DIR_NAME.isEmpty()) {
                        this.FOLDED_DIR.add(new dirEntry("", rest, givenComm, givenAnon));
                    } else {
                        this.FOLDED_DIR.add(new dirEntry(inserted_FULL_DIR_NAME, rest.substring(joint + 1), givenComm, givenAnon));
                    }
                }
            } else {
                String inserted_DIR_NAME = rest;
                String inserted_FULL_DIR_NAME = upperLevel + "." + inserted_DIR_NAME;
                if (hasFoldDir(inserted_DIR_NAME)) {
                    lastEntry.updateDir(givenComm, givenAnon);
                } else {
                    FOLDED_DIR.add(new dirEntry(upperLevel, rest, givenComm, givenAnon));
                }
            }
        }
        
        /**
         * Find out if there is a specified dir in FOLDED_DIR
         * @param foldedDirName name of directory
         * @return true if this entry contain such directory in it's children
         */
        private Boolean hasFoldDir(String foldedDirName) {
            java.util.ListIterator<dirEntry> dirIter = FOLDED_DIR.listIterator();
            while (dirIter.hasNext()) {
                dirEntry foldedDir = dirIter.next();
                if (foldedDir.DIR_NAME.equals(foldedDirName)) {
                    lastEntry = foldedDir;
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Update empty directory
         * @param givenComm commentary
         * @param givenAnon anonymous mode flag 
         */
        public void updateDir(String givenComm, Boolean givenAnon) {
            RibbonServer.logAppend(LOG_ID, 2, "оновлення напрямку (" + this.FULL_DIR_NAME + ")");
            this.COMM = givenComm;
            this.ANON_MODE = givenAnon;
        }
        
        /**
         * Build tree from specifed inner level
         * @param inLevel inner folding level
         * @return tree formated string
         */
        public String treeReport(Integer inLevel) {
            String cap = "==================================================================================================================";
            String spaceStr = "";
            for (Integer space = 0; space < inLevel; space++) {
                spaceStr += "   ";
            }
            String returned = "";
            if (inLevel > 0) {
                String anonMsg = this.ANON_MODE ? "Так" : "Ні";
                returned = spaceStr + this.DIR_NAME + ": " + this.COMM + " Анонімний режим: " + anonMsg + "\n";
            } else {
                returned = cap + "\nНапрямки системи \"Стрічка\"\n\nКореневий напрямок:\n" + cap + "\n";
            }
            if (!this.FOLDED_DIR.isEmpty()) {
                java.util.ListIterator<dirEntry> foldedIter = this.FOLDED_DIR.listIterator();
                while (foldedIter.hasNext()) {
                    dirEntry foldDir = foldedIter.next();
                    returned += foldDir.treeReport(inLevel + 1);
                }
            }
            if (inLevel == 0) {
                return returned + cap + "\n" + RibbonServer.getCurrentDate() + "\n" + cap;
            } else {
                return returned;
            }
        }
        
        /**
         * Build full text report
         * @return csv report
         */
        public String report() {
            return this.FULL_DIR_NAME + "," + this.COMM + this.ANON_MODE.booleanValue() + "\n";
        }
        
        /**
         * Add index to folded directory
         * @param upperLevel upper level
         * @param rest rest of add query
         * @param givenIndex index of message
         */
        public void addIndex(String upperLevel, String rest, String givenIndex) {
            Integer joint;
            if ((joint = rest.indexOf(".")) != -1) {
                String indxed_DIR_NAME = rest.substring(0, joint);
                if (this.hasFoldDir(indxed_DIR_NAME) == false) {
                    RibbonServer.logAppend(LOG_ID, 1, "неможливо додати індекс " + givenIndex + " до напрямку " + upperLevel + ">" + rest);
                    return;
                } else {
                    lastEntry.addIndex(this.FULL_DIR_NAME, rest.substring(joint + 1), givenIndex);
                }
            } else {
                String indxed_DIR_NAME = rest;
                if (this.hasFoldDir(indxed_DIR_NAME) == false) {
                    RibbonServer.logAppend(LOG_ID, 1, "неможливо додати індекс " + givenIndex + " до напрямку " + upperLevel + ">" + rest);
                    return;
                } else {
                    lastEntry.DIR_INDEXCES.add(givenIndex);
                }
            }
        }
        
        /**
         * Remove index from folded directory
         * @param upperLevel upper level
         * @param rest rest of remove query
         * @param givenIndex index of message
         */
        public void removeIndex(String upperLevel, String rest, String givenIndex) {
            Integer joint;
            if ((joint = rest.indexOf(".")) != -1) {
                String indxed_DIR_NAME = rest.substring(0, joint);
                if (this.hasFoldDir(indxed_DIR_NAME) == false) {
                    RibbonServer.logAppend(LOG_ID, 1, "неможливо вилучити індекс " + givenIndex + " з напрямку " + upperLevel + ">" + rest);
                    return;
                } else {
                    lastEntry.removeIndex(this.FULL_DIR_NAME, rest.substring(joint + 1), givenIndex);
                }
            } else {
                String indxed_DIR_NAME = rest;
                if (this.hasFoldDir(indxed_DIR_NAME) == false) {
                    RibbonServer.logAppend(LOG_ID, 1, "неможливо вилучити індекс " + givenIndex + " з напрямку " + upperLevel + ">" + rest);
                    return;
                } else {
                    lastEntry.DIR_INDEXCES.remove(givenIndex);
                }
            }
        }
        
        /**
         * Get cascade of foded diretories.
         * @return formated string for net protocol
         */
        public String PROC_GET_DIR() {
            String returned = "RIBBON_UCTL_LOAD_DIR:" + this.toCsv() + "\n";
            java.util.ListIterator<dirEntry> foldIter = this.FOLDED_DIR.listIterator();
            while (foldIter.hasNext()) {
                returned += foldIter.next().PROC_GET_DIR();
            }
            return returned;
        }
        
        /**
         * Translate directory object into csv index line
         * @return csv formated string
         */
        public String toCsv() {
            String anon_msg;
            if (this.ANON_MODE == false) {
                anon_msg = "0";
            } else {
                anon_msg = "1";
            }
            return this.FULL_DIR_NAME + ",{" + this.COMM + "}," + anon_msg;
        }
        
        /**
         * Return dir with specified path
         * @param upperLevel upper level of path
         * @param rest rest of path
         * @return folded directory object
         */
        public dirEntry returnEndDir(String upperLevel, String rest) {
            Integer joint;
            if ((joint = rest.indexOf(".")) != -1) {
                String indxed_DIR_NAME = rest.substring(0, joint);
                if (this.hasFoldDir(indxed_DIR_NAME) == false) {
                    RibbonServer.logAppend(LOG_ID, 1, "неможливо знайти шлях напрямку " + upperLevel + ">" + rest);
                    return null;
                } else {
                    return lastEntry.returnEndDir(this.FULL_DIR_NAME, rest.substring(joint + 1));
                }
            } else {
                String indxed_DIR_NAME = rest;
                if (this.hasFoldDir(indxed_DIR_NAME) == false) {
                    RibbonServer.logAppend(LOG_ID, 1, "неможливо знайти шлях напрямку " + upperLevel + ">" + rest);
                    return null;
                } else {
                    return lastEntry;
                }
            }
        }
    }
    
    /**
     * Create full chain of directories
     * @param dirChain full path of directory
     * @param commentary commentary for directory
     * @param anon_mode anonymous mode flag
     */
    public void createDirs(String dirChain, String commentary, Boolean anon_mode) {
        this.rootDir.insertDir("", dirChain, commentary, anon_mode);
    }
    
    /**
     * Create full chain of directories with given schema
     * @param givenSchema directory schema
     */
    public void createDirs(dirSchema givenSchema) {
        this.rootDir.insertDir("", givenSchema.SH_DIR_PATH, givenSchema.SH_COMM, givenSchema.SH_ANON_MODE);
    }
    
    /**
     * Dump current tree as text report
     */
    private void dumpTree() {
        try {
            java.io.FileWriter treeWriter = new java.io.FileWriter(RibbonServer.BASE_PATH + "/tree");
            treeWriter.write(this.rootDir.treeReport(0));
            treeWriter.close();
        } catch (java.io.IOException ex) {
            RibbonServer.logAppend(LOG_ID, 1, "неможливо створити файл дерева напрямків!");
        } finally {
            RibbonServer.logAppend(LOG_ID, 3, "створено файл дерева напрямків.");
        }
    }
    
    /**
     * Add given index to specified directory
     * @param givenDir directory in which index will be added
     * @param givenIndex index identifier
     */
    public void addIndexToDir(String givenDir, String givenIndex) {
        this.rootDir.addIndex("", givenDir, givenIndex);
    }
    
    /**
     * Remove given index from specified directory
     * @param givenDir directory from which index will be removed
     * @param givenIndex index indentifier
     */
    public void removeIndexFromDir(String givenDir, String givenIndex) {
        this.rootDir.removeIndex("", givenDir, givenIndex);
    }
    
    /**
     * Return anonymoys mode flag for specifed dir
     * @param givenDir given path to directory
     * @return anonymous flag
     */
    public Boolean getAnonMode(String givenDir) {
        Boolean returned = rootDir.returnEndDir("", givenDir).ANON_MODE;
        if (returned == null) {
            RibbonServer.logAppend(LOG_ID, 1, "не удалось получить доступ к данным директории: " + givenDir);
            return false;
        }
        return returned;
    }
    
    /**
     * Return anonymoys mode flag for specifed dir
     * @param givenDir given path to directory
     * @return path to file directory
     */
    public String getDirPath(String givenDir) {
        String returned = rootDir.returnEndDir("", givenDir).DIR_PATH;
        if (returned == null) {
            RibbonServer.logAppend(LOG_ID, 1, "не удалось получить доступ к данным директории: " + givenDir);
            return null;
        }
        return returned;
    }
    
    /**
     * <b>[RIBBON a1]</b><br>
     * Return all dirs to protocol commandlet
     * @return dirs in csv form;
     */
    public String PROC_GET_DIRS() {
        String returned = "";
        java.util.ListIterator<dirEntry> rootDirs = this.rootDir.FOLDED_DIR.listIterator();
        while (rootDirs.hasNext()) {
            returned += rootDirs.next().PROC_GET_DIR();
        }
        return returned + "END:";
    }
}
