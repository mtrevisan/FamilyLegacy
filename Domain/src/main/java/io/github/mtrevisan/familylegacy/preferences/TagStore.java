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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;


/**
 * Where the free tags of the records are kept (see {@link RecordTags}): the tags are not in the FLEF file, but next to it, in
 * {@code <file>.flef.meta/tags.properties}, so that they travel with the file when the folder is copied, archived or backed up.
 * <p>
 * An entry is {@code <record id>=<tag>\n<tag>...}; the file is human-readable and survives manual edits. The older versions kept the tags
 * of all the files in {@code ~/.familylegacy/tags.properties}: they are read from there the first time a file is opened.
 * <p>
 * Not thread-safe: use it from the thread that edits the file (the event dispatch thread).
 */
public final class TagStore{

	private static final Logger LOGGER = LoggerFactory.getLogger(TagStore.class);

	/** The system property that gives another file for the tags (the tests give their own: the tags of the user are left alone). */
	public static final String FILE_PROPERTY_NAME = "familylegacy.tags.file";

	private static final char ID_SEPARATOR = '|';
	private static final String TAG_SEPARATOR = "\n";


	/** The suffix of the folder, next to the FLEF file, that keeps the application data of that file. */
	public static final String META_SUFFIX = ".meta";
	private static final String TAGS_FILE_NAME = "tags.properties";


	/** The store of the older versions, in the user folder: only read, to bring its tags next to the files. */
	private final Path legacyFile;


	public TagStore(){
		this(defaultFile());
	}

	public TagStore(final Path legacyFile){
		if(legacyFile == null)
			throw new IllegalArgumentException("File must not be null");

		this.legacyFile = legacyFile;
	}

	private static Path defaultFile(){
		final String custom = System.getProperty(FILE_PROPERTY_NAME);
		return (custom != null && !custom.isBlank()
			? Path.of(custom)
			: Path.of(System.getProperty("user.home"), ".familylegacy", "tags.properties"));
	}


	/**
	 * @param documentPath	The path of the FLEF file, {@code null} for a document that has none.
	 * @return	The folder that keeps the application data of that file (next to it, named after it, so that it travels with it).
	 */
	public static Path metaDirectory(final String documentPath){
		final Path document = Path.of(documentPath)
			.toAbsolutePath();
		return document.resolveSibling(document.getFileName() + META_SUFFIX);
	}

	/**
	 * Copies the application data of a file to the folder of another one (a copy of the file, a backup): what is already there is
	 * overwritten, a file that has no data copies nothing.
	 */
	public static void copyMetaDirectory(final String fromDocumentPath, final String toDocumentPath){
		final Path from = metaDirectory(fromDocumentPath);
		final Path to = metaDirectory(toDocumentPath);
		if(from.equals(to) || !Files.isDirectory(from))
			return;

		try(final Stream<Path> files = Files.list(from)){
			Files.createDirectories(to);
			for(final Path source : files.filter(Files::isRegularFile).toList())
				Files.copy(source, to.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
		}
		catch(final IOException e){
			LOGGER.warn("Could not copy the application data of {} to {}: {}", from, to, e.getMessage());
		}
	}


	/**
	 * Reads the tags kept in the folder of the file; if it has none yet, the tags that the older versions kept in the user folder are
	 * taken from there (and written next to the file, so that this happens once).
	 *
	 * @param documentPath	The path of the FLEF file (in its normal form), {@code null} for a document that has none.
	 * @return	The tags of the records of that file, by record id.
	 */
	public Map<String, List<String>> load(final String documentPath){
		if(documentPath == null)
			return new LinkedHashMap<>();

		final Path tagsFile = tagsFile(documentPath);
		if(Files.exists(tagsFile))
			return parse(read(tagsFile), StringUtils.EMPTY);

		final String prefix = documentPath + ID_SEPARATOR;
		final Map<String, List<String>> legacy = parse(read(legacyFile), prefix);
		if(!legacy.isEmpty())
			write(tagsFile, legacy);
		return legacy;
	}

	/**
	 * Writes the tags of a file next to it, replacing the ones that were there.
	 *
	 * @param documentPath	The path of the FLEF file; nothing is written for a document that has none.
	 */
	public void save(final String documentPath, final RecordTags tags){
		if(documentPath == null)
			return;

		final Path tagsFile = tagsFile(documentPath);
		final Map<String, List<String>> snapshot = tags.snapshot();
		//no tags and no folder: nothing is created just to say so; but once the file exists it is kept (empty), so that the tags of the
		//older versions are not read again
		if(snapshot.isEmpty() && !Files.exists(tagsFile))
			return;

		write(tagsFile, snapshot);
	}

	private static Path tagsFile(final String documentPath){
		return metaDirectory(documentPath).resolve(TAGS_FILE_NAME);
	}

	private static Map<String, List<String>> parse(final Properties props, final String prefix){
		final Map<String, List<String>> tags = new LinkedHashMap<>();
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

	private static Properties toProperties(final Map<String, ? extends Collection<String>> tags){
		final Properties props = new Properties();
		for(final Map.Entry<String, ? extends Collection<String>> entry : tags.entrySet())
			props.setProperty(entry.getKey(), String.join(TAG_SEPARATOR, new ArrayList<>(entry.getValue())));
		return props;
	}

	/** Written through a temporary file, so that a crash does not leave half of the tags. */
	private static void write(final Path tagsFile, final Map<String, ? extends Collection<String>> tags){
		ProjectMeta.write(tagsFile, toProperties(tags), "tags");
	}

	/** @return	The content of the tags file for these tags (for the archives), {@code null} if there are none. */
	public static byte[] tagsContent(final RecordTags tags){
		final Map<String, List<String>> snapshot = tags.snapshot();
		return (snapshot.isEmpty()? null: ProjectMeta.toBytes(toProperties(snapshot), "tags"));
	}

	/** @return	The name of the tags file in the {@code .meta} folder. */
	public static String tagsFileName(){
		return TAGS_FILE_NAME;
	}

	private static Properties read(final Path file){
		final Properties props = new Properties();
		if(Files.exists(file))
			try(final InputStream is = Files.newInputStream(file)){
				props.load(is);
			}
			catch(final IOException | IllegalArgumentException e){
				LOGGER.warn("Could not load the tags from {}: {}", file, e.getMessage());
			}
		return props;
	}

}
