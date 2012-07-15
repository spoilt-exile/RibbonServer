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
     * Groups handle object
     */
    public static Groups groupObj;
    
    /**
     * Users handle object
     */
    public static Users userObj;
    
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
    
    public static String RIBBON_VER = "a2";
    
    //public static String INDEX_PATH;
    
    public static Integer PORT;
    
    public static Boolean ALLOW_REMOTE;
    
    public static enum ANON_MODES {OFFLINE, NORMAL, OVERRIDE};
    
    public static ANON_MODES CURR_ANON_MODE;
    
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
        logAppend(LOG_ID, 3, "початок налаштування груп доступу");
        groupObj = new Groups();
        logAppend(LOG_ID, 3, "початок налаштування користувачів");
        userObj = new Users();
        logAppend(LOG_ID, 3, "початок налаштування напрявків");
        dirObj = new Directories();
        logAppend(LOG_ID, 3, "зчитування індексу бази повідомленнь");
        messageObj = new Messenger();
        CURR_STATE = RibbonServer.SYS_STATES.READY;
        Procedures.postInitMessage();
        logAppend(LOG_ID, 2, "налаштування мережі");
        sessionObj = new SessionManager();
        try {
            java.net.ServerSocket RibbonServSocket = new java.net.ServerSocket(PORT);
            logAppend(LOG_ID, 3, "система готова для прийому повідомлень");
            while (true) {
                java.net.Socket inSocket = RibbonServSocket.accept();
                if (!inSocket.getInetAddress().getHostAddress().equals("127.0.0.1") && RibbonServer.ALLOW_REMOTE == false) {
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
            try {
                java.io.FileWriter logWriter = new java.io.FileWriter(logFile, true);
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
        try {
            BASE_PATH = new String(mainConfig.getProperty("base_path").getBytes("ISO-8859-1"), "UTF-8");
            //INDEX_PATH = new String(mainConfig.getProperty("index_path").getBytes("ISO-8859-1"), "UTF-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            RibbonServer.logAppend(LOG_ID, 1, "неможливо визначити шлях до бази!");
            System.exit(3);
        }
        PORT = Integer.valueOf(mainConfig.getProperty("port"));
        String RAW_ALLOW_REMOTE = mainConfig.getProperty("allow_remote");
        String REMOTE_MSG;
        if (RAW_ALLOW_REMOTE.equals("0")) {
            ALLOW_REMOTE = false;
            REMOTE_MSG = "Мережевий доступ вимкнено.";
        } else {
            //If ALLOW_REMOTE doesn't specified as false it will be true always
            ALLOW_REMOTE = true;
            REMOTE_MSG = "Мережевий доступ увімкнено.";
        }
        String RAW_ANON_MODE = mainConfig.getProperty("anon_mode");
        String ANON_MSG;
        if (RAW_ANON_MODE.equals("offline")) {
            CURR_ANON_MODE = ANON_MODES.OFFLINE;
            ANON_MSG = "Анонімний режим вимкнено.";
        } else if (RAW_ANON_MODE.equals("override")) {
            CURR_ANON_MODE = ANON_MODES.OVERRIDE;
            ANON_MSG = "Усі напрямки увімкнено як анонімні.";
        } else {
            //Default behavior
            CURR_ANON_MODE = ANON_MODES.NORMAL;
            ANON_MSG = "Анонімний режим увімкнено.";
        }
        logAppend(LOG_ID, 3, 
                "початкова конфігурація завершена.\n" + 
                "Шлях до бази: " + BASE_PATH + "\n" +
                //"Шлях до індекса: " + INDEX_PATH + "\n" +
                "Порт мережі: " + PORT + "\n" +
                REMOTE_MSG + "\n" + 
                ANON_MSG);
    }
}
