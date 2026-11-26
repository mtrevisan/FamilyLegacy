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
package io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.preferences.ProjectMeta;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.util.AtomicFileWriter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;


/**
 * Persistent store of bookmarks.
 * <p>
 * The bookmarks of a file are saved next to it, in {@code <file>.flef.meta/bookmarks.properties} (see {@link #bindDocument(String)}), so
 * that they travel with the file. The older versions kept the bookmarks of every file in
 * {@code ~/.familylegacy/bookmarks.properties}: they are read from there the first time a file is opened. Each bookmark is stored
 * under a {@code bookmark.<uuid>.} prefix, with the name, type, root id
 * and creation timestamp as dedicated keys, and one
 * {@code bookmark.<uuid>.prop.<key>} entry per type-specific property.
 * The format is human-readable and survives manual edits.
 * <p>
 * The store is not thread-safe; use it from the Swing Event Dispatch
 * Thread.
 */
public final class BookmarkStore{

	private static final Logger LOGGER = LoggerFactory.getLogger(BookmarkStore.class);


	/** The property that ties a bookmark to the file it was made in. */
	public static final String FILE_PROPERTY = "file";

	private static final String PREFIX = "bookmark.";
	/** The system property that gives another file for the bookmarks (the tests give their own: the bookmarks of the user are left alone). */
	public static final String FILE_PROPERTY_NAME = "familylegacy.bookmarks.file";


	private static final String META_FILE_NAME = "bookmarks.properties";

	/** The file of the older versions, in the user folder: it keeps the bookmarks of every file, and it is read to bring them next to their file. */
	private final Path file;
	/** The bookmarks of the user folder (the ones of every file), while no document is bound. */
	private final Map<String, Bookmark> global = new LinkedHashMap<>();
	/** The bookmarks of the bound document, kept next to it. */
	private final Map<String, Bookmark> bookmarks = new LinkedHashMap<>();
	/** The path of the FLEF file the bookmarks are of, {@code null} if none (a document never saved). */
	private String document;


	public BookmarkStore(){
		this(defaultFile());
	}

	private static Path defaultFile(){
		final String custom = System.getProperty(FILE_PROPERTY_NAME);
		return (custom != null && !custom.isBlank()
			? Path.of(custom)
			: Path.of(System.getProperty("user.home"), ".familylegacy", "bookmarks.properties"));
	}

	public BookmarkStore(final Path file){
		if(file == null)
			throw new IllegalArgumentException("File must not be null");
		this.file = file;
		load();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Makes the bookmarks those of a file, which are kept next to it ({@code <file>.meta/bookmarks.properties}) so that they travel with
	 * it. The first time, the ones that the older versions kept in the user folder for that file are taken from there.
	 *
	 * @param filePath	The path of the FLEF file, {@code null} for a document that has none (it has no bookmarks).
	 */
	public void bindDocument(final String filePath){
		document = filePath;
		bookmarks.clear();
		if(filePath == null)
			return;

		final Path meta = ProjectMeta.file(filePath, META_FILE_NAME);
		if(Files.exists(meta)){
			bookmarks.putAll(parseAll(ProjectMeta.read(meta)));
			return;
		}

		for(final Bookmark bookmark : global.values()){
			final String owner = bookmark.property(FILE_PROPERTY);
			if(owner != null && samePath(owner, filePath))
				bookmarks.put(bookmark.id(), bookmark);
		}
		if(!bookmarks.isEmpty())
			save();
	}

	private Map<String, Bookmark> active(){
		return (document != null? bookmarks: global);
	}

	/** Returns every bookmark, ordered by creation time (oldest first). */
	public List<Bookmark> all(){
		final List<Bookmark> list = new ArrayList<>(active().values());
		list.sort(Comparator.comparingLong(Bookmark::createdAt));
		return list;
	}

	/**
	 * Returns the bookmarks that can be shown under the privacy policy: those whose root record is not hidden.
	 *
	 * @param model	The model the root ids refer to.
	 * @return	The bookmarks, ordered by creation time (oldest first).
	 */
	public List<Bookmark> visible(final FLEFModel model, final String filePath){
		return all().stream()
			.filter(bookmark -> belongsToDocument(bookmark, filePath))
			.filter(bookmark -> bookmark.rootId().isEmpty()
				|| !PrivacyService.isHidden(model, model.getRecordById(bookmark.rootId())))
			.toList();
	}

	/**
	 * A bookmark refers to the identifier of a record, and the same identifier is another record in another file: it belongs to the file
	 * it was made in, and is shown only there. One that is tied to no file (made before they were) belongs to none, and is not shown.
	 */
	private boolean belongsToDocument(final Bookmark bookmark, final String filePath){
		//the bookmarks next to a file are all of that file, whatever path they were made under (the file may have been copied or moved)
		if(document != null)
			return true;

		final String file = bookmark.property(FILE_PROPERTY);
		return (file != null && filePath != null && samePath(file, filePath));
	}

	private static boolean samePath(final String a, final String b){
		try{
			return Path.of(a).equals(Path.of(b));
		}
		catch(final InvalidPathException ignored){
			return a.equals(b);
		}
	}

	public boolean isEmpty(){
		return active().isEmpty();
	}

	public Bookmark get(final String id){
		return active().get(id);
	}

	/** Adds or replaces a bookmark and saves the store. */
	public void add(final Bookmark bookmark){
		if(bookmark == null)
			return;
		active().put(bookmark.id(), bookmark);
		save();
	}

	/** Removes a bookmark by id and saves the store. */
	public boolean remove(final String id){
		if(id == null || active().remove(id) == null)
			return false;
		save();
		return true;
	}

	/** Renames a bookmark and saves the store. */
	public void rename(final String id, final String newName){
		final Bookmark b = active().get(id);
		if(b == null)
			return;
		active().put(id, b.withName(newName));
		save();
	}

	/** Creates a new id for a bookmark. */
	public static String newId(){
		return UUID.randomUUID().toString();
	}


	/* ======================================================================
	 *                          Persistence
	 * ====================================================================== */

	private void load(){
		global.clear();
		if(!Files.exists(file))
			return;

		final Properties props = new Properties();
		try(final InputStream is = Files.newInputStream(file)){
			props.load(is);
		}
		catch(final IOException e){
			LOGGER.warn("Could not load bookmarks from {}: {}", file, e.getMessage());

			return;
		}

		global.putAll(parseAll(props));
	}

	private static Map<String, Bookmark> parseAll(final Properties props){
		final Map<String, Bookmark> parsed = new LinkedHashMap<>();
		// Group keys by uuid.
		final Map<String, Map<String, String>> byId = new LinkedHashMap<>();
		for(final String key : props.stringPropertyNames()){
			if(!key.startsWith(PREFIX))
				continue;
			final String rest = key.substring(PREFIX.length());
			final int dot = rest.indexOf('.');
			if(dot < 0)
				continue;
			final String uuid = rest.substring(0, dot);
			final String field = rest.substring(dot + 1);
			byId.computeIfAbsent(uuid, k -> new LinkedHashMap<>()).put(field, props.getProperty(key));
		}

		for(final Map.Entry<String, Map<String, String>> e : byId.entrySet()){
			final Bookmark b = parse(e.getKey(), e.getValue());
			if(b != null)
				parsed.put(b.id(), b);
		}
		return parsed;
	}

	private static Bookmark parse(final String uuid, final Map<String, String> fields){
		try{
			final String name = fields.getOrDefault("name", StringUtils.EMPTY);
			final BookmarkType type = BookmarkType.valueOf(fields.getOrDefault("type", "TREE"));
			final String rootId = fields.getOrDefault("root", StringUtils.EMPTY);
			final long createdAt = Long.parseLong(fields.getOrDefault("createdAt", "0"));

			final Map<String, String> custom = new LinkedHashMap<>();
			for(final Map.Entry<String, String> e : fields.entrySet())
				if(e.getKey().startsWith("prop."))
					custom.put(e.getKey().substring("prop.".length()), e.getValue());

			return new Bookmark(uuid, name, type, rootId, custom, createdAt);
		}
		catch(final RuntimeException ex){
			LOGGER.warn("Skipping malformed bookmark {}: {}", uuid, ex.getMessage());

			return null;
		}
	}

	private void save(){
		final Properties props = new Properties();
		for(final Bookmark b : active().values()){
			final String p = PREFIX + b.id() + ".";
			props.setProperty(p + "name", b.name());
			props.setProperty(p + "type", b.type().name());
			props.setProperty(p + "root", b.rootId());
			props.setProperty(p + "createdAt", Long.toString(b.createdAt()));
			for(final Map.Entry<String, String> e : b.properties().entrySet())
				props.setProperty(p + "prop." + e.getKey(), e.getValue());
		}

		if(document != null){
			final Path meta = ProjectMeta.file(document, META_FILE_NAME);
			//no bookmarks and no folder: none is created; once the file exists it is kept (empty), so that the bookmarks of the older
			//versions are not read again
			if(!props.isEmpty() || Files.exists(meta))
				ProjectMeta.write(meta, props, "bookmarks");
			return;
		}

		try{
			final Path parent = file.toAbsolutePath()
				.getParent();
			if(parent != null)
				Files.createDirectories(parent);
			//written through a temporary file, so that a crash does not leave half of the bookmarks
			final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			props.store(bytes, "FamilyLegacy bookmarks");
			AtomicFileWriter.write(file, bytes.toString(StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1);
		}
		catch(final IOException e){
			LOGGER.warn("Could not save bookmarks to {}: {}", file, e.getMessage());
		}
	}

}
