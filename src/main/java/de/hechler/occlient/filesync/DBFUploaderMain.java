package de.hechler.occlient.filesync;

import de.hechler.occlient.filesync.SyncConfig.SyncEntry;
import de.hechler.occlient.filesync.SyncConfig.SyncTransformType;

public class DBFUploaderMain {
	
	public static void main(String[] args) {
		
//		// test args
		if (args == null || args.length == 0) {
			// args = new String[] { "opencloud-uploader-sync-relative.yaml" };
			args = new String[] { "opencloud-dbf-uploader-syncs.yaml" };
		}
		
		// Erwartet: Pfad zur Sync-Config-Datei (z.B. opencloud-downloader-syncs.txt)
		if (args == null || args.length < 1) {
			System.err.println("Usage: java -jar oc-dbf-uploader.jar <config-yaml>");
			System.err.println("------- YAML SYNTAX -------");
			System.err.println("sync:");
			System.err.println("  - localFolder: <local-folder1>");
			System.err.println("    remoteFolder: <remote-folder1>");
			System.err.println("    ignore:");
			System.err.println("      - <glob-pattern-to-ignore1>");
			System.err.println("      - <glob-pattern-to-ignore2>");
			System.err.println("  - localFolder: <local-folder2>");
			System.err.println("    remoteFolder: <remote-folder2>");
			System.err.println("    transform:");
			System.err.println("      type: encrypt|decrypt|none");
			System.err.println("      passphrase: \"<passphrase>\"");
			System.err.println("------- ----------- -------");
			System.exit(1);
		}
		
		String syncConfigYaml = args[0];
		System.out.println("Using sync config yaml: " + syncConfigYaml);
		
		// Konfiguration aus Datei laden
		OpenCloudConfig config;
		try {
			config = new OpenCloudConfig("opencloud-dbf-uploader.properties");
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
		
		SyncConfig sConf = SyncConfig.load(syncConfigYaml);
		
		for (SyncEntry sync : sConf.sync) {
			String localFolder = sync.localFolder;
			String remoteFolder = sync.remoteFolder;
			String encryptPassphrase = null;
			if (sync.transform != null && sync.transform.type == SyncTransformType.encrypt) {
				if (sync.transform.passphrase == null || sync.transform.passphrase.isEmpty()) {
					encryptPassphrase = config.getDefaultPassphrase();
				}
				else {
					encryptPassphrase = sync.transform.passphrase;
				}
				if (encryptPassphrase == null || encryptPassphrase.isEmpty()) {
					System.err.println("Encryption enabled for mapping " + remoteFolder + " -> " + localFolder + " but no passphrase provided in config or default passphrase. Please provide a passphrase in the sync config yaml or in the properties file.");
					System.exit(5);
				}
			}
			System.out.println("Processing mapping: local='" + localFolder + "' -> remote='" + remoteFolder + "'" + (encryptPassphrase != null ? " (encryption enabled)" : ""));
			if (localFolder.isEmpty() || remoteFolder.isEmpty()) {
				System.err.println("invalid mapping " + remoteFolder + " -> " + localFolder);
				System.exit(6);
			}
			folderSync.syncRemoteFolder(remoteFolder, localFolder, sync.getIgnoreMatchers(), encryptPassphrase);
		}
		
	}
}