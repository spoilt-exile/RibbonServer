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
 * Read, parse and store CSV Ribbon configurations and base index class
 * @author Stanislav Nepochatov
 */
public abstract class IndexReader {
    
    private static String LOG_ID = "Зчитувач індексів";
    
    /**
     * Read directories in directory index file
     * @return arraylist of dir shemas
     * @see Directories.dirSchema
     */
    public static java.util.ArrayList<DirClasses.DirSchema> readDirectories() {
        java.util.ArrayList<DirClasses.DirSchema> Dirs = new java.util.ArrayList<>();
        try {
            java.io.BufferedReader dirIndexReader = new java.io.BufferedReader(new java.io.FileReader(RibbonServer.BASE_PATH + "/" + RibbonServer.DIR_INDEX_PATH));
            while (dirIndexReader.ready()) {
                Dirs.add(new DirClasses.DirSchema(dirIndexReader.readLine()));
            }
        } catch (java.io.FileNotFoundException ex) {
            RibbonServer.logAppend(LOG_ID, 2, "попередній файл індексу напрявків не знайдено. Створюю новий.");
            java.io.File dirIndexFile = new java.io.File(RibbonServer.BASE_PATH + "/" + RibbonServer.DIR_INDEX_PATH);
            try {
                dirIndexFile.createNewFile();
                try (java.io.FileWriter dirIndexWriter = new java.io.FileWriter(dirIndexFile)) {
                    dirIndexWriter.write("СИСТЕМА,{Головний напрямок новин про розробку системи},[ALL],[ALL:110],[]\n");
                    dirIndexWriter.write("СИСТЕМА.Розробка,{Новини про розробку},[UA,RU],[ALL:100],[]\n");
                    dirIndexWriter.write("СИСТЕМА.Тест,{Тестовий напрямок},[UA,RU],[ALL:110],[]\n");
                }
                Dirs.add(new DirClasses.DirSchema("СИСТЕМА", "Головний напрямок новин про розробку системи"));
                Dirs.add(new DirClasses.DirSchema("СИСТЕМА.Розробка", "Новини про розробку"));
                Dirs.add(new DirClasses.DirSchema("СИСТЕМА.Тест", "Тестовий напрямок"));
            } catch (java.io.IOException exq) {
                RibbonServer.logAppend(LOG_ID, 0, "неможливо створити новий файл індексу напрямків!");
                System.exit(4);
            }
        } catch (java.io.IOException ex) {
            RibbonServer.logAppend(LOG_ID, 0, "помилка читання файлу індекса напрямків!");
            System.exit(4);
        }
        return Dirs;
    }
    
    /**
     * Read users in users index file
     * @return arrayList of users entries
     */
    public static java.util.ArrayList<UserClasses.UserEntry> readUsers() {
        java.util.ArrayList<UserClasses.UserEntry> returnedUsers = new java.util.ArrayList<>();
        try {
            java.io.BufferedReader userIndexReader = new java.io.BufferedReader(new java.io.FileReader(RibbonServer.BASE_PATH + "/" + RibbonServer.USERS_INDEX_PATH));
            while (userIndexReader.ready()) {
                returnedUsers.add(new UserClasses.UserEntry(userIndexReader.readLine()));
            }
        } catch (java.io.FileNotFoundException ex) {
            RibbonServer.logAppend(LOG_ID, 2, "попередній файл індексу користувачів не знайдено. Створюю новий.");
            java.io.File usersIndexFile = new java.io.File(RibbonServer.BASE_PATH + "/" + RibbonServer.USERS_INDEX_PATH);
            try {
                usersIndexFile.createNewFile();
                try (java.io.FileWriter usersIndexWriter = new java.io.FileWriter(usersIndexFile)) {
                    usersIndexWriter.write("{root},{Root administrator, pass: root},[ADM],74cc1c60799e0a786ac7094b532f01b1,1\n");
                    usersIndexWriter.write("{test},{Test user, pass: test},[test],d8e8fca2dc0f896fd7cb4cb0031ba249,1\n");
                }
                returnedUsers.add(new UserClasses.UserEntry("{root},{Root administrator, pass: root},[ADM],74cc1c60799e0a786ac7094b532f01b1,1"));
                returnedUsers.add(new UserClasses.UserEntry("{test},{Test user, pass: test},[test],d8e8fca2dc0f896fd7cb4cb0031ba249,1"));
            } catch (java.io.IOException exq) {
                RibbonServer.logAppend(LOG_ID, 0, "неможливо створити новий файл індексу користувачів!");
                System.exit(5);
            }
        } catch (java.io.IOException ex) {
            RibbonServer.logAppend(LOG_ID, 0, "помилка читання файлу індекса користувачів!");
            System.exit(4);
        }
        return returnedUsers;
    }
    
    /**
     * Read groups in groups index file
     * @return arrayList of groups entries
     */
    public static java.util.ArrayList<UserClasses.GroupEntry> readGroups() {
        java.util.ArrayList<UserClasses.GroupEntry> returnedGroups = new java.util.ArrayList<>();
        try {
            java.io.BufferedReader groupIndexReader = new java.io.BufferedReader(new java.io.FileReader(RibbonServer.BASE_PATH + "/" + RibbonServer.GROUPS_INDEX_PATH));
            Integer currentLine = 0;
            while (groupIndexReader.ready()) {
                returnedGroups.add(new UserClasses.GroupEntry(groupIndexReader.readLine()));
            }
        } catch (java.io.FileNotFoundException ex) {
            RibbonServer.logAppend(LOG_ID, 2, "попередній файл індексу груп не знайдено. Створюю новий.");
            java.io.File usersIndexFile = new java.io.File(RibbonServer.BASE_PATH + "/" + RibbonServer.GROUPS_INDEX_PATH);
            try {
                usersIndexFile.createNewFile();
                try (java.io.FileWriter usersIndexWriter = new java.io.FileWriter(usersIndexFile)) {
                    usersIndexWriter.write("{test},{Test group}\n");
                }
                returnedGroups.add(new UserClasses.GroupEntry("{test},{Test group}"));
            } catch (java.io.IOException exq) {
                RibbonServer.logAppend(LOG_ID, 0, "неможливо створити новий файл індексу груп!");
                System.exit(5);
            }
        } catch (java.io.IOException ex) {
            RibbonServer.logAppend(LOG_ID, 0, "помилка читання файлу індекса груп!");
            System.exit(4);
        }
        return returnedGroups;
    }
    
    /**
     * Read message indexes in base index file
     * @return arraylist with index entries
     */
    public static java.util.ArrayList<MessageClasses.MessageEntry> readBaseIndex() {
        java.util.ArrayList<MessageClasses.MessageEntry> returnedIndex = new java.util.ArrayList<>();
        try {
            java.io.BufferedReader baseIndexReader = new java.io.BufferedReader(new java.io.FileReader(RibbonServer.BASE_PATH + "/" + RibbonServer.BASE_INDEX_PATH));
            while (baseIndexReader.ready()) {
                returnedIndex.add(new MessageClasses.MessageEntry(baseIndexReader.readLine()));
            }
        } catch (java.io.FileNotFoundException ex) {
            RibbonServer.logAppend(LOG_ID, 2, "попередній файл індексу бази не знайдено. Створюю новий.");
            java.io.File usersIndexFile = new java.io.File(RibbonServer.BASE_PATH + "/" + RibbonServer.BASE_INDEX_PATH);
            try {
                usersIndexFile.createNewFile();
            } catch (java.io.IOException exq) {
                RibbonServer.logAppend(LOG_ID, 0, "неможливо створити новий файл індексу бази!");
                System.exit(5);
            }
        } catch (java.io.IOException ex) {
            RibbonServer.logAppend(LOG_ID, 0, "помилка читання файлу індекса бази повідомлень!");
            System.exit(4);
        }
        return returnedIndex;
    }
    
    /**
     * Append new message csv to base index file
     * @param csvReport csv formated string
     */
    public synchronized static void appendToBaseIndex(String csvReport) {
        try {
            try (java.io.FileWriter messageWriter = new java.io.FileWriter(RibbonServer.BASE_PATH + "/" + RibbonServer.BASE_INDEX_PATH, true)) {
                messageWriter.write(csvReport + "\n");
            }
        } catch (java.io.IOException ex) {
            RibbonServer.logAppend(LOG_ID, 0, "Неможливо записита файл индекса бази повідомлень!");
        }
    }
    
    /**
     * Update base index file after message manipulations
     */
    public synchronized static void updateBaseIndex() {
        Thread delayExec = new Thread() {
            @Override
            public void run() {
                java.util.ListIterator<MessageClasses.MessageEntry> storeIter = Messenger.messageIndex.listIterator();
                String newIndexFileContent = "";
                while (storeIter.hasNext()) {
                    newIndexFileContent += storeIter.next().toCsv() + "\n";
                }
                try {
                    try (java.io.FileWriter messageWriter = new java.io.FileWriter(RibbonServer.BASE_PATH + "/" + RibbonServer.BASE_INDEX_PATH)) {
                        messageWriter.write(newIndexFileContent);
                    }
                } catch (java.io.IOException ex) {
                    RibbonServer.logAppend(LOG_ID, 0, "Неможливо записита файл индекса бази повідомлень!");
                }
            }
        };
        delayExec.start();
    }
}