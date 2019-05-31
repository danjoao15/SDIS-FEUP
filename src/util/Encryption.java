/**
 * 
 */
package util;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Encryption {
	private byte[] key;

	private static final String AES = "AES";
	private static final String AES_ECB_PKCS5Padding = "AES/ECB/PKCS5Padding";
	public Encryption() {
		super();
		KeyGenerator generatedKey;
		try {
			generatedKey = KeyGenerator.getInstance(AES);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return;
		}
		SecretKey aesKey = generatedKey.generateKey();
		this.setKey(aesKey.getEncoded());
	}

	public Encryption(String encryptKey) {
		this.setKey(encryptKey.getBytes(StandardCharsets.ISO_8859_1));
	}

	public byte[] getKey() {
		return key;
	}

	public void setKey(byte[] key) {
		this.key = key;
	}
	
	public byte [] encriptation(byte [] cleartext){
		try {
			SecretKey auxiliarKey  =  new SecretKeySpec(getKey(), AES);
			Cipher aesCipher = Cipher.getInstance(AES_ECB_PKCS5Padding);
			aesCipher.init(Cipher.ENCRYPT_MODE, auxiliarKey );

			return aesCipher.doFinal(cleartext);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public byte [] decryptation(byte[] cipherText){
		try {
			SecretKey auxiliarKey  =  new SecretKeySpec(getKey(), AES);
			Cipher aesCipher = Cipher.getInstance(AES_ECB_PKCS5Padding);
			aesCipher.init(Cipher.DECRYPT_MODE, auxiliarKey );
			return aesCipher.doFinal(cipherText);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}