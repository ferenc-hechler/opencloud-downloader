# OpenCloudClient - WebDAV Client für Java

Eine einfache Java-Bibliothek für WebDAV-Operationen basierend auf der Sardine-Bibliothek.

## Funktionen

Die `OpenCloudClient`-Klasse bietet folgende Methoden:

### Konstruktor
- `OpenCloudClient(String url, String user, String password)` - Erstellt eine neue WebDAV-Verbindung

### Datei- und Verzeichnisoperationen
- `listFiles(String path)` - Listet alle Datei- und Ordnernamen in einem Pfad auf
- `listResources(String path)` - Listet alle Ressourcen mit Details (DavResource-Objekte) auf
- `exists(String path)` - Prüft, ob ein Pfad existiert
- `createDirectory(String path)` - Erstellt ein neues Verzeichnis
- `downloadFile(String path)` - Lädt eine Datei herunter (gibt InputStream zurück)
- `uploadFile(String path, InputStream data)` - Lädt eine Datei hoch (InputStream)
- `uploadFile(String path, byte[] data)` - Lädt eine Datei hoch (Byte-Array)
- `delete(String path)` - Löscht eine Datei oder ein Verzeichnis
- `move(String sourcePath, String destPath)` - Verschiebt oder benennt eine Datei/Verzeichnis um
- `copy(String sourcePath, String destPath)` - Kopiert eine Datei oder ein Verzeichnis
- `close()` - Schließt die Verbindung zum WebDAV-Server

## Verwendung

### Grundlegendes Beispiel

```java
// Client erstellen
OpenCloudClient client = new OpenCloudClient(
    "https://your-server.com/webdav",
    "username",
    "password"
);

try {
    // Dateien auflisten
    List<String> files = client.listFiles("/");
    files.forEach(System.out::println);
    
    // Datei hochladen
    String content = "Hallo Welt";
    byte[] data = content.getBytes(StandardCharsets.UTF_8);
    client.uploadFile("/test.txt", data);
    
    // Datei herunterladen
    try (InputStream in = client.downloadFile("/test.txt")) {
        String downloaded = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        System.out.println(downloaded);
    }
    
    // Datei löschen
    client.delete("/test.txt");
    
} finally {
    client.close();
}
```

### Erweiterte Beispiele

Siehe `OpenCloudClientExample.java` für umfassendere Beispiele aller verfügbaren Funktionen.

## Abhängigkeiten

Die Bibliothek verwendet die Sardine WebDAV-Bibliothek:

```xml
<dependency>
    <groupId>com.github.lookfirst</groupId>
    <artifactId>sardine</artifactId>
    <version>5.10</version>
</dependency>
```

## Kompatibilität

- Java 21 oder höher
- Kompatibel mit allen WebDAV-Servern (Nextcloud, ownCloud, Apache WebDAV, etc.)

## Fehlerbehandlung

Alle Methoden werfen `RuntimeException` bei Fehlern, die die ursprüngliche `IOException` als Ursache enthalten. Dies ermöglicht eine einfache Fehlerbehandlung:

```java
try {
    client.uploadFile("/path/file.txt", data);
} catch (RuntimeException e) {
    System.err.println("Upload fehlgeschlagen: " + e.getMessage());
    e.printStackTrace();
}
```

## Best Practices

1. **Immer `close()` aufrufen**: Verwenden Sie einen `try-finally`-Block oder try-with-resources, um sicherzustellen, dass die Verbindung geschlossen wird.
2. **InputStreams schließen**: Wenn Sie `downloadFile()` verwenden, schließen Sie den InputStream nach der Verwendung.
3. **Pfadformate**: Pfade können mit oder ohne führenden `/` angegeben werden - der Client normalisiert diese automatisch.

## Lizenz

Dieses Projekt verwendet die Sardine-Bibliothek, die unter der Apache License 2.0 lizenziert ist.
