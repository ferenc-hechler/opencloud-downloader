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


public class SyncConfig {
	
	public static class SyncEntry {
		public String localFolder;
		public String remoteFolder;
		public List<String> ignore;
		private List<PathMatcher> ignoreMatchers;
		public List<PathMatcher> getIgnoreMatchers() {
			if (ignoreMatchers == null) {
				ignoreMatchers = new ArrayList<>();
				if (ignore != null) {
					for (String pattern : ignore) {
						PathMatcher pm = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
						ignoreMatchers.add(pm);
					}
				}
			}
			return ignoreMatchers;
		}
	}
	public List<SyncEntry> sync;
	
	public static SyncConfig load(String yamlFile) {
		Path path = Paths.get(yamlFile);
		if (!Files.exists(path)) {
			System.err.println("Sync config yaml not found: " + yamlFile);
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
