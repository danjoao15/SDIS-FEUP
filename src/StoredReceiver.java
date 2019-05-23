import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class StoredReceiver implements Runnable {

    MulticastSocket MCSocket;
    ConcurrentHashMap<String, ArrayList<String>> stored_received;

    public StoredReceiver(MulticastSocket mcSocket, ConcurrentHashMap<String, ArrayList<String>> stored_received) {
        this.MCSocket = mcSocket;
        this.stored_received = stored_received;
    }

    @Override
    public void run() {
        while (true) {
            byte[] max_msg_buffer = new byte[128000];
            DatagramPacket msg_packet = new DatagramPacket(max_msg_buffer, 128000);
            byte[] msg_buffer;

            try {
                System.out.println("LISTENING TO MCSOCKET");
                MCSocket.receive(msg_packet);
                System.out.println("RECEIVED PACKET");
                msg_buffer = Arrays.copyOfRange(max_msg_buffer,0,msg_packet.getLength());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            String msg = new String(msg_buffer,0,msg_buffer.length);
            String msg_tokens[] = msg.split(" ");

            if(msg_tokens[2].equals(Constants.SenderId)){
                continue;
            }

            if(msg_tokens[0].equals("STORED")){
                if(!stored_received.containsKey(msg_tokens[3] + "&&&" + msg_tokens[4])){
                    stored_received.put(msg_tokens[3] + "&&&" + msg_tokens[4],new ArrayList<>());
                }
                stored_received.get(msg_tokens[3] + "&&&" + msg_tokens[4]).add(msg_tokens[2]);
                System.out.println("Added to list of tokens");
                System.out.println(stored_received);
            }
        }
    }
}
