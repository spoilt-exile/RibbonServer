/*
 * This code is distributed under terms of GNU GPLv2.
 * *See LICENSE file.
 * ©UKRINFORM 2011-2012
 */

package ribbonserver;

/**
 * Main system procedures
 * @author Stanislav Nepochatov
 */
public class Procedures {
    
    private static String LOG_ID = "Бібліотека процедур";
    
    /**
     * RIBBON_ERROR Exception class
     */
    public static class RIBBON_ERROR extends Exception {
        
        RIBBON_ERROR(String givenMessage) {
            super();
            ERROR_MESSAGE = givenMessage;
            PROC_ERROR_MESSAGE = "RIBBON_ERROR:" + ERROR_MESSAGE;
        }
        
        /**
         * Error message;
         */
        public String ERROR_MESSAGE;
        
        /**
         * Error message in protocol form;
         */
        public String PROC_ERROR_MESSAGE;
    }
    
    /**
     * <b>[RIBBON a1]</b><br>
     * Post given message into the system information stream
     * @param givenMessage message which should be released
     * @return processing status;
     */
    public static synchronized String PROC_POST_MESSAGE(Messenger.Message givenMessage) {
        if (RibbonServer.CURR_STATE != RibbonServer.SYS_STATES.READY) {
            RibbonServer.logAppend(LOG_ID, 1, "неможливо випустити повідомлення, система не готова!");
            return "RIBBON_ERROR:Система не готова!";
        } else {
            String acceptedDir = null;
            for (Integer postDirIndex = 0; postDirIndex < givenMessage.DIRS.length; postDirIndex++) {
                if (RibbonServer.userObj.checkUser(givenMessage.AUTHOR, givenMessage.DIRS[postDirIndex]) == false) {
                    return "RIBBON_ERROR:Напрямок " + givenMessage.DIRS[postDirIndex] +" не підтримує анонімний випуск.";
                } else {
                    if (acceptedDir == null) {
                        acceptedDir = givenMessage.DIRS[postDirIndex];
                    }
                }
            }
            RibbonServer.messageObj.addMessageToIndex(givenMessage);
            writeMessage(givenMessage.DIRS, givenMessage.INDEX, givenMessage.CONTENT);
            csvHandler.appendToBaseIndex(givenMessage.returnEntry().toCsv());
            for (Integer dirIndex = 0; dirIndex < givenMessage.DIRS.length; dirIndex++) {
                if (givenMessage.DIRS[dirIndex] == null) {
                    RibbonServer.logAppend(LOG_ID, 1, "неможливо випустити повідомлення" + givenMessage.HEADER + "на напрямок " + givenMessage.DIRS[dirIndex]);
                } else {
                    RibbonServer.logAppend(LOG_ID, 3, givenMessage.DIRS[dirIndex] + " додано повідомлення: [" + givenMessage.HEADER + "]");
                }
            }
            return "OK:";
        }
    }
    
    /**
     * Write message content to file and create links
     * @param fullPath full path to message file
     * @param messageContent content of the message
     */
    public static synchronized void writeMessage(String[] dirArr, String strIndex, String messageContent) {
        Boolean origFileCreated = false;
        String origFilePath = null;
        String currPath = "";
        try {
            for (Integer pathIndex = 0; pathIndex < dirArr.length; pathIndex++) {
                if (dirArr[pathIndex] == null) {
                    continue;
                } else {
                    currPath = RibbonServer.dirObj.getDirPath(dirArr[pathIndex]);
                    if (currPath == null) {
                        continue;
                    }
                    if (origFileCreated == false) {
                        java.io.FileWriter messageWriter = new java.io.FileWriter(currPath + strIndex);
                        messageWriter.write(messageContent);
                        messageWriter.close();
                        origFilePath = currPath + strIndex;
                        origFileCreated = true;
                    } else {
                        java.nio.file.Files.createLink(new java.io.File(currPath + strIndex).toPath(), new java.io.File(origFilePath).toPath());
                    }
                }
            }
        } catch (java.io.IOException ex) {
            RibbonServer.logAppend(LOG_ID, 1, "Неможливо записити файл за шлязом: " + currPath + strIndex);
        } catch (UnsupportedOperationException ex) {
            RibbonServer.logAppend(LOG_ID, 1, "Неможливо створити посилання на файл!");
        }
    }
    
    /**
     * <b>[RIBBON a1]</b><br>
     * Delete message from all indexes.
     * @param givenEntry entry to delete
     */
    public static synchronized void PROC_DELETE_MESSAGE(Messenger.messageEntry givenEntry) {
        for (Integer pathIndex = 0; pathIndex < givenEntry.DIRS.length; pathIndex++) {
            String currPath = RibbonServer.dirObj.getDirPath(givenEntry.DIRS[pathIndex]) + givenEntry.INDEX;
            try {
                java.nio.file.Files.delete(new java.io.File(currPath).toPath());
            } catch (java.io.IOException ex) {
                RibbonServer.logAppend(LOG_ID, 1, "неможливо видалити повідомлення: " + currPath);
            }
        }
        RibbonServer.messageObj.deleteMessageEntryFromIndex(givenEntry);
        RibbonServer.logAppend(LOG_ID, 3, "повідомлення за індексом " + givenEntry.INDEX + "вилучено з системи.");
    }
    
    /**
     * Post system launch notification
     */
    public static void postInitMessage() {
        String formatLine = "======================================================================================";
        PROC_POST_MESSAGE(new Messenger.Message(
            "Системне повідомлення",
            "root",
            new String[] {"СИСТЕМА.Тест"},
            new String[] {"оголошення", "ІТУ"},
            formatLine + "\nСистема \"Стрічка\" " + RibbonServer.RIBBON_VER + "\n" + formatLine + "\n"
                    + "Це повідомлення автоматично генерується системою \"Стрічка\"\n"
                    + "при завантаженні. Зараз система готова для одержання повідомлень."
                    + "\n\n" + RibbonServer.getCurrentDate()));
    }
}
