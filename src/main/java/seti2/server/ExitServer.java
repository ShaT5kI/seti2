package seti2.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Scanner;
import java.util.logging.Logger;

public class ExitServer implements Runnable {
    private final ServerSocket socket;
    private static final Logger log = Logger.getLogger(Server.class.getName());

    public ExitServer(ServerSocket serverSocket) {
        socket = serverSocket;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        String word = scanner.nextLine();
        if ("exit".equals(word)) {
            try {
                socket.close();
                log.info("Server socket close");
                System.exit(0);
            } catch (IOException e) {
                log.severe( "Can't close server socket");
                System.out.println(e.getMessage());
            }
        }
        scanner.close();
    }
}

