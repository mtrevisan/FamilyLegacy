/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.updater;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Lists the optional components of the catalog with what is installed, and installs them: the file is downloaded, checked against its
 * SHA-256 and put in the folder of its target (a {@code .zip} is unpacked, only plain files of the kinds the target accepts; nothing is
 * written outside the folder).
 * <p>
 * What the application installs is remembered (with the SHA-256 of the file) in {@code components.properties}, beside the folders of the
 * targets, so that a new file in the release is told as an update.
 * </p>
 */
public final class ComponentManager{

	/** Where the components that the application installed are remembered, beside the folders of the targets. */
	public static final String REGISTRY = "components.properties";

	/** What a target accepts, by extension (lower case). */
	private static final Map<String, Set<String>> ACCEPTED = Map.of(
		"fonts", Set.of("ttf", "otf", "ttc", "txt"),
		"plugins", Set.of("jar", "txt"));
	private static final int MAX_FILES = 200;
	private static final long MAX_UNPACKED_BYTES = 512L * 1024 * 1024;


	/** What can be done with a component. */
	public enum State{
		/** It is not installed, and can be. */
		AVAILABLE,
		/** It is installed, and is the one of the catalog (or the catalog cannot tell). */
		INSTALLED,
		/** It is installed, but the catalog has a newer file. */
		OUTDATED,
		/** It cannot be installed: the file is not published, has no SHA-256, or the target folder is unknown. */
		UNAVAILABLE
	}


	/**
	 * A component with what is known about it.
	 *
	 * @param component	The component.
	 * @param asset	The file to download, with its SHA-256; {@code null} if it is not published.
	 * @param directory	The folder it goes in; {@code null} if the application cannot tell where it is.
	 * @param state	What can be done.
	 */
	public record Entry(Component component, ReleaseAsset asset, Path directory, State state){

		/** @return	Whether {@link ComponentManager#install} can be called. */
		public boolean canInstall(){
			return (state == State.AVAILABLE || state == State.OUTDATED);
		}

		/** @return	Whether {@link ComponentManager#uninstall} can be called: it is installed and is a single file (not an unpacked zip). */
		public boolean canUninstall(){
			return ((state == State.INSTALLED || state == State.OUTDATED) && directory != null && !removableFiles(component).isEmpty());
		}

	}


	private final GitHubReleases releases;
	private final Transport transport;
	private final Map<String, Path> directories;
	private final URI catalogUrl;


	/**
	 * @param releases	The releases of the application.
	 * @param transport	How to read from the network.
	 * @param directories	The folder of each target ({@link ComponentCatalog#TARGETS}); a target that is missing, or whose value is {@code null},
	 * 	cannot be installed.
	 * @param catalogUrl	Where the catalog is.
	 */
	public ComponentManager(final GitHubReleases releases, final Transport transport, final Map<String, Path> directories, final URI catalogUrl){
		this.releases = releases;
		this.transport = transport;
		this.directories = new HashMap<>(directories);
		this.catalogUrl = catalogUrl;
	}

	/** @return	The manager of the application: its catalog and releases, from the network. */
	public static ComponentManager application(final Map<String, Path> directories){
		return new ComponentManager(GitHubReleases.application(), Transport.http(), directories, ComponentCatalog.DEFAULT_URL);
	}

	/**
	 * @param applicationVersion	The version of the application: the components that need a newer one are left out; may be {@code null}.
	 * @return	The components of the catalog.
	 * @throws IOException	If the catalog cannot be read.
	 */
	public List<Entry> list(final String applicationVersion) throws IOException{
		final ComponentCatalog catalog = ComponentCatalog.read(transport, catalogUrl);
		final Optional<Version> current = (applicationVersion != null? Version.parse(applicationVersion): Optional.empty());

		//(one request for each release, not for each component)
		final Map<String, Optional<GitHubReleases.Assets>> assetsByRelease = new HashMap<>();
		final Map<Path, Properties> registries = new HashMap<>();
		final List<Entry> entries = new ArrayList<>();
		for(final Component component : catalog.components()){
			if(!isUsable(component, current))
				continue;

			final Path directory = directories.get(component.target());
			final ReleaseAsset asset = resolve(component, assetsByRelease);
			entries.add(new Entry(component, asset, directory, stateOf(component, asset, directory, registries)));
		}
		return entries;
	}

	private static boolean isUsable(final Component component, final Optional<Version> current){
		if(component.minVersion() == null || current.isEmpty())
			return true;

		final Optional<Version> minimum = Version.parse(component.minVersion());
		return (minimum.isEmpty() || current.get().compareTo(minimum.get()) >= 0);
	}

	private ReleaseAsset resolve(final Component component, final Map<String, Optional<GitHubReleases.Assets>> assetsByRelease){
		if(component.url() != null)
			return new ReleaseAsset(ComponentCatalog.fileName(component.url()), component.url(), component.size(), component.sha256());

		final Optional<GitHubReleases.Assets> assets = assetsByRelease.computeIfAbsent(component.release(), release -> {
			try{
				return (Component.LATEST.equals(release)? releases.latestAssets(): releases.assetsOf(release));
			}
			catch(final IOException ignored){
				//GitHub cannot be reached for this release: its components are not available now
				return Optional.empty();
			}
		});
		return assets.flatMap(a -> a.find(component.asset()))
			.orElse(null);
	}

	private State stateOf(final Component component, final ReleaseAsset asset, final Path directory, final Map<Path, Properties> registries){
		if(directory == null)
			return State.UNAVAILABLE;

		final Properties registry = registries.computeIfAbsent(registryFile(directory), ComponentManager::readRegistry);
		final String installedSha = registry.getProperty(key(component));
		boolean filesPresent = !component.files().isEmpty();
		for(final String file : component.files())
			filesPresent &= Files.isRegularFile(directory.resolve(file));

		final boolean installed = (installedSha != null? (component.files().isEmpty() || filesPresent): filesPresent);
		if(!installed)
			return (asset != null && asset.isDownloadable()? State.AVAILABLE: State.UNAVAILABLE);
		if(installedSha != null && asset != null && asset.sha256() != null && !installedSha.equalsIgnoreCase(asset.sha256()))
			return State.OUTDATED;
		return State.INSTALLED;
	}

	/**
	 * Downloads the component and installs it.
	 *
	 * @param entry	A component that {@link Entry#canInstall() can be installed}.
	 * @param progress	Told how far the download is (called from this thread); may be {@code null}.
	 * @param cancelled	Tells whether to give up; may be {@code null}.
	 * @throws InstallerDownloader.DownloadCancelledException	If {@code cancelled} says so.
	 * @throws IOException	If the download fails, the file is not the published one, or it cannot be written.
	 */
	public void install(final Entry entry, final InstallerDownloader.Progress progress, final BooleanSupplier cancelled) throws IOException{
		if(!entry.canInstall() || entry.asset() == null || entry.directory() == null)
			throw new IOException("The component " + entry.component().id() + " cannot be installed");

		final Component component = entry.component();
		final ReleaseAsset asset = entry.asset();
		final Path directory = entry.directory();
		final Set<String> accepted = ACCEPTED.get(component.target());

		final Path temporary = Files.createTempDirectory("familylegacy-component");
		try{
			final Path file = new InstallerDownloader(transport).download(asset, temporary, progress, cancelled);
			if(asset.name().toLowerCase(Locale.ROOT).endsWith(".zip"))
				unpack(file, directory, accepted, asset.name());
			else
				copy(file, directory, accepted);
			remember(directory, key(component), asset.sha256());
		}
		finally{
			deleteTree(temporary);
		}
	}

	/**
	 * Removes the file of an installed component and forgets it.
	 *
	 * @param entry	A component that {@link Entry#canUninstall() can be uninstalled}.
	 * @throws IOException	If the file cannot be deleted (on Windows, for instance, a plugin that is loaded cannot: the application must be closed).
	 */
	public void uninstall(final Entry entry) throws IOException{
		if(!entry.canUninstall())
			throw new IOException("The component " + entry.component().id() + " cannot be uninstalled");

		final Path directory = entry.directory();
		for(final String name : removableFiles(entry.component())){
			final Path file = directory.resolve(name).normalize();
			//(the names are plain, but the check costs nothing)
			if(!directory.normalize().equals(file.getParent()))
				throw new IOException("Unexpected file: " + name);

			Files.deleteIfExists(file);
		}
		forget(directory, key(entry.component()));
	}

	/** @return	The file the component put in its folder, if it is a single one (empty for a zip, that is unpacked into many). */
	private static List<String> removableFiles(final Component component){
		final String name = (component.asset() != null? component.asset(): (component.url() != null? ComponentCatalog.fileName(component.url()): null));
		if(name == null || name.toLowerCase(Locale.ROOT).endsWith(".zip"))
			return List.of();

		return List.of(name);
	}

	private static void copy(final Path file, final Path directory, final Set<String> accepted) throws IOException{
		final String name = file.getFileName().toString();
		if(!isAccepted(name, accepted))
			throw new IOException("Unexpected file: " + name);

		Files.createDirectories(directory);
		final Path staging = Files.createTempFile(directory, ".staging-", ".part");
		try{
			Files.copy(file, staging, StandardCopyOption.REPLACE_EXISTING);
			Files.move(staging, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
		}
		finally{
			Files.deleteIfExists(staging);
		}
	}

	/** Unpacks the file: only plain files of the accepted kinds, nothing outside the folder; all of them are written, or none. */
	static void unpack(final Path zip, final Path directory, final Set<String> accepted, final String label) throws IOException{
		Files.createDirectories(directory);
		final Path staging = Files.createTempDirectory(directory, ".staging-");
		try{
			final List<Path> files = new ArrayList<>();
			long total = 0;
			try(final InputStream in = Files.newInputStream(zip);
					final ZipInputStream zis = new ZipInputStream(in)){
				ZipEntry entry;
				while((entry = zis.getNextEntry()) != null){
					if(entry.isDirectory())
						continue;

					final String name = entry.getName();
					//(the names come from the network: they must not lead outside the folder)
					if(!isAccepted(name, accepted) || files.size() >= MAX_FILES)
						throw new IOException("Unexpected file in " + label + ": " + name);

					final Path file = staging.resolve(name);
					try(final OutputStream out = Files.newOutputStream(file)){
						total += copyLimited(zis, out, MAX_UNPACKED_BYTES - total, label);
					}
					files.add(file);
				}
			}
			if(files.isEmpty())
				throw new IOException(label + " is empty");

			for(final Path file : files)
				Files.move(file, directory.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
		}
		finally{
			deleteTree(staging);
		}
	}

	private static boolean isAccepted(final String name, final Set<String> accepted){
		if(!ComponentCatalog.isPlainFileName(name))
			return false;

		final int dot = name.lastIndexOf('.');
		return (dot > 0 && accepted.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT)));
	}

	private static long copyLimited(final InputStream in, final OutputStream out, final long limit, final String label) throws IOException{
		final byte[] buffer = new byte[64 * 1024];
		long copied = 0;
		int read;
		while((read = in.read(buffer)) >= 0){
			copied += read;
			if(copied > limit)
				throw new IOException(label + " is too big once unpacked");

			out.write(buffer, 0, read);
		}
		return copied;
	}


	private static String key(final Component component){
		return component.target() + "." + component.id();
	}

	private static Path registryFile(final Path directory){
		final Path parent = directory.toAbsolutePath().getParent();
		return (parent != null? parent: directory).resolve(REGISTRY);
	}

	private static Properties readRegistry(final Path file){
		final Properties registry = new Properties();
		if(Files.isRegularFile(file))
			try(final InputStream in = Files.newInputStream(file)){
				registry.load(in);
			}
			catch(final IOException | IllegalArgumentException ignored){
				//a registry that cannot be read is as if empty: the components are told as not installed
			}
		return registry;
	}

	private static void remember(final Path directory, final String key, final String sha256) throws IOException{
		update(directory, registry -> registry.setProperty(key, sha256));
	}

	private static void forget(final Path directory, final String key) throws IOException{
		update(directory, registry -> registry.remove(key));
	}

	private static synchronized void update(final Path directory, final Consumer<Properties> change) throws IOException{
		final Path file = registryFile(directory);
		final Properties registry = readRegistry(file);
		change.accept(registry);

		Files.createDirectories(file.getParent());
		final Path staging = Files.createTempFile(file.getParent(), ".registry-", ".part");
		try{
			try(final OutputStream out = Files.newOutputStream(staging)){
				registry.store(out, "The optional components installed by the application (target.id = SHA-256)");
			}
			Files.move(staging, file, StandardCopyOption.REPLACE_EXISTING);
		}
		finally{
			Files.deleteIfExists(staging);
		}
	}

	private static void deleteTree(final Path directory){
		try(final Stream<Path> walk = Files.walk(directory)){
			walk.sorted(Comparator.reverseOrder())
				.forEach(path -> {
					try{
						Files.deleteIfExists(path);
					}
					catch(final IOException ignored){
						//a leftover in the temporary folder is not worth an error
					}
				});
		}
		catch(final IOException ignored){
			//the same
		}
	}

}
