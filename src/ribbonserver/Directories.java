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

import Utils.IOControl;

/**
 * Directories handle class
 * @author Stanislav Nepochatov
 * @since RibbonServer a1
 */
public final class Directories {
    
    /**
     * ID of this component or object for loging.
     */
    private static String LOG_ID = "НАПРЯМКИ";
    
    /**
     * Root directory.
     * @since RibbonServer a1
     */
    private static DirClasses.DirEntry rootDir;
    
    /**
     * Directories global lock.
     * @since RibbonServer a2
     */
    private static final Object dirLock = new Object();
    
    /**
     * Init directory's component.
     * @since RibbonServer a2
     */
    public static void init() {
        rootDir = new DirClasses.DirEntry();
        java.util.ArrayList<DirClasses.DirSchema> readedDirs = IndexReader.readDirectories();
        java.util.ListIterator<DirClasses.DirSchema> readIter = readedDirs.listIterator();
        while (readIter.hasNext()) {
            DirClasses.DirSchema currDir = readIter.next();
            RibbonServer.logAppend(LOG_ID, 3, "додано напрямок (" + currDir.FULL_DIR_NAME + ": " + currDir.COMM + ")");
            createDirs(currDir);
            IOControl.dispathcer.subscribeDir(currDir.DIR_EXPORTS, currDir.FULL_DIR_NAME);
        }
        rootDir.deployDir(RibbonServer.BASE_PATH);
        dumpTree();
    }
    
    /**
     * Create full chain of directories with given schema
     * @param givenSchema directory schema
     * @since RibbonServer a1
     */
    public static void createDirs(DirClasses.DirSchema givenSchema) {
        Directories.rootDir.insertDir("", givenSchema.FULL_DIR_NAME, givenSchema);
    }
    
    /**
     * Dump current tree as text report
     * @since RibbonServer a1
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
     * @since RibbonServer a1
     */
    public static void addIndexToDir(String givenDir, String givenIndex) {
        synchronized (dirLock) {
            Directories.rootDir.addIndex("", givenDir, givenIndex);
        }
    }
    
    /**
     * Remove given index from specified directory
     * @param givenDir directory from which index will be removed
     * @param givenIndex index indentifier
     * @since RibbonServer a1
     */
    public static void removeIndexFromDir(String givenDir, String givenIndex) {
        synchronized (dirLock) {
            Directories.rootDir.removeIndex("", givenDir, givenIndex);
        }
    }
    
    /**
     * Return access description array from 
     * @param givenDir dir to search;
     * @return array with permission entries;
     * @since RibbonServer a2
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
     * @since RibbonServer a1
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
     * Return all dirs to protocol commandlet.
     * @return dirs in csv form;
     * @since RibbonServer a1
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
