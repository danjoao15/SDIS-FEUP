package main;

import java.io.IOException;
import java.io.InputStream;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import util.SingletonThreadPoolExecutor;
import util.Loggs;

public class Server implements Runnable {

	public static final int MAX_LENGTH_PACKET = 300;
	private int port_number;
	private PeerMain peer = null;
	private SSLServerSocket serverSocket;
	
	
	public Server(String[] cipher_suite, int port) throws Exception {
		
		this.port_number = port;

		setSystemProperties();
	}

	
	
	@Override
	public void run() {
		SSLServerSocketFactory serverFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
		
		try {
			serverSocket = (SSLServerSocket) serverFactory.createServerSocket(this.port_number);
			Loggs.LOG.info("Server listening on port " + this.port_number);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		serverSocket.setNeedClientAuth(true);
		serverSocket.setEnabledProtocols(serverSocket.getSupportedProtocols());


		while (true) {
			SSLSocket socket;
			try {
				socket = (SSLSocket) serverSocket.accept();
			} catch (IOException e) {
				Loggs.LOG.warning("Socket closed");
				return;
			}
			try {
				socket.startHandshake();
			} catch (IOException e) {
				System.err.println(e.getMessage());
				return;
			}

			byte[] readData = readSocket(socket);

			HandleMessage p = new HandleMessage(getPeer(), readData, socket);
			
			SingletonThreadPoolExecutor.getInstance().get().execute(p);	
		}

	}
	
	
	
	public void closeConnection() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

	
	public byte[] readSocket(SSLSocket socket) {
		InputStream readStream;
		try {
			readStream = socket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		byte[] readData = new byte[1024 + Loggs.MAX_CHUNK_SIZE];
		try {
			int p = 0;
			byte l = 'a';
			while(l != '\t') {
				l = (byte) readStream.read();
				if(l == '\f') {
					l = (byte) readStream.read();
					readData[p] = l;
					l='a';
				} else {
					readData[p] = l;
				}
				p++;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return readData;
	}

	public PeerMain getPeer() {
		return peer;
	}

	public void setPeer(PeerMain peer) {
		this.peer = peer;
	}

	public void setSystemProperties() {
		System.setProperty("javax.net.ssl.keyStore", "server.keys");
		System.setProperty("javax.net.ssl.keyStorePassword", "123456");
		System.setProperty("javax.net.ssl.trustStore", "truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", "123456");
	}


}