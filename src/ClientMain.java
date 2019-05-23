import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class ClientMain {

    public static void main(String[] args) {
        try {
            // aceder ao registry de RMI e chamar a funcao no cliente e isto corre a funcao no servidor
            Registry registry = LocateRegistry.getRegistry(args[0]);
            ClientCalls stub = (ClientCalls) registry.lookup("ClientCalls");
            stub.action(Arrays.copyOfRange(args, 1, args.length));
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

    }

}
