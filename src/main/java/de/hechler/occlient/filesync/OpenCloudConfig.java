package de.hechler.occlient.filesync;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Konfigurationsmanager für OpenCloudClient
 * Lädt die Konfiguration aus einer config.properties Datei
 */
public class OpenCloudConfig {
    
    // Standard-Dateiname im Home-Verzeichnis des Users
    private static final String DEFAULT_CONFIG_FILE = "opencloud-downloader.properties";
    
    private final Properties properties;
    
    /**
     * Erstellt eine Konfiguration aus der Standard-Datei
     * (Datei: <user.home>/opencloud-downloader.properties)
     * 
     * @throws RuntimeException wenn die Datei nicht gelesen werden kann
     */
    public OpenCloudConfig() {
        this(DEFAULT_CONFIG_FILE);
    }
    
    /**
     * Erstellt eine Konfiguration aus der angegebenen Datei
     * Wenn configFile null oder leer ist, wird die Datei
     * "opencloud-downloader.properties" im Home-Verzeichnis des Users verwendet.
     * 
     * @param configFile Pfad zur Konfigurationsdatei (kann relativ oder absolut sein)
     * @throws RuntimeException wenn die Datei nicht gelesen werden kann
     */
    public OpenCloudConfig(String configFile) {
    	try {
	        properties = new Properties();
	        
	        // Bestimme den zu verwendenden Pfad
	        Path configPath;
            configPath = Paths.get(configFile);
            // Falls ein relativer Pfad übergeben wurde, verwende das user-home Verzeichnis
            if (!configPath.isAbsolute()) {
	            String userHome = System.getProperty("user.home");
	            // Plattformunabhängig: Dateipfad im Home-Verzeichnis
	            configPath = Paths.get(userHome).resolve(configPath);
	        }
	        
	        if (!Files.exists(configPath)) {
	            // Wenn nicht gefunden, werfe eine hilfreiche Fehlermeldung
	            String message = String.format(
	                "Konfigurationsdatei nicht gefunden: %s%n" +
	                "Bitte erstellen Sie die Datei basierend auf config.properties.template",
	                configPath.toAbsolutePath()
	            );
	            throw new IOException(message);
	        }
	        
	        try (InputStream input = new FileInputStream(configPath.toFile())) {
	            properties.load(input);
	        }
	        
	        // Gib im Log (stdout) aus, aus welcher Datei die Konfiguration gelesen wurde
	        System.out.println("Konfiguration geladen aus Datei: " + configPath.toAbsolutePath().toString());
	        
	        // Validierung der erforderlichen Eigenschaften
	        validateRequired("server.url");
	        validateRequired("server.username");
	        validateRequired("server.password");
    	} catch (IOException e) {
			throw new RuntimeException("Fehler beim Laden der Konfigurationsdatei: " + e.getMessage(), e);
		}
    }
    
    private void validateRequired(String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty() || 
            value.contains("your-") || value.contains("YOUR_")) {
            throw new IOException(
                "Erforderliche Konfiguration fehlt oder ist nicht ausgefüllt: " + key
            );
        }
    }
    
    /**
     * @return Die Server-URL
     */
    public String getServerUrl() {
        return properties.getProperty("server.url");
    }
    
    /**
     * @return Der Benutzername
     */
    public String getUsername() {
        return properties.getProperty("server.username");
    }
    
    /**
     * @return Das Passwort
     */
    public String getPassword() {
        return properties.getProperty("server.password");
    }
    
    /**
     * Holt einen optionalen Wert aus der Konfiguration
     * 
     * @param key Der Schlüssel
     * @param defaultValue Der Standardwert, wenn der Schlüssel nicht existiert
     * @return Der Wert oder defaultValue
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}