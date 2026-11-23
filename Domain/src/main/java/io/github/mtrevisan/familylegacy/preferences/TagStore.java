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
package io.github.mtrevisan.familylegacy.preferences;

import io.github.mtrevisan.familylegacy.io.model.RecordTags;
import io.github.mtrevisan.familylegacy.util.AtomicFileWriter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * Where the free tags of the records are kept (see {@link RecordTags}): the tags are not in the FLEF file, so the application keeps them
 * for each file apart, as it does for the bookmarks, in {@code ~/.familylegacy/tags.properties}.
 * <p>
 * An entry is {@code <path of the file>|<record id>=<tag>\n<tag>...}; the file is human-readable and survives manual edits. The entries of
 * the other files are left as they are when one file is saved.
 * <p>
 * Not thread-safe: use it from the thread that edits the file (the event dispatch thread).
 */
public final class TagStore{

	private static final Logger LOGGER = LoggerFactory.getLogger(TagStore.class);

	/** The system property that gives another file for the tags (the tests give their own: the tags of the user are left alone). */
	public static final String FILE_PROPERTY_NAME = "familylegacy.tags.file";

	private static final char ID_SEPARATOR = '|';
	private static final String TAG_SEPARATOR = "\n";


	private final Path file;


	public TagStore(){
		this(defaultFile());
	}

	public TagStore(final Path file){
		if(file == null)
			throw new IllegalArgumentException("File must not be null");

		this.file = file;
	}

	private static Path defaultFile(){
		final String custom = System.getProperty(FILE_PROPERTY_NAME);
		return (custom != null && !custom.isBlank()
			? Path.of(custom)
			: Path.of(System.getProperty("user.home"), ".familylegacy", "tags.properties"));
	}


	/**
	 * @param documentPath	The path of the FLEF file (in its normal form), {@code null} for a document that has none.
	 * @return	The tags of the records of that file, by record id.
	 */
	public Map<String, List<String>> load(final String documentPath){
		final Map<String, List<String>> tags = new LinkedHashMap<>();
		if(documentPath == null)
			return tags;

		final Properties props = read();
		final String prefix = documentPath + ID_SEPARATOR;
		for(final String key : props.stringPropertyNames())
			if(key.startsWith(prefix)){
				final List<String> list = new ArrayList<>();
				for(final String tag : StringUtils.split(props.getProperty(key), TAG_SEPARATOR))
					if(!RecordTags.normalize(tag).isEmpty())
						list.add(tag);
				if(!list.isEmpty())
					tags.put(key.substring(prefix.length()), list);
			}
		return tags;
	}

	/**
	 * Writes the tags of a file, replacing the ones the store had for it.
	 *
	 * @param documentPath	The path of the FLEF file (in its normal form); nothing is written for a document that has none.
	 */
	public void save(final String documentPath, final RecordTags tags){
		if(documentPath == null)
			return;

		final Properties props = read();
		final String prefix = documentPath + ID_SEPARATOR;
		props.stringPropertyNames()
			.stream()
			.filter(key -> key.startsWith(prefix))
			.toList()
			.forEach(props::remove);
		for(final Map.Entry<String, List<String>> entry : tags.snapshot().entrySet())
			props.setProperty(prefix + entry.getKey(), String.join(TAG_SEPARATOR, new ArrayList<>(entry.getValue())));

		try{
			final Path parent = file.toAbsolutePath()
				.getParent();
			if(parent != null)
				Files.createDirectories(parent);
			//written through a temporary file, so that a crash does not leave half of the tags
			final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			props.store(bytes, "FamilyLegacy tags");
			AtomicFileWriter.write(file, bytes.toString(StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1);
		}
		catch(final IOException e){
			LOGGER.warn("Could not save the tags to {}: {}", file, e.getMessage());
		}
	}

	private Properties read(){
		final Properties props = new Properties();
		if(Files.exists(file))
			try(final InputStream is = Files.newInputStream(file)){
				props.load(is);
			}
			catch(final IOException e){
				LOGGER.warn("Could not load the tags from {}: {}", file, e.getMessage());
			}
		return props;
	}

}
