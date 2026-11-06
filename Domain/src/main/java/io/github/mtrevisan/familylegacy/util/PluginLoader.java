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
package io.github.mtrevisan.familylegacy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;


/**
 * Finds the plugins of the application: the jars {@code <prefix>*.jar} in the {@code plugins} directory (the report formats are
 * {@code report-*.jar}, the importers {@code importer-*.jar}), each family read by a class loader of its own whose parent is the one of the
 * application, so that the API and the libraries of the application are not repeated in a plugin, which brings only its own library.
 * <p>
 * A plugin is read as a class path (its {@code module-info} is not used) and found as a {@link java.util.ServiceLoader} provider: it
 * lists its providers in {@code META-INF/services}.
 * </p>
 */
public final class PluginLoader{

	private static final Logger LOGGER = LoggerFactory.getLogger(PluginLoader.class);


	private static final String PLUGINS_DIR = "plugins";
	/** System property that sets the location of the plugins' directory (the one the packaged application sets for the other plugins). */
	private static final String PLUGINS_DIR_PROPERTY = "calendars.plugins.dir";
	private static final String PLUGIN_SUFFIX = ".jar";


	private PluginLoader(){}


	/**
	 * @param prefix  how the jars of the family start (for instance {@code "report-"})
	 * @param name    the name of the class loader
	 * @param parent  the class loader of the application, that gives the API and the libraries
	 * @return	The class loader of the jars of the family, or {@code null} if there is none (no plugins directory, or no jar of the family).
	 */
	public static ClassLoader classLoader(final String prefix, final String name, final ClassLoader parent){
		final Path directory = resolvePluginsDirectory();
		if(directory == null)
			return null;

		final URL[] jars;
		try(final Stream<Path> files = Files.list(directory)){
			jars = files
				.filter(file -> {
					final String fileName = file.getFileName().toString();
					return (fileName.startsWith(prefix) && fileName.endsWith(PLUGIN_SUFFIX));
				})
				.sorted()
				.map(PluginLoader::toUrl)
				.toArray(URL[]::new);
		}
		catch(final IOException ioe){
			LOGGER.warn("Cannot read the plugins directory {}", directory, ioe);
			return null;
		}
		return (jars.length > 0? new URLClassLoader(name, jars, parent): null);
	}

	private static URL toUrl(final Path file){
		try{
			return file.toUri().toURL();
		}
		catch(final MalformedURLException mue){
			throw new IllegalStateException(mue);
		}
	}

	/** Looks where the other plugins are: the property of the packaged application, beside the jar, the development folders. */
	private static Path resolvePluginsDirectory(){
		final String explicit = System.getProperty(PLUGINS_DIR_PROPERTY);
		if(explicit != null){
			final Path path = Paths.get(explicit);
			if(Files.isDirectory(path))
				return path;
		}

		try{
			final Path location = Paths.get(PluginLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI())
				.getParent();
			if(location != null && Files.isDirectory(location.resolve(PLUGINS_DIR)))
				return location.resolve(PLUGINS_DIR);
		}
		catch(final URISyntaxException | RuntimeException ignored){
			//fall back to the development folders
		}

		return Stream.of(Paths.get(PLUGINS_DIR), Paths.get("Core", PLUGINS_DIR))
			.filter(Files::isDirectory)
			.findFirst()
			.orElse(null);
	}

}
