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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * The list of the optional components that can be downloaded: a JSON file kept in the repository of the application, so that a component is
 * added by adding its file to a release and a line to the catalog, without touching the application.
 * <pre>
 * {
 *   "components": [
 *     {
 *       "id": "fonts-cjk",
 *       "name": {"en": "Chinese, Japanese, Korean fonts", "it": "Caratteri cinesi, giapponesi, coreani"},
 *       "description": {"en": "For the PDF reports"},
 *       "target": "fonts",
 *       "asset": "fonts-cjk.zip",
 *       "release": "components",
 *       "files": ["NotoSansSC-Regular.otf"],
 *       "minVersion": "1.2.0"
 *     },
 *     {
 *       "id": "importer-foo",
 *       "name": "Foo importer",
 *       "target": "plugins",
 *       "url": "https://example.org/importer-foo.jar",
 *       "sha256": "..."
 *     }
 *   ]
 * }
 * </pre>
 * {@code name} and {@code description} are a text, or an object by language tag. The file is an {@code asset} of a {@code release} (a tag, or
 * {@code latest}, the default), whose SHA-256 is the one GitHub publishes, or a {@code url} (HTTPS) with its {@code sha256}. A file ending in
 * {@code .zip} is unpacked (only plain files of the kinds the target accepts), any other is copied as it is. {@code target} is {@code fonts}
 * (the fonts of the PDF reports) or {@code plugins}. {@code files} are optional: when they are all in the target folder the component counts
 * as installed even if the application did not install it. {@code requires} (optional) lists the ids, of the same target, of the components
 * that this one needs: they are installed with it, and removed after it. A line that is not understood is skipped, not an error.
 */
public final class ComponentCatalog{

	/** Where the catalog is kept: a file of the repository of the application. */
	public static final URI DEFAULT_URL = URI.create("https://raw.githubusercontent.com/" + GitHubReleases.REPOSITORY + "/main/components/catalog.json");

	/** Where a component can be installed: the fonts of the PDF reports and the plugins. */
	public static final Set<String> TARGETS = Set.of("fonts", "plugins");

	private static final int MAX_BYTES = 1024 * 1024;
	private static final int MAX_COMPONENTS = 500;
	private static final int SHA256_HEX_LENGTH = 64;


	private final List<Component> components;


	private ComponentCatalog(final List<Component> components){
		this.components = List.copyOf(components);
	}

	/** @return	The components, in the order of the catalog. */
	public List<Component> components(){
		return components;
	}

	/**
	 * What the application offers when the repository has no catalog (yet): the fonts of the PDF reports, as the file {@code fonts.zip} of
	 * the latest release.
	 */
	public static ComponentCatalog fallback(){
		return new ComponentCatalog(List.of(new Component("fonts", Map.of("en", "Fonts for the PDF reports"),
			Map.of("en", "Noto fonts for Chinese, Japanese, Korean, Arabic, Hebrew, Thai, Hindi and Bengali"), "fonts", Component.LATEST,
			"fonts.zip", null, null, -1, null, List.of("NotoSans-Regular.ttf"), List.of())));
	}

	/**
	 * Reads the catalog; if there is none at the address the {@link #fallback() fallback} is returned.
	 *
	 * @param transport	How to read from the network.
	 * @param uri	The address of the catalog.
	 * @throws IOException	If the catalog cannot be read or is not valid JSON.
	 */
	public static ComponentCatalog read(final Transport transport, final URI uri) throws IOException{
		final String text;
		try(final InputStream in = transport.open(uri)){
			final byte[] bytes = in.readNBytes(MAX_BYTES + 1);
			if(bytes.length > MAX_BYTES)
				throw new IOException("The catalog is too big");
			text = new String(bytes, StandardCharsets.UTF_8);
		}
		catch(final Transport.HttpStatusException e){
			if(e.status() == 404)
				return fallback();
			throw e;
		}
		return parse(text);
	}

	/**
	 * @param json	The text of the catalog.
	 * @throws IOException	If it is not valid JSON.
	 */
	public static ComponentCatalog parse(final String json) throws IOException{
		final Object root;
		try{
			root = Json.parse(json);
		}
		catch(final IllegalArgumentException e){
			throw new IOException("Unreadable catalog: " + e.getMessage(), e);
		}

		final List<Component> components = new ArrayList<>();
		final Set<String> keys = new LinkedHashSet<>();
		for(final Object entry : Json.asArray(Json.asObject(root).get("components"))){
			if(components.size() >= MAX_COMPONENTS)
				break;

			final Component component = component(Json.asObject(entry));
			if(component != null && keys.add(component.target() + "/" + component.id()))
				components.add(component);
		}
		return new ComponentCatalog(components);
	}

	private static Component component(final Map<String, Object> entry){
		final String id = Json.asString(entry.get("id"));
		final String target = Json.asString(entry.get("target"));
		final Map<String, String> names = localized(entry.get("name"));
		if(!isPlainId(id) || target == null || !TARGETS.contains(target) || names.isEmpty())
			return null;

		final String asset = Json.asString(entry.get("asset"));
		final String urlText = Json.asString(entry.get("url"));
		String release = null;
		URI url = null;
		String sha256 = null;
		if(asset != null){
			if(!isPlainFileName(asset))
				return null;

			release = Json.asString(entry.get("release"));
			if(release == null)
				release = Component.LATEST;
		}
		else if(urlText != null){
			url = httpsUri(urlText);
			sha256 = normalizeSha256(Json.asString(entry.get("sha256")));
			if(url == null || sha256 == null || !isPlainFileName(fileName(url)))
				return null;
		}
		else
			return null;

		final List<String> files = new ArrayList<>();
		for(final Object file : Json.asArray(entry.get("files"))){
			if(!(file instanceof final String name) || !isPlainFileName(name))
				return null;

			files.add(name);
		}
		final List<String> requires = new ArrayList<>();
		for(final Object required : Json.asArray(entry.get("requires"))){
			if(!(required instanceof final String requiredId) || !isPlainId(requiredId))
				return null;

			requires.add(requiredId);
		}
		final long size = (entry.get("size") instanceof final Double value && value >= 0? value.longValue(): -1);
		return new Component(id, names, localized(entry.get("description")), target, release, asset, url, sha256, size,
			Json.asString(entry.get("minVersion")), files, requires);
	}

	/** @return	The text by language tag: a plain text is the English one. */
	private static Map<String, String> localized(final Object value){
		final Map<String, String> texts = new LinkedHashMap<>();
		if(value instanceof final String text){
			if(!text.isBlank())
				texts.put("en", text.strip());
		}
		else
			for(final Map.Entry<String, Object> entry : Json.asObject(value).entrySet())
				if(entry.getValue() instanceof final String text && !text.isBlank())
					texts.put(entry.getKey().toLowerCase(Locale.ROOT), text.strip());
		return texts;
	}

	static String fileName(final URI url){
		final String path = url.getPath();
		if(path == null)
			return null;

		return path.substring(path.lastIndexOf('/') + 1);
	}

	static boolean isPlainId(final String id){
		if(id == null || id.isEmpty() || id.length() > 64)
			return false;

		for(int i = 0; i < id.length(); i ++){
			final char c = id.charAt(i);
			if(!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9') && c != '.' && c != '_' && c != '-')
				return false;
		}
		return true;
	}

	/** A name that cannot lead out of the folder it is written in. */
	static boolean isPlainFileName(final String name){
		return (name != null && !name.isEmpty() && name.length() <= 200 && name.indexOf('/') < 0 && name.indexOf('\\') < 0
			&& name.indexOf(':') < 0 && !name.startsWith(".") && name.indexOf('\0') < 0);
	}

	private static URI httpsUri(final String text){
		try{
			final URI uri = URI.create(text);
			return ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null? uri: null);
		}
		catch(final IllegalArgumentException e){
			return null;
		}
	}

	private static String normalizeSha256(final String hex){
		if(hex == null)
			return null;

		final String lower = hex.strip().toLowerCase(Locale.ROOT);
		if(lower.length() != SHA256_HEX_LENGTH)
			return null;
		for(int i = 0; i < lower.length(); i ++)
			if(Character.digit(lower.charAt(i), 16) < 0)
				return null;
		return lower;
	}

}
