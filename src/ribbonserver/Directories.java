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
public final class Directories {
    
    /**
     * ID of this component or object for loging
     */
    private static String LOG_ID = "Обробник напрямків";
    
    /**
     * Root directory
     */
    private static DirClasses.DirEntry rootDir;
    
    /**
     * Init directory's component
     */
    public static void init() {
        rootDir = new DirClasses.DirEntry();
        java.util.ArrayList<DirClasses.DirSchema> readedDirs = IndexReader.readDirectories();
        java.util.ListIterator<DirClasses.DirSchema> readIter = readedDirs.listIterator();
        while (readIter.hasNext()) {
            DirClasses.DirSchema currDir = readIter.next();
            RibbonServer.logAppend(LOG_ID, 3, "додано напрямок (" + currDir.FULL_DIR_NAME + ": " + currDir.COMM + ")");
            createDirs(currDir);
        }
        rootDir.deployDir(RibbonServer.BASE_PATH);
        dumpTree();
    }
    
    /**
     * Create full chain of directories with given schema
     * @param givenSchema directory schema
     */
    public static void createDirs(DirClasses.DirSchema givenSchema) {
        Directories.rootDir.insertDir("", givenSchema.FULL_DIR_NAME, givenSchema);
    }
    
    /**
     * Dump current tree as text report
     */
    private static void dumpTree() {
        try {
            java.io.FileWriter treeWriter = new java.io.FileWriter(RibbonServer.BASE_PATH + "/tree");
            treeWriter.write(Directories.rootDir.treeReport(0));
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
    public static void addIndexToDir(String givenDir, String givenIndex) {
        Directories.rootDir.addIndex("", givenDir, givenIndex);
    }
    
    /**
     * Remove given index from specified directory
     * @param givenDir directory from which index will be removed
     * @param givenIndex index indentifier
     */
    public static void removeIndexFromDir(String givenDir, String givenIndex) {
        Directories.rootDir.removeIndex("", givenDir, givenIndex);
    }
    
    /**
     * Return access description array from 
     * @param givenDir
     * @return 
     */
    public static DirClasses.DirPermissionEntry[] getDirAccess(String givenDir) {
        try {
            return Directories.rootDir.getAccess("", givenDir);
        } catch (Exception ex) {
            return null;
        }
    }
    
    /**
     * Return anonymoys mode flag for specifed dir
     * @param givenDir given path to directory
     * @return path to file directory
     */
    public static String getDirPath(String givenDir) {
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
    public static String PROC_GET_DIRS() {
        String returned = "";
        java.util.ListIterator<DirClasses.DirEntry> rootDirs = Directories.rootDir.FOLDED_DIR.listIterator();
        while (rootDirs.hasNext()) {
            returned += rootDirs.next().PROC_GET_DIR();
        }
        return returned + "END:";
    }
}
