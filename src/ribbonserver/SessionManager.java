/*
 * This code is distributed under terms of GNU GPLv2.
 * *See LICENSE file.
 * ©UKRINFORM 2011-2012
 */

package ribbonserver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Network sessions store class
 * @author Stanislav Nepochatov
 */
public class SessionManager {
    
    private static String LOG_ID = "Менеджер мережевих сесій";
    
    /**
     * Arraylist with active network sessions
     */
    private java.util.ArrayList<SessionManager.SessionThread> sessionsStore = new java.util.ArrayList<>();
    
    /**
     * Single client session class
     */
    public class SessionThread extends Thread {
        
        /**
         * User name of this session
         */
        public String USER_NAME;
        
        /**
         * Short session description
         */
        public String SESSION_TIP;
        
        /**
         * Is session alive
         */
        public Boolean isAlive = false;
        
        /**
         * Session network socket
         */
        private java.net.Socket SessionSocket;
        
        /**
         * Input stream from client
         */
        public java.io.BufferedReader inStream;
        
        /**
         * Output writer to client
         */
        public java.io.PrintWriter outStream;
        
        /**
         * Protocol handler, parser and executor
         */
        private RibbonProtocol ProtocolHandler;
        
        SessionThread(java.net.Socket givenSocket) {
            SessionSocket = givenSocket;
            try {
                inStream = new java.io.BufferedReader(new java.io.InputStreamReader(SessionSocket.getInputStream(), "UTF-8"));
                outStream = new java.io.PrintWriter(SessionSocket.getOutputStream(), true);
            } catch (java.io.IOException ex) {
                RibbonServer.logAppend(LOG_ID, 1, "неможливо створити потоки для мережевого сокета (" + SessionSocket.getInetAddress().getHostAddress() + ")");
            } finally {
                RibbonServer.logAppend(LOG_ID, 3, "додана нова мережева сессія (" + SessionSocket.getInetAddress().getHostAddress() + ")");
                ProtocolHandler = new RibbonProtocol(this);
                this.isAlive = true;
            }
        }
        
        @Override
        public void run() {
            String inputLine, outputLine;
            this.setSessionName();
            try {
                while (this.isAlive == true) {
                    inputLine = inStream.readLine();
                    String answer = this.ProtocolHandler.process(inputLine);
                    if (answer.equals("COMMIT_CLOSE:")) {
                        isAlive = false;
                    }
                    this.outStream.println(answer);
                    if (this.ProtocolHandler.BROADCAST_TAIL != null) {
                        broadcast(this.ProtocolHandler.BROADCAST_TAIL, this.ProtocolHandler.BROADCAST_TYPE);
                        this.ProtocolHandler.BROADCAST_TAIL = null;
                        this.ProtocolHandler.BROADCAST_TYPE = null;
                    }
                }
                this.inStream.close();
                this.outStream.close();
                this.SessionSocket.close();
                RibbonServer.logAppend(LOG_ID, 3, "мережеву сесію зачинено (" + SessionSocket.getInetAddress().getHostAddress() + ")");
                RibbonServer.sessionObj.closeSession(this);
            } catch (java.lang.NullPointerException ex) {
                RibbonServer.logAppend(LOG_ID, 1, "з'єднання аварійно разірване!");
                RibbonServer.sessionObj.closeSession(this);
            } catch (java.io.IOException ex) {
                RibbonServer.logAppend(LOG_ID, 1, "неможливо прочитати дані з сокету (" + SessionSocket.getInetAddress().getHostAddress() + ")");
                RibbonServer.sessionObj.closeSession(this);
            }
        }
        
        /**
         * Set name of session thread
         */
        public void setSessionName() {
            this.SESSION_TIP = "[" + this.USER_NAME + "] на " + this.SessionSocket.getInetAddress().getHostName();
        }
        
        /**
         * Set reader encoding
         */
        public void setReaderEncoding(String charsetName) {
            try {
                this.inStream = new java.io.BufferedReader(new java.io.InputStreamReader(SessionSocket.getInputStream(), charsetName));
            } catch (java.io.UnsupportedEncodingException ex) {
                RibbonServer.logAppend(LOG_ID, 1, "неможливо встановити кодову сторінку!");
            } catch (java.io.IOException ex) {
                RibbonServer.logAppend(LOG_ID, 1, "неможливо прочитати дані з сокету (" + SessionSocket.getInetAddress().getHostAddress() + ")");
            }
        }
    }

    /**
     * Create new session and add it into session list;
     * @param givenSocket socket to open session;
     */
    public void createNewSession(java.net.Socket givenSocket) {
        SessionManager.SessionThread createdThread = new SessionManager.SessionThread(givenSocket);
        if (createdThread.isAlive) {
            this.sessionsStore.add(createdThread);
            createdThread.start();
        }
    }
    
    /**
     * Close session and delete it from sessaion array;
     * @param givenSession sessionb to close;
     */
    public void closeSession(SessionManager.SessionThread givenSession) {
        if (!givenSession.isAlive) {
            this.sessionsStore.remove(givenSession);
        }
    }
    
    /**
     * Broadcast message to all users
     * @param message a single line message
     */
    public void broadcast(String message, RibbonProtocol.CONNECTION_TYPES type) {
        java.util.ListIterator<SessionThread> sessionIter = this.sessionsStore.listIterator();
        while (sessionIter.hasNext()) {
            SessionThread currSession = sessionIter.next();
            if (currSession.ProtocolHandler.CURR_TYPE == type) {
                currSession.outStream.println(message);
            }
        }
    }
    
    /**
     * Check if there is other control connection for the system
     * @param closingControlThread thread which going to close
     * @return result of checking
     */
    public Boolean hasOtherControl(SessionThread closingControlThread) {
        java.util.ListIterator<SessionThread> sessionIter = this.sessionsStore.listIterator();
        while (sessionIter.hasNext()) {
            SessionThread currSession = sessionIter.next();
            if (currSession.ProtocolHandler.CURR_TYPE == RibbonProtocol.CONNECTION_TYPES.CONTROL && !currSession.equals(closingControlThread)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check server connection limit.
     * @return true if limit achieved/fals if not;
     */
    public Boolean checkConnectionLimit() {
        if (RibbonServer.NETWORK_MAX_CONNECTIONS != -1 && this.sessionsStore.size() == RibbonServer.NETWORK_MAX_CONNECTIONS) {
            RibbonServer.logAppend(LOG_ID, 1, "досягнуто ліміту з'єднань (" + RibbonServer.NETWORK_MAX_CONNECTIONS + ")");
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Find out is user is already logined in system.
     * @param givenName name of user to search;
     * @return true if user is already logined.
     */
    public Boolean isAlreadyLogined(String givenName) {
        for (Iterator<SessionThread> it = this.sessionsStore.iterator(); it.hasNext();) {
            SessionThread iterSess = it.next();
            if (iterSess.USER_NAME == null) {
                continue;
            }
            if (iterSess.USER_NAME.equals(givenName)) {
                return true;
            }
        }
        return false;
    }
}
