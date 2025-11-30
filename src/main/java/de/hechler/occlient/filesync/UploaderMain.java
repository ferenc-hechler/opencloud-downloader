package de.hechler.occlient.filesync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class UploaderMain {
	
	public static void main(String[] args) {
		
		// test args
		if (args == null || args.length == 0) {
			args = new String[] { "opencloud-uploader-syncs-relative.txt" };
		}
		
		// Erwartet: Pfad zur Sync-Config-Datei (z.B. opencloud-uploader-syncs.txt)
		if (args == null || args.length < 1) {
			System.err.println("Usage: java -jar oc-uploader.jar <upload-config-file>");
			System.err.println("Each line in the config must have format: <remoteFolder>=<localFolder>");
			System.exit(1);
		}
		
		String uploadConfigPath = args[0];
		System.out.println("Using upload config: " + uploadConfigPath);
		
		// Konfiguration aus Datei laden
		OpenCloudConfig config;
		try {
			config = new OpenCloudConfig("opencloud-uploader.properties");
		} catch (RuntimeException e) {
			System.err.println("Fehler beim Laden der Konfiguration: " + e.getMessage());
			e.printStackTrace();
			System.exit(2);
			return;
		}
		
		// Client erstellen
		OpenCloudClient client = new OpenCloudClient(
			config.getServerUrl(), 
			config.getUsername(), 
			config.getPassword()
		);
		
		FolderSync folderSync = new FolderSync(client);
		
		Path cfg = Paths.get(uploadConfigPath);
		if (!Files.exists(cfg)) {
			System.err.println("Sync config file not found: " + uploadConfigPath);
			System.exit(3);
		}
		
		try (Stream<String> lines = Files.lines(cfg)) {
			lines.map(String::trim)
			     .filter(l -> !l.isEmpty())
			     .filter(l -> !l.startsWith("#") && !l.startsWith("//"))
			     .forEach(line -> {
					int eq = line.indexOf('=');
					if (eq <= 0) {
						System.err.println("Skipping invalid line (no '='): " + line);
						return;
					}
					String remoteFolder = line.substring(0, eq).trim();
					String localFolder = line.substring(eq + 1).trim();
					if (localFolder.isEmpty() || remoteFolder.isEmpty()) {
						System.err.println("Skipping invalid mapping (empty side): " + line);
						return;
					}
					System.out.println("Processing mapping: remote='" + remoteFolder + "' -> local='" + localFolder + "'");
					try {
						folderSync.syncRemoteFolder(remoteFolder, localFolder);
					} catch (Exception e) {
						System.err.println("Error syncing mapping '" + line + "': " + e.getMessage());
						e.printStackTrace();
					}
				});
		} catch (IOException e) {
			System.err.println("Cannot read upload config file: " + uploadConfigPath + " - " + e.getMessage());
			System.exit(4);
		} finally {
			try {
				client.close();
			} catch (Exception ignore) {}
		}
	}
}