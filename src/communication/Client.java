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

import util.Loggs;

public class Client {
	private static ArrayList<String> cipher = new ArrayList<String>(Arrays.asList("TLS_DHE_RSA_WITH_AES_128_CBC_SHA"));
	

	public static String sendMsg(InetAddress address, int port, String msg, boolean waitResponse) {

		String response = null;
		SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		SSLSocket socket;
		try {
			socket = (SSLSocket) socketFactory.createSocket(address, port);
			socket.setSoTimeout(5000);
		} catch (IOException e) {
			System.err.println("Can't connect to server");
			return null;
		}

		socket.setEnabledCipherSuites(cipher.toArray(new String[0]));

		try {
			send(msg, socket);
		} catch (IOException e1) {
			System.err.println("Can't send message");
			return null;
		}
		if(waitResponse) {
			response = getRespo(socket);
		}
		try {
			socket.close();
		} catch (IOException e) {
			System.err.println("Can't close connection");
			return null;
		}
		return response;
	}

	private static byte[] encode(byte[] sendInfo) {
		ArrayList<Byte> res = new ArrayList<Byte>();
		for(int i = 0; i < sendInfo.length; i++) {
			if(sendInfo[i]=='\t') {
				res.add((byte) '\f');
				res.add((byte) '\t');
			} else {
				if(sendInfo[i]=='\f') {
					res.add((byte) '\f');
					res.add((byte) '\f');
				
				}else {
					res.add(sendInfo[i]);
				}
			}
		}
		byte[] a = new byte[res.size()];
		for (int i = 0; i < res.size(); i++) {
			a[i] = res.get(i);
		}
		return a;
	}
	
	public static void send(String msg, SSLSocket socket) throws IOException {
		byte[] sendInfo = encode(msg.getBytes(StandardCharsets.ISO_8859_1));

		OutputStream sendStream = socket.getOutputStream();
		sendStream.write(sendInfo);
		sendStream.write('\t');
	}
	
	public static String getRespo(SSLSocket socket) {
		byte[] readInfo = new byte[1024 + Loggs.MAX_CHUNK_SIZE];
		InputStream readStream;
		try {
			readStream = socket.getInputStream();
		} catch (IOException e) {
			System.err.println("Can't get input stream");
			return null;
		}
		try {
			readStream.read(readInfo);
		} catch(SocketTimeoutException e) {
			System.err.println("Socket timeout");
			return null;
		} catch (IOException e) {
			System.err.println("Can't read");
			return null;
		}
		try {
			socket.close();
		} catch (IOException e) {
			System.err.println("Can't close connection");;
			return null;
		}
		return new String(readInfo,StandardCharsets.ISO_8859_1);

	}


}

