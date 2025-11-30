package de.hechler.occlient.filesync;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.sardine.DavResource;

/**
 * Beispielklasse zur Demonstration der Verwendung von OpenCloudClient
 */
public class OpenCloudClientReadOnlyExample {

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
			List<OpenCloudClient.FileInfo> files = client.listFiles("/");
			for (OpenCloudClient.FileInfo f : files) {
				System.out.printf("%s\t%s\t%d bytes\t%s%n",
					f.name(),
					f.isDirectory() ? "DIR" : "FILE",
					f.contentLength(),
					f.last_modified() != null ? f.last_modified().toString() : "-"
				);
			}
			
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
			
			// Beispiel 6: Datei herunterladen
			System.out.println("\n=== Datei herunterladen ===");
			try (InputStream in = client.downloadFile("/test-folder/test.txt")) {
				String downloaded = new String(in.readAllBytes(), StandardCharsets.UTF_8);
				System.out.println("Heruntergeladener Inhalt: " + downloaded);
			}
			
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