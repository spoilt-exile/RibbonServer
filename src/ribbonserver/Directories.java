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
    private DirClasses.dirEntry rootDir;
    
    /**
     * Default constructor
     */
    Directories() {
        rootDir = new DirClasses.dirEntry();
        java.util.ArrayList<DirClasses.dirSchema> readedDirs = indexReader.readDirectories();
        java.util.ListIterator<DirClasses.dirSchema> readIter = readedDirs.listIterator();
        while (readIter.hasNext()) {
            DirClasses.dirSchema currDir = readIter.next();
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
    public void createDirs(DirClasses.dirSchema givenSchema) {
        this.rootDir.insertDir("", givenSchema.FULL_DIR_NAME, givenSchema);
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
     * Return access description array from 
     * @param givenDir
     * @return 
     */
    public DirClasses.dirPermissionEntry[] getDirAccess(String givenDir) {
        try {
            return this.rootDir.getAccess("", givenDir);
        } catch (Exception ex) {
            return null;
        }
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
        java.util.ListIterator<DirClasses.dirEntry> rootDirs = this.rootDir.FOLDED_DIR.listIterator();
        while (rootDirs.hasNext()) {
            returned += rootDirs.next().PROC_GET_DIR();
        }
        return returned + "END:";
    }
}
