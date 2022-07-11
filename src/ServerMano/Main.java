package ServerMano;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * localhost:8888/dir1?sort=desc
 * localhost:8888/dir1?sort=asc
 * <p>
 * dir=first
 * dir=last
 * dir2?sort=asc&dir=last
 */

public class Main {
    private static final String ROOT_DIR = "src/ServerMano/resources";
    private static final int PORT = 8888;

    private static Object threadLock = new Object();

    private static int threadNumber = 0;

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void main(String[] args) {
        try (ServerSocket sc = new ServerSocket(PORT)) {
            System.out.println("Server listening on " + PORT);
            File rootDir = new File(ROOT_DIR);
            if (!rootDir.exists()) {
                LOGGER.warning("Root directory " + ROOT_DIR + " do not exist");
            }
            while (true) {
                Socket socket = sc.accept();
                threadNumber ++;
                LOGGER.info("Started new thread "+threadNumber+" number");
                Worker worker = new Worker(String.valueOf(threadNumber),socket,ROOT_DIR,threadLock, LOGGER);
                worker.start();
            }
        } catch (IOException e) {
            System.out.println("Klaida port " + e.getMessage());
        }
    }

    public static Object getThreadLock() {
        return threadLock;
    }

    public static void setThreadLock(Object threadLock) {
        Main.threadLock = threadLock;
    }

    public static int getThreadNumber() {
        return threadNumber;
    }

    public static void setThreadNumber(int threadNumber) {
        Main.threadNumber = threadNumber;
    }
}
