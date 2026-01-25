package de.hechler.occlient.filesync;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Ein InputStream, der die Daten während des Lesens mit AES-256 verschlüsselt.
 * Die verschlüsselten Daten enthalten: Salt (16 Bytes) + IV (16 Bytes) + verschlüsselte Daten.
 */
public class EncryptedInputStream extends FilterInputStream {

    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;
    
    private final byte[] salt;
    private final byte[] iv;
    private final CipherInputStream cipherInputStream;
    private int headerBytesRemaining;
    private int headerPosition;

    /**
     * Erstellt einen verschlüsselnden InputStream.
     * 
     * @param in Der Quell-InputStream mit den Klartextdaten
     * @param passphrase Die Passphrase für die Verschlüsselung
     * @throws RuntimeException wenn die Verschlüsselung nicht initialisiert werden kann
     */
    public EncryptedInputStream(InputStream in, String passphrase) {
        super(in);
        if (passphrase == null || passphrase.isEmpty()) {
            throw new IllegalArgumentException("Passphrase darf nicht leer sein");
        }
        try {
            // Generiere pseudozufälliges Salt (16 Bytes)
            this.salt = ChecksumUtil.calculateMD5bytes("salt:"+passphrase);
            
            // Generiere Schlüssel aus Passphrase mit PBKDF2
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
            
            // Generiere pseudozufälligen IV (16 Bytes)
            this.iv = ChecksumUtil.calculateMD5bytes("iv:"+passphrase);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // Initialisiere Cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            
            // Erstelle CipherInputStream für verschlüsselte Daten
            this.cipherInputStream = new CipherInputStream(in, cipher);
            
            // Header (Salt + IV) muss zuerst gelesen werden
            this.headerBytesRemaining = SALT_LENGTH + IV_LENGTH;
            this.headerPosition = 0;
            
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Initialisieren der Verschlüsselung: " + e.getMessage(), e);
        }
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        int result = read(b, 0, 1);
        return result == -1 ? -1 : (b[0] & 0xFF);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        
        int totalBytesRead = 0;
        
        // Zuerst Header (Salt + IV) ausgeben
        if (headerBytesRemaining > 0) {
            int headerBytesToRead = Math.min(len, headerBytesRemaining);
            
            // Kopiere Salt und/oder IV
            if (headerPosition < SALT_LENGTH) {
                int saltBytesToCopy = Math.min(headerBytesToRead, SALT_LENGTH - headerPosition);
                System.arraycopy(salt, headerPosition, b, off, saltBytesToCopy);
                headerPosition += saltBytesToCopy;
                off += saltBytesToCopy;
                len -= saltBytesToCopy;
                totalBytesRead += saltBytesToCopy;
                headerBytesRemaining -= saltBytesToCopy;
                headerBytesToRead -= saltBytesToCopy;
            }
            
            if (headerBytesToRead > 0 && headerPosition >= SALT_LENGTH) {
                int ivBytesToCopy = headerBytesToRead;
                int ivPosition = headerPosition - SALT_LENGTH;
                System.arraycopy(iv, ivPosition, b, off, ivBytesToCopy);
                headerPosition += ivBytesToCopy;
                off += ivBytesToCopy;
                len -= ivBytesToCopy;
                totalBytesRead += ivBytesToCopy;
                headerBytesRemaining -= ivBytesToCopy;
            }
            
            if (len == 0) {
                return totalBytesRead;
            }
        }
        
        // Dann verschlüsselte Daten vom CipherInputStream lesen
        int encryptedBytesRead = cipherInputStream.read(b, off, len);
        
        if (encryptedBytesRead == -1) {
            return totalBytesRead > 0 ? totalBytesRead : -1;
        }
        
        return totalBytesRead + encryptedBytesRead;
    }

    @Override
    public void close() throws IOException {
        cipherInputStream.close();
        super.close();
    }

    @Override
    public int available() throws IOException {
        // Header-Bytes sind sofort verfügbar
        if (headerBytesRemaining > 0) {
            return headerBytesRemaining;
        }
        // Für verschlüsselte Daten können wir keine genaue Aussage treffen
        return 0;
    }

    @Override
    public synchronized void mark(int readlimit) {
        // Mark/Reset wird nicht unterstützt
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override
    public boolean markSupported() {
        return false;
    }

	/**
	 * Berechnet die Größe der verschlüsselten Datei basierend auf der Größe der Eingabedatei.
	 * 
	 * Die verschlüsselte Datei enthält:
	 * - Salt (16 Bytes)
	 * - IV (16 Bytes)
	 * - Verschlüsselte Daten mit PKCS5-Padding (aufgerundet auf 16-Byte-Blöcke)
	 * 
	 * @param inputSize Größe der Eingabedatei in Bytes
	 * @return Größe der verschlüsselten Datei in Bytes
	 */
	public static long getEncryptedSizeForInputSize(long inputSize) {
		if (inputSize < 0) {
			throw new IllegalArgumentException("Input size cannot be negative");
		}
		
		// Header: Salt (16 Bytes) + IV (16 Bytes)
		long headerSize = SALT_LENGTH + IV_LENGTH;
		
		// PKCS5-Padding: Daten werden auf 16-Byte-Blöcke aufgefüllt
		// Es wird IMMER mindestens 1 Byte Padding hinzugefügt (1-16 Bytes)
		// Wenn die Größe bereits ein Vielfaches von 16 ist, wird ein kompletter Block (16 Bytes) hinzugefügt
		int blockSize = 16;
		long paddedSize = inputSize + blockSize - (inputSize % blockSize);
		
		return headerSize + paddedSize;
	}
}
