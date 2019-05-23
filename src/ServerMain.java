import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.channels.MulticastChannel;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ServerMain implements ClientCalls{

    public ScheduledExecutorService executor_service;

    public MulticastSocket MDBSocket;
    public MulticastSocket MCSocket;
    public MulticastSocket MDRSocket;

    public ConcurrentHashMap<String,ArrayList<String>> stored_received;

    ServerMain(MulticastSocket MDBSocket, MulticastSocket MCSocket, MulticastSocket MDRSocket) {
        this.MDBSocket = MDBSocket;
        this.MCSocket = MCSocket;
        this.MDRSocket = MDRSocket;
        // 10 é o numero de threads que podem correr ao mesmo tempo
        executor_service = Executors.newScheduledThreadPool(10, new MyThreadFactory());
        this.stored_received = new ConcurrentHashMap<>();

        executor_service.execute(new StoredReceiver(MCSocket,stored_received));
    }

    public void start(){
        while (true) {
            byte[] max_msg_buffer = new byte[128000];
            DatagramPacket msg_packet = new DatagramPacket(max_msg_buffer, 128000);
            byte[] msg_buffer;

            try {
                MDBSocket.receive(msg_packet);
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

            if(msg_tokens[0].equals("PUTCHUNK")){
                executor_service.execute(new PutChunkProcessor(stored_received,msg_buffer,MCSocket,msg_tokens));
            }
        }
    }

    public static void main(String[] args) {
        // 1.0 1 224.0.0.3 8887 224.0.0.4 8888 224.0.0.5 8889
        Constants.version = args[0];
        Constants.SenderId = args[1];
        Constants.MDBIP = args[2];
        Constants.MDBPort = Integer.parseInt(args[3]);
        Constants.MCIP = args[4];
        Constants.MCPort = Integer.parseInt(args[5]);
        Constants.MDRIP = args[6];
        Constants.MDRPort = Integer.parseInt(args[7]);

        try {
            MulticastSocket MDBSocket = new MulticastSocket(Constants.MDBPort);
            MulticastSocket MCSocket = new MulticastSocket(Constants.MCPort);
            MulticastSocket MDRSocket = new MulticastSocket(Constants.MDRPort);
            InetAddress MDBGroup = InetAddress.getByName(Constants.MDBIP);
            InetAddress MCGroup = InetAddress.getByName(Constants.MCIP);
            InetAddress MDRGroup = InetAddress.getByName(Constants.MDRIP);
            MDBSocket.joinGroup(MDBGroup);
            MCSocket.joinGroup(MCGroup);
            MDRSocket.joinGroup(MDRGroup);

            ServerMain obj = new ServerMain(MDBSocket,MCSocket,MDRSocket);
            ClientCalls stub = (ClientCalls) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("ClientCalls", stub);

            obj.start();
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void action(String args[]) throws RemoteException {
        if(args[0].equals("BACKUP")) {
            // lancar uma thread a parte que executa o BackupSender
            this.executor_service.execute(new BackupSender(args[1], args[2], MDBSocket,stored_received,5,executor_service));
        }
    }
}
