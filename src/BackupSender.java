import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackupSender implements Runnable {

    String filename;
    String desiredRep;
    int tries;
    MulticastSocket MDBSocket;
    ConcurrentHashMap<String, ArrayList<String>> stored_received;
    ScheduledExecutorService executor_service;

    BackupSender(String filename, String desiredRep, MulticastSocket MDBSocket, ConcurrentHashMap<String, ArrayList<String>> stored_received, int tries, ScheduledExecutorService executor_service) {
        this.filename = filename;
        this.desiredRep = desiredRep;
        this.MDBSocket = MDBSocket;
        this.stored_received = stored_received;
        this.tries = tries;
        this.executor_service = executor_service;
    }

    @Override
    public void run() {
        File file = new File(filename);
        FileInputStream fin = null;
        boolean needToRepeat = false;
        try {
            fin = new FileInputStream(file);
            byte fileContent[] = new byte[(int)file.length()];
            System.out.println("File Size: " + fileContent.length);
            fin.read(fileContent);
            ArrayList<byte[]> chunks = new ArrayList<>();
            int chunk = 64000;
            for(int i = 0; i < fileContent.length; i += chunk) {
                chunks.add(Arrays.copyOfRange(fileContent, i, Math.min(fileContent.length, i + chunk)));
            }
            System.out.println("Chunk Number: " + chunks.size());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(filename.getBytes());

            String file_id = bytesToHex(hash);
            for(int i = 0; i < chunks.size(); i++) {
                if(!stored_received.containsKey(file_id + "&&&" + i)){
                    stored_received.put(file_id + "&&&" + i,new ArrayList<>());
                }
                if(stored_received.get(file_id + "&&&" + i).size() < Integer.parseInt(desiredRep)){
                    System.out.println("Desired rep does not match rep of " + stored_received.get(file_id + "&&&" + i).size());
                    String msg = "PUTCHUNK " + Constants.version + " " + Constants.SenderId + " " + file_id + " " + i + " " + desiredRep + " ";
                    byte[] msgBytes = msg.getBytes();
                    byte[] crlf = {0x0d, 0x0a, 0x0d, 0x0a};
                    byte[] finalMsg = concatAll(msgBytes, crlf, chunks.get(0));
                    InetAddress group = InetAddress.getByName(Constants.MDBIP);

                    DatagramPacket packet
                            = new DatagramPacket(finalMsg, finalMsg.length, group, Constants.MDBPort);
                    MDBSocket.send(packet);
                    needToRepeat = true;
                }
            }
            System.out.println(stored_received);

            if (needToRepeat && tries > 0){
                executor_service.schedule(new BackupSender(filename, desiredRep, MDBSocket,stored_received,tries-1,executor_service),400,TimeUnit.MILLISECONDS);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static byte[] concatAll(byte[] first, byte[]... rest) {
        int totalLength = first.length;
        for (byte[] array : rest) {
            totalLength += array.length;
        }
        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        System.out.println("Final array of " + result.length);
        return result;
    }

    private static String bytesToHex(byte[] hashInBytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : hashInBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
