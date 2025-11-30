package de.hechler.occlient.filesync;

public class Application {
	
	public static void main(String[] args) {
		
		// Testweise Argumente setzen
		if (args == null || args.length == 0) {
			args = new String[] { "./testdata/SKIPWindows", "SKIPWindows" };
		}
		
		// Erwartete Argumente: <local_folder> <remote_folder>
		if (args == null || args.length < 2) {
			System.err.println("Usage: java -jar filesync.jar <local_folder> <remote_folder>");
			System.err.println("Example: java -jar filesync.jar C:\\data \"/remote/path\"");
			System.exit(1);
		}
		
		String localFolder = args[0];
		String remoteFolder = args[1];
		
		System.out.println("FileSync Application Started");
		System.out.println("Local folder: " + localFolder);
		System.out.println("Remote folder: " + remoteFolder);
		
		// Konfiguration aus Datei laden
		OpenCloudConfig config = new OpenCloudConfig();
		
		// Client erstellen
		OpenCloudClient client = new OpenCloudClient(
			config.getServerUrl(), 
			config.getUsername(), 
			config.getPassword()
		);
		
		// Ordner synchronisieren
		FolderSync folderSync = new FolderSync(client);
		folderSync.syncLocalFolder(localFolder, remoteFolder);
		
	}	
}