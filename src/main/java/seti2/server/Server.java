package seti2.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Server {
    private static final Logger log = Logger.getLogger(Server.class.getName());

    public static void main(String[] args) {
        try {
            LogManager.getLogManager().readConfiguration(
                    Server.class.getResourceAsStream("/log4j.properties"));
        } catch (IOException e) {
            System.err.println("Could not setup logger configuration: " + e.toString());
        }

        if (args.length != 1) {
            log.severe( "Incorrect count of args");
            System.exit(1);
        }
        int portNumber = Integer.parseInt(args[0]);
        ExecutorService threadPool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            log.info( "Create server socket");
            ExitServer exitServer = new ExitServer(serverSocket);
            Thread exitThread = new Thread(exitServer);
            exitThread.start();

            while (!serverSocket.isClosed()) {
                Socket newConnection = serverSocket.accept();
                log.info("Client socket connect");
                DownloadFile downloadFile = new DownloadFile(newConnection);
                threadPool.submit(downloadFile);
            }

            threadPool.shutdownNow();
            exitThread.interrupt();
            exitThread.join();
        } catch (IOException e) {
            log.severe( "Can't take connection with client socket");
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            log.severe( "Can't join exitThread");
            System.out.println(e.getMessage());
        }
    }
}
