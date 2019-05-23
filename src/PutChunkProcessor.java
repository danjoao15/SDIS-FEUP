import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PutChunkProcessor implements Runnable {

    ConcurrentHashMap<String, ArrayList<String>> stored_received;
    MulticastSocket MCSocket;
    byte[] msg_buffer;
    String[] msg_tokens;

    public PutChunkProcessor(ConcurrentHashMap<String, ArrayList<String>> stored_received, byte[] msg_buffer, MulticastSocket MCSocket, String[] msg_tokens) {
        this.stored_received = stored_received;
        this.msg_buffer = msg_buffer;
        this.MCSocket = MCSocket;
        this.msg_tokens = msg_tokens;
    }

    @Override
    public void run() {
        Path pathToFile = Paths.get("peer"+Constants.SenderId+"/backup/"+msg_tokens[3]+"/chk" + msg_tokens[4]);
        try {
            Files.createDirectories(pathToFile.getParent());
            Files.createFile(pathToFile);
            Files.write(pathToFile, split_by_tokens(msg_buffer)[1]);
        } catch (IOException e) {
            System.out.println("Already backing up chunk " + msg_tokens[3] + "-----" + msg_tokens[4]);
        }

        //STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>

        String stored = "STORED " + Constants.version + " " + Constants.SenderId  + " " + msg_tokens[3] + " " + msg_tokens[4] + " " + " ";
        byte[] msgBytes = stored.getBytes();
        byte[] crlf = {0x0d, 0x0a, 0x0d, 0x0a};
        byte[] finalMsg = concatAll(msgBytes, crlf);

        InetAddress group = null;
        try {
            group = InetAddress.getByName(Constants.MCIP);
            DatagramPacket packet
                    = new DatagramPacket(finalMsg, finalMsg.length, group, Constants.MCPort);
            MCSocket.send(packet);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
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
        return result;
    }

    public byte[][] split_by_tokens(byte[] data) {
        long crlfnum = 0;

        byte[][] result = new byte[2][];
        for(int i = 0; i < data.length-2; i++){
            if(data[i] == 0x0d && data[i+1] == 0x0a){
                crlfnum++;
            }
            if(crlfnum == 2){
                result[0] = Arrays.copyOfRange(data, 0, i-2);;
                result[1] = Arrays.copyOfRange(data, i+2, data.length);;
                return result;
            }
        }

        return new byte[0][0]; //Caso erro
    }
}
