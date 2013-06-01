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
 * Main Ribbon server class
 * @author Stanislav Nepochatov
 */
public class RibbonServer {
    
    /**
     * Session and network handle object
     */
    public static SessionManager sessionObj;
    
    /**
     * Date format
     */
    private static java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("HH:mm:ss dd.MM.yyyy");
    
    /**
     * ID of this component or object for loging
     */
    public static String LOG_ID = "СИСТЕМА";
    
    /**
     * Current dir variable
     */
    public static String CurrentDirectory = System.getProperty("user.dir");
    
    /**
     * Log file object
     */
    private static java.io.File logFile;
    
    /**
     * Ribbon main configuration
     */
    public static java.util.Properties mainConfig;
    
    /**
     * System states enumeration
     */
    public static enum SYS_STATES {
        
        /**
         * Initialization state.<br>
         * <b>LIMIT:</b> login, posting, IO operations.
         */
        INIT,
        
        /**
         * System is ready to recieve messages.<br>
         * <b>NO LIMIT</b>.
         */
        READY,
        
        /**
         * I/O subsystem emergency state.<br>
         * <b>LIMIT:</b> some IO operations.
         */
        DIRTY,
        
        /**
         * Maintaince state.<br>
         * <b>LIMIT:</b> posting, IO operations.
         */
        MAINTAINING,
        
        /**
         * Closing state.<br>
         * <b>LIMIT:</b> login, posting, IO operations.
         */
        CLOSING
    }
    
    /**
     * Current system state variable
     */
    public static SYS_STATES CURR_STATE = null;
    
    /**
     * List of IO modules strings with errors.
     */
    public static java.util.ArrayList<String> DIRTY_LIST = new java.util.ArrayList<>();
    
    /**
     * Lock for system status concurent operations.
     */
    protected static Object DIRTY_LOCK = new Object();
    
    /**
     * Is system controled by administrator control console
     */
    public static Boolean CONTROL_IS_PRESENT = false;
    
    /** SYSTEM VARIABLES **/
    
    /**
     * Path to Ribbon base.
     */
    public static String BASE_PATH;
    
    /**
     * Allow attachments switch <b>[not yet implemented]</b>.
     */
    public static Boolean BASE_ALLOW_ATTACHMENTS;
    
    /**
     * Version of the server.
     */
    public static String RIBBON_VER = "a2";
    
    /**
     * Port number for listening.
     */
    public static Integer NETWORK_PORT;
    
    /**
     * Allow remote connection (not only localhost) switch.
     */
    public static Boolean NETWORK_ALLOW_REMOTE;
    
    /**
     * Network connections limit variable.
     */
    public static Integer NETWORK_MAX_CONNECTIONS;
    
    /**
     * Cache switch <b>[not yet implemented]</b>.
     */
    public static Boolean CACHE_ENABLED;
    
    /**
     * Size of cache <b>[not yet implemented]</b>.
     */
    public static Integer CACHE_SIZE;
    
    /**
     * Defalut ALL group permissions.
     */
    public static String ACCESS_ALL_MASK;
    
    /**
     * Allow to login user to more than one session.
     */
    public static Boolean ACCESS_ALLOW_MULTIPLIE_LOGIN;
    
    /**
     * Post system exception to specified directory.
     */
    public static Boolean DEBUG_POST_EXCEPTIONS;
    
    /**
     * Directory to post exception messages.
     */
    public static String DEBUG_POST_DIR;
    
    /**
     * Name of directory index file.
     */
    public static String DIR_INDEX_PATH = "dir.index";
    
    /**
     * Name of users index file.
     */
    public static String USERS_INDEX_PATH = "users.index";
    
    /**
     * Name of group index file.
     */
    public static String GROUPS_INDEX_PATH = "groups.index";
    
    /**
     * Name of messages index file.
     */
    public static String BASE_INDEX_PATH = "base.index";
    
    /**
     * Import quene object.
     */
    public static Import.Quene ImportQuene;
    
    /**
     * Export dispatcher object.
     */
    public static Export.Dispatcher ExportDispatcher;
    
    /**
     * System wrapper for system to libRibbonIO communication.
     */
    private static class IOWrapper extends Utils.SystemWrapper {

        @Override
        public void log(String logSource, Integer logLevel, String logMessage) {
            RibbonServer.logAppend(logSource, logLevel, logMessage);
        }

        @Override
        public void addMessage(MessageClasses.Message givenMessage) {
            Procedures.PROC_POST_MESSAGE(givenMessage);
            SessionManager.broadcast("RIBBON_UCTL_LOAD_INDEX:" + givenMessage.toCsv(), RibbonProtocol.CONNECTION_TYPES.CLIENT);
        }

        @Override
        public void registerPropertyName(String givenName) {
            Boolean result = MessageClasses.MessageProperty.Types.registerTypeIfNotExist(givenName);
            if (result) {
                this.log(IOControl.IMPORT_LOGID, 2, "зареєстровано новий тип ознак '" + givenName + "'");
            }
        }

        @Override
        public String getDate() {
            return RibbonServer.getCurrentDate();
        }

        @Override
        public String getProperty(String key) {
            return RibbonServer.mainConfig.getProperty(key);
        }

        @Override
        public void enableDirtyState(String moduleType, String moduleScheme, String modulePrint) {
            String moduleString = moduleType + ":" + modulePrint;
            if (RibbonServer.DIRTY_LIST.contains(moduleString)) {
                return;
            }
            if (RibbonServer.DIRTY_LIST.isEmpty()) {
                RibbonServer.logAppend(RibbonServer.LOG_ID, 2, "модуль " + moduleType + " за схемою " + moduleScheme + " отримав помилку при роботі.");
                RibbonServer.logAppend(RibbonServer.LOG_ID, 1, "система переходить у \'брудний\' режим!");
                //TODO add admin remote notification of such event
            }
            synchronized (RibbonServer.DIRTY_LOCK) {
                RibbonServer.CURR_STATE = RibbonServer.SYS_STATES.DIRTY;
                RibbonServer.DIRTY_LIST.add(moduleType + ":" + modulePrint);
            }
        }

        @Override
        public void disableDirtyState(String moduleType, String moduleScheme, String modulePrint) {
            String moduleString = moduleType + ":" + modulePrint;
            synchronized (RibbonServer.DIRTY_LOCK) {
                if (!RibbonServer.DIRTY_LIST.remove(moduleString)) {
                    RibbonServer.logAppend(RibbonServer.LOG_ID, 1, "запису " + moduleScheme + " немає у списку збійних модулів!");
                }
                if (RibbonServer.DIRTY_LIST.isEmpty() && RibbonServer.CURR_STATE == RibbonServer.SYS_STATES.DIRTY) {
                    RibbonServer.logAppend(RibbonServer.LOG_ID, 2, "система працює у штатному режимі");
                    RibbonServer.CURR_STATE = RibbonServer.SYS_STATES.READY;
                }
            }
        }

        @Override
        public void updateIndex(String givenIndex) {
            IndexReader.updateBaseIndex();
            SessionManager.broadcast("RIBBON_UCTL_UPDATE_INDEX:" + Messenger.getMessageEntryByIndex(givenIndex).toCsv(), RibbonProtocol.CONNECTION_TYPES.CLIENT);
        }
    }

    /**
     * Main server's function
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        createLogFile();
        logAppend(LOG_ID, -1, "Начало роботи системы...");
        logAppend(LOG_ID, 2, "Версія системи: " + RIBBON_VER);
        CURR_STATE = RibbonServer.SYS_STATES.INIT;
        setSystemVariables();
        logAppend(LOG_ID, 2, "налаштування бібліотек імпорту до системи");
        Utils.IOControl.initWrapper(new IOWrapper());
        ImportQuene = new Import.Quene(CurrentDirectory + "/imports/", BASE_PATH + "/import/");
        ExportDispatcher = new Export.Dispatcher(CurrentDirectory + "/exports/", BASE_PATH + "/export/");
        logAppend(LOG_ID, 3, "початок налаштування контролю доступу");
        AccessHandler.init();
        logAppend(LOG_ID, 3, "початок налаштування напрявків");
        Directories.init();
        logAppend(LOG_ID, 3, "зчитування індексу бази повідомленнь");
        Messenger.init();
        logAppend(LOG_ID, 3, "зчитування індексу сесій системи");
        SessionManager.init();
        CURR_STATE = RibbonServer.SYS_STATES.READY;
        Procedures.postInitMessage();
        ImportQuene.importRun();
        logAppend(LOG_ID, 2, "налаштування мережі");
        try {
            java.net.ServerSocket RibbonServSocket = new java.net.ServerSocket(NETWORK_PORT);
            logAppend(LOG_ID, 3, "система готова для прийому повідомлень");
            while (true) {
                java.net.Socket inSocket = RibbonServSocket.accept();
                if ((!inSocket.getInetAddress().getHostAddress().equals("127.0.0.1") && RibbonServer.NETWORK_ALLOW_REMOTE == false) || SessionManager.checkConnectionLimit() == true) {
                    inSocket.close();
                } else {
                    SessionManager.createNewSession(inSocket);
                }
            }
        } catch (java.io.IOException ex) {
            logAppend(LOG_ID, 0, "неможливо створити сервер!");
            System.exit(7);
        }
    }
    
    /**
     * Create Ribbon log file if it doesn't exist
     */
    private static void createLogFile() {
        logFile = new java.io.File(CurrentDirectory + "/ribbonserver.log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (java.io.IOException ex) {
                logFile = null;
                logAppend(LOG_ID, 0, "неможливо створити файл журналу!");
                System.exit(1);
            }
        }
    }
    
    /**
     * Print formated messages to System.out<br>
     * <b>Message types:</b><br>
     * <ul>
     * <li>(<b>0</b>): critical error in application;</li>
     * <li>(<b>1</b>): handleable error in application;</li>
     * <li>(<b>2</b>): warning during execution;</li>
     * <li>(<b>3</b>): common information;</li>
     * </ul>
     * @param component component which call this method;
     * @param type message type (see below);
     * @param message string of text message;
     */
    public static synchronized void logAppend(String component, Integer type, String message) {
        String typeStr = "";
        switch (type) {
            case 0:
                typeStr = "критична помилка";
                break;
            case 1:
                typeStr = "помилка";
                break;
            case 2:
                typeStr = "попередження";
                break;
            case 3:
                typeStr = "повідомлення";
                break;
        }
        String compiledMessage = getCurrentDate() + " ["+ component + "] " + typeStr + ">> '" + message + "';";
        System.out.println(compiledMessage);
        if (CONTROL_IS_PRESENT == true) {
            SessionManager.broadcast(compiledMessage, RibbonProtocol.CONNECTION_TYPES.CONTROL);
        }
        if (logFile != null) {
            try (java.io.FileWriter logWriter = new java.io.FileWriter(logFile, true)) {
                logWriter.write(compiledMessage + "\n");
                logWriter.close();
            } catch (Exception ex) {
                logFile = null;
                logAppend(LOG_ID, 0, "неможливо записати файл журналу!");
                System.exit(1);
            }
        }
    }
    
    /**
     * Get current date with default date format
     * @return current date
     */
    public static String getCurrentDate() {
        java.util.Date now = new java.util.Date();
        String strDate = dateFormat.format(now);
        return strDate;
    }
    
    /**
     * Read system variables from properties file<br>
     * and set them to local variables;
     */
    private static void setSystemVariables() {
        mainConfig = new java.util.Properties();
        
        //Reading properties file
        try {
            mainConfig.load(new java.io.FileInputStream(new java.io.File(CurrentDirectory + "/server.properties")));
        } catch (java.io.FileNotFoundException ex) {
            logAppend(LOG_ID, 0, "у робочої директорії немає файлу конфігурації системи!"
                    + "\nРобоча директорія: " + CurrentDirectory);
            System.exit(2);
        } catch (java.io.IOException ex) {
            logAppend(LOG_ID, 0, "неможливо прочитати файл конфігурації системи!");
            System.exit(2);
        }
        
        //Setting base variables
        try {
            BASE_PATH = new String(mainConfig.getProperty("base_path").getBytes("ISO-8859-1"), "UTF-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            RibbonServer.logAppend(LOG_ID, 1, "неможливо визначити шлях до бази!");
            System.exit(3);
        }
        BASE_ALLOW_ATTACHMENTS = mainConfig.getProperty("base_allow_attachments").equals("0") ? false : true;
        
        //Setting network variables
        NETWORK_PORT = Integer.valueOf(mainConfig.getProperty("networking_port"));
        if (mainConfig.getProperty("networking_allow_remote").equals("0")) {
            RibbonServer.NETWORK_ALLOW_REMOTE = false;
        } else {
            RibbonServer.NETWORK_ALLOW_REMOTE = true;
        }
        NETWORK_MAX_CONNECTIONS = Integer.valueOf(mainConfig.getProperty("networking_max_connections"));
        
        //Setting cache variables
        CACHE_ENABLED = mainConfig.getProperty("cache_enabled").equals("0") ? false : true;
        CACHE_SIZE = Integer.valueOf(mainConfig.getProperty("cache_size"));
        
        //Setting access variables
        ACCESS_ALL_MASK = mainConfig.getProperty("access_all_mask");
        ACCESS_ALLOW_MULTIPLIE_LOGIN = mainConfig.getProperty("access_allow_multiplie_login").equals("0") ? false : true;
        
        //Setting debug variables
        DEBUG_POST_EXCEPTIONS = mainConfig.getProperty("debug_post_exceptions").equals("0") ? false : true;
        try {
            DEBUG_POST_DIR = new String(mainConfig.getProperty("debug_post_dir").getBytes("ISO-8859-1"), "UTF-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            RibbonServer.logAppend(LOG_ID, 1, "неможливо визначити напрямок реєстрації помилок!");
            if (DEBUG_POST_EXCEPTIONS == true) {
                System.exit(3);
            }
        }
        
        logAppend(LOG_ID, 3, 
                "початкова конфігурація завершена.\n" + 
                "Шлях до бази: " + BASE_PATH + "\n" +
                (RibbonServer.BASE_ALLOW_ATTACHMENTS ? "Зберігання файлів увімкнено." : "") + "\n" +
                (RibbonServer.CACHE_ENABLED ? "Кешування бази увімкнено.\nРозмір кешу: " + RibbonServer.CACHE_SIZE + "\n" : "") +
                (RibbonServer.NETWORK_ALLOW_REMOTE ? "Мережевий доступ увімкнено.\n"
                + "Мережевий порт:" + RibbonServer.NETWORK_PORT + "\n"
                + (RibbonServer.NETWORK_MAX_CONNECTIONS == -1 ? "Без ліміту з'єднань." : "Кількість з'єднань: " 
                + RibbonServer.NETWORK_MAX_CONNECTIONS) + "\n" : "Мережевий доступ заблоковано.\n")
                + (RibbonServer.ACCESS_ALLOW_MULTIPLIE_LOGIN ? "Дозволена неодноразова авторізація.\n" : "Неодноразова авторізація заблокована.\n")
                + "Маска для системної категорії доступу ALL:" + RibbonServer.ACCESS_ALL_MASK + "\n"
                + (RibbonServer.DEBUG_POST_EXCEPTIONS ? "Режим дебагінгу активовано.\nНапрямок реєстрації помилок: " + RibbonServer.DEBUG_POST_DIR : "")
                );
    }
    
    /**
     * Get hash sum of given string.
     * @param givenStr given string;
     * @return md5 hash sum representation;
     */
    public static String getHash(String givenStr) {
        StringBuffer hexString = new StringBuffer();
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            md.update(givenStr.getBytes());
            byte[] hash = md.digest();
            for (int i = 0; i < hash.length; i++) {
                if ((0xff & hash[i]) < 0x10) {
                    hexString.append("0"
                            + Integer.toHexString((0xFF & hash[i])));
                } else {
                    hexString.append(Integer.toHexString(0xFF & hash[i]));
                }
            }
        } catch (Exception ex) {
            RibbonServer.logAppend(LOG_ID, 1, "Неможливо добути хеш-суму!");
        }
        return hexString.toString();
    }
}
