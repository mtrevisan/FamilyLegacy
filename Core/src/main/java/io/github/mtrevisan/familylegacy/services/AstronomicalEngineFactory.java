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


public final class AstronomicalEngineFactory{

	private static final String PLUGINS_DIR = "plugins";

	private static final AstronomicalEngine ENGINE;
	static{
		ENGINE = loadEngine();
	}

	private AstronomicalEngineFactory(){}


	public static AstronomicalEngine getEngine(){
		return ENGINE;
	}


	private static AstronomicalEngine loadEngine(){
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
			final ModuleLayer parentLayer = AstronomicalEngineFactory.class.getModule().getLayer() != null
				? AstronomicalEngineFactory.class.getModule().getLayer()
				: ModuleLayer.boot();

			// Resolve child module configuration
			final Configuration configuration = parentLayer.configuration()
				.resolve(finder, ModuleFinder.of(), pluginModules);

			// Define child layer using Core class loader context
			final ClassLoader coreClassLoader = AstronomicalEngineFactory.class.getClassLoader();
			final ModuleLayer pluginLayer = parentLayer.defineModulesWithOneLoader(configuration, coreClassLoader);

			// Discover SPI provider implementations inside the dynamic layer
			final ServiceLoader<AstronomicalEngine> loader = ServiceLoader.load(pluginLayer, AstronomicalEngine.class);
			for(final ServiceLoader.Provider<AstronomicalEngine> provider : loader.stream().toList()){
				final AstronomicalEngine engine = provider.get();
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
			final Path jarLocation = Paths.get(AstronomicalEngineFactory.class.getProtectionDomain()
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

	private static AstronomicalEngine loadFromClasspath(){
		final ServiceLoader<AstronomicalEngine> loader = ServiceLoader.load(AstronomicalEngine.class,
			AstronomicalEngineFactory.class.getClassLoader());
		for(final AstronomicalEngine engine : loader)
			if(engine.isAvailable())
				return engine;

		return new FallbackAstronomicalEngine();
	}

}
