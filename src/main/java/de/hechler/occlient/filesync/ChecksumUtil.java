package de.hechler.occlient.filesync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility zum Berechnen von Checksummen (MD5)
 */
public final class ChecksumUtil {

    private ChecksumUtil() {}

    public static String calculateMD5enc(Path path, String encryptPassphrase) {
    	if (encryptPassphrase != null) {
			// TODO: return calcEncryptedMD5(path, encryptPassphrase);
    		return null;
		} else {
			return calculateMD5(path);
		}
    }
    
    public static String calculateMD5dec(Path path, String decryptPassphrase) {
    	if (decryptPassphrase != null) {
			// TODO: return calcDecryptedMD5(path, decryptPassphrase);
    		return null;
		} else {
			return calculateMD5(path);
		}
    }
    
    /**
     * Berechnet die MD5-Prüfsumme der angegebenen Datei und liefert sie als hex-String zurück.
     * Bei Fehlern wird null zurückgegeben.
     *
     * @param path Pfad zur Datei
     * @return MD5-Hash als hex-String (kleinbuchstaben) oder null bei Fehlern
     */
    public static String calculateMD5(Path path) {
        if (path == null || !Files.exists(path)) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = Files.newInputStream(path);
                 DigestInputStream dis = new DigestInputStream(is, md)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                    // read stream to update digest
                }
            }
            byte[] digest = md.digest();
            return toHexString(digest);
        } catch (NoSuchAlgorithmException | IOException e) {
            System.err.println("Fehler beim Berechnen der MD5-Prüfsumme für " + path + ": " + e.getMessage());
            return null;
        }
    }

	private static String toHexString(byte[] buffer) {
		// convert to hex
		StringBuilder sb = new StringBuilder(buffer.length * 2);
		for (byte b : buffer) {
		    sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
    
    public static byte[] calculateMD5bytes(String text) {
        if (text == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(text.getBytes());
        } catch (NoSuchAlgorithmException e) {
        	throw new RuntimeException("MD5 MessageDigest nicht gefunden: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verschlüsselt eine Datei mit AES-256 unter Verwendung einer Passphrase.
     * Die verschlüsselte Datei enthält: Salt (16 Bytes) + IV (16 Bytes) + verschlüsselte Daten.
     * 
     * @param inputPath Pfad zur zu verschlüsselnden Datei
     * @param outputPath Pfad für die verschlüsselte Ausgabedatei
     * @param passphrase Passphrase für die Verschlüsselung
     * @throws IOException bei I/O-Fehlern
     * @throws RuntimeException bei Verschlüsselungsfehlern
     */
    public static void encryptFile(Path inputPath, Path outputPath, String passphrase) throws IOException {
        if (inputPath == null || !Files.exists(inputPath)) {
            throw new IOException("Input file does not exist: " + inputPath);
        }
        if (passphrase == null || passphrase.isEmpty()) {
            throw new IllegalArgumentException("Passphrase darf nicht leer sein");
        }
        
        try (InputStream plaintextInput = Files.newInputStream(inputPath);
             EncryptedInputStream encryptedInput = new EncryptedInputStream(plaintextInput, passphrase);
             OutputStream os = Files.newOutputStream(outputPath)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = encryptedInput.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            
        } catch (RuntimeException e) {
            throw new RuntimeException("Fehler beim Verschlüsseln der Datei " + inputPath + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Entschlüsselt eine Datei, die mit encryptFile verschlüsselt wurde.
     * Die verschlüsselte Datei muss das Format haben: Salt (16 Bytes) + IV (16 Bytes) + verschlüsselte Daten.
     * 
     * @param inputPath Pfad zur verschlüsselten Datei
     * @param outputPath Pfad für die entschlüsselte Ausgabedatei
     * @param passphrase Passphrase für die Entschlüsselung
     * @throws IOException bei I/O-Fehlern
     * @throws RuntimeException bei Entschlüsselungsfehlern
     */
    public static void decryptFile(Path inputPath, Path outputPath, String passphrase) throws IOException {
        if (inputPath == null || !Files.exists(inputPath)) {
            throw new IOException("Input file does not exist: " + inputPath);
        }
        if (passphrase == null || passphrase.isEmpty()) {
            throw new IllegalArgumentException("Passphrase darf nicht leer sein");
        }
        
        try (InputStream encryptedInput = Files.newInputStream(inputPath);
             DecryptedInputStream decryptedInput = new DecryptedInputStream(encryptedInput, passphrase);
             OutputStream os = Files.newOutputStream(outputPath)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = decryptedInput.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            
        } catch (RuntimeException e) {
            throw new RuntimeException("Fehler beim Entschlüsseln der Datei " + inputPath + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Berechnet den MD5-Hash einer Datei, als ob sie verschlüsselt wäre.
     * Dies ist nützlich, um den Hash der verschlüsselten Version zu berechnen,
     * ohne die Datei tatsächlich auf die Festplatte zu schreiben.
     * 
     * @param inputPath Pfad zur Klartext-Datei
     * @param passphrase Passphrase für die (virtuelle) Verschlüsselung
     * @return MD5-Hash als Hex-String oder null bei Fehler
     */
    public static String calcEncryptedMD5(Path inputPath, String passphrase) {
        if (inputPath == null || !Files.exists(inputPath)) {
            System.err.println("Input file does not exist: " + inputPath);
            return null;
        }
        if (passphrase == null || passphrase.isEmpty()) {
            throw new IllegalArgumentException("Passphrase darf nicht leer sein");
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            try (InputStream plaintextInput = Files.newInputStream(inputPath);
                 EncryptedInputStream encryptedInput = new EncryptedInputStream(plaintextInput, passphrase);
                 DigestInputStream digestInput = new DigestInputStream(encryptedInput, md)) {
                
                byte[] buffer = new byte[8192];
                while (digestInput.read(buffer) != -1) {
                    // Daten werden durch DigestInputStream gelesen und MD5 wird berechnet
                }
            }
            
            // Konvertiere MD5 zu Hex-String
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (Exception e) {
            System.err.println("Fehler beim Berechnen des verschlüsselten MD5 für " + inputPath + ": " + e.getMessage());
            return null;
        }
    }
}
