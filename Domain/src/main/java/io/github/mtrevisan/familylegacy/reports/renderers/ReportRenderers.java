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
package io.github.mtrevisan.familylegacy.reports.renderers;

import io.github.mtrevisan.familylegacy.util.PluginLoader;
import io.github.mtrevisan.reports.ReportRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;


/**
 * The renderers that write a report in a file format, found as {@link ServiceLoader} providers of {@link ReportRenderer}.
 * <p>
 * The light formats (text, Markdown, HTML) are in the application; the ones that carry a heavy library (PDF, Word) are plugins, as the
 * astronomical engine is: a jar {@code report-<id>.jar} in the {@code plugins} directory, loaded by a class loader of its own, so that
 * a format is available only if its jar is there.
 */
public final class ReportRenderers{

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportRenderers.class);


	/** The format that is selected when nothing was chosen. */
	public static final String DEFAULT_ID = "markdown";
	/** The formats the application knows, in the order of the dialog: the ones without a renderer are offered as not available. */
	public static final List<String> KNOWN_IDS = List.of("text", "markdown", "html", "docx", "pdf", "epub", "site");

	private static final String PLUGIN_PREFIX = "report-";
	private static final String PLUGIN_SUFFIX = ".jar";


	private ReportRenderers(){}


	/** @return	The available renderers, in the order of the dialog (the ones of the application, then the ones of the plugins). */
	public static List<ReportRenderer> available(){
		final Map<String, ReportRenderer> renderers = new LinkedHashMap<>();
		collect(ServiceLoader.load(ReportRenderer.class), renderers);
		for(final ReportRenderer renderer : PluginHolder.RENDERERS)
			renderers.putIfAbsent(renderer.id(), renderer);

		final List<ReportRenderer> result = new ArrayList<>(renderers.values());
		result.sort(Comparator.comparingInt((ReportRenderer renderer) -> {
				final int index = KNOWN_IDS.indexOf(renderer.id());
				return (index >= 0? index: KNOWN_IDS.size());
			})
			.thenComparing(ReportRenderer::id));
		return result;
	}

	/** @return	The name of the jar of the plugin that provides the format. */
	public static String pluginFileName(final String id){
		return PLUGIN_PREFIX + id + PLUGIN_SUFFIX;
	}

	private static void collect(final ServiceLoader<ReportRenderer> loader, final Map<String, ReportRenderer> renderers){
		//a provider that cannot be created (a plugin that lacks a library, a different version of the API) is skipped, not fatal
		loader.stream()
			.forEach(provider -> {
				try{
					final ReportRenderer renderer = provider.get();
					renderers.putIfAbsent(renderer.id(), renderer);
				}
				catch(final Exception | LinkageError e){
					LOGGER.warn("Cannot load the report format {}", provider.type().getName(), e);
				}
			});
	}


	/** The plugins are read once: they are in a directory that does not change while the application runs. */
	private static final class PluginHolder{

		private static final List<ReportRenderer> RENDERERS = loadPlugins();


		private PluginHolder(){}


		private static List<ReportRenderer> loadPlugins(){
			//the API and the libraries of the application come from the parent: a plugin brings only its own library
			final ClassLoader loader = PluginLoader.classLoader(PLUGIN_PREFIX, "report-plugins", ReportRenderers.class.getClassLoader());
			if(loader == null)
				return List.of();

			final Map<String, ReportRenderer> renderers = new LinkedHashMap<>();
			collect(ServiceLoader.load(ReportRenderer.class, loader), renderers);
			return List.copyOf(renderers.values());
		}

	}

}
