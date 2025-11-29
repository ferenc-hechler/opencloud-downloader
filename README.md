# Info for OpenCloud eu and WebDAV

https://docs.opencloud.eu/de/docs/user/admin/web-dav

WebDAV URL: https://opencloud.k8s.cluster-4.de/remote.php/dav/spaces/4a4a14b1-c59e-4d38-942c-ec6c5ddaabea$3e4119b2-4807-4457-af65-a99b0f74faef
WebDAV Path: /spaces/4a4a14b1-c59e-4d38-942c-ec6c5ddaabea%243e4119b2-4807-4457-af65-a99b0f74faef

## Konfiguration

Die Zugangsdaten werden aus einer `config.properties` Datei geladen, die nicht in Git eingecheckt wird (aus Sicherheitsgründen).

### Setup:

1. Kopieren Sie `config.properties.template` nach `config.properties`:
   ```cmd
   copy config.properties.template config.properties
   ```

2. Bearbeiten Sie `config.properties` und tragen Sie Ihre Zugangsdaten ein:
   - `server.url`: Die WebDAV-Server-URL
   - `server.username`: Ihr Benutzername
   - `server.password`: Ihr Passwort

3. Die `config.properties` Datei wird automatisch von Git ignoriert (siehe `.gitignore`)

**Wichtig**: Committen Sie niemals die `config.properties` Datei mit echten Zugangsdaten in Git!


