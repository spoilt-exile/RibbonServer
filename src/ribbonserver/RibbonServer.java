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
     * Default time and date format for server.
     * @since RibbonServer a1
     */
    private static java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("HH:mm:ss dd.MM.yyyy");
    
    /**
     * ID of this component or object for loging.
     * @since RibbonServer a1
     */
    public static String LOG_ID = "СИСТЕМА";
    
    /**
     * Current dir variable.
     * @since RibbonServer a1
     */
    public static String CurrentDirectory = System.getProperty("user.dir");
    
    /**
     * Log file object.
     * @since RibbonServer a1
     */
    private static java.io.File logFile;
    
    /**
     * RibbonServer main configuration.
     * @since RibbonServer a1
     */
    public static java.util.Properties mainConfig;
    
    /**
     * System states enumeration.
     * @since RibbonServer a1
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
     * Current system state variable.
     * @since RibbonServer a1
     */
    public static SYS_STATES CURR_STATE = null;
    
    /**
     * List of IO modules strings with errors.
     * @since RibbonServer a2
     */
    public static java.util.ArrayList<String> DIRTY_LIST = new java.util.ArrayList<>();
    
    /**
     * Lock for system status concurent operations.
     * @since RibbonServer a2
     */
    protected static final Object DIRTY_LOCK = new Object();
    
    /**
     * Is system controled by administrator control console.
     * @since RibbonServer a1
     */
    public static Boolean CONTROL_IS_PRESENT = false;
    
    /** SYSTEM VARIABLES **/
    
    /**
     * Path to Ribbon base.
     * @since RibbonServer a1
     */
    public static String BASE_PATH;
    
    /**
     * Allow attachments switch <b>[not yet implemented]</b>.
     * @since RibbonServer a2
     */
    public static Boolean BASE_ALLOW_ATTACHMENTS;
    
    /**
     * Major server version id.
     * @since RibbonServer a1
     */
    public static final String RIBBON_MAJOR_VER = "a2";
    
    /**
     * Minor server version postfix.
     * @since RibbonServer a2
     */
    public static final String RIBBON_MINOR_VER = ".2";
    
    /**
     * Devel server version postfix.
     * @since RibbonServer a2
     */
    public static final String RIBBON_DEVEL_VER = "";
    
    /**
     * Port number for listening.
     * @since RibbonServer a1
     */
    public static Integer NETWORK_PORT;
    
    /**
     * Allow remote connection (not only localhost) switch.
     * @since RibbonServer a2
     */
    public static Boolean NETWORK_ALLOW_REMOTE;
    
    /**
     * Network connections limit variable.
     * @since RibbonServer a2
     */
    public static Integer NETWORK_MAX_CONNECTIONS;
    
    /**
     * Cache switch <b>[not yet implemented]</b>.
     * @since RibbonServer a2
     */
    public static Boolean CACHE_ENABLED;
    
    /**
     * Size of cache <b>[not yet implemented]</b>.
     * @since RibbonServer a2
     */
    public static Integer CACHE_SIZE;
    
    /**
     * Defalut ALL group permissions.
     * @since RibbonServer a2
     */
    public static String ACCESS_ALL_MASK;
    
    /**
     * Constant default ALL group permission (for validation).
     * @since RibbonServer a2
     */
    public static final String VAL_ACCESS_ALL_MASK = "100";
    
    /**
     * Allow to login user to more than one session.
     * @since RibbonServer a2
     */
    public static Boolean ACCESS_ALLOW_MULTIPLIE_LOGIN;
    
    /**
     * Allow to login by previous session hash.
     * @since RibbonServer a2
     */
    public static Boolean ACCESS_ALLOW_SESSIONS;
    
    /**
     * Max count of session hsah reusing.
     * @since RibbonServer a2
     */
    public static Integer ACCESS_SESSION_MAX_COUNT;
    
    /**
     * Allow to use remote connection mode.
     * @since RibbonServer a2
     */
    public static Boolean ACCESS_ALLOW_REMOTE;
    
    /**
     * Group of user which is allowed to create remote connections.
     * @since RibbonServer a2
     */
    public static String ACCESS_REMOTE_GROUP;
    
    /**
     * Post init message flag.
     * @since RibbonServer a2
     */
    public static Boolean OPT_POST_INIT;
    
    /**
     * Create text reports during startup.
     * @since RibbonServer a2
     */
    public static Boolean OPT_CREATE_REPORTS;
    
    /**
     * Enable/disable import and export operations.
     * @since RibbonServer a2
     */
    public static Boolean IO_ENABLED;
    
    /**
     * Ignore all attempts to set dirty status on the server.<br>
     * <b>WARNING!</b> This settings is dangerous for production installations!<br>
     * <b>ONLY FOR TEST PURPOSES!<b>
     * @since RibbonServer a2
     */
    public static Boolean IO_IGNORE_DIRTY;
    
    /**
     * Import emergency directory (in case of bad validation).
     * @since RibbonServer a2
     */
    public static String IO_IMPORT_EM_DIR;
    
    /**
     * Post system exception to specified directory.
     * @since RibbonServer a2
     */
    public static Boolean DEBUG_POST_EXCEPTIONS;
    
    /**
     * Directory to post exception messages.
     * @since RibbonServer a2
     */
    public static String DEBUG_POST_DIR;
    
    /**
     * Name of directory index file.
     * @since RibbonServer a1
     */
    public static String DIR_INDEX_PATH = "dir.index";
    
    /**
     * Name of users index file.
     * @since RibbonServer a1
     */
    public static String USERS_INDEX_PATH = "users.index";
    
    /**
     * Name of group index file.
     * @since RibbonServer a2
     */
    public static String GROUPS_INDEX_PATH = "groups.index";
    
    /**
     * Name of messages index file.
     * @since RibbonServer a1
     */
    public static String BASE_INDEX_PATH = "base.index";
    
    /**
     * Import quene object.
     * @since RibbonServer a2
     */
    public static Import.Quene ImportQuene;
    
    /**
     * Export dispatcher object.
     * @since RibbonServer a2
     */
    public static Export.Dispatcher ExportDispatcher;
    
    /**
     * System wrapper for system to libRibbonIO communication.
     * @since RibbonServer a2
     */
    private static class IOWrapper extends Utils.SystemWrapper {

        @Override
        public void log(String logSource, Integer logLevel, String logMessage) {
            RibbonServer.logAppend(logSource, logLevel, logMessage);
        }

        @Override
        public void addMessage(String schemeName, String typeName, MessageClasses.Message givenMessage) {
            for (int index = 0; index < givenMessage.DIRS.length; index++) {
                if (Directories.getDirPath(givenMessage.DIRS[index]) == null) {
                    this.log("ОБГОРТКА", 1, "схема " + schemeName + " (" + typeName + ") посилається на неіснуючий напрямок " + givenMessage.DIRS[index]);
                    givenMessage.DIRS = new String[] {IO_IMPORT_EM_DIR};
                    break;
                }
            }
            Procedures.PROC_POST_MESSAGE(givenMessage);
            SessionManager.broadcast("RIBBON_UCTL_LOAD_INDEX:" + givenMessage.toCsv(), RibbonProtocol.CONNECTION_TYPES.CLIENT);
        }

        @Override
        public void registerPropertyName(String givenName) {
            Boolean result = MessageClasses.MessageProperty.Types.registerTypeIfNotExist(givenName);
            if (result) {
                this.log(IOControl.LOG_ID, 2, "зареєстровано новий тип ознак '" + givenName + "'");
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
            if (RibbonServer.IO_IGNORE_DIRTY) {
                return;
            }
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
            if (RibbonServer.IO_IGNORE_DIRTY) {
                return;
            }
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

        @Override
        public void postException(String desc, Throwable ex) {
            Procedures.postException(desc, ex);
        }
    }

    /**
     * Main server's function
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        createLogFile();
        logAppend(LOG_ID, -1, "Начало роботи системы...");
        logAppend(LOG_ID, 2, "Версія системи: " + RIBBON_MAJOR_VER + RIBBON_MINOR_VER + RIBBON_DEVEL_VER);
        CURR_STATE = RibbonServer.SYS_STATES.INIT;
        setSystemVariables();
        if (IO_ENABLED) {
            logAppend(LOG_ID, 2, "налаштування бібліотек імпорту до системи");
            Utils.IOControl.initWrapper(new IOWrapper());
            IOControl.registerPathes(BASE_PATH + "/import/", BASE_PATH + "/export/");
            ImportQuene = new Import.Quene(CurrentDirectory + "/imports/", BASE_PATH + "/import/");
            ExportDispatcher = new Export.Dispatcher(CurrentDirectory + "/exports/", BASE_PATH + "/export/");
            IOControl.registerImport(ImportQuene);
            IOControl.registerExport(ExportDispatcher);
        }
        logAppend(LOG_ID, 3, "початок налаштування контролю доступу");
        AccessHandler.init();
        logAppend(LOG_ID, 3, "початок налаштування напрявків");
        Directories.init();
        logAppend(LOG_ID, 3, "зчитування індексу бази повідомленнь");
        Messenger.init();
        logAppend(LOG_ID, 3, "зчитування індексу сесій системи");
        SessionManager.init();
        CURR_STATE = RibbonServer.SYS_STATES.READY;
        if (OPT_CREATE_REPORTS) {
            Directories.dumpTree();
        }
        if (OPT_POST_INIT) {
            Procedures.postInitMessage();
        }
        if (IO_ENABLED) {
            ImportQuene.importRun();
        }
        logAppend(LOG_ID, 3, "проводиться перевірка конфігурації");
        validateSystemVariables();
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
     * Create Ribbon log file if it doesn't exist.
     * @since RibbonServer a1
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
     * @since RibbonServer a1
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
     * Get current date with default date format.
     * @return current date
     * @since RibbonServer a1
     */
    public static String getCurrentDate() {
        java.util.Date now = new java.util.Date();
        String strDate = dateFormat.format(now);
        return strDate;
    }
    
    /**
     * Read system variables from properties file 
     * and set them to local variables.
     * @since RibbonServer a1
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
        //BASE_ALLOW_ATTACHMENTS = mainConfig.getProperty("base_allow_attachments").equals("0") ? false : true;
        
        //Setting network variables
        NETWORK_PORT = Integer.valueOf(mainConfig.getProperty("networking_port"));
        if (mainConfig.getProperty("networking_allow_remote").equals("0")) {
            RibbonServer.NETWORK_ALLOW_REMOTE = false;
        } else {
            RibbonServer.NETWORK_ALLOW_REMOTE = true;
        }
        NETWORK_MAX_CONNECTIONS = Integer.valueOf(mainConfig.getProperty("networking_max_connections"));
        
        //Setting cache variables
        //CACHE_ENABLED = mainConfig.getProperty("cache_enabled").equals("0") ? false : true;
        //CACHE_SIZE = Integer.valueOf(mainConfig.getProperty("cache_size"));
        
        //Setting access variables
        ACCESS_ALL_MASK = mainConfig.getProperty("access_all_mask");
        ACCESS_ALLOW_MULTIPLIE_LOGIN = mainConfig.getProperty("access_allow_multiplie_login").equals("0") ? false : true;
        ACCESS_ALLOW_SESSIONS = mainConfig.getProperty("access_enable_sessions").equals("0") ? false : true;
        ACCESS_SESSION_MAX_COUNT = Integer.valueOf(mainConfig.getProperty("access_session_count_max"));
        ACCESS_ALLOW_REMOTE = mainConfig.getProperty("access_allow_remote").equals("0") ? false : true;
        try {
            ACCESS_REMOTE_GROUP = new String(mainConfig.getProperty("access_remote_group").getBytes("ISO-8859-1"), "UTF-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            RibbonServer.logAppend(LOG_ID, 1, "неможливо визначити напрямок реєстрації помилок!");
            System.exit(3);
        }
        
        //Setting optional variables
        OPT_POST_INIT = mainConfig.getProperty("opt_post_init").equals("0") ? false : true;
        OPT_CREATE_REPORTS = mainConfig.getProperty("opt_create_reports").equals("0") ? false : true;
        
        //Setting IO control varibales
        IO_ENABLED = mainConfig.getProperty("io_enabled").equals("0") ? false : true;
        IO_IGNORE_DIRTY = mainConfig.getProperty("io_ignore_dirty").equals("0") ? false : true;
        try {
            IO_IMPORT_EM_DIR = new String(mainConfig.getProperty("io_import_em_dir").getBytes("ISO-8859-1"), "UTF-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            RibbonServer.logAppend(LOG_ID, 1, "неможливо визначити напрямок реєстрації помилок!");
            System.exit(3);
        }
        //Integer loc_IO_EXPORT_QUENE_SIZE = ACCESS_SESSION_MAX_COUNT = Integer.valueOf(mainConfig.getProperty("io_export_quene_size"));
        //Integer loc_IO_EXPORT_ERRQUENE_SIZE = ACCESS_SESSION_MAX_COUNT = Integer.valueOf(mainConfig.getProperty("io_export_errquene_size"));
        
        //Setting debug variables
        DEBUG_POST_EXCEPTIONS = mainConfig.getProperty("debug_post_exceptions").equals("0") ? false : true;
        try {
            DEBUG_POST_DIR = new String(mainConfig.getProperty("debug_post_dir").getBytes("ISO-8859-1"), "UTF-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            RibbonServer.logAppend(LOG_ID, 1, "неможливо визначити напрямок реєстрації помилок!");
            System.exit(3);
        }
        
        logAppend(LOG_ID, 3, 
                "початкова конфігурація завершена.\n" + 
                "Шлях до бази: " + BASE_PATH + "\n" +
                //(RibbonServer.BASE_ALLOW_ATTACHMENTS ? "Зберігання файлів увімкнено." : "") + "\n" +
                //(RibbonServer.CACHE_ENABLED ? "Кешування бази увімкнено.\nРозмір кешу: " + RibbonServer.CACHE_SIZE + "\n" : "") +
                (RibbonServer.NETWORK_ALLOW_REMOTE ? "Мережевий доступ увімкнено.\n"
                + "Мережевий порт:" + RibbonServer.NETWORK_PORT + "\n"
                + (RibbonServer.NETWORK_MAX_CONNECTIONS == -1 ? "Без ліміту з'єднань." : "Кількість з'єднань: " 
                + RibbonServer.NETWORK_MAX_CONNECTIONS) + "\n" : "Мережевий доступ заблоковано.\n")
                + (RibbonServer.ACCESS_ALLOW_MULTIPLIE_LOGIN ? "Дозволена неодноразова авторізація.\n" : "Неодноразова авторізація заблокована.\n")
                + "Маска для системної категорії доступу ALL:" + RibbonServer.ACCESS_ALL_MASK + "\n"
                + (RibbonServer.ACCESS_ALLOW_SESSIONS ? "Сесії дозволені.\nКількість споживань сесії:" + RibbonServer.ACCESS_SESSION_MAX_COUNT + "\n" : "")
                + (RibbonServer.ACCESS_ALLOW_REMOTE ? "Видалений режим дозволено.\nГрупа для видаленого режиму:" + RibbonServer.ACCESS_REMOTE_GROUP + "\n" : "")
                + (RibbonServer.OPT_POST_INIT ? "Автоматичний випуск тестового повідомлення увімкнено.\n" : "")
                + (RibbonServer.OPT_CREATE_REPORTS ? "Створення рапортів увімкнено.\n" : "")
                + (RibbonServer.IO_ENABLED ? 
                    (
                    "Операції іморту/експорту увікнені.\n" +
                    (RibbonServer.IO_IGNORE_DIRTY ? "[УВАГА!] Режим ігнорування помилок увікнено!\n" : "") + 
                    "Аварійний напрямок:" + RibbonServer.IO_IMPORT_EM_DIR + "\n"
                    //"Розмір черги експорту:" + loc_IO_EXPORT_QUENE_SIZE + "\n" +
                    //"Розмір черги помилок експорту:" + loc_IO_EXPORT_ERRQUENE_SIZE + "\n"
                    ):
                    ""
                )
                + (RibbonServer.DEBUG_POST_EXCEPTIONS ? "Режим дебагінгу активовано.\nНапрямок реєстрації помилок: " + RibbonServer.DEBUG_POST_DIR : "")
                );
    }
    
    /**
     * Check correctness of some system configurations.<br>
     * <b>WARNING!</b> May stop system with error.
     * @since RibbonServer a2
     */
    private static void validateSystemVariables() {
        
        //Set constant access ALL group mask if corrupted;
        Boolean MASK_VALID = true;
        for (char curr: ACCESS_ALL_MASK.toCharArray()) {
            if (curr == '0' || curr == '1') {
                continue;
            } else {
                MASK_VALID = false;
                break;
            }
        }
        if (ACCESS_ALL_MASK.length() > 3 && !MASK_VALID) {
            logAppend(LOG_ID, 1, "Невірне налаштування маски доступу ALL (" + ACCESS_ALL_MASK + ")");
            ACCESS_ALL_MASK = VAL_ACCESS_ALL_MASK;
        }
        
        //Turn off sessions if session use max count is lower or equal to 0;
        if (ACCESS_ALLOW_SESSIONS && ACCESS_SESSION_MAX_COUNT <= 0) {
            logAppend(LOG_ID, 1, "Невірне налаштування кешу (" + ACCESS_SESSION_MAX_COUNT + ")");
            ACCESS_ALLOW_SESSIONS = false;
        }
        
        //EXIT if group doesn't exist
        if (ACCESS_ALLOW_REMOTE && !AccessHandler.isGroupExisted(ACCESS_REMOTE_GROUP)) {
            logAppend(LOG_ID, 0, "помилка видаленого режиму: групи " + ACCESS_REMOTE_GROUP + " не існує");
            logAppend(LOG_ID, 3, "Перевірьте параметр access_remote_group у файлі конфігурації");
            System.exit(3);
        }
        
        //I/O section check
        if (IO_ENABLED) {
            
            //EXIT if emergency dir set incorrect
            if (Directories.getDirPath(IO_IMPORT_EM_DIR) == null) {
                logAppend(LOG_ID, 0, "помилка налаштування імпорту: напрямку " + IO_IMPORT_EM_DIR + " не існує");
                logAppend(LOG_ID, 3, "Перевірьте параметр io_import_em_dir у файлі конфігурації");
                System.exit(3);
            }
            
        }
        
        //EXIT if debug directory doesn't exist
        if (DEBUG_POST_EXCEPTIONS && Directories.getDirPath(DEBUG_POST_DIR) == null) {
            logAppend(LOG_ID, 0, "помилка дебагінгу: напрямку " + DEBUG_POST_DIR + " не існує");
            logAppend(LOG_ID, 3, "Перевірьте параметр debug_post_dir у файлі конфігурації");
            System.exit(3);
        }
    }
    
    /**
     * Get hash sum of given string.
     * @param givenStr given string;
     * @return md5 hash sum representation;
     * @since RibbonServer a2
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
