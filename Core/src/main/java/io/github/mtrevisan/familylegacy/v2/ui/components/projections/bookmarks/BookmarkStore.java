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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.bookmarks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
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
 * Bookmarks are saved in a properties file under
 * {@code ~/.familylegacy/bookmarks.properties}. Each bookmark is stored
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

	private static final String PREFIX = "bookmark.";
	private static final Path DEFAULT_FILE = Path.of(
		System.getProperty("user.home"), ".familylegacy", "bookmarks.properties");


	private final Path file;
	private final Map<String, Bookmark> bookmarks = new LinkedHashMap<>();


	public BookmarkStore(){
		this(DEFAULT_FILE);
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

	/** Returns every bookmark, ordered by creation time (oldest first). */
	public List<Bookmark> all(){
		final List<Bookmark> list = new ArrayList<>(bookmarks.values());
		list.sort(Comparator.comparingLong(Bookmark::createdAt));
		return list;
	}

	public boolean isEmpty(){
		return bookmarks.isEmpty();
	}

	public Bookmark get(final String id){
		return bookmarks.get(id);
	}

	/** Adds or replaces a bookmark and saves the store. */
	public void add(final Bookmark bookmark){
		if(bookmark == null)
			return;
		bookmarks.put(bookmark.id(), bookmark);
		save();
	}

	/** Removes a bookmark by id and saves the store. */
	public boolean remove(final String id){
		if(id == null || bookmarks.remove(id) == null)
			return false;
		save();
		return true;
	}

	/** Renames a bookmark and saves the store. */
	public void rename(final String id, final String newName){
		final Bookmark b = bookmarks.get(id);
		if(b == null)
			return;
		bookmarks.put(id, b.withName(newName));
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
		bookmarks.clear();
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
				bookmarks.put(b.id(), b);
		}
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
		props.setProperty("#", "FamilyLegacy bookmarks");
		for(final Bookmark b : bookmarks.values()){
			final String p = PREFIX + b.id() + ".";
			props.setProperty(p + "name", b.name());
			props.setProperty(p + "type", b.type().name());
			props.setProperty(p + "root", b.rootId());
			props.setProperty(p + "createdAt", Long.toString(b.createdAt()));
			for(final Map.Entry<String, String> e : b.properties().entrySet())
				props.setProperty(p + "prop." + e.getKey(), e.getValue());
		}

		try{
			Files.createDirectories(file.getParent());
			try(final OutputStream os = Files.newOutputStream(file)){
				props.store(os, "FamilyLegacy bookmarks");
			}
		}
		catch(final IOException e){
			LOGGER.warn("Could not save bookmarks to {}: {}", file, e.getMessage());
		}
	}

}
