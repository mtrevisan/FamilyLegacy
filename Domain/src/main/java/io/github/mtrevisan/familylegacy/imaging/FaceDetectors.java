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
package io.github.mtrevisan.familylegacy.imaging;

import io.github.mtrevisan.familylegacy.util.PluginLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.ServiceLoader;


/**
 * The face detector of the application, found as a {@link ServiceLoader} provider of {@link FaceDetector}: a plugin, the jar
 * {@code face-detector.jar} in the {@code plugins} directory (it is downloaded apart, as the fonts are), loaded by a class loader of its
 * own. Without it there is no detector, and nothing else changes.
 */
public final class FaceDetectors{

	private static final Logger LOGGER = LoggerFactory.getLogger(FaceDetectors.class);


	/** How the jars of the detectors start. */
	public static final String PLUGIN_PREFIX = "face-detector";


	private FaceDetectors(){}


	/** @return	The detector, or empty if no plugin provides one. */
	public static Optional<FaceDetector> available(){
		return Optional.ofNullable(Holder.DETECTOR);
	}


	/** Loads the plugin and prepares the detector, if any (slow: call it away from the event dispatch thread, e.g. during the splash). */
	public static void warmUp(){
		final FaceDetector detector = Holder.DETECTOR;
		if(detector != null){
			try{
				detector.warmUp();
			}
			catch(final Exception | LinkageError e){
				LOGGER.warn("Cannot warm up the face detector", e);
			}
		}
	}

	private static FaceDetector load(){
		FaceDetector detector = first(ServiceLoader.load(FaceDetector.class));
		if(detector == null){
			//the model and the libraries of the application come from the parent: a plugin brings only its own code
			final ClassLoader loader = PluginLoader.classLoader(PLUGIN_PREFIX, "facedetector-plugins",
				FaceDetectors.class.getClassLoader());
			if(loader != null)
				detector = first(ServiceLoader.load(FaceDetector.class, loader));
		}
		return detector;
	}

	private static FaceDetector first(final ServiceLoader<FaceDetector> loader){
		//a provider that cannot be created (a plugin that lacks a library, a different version of the API) is skipped, not fatal
		return loader.stream()
			.map(provider -> {
				try{
					return provider.get();
				}
				catch(final Exception | LinkageError e){
					LOGGER.warn("Cannot load the face detector {}", provider.type().getName(), e);
					return null;
				}
			})
			.filter(java.util.Objects::nonNull)
			.findFirst()
			.orElse(null);
	}


	/** The plugin is read once: it is in a directory that does not change while the application runs. */
	private static final class Holder{

		private static final FaceDetector DETECTOR = load();


		private Holder(){}

	}

}
