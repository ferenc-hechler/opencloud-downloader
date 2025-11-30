package de.hechler.occlient.filesync;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.Header;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.github.sardine.impl.SardineImpl;

public class OpenCloudClient {

	private Sardine sardine;
	private String baseUrl;

	/**
	 * Erstellt einen neuen WebDAV-Client für den Zugriff auf einen Cloud-Server
	 * 
	 * @param url      Die Basis-URL des WebDAV-Servers
	 * @param user     Der Benutzername für die Authentifizierung
	 * @param password Das Passwort für die Authentifizierung
	 */
	public OpenCloudClient(String url, String user, String password) {
		// Verbindung zum WebDAV-Server herstellen
		this.sardine = SardineFactory.begin(user, password);
		this.baseUrl = url.endsWith("/") ? url : url + "/";
	}
	
	public record FileInfo(String name, boolean isDirectory, long contentLength, Date last_modified, String md5) {}
	
	/**
	 * Listet alle Dateien und Ordner im angegebenen Pfad auf
	 * 
	 * @param path Der Pfad, dessen Inhalt aufgelistet werden soll
	 * @return Liste der Datei- und Ordnernamen
	 */
	public List<FileInfo> listFiles(String path) {
		try {
			String fullPath = buildFullPath(path);
			List<DavResource> resources = sardine.list(fullPath);
			
			// Filtere das Elternverzeichnis selbst heraus und gebe FileInfo-Objekte zurück
			return resources.stream()
					.skip(1) // Erstes Element ist das Verzeichnis selbst
					.map(r -> {
						String name = r.getName();
						boolean isDirectory = r.isDirectory();
						long contentLength = r.getContentLength();
						Date last_modified = r.getModified();
						String md5 = r.getCustomProps().get("checksums") != null ? r.getCustomProps().get("checksums").replaceFirst(".*MD5[:]([a-fA-F0-9]{32}).*", "$1").toLowerCase() :  null;
						if (name.endsWith("README3.md")) {
							System.out.println("Found README3.md with md5: " + md5);
						}
						return new FileInfo(name, isDirectory, contentLength, last_modified, md5);
					})
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Auflisten der Dateien im Pfad: " + path, e);
		}
	}
	
	/**
	 * Listet alle Ressourcen mit Details im angegebenen Pfad auf
	 * 
	 * @param path Der Pfad, dessen Inhalt aufgelistet werden soll
	 * @return Liste der DavResource-Objekte mit detaillierten Informationen
	 */
	public List<DavResource> listResources(String path) {
		try {
			String fullPath = buildFullPath(path);
			List<DavResource> resources = sardine.list(fullPath);
			// Erstes Element (das Verzeichnis selbst) überspringen
			return resources.stream().skip(1).collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Auflisten der Ressourcen im Pfad: " + path, e);
		}
	}
	
	/**
	 * Prüft, ob ein Pfad existiert
	 * 
	 * @param path Der zu prüfende Pfad
	 * @return true wenn der Pfad existiert, sonst false
	 */
	public boolean exists(String path) {
		try {
			String fullPath = buildFullPath(path);
			return sardine.exists(fullPath);
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Prüfen der Existenz von: " + path, e);
		}
	}
	
	/**
	 * Erstellt ein neues Verzeichnis
	 * 
	 * @param path Der Pfad des zu erstellenden Verzeichnisses
	 */
	public void createDirectory(String path) {
		try {
			String fullPath = buildFullPath(path);
			sardine.createDirectory(fullPath);
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Erstellen des Verzeichnisses: " + path, e);
		}
	}
	
	/**
	 * Lädt eine Datei vom Server herunter
	 * 
	 * @param path Der Pfad zur Datei auf dem Server
	 * @return InputStream mit dem Inhalt der Datei
	 */
	public InputStream downloadFile(String path) {
		try {
			String fullPath = buildFullPath(path);
			return sardine.get(fullPath);
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Herunterladen der Datei: " + path, e);
		}
	}
	
	/**
	 * Lädt eine Datei auf den Server hoch
	 * 
	 * @param path Der Zielpfad auf dem Server
	 * @param data Die hochzuladenden Daten als InputStream
	 */
	public void uploadFile(String path, InputStream data) {
		try {
			// InputStream in Byte-Array konvertieren, um "non-repeatable request entity" Fehler zu vermeiden
			byte[] bytes = data.readAllBytes();
			uploadFile(path, bytes);
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Hochladen der Datei: " + path, e);
		}
	}
	
	/**
	 * Lädt eine Datei auf den Server hoch
	 * 
	 * @param path        Der Zielpfad auf dem Server
	 * @param data        Die hochzuladenden Daten als Byte-Array
	 */
	public void uploadFile(String path, byte[] data) {
		try {
			String fullPath = buildFullPath(path);
			sardine.put(fullPath, data);
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Hochladen der Datei: " + path, e);
		}
	}

	/**
	 * Upload with explicit lastModified (ms since epoch). Attempts to set remote mtime
	 * using the X-OC-Mtime header (seconds since epoch) which is supported by Nextcloud/ownCloud.
	 * If the underlying Sardine implementation supports a put variant with headers it will be used.
	 * Otherwise the header is ignored and a normal upload is performed.
	 *
	 * @param path         remote path
	 * @param data         content bytes
	 * @param lastModified local lastModified in milliseconds since epoch
	 */
	public void uploadFile(String path, byte[] data, long lastModified) {
		String fullPath = buildFullPath(path);
		try {
			// X-OC-Mtime expects seconds
			String mtime = Long.toString(lastModified / 1000L);
			ByteArrayEntity entity = new ByteArrayEntity(data);
			List<Header> headerList = new ArrayList<>();
			headerList.add(new BasicHeader("X-OC-Mtime", mtime));
			((SardineImpl)sardine).put(fullPath, entity, headerList);
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new RuntimeException("Fehler beim Hochladen der Datei mit mtime: " + path, e);
		}
	}

	/**
	 * Upload with InputStream and lastModified (ms since epoch). Reads stream fully and delegates.
	 */
	public void uploadFile(String path, InputStream data, long lastModified) {
		try {
			byte[] bytes = data.readAllBytes();
			uploadFile(path, bytes, lastModified);
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Hochladen der Datei: " + path, e);
		}
	}
	
	/**
	 * Löscht eine Datei oder ein Verzeichnis
	 * 
	 * @param path Der Pfad zur zu löschenden Ressource
	 */
	public void delete(String path) {
		try {
			String fullPath = buildFullPath(path);
			sardine.delete(fullPath);
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Löschen von: " + path, e);
		}
	}
	
	/**
	 * Verschiebt oder benennt eine Datei/ein Verzeichnis um
	 * 
	 * @param sourcePath Der Quellpfad
	 * @param destPath   Der Zielpfad
	 */
	public void move(String sourcePath, String destPath) {
		try {
			String fullSourcePath = buildFullPath(sourcePath);
			String fullDestPath = buildFullPath(destPath);
			sardine.move(fullSourcePath, fullDestPath);
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Verschieben von " + sourcePath + " nach " + destPath, e);
		}
	}
	
	/**
	 * Kopiert eine Datei oder ein Verzeichnis
	 * 
	 * @param sourcePath Der Quellpfad
	 * @param destPath   Der Zielpfad
	 */
	public void copy(String sourcePath, String destPath) {
		try {
			String fullSourcePath = buildFullPath(sourcePath);
			String fullDestPath = buildFullPath(destPath);
			sardine.copy(fullSourcePath, fullDestPath);
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Kopieren von " + sourcePath + " nach " + destPath, e);
		}
	}
	
	/**
	 * Schließt die Verbindung zum WebDAV-Server
	 */
	public void close() {
		try {
			if (sardine != null) {
				sardine.shutdown();
			}
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Schließen der Verbindung", e);
		}
	}
	
	/**
	 * Hilfsmethode zum Erstellen des vollständigen Pfads
	 * 
	 * @param path Der relative Pfad
	 * @return Der vollständige URL-Pfad
	 */
	private String buildFullPath(String path) {
		if (path == null || path.isEmpty()) {
			return baseUrl;
		}
		String cleanPath = path.startsWith("/") ? path.substring(1) : path;
		return baseUrl + cleanPath;
	}
	
}