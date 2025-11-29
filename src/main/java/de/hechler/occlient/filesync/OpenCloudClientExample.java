package de.hechler.occlient.filesync;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.sardine.DavResource;

/**
 * Beispielklasse zur Demonstration der Verwendung von OpenCloudClient
 */
public class OpenCloudClientExample {

	public static void main(String[] args) {
		try {
			// Konfiguration aus Datei laden
			OpenCloudConfig config = new OpenCloudConfig();
			
			// Client erstellen
			OpenCloudClient client = new OpenCloudClient(
				config.getServerUrl(), 
				config.getUsername(), 
				config.getPassword()
			);
			
			try {
			// Beispiel 1: Dateien auflisten
			System.out.println("=== Dateien auflisten ===");
			List<String> files = client.listFiles("/");
			files.forEach(System.out::println);
			
			// Beispiel 2: Ressourcen mit Details auflisten
			System.out.println("\n=== Ressourcen mit Details ===");
			List<DavResource> resources = client.listResources("/");
			for (DavResource resource : resources) {
				System.out.printf("%s - %s - %d bytes%n", 
					resource.getName(), 
					resource.isDirectory() ? "DIR" : "FILE",
					resource.getContentLength());
			}
			
			// Beispiel 3: Existenz prüfen
			System.out.println("\n=== Existenz prüfen ===");
			boolean exists = client.exists("/test");
			System.out.println("Pfad /test existiert: " + exists);
			
			// Beispiel 4: Verzeichnis erstellen
			System.out.println("\n=== Verzeichnis erstellen ===");
			if (!client.exists("/test-folder")) {
				client.createDirectory("/test-folder");
				System.out.println("Verzeichnis /test-folder erstellt");
			}
			
			// Beispiel 5: Datei hochladen (Byte-Array)
			System.out.println("\n=== Datei hochladen ===");
			String content = "Dies ist ein Testinhalt";
			byte[] data = content.getBytes(StandardCharsets.UTF_8);
			client.uploadFile("/test-folder/test.txt", data);
			System.out.println("Datei hochgeladen");
			
			// Beispiel 6: Datei herunterladen
			System.out.println("\n=== Datei herunterladen ===");
			try (InputStream in = client.downloadFile("/test-folder/test.txt")) {
				String downloaded = new String(in.readAllBytes(), StandardCharsets.UTF_8);
				System.out.println("Heruntergeladener Inhalt: " + downloaded);
			}
			
			// Beispiel 7: Datei kopieren
			System.out.println("\n=== Datei kopieren ===");
			client.copy("/test-folder/test.txt", "/test-folder/test-copy.txt");
			System.out.println("Datei kopiert");
			
			// Beispiel 8: Datei verschieben/umbenennen
			System.out.println("\n=== Datei verschieben ===");
			client.move("/test-folder/test-copy.txt", "/test-folder/test-renamed.txt");
			System.out.println("Datei verschoben/umbenannt");
			
			// Beispiel 9: Datei löschen
			System.out.println("\n=== Datei löschen ===");
			client.delete("/test-folder/test-renamed.txt");
			System.out.println("Datei gelöscht");
			
			// Beispiel 10: InputStream hochladen
			System.out.println("\n=== Datei mit InputStream hochladen ===");
			String streamContent = "Inhalt über InputStream";
			InputStream streamData = new ByteArrayInputStream(
				streamContent.getBytes(StandardCharsets.UTF_8));
			client.uploadFile("/test-folder/stream-test.txt", streamData);
			System.out.println("Datei über InputStream hochgeladen");
			
			} catch (Exception e) {
				System.err.println("Fehler: " + e.getMessage());
				e.printStackTrace();
			} finally {
				// Verbindung schließen
				client.close();
				System.out.println("\nVerbindung geschlossen");
			}
			
		} catch (Exception e) {
			System.err.println("Fehler beim Laden der Konfiguration: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
