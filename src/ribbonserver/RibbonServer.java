/*
 * This code is distributed under terms of GNU GPLv2.
 * *See LICENSE file.
 * ©UKRINFORM 2011-2012
 */

package ribbonserver;

/**
 * Main Ribbon server class
 * @author Stanislav Nepochatov
 */
public class RibbonServer {
    
    /**
     * Directories handle object
     */
    public static Directories dirObj;
    
    /**
     * Messages and tags handle object
     */
    public static Messenger messageObj;
    
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
    public static String LOG_ID = "Укрінформ \"Стрічка\"";
    
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
        INIT,       //Initialication state
        READY,      //System is ready to recieve messages
        MAINTAINING,//System is under maintaining
        CLOSING     //System is going to shoutdown
    }
    
    /**
     * Current system state variable
     */
    public static SYS_STATES CURR_STATE = null;
    
    /**
     * Is system controled by administrator control console
     */
    public static Boolean CONTROL_IS_PRESENT = false;
    
    /** SYSTEM VARIABLES **/
    
    public static String BASE_PATH;
    
    public static Boolean BASE_ALLOW_ATTACHMENTS;
    
    public static String RIBBON_VER = "a2";
    
    public static Integer NETWORK_PORT;
    
    public static Boolean NETWORK_ALLOW_REMOTE;
    
    public static Integer NETWORK_MAX_CONNECTIONS;
    
    public static Boolean CACHE_ENABLED;
    
    public static Integer CACHE_SIZE;
    
    public static String ACCESS_ALL_MASK;
    
    public static Boolean ACCESS_ALLOW_MULTIPLIE_LOGIN;
    
    public static Boolean DEBUG_POST_EXCEPTIONS;
    
    public static String DEBUG_POST_DIR;
    
    public static String DIR_INDEX_PATH = "dir.index";
    
    public static String USERS_INDEX_PATH = "users.index";
    
    public static String GROUPS_INDEX_PATH = "groups.index";
    
    public static String BASE_INDEX_PATH = "base.index";

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
        logAppend(LOG_ID, 3, "початок налаштування контролю доступу");
        AccessHandler.init();
        logAppend(LOG_ID, 3, "початок налаштування напрявків");
        dirObj = new Directories();
        logAppend(LOG_ID, 3, "зчитування індексу бази повідомленнь");
        messageObj = new Messenger();
        CURR_STATE = RibbonServer.SYS_STATES.READY;
        Procedures.postInitMessage();
        logAppend(LOG_ID, 2, "налаштування мережі");
        sessionObj = new SessionManager();
        try {
            java.net.ServerSocket RibbonServSocket = new java.net.ServerSocket(NETWORK_PORT);
            logAppend(LOG_ID, 3, "система готова для прийому повідомлень");
            while (true) {
                java.net.Socket inSocket = RibbonServSocket.accept();
                if ((!inSocket.getInetAddress().getHostAddress().equals("127.0.0.1") && RibbonServer.NETWORK_ALLOW_REMOTE == false) || RibbonServer.sessionObj.checkConnectionLimit() == true) {
                    inSocket.close();
                } else {
                    RibbonServer.sessionObj.createNewSession(inSocket);
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
            RibbonServer.sessionObj.broadcast(compiledMessage, RibbonProtocol.CONNECTION_TYPES.CONTROL);
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
}
