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
package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * Registry for RecordTypeHandler instances.
 * Maps record type names (e.g., "INDIVIDUAL") to their handlers.
 */
public final class HandlerRegistry{

	private static final Map<Class<? extends RecordTypeHandler<?>>, RecordTypeHandler<?>> handlers = new ConcurrentHashMap<>();

	private static final Map<String, Class<? extends RecordTypeHandler<?>>> HANDLER_MAP = new ConcurrentHashMap<>();


	private HandlerRegistry(){}


	/**
	 * Returns the handler for the given record type.
	 *
	 * @param handlerClass	The handler class (e.g., IndividualHandler.class).
	 * @return	The handler, or {@code null} if not able to return a class.
	 */
	public static RecordTypeHandler<?> getHandler(final Class<? extends RecordTypeHandler<?>> handlerClass){
		return handlers.computeIfAbsent(handlerClass, k -> {
			try{
				return k.getDeclaredConstructor()
					.newInstance();
			}
			catch(final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored){}
			return null;
		});
	}

	/**
	 * Returns the handler for the given record type.
	 *
	 * @param handlerClassType	The record type (e.g., "INDIVIDUAL").
	 * @return	The handler, or {@code null} if not able to return a class.
	 */
	public static RecordTypeHandler<?> getHandler(final String handlerClassType){
		final Class<? extends RecordTypeHandler<?>> handlerClass = getHandlerClass(handlerClassType);
		return handlers.computeIfAbsent(handlerClass, k -> {
			try{
				return k.getDeclaredConstructor()
					.newInstance();
			}
			catch(final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored){}
			return null;
		});
	}

	/**
	 * Checks if a handler is registered for the given type.
	 */
	public static boolean isRegistered(final Class<? extends RecordTypeHandler<?>> handlerClass){
		return handlers.containsKey(handlerClass);
	}

	/**
	 * Returns all registered handlers.
	 */
	public static Map<Class<? extends RecordTypeHandler<?>>, RecordTypeHandler<?>> getHandlers(){
		return Collections.unmodifiableMap(handlers);
	}


	public static String getHandlerType(final Class<? extends RecordTypeHandler<?>> handlerClass){
		try{
			final Field field = handlerClass.getDeclaredField("TYPE");
			field.setAccessible(true);
			return (String)field.get(null);
		}
		catch(final NoSuchFieldException | IllegalAccessException e){
			throw new IllegalArgumentException(
				"Handler class " + handlerClass.getName() + " does not define a static TYPE field",
				e
			);
		}
	}


	/**
	 * Retrieves the handler class for the given type.
	 *
	 * @param type the handler type (e.g., "INDIVIDUAL", "SOURCE")
	 * @return the class that extends {@link RecordTypeHandler}, or {@code null} if not found
	 */
	public static Class<? extends RecordTypeHandler<?>> getHandlerClass(final String type){
		Class<? extends RecordTypeHandler<?>> handlerClass = HANDLER_MAP.get(type);
		if(handlerClass == null){
			scanHandlers(CauseHandler.class);

			handlerClass = HANDLER_MAP.get(type);
		}
		return handlerClass;
	}

	/**
	 * Scans the package containing the given class for all classes that extend
	 * {@link RecordTypeHandler}.
	 *
	 * @param sampleClass any class belonging to the package to scan
	 */
	private static void scanHandlers(final Class<?> sampleClass){
		final String packageName = sampleClass.getPackage().getName();
		scanHandlers(packageName);
	}

	/**
	 * Scans the specified package for all classes that extend {@link RecordTypeHandler}.
	 * For each found class, it reads the static {@code TYPE} field and stores the mapping.
	 *
	 * @param packageName the package to scan (e.g., "io.github.mtrevisan.familylegacy.v2.ui.handlers")
	 */
	private static void scanHandlers(final String packageName){
		final String packagePath = packageName.replace('.', '/');
		final ClassLoader classLoader = Thread.currentThread()
			.getContextClassLoader();
		try{
			final Enumeration<URL> resources = classLoader.getResources(packagePath);
			while(resources.hasMoreElements()){
				final URL resource = resources.nextElement();
				final String protocol = resource.getProtocol();
				if("file".equals(protocol)){
					// File-system directory
					final File directory = new File(URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8.name()));
					scanDirectory(packageName, directory);
				}
				else if("jar".equals(protocol)){
					// JAR entry
					final String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
					scanJar(jarPath, packageName);
				}
				// Other protocols (e.g., "war", "bundle") are not handled for simplicity
			}
		}
		catch(final IOException e){
			throw new RuntimeException("Failed to scan package: " + packageName, e);
		}
	}

	/**
	 * Recursively scans a directory for .class files.
	 *
	 * @param packageName the current package name (used to build full class names)
	 * @param directory   the directory to scan
	 */
	private static void scanDirectory(final String packageName, final File directory){
		if(!directory.exists() || !directory.isDirectory())
			return;

		final File[] files = directory.listFiles();
		if(files == null)
			return;

		for(final File file : files){
			if(file.isDirectory())
				// Recurse into subdirectories
				scanDirectory(packageName + "." + file.getName(), file);
			else if(file.getName().endsWith(".class")){
				final String className = packageName + "." + file.getName().replace(".class", "");
				processClass(className);
			}
		}
	}

	/**
	 * Scans a JAR file for classes in the specified package.
	 *
	 * @param jarPath     the path to the JAR file
	 * @param packageName the package name to scan (e.g., "io.github...handlers")
	 */
	private static void scanJar(final String jarPath, final String packageName){
		try(final JarFile jarFile = new JarFile(jarPath)){
			final Enumeration<JarEntry> entries = jarFile.entries();
			final String packagePath = packageName.replace('.', '/');
			while(entries.hasMoreElements()){
				final JarEntry entry = entries.nextElement();
				final String name = entry.getName();
				// Match class files directly under the package path (including subpackages)
				if(name.startsWith(packagePath) && name.endsWith(".class") && !name.contains("$")){
					// Convert path to fully-qualified class name
					final String className = name.replace('/', '.').replace(".class", "");
					processClass(className);
				}
			}
		}
		catch(final IOException e){
			// Log or ignore – JAR scanning is best-effort
			System.err.println("Could not scan JAR: " + jarPath + " - " + e.getMessage());
		}
	}

	/**
	 * Loads a class by name and, if it extends RecordTypeHandler, stores it in the map.
	 *
	 * @param className the fully-qualified class name
	 */
	private static void processClass(final String className){
		try{
			final Class<?> clazz = Class.forName(className);
			if(RecordTypeHandler.class.isAssignableFrom(clazz) && !clazz.isInterface()){
				@SuppressWarnings("unchecked")
				final Class<? extends RecordTypeHandler<?>> handlerClass =
					(Class<? extends RecordTypeHandler<?>>)clazz;
				final String type = getTypeConstant(handlerClass);
				if(type != null && !type.isEmpty())
					HANDLER_MAP.put(type, handlerClass);
			}
		}
		catch(final ClassNotFoundException ignored){
			// Skip classes that cannot be loaded
		}
	}

	/**
	 * Extracts the value of the static {@code TYPE} field from the given handler class.
	 *
	 * @param clazz the handler class
	 * @return the value of the TYPE field, or {@code null} if the field is missing or not a String
	 */
	private static String getTypeConstant(final Class<? extends RecordTypeHandler<?>> clazz){
		try{
			final Field field = clazz.getDeclaredField("TYPE");
			field.setAccessible(true);
			final Object value = field.get(null);
			if(value instanceof String)
				return (String)value;
		}
		catch(final NoSuchFieldException e){
			System.err.println("Handler class " + clazz.getName() + " does not define a static TYPE field.");
		}
		catch(final IllegalAccessException e){
			System.err.println("Cannot access TYPE field on " + clazz.getName());
		}
		return null;
	}

}
