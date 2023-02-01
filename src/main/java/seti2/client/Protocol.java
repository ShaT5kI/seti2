package seti2.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

public class Protocol {
    private static final int ARR_SIZE = 1024;
    private static final int THAT_ALL = -1;
    private byte[] hashOfFile;

    private static final Logger log = Logger.getLogger(Client.class.getName());

    public void calcHash(Path filePath) {

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            log.severe( "Can't create MessageDigest. Not algorithm");
            System.out.println(e.getMessage());
        }
        byte[] buffer = new byte[ARR_SIZE];
        int readSize;

        try (InputStream is = Files.newInputStream(filePath)) {
            while (0 < (readSize = is.read(buffer))) {
                md.update(buffer, 0, readSize);
            }
        } catch (IOException e) {
            log.severe( "Can't create input stream");
            System.out.println(e.getMessage());
        }
        hashOfFile = md.digest();
    }

    public void sendData(DataOutputStream out, Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString();
        out.writeUTF(fileName);

        long size = Files.size(filePath);
        out.writeLong(size);

        int hashSize = hashOfFile.length;
        out.writeInt(hashSize);
        out.write(hashOfFile);
    }

    public void sendFile(DataOutputStream out, Path filePath) throws IOException {
        byte[] buff = new byte[ARR_SIZE];
        int readSize;
        InputStream stream = Files.newInputStream(filePath);
        while ((readSize = stream.read(buff)) > 0) {
            out.writeInt(readSize);
            out.write(buff);
        }
        out.writeInt(THAT_ALL);
    }

    public Boolean checkAnswer(DataInputStream in) throws IOException {
        return in.readBoolean();
    }

    public void sendAck(int ack, DataOutputStream out) throws IOException {
        out.writeInt(ack);
        out.flush();
    }
}
