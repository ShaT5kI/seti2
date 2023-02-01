package seti2.client;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class FileSender {
    private final Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private final Path filePath;
    private final Protocol protocol;

    private static final Logger log = Logger.getLogger(Client.class.getName());

    public FileSender(Socket socket, String filePathString) {
        this.socket = socket;
        filePath = Paths.get(filePathString);
        protocol = new Protocol();
        try {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            log.severe( "Can't create data streams");
        }
    }

    private void freeData() {
        try {
            dataInputStream.close();
            dataOutputStream.close();
            socket.close();
        } catch (IOException e) {
            log.severe( "Can't free client data");
            System.out.println(e.getMessage());
        }
    }

    public void workWithProtocol() {
        try {
            protocol.calcHash(filePath);
            protocol.sendData(dataOutputStream, filePath);
            protocol.sendFile(dataOutputStream, filePath);
            Boolean serverAnswer = protocol.checkAnswer(dataInputStream);
            if (serverAnswer) {
                System.out.println("Successful connection");
                log.info( "Successful connection for client " + socket.getInetAddress());
                protocol.sendAck(1, dataOutputStream);
            }
            else {
                System.out.println("Not successful connection");
                log.info( "Not successful connection for client " + socket.getInetAddress());
                protocol.sendAck(0, dataOutputStream);
            }
            freeData();
        } catch (IOException e) {
            log.severe( "Exception during work with protocol");
            freeData();
            System.out.println(e.getMessage());
        }
    }
}
