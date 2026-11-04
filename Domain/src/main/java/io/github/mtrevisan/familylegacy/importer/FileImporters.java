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
package io.github.mtrevisan.familylegacy.importer;

import io.github.mtrevisan.familylegacy.util.PluginLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;


/**
 * The importers that read a file format, found as {@link ServiceLoader} providers of {@link FileImporter}.
 * <p>
 * They are all plugins, as the heavy report formats and the astronomical engine are: a jar {@code importer-<id>.jar} in the
 * {@code plugins} directory, loaded by a class loader of its own (all the importers by the same one, because the importer of Gramps
 * uses the helpers of the one of GEDCOM), so that a format is available only if its jar is there.
 * </p>
 */
public final class FileImporters{

	private static final Logger LOGGER = LoggerFactory.getLogger(FileImporters.class);

	/** The formats the application knows, in the order of the menu: the ones without an importer are offered as not available. */
	public static final List<String> KNOWN_IDS = List.of("gedcom", "gramps");

	private static final String PLUGIN_PREFIX = "importer-";
	private static final String PLUGIN_SUFFIX = ".jar";


	private FileImporters(){}


	/** @return	The available importers, in the order of the menu (the ones of the plugins that are not known come last). */
	public static List<FileImporter> available(){
		final Map<String, FileImporter> importers = new LinkedHashMap<>();
		collect(ServiceLoader.load(FileImporter.class), importers);
		for(final FileImporter importer : PluginHolder.IMPORTERS)
			importers.putIfAbsent(importer.id(), importer);

		final List<FileImporter> result = new ArrayList<>(importers.values());
		result.sort(Comparator.comparingInt((FileImporter importer) -> {
				final int index = KNOWN_IDS.indexOf(importer.id());
				return (index >= 0? index: KNOWN_IDS.size());
			})
			.thenComparing(FileImporter::id));
		return result;
	}

	/** @return	The name of the jar of the plugin that provides the format. */
	public static String pluginFileName(final String id){
		return PLUGIN_PREFIX + id + PLUGIN_SUFFIX;
	}

	private static void collect(final ServiceLoader<FileImporter> loader, final Map<String, FileImporter> importers){
		//a provider that cannot be created (a plugin that lacks a library, a different version of the API) is skipped, not fatal
		loader.stream()
			.forEach(provider -> {
				try{
					final FileImporter importer = provider.get();
					importers.putIfAbsent(importer.id(), importer);
				}
				catch(final Exception | LinkageError e){
					LOGGER.warn("Cannot load the importer {}", provider.type().getName(), e);
				}
			});
	}


	/** The plugins are read once: they are in a directory that does not change while the application runs. */
	private static final class PluginHolder{

		private static final List<FileImporter> IMPORTERS = loadPlugins();


		private PluginHolder(){}


		private static List<FileImporter> loadPlugins(){
			//the model and the libraries of the application come from the parent: a plugin brings only its own code
			final ClassLoader loader = PluginLoader.classLoader(PLUGIN_PREFIX, "importer-plugins", FileImporters.class.getClassLoader());
			if(loader == null)
				return List.of();

			final Map<String, FileImporter> importers = new LinkedHashMap<>();
			collect(ServiceLoader.load(FileImporter.class, loader), importers);
			return List.copyOf(importers.values());
		}

	}

}
