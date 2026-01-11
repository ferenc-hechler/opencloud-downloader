package de.hechler.occlient.filesync;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FolderSync {

	protected OpenCloudClient client;
	
	public FolderSync(OpenCloudClient client) {
		this.client = client;
	}

	public void syncLocalFolder(String localFolder, String remoteFolder, List<PathMatcher> ignorePatterns) {
		// check local folder, create if not exists
		// update local folder to match remote folder
		// compare local and remote files and sync
		// for comparison use file size and last modified date and md5 if available
		// download missing files from remote to local
		// delete local files not present in remote
		System.out.println("Syncing local folder '" + localFolder + "' from remote folder '" + remoteFolder + "'");
		
		Path localPath = Paths.get(localFolder);
		try {
			if (!Files.exists(localPath)) {
				Files.createDirectories(localPath);
			}
		} catch (IOException e) {
			throw new RuntimeException("Konnte lokalen Ordner nicht erstellen: " + localFolder, e);
		}
		
		// Hole Remote-Einträge
		List<OpenCloudClient.FileInfo> remoteEntries;
		try {
			remoteEntries = client.listFiles(remoteFolder);
		} catch (Exception e) {
			throw new RuntimeException("Fehler beim Listen des Remote-Ordners: " + remoteFolder, e);
		}
		
		// Sammle Remote-Namen für Lösch-Entscheidung
		Set<String> remoteNames = new HashSet<>();
		for (OpenCloudClient.FileInfo fi : remoteEntries) {
			if (checkIgnore(fi.name(), ignorePatterns)) {
				continue;
			}
			remoteNames.add(fi.name());
			Path target = localPath.resolve(fi.name());
			if (fi.isDirectory()) {
				// ensure directory exists and recurse
				try {
					if (Files.exists(target) && !Files.isDirectory(target)) {
						// conflict: local is file, remote is directory -> delete local file
						Files.deleteIfExists(target);
					}
					if (!Files.exists(target)) {
						Files.createDirectories(target);
					}
					// build remote child path
					String childRemote = remoteFolder.endsWith("/") ? remoteFolder + fi.name() : remoteFolder + "/" + fi.name();
					syncLocalFolder(target.toString(), childRemote, ignorePatterns);
				} catch (IOException e) {
					System.err.println("  Fehler beim Erstellen/Syncen von Verzeichnis: " + target + " - " + e.getMessage());
				}
			} else {
				// file: decide whether to download
				boolean download = false;
				try {
					if (Files.exists(target) && Files.isDirectory(target)) {
						// conflict: local is dir, remote is file -> delete local dir
						deleteRecursively(target);
						download = true;
					} else if (!Files.exists(target)) {
						download = true;
					} else {
						long localSize = Files.size(target);
						long remoteSize = fi.contentLength();
						long localLast = Files.getLastModifiedTime(target).toMillis();
						long remoteLast = fi.last_modified() != null ? fi.last_modified().getTime() : 0L;
						// consider difference if size differs or remote newer (allow small clock skew)
						if (localSize != remoteSize) {
							download = true;
						} else if (Math.abs(remoteLast - localLast) >= 1000) {
							download = true;
						}
						if (download && (localSize == remoteSize) && fi.md5() != null) {
							// if md5 available, check it
							String localMd5 = ChecksumUtil.calculateMD5(target);
							if (fi.md5().equalsIgnoreCase(localMd5)) {
								download = false;
								// set last modified time to remote's timestamp if available
								if (fi.last_modified() != null) {
									Files.setLastModifiedTime(target, FileTime.fromMillis(fi.last_modified().getTime()));
								}							}
						}
					}
				} catch (IOException e) {
					System.err.println("  Fehler beim Prüfen der lokalen Datei: " + target + " - " + e.getMessage());
					download = true;
				}

				if (download) {
					String remoteFilePath = remoteFolder.endsWith("/") ? remoteFolder + fi.name() : remoteFolder + "/" + fi.name();
					System.out.println("  Downloading: " + remoteFilePath + " -> " + target);
					try (InputStream in = client.downloadFile(remoteFilePath)) {
						// ensure parent exists
						if (target.getParent() != null && !Files.exists(target.getParent())) {
							Files.createDirectories(target.getParent());
						}
						// write to temp file then move atomically
						Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
						Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
						try {
							Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
						} catch (IOException moveEx) {
							// fallback if atomic move not supported
							Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
						}
						// set last modified time to remote's timestamp if available
						if (fi.last_modified() != null) {
							Files.setLastModifiedTime(target, FileTime.fromMillis(fi.last_modified().getTime()));
						}
					} catch (Exception e) {
						System.err.println("  Fehler beim Herunterladen der Datei " + remoteFilePath + ": " + e.getMessage());
					}
				}
			}
		}
		
		// Lösche lokale Dateien/Verzeichnisse, die nicht in remoteNames sind
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(localPath)) {
			for (Path p : ds) {
				String name = p.getFileName().toString();
				if (checkIgnore(name, ignorePatterns)) {
					continue;
				}
				if (!remoteNames.contains(name)) {
					// delete file or directory recursively
					try {
						deleteRecursively(p);
						System.out.println("  Deleted local entry not present on remote: " + p);
					} catch (IOException e) {
						System.err.println("  Fehler beim Löschen lokaler Datei/Verzeichnis: " + p + " - " + e.getMessage());
					}
				}
			}
		} catch (IOException e) {
			System.err.println("  Fehler beim Auflisten des lokalen Ordners: " + localPath + " - " + e.getMessage());
		}
	}
	
	private boolean checkIgnore(String name, List<PathMatcher> ignorePatterns) {
		if (ignorePatterns == null) {
			return false;
		}
		for (PathMatcher pattern : ignorePatterns) {
			if (pattern.matches(Paths.get(name))) {
				return true;
			}
		}
		return false;
	}

	private void deleteRecursively(Path path) throws IOException {
		if (Files.isDirectory(path)) {
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
				for (Path child : ds) {
					deleteRecursively(child);
				}
			}
		}
		Files.deleteIfExists(path);
	}

	public void syncRemoteFolder(String remoteFolder, String localFolder, List<PathMatcher> ignorePatterns) {
		System.out.println("Syncing remote folder '" + remoteFolder + "' with local folder '" + localFolder + "'");
		Path localPath = Paths.get(localFolder);
		// If local doesn't exist -> remove remote
		if (!Files.exists(localPath)) {
			System.out.println("  Local folder does not exist: " + localFolder + " -> deleting remote if exists");
			try {
				if (client.exists(remoteFolder)) {
					deleteRemoteRecursively(remoteFolder);
				}
			} catch (Exception e) {
				System.err.println("  Error deleting remote folder " + remoteFolder + ": " + e.getMessage());
			}
			return;
		}
		
		// ensure remote folder exists
		try {
			if (!client.exists(remoteFolder)) {
				client.createDirectory(remoteFolder);
			}
		} catch (Exception e) {
			throw new RuntimeException("Fehler beim Sicherstellen des Remote-Ordners: " + remoteFolder, e);
		}
		
		// list remote entries
		List<OpenCloudClient.FileInfo> remoteEntries;
		try {
			remoteEntries = client.listFiles(remoteFolder);
		} catch (Exception e) {
			throw new RuntimeException("Fehler beim Listen des Remote-Ordners: " + remoteFolder, e);
		}
		// map remote by name
		Set<String> remoteNames = new HashSet<>();
		for (OpenCloudClient.FileInfo fi : remoteEntries) {
			if (checkIgnore(fi.name(), ignorePatterns)) {
				continue;
			}
			remoteNames.add(fi.name());
		}
		
		// iterate local entries and upload/update
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(localPath)) {
			for (Path p : ds) {
				String name = p.getFileName().toString();
				if (checkIgnore(name, ignorePatterns)) {
					continue;
				}
				String remotePath = remoteFolder.endsWith("/") ? remoteFolder + name : remoteFolder + "/" + name;
				if (Files.isDirectory(p)) {
					// local is directory
					// if remote exists and is file -> delete remote file
					OpenCloudClient.FileInfo rem = findRemote(remoteEntries, name);
					try {
						if (rem != null && !rem.isDirectory()) {
							client.delete(remotePath);
							remoteNames.remove(name);
						}
						// ensure remote dir exists
						if (!client.exists(remotePath)) {
							client.createDirectory(remotePath);
						}
						// recurse
						syncRemoteFolder(remotePath, p.toString(), ignorePatterns);
					} catch (Exception e) {
						System.err.println("  Error syncing directory " + p + " -> " + remotePath + ": " + e.getMessage());
					}
				} else if (Files.isRegularFile(p)) {
					// local is file -> determine upload needed
					boolean upload = false;
					OpenCloudClient.FileInfo rem = findRemote(remoteEntries, name);
					try {
						if (rem != null && rem.isDirectory()) {
							// conflict: remote is directory -> delete it
							deleteRemoteRecursively(remotePath);
							upload = true;
						} else if (rem == null) {
							upload = true;
						} else {
							long localSize = Files.size(p);
							long remoteSize = rem.contentLength();
							long localLast = Files.getLastModifiedTime(p).toMillis();
							long remoteLast = rem.last_modified() != null ? rem.last_modified().getTime() : 0L;
							// rule: if same size AND same lastModified -> skip
							if (localSize == remoteSize && remoteLast == localLast) {
								upload = false;
							} else if (localSize == remoteSize && rem.md5() != null) {
								String localMd5 = ChecksumUtil.calculateMD5(p);
								if (localMd5 != null && rem.md5().equalsIgnoreCase(localMd5)) {
									upload = false;
									// set last modified time to remote's timestamp if available
									if (rem.last_modified() != null) {
										Files.setLastModifiedTime(p, FileTime.fromMillis(rem.last_modified().getTime()));
									}
								} else {
									upload = true;
								}
						} else {
							upload = true;
						}
					}
					} catch (IOException e) {
						System.err.println("  Fehler beim Prüfen der lokalen Datei: " + p + " - " + e.getMessage());
						upload = true;
					}
					
					if (upload) {
						System.out.println("  Uploading: " + p + " -> " + remotePath);
						try (InputStream in = Files.newInputStream(p)) {
							// ensure parent exists remotely
							// (we assume parent exists because we created remoteFolder früher)
							client.uploadFile(remotePath, in, Files.getLastModifiedTime(p).toMillis());
							remoteNames.add(name);
						} catch (Exception e) {
							System.err.println("  Fehler beim Hochladen der Datei " + p + ": " + e.getMessage());
						}
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Lesen des lokalen Ordners: " + localPath, e);
		}
		
		// delete remote entries that do not exist locally
		for (OpenCloudClient.FileInfo fi : remoteEntries) {
			if (checkIgnore(fi.name(), ignorePatterns)) {
				continue;
			}
			if (!Files.exists(localPath.resolve(fi.name()))) {
				String remotePath = remoteFolder.endsWith("/") ? remoteFolder + fi.name() : remoteFolder + "/" + fi.name();
				try {
					deleteRemoteRecursively(remotePath);
					System.out.println("  Deleted remote entry not present locally: " + remotePath);
				} catch (Exception e) {
					System.err.println("  Error deleting remote entry " + remotePath + ": " + e.getMessage());
				}
			}
		}
	}
	
	private void deleteRemoteRecursively(String remotePath) {
		try {
			// try listing; if it fails or returns empty, just delete the resource
			List<OpenCloudClient.FileInfo> entries = null;
			try {
				entries = client.listFiles(remotePath);
			} catch (Exception e) {
				// might be a file or non-listable resource
				}
			if (entries != null && !entries.isEmpty()) {
				for (OpenCloudClient.FileInfo fi : entries) {
					String childRemote = remotePath.endsWith("/") ? remotePath + fi.name() : remotePath + "/" + fi.name();
					if (fi.isDirectory()) {
						deleteRemoteRecursively(childRemote);
					} else {
						client.delete(childRemote);
					}
				}
			}
			// finally delete the resource itself
			client.delete(remotePath);
		} catch (Exception e) {
			throw new RuntimeException("Fehler beim rekursiven Löschen des Remote-Pfads: " + remotePath, e);
		}
	}
	
	private OpenCloudClient.FileInfo findRemote(List<OpenCloudClient.FileInfo> remoteEntries, String name) {
		if (remoteEntries == null) return null;
		for (OpenCloudClient.FileInfo fi : remoteEntries) {
			if (fi.name().equals(name)) return fi;
		}
		return null;
	}

}