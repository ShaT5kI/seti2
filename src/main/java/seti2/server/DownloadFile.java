package seti2.server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class DownloadFile implements Runnable {
    private final Socket clientSocket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private MessageDigest md;
    private byte[] hashClientFile;
    private byte[] hashServerFile;
    private String fileName;
    private Path newPath;
    private OutputStream outNewFile;
    private int readAll = 0;
    private int readNow = 0;
    private long spendTime = 0;
    private static final int ARR_SIZE = 1024;
    private static final int CORE_POOL_SIZE = 1;
    private static final int INITIAL_DELAY = 0;
    private static final int PERIOD = 3;
    private static final int TIME = 10000;

    private static final Logger log = Logger.getLogger(DownloadFile.class.getName());

    public DownloadFile(Socket client) {
        clientSocket = client;
    }

    @Override
    public void run() {
        try {
            dataInputStream = new DataInputStream(clientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            md = MessageDigest.getInstance("SHA-256");

            readDataOfFile();
            makeDir();
            createFile();
            writeFile();
            checkHash();
            getAnswer();
            closeConnection();
        } catch (IOException | NoSuchAlgorithmException | InterruptedException e) {
            log.severe( "Exception during download file");
            e.printStackTrace();
        } finally {
            try {
                dataOutputStream.close();
                dataInputStream.close();
            } catch (IOException e) {
                log.severe( "Exception while trying to close the streams");
                e.printStackTrace();
            }
        }
    }

    private void readDataOfFile() throws IOException {
        fileName = dataInputStream.readUTF();
        long fileSize = dataInputStream.readLong();
        int hashSize = dataInputStream.readInt();
        hashClientFile = dataInputStream.readNBytes(hashSize);
    }

    private void makeDir() {
        File fileForPath = new File("src\\main\\java\\seti2\\server\\uploads");
        fileForPath.mkdir();
        newPath = Paths.get(fileForPath.getPath());
    }

    private void createFile() throws IOException {
        outNewFile = Files.newOutputStream(newPath.resolve(fileName));
    }

    private void countSpeed() {
        if (readNow == 0) {
            return;
        }
        double speedNow = (double) readNow / PERIOD;
        spendTime += PERIOD;
        double speedAllTime = (double) readAll / spendTime;
        readNow = 0;

        System.out.println("Client - " + clientSocket.getInetAddress() + ". Speed now = " +
                speedNow + " B/s. Speed all time = " + speedAllTime + " B/s.");
        log.info ( "Print speed");
    }
    
    private void writeFile() throws IOException {
        var scheduledThreadPool = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
        scheduledThreadPool.scheduleAtFixedRate(this::countSpeed, INITIAL_DELAY, PERIOD, TimeUnit.SECONDS);
        log.info( "Start timer for client " + clientSocket.getInetAddress());

        byte[] array;
        int readSize = dataInputStream.readInt();
        while(readSize > 0) {
            array = dataInputStream.readNBytes(readSize);

            readNow += readSize;
            readAll += readSize;

            outNewFile.write(array, 0, readSize);
            outNewFile.flush();

            md.update(array, 0, readSize);
            readSize = dataInputStream.readInt();
            if (readSize > ARR_SIZE) {
                break;
            }
        }
        scheduledThreadPool.shutdown();
        hashServerFile = md.digest();
        log.info( "Read all file from " + clientSocket.getInetAddress());
    }

    private void checkHash() throws IOException, InterruptedException {
        dataOutputStream.writeBoolean(Arrays.equals(hashClientFile, hashServerFile));
        dataOutputStream.flush();
    }

    private void getAnswer() throws IOException, InterruptedException {
        dataInputStream.readInt();
        sleep(TIME);
    }

    private void closeConnection() throws IOException {
        clientSocket.close();
        log.info( "Close connection with client " + clientSocket.getInetAddress());
    }
}
