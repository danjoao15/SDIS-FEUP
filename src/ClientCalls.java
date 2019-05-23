import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCalls extends Remote {
    void action(String args[]) throws RemoteException;
}
