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
	
	private static final String DEFAULT_CONFIG_FILE = "config.properties";
	
	private final Properties properties;
	
	/**
	 * Erstellt eine Konfiguration aus der Standard-Datei config.properties
	 * 
	 * @throws IOException wenn die Datei nicht gelesen werden kann
	 */
	public OpenCloudConfig() throws IOException {
		this(DEFAULT_CONFIG_FILE);
	}
	
	/**
	 * Erstellt eine Konfiguration aus der angegebenen Datei
	 * 
	 * @param configFile Pfad zur Konfigurationsdatei
	 * @throws IOException wenn die Datei nicht gelesen werden kann
	 */
	public OpenCloudConfig(String configFile) throws IOException {
		properties = new Properties();
		
		// Versuche zuerst als absolute Datei
		Path configPath = Paths.get(configFile);
		
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
		
		// Validierung der erforderlichen Eigenschaften
		validateRequired("server.url");
		validateRequired("server.username");
		validateRequired("server.password");
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
