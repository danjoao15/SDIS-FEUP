package util;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import chord.ManageChord;

public class Loggs {
	public static final int TIME_MAX_TO_SLEEP = 400;
	public static final int MAX_CHUNK_SIZE = 64000;
	public static final int BYTE_TO_KBYTE = 1000;

	public static Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	static {
		LOG.setUseParentHandlers(false);
		LOG.setLevel(Level.ALL);
		try {
			FileHandler h = new FileHandler("backup_%g_log_%u.logging", true);
			h.setFormatter(new SimpleFormatter());
			LOG.addHandler(h);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void randonSleep(int time) throws InterruptedException {
		Random r = new Random();
		Thread.sleep(r.nextInt(time));
	}

	public static boolean inTheMiddle(String inf, String sup, String value) {
		BigInteger _inf = new BigInteger(inf,16);
		BigInteger _sup = new BigInteger(sup,16);
		BigInteger _value = new BigInteger(value,16);

		BigInteger aux = new BigInteger((Math.pow(2, ManageChord.getM())+"").getBytes());

		if(_sup.compareTo(_inf) <= 0) {
			_sup  = _sup.add(aux);
		}
		if(_value.compareTo(_inf) < 0) {
			_value = _value.add(aux);
		}
		return ((_inf.compareTo(_value) < 0) && (_value.compareTo(_sup) <= 0));
	}
	
	public static String highest(String id1, String id2) {
		if (id1 == null) return id2;
		if (id2 == null) return id1;
		BigInteger _id1 = new BigInteger(id1,16);
		BigInteger _id2 = new BigInteger(id2,16);
		if (_id1.compareTo(_id2) > 0) {
			return id1;
		}
		return id2;
	}
	
	public static void logging(String message) {
		LOG.info(message);
	}

	public static String read(String filepath) {
		byte[] encoded;
		try {
			encoded = Files.readAllBytes(Paths.get(filepath));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return new String(encoded, StandardCharsets.ISO_8859_1);
	}

	public static void write(Path filePath, byte[] body) throws IOException {
		if(!Files.exists(filePath)) {
			Files.createFile(filePath);
			AsynchronousFileChannel channel = AsynchronousFileChannel.open(filePath,StandardOpenOption.WRITE);
			CompletionHandler<Integer, ByteBuffer> writter = new CompletionHandler<Integer, ByteBuffer>() {
				@Override
				public void completed(Integer result, ByteBuffer buffer) {
					System.out.println("Finished writing!");
				}

				@Override
				public void failed(Throwable arg0, ByteBuffer arg1) {
					System.err.println("Error: Could not write!");

				}

			};
			ByteBuffer src = ByteBuffer.allocate(body.length);
			src.put(body);
			src.flip();
			channel.write(src, 0, src, writter);
		} else {
			System.out.println("File may exist already.");
		}
	}

	public static void delete(Path filePath) {
		try {
			Files.deleteIfExists(filePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String byteArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder(a.length * 2);
		for(byte b: a)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}

	public static String getIdFromHash(byte[] hash, int length) {
		byte[] slice = Arrays.copyOf(hash, length);
		return byteArrayToHex(slice);
	}

}