package communication;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import utils.Utils;

public class Client {
	private static ArrayList<String> cipherString = new ArrayList<String>(Arrays.asList("TLS_DHE_RSA_WITH_AES_128_CBC_SHA"));
	

	public static String sendMessage(InetAddress address, int port, String message, boolean wait) {

		String response = null;
		SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		SSLSocket socket;
		try {
			socket = (SSLSocket) socketFactory.createSocket(address, port);
			socket.setSoTimeout(5000);
		} catch (IOException e) {
			System.err.println("Connection refused - couldn't connect to server");
			return null;
		}

		socket.setEnabledCipherSuites(cipherString.toArray(new String[0]));

		try {
			send(message, socket);
		} catch (IOException error1) {
			System.err.println("Connection refused - couldn't send message");
			return null;
		}
		if(wait) {
			response = getResponse(socket);
		}
		try {
			socket.close();
		} catch (IOException error0) {
			System.err.println("Error closing connection");
			return null;
		}
		return response;
	}

	public static void send(String message, SSLSocket socket) throws IOException {
		byte[] sendData = encodeData(message.getBytes(StandardCharsets.ISO_8859_1));

		OutputStream sendStream = socket.getOutputStream();
		sendStream.write(sendData);
		sendStream.write('\t');
	}

	private static byte[] encodeData(byte[] sendData) {
		ArrayList<Byte> res = new ArrayList<Byte>();
		for(int i = 0; i < sendData.length; i++) {
			if(sendData[i]=='\t') {
				res.add((byte) '\f');
				res.add((byte) '\t');
			} else {
				if(sendData[i]=='\f') {
					res.add((byte) '\f');
					res.add((byte) '\f');
				
				}else {
					res.add(sendData[i]);
				}
			}
		}
		byte[] a = new byte[res.size()];
		for (int i = 0; i < res.size(); i++) {
			a[i] = res.get(i);
		}
		return a;
	}

	public static String getResponse(SSLSocket socket) {
		byte[] readData = new byte[1024 + Utils.MAX_LENGTH_CHUNK];
		InputStream readStream;
		try {
			readStream = socket.getInputStream();
		} catch (IOException error2) {
			System.err.println("Error getting input stream");
			return null;
		}
		try {
			readStream.read(readData);
		} catch(SocketTimeoutException e) {
			System.err.println("Socket timeout");
			return null;
		} catch (IOException errror3) {
			System.err.println("Error reading");
			return null;
		}
		try {
			socket.close();
		} catch (IOException error4) {
			System.err.println("Error closing connection");;
			return null;
		}
		return new String(readData,StandardCharsets.ISO_8859_1);

	}


}

