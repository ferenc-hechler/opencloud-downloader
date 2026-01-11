package de.hechler.occlient.filesync;

import de.hechler.occlient.filesync.SyncConfig.SyncEntry;

public class UploaderMain {
	
	public static void main(String[] args) {
		
//		// test args
		if (args == null || args.length == 0) {
			args = new String[] { "opencloud-uploader-sync-relative.yaml" };
		}
		
		// Erwartet: Pfad zur Sync-Config-Datei (z.B. opencloud-downloader-syncs.txt)
		if (args == null || args.length < 1) {
			System.err.println("Usage: java -jar oc-uploader.jar <config-yaml>");
			System.err.println("------- YAML SYNTAX -------");
			System.err.println("sync:");
			System.err.println("  - localFolder: <local-folder1>");
			System.err.println("    remoteFolder: <remote-folder1>");
			System.err.println("    ignore:");
			System.err.println("      - <glob-pattern-to-ignore1>");
			System.err.println("      - <glob-pattern-to-ignore2>");
			System.err.println("  - localFolder: <local-folder2>");
			System.err.println("    remoteFolder: <remote-folder2>");
			System.err.println("------- ----------- -------");
			System.exit(1);
		}
		
		String syncConfigYaml = args[0];
		System.out.println("Using sync config yaml: " + syncConfigYaml);
		
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
		
		SyncConfig sConf = SyncConfig.load(syncConfigYaml);
		
		for (SyncEntry sync : sConf.sync) {
			String localFolder = sync.localFolder;
			String remoteFolder = sync.remoteFolder;
			System.out.println("Processing mapping: local='" + localFolder + "' -> remote='" + remoteFolder + "'");
			if (localFolder.isEmpty() || remoteFolder.isEmpty()) {
				System.err.println("invalid mapping " + remoteFolder + " -> " + localFolder);
				System.exit(6);
			}
			folderSync.syncRemoteFolder(remoteFolder, localFolder, sync.getIgnoreMatchers());
		}
		
	}
}