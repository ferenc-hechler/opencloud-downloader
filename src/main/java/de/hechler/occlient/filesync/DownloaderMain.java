package de.hechler.occlient.filesync;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import de.hechler.occlient.filesync.DownloaderMain.SyncConfig.SyncEntry;

public class DownloaderMain {
	
	public static class SyncConfig {
		public static class SyncEntry {
			public String localFolder;
			public String remoteFolder;
			public List<String> ignore;
		}
		public List<SyncEntry> sync;
	}		
	
	public static void main(String[] args) {
		
		// test args
		if (args == null || args.length == 0) {
//			args = new String[] { "opencloud-downloader-sync-LENOVO.yaml" };
			args = new String[] { "opencloud-downloader-sync-relative.yaml" };
		}
		
		// Erwartet: Pfad zur Sync-Config-Datei (z.B. opencloud-downloader-syncs.txt)
		if (args == null || args.length < 1) {
			System.err.println("Usage: java -jar oc-downloader.jar <config-yaml>");
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
			config = new OpenCloudConfig();
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
		
		SyncConfig sConf = readSyncConfg(syncConfigYaml);
		
		for (SyncEntry sync : sConf.sync) {
			String localFolder = sync.localFolder;
			String remoteFolder = sync.remoteFolder;
			System.out.println("Processing mapping: remote='" + remoteFolder + "' -> local='" + localFolder + "'");
			if (localFolder.isEmpty() || remoteFolder.isEmpty()) {
				System.err.println("invalid mapping " + remoteFolder + " -> " + localFolder);
				System.exit(6);
			}
			List<PathMatcher> ignores = null;
			if (sync.ignore != null) {
				ignores = new ArrayList<>();
				for (String ignorePattern : sync.ignore) {
					ignores.add(FileSystems.getDefault().getPathMatcher("glob:" + ignorePattern));
				}
			}
			folderSync.syncLocalFolder(localFolder, remoteFolder, ignores);
		}
	}

	private static SyncConfig readSyncConfg(String syncConfigYaml) {
		Path path = Paths.get(syncConfigYaml);
		if (!Files.exists(path)) {
			System.err.println("Sync config yaml not found: " + syncConfigYaml);
			System.exit(3);
		}
		Yaml yaml = new Yaml(new Constructor(SyncConfig.class, new LoaderOptions()));
		try (InputStream is = Files.newInputStream(path)) {
			SyncConfig syncConfig = yaml.load(is);
			return syncConfig;
		} catch (IOException e) {
			System.err.println("Error reading sync config yaml: " + e.getMessage());
			System.exit(4);
		}
		return null; // never reached
	}
}