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
 * Ein InputStream, der verschlüsselte Daten während des Lesens entschlüsselt.
 * Erwartet das Format: Salt (16 Bytes) + IV (16 Bytes) + verschlüsselte Daten.
 * Kompatibel mit EncryptedInputStream.
 */
public class DecryptedInputStream extends FilterInputStream {

    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;
    private static final int HEADER_LENGTH = SALT_LENGTH + IV_LENGTH;
    
    private final CipherInputStream cipherInputStream;
    private boolean initialized = false;

    /**
     * Erstellt einen entschlüsselnden InputStream.
     * 
     * @param in Der verschlüsselte InputStream (mit Salt + IV + verschlüsselte Daten)
     * @param passphrase Die Passphrase für die Entschlüsselung
     * @throws RuntimeException wenn die Entschlüsselung nicht initialisiert werden kann
     */
    public DecryptedInputStream(InputStream in, String passphrase) {
        super(in);
        
        if (passphrase == null || passphrase.isEmpty()) {
            throw new IllegalArgumentException("Passphrase darf nicht leer sein");
        }
        
        try {
            // Lese Salt (16 Bytes) aus dem Stream
            byte[] salt = new byte[SALT_LENGTH];
            int saltBytesRead = readFully(in, salt);
            if (saltBytesRead < SALT_LENGTH) {
                throw new IOException("Stream zu kurz: Salt konnte nicht vollständig gelesen werden (gelesen: " + saltBytesRead + " Bytes)");
            }
            
            // Lese IV (16 Bytes) aus dem Stream
            byte[] iv = new byte[IV_LENGTH];
            int ivBytesRead = readFully(in, iv);
            if (ivBytesRead < IV_LENGTH) {
                throw new IOException("Stream zu kurz: IV konnte nicht vollständig gelesen werden (gelesen: " + ivBytesRead + " Bytes)");
            }
            
            // Generiere Schlüssel aus Passphrase mit PBKDF2
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
            
            // Erstelle IV Spec
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // Initialisiere Cipher für Entschlüsselung
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            
            // Erstelle CipherInputStream für entschlüsselte Daten
            this.cipherInputStream = new CipherInputStream(in, cipher);
            this.initialized = true;
            
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Lesen des Headers: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Initialisieren der Entschlüsselung: " + e.getMessage(), e);
        }
    }

    /**
     * Liest vollständig ein Byte-Array aus dem InputStream.
     * 
     * @param in Der InputStream
     * @param buffer Der Ziel-Buffer
     * @return Anzahl der gelesenen Bytes
     * @throws IOException bei I/O-Fehlern
     */
    private int readFully(InputStream in, byte[] buffer) throws IOException {
        int totalBytesRead = 0;
        int bytesRead;
        
        while (totalBytesRead < buffer.length) {
            bytesRead = in.read(buffer, totalBytesRead, buffer.length - totalBytesRead);
            if (bytesRead == -1) {
                return totalBytesRead;
            }
            totalBytesRead += bytesRead;
        }
        
        return totalBytesRead;
    }

    @Override
    public int read() throws IOException {
        if (!initialized) {
            throw new IOException("Stream nicht initialisiert");
        }
        return cipherInputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!initialized) {
            throw new IOException("Stream nicht initialisiert");
        }
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        
        return cipherInputStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        if (!initialized) {
            throw new IOException("Stream nicht initialisiert");
        }
        return cipherInputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        if (!initialized) {
            return 0;
        }
        return cipherInputStream.available();
    }

    @Override
    public void close() throws IOException {
        if (initialized) {
            cipherInputStream.close();
        }
        super.close();
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
}
