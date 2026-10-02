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
package io.github.mtrevisan.familylegacy.services;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class EphemerisEngineFactory{

	private static final String PLUGINS_DIR = "plugins";

	private static final EphemerisEngine ENGINE;
	static{
		ENGINE = loadEngine();
	}

	private EphemerisEngineFactory(){}


	public static EphemerisEngine getEngine(){
		return ENGINE;
	}


	private static EphemerisEngine loadEngine(){
		final Path pluginsPath = resolvePluginsDirectory();
		if(pluginsPath == null)
			return loadFromClasspath();

		try(final Stream<Path> stream = Files.list(pluginsPath)){
			final Path[] jarPaths = stream
				.filter(p -> p.toString().endsWith(".jar"))
				.filter(p -> !p.getFileName().toString().toLowerCase().contains("copy"))
				.toArray(Path[]::new);

			if(jarPaths.length == 0)
				return loadFromClasspath();

			// Locate JPMS modules within target plugin JARs
			final ModuleFinder finder = ModuleFinder.of(jarPaths);
			final Set<String> pluginModules = finder.findAll().stream()
				.map(reference -> reference.descriptor().name())
				.collect(Collectors.toSet());

			if(pluginModules.isEmpty())
				return loadFromClasspath();

			// Retrieve host module layer
			final ModuleLayer parentLayer = EphemerisEngineFactory.class.getModule().getLayer() != null
				? EphemerisEngineFactory.class.getModule().getLayer()
				: ModuleLayer.boot();

			// Resolve child module configuration
			final Configuration configuration = parentLayer.configuration()
				.resolve(finder, ModuleFinder.of(), pluginModules);

			// Define child layer using Core class loader context
			final ClassLoader coreClassLoader = EphemerisEngineFactory.class.getClassLoader();
			final ModuleLayer pluginLayer = parentLayer.defineModulesWithOneLoader(configuration, coreClassLoader);

			// Discover SPI provider implementations inside the dynamic layer
			final ServiceLoader<EphemerisEngine> loader = ServiceLoader.load(pluginLayer, EphemerisEngine.class);
			for(final ServiceLoader.Provider<EphemerisEngine> provider : loader.stream().toList()){
				final EphemerisEngine engine = provider.get();
				if(engine.isAvailable()){
					return engine;
				}
			}
		}
		catch(final Exception ignored){}

		return loadFromClasspath();
	}

	/**
	 * Resolves the plugins directory across IDE, development, and production environments.
	 *
	 * @return The resolved Path to the plugins directory, or null if not found.
	 */
	private static Path resolvePluginsDirectory(){
		// 1. Try resolving relative to application installation location at runtime
		try{
			final Path jarLocation = Paths.get(EphemerisEngineFactory.class.getProtectionDomain()
				.getCodeSource().getLocation().toURI()).getParent();
			if(jarLocation != null){
				final Path appPlugins = jarLocation.resolve(PLUGINS_DIR);
				if(Files.exists(appPlugins) && Files.isDirectory(appPlugins))
					return appPlugins;
			}
		}
		catch(final URISyntaxException ignored){
			// Fallback to relative paths if protection domain resolution fails
		}

		// 2. Fallback candidate locations for development and IDE execution
		final List<Path> candidatePaths = List.of(
			Paths.get(PLUGINS_DIR),
			Paths.get("Core", PLUGINS_DIR)
		);

		return candidatePaths.stream()
			.filter(p -> Files.exists(p) && Files.isDirectory(p))
			.findFirst()
			.orElse(null);
	}

	private static EphemerisEngine loadFromClasspath(){
		final ServiceLoader<EphemerisEngine> loader = ServiceLoader.load(EphemerisEngine.class,
			EphemerisEngineFactory.class.getClassLoader());
		for(final EphemerisEngine engine : loader)
			if(engine.isAvailable())
				return engine;

		return new FallbackEphemerisEngine();
	}

}
