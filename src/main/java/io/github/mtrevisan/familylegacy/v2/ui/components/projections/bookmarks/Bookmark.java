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

import java.util.Map;


/**
 * A saved view: a name, a type, a root entity id, and a bag of
 * type-specific properties.
 * <p>
 * The record is immutable. Properties are stored as strings to keep
 * serialization simple and human-readable; the caller is responsible
 * for parsing them back.
 *
 * @param id         a UUID that identifies the bookmark across renames
 * @param name       the user-visible label
 * @param type       the kind of state captured
 * @param rootId     the root entity id (individual or group)
 * @param properties type-specific properties (may be empty)
 * @param createdAt  creation timestamp, in milliseconds since the epoch
 */
public record Bookmark(
	String id,
	String name,
	BookmarkType type,
	String rootId,
	Map<String, String> properties,
	long createdAt
){

	public Bookmark{
		if(id == null || id.isBlank())
			throw new IllegalArgumentException("Bookmark id must not be blank");
		if(name == null)
			name = "";
		if(type == null)
			throw new IllegalArgumentException("Bookmark type must not be null");
		if(rootId == null)
			rootId = "";
		properties = (properties != null? Map.copyOf(properties): Map.of());
	}


	/** Returns a copy of this bookmark with a new name. The id is preserved. */
	public Bookmark withName(final String newName){
		return new Bookmark(id, newName, type, rootId, properties, createdAt);
	}

	/** Reads a string property, or {@code null} when absent. */
	public String property(final String key){
		return properties.get(key);
	}

	/** Reads an int property with a fallback. */
	public int intProperty(final String key, final int fallback){
		final String value = properties.get(key);
		if(value == null)
			return fallback;
		try{
			return Integer.parseInt(value);
		}
		catch(final NumberFormatException ignored){
			return fallback;
		}
	}

	/** Reads a boolean property with a fallback. */
	public boolean booleanProperty(final String key, final boolean fallback){
		final String value = properties.get(key);
		return (value != null? Boolean.parseBoolean(value): fallback);
	}

}
