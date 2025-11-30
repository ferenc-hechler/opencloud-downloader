package de.hechler.occlient.filesync;

import java.io.IOException;
import java.io.InputStream;
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
            // convert to hex
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            System.err.println("Fehler beim Berechnen der MD5-Prüfsumme für " + path + ": " + e.getMessage());
            return null;
        }
    }
}
