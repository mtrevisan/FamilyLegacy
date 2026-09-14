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


/**
 * The kind of state captured by a bookmark.
 * <p>
 * A bookmark is a projection plus a root entity. The dossier panel is
 * not part of the bookmark: applying a root individual or group
 * triggers the selection callback that populates the corresponding
 * dossier, so capturing the dossier separately would be redundant.
 */
public enum BookmarkType{

	/** Ancestor tree loaded on a root individual. */
	TREE("Tree"),

	/** Sugiyama pedigree graph loaded on a root individual. */
	SUGIYAMA("Sugiyama graph"),

	/** Ego network loaded on a root individual or group. */
	EGO_NETWORK("Ego network");


	private final String displayName;


	BookmarkType(final String displayName){
		this.displayName = displayName;
	}


	public String displayName(){
		return displayName;
	}

}
