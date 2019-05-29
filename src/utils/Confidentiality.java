/**
 * 
 */
package utils;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Confidentiality {
	private byte[] key;

	private static final String AES = "AES";
	private static final String AES_ECB_PKCS5Padding = "AES/ECB/PKCS5Padding";
	public Confidentiality() {
		super();
		KeyGenerator keygen;
		try {
			keygen = KeyGenerator.getInstance(AES);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return;
		}
		SecretKey aesKey = keygen.generateKey();
		this.setKey(aesKey.getEncoded());
	}

	public Confidentiality(String encryptKey) {
		this.setKey(encryptKey.getBytes(StandardCharsets.ISO_8859_1));
	}

	public byte [] encript(byte [] cleartext){
		try {
			SecretKey aux =  new SecretKeySpec(getKey(), AES);
			Cipher aesCipher = Cipher.getInstance(AES_ECB_PKCS5Padding);
			aesCipher.init(Cipher.ENCRYPT_MODE, aux);

			return aesCipher.doFinal(cleartext);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public byte [] decrypt(byte[] ciphertext){
		try {
			SecretKey aux =  new SecretKeySpec(getKey(), AES);
			Cipher aesCipher = Cipher.getInstance(AES_ECB_PKCS5Padding);
			aesCipher.init(Cipher.DECRYPT_MODE, aux);
			return aesCipher.doFinal(ciphertext);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @return the key
	 */
	public byte[] getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(byte[] key) {
		this.key = key;
	}
}