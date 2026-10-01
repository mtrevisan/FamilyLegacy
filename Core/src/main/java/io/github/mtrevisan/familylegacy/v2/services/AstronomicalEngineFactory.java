package io.github.mtrevisan.familylegacy.v2.services;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;


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
		final File dir = new File(PLUGINS_DIR);
		if(!dir.exists() || !dir.isDirectory())
			return loadFromClasspath();

		final File[] jarFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
		if(jarFiles == null || jarFiles.length == 0)
			return loadFromClasspath();

		final List<URL> urls = new ArrayList<>();
		for(final File file : jarFiles){
			try{
				urls.add(file.toURI().toURL());
			}
			catch(final MalformedURLException ignored){
			}
		}

		if(urls.isEmpty())
			return loadFromClasspath();

		final ClassLoader pluginClassLoader = new URLClassLoader(urls.toArray(new URL[0]),
			AstronomicalEngineFactory.class.getClassLoader());

		final ServiceLoader<AstronomicalEngine> loader = ServiceLoader.load(AstronomicalEngine.class, pluginClassLoader);
		for(final AstronomicalEngine engine : loader)
			if(engine.isAvailable())
				return engine;

		return loadFromClasspath();
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
